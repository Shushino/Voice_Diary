package com.george.voicediary.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.george.voicediary.data.manager.DraftManager
import com.george.voicediary.data.manager.EntryDraft
import com.george.voicediary.domain.model.DiaryEntry
import com.george.voicediary.domain.model.Mood
import com.george.voicediary.domain.usecase.CreateEntryUseCase
import com.george.voicediary.domain.usecase.GetEntryByIdUseCase
import com.george.voicediary.domain.usecase.UpdateEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateEditViewModel @Inject constructor(
    private val createEntryUseCase: CreateEntryUseCase,
    private val updateEntryUseCase: UpdateEntryUseCase,
    private val getEntryByIdUseCase: GetEntryByIdUseCase,
    private val draftManager: DraftManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(CreateEditUiState())
    val state: StateFlow<CreateEditUiState> = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<CreateEditEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var autoSaveJob: Job? = null
    private val entryId: Long? = savedStateHandle.get<Long>("entryId")

    init {
        _state.update { it.copy(entryId = entryId) }
        if (entryId != null && entryId != -1L) {
            loadEntry(entryId)
        } else {
            checkDraft()
            startAutoSave()
        }
    }

    private fun loadEntry(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getEntryByIdUseCase(id).firstOrNull()?.let { entry ->
                _state.update { it.copy(
                    title = entry.title ?: "",
                    body = entry.body,
                    selectedMood = entry.mood,
                    tags = entry.tags,
                    wordCount = countWords(entry.body),
                    isLoading = false
                ) }
            }
        }
    }

    private fun checkDraft() {
        viewModelScope.launch {
            draftManager.draftFlow.firstOrNull()?.let { draft ->
                _state.update { it.copy(
                    title = draft.title,
                    body = draft.body,
                    selectedMood = try { Mood.valueOf(draft.mood) } catch (e: Exception) { Mood.NEUTRAL },
                    tags = draft.tags,
                    wordCount = countWords(draft.body),
                    showDraftRestoredBanner = true
                ) }
            }
        }
    }

    private fun startAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(10000) // 10 seconds
                val currentState = _state.value
                if (currentState.title.isNotBlank() || currentState.body.isNotBlank()) {
                    draftManager.saveDraft(
                        EntryDraft(
                            title = currentState.title,
                            body = currentState.body,
                            mood = currentState.selectedMood.name,
                            tags = currentState.tags
                        )
                    )
                }
            }
        }
    }

    fun onTitleChange(title: String) {
        _state.update { it.copy(title = title) }
    }

    fun onBodyChange(body: String) {
        _state.update { it.copy(body = body, wordCount = countWords(body)) }
    }

    fun setMood(mood: Mood) {
        _state.update { it.copy(selectedMood = mood) }
    }

    fun addTag(name: String) {
        if (name.isNotBlank() && !state.value.tags.contains(name)) {
            _state.update { it.copy(tags = it.tags + name) }
        }
    }

    fun removeTag(name: String) {
        _state.update { it.copy(tags = it.tags - name) }
    }

    fun dismissBanner() {
        _state.update { it.copy(showDraftRestoredBanner = false) }
    }

    fun saveEntry() {
        viewModelScope.launch {
            val currentState = _state.value
            _state.update { it.copy(isSaving = true) }
            
            val entry = DiaryEntry(
                id = currentState.entryId ?: 0L,
                title = currentState.title,
                body = currentState.body,
                mood = currentState.selectedMood,
                tags = currentState.tags,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                deletedAt = null
            )

            if (currentState.entryId != null && currentState.entryId != -1L) {
                updateEntryUseCase(entry)
            } else {
                createEntryUseCase(entry)
                draftManager.clearDraft()
            }

            _eventFlow.emit(CreateEditEvent.Saved)
        }
    }

    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split("\\s+".toRegex()).size
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
        super.onCleared()
    }
}