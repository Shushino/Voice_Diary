package com.shushino.voicediary.di

import android.content.Context
import com.shushino.voicediary.data.manager.AudioPlayerManager
import com.shushino.voicediary.data.manager.AudioUploadManager
import com.shushino.voicediary.data.manager.SpeechTranscriptManager
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

    @Provides
    @Singleton
    fun provideSpeechTranscriptManager(@ApplicationContext context: Context): SpeechTranscriptManager {
        return SpeechTranscriptManager(context)
    }
}
