package com.george.voicediary.data.local.dao

import androidx.room.*
import com.george.voicediary.data.local.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun getForEntry(entryId: Long): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun delete(id: Long)
}