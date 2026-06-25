package com.shushino.voicediary.domain.model

data class DiaryEntry(
    val id: Long = 0L,
    val title: String?,
    val body: String,
    val mood: Mood,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val deletedAt: Long?,
    val voiceNoteCount: Int = 0
)
