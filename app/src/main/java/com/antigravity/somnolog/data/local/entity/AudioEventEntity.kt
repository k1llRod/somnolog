package com.antigravity.somnolog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.antigravity.somnolog.domain.model.AudioEvent

@Entity(
    tableName = "audio_events",
    foreignKeys = [
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class AudioEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val durationMs: Long,
    val peakDb: Float,
    val filePath: String,
    val note: String? = null
) {
    fun toDomain(): AudioEvent = AudioEvent(
        id = id,
        sessionId = sessionId,
        timestamp = timestamp,
        durationMs = durationMs,
        peakDb = peakDb,
        filePath = filePath,
        note = note
    )

    companion object {
        fun fromDomain(event: AudioEvent): AudioEventEntity = AudioEventEntity(
            id = event.id,
            sessionId = event.sessionId,
            timestamp = event.timestamp,
            durationMs = event.durationMs,
            peakDb = event.peakDb,
            filePath = event.filePath,
            note = event.note
        )
    }
}
