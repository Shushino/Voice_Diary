package com.shushino.voicediary.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shushino.voicediary.data.local.converters.GsonTypeConverters
import com.shushino.voicediary.data.local.dao.EntryDao
import com.shushino.voicediary.data.local.dao.PhotoDao
import com.shushino.voicediary.data.local.dao.VoiceNoteDao
import com.shushino.voicediary.data.local.entity.EntryEntity
import com.shushino.voicediary.data.local.entity.PhotoEntity
import com.shushino.voicediary.data.local.entity.VoiceNoteEntity

@Database(
    entities = [
        EntryEntity::class,
        VoiceNoteEntity::class,
        PhotoEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(GsonTypeConverters::class)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun photoDao(): PhotoDao
}
