package com.antigravity.somnolog.domain.usecase

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * UseCase for calculating Root Mean Square (RMS) and decibels (dBFS & normalized dB SPL).
 *
 * For 16-bit PCM mono audio, max positive amplitude is 32767.
 * RMS represents the quadratic mean of signal amplitudes over a window.
 */
class CalculateRmsUseCase {

    companion object {
        const val MAX_PCM_VALUE = 32767.0
        const val MIN_DBFS = -90.0
        const val DEFAULT_CALIBRATION_OFFSET = 90.0
    }

    /**
     * Calculates the Root Mean Square (RMS) of a 16-bit PCM audio sample array.
     * Returns a value between 0.0 and 32767.0.
     */
    fun calculateRms(samples: ShortArray, count: Int = samples.size): Double {
        if (count <= 0) return 0.0

        var sumSquares = 0.0
        for (i in 0 until count) {
            val sampleVal = samples[i].toDouble()
            sumSquares += sampleVal * sampleVal
        }

        val meanSquare = sumSquares / count
        return sqrt(meanSquare)
    }

    /**
     * Converts an RMS value to Decibels Full Scale (dBFS).
     * Output ranges from -90.0 dBFS (virtual silence) to 0.0 dBFS (peak clipping).
     */
    fun calculateDbFs(rms: Double): Double {
        if (rms <= 0.0) return MIN_DBFS
        val safeRms = max(rms, 1.0)
        val dbfs = 20.0 * log10(safeRms / MAX_PCM_VALUE)
        return max(MIN_DBFS, min(0.0, dbfs))
    }

    /**
     * Calculates a normalized ambient decibel scale (0 to 100 dB SPL estimate)
     * using the calibrated offset.
     *
     * Example:
     * - Silence / noise floor: ~30-40 dB
     * - Whispering / sleep talk: ~50-65 dB
     * - Loud snoring / speaking: ~70-85 dB
     */
    fun calculateNormalizedDb(
        samples: ShortArray,
        count: Int = samples.size,
        calibrationOffset: Double = DEFAULT_CALIBRATION_OFFSET
    ): Float {
        val rms = calculateRms(samples, count)
        val dbfs = calculateDbFs(rms)
        val normalized = dbfs + calibrationOffset
        return max(0.0, min(100.0, normalized)).toFloat()
    }

    /**
     * Convenience method to calculate both RMS and normalized dB in one pass.
     */
    operator fun invoke(samples: ShortArray, count: Int = samples.size): AudioLevel {
        val rms = calculateRms(samples, count)
        val dbfs = calculateDbFs(rms)
        val normalizedDb = max(0.0, min(100.0, dbfs + DEFAULT_CALIBRATION_OFFSET)).toFloat()
        return AudioLevel(
            rms = rms,
            dbfs = dbfs,
            decibels = normalizedDb
        )
    }

    data class AudioLevel(
        val rms: Double,
        val dbfs: Double,
        val decibels: Float
    )
}
