package com.myfinancemanager.app.data.repository

import com.myfinancemanager.app.data.local.dao.UserDao
import com.myfinancemanager.app.data.local.entity.UserEntity
import com.myfinancemanager.app.data.remote.ApiErrors
import com.myfinancemanager.app.data.remote.FinanceApi
import com.myfinancemanager.app.data.remote.NeonAuthApi
import com.myfinancemanager.app.data.remote.NeonAuthResponse
import com.myfinancemanager.app.data.remote.NeonSignInBody
import com.myfinancemanager.app.data.remote.NeonSignUpBody
import com.myfinancemanager.app.data.remote.UpdateProfileBody
import com.myfinancemanager.app.data.session.NeonAuthCookieJar
import com.myfinancemanager.app.data.session.Session
import com.myfinancemanager.app.data.session.SessionStore
import com.myfinancemanager.app.util.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.HttpException
import java.io.IOException

/**
 * Owns the sign-in lifecycle.
 *
 * Credentials belong to Neon Auth: signing up and signing in are calls to its REST API, and the
 * JWT that results is the only thing this app ever presents to our own backend. No password is
 * hashed, stored or sent anywhere else.
 *
 * ### Why the local user id is the backend's id, not Neon Auth's
 *
 * [Session.userId] is the backend profile id, fetched once per sign-in from `GET /users/me`.
 * Every local record is partitioned by it, and it is stable across this migration because the
 * backend links a Neon Auth identity to an existing account by email. Reusing it means data
 * already on the phone stays attached to the same owner instead of being stranded under an id
 * nothing reads any more.
 */
class AuthRepository(
    private val userDao: UserDao,
    private val sessionStore: SessionStore,
    private val neonAuth: NeonAuthApi,
    private val cookieJar: NeonAuthCookieJar,
    private val financeApi: FinanceApi
) {
    val session: Flow<Session?> = sessionStore.session

    suspend fun signUp(email: String, password: String, displayName: String): Result<Session> {
        val normalized = email.trim().lowercase()
        val name = displayName.ifBlank { normalized.substringBefore("@") }
        val remote = runCatching { neonAuth.signUp(NeonSignUpBody(normalized, password, name)) }
            .getOrElse { return failure(it) }
        return completeSignIn(remote, normalized, name)
    }

    suspend fun login(email: String, password: String): Result<Session> {
        val normalized = email.trim().lowercase()
        val remote = runCatching { neonAuth.signIn(NeonSignInBody(normalized, password)) }
            .getOrElse { return failure(it) }
        return completeSignIn(remote, normalized, null)
    }

    /**
     * Turns a Neon Auth session into a stored app session.
     *
     * The token Neon Auth returns from sign-in is an opaque session token, not a JWT, so the JWT
     * is fetched separately from `GET /token`. A provisional session is saved first because the
     * authenticated client reads its bearer token from the session store, and the profile call
     * that follows needs one.
     */
    private suspend fun completeSignIn(
        remote: NeonAuthResponse,
        fallbackEmail: String,
        fallbackName: String?
    ): Result<Session> {
        val neonUser = remote.user
            ?: return Result.failure(
                IllegalStateException("Neon Auth did not return the account details.")
            )
        val email = neonUser.email.ifBlank { fallbackEmail }

        val jwt = runCatching { neonAuth.token().token }.getOrElse { return failure(it) }
        sessionStore.save(
            Session(
                userId = neonUser.id,
                email = email,
                displayName = neonUser.name?.takeIf { it.isNotBlank() } ?: fallbackName
                    ?: email.substringBefore("@"),
                accessToken = jwt,
                provider = PROVIDER
            )
        )

        // The account exists in Neon Auth from here on, so a failure past this point must say so
        // rather than inviting a retry that would hit "email already exists".
        val profile = runCatching { financeApi.profile() }.getOrElse {
            return Result.failure(
                IllegalStateException(
                    "Your account was created, but the server could not be reached to finish " +
                        "signing in. Please sign in again in a moment."
                )
            )
        }

        val displayName = profile.fullName?.takeIf { it.isNotBlank() }
            ?: neonUser.name?.takeIf { it.isNotBlank() }
            ?: fallbackName ?: email.substringBefore("@")
        userDao.upsert(
            UserEntity(
                id = profile.id,
                email = profile.email.ifBlank { email },
                displayName = displayName,
                authProvider = PROVIDER,
                passwordHash = null,
                createdAt = Dates.now()
            )
        )

        val session = Session(
            userId = profile.id,
            email = profile.email.ifBlank { email },
            displayName = displayName,
            accessToken = jwt,
            provider = PROVIDER
        )
        sessionStore.save(session)
        return Result.success(session)
    }

    /**
     * Ends the session on both sides: Neon Auth revokes it, and the local cookie and session are
     * cleared. Cached records are left alone — they are the user's own data and come back on the
     * next sign-in.
     */
    suspend fun logout() {
        runCatching { neonAuth.signOut() }
        cookieJar.clear()
        sessionStore.clear()
    }

    /**
     * Deletes every record this account owns, server-side, and clears the device.
     *
     * It cannot delete the sign-in itself. The managed Neon Auth service exposes no
     * account-deletion route on its public API — `POST /delete-user` answers 404 on this
     * instance regardless of the Origin sent — and the endpoint that can do it belongs to
     * Neon's management API, which requires a NEON_API_KEY this app does not hold. Removing the
     * identity is therefore a Neon Console (or management API) operation, and the UI says so
     * rather than implying the account is gone.
     */
    suspend fun deleteAccount(): Result<Unit> {
        val purged = runCatching { financeApi.deleteAccount() }.getOrElse { return failure(it) }
        if (!purged.isSuccessful) {
            return Result.failure(
                IllegalStateException(
                    ApiErrors.messageFrom(purged.errorBody()?.string())
                        ?: "The server could not remove your records (HTTP ${purged.code()}). " +
                        "Please try again."
                )
            )
        }
        cookieJar.clear()
        return Result.success(Unit)
    }

    /** Persists the display name and currency on the server, then mirrors them locally. */
    suspend fun updateProfile(userId: String, displayName: String, currency: String): Result<Unit> {
        val remote = runCatching {
            financeApi.updateProfile(UpdateProfileBody(fullName = displayName, currency = currency))
        }.getOrElse { return failure(it) }
        val name = remote.fullName?.takeIf { it.isNotBlank() } ?: displayName
        // Keep the local row's id: every record is partitioned by it, so it must not move.
        userDao.getById(userId)?.let { current ->
            userDao.upsert(
                current.copy(
                    displayName = name,
                    currencyCode = remote.currency ?: currency
                )
            )
        }
        sessionStore.updateDisplayName(name)
        return Result.success(Unit)
    }

    /** Clears the local session and profile row; used once the remote account is gone. */
    suspend fun clearLocalAccount(userId: String) {
        userDao.delete(userId)
        cookieJar.clear()
        sessionStore.clear()
    }

    suspend fun currentSession(): Session? = sessionStore.session.firstOrNull()

    /**
     * Turns a transport failure into a message worth showing.
     *
     * Retrofit's own message is only "HTTP 400 Bad Request". Both Neon Auth and our backend put
     * the useful text in a `message` field ("Invalid email or password", "An account with this
     * email already exists", per-field validation details), so that is parsed out here.
     */
    private fun failure(error: Throwable): Result<Nothing> = when (error) {
        is HttpException -> Result.failure(
            IllegalStateException(
                ApiErrors.messageFrom(error.response()?.errorBody()?.string())
                    ?: "Request failed (HTTP ${error.code()}). Please try again."
            )
        )
        is IOException -> Result.failure(
            IllegalStateException("Cannot reach the server. Check your connection and try again.")
        )
        else -> Result.failure(
            IllegalStateException(error.message ?: "Something went wrong. Please try again.")
        )
    }

    private companion object {
        const val PROVIDER = "neon-auth"
    }
}
