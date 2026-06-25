package com.shushino.voicediary.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shushino.voicediary.domain.usecase.GetEntryByIdUseCase
import com.shushino.voicediary.data.manager.AudioPlayerManager
import com.shushino.voicediary.data.manager.SpeechTranscriptManager
import com.shushino.voicediary.data.local.dao.VoiceNoteDao
import com.shushino.voicediary.domain.model.VoiceNote
import com.shushino.voicediary.domain.usecase.DeleteVoiceNoteUseCase
import com.shushino.voicediary.domain.usecase.GetPhotosForEntryUseCase
import com.shushino.voicediary.domain.usecase.GetVoiceNotesForEntryUseCase
import com.shushino.voicediary.domain.usecase.SoftDeleteEntryUseCase
import com.shushino.voicediary.domain.usecase.UpdateVoiceNoteLabelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    private val getEntryByIdUseCase: GetEntryByIdUseCase,
    private val getVoiceNotesForEntryUseCase: GetVoiceNotesForEntryUseCase,
    private val getPhotosForEntryUseCase: GetPhotosForEntryUseCase,
    private val softDeleteEntryUseCase: SoftDeleteEntryUseCase,
    private val deleteVoiceNoteUseCase: DeleteVoiceNoteUseCase,
    private val updateVoiceNoteLabelUseCase: UpdateVoiceNoteLabelUseCase,
    private val speechTranscriptManager: SpeechTranscriptManager,
    private val voiceNoteDao: VoiceNoteDao,
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
                getVoiceNotesForEntryUseCase(id),
                getPhotosForEntryUseCase(id)
            ) { entry, voiceNotes, photos ->
                _state.update { 
                    it.copy(
                        entry = entry,
                        voiceNotes = voiceNotes,
                        photos = photos,
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
        viewModelScope.launch {
            updateVoiceNoteLabelUseCase(id, label)
        }
    }

    private val _transcribingVoiceNotes = MutableStateFlow<Set<Long>>(emptySet())
    val transcribingVoiceNotes: StateFlow<Set<Long>> = _transcribingVoiceNotes.asStateFlow()

    fun transcribeVoiceNote(voiceNote: VoiceNote) {
        _transcribingVoiceNotes.update { it + voiceNote.id }
        speechTranscriptManager.transcribeFile(
            filePath = voiceNote.filePath,
            onSuccess = { transcript ->
                viewModelScope.launch {
                    voiceNoteDao.updateTranscript(voiceNote.id, transcript)
                    _transcribingVoiceNotes.update { it - voiceNote.id }
                }
            },
            onError = { error ->
                viewModelScope.launch {
                    _eventFlow.emit(EntryDetailEvent.Error(error))
                    _transcribingVoiceNotes.update { it - voiceNote.id }
                }
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        speechTranscriptManager.release()
    }
}
