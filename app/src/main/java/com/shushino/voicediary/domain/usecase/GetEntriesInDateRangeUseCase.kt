package com.shushino.voicediary.domain.usecase

import com.shushino.voicediary.domain.repository.DiaryRepository
import javax.inject.Inject

class GetEntriesInDateRangeUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    operator fun invoke(start: Long, end: Long) = repository.getEntriesInDateRange(start, end)
}
