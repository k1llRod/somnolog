package com.antigravity.somnolog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.antigravity.somnolog.data.local.entity.AudioEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioEvent(event: AudioEventEntity): Long

    @Query("SELECT * FROM audio_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getEventsForSession(sessionId: Long): Flow<List<AudioEventEntity>>

    @Query("SELECT * FROM audio_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<AudioEventEntity>>

    @Query("SELECT * FROM audio_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): AudioEventEntity?

    @Query("SELECT COUNT(*) FROM audio_events WHERE sessionId = :sessionId")
    suspend fun countEventsForSession(sessionId: Long): Int

    @Update
    suspend fun updateAudioEvent(event: AudioEventEntity)

    @Delete
    suspend fun deleteAudioEvent(event: AudioEventEntity)
}
