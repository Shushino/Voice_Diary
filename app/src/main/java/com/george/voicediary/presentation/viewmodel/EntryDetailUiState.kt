package com.george.voicediary.presentation.viewmodel

import com.george.voicediary.domain.model.DiaryEntry
import com.george.voicediary.domain.model.VoiceNote

data class EntryDetailUiState(
    val entry: DiaryEntry? = null,
    val voiceNotes: List<VoiceNote> = emptyList(),
    val isLoading: Boolean = false
)

sealed class EntryDetailEvent {
    object Deleted : EntryDetailEvent()
}