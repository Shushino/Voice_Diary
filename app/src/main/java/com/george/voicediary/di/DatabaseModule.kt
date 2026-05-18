package com.george.voicediary.di

import android.app.Application
import androidx.room.Room
import com.george.voicediary.data.local.database.DiaryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDiaryDatabase(app: Application): DiaryDatabase {
        val passphrase = "diary_key_george".toByteArray()
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
}