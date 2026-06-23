package com.george.voicediary.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.george.voicediary.data.local.converters.GsonTypeConverters
import com.george.voicediary.data.local.dao.EntryDao
import com.george.voicediary.data.local.dao.PhotoDao
import com.george.voicediary.data.local.dao.TagDao
import com.george.voicediary.data.local.dao.VoiceNoteDao
import com.george.voicediary.data.local.entity.EntryEntity
import com.george.voicediary.data.local.entity.PhotoEntity
import com.george.voicediary.data.local.entity.TagEntity
import com.george.voicediary.data.local.entity.VoiceNoteEntity

@Database(
    entities = [EntryEntity::class, VoiceNoteEntity::class, PhotoEntity::class, TagEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(GsonTypeConverters::class)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun photoDao(): PhotoDao
    abstract fun tagDao(): TagDao
}
