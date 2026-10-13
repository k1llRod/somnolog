package com.antigravity.somnolog.domain.usecase

import com.antigravity.somnolog.domain.model.AudioEvent
import com.antigravity.somnolog.domain.repository.SleepRepository
import java.io.File

/**
 * UseCase to handle clip validation and persistence according to technical rules:
 * - Minimum duration: 0.8 seconds (discard if shorter to avoid mechanical clicks/pillow friction)
 * - Persist valid clip metadata to database and update session count
 */
class SaveClipUseCase(private val repository: SleepRepository) {

    companion object {
        const val MIN_CLIP_DURATION_MS = 800L
        const val MAX_CLIP_DURATION_MS = 60000L
    }

    suspend operator fun invoke(
        sessionId: Long,
        timestamp: Long,
        durationMs: Long,
        peakDb: Float,
        filePath: String,
        note: String? = null
    ): AudioEvent? {
        val audioFile = File(filePath)

        // Rule: Discard clips shorter than 0.8s
        if (durationMs < MIN_CLIP_DURATION_MS) {
            if (audioFile.exists()) {
                audioFile.delete()
            }
            return null
        }

        // Clamp duration if needed
        val finalDuration = minOf(durationMs, MAX_CLIP_DURATION_MS)

        val event = AudioEvent(
            sessionId = sessionId,
            timestamp = timestamp,
            durationMs = finalDuration,
            peakDb = peakDb,
            filePath = filePath,
            note = note
        )

        val id = repository.saveAudioEvent(event)
        return event.copy(id = id)
    }
}
