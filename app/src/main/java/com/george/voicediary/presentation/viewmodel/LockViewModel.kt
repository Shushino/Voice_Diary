package com.george.voicediary.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.george.voicediary.data.manager.LockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val lockManager: LockManager
) : ViewModel() {

    private val _state = MutableStateFlow(LockUiState())
    val state: StateFlow<LockUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isPinSet = lockManager.isPinSet(), isBiometricAvailable = lockManager.isBiometricAvailable()) }
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
        _state.update { it.copy(isUnlocked = true) }
    }

    private fun verifyPin() {
        viewModelScope.launch {
            delay(200) // Small delay for UI feedback
            val isCorrect = lockManager.verifyPin(_state.value.pinInput)
            if (isCorrect) {
                _state.update { it.copy(isUnlocked = true, errorMessage = null) }
            } else {
                _state.update { it.copy(pinInput = "", errorMessage = "Incorrect PIN") }
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
    val isBiometricAvailable: Boolean = false
)