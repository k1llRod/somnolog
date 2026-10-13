package com.antigravity.somnolog

import com.antigravity.somnolog.domain.model.AudioEvent
import com.antigravity.somnolog.domain.model.SleepSession
import com.antigravity.somnolog.domain.repository.SleepRepository
import com.antigravity.somnolog.domain.usecase.SaveClipUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class SaveClipUseCaseTest {

    private lateinit var fakeRepository: FakeSleepRepository
    private lateinit var saveClipUseCase: SaveClipUseCase
    private lateinit var tempFile: File

    @Before
    fun setUp() {
        fakeRepository = FakeSleepRepository()
        saveClipUseCase = SaveClipUseCase(fakeRepository)
        tempFile = File.createTempFile("test_audio", ".m4a")
        tempFile.writeText("fake audio content")
    }

    @Test
    fun `clip shorter than 800ms is discarded and file is deleted`() {
        runBlocking {
            assertTrue(tempFile.exists())

            val result = saveClipUseCase(
                sessionId = 1L,
                timestamp = System.currentTimeMillis(),
                durationMs = 799L, // Shorter than 800ms filter threshold
                peakDb = 72.0f,
                filePath = tempFile.absolutePath
            )

            assertNull("Clip under 800ms must be discarded", result)
            assertFalse("Physical file of discarded clip must be deleted", tempFile.exists())
            assertEquals("Repository should have 0 events", 0, fakeRepository.events.size)
        }
    }

    @Test
    fun `clip with 800ms or longer is saved and retained`() {
        runBlocking {
            val result = saveClipUseCase(
                sessionId = 1L,
                timestamp = 1000L,
                durationMs = 2500L,
                peakDb = 68.5f,
                filePath = tempFile.absolutePath
            )

            assertNotNull(result)
            assertEquals(2500L, result?.durationMs)
            assertEquals(68.5f, result?.peakDb)
            assertTrue(tempFile.exists())
            assertEquals(1, fakeRepository.events.size)

            // Cleanup
            tempFile.delete()
        }
    }

    @Test
    fun `clip duration is capped at 60 seconds max`() {
        runBlocking {
            val result = saveClipUseCase(
                sessionId = 1L,
                timestamp = 1000L,
                durationMs = 75000L, // 75 seconds
                peakDb = 85.0f,
                filePath = tempFile.absolutePath
            )

            assertNotNull(result)
            assertEquals(SaveClipUseCase.MAX_CLIP_DURATION_MS, result?.durationMs)

            // Cleanup
            tempFile.delete()
        }
    }

    private class FakeSleepRepository : SleepRepository {
        val events = mutableListOf<AudioEvent>()
        private var nextId = 1L

        override fun getAllSessions(): Flow<List<SleepSession>> = flowOf(emptyList())
        override suspend fun getSessionById(sessionId: Long): SleepSession? = null
        override suspend fun getActiveSession(): SleepSession? = null
        override suspend fun startSession(baselineDb: Float, thresholdDb: Float): Long = 1L
        override suspend fun endSession(sessionId: Long) {}
        override suspend fun deleteSession(sessionId: Long) {}
        override fun getEventsForSession(sessionId: Long): Flow<List<AudioEvent>> = flowOf(events)
        override fun getAllEvents(): Flow<List<AudioEvent>> = flowOf(events)

        override suspend fun saveAudioEvent(event: AudioEvent): Long {
            events.add(event.copy(id = nextId))
            return nextId++
        }

        override suspend fun deleteAudioEvent(event: AudioEvent) {
            events.removeAll { it.id == event.id }
        }
    }
}
