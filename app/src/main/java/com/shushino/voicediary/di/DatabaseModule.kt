package com.shushino.voicediary.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.shushino.voicediary.data.local.dao.EntryDao
import com.shushino.voicediary.data.local.dao.PhotoDao
import com.shushino.voicediary.data.local.dao.VoiceNoteDao
import com.shushino.voicediary.data.local.database.DiaryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.inject.Singleton
import android.util.Base64

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_KEY_ALIAS = "diary_db_key"
    private const val DATABASE_KEY_PREFS = "db_key_prefs"
    private const val ENCRYPTED_KEY = "encrypted_key"
    private const val KEY_IV = "key_iv"

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
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideEntryDao(db: DiaryDatabase): EntryDao = db.entryDao()

    @Provides
    @Singleton
    fun provideVoiceNoteDao(db: DiaryDatabase): VoiceNoteDao = db.voiceNoteDao()

    @Provides
    @Singleton
    fun providePhotoDao(db: DiaryDatabase): PhotoDao = db.photoDao()

    private fun getOrCreateDatabaseKey(app: Application): ByteArray {
        val prefs = app.getSharedPreferences(DATABASE_KEY_PREFS, Context.MODE_PRIVATE)
        val encryptedKeyBase64 = prefs.getString(ENCRYPTED_KEY, null)
        val ivBase64 = prefs.getString(KEY_IV, null)

        if (encryptedKeyBase64 != null && ivBase64 != null) {
            return decryptDatabaseKey(encryptedKeyBase64, ivBase64)
        }

        // Generate new 256-bit key
        val key = ByteArray(32)
        java.security.SecureRandom().nextBytes(key)
        
        val (encryptedKey, iv) = encryptDatabaseKey(key)
        prefs.edit()
            .putString(ENCRYPTED_KEY, encryptedKey)
            .putString(KEY_IV, iv)
            .apply()

        return key
    }

    private fun encryptDatabaseKey(key: ByteArray): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey())
        val encryptedBytes = cipher.doFinal(key)
        val iv = cipher.iv
        
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT) to 
               Base64.encodeToString(iv, Base64.DEFAULT)
    }

    private fun decryptDatabaseKey(encryptedKeyBase64: String, ivBase64: String): ByteArray {
        val encryptedBytes = Base64.decode(encryptedKeyBase64, Base64.DEFAULT)
        val iv = Base64.decode(ivBase64, Base64.DEFAULT)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKeystoreKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encryptedBytes)
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        
        if (!keyStore.containsAlias(DATABASE_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            keyGenerator.init(
                android.security.keystore.KeyGenParameterSpec.Builder(
                    DATABASE_KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or 
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            )
            keyGenerator.generateKey()
        }
        
        return (keyStore.getEntry(DATABASE_KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }
}
