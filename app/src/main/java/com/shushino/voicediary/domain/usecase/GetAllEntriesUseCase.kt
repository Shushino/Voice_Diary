package com.shushino.voicediary.domain.usecase

import com.shushino.voicediary.domain.repository.DiaryRepository
import javax.inject.Inject

class GetAllEntriesUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    operator fun invoke() = repository.getAllEntries()
}
