package com.antigravity.somnolog.service

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AAC-LC Audio Encoder using Android native MediaCodec and MediaMuxer.
 *
 * Encodes 16-bit PCM audio stream into standard .m4a (AAC-LC) containers
 * at 48 kbps mono, maximizing speech clarity while keeping disk usage minimal.
 */
class AudioEncoder(
    private val sampleRate: Int = 16000,
    private val channelCount: Int = 1,
    private val bitRate: Int = 48000
) {

    companion object {
        private const val TAG = "AudioEncoder"
        private const val MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC
        private const val TIMEOUT_USEC = 10000L
    }

    private var codec: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var trackIndex = -1
    private var isMuxerStarted = false
    private var isRunning = false
    private var presentationTimeUs = 0L

    private val lock = Any()

    /**
     * Initializes and starts the AAC encoder and MediaMuxer pointing to the target file.
     */
    fun start(outputFile: File) {
        synchronized(lock) {
            if (isRunning) return

            try {
                // Prepare MediaFormat for AAC-LC
                val format = MediaFormat.createAudioFormat(MIME_TYPE, sampleRate, channelCount).apply {
                    setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                    setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                    setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
                }

                // Initialize Codec
                codec = MediaCodec.createEncoderByType(MIME_TYPE).apply {
                    configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    start()
                }

                // Initialize Muxer (.m4a container)
                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                trackIndex = -1
                isMuxerStarted = false
                presentationTimeUs = 0L
                isRunning = true

                Log.d(TAG, "AudioEncoder started for file: ${outputFile.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing AudioEncoder", e)
                release()
                throw e
            }
        }
    }

    /**
     * Feeds PCM 16-bit samples into the encoder.
     */
    fun encode(samples: ShortArray, offset: Int = 0, count: Int = samples.size) {
        synchronized(lock) {
            if (!isRunning || codec == null) return

            val localCodec = codec ?: return

            // Convert ShortArray to Little Endian ByteBuffer
            val byteCount = count * 2
            val byteBuffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.LITTLE_ENDIAN)
            for (i in offset until (offset + count)) {
                byteBuffer.putShort(samples[i])
            }
            byteBuffer.flip()

            var inputOffset = 0
            while (inputOffset < byteCount && isRunning) {
                val inputBufferIndex = localCodec.dequeueInputBuffer(TIMEOUT_USEC)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = localCodec.getInputBuffer(inputBufferIndex) ?: continue
                    inputBuffer.clear()

                    val bytesToWrite = minOf(byteCount - inputOffset, inputBuffer.remaining())
                    val chunk = ByteArray(bytesToWrite)
                    byteBuffer.get(chunk, 0, bytesToWrite)
                    inputBuffer.put(chunk)

                    // Presentation time in microseconds: (sampleCount * 1_000_000) / sampleRate
                    val samplesInChunk = bytesToWrite / 2
                    val durationUs = (samplesInChunk * 1_000_000L) / sampleRate

                    localCodec.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        bytesToWrite,
                        presentationTimeUs,
                        0
                    )

                    presentationTimeUs += durationUs
                    inputOffset += bytesToWrite
                }

                drainEncoder(endOfStream = false)
            }
        }
    }

    /**
     * Finalizes encoding, drains any remaining frames with EOS, and closes the muxer and codec.
     */
    fun stop() {
        synchronized(lock) {
            if (!isRunning) return
            isRunning = false

            val localCodec = codec
            if (localCodec != null) {
                try {
                    // Send End of Stream buffer
                    val inputBufferIndex = localCodec.dequeueInputBuffer(TIMEOUT_USEC)
                    if (inputBufferIndex >= 0) {
                        localCodec.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            0,
                            presentationTimeUs,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                    }
                    drainEncoder(endOfStream = true)
                } catch (e: Exception) {
                    Log.w(TAG, "Exception during EOS drain: ${e.message}")
                }
            }

            release()
            Log.d(TAG, "AudioEncoder stopped successfully")
        }
    }

    /**
     * Drains output buffers from MediaCodec and writes to MediaMuxer.
     */
    private fun drainEncoder(endOfStream: Boolean) {
        val localCodec = codec ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        while (true) {
            val outputBufferIndex = localCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)

            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                }
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (isMuxerStarted) {
                        Log.e(TAG, "Format changed after muxer started")
                    } else {
                        val newFormat = localCodec.outputFormat
                        muxer?.let { m ->
                            trackIndex = m.addTrack(newFormat)
                            m.start()
                            isMuxerStarted = true
                            Log.d(TAG, "MediaMuxer started with track index: $trackIndex")
                        }
                    }
                }
                outputBufferIndex >= 0 -> {
                    val outputBuffer = localCodec.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size > 0 && isMuxerStarted) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer?.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                        }

                        localCodec.releaseOutputBuffer(outputBufferIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }
            }
        }
    }

    private fun release() {
        try {
            codec?.stop()
        } catch (_: Exception) {}
        try {
            codec?.release()
        } catch (_: Exception) {}
        codec = null

        try {
            if (isMuxerStarted) {
                muxer?.stop()
            }
        } catch (_: Exception) {}
        try {
            muxer?.release()
        } catch (_: Exception) {}
        muxer = null
        isMuxerStarted = false
    }
}
