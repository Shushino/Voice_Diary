package com.shushino.voicediary.presentation.viewmodel

import com.shushino.voicediary.domain.model.DiaryEntry
import com.shushino.voicediary.domain.model.Mood
import com.shushino.voicediary.domain.model.WritingStats
import java.time.LocalDate
import java.time.YearMonth

data class HomeUiState(
    val entries: List<DiaryEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedMoodFilter: Mood? = null,
    val hasVoiceNoteFilter: Boolean = false,
    val isLoading: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST,
    val selectedDate: LocalDate? = null,
    val entriesOnSelectedDate: List<DiaryEntry> = emptyList(),
    val monthEntryDates: Set<LocalDate> = emptySet(),
    val currentMonth: YearMonth = YearMonth.now(),
    val writingStats: WritingStats? = null,
    val statsExpanded: Boolean = true
)
