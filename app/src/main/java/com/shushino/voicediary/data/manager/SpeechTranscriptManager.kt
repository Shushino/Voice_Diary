package com.shushino.voicediary.data.manager

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechTranscriptManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Stub implementation for speech-to-text.
     * In a real app, this would use SpeechRecognizer or a cloud API.
     */
    fun transcribeFile(
        filePath: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // Mock transcription process
        scope.launch {
            delay(2000) // Simulate processing
            // For now, we'll just return a placeholder or fail
            onError("On-device transcription is currently a stub. Consider using a cloud API for production.")
        }
    }

    fun release() {
        scope.cancel()
    }
}
