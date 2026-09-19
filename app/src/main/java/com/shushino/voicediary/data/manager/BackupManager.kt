package com.shushino.voicediary.data.manager

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shushino.voicediary.data.model.EntryExportDto
import com.shushino.voicediary.data.model.PhotoExportDto
import com.shushino.voicediary.data.model.VoiceNoteExportDto
import com.shushino.voicediary.domain.model.DiaryEntry
import com.shushino.voicediary.domain.model.Mood
import com.shushino.voicediary.domain.model.Photo
import com.shushino.voicediary.domain.model.VoiceNote
import com.shushino.voicediary.domain.repository.DiaryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates and restores `.vdiary` zip backups (manifest.json + audio/photos).
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diaryRepository: DiaryRepository
) {

    private val gson = Gson()

    /**
     * Exports all active entries to Downloads as a `.vdiary` zip.
     * @return number of entries exported
     */
    suspend fun exportAllEntries(
        includeAudio: Boolean = true,
        includeImages: Boolean = true
    ): Result<Int> = runCatching {
        val entries = diaryRepository.getAllActiveEntriesSync()
        if (entries.isEmpty()) {
            error("No entries to export")
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "voicediary_backup_$timestamp.vdiary"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: error("Failed to create export file")
            resolver.openOutputStream(uri)?.use { os ->
                ZipOutputStream(os).use { zos ->
                    writeEntriesToZip(entries, zos, includeAudio, includeImages)
                }
            } ?: error("Failed to open export stream")
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    writeEntriesToZip(entries, zos, includeAudio, includeImages)
                }
            }
        }
        entries.size
    }

    /**
     * Imports a `.vdiary` backup from a content [uri].
     */
    suspend fun importBackup(uri: Uri): Result<Unit> = runCatching {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: error("Failed to open file")
        importFromStream(inputStream)
    }

    /**
     * Stream-based import for unit tests and content-resolver callers.
     * @param filesRoot app files directory (defaults to [Context.getFilesDir])
     */
    suspend fun importFromStream(
        inputStream: InputStream,
        filesRoot: File = context.filesDir
    ) {
        ZipInputStream(inputStream).use { zipInputStream ->
            var zipEntry: ZipEntry? = zipInputStream.nextEntry
            if (zipEntry == null || zipEntry.name != "manifest.json") {
                error("Invalid backup: manifest.json must be the first entry")
            }

            val manifestJson = zipInputStream.readBytes().decodeToString()
            zipInputStream.closeEntry()

            val entries: List<EntryExportDto> = gson.fromJson(
                manifestJson,
                object : TypeToken<List<EntryExportDto>>() {}.type
            )

            val entryMap = mutableMapOf<Long, Long>()
            entries.forEach { dto ->
                val newEntryId = diaryRepository.createEntry(
                    DiaryEntry(
                        title = dto.title,
                        body = dto.body,
                        mood = Mood.valueOf(dto.mood),
                        tags = dto.tags,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        deletedAt = null
                    )
                )
                entryMap[dto.id] = newEntryId
            }

            zipEntry = zipInputStream.nextEntry
            while (zipEntry != null) {
                val entryName = zipEntry.name
                if (entryName.startsWith("audio/") || entryName.startsWith("photos/")) {
                    val isAudio = entryName.startsWith("audio/")
                    val parts = entryName.split('/')
                    if (parts.size == 2) {
                        val fileName = parts[1]
                        val oldEntryId = fileName.substringBefore('_').toLongOrNull()
                        val originalName = fileName.substringAfter('_')

                        val newEntryId = entryMap[oldEntryId]
                        if (newEntryId != null) {
                            val subDir = if (isAudio) "voicenotes" else "photos"
                            val destDir = File(filesRoot, subDir)
                            if (!destDir.exists()) destDir.mkdirs()

                            val destFile = File(
                                destDir,
                                buildImportedFileName(if (isAudio) "audio" else "photo", originalName)
                            )

                            FileOutputStream(destFile).use { fos ->
                                BufferedOutputStream(fos).use { bos ->
                                    zipInputStream.copyTo(bos)
                                }
                            }

                            if (isAudio) {
                                val dto = entries.find { it.id == oldEntryId }
                                val vnDto = dto?.voiceNotes?.find { it.originalFilename == originalName }
                                if (vnDto != null) {
                                    diaryRepository.addVoiceNote(
                                        VoiceNote(
                                            entryId = newEntryId,
                                            filePath = destFile.absolutePath,
                                            durationMs = vnDto.durationMs,
                                            label = vnDto.label,
                                            transcript = vnDto.transcript,
                                            createdAt = System.currentTimeMillis(),
                                            deletedAt = null
                                        )
                                    )
                                }
                            } else {
                                diaryRepository.addPhoto(
                                    Photo(
                                        entryId = newEntryId,
                                        filePath = destFile.absolutePath,
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
                zipInputStream.closeEntry()
                zipEntry = zipInputStream.nextEntry
            }
        }
    }

    /**
     * Writes a backup zip to [outputStream]. Exposed for round-trip unit tests.
     */
    suspend fun exportToStream(
        entries: List<DiaryEntry>,
        outputStream: OutputStream,
        includeAudio: Boolean = true,
        includeImages: Boolean = true
    ) {
        ZipOutputStream(outputStream).use { zos ->
            writeEntriesToZip(entries, zos, includeAudio, includeImages)
        }
    }

    suspend fun writeEntriesToZip(
        entries: List<DiaryEntry>,
        zos: ZipOutputStream,
        includeAudio: Boolean,
        includeImages: Boolean
    ) {
        val exportDtos = mutableListOf<EntryExportDto>()
        val filesToInclude = mutableListOf<Pair<String, String>>()

        entries.forEach { entry ->
            val voiceNotes = if (includeAudio) {
                diaryRepository.getVoiceNotesForEntry(entry.id).first()
            } else emptyList()

            val photos = if (includeImages) {
                diaryRepository.getPhotosForEntry(entry.id).first()
            } else emptyList()

            val vnDtos = voiceNotes.map { vn ->
                val originalName = File(vn.filePath).name
                val zipPath = "audio/${entry.id}_$originalName"
                filesToInclude.add(zipPath to vn.filePath)

                VoiceNoteExportDto(
                    originalFilename = originalName,
                    durationMs = vn.durationMs,
                    label = vn.label,
                    transcript = vn.transcript
                )
            }

            val pDtos = photos.map { p ->
                val originalName = File(p.filePath).name
                val zipPath = "photos/${entry.id}_$originalName"
                filesToInclude.add(zipPath to p.filePath)

                PhotoExportDto(originalFilename = originalName)
            }

            exportDtos.add(
                EntryExportDto(
                    id = entry.id,
                    title = entry.title,
                    body = entry.body,
                    mood = entry.mood.name,
                    tags = entry.tags,
                    createdAt = entry.createdAt,
                    updatedAt = entry.updatedAt,
                    voiceNotes = vnDtos,
                    photos = pDtos
                )
            )
        }

        zos.putNextEntry(ZipEntry("manifest.json"))
        zos.write(gson.toJson(exportDtos).toByteArray())
        zos.closeEntry()

        filesToInclude.forEach { (zipPath, localPath) ->
            try {
                val file = File(localPath)
                if (file.exists()) {
                    zos.putNextEntry(ZipEntry(zipPath))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            } catch (_: Exception) {
                // Skip unreadable media; keep backup usable
            }
        }
    }

    fun buildImportedFileName(prefix: String, originalFilename: String): String {
        val baseName = originalFilename
            .substringAfterLast('/')
            .substringAfterLast('\\')
        val safeName = baseName
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_')
            .take(120)
            .ifBlank { "file" }

        return "${prefix}_${UUID.randomUUID()}_$safeName"
    }
}
