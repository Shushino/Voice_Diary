package com.george.voicediary.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.george.voicediary.data.SettingsDataStore
import com.george.voicediary.data.manager.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import android.os.Environment
import com.george.voicediary.data.FontSize
import com.george.voicediary.data.ThemeMode
import com.george.voicediary.domain.repository.DiaryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val reminderScheduler: ReminderScheduler,
    private val diaryRepository: DiaryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _exportStatus = MutableSharedFlow<String>()
    val exportStatus: SharedFlow<String> = _exportStatus.asSharedFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsDataStore.reminderEnabled,
        settingsDataStore.reminderHour,
        settingsDataStore.reminderMinute,
        settingsDataStore.themeMode,
        settingsDataStore.fontSize,
        settingsDataStore.biometricEnabled
    ) { params: Array<Any> ->
        SettingsUiState(
            reminderEnabled = params[0] as Boolean,
            hour = params[1] as Int,
            minute = params[2] as Int,
            themeMode = params[3] as ThemeMode,
            fontSize = params[4] as FontSize,
            biometricEnabled = params[5] as Boolean
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

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setBiometricEnabled(enabled)
        }
    }

    fun exportAllEntries() {
        viewModelScope.launch {
            try {
                val entries = diaryRepository.getAllActiveEntriesSync()
                if (entries.isEmpty()) {
                    _exportStatus.emit("No entries to export")
                    return@launch
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "voicediary_export_$timestamp.zip"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        resolver.openOutputStream(it)?.use { os ->
                            ZipOutputStream(os).use { zos ->
                                writeEntriesToZip(entries, zos)
                            }
                        }
                        _exportStatus.emit("Exported ${entries.size} entries to Downloads ✓")
                    } ?: _exportStatus.emit("Failed to create export file")
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, fileName)
                    FileOutputStream(file).use { fos ->
                        ZipOutputStream(fos).use { zos ->
                            writeEntriesToZip(entries, zos)
                        }
                    }
                    _exportStatus.emit("Exported ${entries.size} entries to Downloads ✓")
                }
            } catch (e: Exception) {
                _exportStatus.emit("Export failed: ${e.localizedMessage}")
            }
        }
    }

    private fun writeEntriesToZip(entries: List<com.george.voicediary.domain.model.DiaryEntry>, zos: ZipOutputStream) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        entries.forEach { entry ->
            val mdContent = """
                # ${entry.title}
                Date: ${sdf.format(Date(entry.createdAt))}
                Mood: ${entry.mood.emoji} ${entry.mood.name}
                Tags: ${entry.tags.joinToString(", ")}
                
                ${entry.body}
            """.trimIndent()

            val sanitizedTitle = (entry.title ?: "Untitled").replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val entryName = "${sanitizedTitle}-${entry.id}.md"
            
            zos.putNextEntry(ZipEntry(entryName))
            zos.write(mdContent.toByteArray())
            zos.closeEntry()
        }
    }
}

data class SettingsUiState(
    val reminderEnabled: Boolean = false,
    val hour: Int = 21,
    val minute: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: FontSize = FontSize.MEDIUM,
    val biometricEnabled: Boolean = false
)
