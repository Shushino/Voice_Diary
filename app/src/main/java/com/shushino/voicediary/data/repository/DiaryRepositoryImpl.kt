package com.shushino.voicediary.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shushino.voicediary.data.local.dao.EntryDao
import com.shushino.voicediary.data.local.dao.PhotoDao
import com.shushino.voicediary.data.local.dao.VoiceNoteDao
import com.shushino.voicediary.data.local.entity.EntryEntity
import com.shushino.voicediary.data.local.entity.EntryWithMetadata
import com.shushino.voicediary.data.local.entity.PhotoEntity
import com.shushino.voicediary.data.local.entity.VoiceNoteEntity
import com.shushino.voicediary.domain.model.DiaryEntry
import com.shushino.voicediary.domain.model.Mood
import com.shushino.voicediary.domain.model.Photo
import com.shushino.voicediary.domain.model.VoiceNote
import com.shushino.voicediary.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

class DiaryRepositoryImpl @Inject constructor(
    private val entryDao: EntryDao,
    private val voiceNoteDao: VoiceNoteDao,
    private val photoDao: PhotoDao
) : DiaryRepository {

    private val gson = Gson()

    override fun getAllEntries(): Flow<List<DiaryEntry>> {
        return entryDao.getAllEntries().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getEntryById(id: Long): Flow<DiaryEntry?> {
        return entryDao.getEntryById(id).map { it?.toDomain() }
    }

    override fun searchEntries(query: String): Flow<List<DiaryEntry>> {
        return entryDao.searchEntries(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getEntriesInDateRange(start: Long, end: Long): Flow<List<DiaryEntry>> {
        return entryDao.getEntriesInDateRange(start, end).map { list ->
            list.map { it.toDomain() }
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

    override fun getTrashedEntries(): Flow<List<DiaryEntry>> {
        return entryDao.getTrashedEntries().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun hardDeleteEntry(id: Long) {
        val voiceNotes = voiceNoteDao.getVoiceNotesForEntry(id)
        val photos = photoDao.getPhotosForEntry(id)
        deleteVoiceNoteFiles(voiceNotes)
        deletePhotoFiles(photos)
        entryDao.hardDeleteEntry(id)
    }

    override suspend fun emptyTrash() {
        val trashedIds = entryDao.getTrashedEntryIds()
        if (trashedIds.isNotEmpty()) {
            val voiceNotes = voiceNoteDao.getVoiceNotesForEntries(trashedIds)
            val photos = photoDao.getPhotosForEntries(trashedIds)
            deleteVoiceNoteFiles(voiceNotes)
            deletePhotoFiles(photos)
        }
        entryDao.emptyTrash()
    }

    override suspend fun restoreEntry(id: Long) {
        entryDao.restoreEntry(id)
    }

    override suspend fun getAllActiveEntriesSync(): List<DiaryEntry> {
        return entryDao.getAllActiveEntriesSync().map { it.toDomainFromEntity() }
    }

    override fun getVoiceNotesForEntry(entryId: Long): Flow<List<VoiceNote>> {
        return voiceNoteDao.getForEntry(entryId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addVoiceNote(voiceNote: VoiceNote) {
        voiceNoteDao.insert(voiceNote.toEntity())
    }

    override suspend fun deleteVoiceNote(id: Long) {
        voiceNoteDao.softDelete(id, System.currentTimeMillis())
    }

    override suspend fun updateVoiceNoteLabel(id: Long, label: String) {
        voiceNoteDao.updateLabel(id, label)
    }

    override fun getPhotosForEntry(entryId: Long): Flow<List<Photo>> {
        return photoDao.getForEntry(entryId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addPhoto(photo: Photo) {
        photoDao.insert(photo.toEntity())
    }

    override suspend fun deletePhoto(id: Long) {
        val photo = photoDao.getById(id)
        if (photo != null) {
            deletePhotoFiles(listOf(photo))
        }
        photoDao.delete(id)
    }

    private fun EntryEntity.toDomainFromEntity(): DiaryEntry {
        val type = object : TypeToken<List<String>>() {}.type
        val tagsList: List<String> = gson.fromJson(this.tags, type) ?: emptyList()

        return DiaryEntry(
            id = this.id,
            title = this.title,
            body = this.body,
            mood = Mood.valueOf(this.mood),
            tags = tagsList,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            isDeleted = this.isDeleted,
            deletedAt = this.deletedAt
        )
    }

    private fun EntryWithMetadata.toDomain(): DiaryEntry {
        val type = object : TypeToken<List<String>>() {}.type
        val tagsList: List<String> = gson.fromJson(this.entry.tags, type) ?: emptyList()

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
        return EntryEntity(
            id = this.id,
            title = this.title,
            body = this.body,
            mood = this.mood.name,
            tags = gson.toJson(this.tags),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            isDeleted = this.deletedAt != null,
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

    companion object {
        /** Deletes files at the given paths if they exist. Safe for missing/unreadable files. */
        fun deleteFilesSafely(paths: Iterable<String>) {
            paths.forEach { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {
                    // Best-effort cleanup; DB row removal must still proceed
                }
            }
        }
    }

    private fun deleteVoiceNoteFiles(voiceNotes: List<VoiceNoteEntity>) {
        deleteFilesSafely(voiceNotes.map { it.filePath })
    }

    private fun deletePhotoFiles(photos: List<PhotoEntity>) {
        deleteFilesSafely(photos.map { it.filePath })
    }
}
