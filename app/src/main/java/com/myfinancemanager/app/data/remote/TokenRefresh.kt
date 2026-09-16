package com.myfinancemanager.app.data.remote

import com.myfinancemanager.app.data.session.Session
import com.myfinancemanager.app.data.session.SessionStore
import retrofit2.HttpException
import java.io.IOException

/** Outcome of a refresh-token exchange. */
sealed interface RefreshResult {
    /** New tokens obtained and persisted. */
    data class Success(val session: Session) : RefreshResult

    /** The server rejected the refresh token (4xx) — the session is unrecoverable. */
    data object Rejected : RefreshResult

    /** Network/IO or server-side (5xx) problem — retry later, session still valid. */
    data object Transient : RefreshResult
}

class TokenRefresh(
    private val sessionStore: SessionStore,
    private val neonAuth: NeonAuthApi
) {

    /**
     * Mints a fresh JWT from Neon Auth and persists it.
     *
     * Neon Auth authorises `GET /token` with its own session cookie (persisted by
     * [com.myfinancemanager.app.data.session.NeonAuthCookieJar]) rather than a refresh token,
     * so there is nothing to pass in. The client is the bare Neon Auth one, which has no
     * authenticator attached, so a failing refresh can never recurse.
     */
    suspend fun refresh(current: Session): RefreshResult {
        return try {
            val next = current.copy(accessToken = neonAuth.token().token)
            sessionStore.save(next)
            RefreshResult.Success(next)
        } catch (e: HttpException) {
            // 4xx means the session cookie is gone or rejected: the user must sign in again.
            if (e.code() in 400..499) RefreshResult.Rejected else RefreshResult.Transient
        } catch (e: IOException) {
            RefreshResult.Transient
        }
    }
}
