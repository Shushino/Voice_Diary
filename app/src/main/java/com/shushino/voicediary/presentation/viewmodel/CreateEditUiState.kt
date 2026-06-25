package com.shushino.voicediary.presentation.viewmodel

import com.shushino.voicediary.domain.model.Mood

data class CreateEditUiState(
    val title: String = "",
    val body: String = "",
    val selectedMood: Mood = Mood.NEUTRAL,
    val tags: List<String> = emptyList(),
    val wordCount: Int = 0,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val entryId: Long? = null,
    val originalCreatedAt: Long? = null,
    val showDraftRestoredBanner: Boolean = false
)

sealed class CreateEditEvent {
    object Saved : CreateEditEvent()
}
