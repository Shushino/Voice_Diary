package com.george.voicediary.data.local.dao

import androidx.room.*
import com.george.voicediary.data.local.entity.EntryEntity
import com.george.voicediary.data.local.entity.EntryWithMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("""
        SELECT e.*, (SELECT COUNT(*) FROM voice_notes v WHERE v.entryId = e.id AND v.isDeleted = 0) as voiceNoteCount 
        FROM entries e 
        WHERE e.isDeleted = 0 
        ORDER BY e.createdAt DESC
    """)
    fun getAllEntries(): Flow<List<EntryWithMetadata>>

    @Query("""
        SELECT e.*, (SELECT COUNT(*) FROM voice_notes v WHERE v.entryId = e.id AND v.isDeleted = 0) as voiceNoteCount 
        FROM entries e 
        WHERE e.id = :id AND e.isDeleted = 0
    """)
    fun getEntryById(id: Long): Flow<EntryWithMetadata?>

    @Query("""
        SELECT e.*, (SELECT COUNT(*) FROM voice_notes v WHERE v.entryId = e.id AND v.isDeleted = 0) as voiceNoteCount 
        FROM entries e 
        WHERE (e.title LIKE '%' || :query || '%' OR e.body LIKE '%' || :query || '%') AND e.isDeleted = 0 
        ORDER BY e.createdAt DESC
    """)
    fun searchEntries(query: String): Flow<List<EntryWithMetadata>>

    @Query("SELECT * FROM entries WHERE mood = :mood AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getEntriesByMood(mood: String): Flow<List<EntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: EntryEntity): Long

    @Update
    suspend fun updateEntry(entry: EntryEntity)

    @Query("UPDATE entries SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteEntry(id: Long, deletedAt: Long)
}