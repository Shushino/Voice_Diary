package com.shushino.voicediary.data.manager

import android.content.Context
import com.shushino.voicediary.domain.model.DiaryEntry
import com.shushino.voicediary.domain.model.Mood
import com.shushino.voicediary.domain.model.Photo
import com.shushino.voicediary.domain.model.VoiceNote
import com.shushino.voicediary.domain.repository.DiaryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

class BackupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var diaryRepository: DiaryRepository
    private lateinit var backupManager: BackupManager
    private lateinit var filesRoot: File

    @Before
    fun setUp() {
        filesRoot = tempFolder.newFolder("files")
        context = mockk(relaxed = true)
        every { context.filesDir } returns filesRoot
        diaryRepository = mockk(relaxed = true)
        backupManager = BackupManager(context, diaryRepository)
    }

    @Test
    fun exportThenImport_roundTripsEntryAndMedia() = runBlocking {
        val audioFile = File(filesRoot, "voicenotes").also { it.mkdirs() }.let { dir ->
            File(dir, "clip.m4a").also { it.writeText("AUDIO-BYTES") }
        }
        val photoFile = File(filesRoot, "photos").also { it.mkdirs() }.let { dir ->
            File(dir, "shot.jpg").also { it.writeText("PHOTO-BYTES") }
        }

        val entry = DiaryEntry(
            id = 99L,
            title = "Trip",
            body = "Nice day",
            mood = Mood.HAPPY,
            tags = listOf("travel"),
            createdAt = 1000L,
            updatedAt = 2000L,
            deletedAt = null
        )

        coEvery { diaryRepository.getVoiceNotesForEntry(99L) } returns flowOf(
            listOf(
                VoiceNote(
                    id = 1L,
                    entryId = 99L,
                    filePath = audioFile.absolutePath,
                    durationMs = 1234L,
                    label = "memo",
                    transcript = "hello",
                    createdAt = 1000L,
                    deletedAt = null
                )
            )
        )
        coEvery { diaryRepository.getPhotosForEntry(99L) } returns flowOf(
            listOf(
                Photo(
                    id = 2L,
                    entryId = 99L,
                    filePath = photoFile.absolutePath,
                    createdAt = 1000L
                )
            )
        )

        val baos = ByteArrayOutputStream()
        backupManager.exportToStream(listOf(entry), baos, includeAudio = true, includeImages = true)
        val zipBytes = baos.toByteArray()

        // manifest must be first zip entry
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            val first = zis.nextEntry
            assertEquals("manifest.json", first?.name)
        }

        val createdEntry = slot<DiaryEntry>()
        coEvery { diaryRepository.createEntry(capture(createdEntry)) } returns 55L
        coEvery { diaryRepository.addVoiceNote(any()) } returns Unit
        coEvery { diaryRepository.addPhoto(any()) } returns Unit

        val importRoot = tempFolder.newFolder("import_files")
        backupManager.importFromStream(ByteArrayInputStream(zipBytes), filesRoot = importRoot)

        assertEquals("Trip", createdEntry.captured.title)
        assertEquals("Nice day", createdEntry.captured.body)
        assertEquals(Mood.HAPPY, createdEntry.captured.mood)
        assertEquals(listOf("travel"), createdEntry.captured.tags)

        coVerify(exactly = 1) {
            diaryRepository.addVoiceNote(
                match {
                    it.entryId == 55L && it.durationMs == 1234L && it.label == "memo" &&
                        it.createdAt == 1000L // original media timestamp preserved
                }
            )
        }
        coVerify(exactly = 1) { diaryRepository.addPhoto(match { it.entryId == 55L && it.createdAt == 1000L }) }

        val importedAudio = File(importRoot, "voicenotes").listFiles()?.firstOrNull()
        val importedPhoto = File(importRoot, "photos").listFiles()?.firstOrNull()
        assertTrue(importedAudio != null && importedAudio.readText() == "AUDIO-BYTES")
        assertTrue(importedPhoto != null && importedPhoto.readText() == "PHOTO-BYTES")
    }

    @Test
    fun importFromStream_skipsEntriesThatAlreadyExist() = runBlocking {
        val manifest = """[{"id":99,"title":"Trip","body":"B","mood":"CALM","tags":[],
            "createdAt":1,"updatedAt":2,"voiceNotes":[],"photos":[]}]"""
        val baos = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
            zos.write(manifest.toByteArray())
            zos.closeEntry()
        }

        // Entry id 99 already exists on this device (active or trashed)
        coEvery { diaryRepository.getAllEntriesSync() } returns listOf(
            DiaryEntry(id = 99L, title = "Existing", body = "x", mood = Mood.CALM, tags = emptyList(), createdAt = 1L, updatedAt = 1L, deletedAt = null)
        )
        coEvery { diaryRepository.createEntry(any()) } returns 55L

        backupManager.importFromStream(ByteArrayInputStream(baos.toByteArray()), filesRoot = filesRoot)

        coVerify(exactly = 0) { diaryRepository.createEntry(any()) }
    }

    @Test
    fun importFromStream_importsUnknownEntries() = runBlocking {
        val manifest = """[{"id":99,"title":"Trip","body":"B","mood":"CALM","tags":[],
            "createdAt":1,"updatedAt":2,"voiceNotes":[],"photos":[]}]"""
        val baos = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
            zos.write(manifest.toByteArray())
            zos.closeEntry()
        }

        coEvery { diaryRepository.getAllEntriesSync() } returns emptyList()
        coEvery { diaryRepository.createEntry(any()) } returns 55L

        backupManager.importFromStream(ByteArrayInputStream(baos.toByteArray()), filesRoot = filesRoot)

        coVerify(exactly = 1) { diaryRepository.createEntry(any()) }
    }

    @Test
    fun importFromStream_fallsBackToNowWhenLegacyBackupHasNoMediaTimestamp() = runBlocking {
        // A 1.x backup: voiceNotes have no createdAt field at all
        val manifest = """[{"id":5,"title":"Old","body":"B","mood":"CALM","tags":[],
            "createdAt":1,"updatedAt":2,
            "voiceNotes":[{"originalFilename":"a.m4a","durationMs":5,"label":null,"transcript":null}],
            "photos":[]}]"""
        val audio = File(filesRoot, "voicenotes").also { it.mkdirs() }.let { dir ->
            File(dir, "a.m4a").also { it.writeText("AUDIO") }
        }
        val baos = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
            zos.write(manifest.toByteArray())
            zos.closeEntry()
            zos.putNextEntry(java.util.zip.ZipEntry("audio/5_a.m4a"))
            zos.write("AUDIO".toByteArray())
            zos.closeEntry()
        }

        coEvery { diaryRepository.getAllEntriesSync() } returns emptyList()
        coEvery { diaryRepository.createEntry(any()) } returns 77L

        val importedNote = slot<VoiceNote>()
        coEvery { diaryRepository.addVoiceNote(capture(importedNote)) } returns Unit

        backupManager.importFromStream(ByteArrayInputStream(baos.toByteArray()), filesRoot = filesRoot)

        // createdAt must fall back to something sensible (import time), not 0
        assert(importedNote.captured.createdAt > 0L)
    }

    @Test
    fun importFromStream_rejectsMissingManifestFirst() = runBlocking {
        val baos = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("audio/1_x.m4a"))
            zos.write("x".toByteArray())
            zos.closeEntry()
        }

        val result = runCatching {
            backupManager.importFromStream(ByteArrayInputStream(baos.toByteArray()), filesRoot)
        }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("manifest.json") == true)
    }
}
