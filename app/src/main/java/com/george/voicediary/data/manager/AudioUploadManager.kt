package com.george.voicediary.data.manager

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

            val inputStream = contentResolver.openInputStream(uri) ?: throw IOException("Failed to open input stream")
            val voiceNotesDir = File(context.filesDir, "voicenotes")
            if (!voiceNotesDir.exists()) voiceNotesDir.mkdirs()

            val fileName = "upload_${System.currentTimeMillis()}.${getFileExtension(mimeType)}"
            val destinationFile = File(voiceNotesDir, fileName)

            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()

            val duration = getAudioDuration(destinationFile.absolutePath)

            destinationFile.absolutePath to duration
        }
    }

    private fun getFileSize(uri: Uri): Long {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)
        val fileSize = if (cursor != null && cursor.moveToFirst() && sizeIndex != null) {
            cursor.getLong(sizeIndex)
        } else {
            0L
        }
        cursor?.close()
        return fileSize
    }

    private fun getAudioDuration(filePath: String): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val duration = durationStr?.toLongOrNull() ?: 0L
        retriever.release()
        return duration
    }

    private fun getFileExtension(mimeType: String): String {
        return when (mimeType) {
            "audio/mpeg" -> "mp3"
            "audio/aac" -> "aac"
            "audio/wav" -> "wav"
            "audio/ogg" -> "ogg"
            "audio/x-m4a" -> "m4a"
            else -> "tmp"
        }
    }
}