package com.george.voicediary.data.manager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.biometric.BiometricManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.security.MessageDigest

val Context.dataStore by preferencesDataStore(name = "app_preferences")

@Singleton
class LockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val PIN_KEY = stringPreferencesKey("app_pin")

    suspend fun setPin(rawPin: String) {
        val hashedPin = hashPin(rawPin)
        context.dataStore.edit { preferences ->
            preferences[PIN_KEY] = hashedPin
        }
    }

    suspend fun verifyPin(rawPin: String): Boolean {
        val storedPin = context.dataStore.data.first()[PIN_KEY]
        return storedPin == hashPin(rawPin)
    }

    suspend fun isPinSet(): Boolean {
        return context.dataStore.data.first()[PIN_KEY] != null
    }

    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    private fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

}