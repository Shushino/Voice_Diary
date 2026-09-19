package com.shushino.voicediary.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shushino.voicediary.data.ColorPalette
import com.shushino.voicediary.data.FontSize
import com.shushino.voicediary.data.SettingsDataStore
import com.shushino.voicediary.data.ThemeMode
import com.shushino.voicediary.data.manager.BackupManager
import com.shushino.voicediary.data.manager.LockManager
import com.shushino.voicediary.data.manager.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val reminderScheduler: ReminderScheduler,
    private val backupManager: BackupManager,
    private val lockManager: LockManager
) : ViewModel() {

    private val _exportStatus = MutableSharedFlow<String>()
    val exportStatus = _exportStatus.asSharedFlow()

    val isPinSet: StateFlow<Boolean> = lockManager.isPinSetFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsDataStore.reminderEnabled,
        settingsDataStore.reminderHour,
        settingsDataStore.reminderMinute,
        settingsDataStore.themeMode,
        settingsDataStore.fontSize,
        settingsDataStore.biometricEnabled,
        settingsDataStore.colorPalette,
        isPinSet
    ) { params: Array<Any> ->
        SettingsUiState(
            reminderEnabled = params[0] as Boolean,
            hour = params[1] as Int,
            minute = params[2] as Int,
            themeMode = params[3] as ThemeMode,
            fontSize = params[4] as FontSize,
            biometricEnabled = params[5] as Boolean,
            colorPalette = params[6] as ColorPalette,
            pinSet = params[7] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun toggleReminder(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setReminderEnabled(enabled)
            if (enabled) {
                reminderScheduler.scheduleDailyReminder(uiState.value.hour, uiState.value.minute)
                reminderScheduler.scheduleWeeklySummary()
            } else {
                reminderScheduler.cancelAll()
            }
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsDataStore.setReminderHour(hour)
            settingsDataStore.setReminderMinute(minute)
            if (uiState.value.reminderEnabled) {
                reminderScheduler.scheduleDailyReminder(hour, minute)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun setFontSize(size: FontSize) {
        viewModelScope.launch {
            settingsDataStore.setFontSize(size)
        }
    }

    fun setColorPalette(palette: ColorPalette) {
        viewModelScope.launch {
            settingsDataStore.setColorPalette(palette)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setBiometricEnabled(enabled)
        }
    }

    fun removePin() {
        viewModelScope.launch {
            lockManager.clearPin()
        }
    }

    fun exportAllEntries(includeAudio: Boolean = true, includeImages: Boolean = true) {
        viewModelScope.launch {
            backupManager.exportAllEntries(includeAudio, includeImages)
                .onSuccess { count ->
                    _exportStatus.emit("Exported $count entries to Downloads ✓")
                }
                .onFailure { e ->
                    val message = e.message ?: e.localizedMessage ?: "Export failed"
                    if (message == "No entries to export") {
                        _exportStatus.emit(message)
                    } else {
                        _exportStatus.emit("Export failed: $message")
                    }
                }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.importBackup(uri)
                .onSuccess {
                    _exportStatus.emit("Import successful ✓")
                }
                .onFailure { e ->
                    _exportStatus.emit("Import failed: ${e.localizedMessage}")
                }
        }
    }
}

data class SettingsUiState(
    val reminderEnabled: Boolean = false,
    val hour: Int = 21,
    val minute: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: FontSize = FontSize.MEDIUM,
    val colorPalette: ColorPalette = ColorPalette.DEFAULT,
    val biometricEnabled: Boolean = false,
    val pinSet: Boolean = false
)
