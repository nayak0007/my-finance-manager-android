package com.myfinancemanager.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.prefsDataStore by preferencesDataStore(name = "user_prefs")

data class AppPreferences(
    val currencyCode: String = "INR",
    val smsCaptureEnabled: Boolean = false,
    val emailCaptureEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val insightFrequencyDays: Int = 7,
    val onboardingComplete: Boolean = false
)

class UserPreferences(private val context: Context) {
    private val currency = stringPreferencesKey("currency")
    private val smsEnabled = booleanPreferencesKey("sms_enabled")
    private val emailEnabled = booleanPreferencesKey("email_enabled")
    private val notifications = booleanPreferencesKey("notifications")
    private val insightDays = intPreferencesKey("insight_days")
    private val onboarded = booleanPreferencesKey("onboarded")

    val prefs: Flow<AppPreferences> = context.prefsDataStore.data.map { p ->
        AppPreferences(
            currencyCode = p[currency] ?: "INR",
            smsCaptureEnabled = p[smsEnabled] ?: false,
            emailCaptureEnabled = p[emailEnabled] ?: false,
            notificationsEnabled = p[notifications] ?: true,
            insightFrequencyDays = p[insightDays] ?: 7,
            onboardingComplete = p[onboarded] ?: false
        )
    }

    suspend fun setCurrency(code: String) {
        context.prefsDataStore.edit { it[currency] = code }
    }

    suspend fun setSmsEnabled(enabled: Boolean) {
        context.prefsDataStore.edit { it[smsEnabled] = enabled }
    }

    suspend fun setEmailEnabled(enabled: Boolean) {
        context.prefsDataStore.edit { it[emailEnabled] = enabled }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.prefsDataStore.edit { it[notifications] = enabled }
    }

    suspend fun setInsightFrequency(days: Int) {
        context.prefsDataStore.edit { it[insightDays] = days }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.prefsDataStore.edit { it[onboarded] = complete }
    }

    suspend fun clear() {
        context.prefsDataStore.edit { it.clear() }
    }
}
