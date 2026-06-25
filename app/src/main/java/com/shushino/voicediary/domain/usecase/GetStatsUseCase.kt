package com.shushino.voicediary.domain.usecase

import com.shushino.voicediary.data.repository.StatsRepository
import com.shushino.voicediary.domain.model.WritingStats
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetStatsUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    operator fun invoke(): Flow<WritingStats> = statsRepository.getStats()
}
