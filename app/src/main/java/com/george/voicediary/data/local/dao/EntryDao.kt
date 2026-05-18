package com.george.voicediary.data.local.dao

import androidx.room.*
import com.george.voicediary.data.local.entity.EntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE id = :id AND isDeleted = 0")
    fun getEntryById(id: Long): Flow<EntryEntity?>

    @Query("SELECT * FROM entries WHERE (title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%') AND isDeleted = 0 ORDER BY createdAt DESC")
    fun searchEntries(query: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE mood = :mood AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getEntriesByMood(mood: String): Flow<List<EntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: EntryEntity): Long

    @Update
    suspend fun updateEntry(entry: EntryEntity)

    @Query("UPDATE entries SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteEntry(id: Long, deletedAt: Long)
}