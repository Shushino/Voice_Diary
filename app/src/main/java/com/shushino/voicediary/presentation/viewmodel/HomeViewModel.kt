package com.shushino.voicediary.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shushino.voicediary.domain.model.DiaryEntry
import com.shushino.voicediary.domain.model.Mood
import com.shushino.voicediary.domain.usecase.GetAllEntriesUseCase
import com.shushino.voicediary.domain.usecase.GetEntriesInDateRangeUseCase
import com.shushino.voicediary.domain.usecase.GetStatsUseCase
import com.shushino.voicediary.domain.usecase.SearchEntriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllEntriesUseCase: GetAllEntriesUseCase,
    private val searchEntriesUseCase: SearchEntriesUseCase,
    private val getEntriesInDateRangeUseCase: GetEntriesInDateRangeUseCase,
    private val getStatsUseCase: GetStatsUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedMood = MutableStateFlow<Mood?>(null)
    private val _hasVoiceNoteFilter = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _monthEntries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    private val _statsExpanded = MutableStateFlow(true)
    private var monthLoadJob: Job? = null

    val state = combine(
        listOf(
            _searchQuery
                .debounce(300)
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        getAllEntriesUseCase()
                    } else {
                        searchEntriesUseCase(query)
                    }
                },
            _searchQuery,
            _selectedMood,
            _hasVoiceNoteFilter,
            _isLoading,
            _viewMode,
            _selectedDate,
            _currentMonth,
            _monthEntries,
            getStatsUseCase(),
            _statsExpanded
        )
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val entries = array[0] as List<DiaryEntry>
        val query = array[1] as String
        val mood = array[2] as Mood?
        val hasVoiceNote = array[3] as Boolean
        val loading = array[4] as Boolean
        val viewMode = array[5] as ViewMode
        val selectedDate = array[6] as LocalDate?
        val currentMonth = array[7] as YearMonth
        @Suppress("UNCHECKED_CAST")
        val monthEntries = array[8] as List<DiaryEntry>
        val stats = array[9] as com.shushino.voicediary.domain.model.WritingStats
        val statsExpanded = array[10] as Boolean
        
        val filteredEntries = entries.filter { entry ->
            val moodMatch = mood == null || entry.mood == mood
            val voiceNoteMatch = !hasVoiceNote || entry.voiceNoteCount > 0
            moodMatch && voiceNoteMatch
        }

        val entriesOnSelectedDate = if (selectedDate != null) {
            monthEntries.filter { 
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(it.createdAt), 
                    ZoneId.systemDefault()
                ).toLocalDate() == selectedDate 
            }
        } else {
            emptyList()
        }

        val monthEntryDates = monthEntries.map {
            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(it.createdAt), 
                ZoneId.systemDefault()
            ).toLocalDate()
        }.toSet()

        HomeUiState(
            entries = filteredEntries,
            searchQuery = query,
            selectedMoodFilter = mood,
            hasVoiceNoteFilter = hasVoiceNote,
            isLoading = loading,
            viewMode = viewMode,
            selectedDate = selectedDate,
            entriesOnSelectedDate = entriesOnSelectedDate,
            monthEntryDates = monthEntryDates,
            currentMonth = currentMonth,
            writingStats = stats,
            statsExpanded = statsExpanded
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    init {
        loadEntriesForMonth(_currentMonth.value)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun filterByMood(mood: Mood?) {
        _selectedMood.value = mood
    }

    fun toggleHasVoiceNoteFilter() {
        _hasVoiceNoteFilter.value = !_hasVoiceNoteFilter.value
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun navigateMonth(offset: Int) {
        _currentMonth.value = _currentMonth.value.plusMonths(offset.toLong())
        loadEntriesForMonth(_currentMonth.value)
    }

    fun toggleStatsExpanded() {
        _statsExpanded.value = !_statsExpanded.value
    }

    fun loadEntriesForMonth(month: YearMonth) {
        monthLoadJob?.cancel()
        monthLoadJob = viewModelScope.launch {
            val start = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val end = month.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            getEntriesInDateRangeUseCase(start, end).collect { entries ->
                _monthEntries.value = entries
            }
        }
    }
}
