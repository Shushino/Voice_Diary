package com.george.voicediary.presentation.viewmodel

import com.george.voicediary.domain.model.DiaryEntry
import com.george.voicediary.domain.model.Mood

data class HomeUiState(
    val entries: List<DiaryEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedMoodFilter: Mood? = null,
    val isLoading: Boolean = false
)