package com.antigravity.somnolog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.antigravity.somnolog.domain.model.SleepSession

@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val clipCount: Int = 0,
    val baselineNoiseDb: Float = 40.0f,
    val triggerThresholdDb: Float = 55.0f
) {
    fun toDomain(): SleepSession = SleepSession(
        id = id,
        startTime = startTime,
        endTime = endTime,
        clipCount = clipCount,
        baselineNoiseDb = baselineNoiseDb,
        triggerThresholdDb = triggerThresholdDb
    )

    companion object {
        fun fromDomain(session: SleepSession): SleepSessionEntity = SleepSessionEntity(
            id = session.id,
            startTime = session.startTime,
            endTime = session.endTime,
            clipCount = session.clipCount,
            baselineNoiseDb = session.baselineNoiseDb,
            triggerThresholdDb = session.triggerThresholdDb
        )
    }
}
