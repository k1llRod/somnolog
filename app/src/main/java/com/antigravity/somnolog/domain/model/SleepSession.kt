package com.antigravity.somnolog.domain.model

/**
 * Domain model representing an overnight sleep monitoring session.
 */
data class SleepSession(
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val clipCount: Int = 0,
    val baselineNoiseDb: Float = 40.0f,
    val triggerThresholdDb: Float = 55.0f
) {
    val isActive: Boolean get() = endTime == null
}
