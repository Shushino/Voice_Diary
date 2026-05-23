package com.george.voicediary.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.george.voicediary.data.SettingsDataStore
import com.george.voicediary.data.manager.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsDataStore.reminderEnabled,
        settingsDataStore.reminderHour,
        settingsDataStore.reminderMinute
    ) { enabled, hour, minute ->
        SettingsUiState(enabled, hour, minute)
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
}

data class SettingsUiState(
    val reminderEnabled: Boolean = false,
    val hour: Int = 21,
    val minute: Int = 0
)