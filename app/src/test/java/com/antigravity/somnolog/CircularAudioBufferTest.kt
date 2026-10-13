package com.antigravity.somnolog

import com.antigravity.somnolog.service.CircularAudioBuffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CircularAudioBufferTest {

    @Test
    fun `buffer stores samples without wrapping when under capacity`() {
        val buffer = CircularAudioBuffer(capacityInSamples = 10)
        val input = shortArrayOf(1, 2, 3, 4, 5)

        buffer.write(input)

        assertEquals(5, buffer.currentSize)
        assertArrayEquals(shortArrayOf(1, 2, 3, 4, 5), buffer.getSnapshot())
    }

    @Test
    fun `buffer wraps around and maintains chronological order`() {
        val buffer = CircularAudioBuffer(capacityInSamples = 5)

        // Write 3 samples: [1, 2, 3]
        buffer.write(shortArrayOf(1, 2, 3))
        assertEquals(3, buffer.currentSize)
        assertArrayEquals(shortArrayOf(1, 2, 3), buffer.getSnapshot())

        // Write 4 more samples: [4, 5, 6, 7]
        // Capacity is 5, so buffer should contain the last 5 samples in chronological order: [3, 4, 5, 6, 7]
        buffer.write(shortArrayOf(4, 5, 6, 7))

        assertEquals(5, buffer.currentSize)
        assertArrayEquals(shortArrayOf(3, 4, 5, 6, 7), buffer.getSnapshot())
    }

    @Test
    fun `buffer correctly overwrites multiple full loops`() {
        val buffer = CircularAudioBuffer(capacityInSamples = 4)

        // Write 12 samples sequentially
        val input = ShortArray(12) { (it + 1).toShort() } // 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12
        buffer.write(input)

        assertEquals(4, buffer.capacityInSamples)
        assertEquals(4, buffer.currentSize)
        // Should keep last 4 samples: 9, 10, 11, 12
        assertArrayEquals(shortArrayOf(9, 10, 11, 12), buffer.getSnapshot())
    }

    @Test
    fun `clear resets the buffer state`() {
        val buffer = CircularAudioBuffer(capacityInSamples = 5)
        buffer.write(shortArrayOf(10, 20, 30))

        assertEquals(3, buffer.currentSize)
        buffer.clear()

        assertEquals(0, buffer.currentSize)
        assertEquals(0, buffer.getSnapshot().size)
    }
}
