package com.shushino.voicediary.presentation.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shushino.voicediary.data.manager.LockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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

    // Brute-force protection: after 5 wrong PINs the pad locks for 30s, doubling with
    // every further failure up to 5 minutes. In-memory only — unlocking the app or
    // rebooting resets it, but the PBKDF2 cost (100k iterations per try) still applies.
    private var failedAttempts = 0
    private var lockoutUntilRealtime = 0L
    private var lockoutTicker: Job? = null

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
        if (_state.value.lockoutRemainingSec > 0) return
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
                failedAttempts = 0
                lockoutUntilRealtime = 0
                lockManager.setUnlocked(true)
                _state.update { it.copy(isUnlocked = true, errorMessage = null, isVerifying = false, lockoutRemainingSec = 0) }
            } else {
                failedAttempts++
                var message = "Incorrect PIN"
                if (failedAttempts >= FREE_TRIES) {
                    val lockoutSec = ((30_000L shl (failedAttempts - FREE_TRIES).coerceAtMost(4)) / 1000)
                        .toInt()
                        .coerceAtMost(MAX_LOCKOUT_SEC)
                    lockoutUntilRealtime = SystemClock.elapsedRealtime() + lockoutSec * 1000L
                    startLockoutTicker()
                    message = "Too many attempts"
                }
                _state.update { it.copy(pinInput = "", errorMessage = message, isVerifying = false) }
            }
        }
    }

    private fun startLockoutTicker() {
        lockoutTicker?.cancel()
        lockoutTicker = viewModelScope.launch {
            while (isActive) {
                val remaining = ((lockoutUntilRealtime - SystemClock.elapsedRealtime()) / 1000L).toInt()
                if (remaining <= 0) {
                    _state.update { it.copy(lockoutRemainingSec = 0, errorMessage = null) }
                    break
                }
                _state.update { it.copy(lockoutRemainingSec = remaining) }
                delay(250)
            }
        }
    }

    fun resetPinInput() {
        _state.update { it.copy(pinInput = "", errorMessage = null) }
    }

    companion object {
        private const val FREE_TRIES = 5
        private const val MAX_LOCKOUT_SEC = 5 * 60
    }
}

data class LockUiState(
    val isPinSet: Boolean = false,
    val isUnlocked: Boolean = false,
    val pinInput: String = "",
    val errorMessage: String? = null,
    val isBiometricAvailable: Boolean = false,
    val isVerifying: Boolean = false,
    /** Seconds until the PIN pad unlocks again after too many wrong tries (0 = active). */
    val lockoutRemainingSec: Int = 0
)
