package com.george.voicediary.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.george.voicediary.domain.model.Mood
import com.george.voicediary.domain.usecase.GetAllEntriesUseCase
import com.george.voicediary.domain.usecase.SearchEntriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllEntriesUseCase: GetAllEntriesUseCase,
    private val searchEntriesUseCase: SearchEntriesUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedMood = MutableStateFlow<Mood?>(null)
    private val _isLoading = MutableStateFlow(false)

    val state = combine(
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
        _isLoading
    ) { entries, query, mood, loading ->
        val filteredEntries = if (mood != null) {
            entries.filter { it.mood == mood }
        } else {
            entries
        }
        HomeUiState(
            entries = filteredEntries,
            searchQuery = query,
            selectedMoodFilter = mood,
            isLoading = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun filterByMood(mood: Mood?) {
        _selectedMood.value = mood
    }
}