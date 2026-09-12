package com.myfinancemanager.app.data.remote

import com.myfinancemanager.app.data.session.Session
import com.myfinancemanager.app.data.session.SessionStore

class TokenRefresh(private val sessionStore: SessionStore) {
    suspend fun refresh(current: Session): Session? {
        val api = ApiClient.create { current.refreshToken }
        val remote = runCatching {
            api.refresh(RefreshRequest(current.refreshToken))
        }.getOrNull() ?: return null
        val next = current.copy(
            accessToken = remote.accessToken,
            refreshToken = remote.refreshToken.ifBlank { current.refreshToken }
        )
        sessionStore.save(next)
        return next
    }
}
