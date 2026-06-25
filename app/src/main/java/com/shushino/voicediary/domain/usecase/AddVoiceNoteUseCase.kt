package com.shushino.voicediary.domain.usecase

import com.shushino.voicediary.domain.model.VoiceNote
import com.shushino.voicediary.domain.repository.DiaryRepository
import javax.inject.Inject

class AddVoiceNoteUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    suspend operator fun invoke(voiceNote: VoiceNote) = repository.addVoiceNote(voiceNote)
}
