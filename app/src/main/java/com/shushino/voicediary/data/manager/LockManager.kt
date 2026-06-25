package com.shushino.voicediary.data.manager

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.biometric.BiometricManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.lockDataStore by preferencesDataStore(name = "lock_preferences")

@Singleton
class LockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val PIN_KEY = stringPreferencesKey("app_pin")
    private val PIN_SALT_KEY = stringPreferencesKey("pin_salt")
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    val isPinSetFlow: Flow<Boolean> = context.lockDataStore.data.map { preferences ->
        preferences[PIN_KEY] != null
    }

    suspend fun setPin(rawPin: String) {
        val salt = generateSalt()
        val hashedPin = hashPin(rawPin, salt)
        context.lockDataStore.edit { preferences ->
            preferences[PIN_KEY] = hashedPin
            preferences[PIN_SALT_KEY] = salt
        }
    }

    suspend fun verifyPin(rawPin: String): Boolean {
        val preferences = context.lockDataStore.data.first()
        val storedPin = preferences[PIN_KEY] ?: return false
        val storedSalt = preferences[PIN_SALT_KEY] ?: return false
        return storedPin == hashPin(rawPin, storedSalt)
    }

    suspend fun isPinSet(): Boolean {
        return context.lockDataStore.data.first()[PIN_KEY] != null
    }

    suspend fun clearPin() {
        context.lockDataStore.edit { preferences ->
            preferences.remove(PIN_KEY)
            preferences.remove(PIN_SALT_KEY)
        }
    }

    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // This will be called by the AppLifecycleObserver
    fun setUnlocked(unlocked: Boolean) {
        _isUnlocked.value = unlocked
    }

    private fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    private suspend fun hashPin(pin: String, salt: String): String = withContext(Dispatchers.Default) {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, 100_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        Base64.encodeToString(hash, Base64.NO_WRAP)
    }

}
