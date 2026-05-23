package com.george.voicediary.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.george.voicediary.domain.model.DiaryEntry
import com.george.voicediary.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    val trashedEntries: StateFlow<List<DiaryEntry>> = diaryRepository.getTrashedEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun restoreEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            diaryRepository.restoreEntry(entry.id)
        }
    }

    fun deleteEntryPermanently(entry: DiaryEntry) {
        viewModelScope.launch {
            diaryRepository.hardDeleteEntry(entry.id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            diaryRepository.emptyTrash()
        }
    }
}
