package com.george.voicediary.domain.usecase

import com.george.voicediary.domain.model.VoiceNote
import com.george.voicediary.domain.repository.DiaryRepository
import javax.inject.Inject

class AddVoiceNoteUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    suspend operator fun invoke(voiceNote: VoiceNote) = repository.addVoiceNote(voiceNote)
}