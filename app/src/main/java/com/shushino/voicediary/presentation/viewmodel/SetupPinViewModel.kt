package com.shushino.voicediary.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shushino.voicediary.data.manager.LockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupPinViewModel @Inject constructor(
    private val lockManager: LockManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** True when the user came from "Remove PIN" (verify current PIN, then clear it). */
    private val removeMode: Boolean = savedStateHandle.get<Boolean>("remove") ?: false

    /** True when changing an existing PIN (must verify the current one first). */
    private val changeMode: Boolean = savedStateHandle.get<Boolean>("isChange") ?: false

    private val _state = MutableStateFlow(
        SetupPinUiState(
            // Changing or removing a PIN requires proving the current one first.
            currentStep = if (removeMode || changeMode) SetupPinStep.VERIFY_CURRENT else SetupPinStep.CHOOSE_PIN,
            removeMode = removeMode
        )
    )
    val state: StateFlow<SetupPinUiState> = _state.asStateFlow()

    private var firstPin: String? = null

    fun onDigitEntered(digit: Char) {
        if (_state.value.pinInput.length < 4) {
            _state.update { it.copy(pinInput = it.pinInput + digit, errorMessage = null) }
            if (_state.value.pinInput.length == 4) {
                viewModelScope.launch {
                    delay(200)
                    when (_state.value.currentStep) {
                        SetupPinStep.VERIFY_CURRENT -> {
                            val correct = lockManager.verifyPin(_state.value.pinInput)
                            if (correct) {
                                if (removeMode) {
                                    lockManager.clearPin()
                                    _state.update { it.copy(finished = true, pinInput = "") }
                                } else {
                                    _state.update { it.copy(currentStep = SetupPinStep.CHOOSE_PIN, pinInput = "") }
                                }
                            } else {
                                _state.update { it.copy(errorMessage = "Incorrect PIN. Try again.", pinInput = "") }
                            }
                        }
                        SetupPinStep.CHOOSE_PIN -> {
                            firstPin = _state.value.pinInput
                            _state.update { it.copy(currentStep = SetupPinStep.CONFIRM_PIN, pinInput = "") }
                        }
                        SetupPinStep.CONFIRM_PIN -> {
                            if (firstPin == _state.value.pinInput) {
                                lockManager.setPin(firstPin!!)
                                lockManager.setUnlocked(true)
                                _state.update { it.copy(finished = true, pinInput = "") }
                            } else {
                                _state.update { it.copy(errorMessage = "PINs do not match. Try again.", pinInput = "") }
                                firstPin = null // Reset for new attempt
                                delay(500) // Keep error visible for a bit
                                _state.update { it.copy(currentStep = SetupPinStep.CHOOSE_PIN, errorMessage = null) }
                            }
                        }
                    }
                }
            }
        }
    }

    fun onDelete() {
        if (_state.value.pinInput.isNotEmpty()) {
            _state.update { it.copy(pinInput = it.pinInput.dropLast(1), errorMessage = null) }
        }
    }

    enum class SetupPinStep {
        VERIFY_CURRENT, CHOOSE_PIN, CONFIRM_PIN
    }
}

data class SetupPinUiState(
    val currentStep: SetupPinViewModel.SetupPinStep = SetupPinViewModel.SetupPinStep.CHOOSE_PIN,
    val pinInput: String = "",
    val errorMessage: String? = null,
    val finished: Boolean = false,
    val removeMode: Boolean = false
)
