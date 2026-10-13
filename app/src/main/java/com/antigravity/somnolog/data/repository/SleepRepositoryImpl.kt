package com.antigravity.somnolog.data.repository

import com.antigravity.somnolog.data.local.AppDatabase
import com.antigravity.somnolog.data.local.entity.AudioEventEntity
import com.antigravity.somnolog.data.local.entity.SleepSessionEntity
import com.antigravity.somnolog.domain.model.AudioEvent
import com.antigravity.somnolog.domain.model.SleepSession
import com.antigravity.somnolog.domain.repository.SleepRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Repository implementation for managing sleep sessions and recorded audio events.
 */
class SleepRepositoryImpl(private val database: AppDatabase) : SleepRepository {

    private val sessionDao = database.sessionDao()
    private val audioEventDao = database.audioEventDao()

    override fun getAllSessions(): Flow<List<SleepSession>> {
        return sessionDao.getAllSessions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSessionById(sessionId: Long): SleepSession? {
        return sessionDao.getSessionById(sessionId)?.toDomain()
    }

    override suspend fun getActiveSession(): SleepSession? {
        return sessionDao.getActiveSession()?.toDomain()
    }

    override suspend fun startSession(baselineDb: Float, thresholdDb: Float): Long {
        val newSession = SleepSessionEntity(
            startTime = System.currentTimeMillis(),
            baselineNoiseDb = baselineDb,
            triggerThresholdDb = thresholdDb,
            clipCount = 0
        )
        return sessionDao.insertSession(newSession)
    }

    override suspend fun endSession(sessionId: Long) {
        val session = sessionDao.getSessionById(sessionId) ?: return
        val updated = session.copy(endTime = System.currentTimeMillis())
        sessionDao.updateSession(updated)
    }

    override suspend fun deleteSession(sessionId: Long) {
        val session = sessionDao.getSessionById(sessionId) ?: return
        // Also delete audio files on disk associated with this session
        sessionDao.deleteSession(session)
    }

    override fun getEventsForSession(sessionId: Long): Flow<List<AudioEvent>> {
        return audioEventDao.getEventsForSession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllEvents(): Flow<List<AudioEvent>> {
        return audioEventDao.getAllEvents().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveAudioEvent(event: AudioEvent): Long {
        val id = audioEventDao.insertAudioEvent(AudioEventEntity.fromDomain(event))
        sessionDao.incrementClipCount(event.sessionId)
        return id
    }

    override suspend fun deleteAudioEvent(event: AudioEvent) {
        // Delete physical file from disk
        try {
            val file = File(event.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
            // Ignore file deletion errors
        }
        audioEventDao.deleteAudioEvent(AudioEventEntity.fromDomain(event))
    }
}
