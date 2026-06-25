package com.shushino.voicediary.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shushino.voicediary.data.manager.DraftManager
import com.shushino.voicediary.data.manager.EntryDraft
import com.shushino.voicediary.domain.model.DiaryEntry
import com.shushino.voicediary.domain.model.Mood
import com.shushino.voicediary.domain.model.Photo
import com.shushino.voicediary.domain.model.VoiceNote
import com.shushino.voicediary.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CreateEditViewModel @Inject constructor(
    private val createEntryUseCase: CreateEntryUseCase,
    private val updateEntryUseCase: UpdateEntryUseCase,
    private val getEntryByIdUseCase: GetEntryByIdUseCase,
    private val addVoiceNoteUseCase: AddVoiceNoteUseCase,
    private val addPhotoUseCase: AddPhotoUseCase,
    private val getPhotosForEntryUseCase: GetPhotosForEntryUseCase,
    private val deletePhotoUseCase: DeletePhotoUseCase,
    private val draftManager: DraftManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(CreateEditUiState())
    val state: StateFlow<CreateEditUiState> = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<CreateEditEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val photosForEntry: StateFlow<List<Photo>> = state
        .map { it.entryId }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id != null && id != -1L) {
                getPhotosForEntryUseCase(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var autoSaveJob: Job? = null
    private val entryId: Long? = savedStateHandle.get<Long>("entryId").takeIf { it != -1L }

    init {
        _state.update { it.copy(entryId = entryId) }
        if (entryId != null) {
            loadEntry(entryId)
            // Skip draft restore for existing entries
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
                    originalCreatedAt = entry.createdAt,
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

    fun addVoiceNote(voiceNote: VoiceNote) {
        viewModelScope.launch {
            addVoiceNoteUseCase(voiceNote)
        }
    }

    fun addPhoto(entryId: Long, filePath: String) {
        viewModelScope.launch {
            addPhotoUseCase(
                Photo(
                    entryId = entryId,
                    filePath = filePath,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deletePhoto(id: Long) {
        viewModelScope.launch {
            deletePhotoUseCase(id)
        }
    }

    fun copyImageToInternal(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val photosDir = File(context.filesDir, "photos")
            if (!photosDir.exists()) photosDir.mkdirs()

            val fileName = "photo_${System.currentTimeMillis()}.jpg"
            val destinationFile = File(photosDir, fileName)

            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun saveEntry(autoSave: Boolean = false) {
        viewModelScope.launch {
            val currentState = _state.value
            _state.update { it.copy(isSaving = true) }
            val now = System.currentTimeMillis()
            val createdAt = if (currentState.entryId != null && currentState.entryId != -1L) {
                currentState.originalCreatedAt ?: now
            } else {
                now
            }
            
            val entry = DiaryEntry(
                id = currentState.entryId ?: 0L,
                title = currentState.title,
                body = currentState.body,
                mood = currentState.selectedMood,
                tags = currentState.tags,
                createdAt = createdAt,
                updatedAt = now,
                deletedAt = null
            )

            if (currentState.entryId != null && currentState.entryId != -1L) {
                updateEntryUseCase(entry)
            } else {
                val newEntryId = createEntryUseCase(entry)
                _state.update { it.copy(entryId = newEntryId, originalCreatedAt = createdAt) }
            }

            if (!autoSave) {
                draftManager.clearDraft()
                _eventFlow.emit(CreateEditEvent.Saved)
            } else {
                _state.update { it.copy(isSaving = false) }
            }
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
