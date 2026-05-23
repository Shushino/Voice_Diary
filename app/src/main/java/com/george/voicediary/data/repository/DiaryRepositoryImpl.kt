package com.george.voicediary.data.repository

import com.george.voicediary.data.local.dao.EntryDao
import com.george.voicediary.data.local.dao.PhotoDao
import com.george.voicediary.data.local.dao.TagDao
import com.george.voicediary.data.local.dao.VoiceNoteDao
import com.george.voicediary.data.local.entity.EntryEntity
import com.george.voicediary.data.local.entity.PhotoEntity
import com.george.voicediary.data.local.entity.TagEntity
import com.george.voicediary.data.local.entity.VoiceNoteEntity
import com.george.voicediary.domain.model.DiaryEntry
import com.george.voicediary.domain.model.DiaryTag
import com.george.voicediary.domain.model.Mood
import com.george.voicediary.domain.model.Photo
import com.george.voicediary.domain.model.VoiceNote
import com.george.voicediary.domain.repository.DiaryRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DiaryRepositoryImpl @Inject constructor(
    private val entryDao: EntryDao,
    private val voiceNoteDao: VoiceNoteDao,
    private val photoDao: PhotoDao,
    private val tagDao: TagDao
) : DiaryRepository {

    private val gson = Gson()

    override fun getAllEntries(): Flow<List<DiaryEntry>> {
        return entryDao.getAllEntries().map { metadataList ->
            metadataList.map { it.toDomain() }
        }
    }

    override fun getEntryById(id: Long): Flow<DiaryEntry?> {
        return entryDao.getEntryById(id).map { it?.toDomain() }
    }

    override fun searchEntries(query: String): Flow<List<DiaryEntry>> {
        return entryDao.searchEntries(query).map { metadataList ->
            metadataList.map { it.toDomain() }
        }
    }

    override fun getEntriesInDateRange(start: Long, end: Long): Flow<List<DiaryEntry>> {
        return entryDao.getEntriesInDateRange(start, end).map { metadataList ->
            metadataList.map { it.toDomain() }
        }
    }

    override suspend fun createEntry(entry: DiaryEntry): Long {
        return entryDao.insertEntry(entry.toEntity())
    }

    override suspend fun updateEntry(entry: DiaryEntry) {
        entryDao.updateEntry(entry.toEntity())
    }

    override suspend fun softDeleteEntry(id: Long) {
        entryDao.softDeleteEntry(id, System.currentTimeMillis())
    }

    override fun getVoiceNotesForEntry(entryId: Long): Flow<List<VoiceNote>> {
        return voiceNoteDao.getForEntry(entryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addVoiceNote(voiceNote: VoiceNote) {
        voiceNoteDao.insert(voiceNote.toEntity())
    }

    override suspend fun deleteVoiceNote(id: Long) {
        voiceNoteDao.softDelete(id, System.currentTimeMillis())
    }

    override fun getPhotosForEntry(entryId: Long): Flow<List<Photo>> {
        return photoDao.getForEntry(entryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addPhoto(photo: Photo) {
        photoDao.insert(photo.toEntity())
    }

    override suspend fun deletePhoto(id: Long) {
        photoDao.delete(id)
    }

    private fun com.george.voicediary.data.local.entity.EntryWithMetadata.toDomain(): DiaryEntry {
        val tagsList: List<String> = gson.fromJson(this.entry.tags, object : TypeToken<List<String>>() {}.type)
        return DiaryEntry(
            id = this.entry.id,
            title = this.entry.title,
            body = this.entry.body,
            mood = Mood.valueOf(this.entry.mood),
            tags = tagsList,
            createdAt = this.entry.createdAt,
            updatedAt = this.entry.updatedAt,
            isDeleted = this.entry.isDeleted,
            deletedAt = this.entry.deletedAt,
            voiceNoteCount = this.voiceNoteCount
        )
    }

    private fun DiaryEntry.toEntity(): EntryEntity {
        val tagsJson = gson.toJson(this.tags)
        return EntryEntity(
            id = this.id,
            title = this.title,
            body = this.body,
            mood = this.mood.name,
            tags = tagsJson,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            isDeleted = this.isDeleted,
            deletedAt = this.deletedAt
        )
    }

    private fun VoiceNoteEntity.toDomain(): VoiceNote {
        return VoiceNote(
            id = this.id,
            entryId = this.entryId,
            filePath = this.filePath,
            durationMs = this.durationMs,
            label = this.label,
            transcript = this.transcript,
            createdAt = this.createdAt,
            isDeleted = this.isDeleted,
            deletedAt = this.deletedAt
        )
    }

    private fun VoiceNote.toEntity(): VoiceNoteEntity {
        return VoiceNoteEntity(
            id = this.id,
            entryId = this.entryId,
            filePath = this.filePath,
            durationMs = this.durationMs,
            label = this.label,
            transcript = this.transcript,
            createdAt = this.createdAt,
            isDeleted = this.isDeleted,
            deletedAt = this.deletedAt
        )
    }

    private fun PhotoEntity.toDomain(): Photo {
        return Photo(
            id = this.id,
            entryId = this.entryId,
            filePath = this.filePath,
            createdAt = this.createdAt
        )
    }

    private fun Photo.toEntity(): PhotoEntity {
        return PhotoEntity(
            id = this.id,
            entryId = this.entryId,
            filePath = this.filePath,
            createdAt = this.createdAt
        )
    }

    private fun TagEntity.toDomain(): DiaryTag {
        return DiaryTag(
            id = this.id,
            name = this.name,
            colorHex = this.colorHex
        )
    }

    private fun DiaryTag.toEntity(): TagEntity {
        return TagEntity(
            id = this.id,
            name = this.name,
            colorHex = this.colorHex
        )
    }
}