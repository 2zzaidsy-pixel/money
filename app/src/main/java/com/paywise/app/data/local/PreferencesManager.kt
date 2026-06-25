package com.paywise.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "paywise_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_IS_ONBOARDING_DONE = booleanPreferencesKey("is_onboarding_done")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_CURRENCY = stringPreferencesKey("currency")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_BUDGET_ALERTS = booleanPreferencesKey("budget_alerts")
        val KEY_LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    }

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_ONBOARDING_DONE] ?: false
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_LOGGED_IN] ?: false
    }

    val userId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE] ?: false
    }

    val currentLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "en"
    }

    val currentCurrency: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURRENCY] ?: "SAR"
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val budgetAlerts: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BUDGET_ALERTS] ?: true
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[KEY_IS_ONBOARDING_DONE] = true }
    }

    suspend fun setLoggedIn(userId: String) {
        context.dataStore.edit {
            it[KEY_IS_LOGGED_IN] = true
            it[KEY_USER_ID] = userId
        }
    }

    suspend fun setLoggedOut() {
        context.dataStore.edit {
            it[KEY_IS_LOGGED_IN] = false
            it[KEY_USER_ID] = ""
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { it[KEY_CURRENCY] = currency }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setBudgetAlerts(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BUDGET_ALERTS] = enabled }
    }

    suspend fun updateLastSyncTime() {
        context.dataStore.edit { it[KEY_LAST_SYNC_TIME] = System.currentTimeMillis() }
    }
}
