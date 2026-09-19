package com.shushino.voicediary.data.manager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioImportManagerTest {

    @Test
    fun validateMimeType_acceptsAudioMime() {
        assertTrue(AudioImportManager.validateMimeType("audio/mpeg").isSuccess)
        assertTrue(AudioImportManager.validateMimeType("audio/mp4").isSuccess)
    }

    @Test
    fun validateMimeType_rejectsNullOrNonAudio() {
        assertTrue(AudioImportManager.validateMimeType(null).isFailure)
        assertTrue(AudioImportManager.validateMimeType("image/jpeg").isFailure)
        assertTrue(AudioImportManager.validateMimeType("video/mp4").isFailure)
    }

    @Test
    fun validateFileSize_rejectsOverLimit() {
        val over = 50L * 1024 * 1024 + 1
        val result = AudioImportManager.validateFileSize(over)
        assertTrue(result.isFailure)
        assertEquals("File too large (max 50MB)", result.exceptionOrNull()?.message)
    }

    @Test
    fun validateFileSize_acceptsAtOrUnderLimit() {
        assertTrue(AudioImportManager.validateFileSize(0).isSuccess)
        assertTrue(AudioImportManager.validateFileSize(50L * 1024 * 1024).isSuccess)
    }

    @Test
    fun getFileExtension_mapsKnownMimes() {
        assertEquals("mp3", AudioImportManager.getFileExtension("audio/mpeg"))
        assertEquals("wav", AudioImportManager.getFileExtension("audio/wav"))
        assertEquals("m4a", AudioImportManager.getFileExtension("audio/unknown"))
    }
}
