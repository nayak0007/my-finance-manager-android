package com.myfinancemanager.app.data.remote

import com.myfinancemanager.app.data.session.SessionStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp [Authenticator] that silently replaces an expired Neon Auth JWT on 401.
 *
 * Neon Auth issues 15-minute JWTs, so a long-lived app would otherwise be signed out constantly.
 * A 401 from the backend means the JWT expired (or was rejected), and the fix is to ask Neon
 * Auth for a new one using the session cookie that [com.myfinancemanager.app.data.session.NeonAuthCookieJar]
 * keeps. There is no refresh token to exchange.
 *
 * Behavior:
 * - Only attached to the backend client. The Neon Auth client has no authenticator, so a 401
 *   from sign-in itself is a real credential failure rather than an expiry to retry.
 * - Single-flight: concurrent 401s across threads wait on a mutex and then reuse whichever
 *   token won the race, so one expiry costs one token request, not one per in-flight call.
 * - Bounded: follows OkHttp's [Response.priorResponse] chain, giving up after one retry so a
 *   failing request can never ping-pong forever.
 * - Definitive vs transient: if Neon Auth rejects the session (4xx) the stored session is
 *   cleared so the UI returns to the login screen; an IO error leaves the session intact so the
 *   original call simply fails and can be retried.
 *
 * Runs on OkHttp's dispatcher threads and must therefore stay synchronous internally
 * (blocking bridge into coroutines via [runBlocking]).
 */
class TokenAuthenticator(
    private val sessionStore: SessionStore,
    private val neonAuth: NeonAuthApi
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Bound retries: if this response already follows prior attempts, stop.
        if (response.priorResponse?.priorResponse != null) return null

        val failedAccessToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.takeIf { it.isNotBlank() }

        val refreshed = runBlocking {
            mutex.withLock {
                val stored = sessionStore.currentSync() ?: return@runBlocking null

                // Another thread may have refreshed while we waited. If the request was made
                // with the current stored token, re-issuing it with the new one is enough —
                // no second round-trip to Neon Auth.
                if (failedAccessToken != null && stored.accessToken != failedAccessToken) {
                    return@runBlocking stored
                }

                when (val result = TokenRefresh(sessionStore, neonAuth).refresh(stored)) {
                    is RefreshResult.Success -> result.session
                    RefreshResult.Rejected -> {
                        // The Neon Auth session is gone: the user has to sign in again.
                        sessionStore.clear()
                        null
                    }
                    RefreshResult.Transient -> null
                }
            }
        } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${refreshed.accessToken}")
            .build()
    }
}
