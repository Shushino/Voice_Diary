package com.george.voicediary.domain.repository

import com.george.voicediary.domain.model.DiaryEntry
import com.george.voicediary.domain.model.Photo
import com.george.voicediary.domain.model.VoiceNote
import kotlinx.coroutines.flow.Flow

interface DiaryRepository {
    fun getAllEntries(): Flow<List<DiaryEntry>>
    fun getEntryById(id: Long): Flow<DiaryEntry?>
    fun searchEntries(query: String): Flow<List<DiaryEntry>>
    fun getEntriesInDateRange(start: Long, end: Long): Flow<List<DiaryEntry>>
    suspend fun createEntry(entry: DiaryEntry): Long
    suspend fun updateEntry(entry: DiaryEntry)
    suspend fun softDeleteEntry(id: Long)
    fun getVoiceNotesForEntry(entryId: Long): Flow<List<VoiceNote>>
    suspend fun addVoiceNote(voiceNote: VoiceNote)
    suspend fun deleteVoiceNote(id: Long)
    fun getPhotosForEntry(entryId: Long): Flow<List<Photo>>
    suspend fun addPhoto(photo: Photo)
    suspend fun deletePhoto(id: Long)
}