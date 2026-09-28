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
import com.shushino.voicediary.data.manager.NoEntriesToExportException
import com.shushino.voicediary.data.manager.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    /** True while a backup export/import is running; the UI disables the buttons. */
    private val _isBackupBusy = MutableStateFlow(false)
    val isBackupBusy: StateFlow<Boolean> = _isBackupBusy.asStateFlow()

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
                // Read the stored time from DataStore, not the (possibly stale) uiState.
                val hour = settingsDataStore.reminderHour.first()
                val minute = settingsDataStore.reminderMinute.first()
                reminderScheduler.scheduleDailyReminder(hour, minute)
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
            val enabled = settingsDataStore.reminderEnabled.first()
            if (enabled) {
                // Re-anchor so the next fire actually lands at the newly chosen time
                // (UPDATE keeps the original WorkManager period anchor).
                reminderScheduler.scheduleDailyReminder(hour, minute, reanchor = true)
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

    fun exportAllEntries(includeAudio: Boolean = true, includeImages: Boolean = true) {
        if (_isBackupBusy.value) return
        _isBackupBusy.value = true
        viewModelScope.launch {
            try {
                backupManager.exportAllEntries(includeAudio, includeImages)
                    .onSuccess { count ->
                        _exportStatus.emit("Exported $count entries to Downloads ✓")
                    }
                    .onFailure { e ->
                        if (e is NoEntriesToExportException) {
                            _exportStatus.emit(e.message ?: "No entries to export")
                        } else {
                            _exportStatus.emit("Export failed: ${e.message ?: e.localizedMessage ?: "unknown error"}")
                        }
                    }
            } finally {
                _isBackupBusy.value = false
            }
        }
    }

    fun importBackup(uri: Uri) {
        if (_isBackupBusy.value) return
        _isBackupBusy.value = true
        viewModelScope.launch {
            try {
                backupManager.importBackup(uri)
                    .onSuccess {
                        _exportStatus.emit("Import successful ✓")
                    }
                    .onFailure { e ->
                        _exportStatus.emit("Import failed: ${e.localizedMessage}")
                    }
            } finally {
                _isBackupBusy.value = false
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
