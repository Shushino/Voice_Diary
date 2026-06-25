package com.shushino.voicediary.data.manager

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioUploadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024 // 50 MB
        const val AUDIO_MIME_TYPE = "audio/*"
    }

    fun validateAndCopyAudioFile(uri: Uri): Result<Pair<String, Long>> {
        return runCatching {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)

            if (mimeType == null || !mimeType.startsWith("audio/")) {
                return Result.failure(IllegalArgumentException("Unsupported file format"))
            }

            val fileSize = getFileSize(uri)
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return Result.failure(IllegalArgumentException("File too large (max 50MB)"))
            }

            val voiceNotesDir = File(context.filesDir, "voicenotes")
            if (!voiceNotesDir.exists()) voiceNotesDir.mkdirs()

            val fileName = "upload_${System.currentTimeMillis()}.${getFileExtension(mimeType)}"
            val destinationFile = File(voiceNotesDir, fileName)

            val inputStream = contentResolver.openInputStream(uri) ?: throw IOException("Failed to open input stream")
            inputStream.use { input ->
                FileOutputStream(destinationFile).use { outputStream ->
                    input.copyTo(outputStream)
                }
            }

            val duration = getAudioDuration(destinationFile.absolutePath)

            destinationFile.absolutePath to duration
        }
    }

    private fun getFileSize(uri: Uri): Long {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex != -1) {
                cursor.getLong(sizeIndex)
            } else {
                0L
            }
        }
            ?: 0L
    }

    private fun getAudioDuration(filePath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    private fun getFileExtension(mimeType: String): String {
        return when (mimeType.lowercase()) {
            "audio/mpeg" -> "mp3"
            "audio/mp3" -> "mp3"
            "audio/aac" -> "m4a"
            "audio/mp4" -> "m4a"
            "audio/m4a" -> "m4a"
            "audio/mp4a-latm" -> "m4a"
            "audio/wav" -> "wav"
            "audio/x-wav" -> "wav"
            "audio/ogg" -> "ogg"
            "audio/flac" -> "flac"
            "audio/3gpp" -> "3gp"
            "audio/amr" -> "amr"
            "audio/x-m4a" -> "m4a"
            else -> "m4a"
        }
    }
}
