package com.shushino.voicediary.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shushino.voicediary.data.manager.LockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.shushino.voicediary.data.SettingsDataStore
import kotlinx.coroutines.flow.collectLatest

@HiltViewModel
class LockViewModel @Inject constructor(
    private val lockManager: LockManager,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(LockUiState())
    val state: StateFlow<LockUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isPinSet = lockManager.isPinSet()) }
        }
        viewModelScope.launch {
            settingsDataStore.biometricEnabled.collectLatest { enabled ->
                val available = lockManager.isBiometricAvailable() && enabled
                _state.update { it.copy(isBiometricAvailable = available) }
            }
        }
    }

    fun onDigitEntered(digit: Char) {
        if (_state.value.pinInput.length < 4) {
            _state.update { it.copy(pinInput = it.pinInput + digit) }
            if (_state.value.pinInput.length == 4) {
                verifyPin()
            }
        }
    }

    fun onDelete() {
        if (_state.value.pinInput.isNotEmpty()) {
            _state.update { it.copy(pinInput = it.pinInput.dropLast(1), errorMessage = null) }
        }
    }

    fun onBiometricSuccess() {
        lockManager.setUnlocked(true)
        _state.update { it.copy(isUnlocked = true) }
    }

    private fun verifyPin() {
        viewModelScope.launch {
            _state.update { it.copy(isVerifying = true) }
            val isCorrect = lockManager.verifyPin(_state.value.pinInput)
            if (isCorrect) {
                lockManager.setUnlocked(true)
                _state.update { it.copy(isUnlocked = true, errorMessage = null, isVerifying = false) }
            } else {
                _state.update { it.copy(pinInput = "", errorMessage = "Incorrect PIN", isVerifying = false) }
            }
        }
    }

    fun resetPinInput() {
        _state.update { it.copy(pinInput = "", errorMessage = null) }
    }
}

data class LockUiState(
    val isPinSet: Boolean = false,
    val isUnlocked: Boolean = false,
    val pinInput: String = "",
    val errorMessage: String? = null,
    val isBiometricAvailable: Boolean = false,
    val isVerifying: Boolean = false
)
