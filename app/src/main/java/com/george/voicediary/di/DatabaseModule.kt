package com.george.voicediary.di

import android.app.Application
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.room.Room
import com.george.voicediary.data.local.database.DiaryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DATABASE_KEY_ALIAS = "voicediary_db_key"
    private const val DATABASE_KEY_PREFS = "voicediary_db_key_prefs"
    private const val ENCRYPTED_KEY = "encrypted_database_key"
    private const val KEY_IV = "database_key_iv"

    @Provides
    @Singleton
    fun provideDiaryDatabase(app: Application): DiaryDatabase {
        val passphrase = getOrCreateDatabaseKey(app)
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            app,
            DiaryDatabase::class.java,
            "diary_db"
        )
            .openHelperFactory(factory)
            .build()
    }

    @Provides
    @Singleton
    fun provideEntryDao(db: DiaryDatabase) = db.entryDao()

    @Provides
    @Singleton
    fun provideVoiceNoteDao(db: DiaryDatabase) = db.voiceNoteDao()

    @Provides
    @Singleton
    fun providePhotoDao(db: DiaryDatabase) = db.photoDao()

    @Provides
    @Singleton
    fun provideTagDao(db: DiaryDatabase) = db.tagDao()

    private fun getOrCreateDatabaseKey(app: Application): ByteArray {
        val prefs = app.getSharedPreferences(DATABASE_KEY_PREFS, Context.MODE_PRIVATE)
        val encryptedKey = prefs.getString(ENCRYPTED_KEY, null)
        val iv = prefs.getString(KEY_IV, null)

        if (encryptedKey != null && iv != null) {
            return decryptDatabaseKey(encryptedKey, iv)
        }

        val databaseKey = ByteArray(32)
        SecureRandom().nextBytes(databaseKey)
        val (storedEncryptedKey, storedIv) = encryptDatabaseKey(databaseKey)

        prefs.edit()
            .putString(ENCRYPTED_KEY, storedEncryptedKey)
            .putString(KEY_IV, storedIv)
            .apply()

        return databaseKey
    }

    private fun encryptDatabaseKey(databaseKey: ByteArray): Pair<String, String> {
        val secretKey = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedKey = cipher.doFinal(databaseKey)
        return Pair(
            Base64.encodeToString(encryptedKey, Base64.NO_WRAP),
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        )
    }

    private fun decryptDatabaseKey(encryptedKey: String, iv: String): ByteArray {
        val secretKey = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
        )
        return cipher.doFinal(Base64.decode(encryptedKey, Base64.NO_WRAP))
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val existingKey = keyStore.getKey(DATABASE_KEY_ALIAS, null)
        if (existingKey is SecretKey) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                DATABASE_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }
}
