package com.antigravity.somnolog.service

/**
 * Thread-safe Circular Audio Buffer (Ring Buffer) designed for pre-trigger caching.
 *
 * Stores raw 16-bit PCM samples in memory. When sound triggers above the threshold,
 * the buffer provides the preceding ~1.5 to 2.0 seconds of audio so no speech onset
 * or initial syllables are lost.
 *
 * @param capacityInSamples Total number of 16-bit samples to keep (e.g. 16000 Hz * 2s = 32000 samples).
 */
class CircularAudioBuffer(val capacityInSamples: Int) {

    private val buffer = ShortArray(capacityInSamples)
    private var writeIndex = 0
    private var isFull = false
    private val lock = Any()

    /**
     * Writes incoming PCM 16-bit samples into the circular buffer.
     * Older samples are overwritten once capacity is reached.
     */
    fun write(samples: ShortArray, offset: Int = 0, count: Int = samples.size) {
        if (count <= 0 || capacityInSamples <= 0) return

        synchronized(lock) {
            var currentOffset = offset
            var remaining = count

            while (remaining > 0) {
                val spaceToEnd = capacityInSamples - writeIndex
                val toWrite = minOf(remaining, spaceToEnd)

                System.arraycopy(samples, currentOffset, buffer, writeIndex, toWrite)

                writeIndex = (writeIndex + toWrite) % capacityInSamples
                if (!isFull && (writeIndex == 0 || toWrite == spaceToEnd)) {
                    isFull = true
                }

                currentOffset += toWrite
                remaining -= toWrite
            }
        }
    }

    /**
     * Returns a snapshot of all buffered audio up to this point in chronological order (oldest to newest).
     * This provides the pre-trigger audio for the encoder.
     */
    fun getSnapshot(): ShortArray {
        synchronized(lock) {
            val totalAvailable = if (isFull) capacityInSamples else writeIndex
            if (totalAvailable == 0) return ShortArray(0)

            val result = ShortArray(totalAvailable)

            if (!isFull) {
                // Buffer hasn't wrapped around yet; simple copy from 0 to writeIndex
                System.arraycopy(buffer, 0, result, 0, writeIndex)
            } else {
                // Buffer has wrapped around:
                // Oldest samples are from writeIndex to end
                val firstPartSize = capacityInSamples - writeIndex
                System.arraycopy(buffer, writeIndex, result, 0, firstPartSize)

                // Newer samples are from 0 to writeIndex
                if (writeIndex > 0) {
                    System.arraycopy(buffer, 0, result, firstPartSize, writeIndex)
                }
            }

            return result
        }
    }

    /**
     * Current number of valid samples stored in the buffer.
     */
    val currentSize: Int
        get() = synchronized(lock) {
            if (isFull) capacityInSamples else writeIndex
        }

    /**
     * Clears the buffer.
     */
    fun clear() {
        synchronized(lock) {
            writeIndex = 0
            isFull = false
            buffer.fill(0)
        }
    }
}
