package com.shushino.voicediary.data.repository

import com.shushino.voicediary.data.local.dao.EntryDao
import com.shushino.voicediary.data.local.dao.PhotoDao
import com.shushino.voicediary.data.local.dao.VoiceNoteDao
import com.shushino.voicediary.data.local.entity.EntryEntity
import com.shushino.voicediary.data.local.entity.EntryWithMetadata
import com.shushino.voicediary.data.local.entity.PhotoEntity
import com.shushino.voicediary.data.local.entity.VoiceNoteEntity
import com.shushino.voicediary.domain.model.Mood
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DiaryRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var entryDao: EntryDao
    private lateinit var voiceNoteDao: VoiceNoteDao
    private lateinit var photoDao: PhotoDao
    private lateinit var repository: DiaryRepositoryImpl

    @Before
    fun setUp() {
        entryDao = mockk(relaxed = true)
        voiceNoteDao = mockk(relaxed = true)
        photoDao = mockk(relaxed = true)
        repository = DiaryRepositoryImpl(entryDao, voiceNoteDao, photoDao)
    }

    @Test
    fun getAllEntries_mapsVoiceNoteCountAndTags() = runBlocking {
        val entity = EntryEntity(
            id = 7L,
            title = "Hello",
            body = "World",
            mood = Mood.HAPPY.name,
            tags = """["a","b"]""",
            createdAt = 100L,
            updatedAt = 200L,
            isDeleted = false,
            deletedAt = null
        )
        every { entryDao.getAllEntries() } returns flowOf(
            listOf(EntryWithMetadata(entry = entity, voiceNoteCount = 3))
        )

        val entries = repository.getAllEntries().first()
        assertEquals(1, entries.size)
        assertEquals(7L, entries[0].id)
        assertEquals(listOf("a", "b"), entries[0].tags)
        assertEquals(3, entries[0].voiceNoteCount)
        assertEquals(Mood.HAPPY, entries[0].mood)
        assertFalse(entries[0].isDeleted)
    }

    @Test
    fun hardDeleteEntry_deletesAssociatedFilesThenDao() = runBlocking {
        val audio = tempFolder.newFile("note.m4a").also { it.writeText("audio") }
        val photo = tempFolder.newFile("pic.jpg").also { it.writeText("img") }

        coEvery { voiceNoteDao.getVoiceNotesForEntry(42L) } returns listOf(
            VoiceNoteEntity(
                id = 1L,
                entryId = 42L,
                filePath = audio.absolutePath,
                durationMs = 1000L,
                label = null,
                transcript = null,
                createdAt = 1L,
                deletedAt = null
            )
        )
        coEvery { photoDao.getPhotosForEntry(42L) } returns listOf(
            PhotoEntity(id = 2L, entryId = 42L, filePath = photo.absolutePath, createdAt = 1L)
        )

        repository.hardDeleteEntry(42L)

        assertFalse(audio.exists())
        assertFalse(photo.exists())
        coVerify(exactly = 1) { entryDao.hardDeleteEntry(42L) }
    }

    @Test
    fun emptyTrash_deletesFilesForAllTrashedEntries() = runBlocking {
        val audio = tempFolder.newFile("trash_note.m4a").also { it.writeText("a") }
        val photo = tempFolder.newFile("trash_pic.jpg").also { it.writeText("p") }

        coEvery { entryDao.getTrashedEntryIds() } returns listOf(10L, 11L)
        coEvery { voiceNoteDao.getVoiceNotesForEntries(listOf(10L, 11L)) } returns listOf(
            VoiceNoteEntity(
                id = 1L,
                entryId = 10L,
                filePath = audio.absolutePath,
                durationMs = 1L,
                label = null,
                transcript = null,
                createdAt = 1L,
                deletedAt = null
            )
        )
        coEvery { photoDao.getPhotosForEntries(listOf(10L, 11L)) } returns listOf(
            PhotoEntity(id = 2L, entryId = 11L, filePath = photo.absolutePath, createdAt = 1L)
        )

        repository.emptyTrash()

        assertFalse(audio.exists())
        assertFalse(photo.exists())
        coVerify(exactly = 1) { entryDao.emptyTrash() }
    }

    @Test
    fun emptyTrash_withNoTrashedIds_skipsMediaLookup() = runBlocking {
        coEvery { entryDao.getTrashedEntryIds() } returns emptyList()

        repository.emptyTrash()

        coVerify(exactly = 0) { voiceNoteDao.getVoiceNotesForEntries(any()) }
        coVerify(exactly = 0) { photoDao.getPhotosForEntries(any()) }
        coVerify(exactly = 1) { entryDao.emptyTrash() }
    }

    @Test
    fun deletePhoto_removesFileFromDisk() = runBlocking {
        val photo = tempFolder.newFile("to_delete.jpg").also { it.writeText("x") }
        coEvery { photoDao.getById(5L) } returns PhotoEntity(
            id = 5L,
            entryId = 1L,
            filePath = photo.absolutePath,
            createdAt = 1L
        )

        repository.deletePhoto(5L)

        assertFalse(photo.exists())
        coVerify { photoDao.delete(5L) }
    }

    @Test
    fun deleteFilesSafely_ignoresMissingPaths() {
        val existing = tempFolder.newFile("exists.bin").also { it.writeText("ok") }
        val missing = File(tempFolder.root, "missing.bin")

        DiaryRepositoryImpl.deleteFilesSafely(listOf(existing.absolutePath, missing.absolutePath))

        assertFalse(existing.exists())
        assertTrue(!missing.exists())
    }
}
