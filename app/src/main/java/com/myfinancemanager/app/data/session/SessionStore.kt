package com.myfinancemanager.app.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session")

data class Session(
    val userId: String,
    val email: String,
    val displayName: String,
    val accessToken: String,
    val refreshToken: String,
    val provider: String
)

class SessionStore(private val context: Context) {
    private val userId = stringPreferencesKey("user_id")
    private val email = stringPreferencesKey("email")
    private val displayName = stringPreferencesKey("display_name")
    private val accessToken = stringPreferencesKey("access_token")
    private val refreshToken = stringPreferencesKey("refresh_token")
    private val provider = stringPreferencesKey("provider")

    val session: Flow<Session?> = context.sessionDataStore.data.map { prefs ->
        val id = prefs[userId] ?: return@map null
        Session(
            userId = id,
            email = prefs[email].orEmpty(),
            displayName = prefs[displayName].orEmpty(),
            accessToken = prefs[accessToken].orEmpty(),
            refreshToken = prefs[refreshToken].orEmpty(),
            provider = prefs[provider].orEmpty()
        )
    }

    suspend fun save(session: Session) {
        context.sessionDataStore.edit { prefs ->
            prefs[userId] = session.userId
            prefs[email] = session.email
            prefs[displayName] = session.displayName
            prefs[accessToken] = session.accessToken
            prefs[refreshToken] = session.refreshToken
            prefs[provider] = session.provider
        }
    }

    suspend fun updateDisplayName(name: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[displayName] = name
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}
