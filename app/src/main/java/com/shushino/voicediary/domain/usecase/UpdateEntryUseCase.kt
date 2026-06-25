package com.shushino.voicediary.domain.usecase

import com.shushino.voicediary.domain.model.DiaryEntry
import com.shushino.voicediary.domain.repository.DiaryRepository
import javax.inject.Inject

class UpdateEntryUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    suspend operator fun invoke(entry: DiaryEntry) = repository.updateEntry(entry)
}
