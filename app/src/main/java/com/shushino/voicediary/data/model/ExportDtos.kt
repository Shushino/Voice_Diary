package com.shushino.voicediary.data.model

import androidx.annotation.Keep

@Keep
data class EntryExportDto(
    val id: Long,
    val title: String?,
    val body: String,
    val mood: String,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
    val voiceNotes: List<VoiceNoteExportDto>,
    val photos: List<PhotoExportDto>
)

@Keep
data class VoiceNoteExportDto(
    val originalFilename: String,
    val durationMs: Long,
    val label: String?,
    val transcript: String?,
    /** Added in backup format v2. Older 1.x backups omit it; import falls back to "now". */
    val createdAt: Long = 0L
)

@Keep
data class PhotoExportDto(
    val originalFilename: String,
    /** Added in backup format v2. Older 1.x backups omit it; import falls back to "now". */
    val createdAt: Long = 0L
)
