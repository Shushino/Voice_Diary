package com.george.voicediary.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "voice_notes",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["entryId"])]
)
data class VoiceNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val entryId: Long,
    val filePath: String,
    val durationMs: Long,
    val label: String?,
    val transcript: String?,
    val createdAt: Long,
    val isDeleted: Boolean = false,
    val deletedAt: Long?
)