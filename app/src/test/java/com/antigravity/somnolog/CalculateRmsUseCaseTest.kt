package com.antigravity.somnolog

import com.antigravity.somnolog.domain.usecase.CalculateRmsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateRmsUseCaseTest {

    private val calculateRmsUseCase = CalculateRmsUseCase()

    @Test
    fun `rms of silence is zero`() {
        val silence = ShortArray(100) { 0 }
        val rms = calculateRmsUseCase.calculateRms(silence)
        assertEquals(0.0, rms, 0.001)

        val dbfs = calculateRmsUseCase.calculateDbFs(rms)
        assertEquals(-90.0, dbfs, 0.001)
    }

    @Test
    fun `rms of maximum amplitude is max pcm value`() {
        val maxSamples = ShortArray(100) { Short.MAX_VALUE }
        val rms = calculateRmsUseCase.calculateRms(maxSamples)
        assertEquals(32767.0, rms, 0.5)

        val dbfs = calculateRmsUseCase.calculateDbFs(rms)
        assertEquals(0.0, dbfs, 0.01)
    }

    @Test
    fun `rms of alternating square wave is equal to amplitude`() {
        val squareWave = ShortArray(100) { if (it % 2 == 0) 10000 else -10000 }
        val rms = calculateRmsUseCase.calculateRms(squareWave)
        assertEquals(10000.0, rms, 0.01)
    }

    @Test
    fun `normalized decibel calculation maps silence to lower bound and peaks to upper bound`() {
        val silence = ShortArray(1600) { 0 }
        val silenceDb = calculateRmsUseCase.calculateNormalizedDb(silence)
        assertEquals(0.0f, silenceDb, 0.01f)

        val maxPeak = ShortArray(1600) { Short.MAX_VALUE }
        val maxDb = calculateRmsUseCase.calculateNormalizedDb(maxPeak)
        assertEquals(90.0f, maxDb, 0.1f)
    }

    @Test
    fun `audio level invoke returns consistent structure`() {
        val samples = ShortArray(1000) { 5000 }
        val level = calculateRmsUseCase(samples)

        assertEquals(5000.0, level.rms, 0.1)
        assertTrue(level.dbfs < 0.0)
        assertTrue(level.decibels > 0.0f)
    }
}
