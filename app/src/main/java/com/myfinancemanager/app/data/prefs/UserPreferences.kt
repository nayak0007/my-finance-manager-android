package com.myfinancemanager.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.prefsDataStore by preferencesDataStore(name = "user_prefs")

data class AppPreferences(
    val currencyCode: String = "INR",
    val smsCaptureEnabled: Boolean = false,
    val emailCaptureEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val insightFrequencyDays: Int = 7,
    val onboardingComplete: Boolean = false,
    /**
     * Set when the capture switches or the sender lists change on this device, so the next sync
     * pushes them instead of pulling the account's copy over the top of the edit.
     */
    val captureSettingsDirty: Boolean = false,
    /** As above, for notifications, insight frequency and currency. */
    val profileSettingsDirty: Boolean = false
)

/**
 * Local cache of the account's settings.
 *
 * Everything here has a home on the backend, and this store is what makes the UI respond
 * instantly instead of waiting for a round-trip: a change is written here, flagged dirty, and
 * pushed by the next sync. The copy the account holds is always the authority on the pull side.
 */
class UserPreferences(private val context: Context) {
    private val currency = stringPreferencesKey("currency")
    private val smsEnabled = booleanPreferencesKey("sms_enabled")
    private val emailEnabled = booleanPreferencesKey("email_enabled")
    private val notifications = booleanPreferencesKey("notifications")
    private val insightDays = intPreferencesKey("insight_days")
    private val onboarded = booleanPreferencesKey("onboarded")
    private val captureDirty = booleanPreferencesKey("capture_settings_dirty")
    private val profileDirty = booleanPreferencesKey("profile_settings_dirty")

    val prefs: Flow<AppPreferences> = context.prefsDataStore.data.map { p ->
        AppPreferences(
            currencyCode = p[currency] ?: "INR",
            smsCaptureEnabled = p[smsEnabled] ?: false,
            emailCaptureEnabled = p[emailEnabled] ?: false,
            notificationsEnabled = p[notifications] ?: true,
            insightFrequencyDays = p[insightDays] ?: 7,
            onboardingComplete = p[onboarded] ?: false,
            captureSettingsDirty = p[captureDirty] ?: false,
            profileSettingsDirty = p[profileDirty] ?: false
        )
    }

    /** One-shot read, used by the sync engine to decide which direction settings travel. */
    suspend fun current(): AppPreferences = prefs.first()

    suspend fun setCurrency(code: String) {
        context.prefsDataStore.edit {
            it[currency] = code
            it[profileDirty] = true
        }
    }

    suspend fun setSmsEnabled(enabled: Boolean) {
        context.prefsDataStore.edit {
            it[smsEnabled] = enabled
            it[captureDirty] = true
        }
    }

    suspend fun setEmailEnabled(enabled: Boolean) {
        context.prefsDataStore.edit {
            it[emailEnabled] = enabled
            it[captureDirty] = true
        }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.prefsDataStore.edit {
            it[notifications] = enabled
            it[profileDirty] = true
        }
    }

    suspend fun setInsightFrequency(days: Int) {
        context.prefsDataStore.edit {
            it[insightDays] = days
            it[profileDirty] = true
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.prefsDataStore.edit { it[onboarded] = complete }
    }

    /**
     * Flags the capture settings for a push. Used when the change is not a switch — adding or
     * removing a sender rule edits the same account document.
     */
    suspend fun markCaptureSettingsDirty() {
        context.prefsDataStore.edit { it[captureDirty] = true }
    }

    /**
     * Applies the account's capture switches after a push or pull, clearing the pending flag.
     * Skips the write when nothing actually moved, so an unchanged account does not churn the
     * DataStore (and re-compose the whole UI) on every sync.
     */
    suspend fun applyRemoteCaptureSettings(smsEnabled: Boolean, emailEnabled: Boolean) {
        val local = current()
        if (!local.captureSettingsDirty &&
            local.smsCaptureEnabled == smsEnabled &&
            local.emailCaptureEnabled == emailEnabled
        ) {
            return
        }
        context.prefsDataStore.edit {
            it[this.smsEnabled] = smsEnabled
            it[this.emailEnabled] = emailEnabled
            it[captureDirty] = false
        }
    }

    /** Applies the account's profile settings after a push or pull, clearing the pending flag. */
    suspend fun applyRemoteProfileSettings(
        notificationsEnabled: Boolean,
        insightFrequencyDays: Int,
        currencyCode: String
    ) {
        val local = current()
        if (!local.profileSettingsDirty &&
            local.notificationsEnabled == notificationsEnabled &&
            local.insightFrequencyDays == insightFrequencyDays &&
            local.currencyCode == currencyCode
        ) {
            return
        }
        context.prefsDataStore.edit {
            it[notifications] = notificationsEnabled
            it[insightDays] = insightFrequencyDays
            it[currency] = currencyCode
            it[profileDirty] = false
        }
    }

    suspend fun clear() {
        context.prefsDataStore.edit { it.clear() }
    }
}
