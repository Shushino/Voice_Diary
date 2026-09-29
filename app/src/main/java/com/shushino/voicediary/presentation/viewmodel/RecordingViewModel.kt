package com.shushino.voicediary.presentation.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shushino.voicediary.data.manager.AudioRecorderManager
import com.shushino.voicediary.domain.model.VoiceNote
import com.shushino.voicediary.domain.repository.DiaryRepository
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
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecordingUiState())
    val state = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<RecordingEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var timerJob: Job? = null
    private var samplerJob: Job? = null
    private var currentOutputPath: String? = null

    // Real elapsed recording time (excluding pauses), immune to timer drift.
    private var accumulatedMs = 0L
    private var segmentStartRealtime = 0L
    private var autoStopTriggered = false

    private fun currentElapsedMs(): Long =
        accumulatedMs + if (segmentStartRealtime > 0) SystemClock.elapsedRealtime() - segmentStartRealtime else 0L

    fun start(outputPath: String) {
        currentOutputPath = outputPath
        accumulatedMs = 0L
        autoStopTriggered = false
        recorderManager.startRecording(outputPath)
        segmentStartRealtime = SystemClock.elapsedRealtime()
        _state.update {
            it.copy(
                isRecording = true,
                isPaused = false,
                elapsedMs = 0L,
                amplitudes = emptyList(),
                showWarning = false,
                isFinished = false
            )
        }
        startTimer()
        startSampler()
    }

    fun pause() {
        if (!_state.value.isRecording || _state.value.isPaused) return
        recorderManager.pauseRecording()
        accumulatedMs = currentElapsedMs()
        segmentStartRealtime = 0L
        _state.update { it.copy(isPaused = true, elapsedMs = accumulatedMs) }
        timerJob?.cancel()
        samplerJob?.cancel()
    }

    fun resume() {
        if (!_state.value.isRecording || !_state.value.isPaused) return
        recorderManager.resumeRecording()
        segmentStartRealtime = SystemClock.elapsedRealtime()
        _state.update { it.copy(isPaused = false) }
        startTimer()
        startSampler()
    }

    fun stopAndSave(entryId: Long) {
        // Idempotent: a second Stop tap (or a max-duration tick racing the user) must not
        // save twice or act while no recording is live.
        if (!_state.value.isRecording) return
        _state.update { it.copy(isRecording = false) }
        stopJobs()

        val path = currentOutputPath
        val duration = currentElapsedMs()
        val stopped = recorderManager.stopRecording()

        if (!stopped || path == null) {
            // No usable audio was captured (e.g. sub-second recording) — clean up the
            // empty file instead of saving a broken voice note.
            path?.let { File(it).delete() }
            _state.update { it.copy(isPaused = false, isFinished = true) }
            viewModelScope.launch { _eventFlow.emit(RecordingEvent.RecordingTooShort) }
            return
        }

        viewModelScope.launch {
            diaryRepository.addVoiceNote(
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
            _state.update { it.copy(isPaused = false, isFinished = true) }
            _eventFlow.emit(RecordingEvent.Saved)
        }
    }

    fun discard() {
        if (_state.value.isRecording) {
            recorderManager.stopRecording()
        }
        currentOutputPath?.let { File(it).delete() }
        stopJobs()
        _state.update { it.copy(isRecording = false, isPaused = false, isFinished = true) }
    }

    fun reset() {
        stopJobs()
        _state.update { RecordingUiState() }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(500)
                val elapsed = currentElapsedMs()
                if (elapsed >= maxDurationMs && !autoStopTriggered) {
                    autoStopTriggered = true
                    stopAndSaveAutomatic()
                    return@launch
                }
                _state.update {
                    it.copy(
                        elapsedMs = elapsed,
                        showWarning = elapsed >= warnThresholdMs && elapsed < maxDurationMs
                    )
                }
            }
        }
    }

    private fun stopAndSaveAutomatic() {
        // The sheet listens for this and calls stopAndSave(entryId), which is now idempotent.
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

    companion object {
        private const val warnThresholdMs = 9 * 60 * 1000L
        private const val maxDurationMs = 10 * 60 * 1000L
    }

    sealed class RecordingEvent {
        object Saved : RecordingEvent()
        object MaxDurationReached : RecordingEvent()

        /** Recording stopped before any audio was captured; nothing was saved. */
        object RecordingTooShort : RecordingEvent()
    }
}
