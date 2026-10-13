package com.antigravity.somnolog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.antigravity.somnolog.data.local.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SleepSessionEntity): Long

    @Update
    suspend fun updateSession(session: SleepSessionEntity)

    @Query("SELECT * FROM sleep_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SleepSessionEntity>>

    @Query("UPDATE sleep_sessions SET clipCount = clipCount + 1 WHERE id = :sessionId")
    suspend fun incrementClipCount(sessionId: Long)

    @Delete
    suspend fun deleteSession(session: SleepSessionEntity)
}
