package com.shushino.voicediary.data.manager

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recorder: MediaRecorder? = null

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    fun startRecording(outputPath: String) {
        val file = File(outputPath)
        file.parentFile?.mkdirs()

        recorder = createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputPath)

            prepare()
            start()
        }
    }

    fun pauseRecording() {
        recorder?.pause()
    }

    fun resumeRecording() {
        recorder?.resume()
    }

    /**
     * Stops and releases the recorder.
     * @return true if audio data was written to the output file. MediaRecorder.stop()
     * throws IllegalStateException when stopped too early to have produced any frames
     * (e.g. a sub-second recording), which used to crash the app — treat it as a
     * discarded recording instead. Always releases the recorder.
     */
    fun stopRecording(): Boolean {
        val current = recorder ?: return false
        val stopped = try {
            current.stop()
            true
        } catch (_: Exception) {
            false
        } finally {
            try {
                current.reset()
            } catch (_: Exception) {
            }
            try {
                current.release()
            } catch (_: Exception) {
            }
            recorder = null
        }
        return stopped
    }

    fun getAmplitude(): Int {
        return recorder?.maxAmplitude ?: 0
    }
}
