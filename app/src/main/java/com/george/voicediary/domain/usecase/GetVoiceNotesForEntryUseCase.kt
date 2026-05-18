package com.george.voicediary.domain.usecase

import com.george.voicediary.domain.repository.DiaryRepository
import javax.inject.Inject

class GetVoiceNotesForEntryUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    operator fun invoke(entryId: Long) = repository.getVoiceNotesForEntry(entryId)
}