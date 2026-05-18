package com.george.voicediary.data.local.entity

import androidx.room.Embedded

data class EntryWithMetadata(
    @Embedded val entry: EntryEntity,
    val voiceNoteCount: Int
)