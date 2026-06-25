package com.shushino.voicediary.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shushino.voicediary.data.manager.AudioRecorderManager
import com.shushino.voicediary.domain.model.VoiceNote
import com.shushino.voicediary.domain.usecase.AddVoiceNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class RecordingUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0L,
    val amplitudes: List<Float> = emptyList(),
    val showWarning: Boolean = false,
    val isFinished: Boolean = false
)

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val recorderManager: AudioRecorderManager,
    private val addVoiceNoteUseCase: AddVoiceNoteUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RecordingUiState())
    val state = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<RecordingEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var timerJob: Job? = null
    private var samplerJob: Job? = null
    private var currentOutputPath: String? = null

    fun start(outputPath: String) {
        currentOutputPath = outputPath
        recorderManager.startRecording(outputPath)
        _state.update { it.copy(isRecording = true, isPaused = false, elapsedMs = 0L, amplitudes = emptyList(), showWarning = false, isFinished = false) }
        startTimer()
        startSampler()
    }

    fun pause() {
        recorderManager.pauseRecording()
        _state.update { it.copy(isPaused = true) }
        timerJob?.cancel()
        samplerJob?.cancel()
    }

    fun resume() {
        recorderManager.resumeRecording()
        _state.update { it.copy(isPaused = false) }
        startTimer()
        startSampler()
    }

    fun stopAndSave(entryId: Long) {
        recorderManager.stopRecording()
        val path = currentOutputPath ?: return
        val duration = _state.value.elapsedMs

        viewModelScope.launch {
            addVoiceNoteUseCase(
                VoiceNote(
                    entryId = entryId,
                    filePath = path,
                    durationMs = duration,
                    createdAt = System.currentTimeMillis(),
                    label = null,
                    transcript = null,
                    deletedAt = null
                )
            )
            _state.update { it.copy(isRecording = false, isFinished = true) }
            _eventFlow.emit(RecordingEvent.Saved)
        }
        stopJobs()
    }

    fun discard() {
        recorderManager.stopRecording()
        currentOutputPath?.let { File(it).delete() }
        _state.update { it.copy(isRecording = false, isFinished = true) }
        stopJobs()
    }

    fun reset() {
        stopJobs()
        _state.update { RecordingUiState() }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { 
                    val newElapsed = it.elapsedMs + 1000
                    val showWarning = newElapsed >= 9 * 60 * 1000 // 9 minutes
                    
                    if (newElapsed >= 10 * 60 * 1000) { // 10 minutes limit
                        stopAndSaveAutomatic()
                        it.copy(elapsedMs = newElapsed, showWarning = showWarning)
                    } else {
                        it.copy(elapsedMs = newElapsed, showWarning = showWarning)
                    }
                }
            }
        }
    }

    private fun stopAndSaveAutomatic() {
        // This is a bit tricky since we don't have entryId here.
        // In a real app, we might save as orphaned or rely on the UI to call stopAndSave.
        // For now, we'll just emit an event or let the UI handle the max duration.
        viewModelScope.launch {
            _eventFlow.emit(RecordingEvent.MaxDurationReached)
        }
    }

    private fun startSampler() {
        samplerJob?.cancel()
        samplerJob = viewModelScope.launch {
            while (true) {
                delay(100)
                val amplitude = recorderManager.getAmplitude().toFloat()
                _state.update { 
                    val newAmplitudes = (it.amplitudes + amplitude).takeLast(50)
                    it.copy(amplitudes = newAmplitudes)
                }
            }
        }
    }

    private fun stopJobs() {
        timerJob?.cancel()
        samplerJob?.cancel()
    }

    override fun onCleared() {
        stopJobs()
        if (_state.value.isRecording) {
            recorderManager.stopRecording()
            currentOutputPath?.let { File(it).delete() } // Discard incomplete recording
        }
        super.onCleared()
    }

    sealed class RecordingEvent {
        object Saved : RecordingEvent()
        object MaxDurationReached : RecordingEvent()
    }
}
