package com.myfinancemanager.app.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/**
 * The signed-in user.
 *
 * [userId] is the Neon Auth user id (the JWT's `sub`), which is also the key every local record
 * is partitioned by.
 *
 * There is no refresh token: Neon Auth refreshes the JWT through its own session cookie, which
 * [NeonAuthCookieJar] persists. [accessToken] is the short-lived JWT that the backend verifies.
 */
data class Session(
    val userId: String,
    val email: String,
    val displayName: String,
    val accessToken: String,
    val provider: String
)

class SessionStore(private val context: Context) {
    private val userId = stringPreferencesKey("user_id")
    private val email = stringPreferencesKey("email")
    private val displayName = stringPreferencesKey("display_name")
    private val accessToken = stringPreferencesKey("access_token")
    private val provider = stringPreferencesKey("provider")

    /** In-memory mirror of the persisted session for synchronous OkHttp callbacks. */
    @Volatile
    private var cached: Session? = null

    val session: Flow<Session?> = context.sessionDataStore.data.map { prefs ->
        val id = prefs[userId] ?: return@map null
        Session(
            userId = id,
            email = prefs[email].orEmpty(),
            displayName = prefs[displayName].orEmpty(),
            accessToken = prefs[accessToken].orEmpty(),
            provider = prefs[provider].orEmpty()
        )
    }

    /**
     * Synchronous snapshot of the stored session, safe to call from OkHttp interceptor
     * and authenticator threads. Only the first read touches DataStore (blocking); later
     * reads are served from the cache maintained by [save] and [clear].
     */
    fun currentSync(): Session? {
        cached?.let { return it }
        return runBlocking { session.first().also { cached = it } }
    }

    suspend fun save(session: Session) {
        context.sessionDataStore.edit { prefs ->
            prefs[userId] = session.userId
            prefs[email] = session.email
            prefs[displayName] = session.displayName
            prefs[accessToken] = session.accessToken
            prefs[provider] = session.provider
        }
        cached = session
    }

    suspend fun updateDisplayName(name: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[displayName] = name
        }
        cached = cached?.copy(displayName = name)
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
        cached = null
    }
}
