package com.shushino.voicediary.di

import com.shushino.voicediary.data.repository.DiaryRepositoryImpl
import com.shushino.voicediary.domain.repository.DiaryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindDiaryRepository(diaryRepositoryImpl: DiaryRepositoryImpl): DiaryRepository
}
