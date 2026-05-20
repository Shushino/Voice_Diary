package com.george.voicediary.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.george.voicediary.domain.usecase.GetEntryByIdUseCase
import com.george.voicediary.data.manager.AudioPlayerManager
import com.george.voicediary.domain.usecase.DeleteVoiceNoteUseCase
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
    private val deleteVoiceNoteUseCase: DeleteVoiceNoteUseCase,
    val audioPlayerManager: AudioPlayerManager,
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

    fun softDeleteVoiceNote(id: Long) {
        viewModelScope.launch {
            audioPlayerManager.release() // Stop any playing audio
            deleteVoiceNoteUseCase(id)
        }
    }

    fun updateVoiceNoteLabel(id: Long, label: String) {
        // TODO: Implement update voice note label use case
    }
}