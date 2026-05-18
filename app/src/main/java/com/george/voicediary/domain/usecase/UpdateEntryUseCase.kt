package com.george.voicediary.domain.usecase

import com.george.voicediary.domain.model.DiaryEntry
import com.george.voicediary.domain.repository.DiaryRepository
import javax.inject.Inject

class UpdateEntryUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    suspend operator fun invoke(entry: DiaryEntry) = repository.updateEntry(entry)
}