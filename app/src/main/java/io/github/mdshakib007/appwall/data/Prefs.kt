package io.github.mdshakib007.appwall.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.mdshakib007.appwall.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "appwall_prefs")

class Prefs(private val context: Context) {
    private object K {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val themeMode = stringPreferencesKey("theme_mode")
        val dnsFilter = booleanPreferencesKey("dns_filter_enabled")
        val installedAt = longPreferencesKey("installed_at")
        val settingsGuard = booleanPreferencesKey("settings_guard")
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[K.onboardingDone] ?: false }
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[K.themeMode] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)
    }
    /** Whether the user wants the DNS-level website filter (VPN) running. Default on. */
    val dnsFilterEnabled: Flow<Boolean> = context.dataStore.data.map { it[K.dnsFilter] ?: true }
    val installedAt: Flow<Long> = context.dataStore.data.map { it[K.installedAt] ?: 0L }
    /** During Focus Mode, keep the user out of AppWall's own system-settings pages. Default on. */
    val settingsGuard: Flow<Boolean> = context.dataStore.data.map { it[K.settingsGuard] ?: true }

    suspend fun setOnboardingDone(done: Boolean) = context.dataStore.edit { it[K.onboardingDone] = done }
    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[K.themeMode] = mode.name }
    suspend fun setDnsFilterEnabled(enabled: Boolean) = context.dataStore.edit { it[K.dnsFilter] = enabled }
    suspend fun setSettingsGuard(enabled: Boolean) = context.dataStore.edit { it[K.settingsGuard] = enabled }
    suspend fun ensureInstalledAt(now: Long) = context.dataStore.edit { if (it[K.installedAt] == null) it[K.installedAt] = now }
}
