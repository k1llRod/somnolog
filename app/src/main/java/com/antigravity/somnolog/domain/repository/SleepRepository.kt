package com.antigravity.somnolog.domain.repository

import com.antigravity.somnolog.domain.model.AudioEvent
import com.antigravity.somnolog.domain.model.SleepSession
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for sleep monitoring sessions and audio events.
 */
interface SleepRepository {
    fun getAllSessions(): Flow<List<SleepSession>>
    suspend fun getSessionById(sessionId: Long): SleepSession?
    suspend fun getActiveSession(): SleepSession?
    suspend fun startSession(baselineDb: Float, thresholdDb: Float): Long
    suspend fun endSession(sessionId: Long)
    suspend fun deleteSession(sessionId: Long)
    fun getEventsForSession(sessionId: Long): Flow<List<AudioEvent>>
    fun getAllEvents(): Flow<List<AudioEvent>>
    suspend fun saveAudioEvent(event: AudioEvent): Long
    suspend fun deleteAudioEvent(event: AudioEvent)
}
