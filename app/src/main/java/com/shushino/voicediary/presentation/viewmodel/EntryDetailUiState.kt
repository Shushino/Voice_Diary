package com.shushino.voicediary.presentation.viewmodel

import com.shushino.voicediary.domain.model.DiaryEntry
import com.shushino.voicediary.domain.model.Photo
import com.shushino.voicediary.domain.model.VoiceNote

data class EntryDetailUiState(
    val entry: DiaryEntry? = null,
    val voiceNotes: List<VoiceNote> = emptyList(),
    val photos: List<Photo> = emptyList(),
    val isLoading: Boolean = false
)

sealed class EntryDetailEvent {
    object Deleted : EntryDetailEvent()
    data class Error(val message: String) : EntryDetailEvent()
}
