package com.george.voicediary.data.local.dao

import androidx.room.*
import com.george.voicediary.data.local.entity.VoiceNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceNoteDao {
    @Query("SELECT * FROM voice_notes WHERE entryId = :entryId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getForEntry(entryId: Long): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE entryId = :entryId")
    suspend fun getVoiceNotesForEntry(entryId: Long): List<VoiceNoteEntity>

    @Query("SELECT * FROM voice_notes WHERE entryId IN (:entryIds)")
    suspend fun getVoiceNotesForEntries(entryIds: List<Long>): List<VoiceNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(voiceNote: VoiceNoteEntity)

    @Query("UPDATE voice_notes SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)
}
