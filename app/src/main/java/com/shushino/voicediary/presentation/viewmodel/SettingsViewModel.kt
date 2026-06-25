package com.shushino.voicediary.presentation.viewmodel

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shushino.voicediary.data.ColorPalette
import com.shushino.voicediary.data.FontSize
import com.shushino.voicediary.data.SettingsDataStore
import com.shushino.voicediary.data.ThemeMode
import com.shushino.voicediary.data.manager.LockManager
import com.shushino.voicediary.data.manager.ReminderScheduler
import com.shushino.voicediary.data.model.EntryExportDto
import com.shushino.voicediary.data.model.PhotoExportDto
import com.shushino.voicediary.data.model.VoiceNoteExportDto
import com.shushino.voicediary.domain.model.DiaryEntry
import com.shushino.voicediary.domain.model.Mood
import com.shushino.voicediary.domain.model.Photo
import com.shushino.voicediary.domain.model.VoiceNote
import com.shushino.voicediary.domain.repository.DiaryRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val reminderScheduler: ReminderScheduler,
    private val diaryRepository: DiaryRepository,
    private val lockManager: LockManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _exportStatus = MutableSharedFlow<String>()
    val exportStatus = _exportStatus.asSharedFlow()

    val isPinSet: StateFlow<Boolean> = lockManager.isPinSetFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsDataStore.reminderEnabled,
        settingsDataStore.reminderHour,
        settingsDataStore.reminderMinute,
        settingsDataStore.themeMode,
        settingsDataStore.fontSize,
        settingsDataStore.biometricEnabled,
        settingsDataStore.colorPalette,
        isPinSet
    ) { params: Array<Any> ->
        SettingsUiState(
            reminderEnabled = params[0] as Boolean,
            hour = params[1] as Int,
            minute = params[2] as Int,
            themeMode = params[3] as ThemeMode,
            fontSize = params[4] as FontSize,
            biometricEnabled = params[5] as Boolean,
            colorPalette = params[6] as ColorPalette,
            pinSet = params[7] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun toggleReminder(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setReminderEnabled(enabled)
            if (enabled) {
                reminderScheduler.scheduleDailyReminder(uiState.value.hour, uiState.value.minute)
                reminderScheduler.scheduleWeeklySummary()
            } else {
                reminderScheduler.cancelAll()
            }
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsDataStore.setReminderHour(hour)
            settingsDataStore.setReminderMinute(minute)
            if (uiState.value.reminderEnabled) {
                reminderScheduler.scheduleDailyReminder(hour, minute)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun setFontSize(size: FontSize) {
        viewModelScope.launch {
            settingsDataStore.setFontSize(size)
        }
    }

    fun setColorPalette(palette: ColorPalette) {
        viewModelScope.launch {
            settingsDataStore.setColorPalette(palette)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setBiometricEnabled(enabled)
        }
    }

    fun removePin() {
        viewModelScope.launch {
            lockManager.clearPin()
        }
    }

    fun exportAllEntries(includeAudio: Boolean = true, includeImages: Boolean = true) {
        viewModelScope.launch {
            try {
                val entries = diaryRepository.getAllActiveEntriesSync()
                if (entries.isEmpty()) {
                    _exportStatus.emit("No entries to export")
                    return@launch
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
                    uri?.let {
                        resolver.openOutputStream(it)?.use { os ->
                            ZipOutputStream(os).use { zos ->
                                writeEntriesToZip(entries, zos, includeAudio, includeImages)
                            }
                        }
                        _exportStatus.emit("Exported ${entries.size} entries to Downloads ✓")
                    } ?: _exportStatus.emit("Failed to create export file")
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, fileName)
                    FileOutputStream(file).use { fos ->
                        ZipOutputStream(fos).use { zos ->
                            writeEntriesToZip(entries, zos, includeAudio, includeImages)
                        }
                    }
                    _exportStatus.emit("Exported ${entries.size} entries to Downloads ✓")
                }
            } catch (e: Exception) {
                _exportStatus.emit("Export failed: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun writeEntriesToZip(
        entries: List<DiaryEntry>,
        zos: ZipOutputStream,
        includeAudio: Boolean,
        includeImages: Boolean
    ) {
        val exportDtos = mutableListOf<EntryExportDto>()
        val filesToInclude = mutableListOf<Pair<String, String>>() // zipPath to localPath

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

        // 1. Write manifest.json FIRST
        zos.putNextEntry(ZipEntry("manifest.json"))
        zos.write(Gson().toJson(exportDtos).toByteArray())
        zos.closeEntry()

        // 2. Write binary files
        filesToInclude.forEach { (zipPath, localPath) ->
            try {
                val file = File(localPath)
                if (file.exists()) {
                    zos.putNextEntry(ZipEntry(zipPath))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            } catch (e: Exception) {
                // Skip if file error
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Failed to open file")
                val zipInputStream = ZipInputStream(inputStream)
                
                // 1. manifest.json MUST be the first entry
                var zipEntry: ZipEntry? = zipInputStream.getNextEntry()
                if (zipEntry == null || zipEntry.name != "manifest.json") {
                    _exportStatus.emit("Invalid backup: manifest.json must be the first entry")
                    zipInputStream.close()
                    return@launch
                }

                val manifestJson = zipInputStream.readBytes().decodeToString()
                zipInputStream.closeEntry()

                val entries: List<EntryExportDto> = Gson().fromJson(
                    manifestJson,
                    object : TypeToken<List<EntryExportDto>>() {}.type
                )

                // Map to track old entry ID to new entry ID for file streaming
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

                // 2. Stream subsequent entries directly to disk
                zipEntry = zipInputStream.getNextEntry()
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
                                val destDir = File(context.filesDir, subDir)
                                if (!destDir.exists()) destDir.mkdirs()

                                val destFile = File(destDir, buildImportedFileName(if (isAudio) "audio" else "photo", originalName))
                                
                                // Stream to disk
                                FileOutputStream(destFile).use { fos ->
                                    BufferedOutputStream(fos).use { bos ->
                                        zipInputStream.copyTo(bos)
                                    }
                                }

                                // Update database with the new file path
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
                    zipEntry = zipInputStream.getNextEntry()
                }
                zipInputStream.close()

                _exportStatus.emit("Import successful ✓")
            } catch (e: Exception) {
                _exportStatus.emit("Import failed: ${e.localizedMessage}")
            }
        }
    }

    private fun buildImportedFileName(prefix: String, originalFilename: String): String {
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

data class SettingsUiState(
    val reminderEnabled: Boolean = false,
    val hour: Int = 21,
    val minute: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: FontSize = FontSize.MEDIUM,
    val colorPalette: ColorPalette = ColorPalette.DEFAULT,
    val biometricEnabled: Boolean = false,
    val pinSet: Boolean = false
)
