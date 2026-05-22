package com.george.voicediary.data.manager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.biometric.BiometricManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.security.MessageDigest

private val Context.lockDataStore by preferencesDataStore(name = "lock_preferences")

@Singleton
class LockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val PIN_KEY = stringPreferencesKey("app_pin")
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    suspend fun setPin(rawPin: String) {
        val hashedPin = hashPin(rawPin)
        context.lockDataStore.edit { preferences ->
            preferences[PIN_KEY] = hashedPin
        }
    }

    suspend fun verifyPin(rawPin: String): Boolean {
        val storedPin = context.lockDataStore.data.first()[PIN_KEY]
        return storedPin == hashPin(rawPin)
    }

    suspend fun isPinSet(): Boolean {
        return context.lockDataStore.data.first()[PIN_KEY] != null
    }

    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // This will be called by the AppLifecycleObserver
    fun setUnlocked(unlocked: Boolean) {
        _isUnlocked.value = unlocked
    }

    private fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

}