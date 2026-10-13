package com.antigravity.somnolog.domain.model

/**
 * Domain model representing an audio trigger recording event (sleep talk, snoring, noise).
 */
data class AudioEvent(
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val durationMs: Long,
    val peakDb: Float,
    val filePath: String,
    val note: String? = null
)
