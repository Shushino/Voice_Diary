package com.george.voicediary.di

import android.content.Context
import com.george.voicediary.data.manager.AudioPlayerManager
import com.george.voicediary.data.manager.AudioUploadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ManagerModule {

    @Provides
    @Singleton
    fun provideAudioPlayerManager(@ApplicationContext context: Context): AudioPlayerManager {
        return AudioPlayerManager(context)
    }

    @Provides
    @Singleton
    fun provideAudioUploadManager(@ApplicationContext context: Context): AudioUploadManager {
        return AudioUploadManager(context)
    }
}