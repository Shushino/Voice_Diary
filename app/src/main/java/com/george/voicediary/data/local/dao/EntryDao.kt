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
        WHERE (e.title LIKE '%' || :query || '%' OR e.body LIKE '%' || :query || '%' OR e.tags LIKE '%' || :query || '%') AND e.isDeleted = 0
        ORDER BY e.createdAt DESC
    """)
    fun searchEntries(query: String): Flow<List<EntryWithMetadata>>

    @Query("SELECT * FROM entries WHERE mood = :mood AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getEntriesByMood(mood: String): Flow<List<EntryEntity>>

    @Query("""
        SELECT e.*, (SELECT COUNT(*) FROM voice_notes v WHERE v.entryId = e.id AND v.isDeleted = 0) as voiceNoteCount 
        FROM entries e 
        WHERE e.createdAt >= :start AND e.createdAt <= :end AND e.isDeleted = 0 
        ORDER BY e.createdAt DESC
    """)
    fun getEntriesInDateRange(start: Long, end: Long): Flow<List<EntryWithMetadata>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: EntryEntity): Long

    @Update
    suspend fun updateEntry(entry: EntryEntity)

    @Query("UPDATE entries SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteEntry(id: Long, deletedAt: Long)

    @Query("""
        SELECT e.*, (SELECT COUNT(*) FROM voice_notes v WHERE v.entryId = e.id AND v.isDeleted = 0) as voiceNoteCount 
        FROM entries e 
        WHERE e.isDeleted = 1 
        ORDER BY e.deletedAt DESC
    """)
    fun getTrashedEntries(): Flow<List<EntryWithMetadata>>

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun hardDeleteEntry(id: Long)

    @Query("DELETE FROM entries WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("SELECT id FROM entries WHERE isDeleted = 1")
    suspend fun getTrashedEntryIds(): List<Long>

    @Query("UPDATE entries SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreEntry(id: Long)

    @Query("SELECT * FROM entries WHERE isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getAllActiveEntriesSync(): List<EntryEntity>
}
