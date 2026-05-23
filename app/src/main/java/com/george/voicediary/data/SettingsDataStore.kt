package com.george.voicediary.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val reminderEnabledKey = booleanPreferencesKey("reminder_enabled")
    private val reminderHourKey = intPreferencesKey("reminder_hour")
    private val reminderMinuteKey = intPreferencesKey("reminder_minute")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val fontSizeKey = stringPreferencesKey("font_size")
    private val biometricEnabledKey = booleanPreferencesKey("biometric_enabled")

    val reminderEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[reminderEnabledKey] ?: false }

    val reminderHour: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[reminderHourKey] ?: 21 }

    val reminderMinute: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[reminderMinuteKey] ?: 0 }

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { preferences -> 
            val modeStr = preferences[themeModeKey] ?: ThemeMode.SYSTEM.name
            try { ThemeMode.valueOf(modeStr) } catch (e: Exception) { ThemeMode.SYSTEM }
        }

    val fontSize: Flow<FontSize> = context.dataStore.data
        .map { preferences -> 
            val sizeStr = preferences[fontSizeKey] ?: FontSize.MEDIUM.name
            try { FontSize.valueOf(sizeStr) } catch (e: Exception) { FontSize.MEDIUM }
        }

    val biometricEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[biometricEnabledKey] ?: false }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[reminderEnabledKey] = enabled
        }
    }

    suspend fun setReminderHour(hour: Int) {
        context.dataStore.edit { preferences ->
            preferences[reminderHourKey] = hour
        }
    }

    suspend fun setReminderMinute(minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[reminderMinuteKey] = minute
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[themeModeKey] = mode.name
        }
    }

    suspend fun setFontSize(size: FontSize) {
        context.dataStore.edit { preferences ->
            preferences[fontSizeKey] = size.name
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[biometricEnabledKey] = enabled
        }
    }
}

enum class ThemeMode { LIGHT, DARK, AMOLED, SYSTEM }
enum class FontSize { SMALL, MEDIUM, LARGE }
