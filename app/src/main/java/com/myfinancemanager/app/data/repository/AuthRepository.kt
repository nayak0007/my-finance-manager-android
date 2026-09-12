package com.myfinancemanager.app.data.repository

import com.myfinancemanager.app.data.local.dao.UserDao
import com.myfinancemanager.app.data.local.entity.UserEntity
import com.myfinancemanager.app.data.remote.ApiClient
import com.myfinancemanager.app.data.remote.AuthRequest
import com.myfinancemanager.app.data.remote.FinanceApi
import com.myfinancemanager.app.data.session.Session
import com.myfinancemanager.app.data.session.SessionStore
import com.myfinancemanager.app.util.Dates
import com.myfinancemanager.app.util.Ids
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest

class AuthRepository(
    private val userDao: UserDao,
    private val sessionStore: SessionStore
) {
    private val api: FinanceApi = ApiClient.create { null }

    val session: Flow<Session?> = sessionStore.session

    suspend fun signUp(email: String, password: String, displayName: String): Result<Session> {
        val normalized = email.trim().lowercase()
        if (userDao.getByEmail(normalized) != null) {
            return Result.failure(IllegalStateException("An account with this email already exists."))
        }
        val remote = runCatching { api.signup(AuthRequest(normalized, password)) }.getOrNull()
        val user = UserEntity(
            id = remote?.userId ?: Ids.new(),
            email = normalized,
            displayName = displayName.ifBlank { normalized.substringBefore("@") },
            authProvider = "email",
            passwordHash = hash(password),
            createdAt = Dates.now()
        )
        userDao.upsert(user)
        val session = Session(
            userId = user.id,
            email = user.email,
            displayName = user.displayName,
            accessToken = remote?.accessToken ?: "local-${user.id}",
            refreshToken = remote?.refreshToken ?: "refresh-${user.id}",
            provider = "email"
        )
        sessionStore.save(session)
        return Result.success(session)
    }

    suspend fun login(email: String, password: String): Result<Session> {
        val normalized = email.trim().lowercase()
        val remote = runCatching { api.login(AuthRequest(normalized, password)) }.getOrNull()
        if (remote != null) {
            val user = UserEntity(
                id = remote.userId,
                email = remote.email,
                displayName = remote.displayName,
                authProvider = "email",
                passwordHash = hash(password),
                createdAt = Dates.now()
            )
            userDao.upsert(user)
            val session = Session(
                userId = remote.userId,
                email = remote.email,
                displayName = remote.displayName,
                accessToken = remote.accessToken,
                refreshToken = remote.refreshToken,
                provider = "email"
            )
            sessionStore.save(session)
            return Result.success(session)
        }
        val existing = userDao.getByEmail(normalized)
            ?: return Result.failure(IllegalArgumentException("No account found for this email."))
        if (existing.passwordHash != hash(password)) {
            return Result.failure(IllegalArgumentException("Incorrect password."))
        }
        val session = Session(
            userId = existing.id,
            email = existing.email,
            displayName = existing.displayName,
            accessToken = "local-${existing.id}",
            refreshToken = "refresh-${existing.id}",
            provider = existing.authProvider
        )
        sessionStore.save(session)
        return Result.success(session)
    }

    suspend fun loginWithGoogle(email: String, displayName: String): Result<Session> {
        val normalized = email.trim().lowercase()
        val existing = userDao.getByEmail(normalized)
        val user = existing ?: UserEntity(
            id = Ids.new(),
            email = normalized,
            displayName = displayName.ifBlank { normalized.substringBefore("@") },
            authProvider = "google",
            passwordHash = null,
            createdAt = Dates.now()
        ).also { userDao.upsert(it) }
        val session = Session(
            userId = user.id,
            email = user.email,
            displayName = user.displayName,
            accessToken = "google-${user.id}",
            refreshToken = "refresh-${user.id}",
            provider = "google"
        )
        sessionStore.save(session)
        return Result.success(session)
    }

    suspend fun logout() {
        sessionStore.clear()
    }

    suspend fun deleteAccount(userId: String) {
        userDao.delete(userId)
        sessionStore.clear()
    }

    suspend fun updateProfile(userId: String, displayName: String, currency: String) {
        val current = userDao.getById(userId) ?: return
        userDao.upsert(current.copy(displayName = displayName, currencyCode = currency))
        sessionStore.updateDisplayName(displayName)
    }

    private fun hash(password: String): String {
        val salt = "mfm-local-v1"
        val bytes = MessageDigest.getInstance("SHA-256").digest("$salt:$password".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
