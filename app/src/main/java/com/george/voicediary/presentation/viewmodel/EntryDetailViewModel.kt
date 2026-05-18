package com.george.voicediary.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.george.voicediary.domain.usecase.GetEntryByIdUseCase
import com.george.voicediary.domain.usecase.GetVoiceNotesForEntryUseCase
import com.george.voicediary.domain.usecase.SoftDeleteEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    private val getEntryByIdUseCase: GetEntryByIdUseCase,
    private val getVoiceNotesForEntryUseCase: GetVoiceNotesForEntryUseCase,
    private val softDeleteEntryUseCase: SoftDeleteEntryUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val entryId: Long? = savedStateHandle.get<Long>("entryId")

    private val _state = MutableStateFlow(EntryDetailUiState())
    val state: StateFlow<EntryDetailUiState> = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EntryDetailEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        entryId?.let { loadEntry(it) }
    }

    private fun loadEntry(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            combine(
                getEntryByIdUseCase(id),
                getVoiceNotesForEntryUseCase(id)
            ) { entry, voiceNotes ->
                _state.update { 
                    it.copy(
                        entry = entry,
                        voiceNotes = voiceNotes,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun deleteEntry() {
        entryId?.let { id ->
            viewModelScope.launch {
                softDeleteEntryUseCase(id)
                _eventFlow.emit(EntryDetailEvent.Deleted)
            }
        }
    }
}