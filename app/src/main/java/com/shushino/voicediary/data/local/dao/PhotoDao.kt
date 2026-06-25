package com.shushino.voicediary.data.local.dao

import androidx.room.*
import com.shushino.voicediary.data.local.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun getForEntry(entryId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getById(id: Long): PhotoEntity?

    @Query("SELECT * FROM photos WHERE entryId = :entryId")
    suspend fun getPhotosForEntry(entryId: Long): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE entryId IN (:entryIds)")
    suspend fun getPhotosForEntries(entryIds: List<Long>): List<PhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun delete(id: Long)
}
