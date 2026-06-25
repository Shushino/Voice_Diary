package com.shushino.voicediary.domain.model

data class Photo(
    val id: Long = 0L,
    val entryId: Long,
    val filePath: String,
    val createdAt: Long
)
