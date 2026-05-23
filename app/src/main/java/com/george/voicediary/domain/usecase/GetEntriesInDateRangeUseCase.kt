package com.george.voicediary.domain.usecase

import com.george.voicediary.domain.repository.DiaryRepository
import javax.inject.Inject

class GetEntriesInDateRangeUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    operator fun invoke(start: Long, end: Long) = repository.getEntriesInDateRange(start, end)
}