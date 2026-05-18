package com.george.voicediary.domain.model

data class VoiceNote(
    val id: Long = 0L,
    val entryId: Long,
    val filePath: String,
    val durationMs: Long,
    val label: String?,
    val transcript: String?,
    val createdAt: Long,
    val isDeleted: Boolean = false,
    val deletedAt: Long?
)