package com.antigravity.somnolog.service

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Manages the native AudioRecord instance and real-time audio capture loop.
 *
 * Runs on a dedicated background thread with audio priority to capture 16-bit PCM samples
 * at 16000 Hz mono continuously with minimal CPU and battery consumption.
 */
class AudioRecordManager(
    val sampleRate: Int = 16000,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {

    companion object {
        private const val TAG = "AudioRecordManager"
        // 100ms chunk at 16000 Hz = 1600 samples
        const val CHUNK_SIZE_SAMPLES = 1600
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    var isListening: Boolean = false
        private set

    /**
     * Starts continuous audio capture. Audio chunks (100ms PCM shorts) are dispatched
     * to the provided [onAudioChunk] callback.
     */
    @SuppressLint("MissingPermission")
    fun startListening(onAudioChunk: (samples: ShortArray, readCount: Int) -> Unit) {
        if (isListening) return

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid AudioRecord buffer size: $minBufferSize")
                return
            }

            val bufferSize = max(minBufferSize * 2, CHUNK_SIZE_SAMPLES * 2)

            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord could not be initialized")
                record.release()
                return
            }

            audioRecord = record
            record.startRecording()
            isListening = true

            recordingJob = scope.launch {
                // Set audio thread priority for low latency and zero dropouts
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

                val readBuffer = ShortArray(CHUNK_SIZE_SAMPLES)

                try {
                    while (isActive && isListening) {
                        val readCount = audioRecord?.read(readBuffer, 0, CHUNK_SIZE_SAMPLES) ?: -1

                        if (readCount > 0) {
                            onAudioChunk(readBuffer, readCount)
                        } else if (readCount < 0) {
                            Log.w(TAG, "AudioRecord read error: $readCount")
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in audio recording loop", e)
                } finally {
                    stopListeningInternal()
                }
            }

            Log.d(TAG, "Audio capture loop started successfully ($sampleRate Hz)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord", e)
            stopListeningInternal()
        }
    }

    /**
     * Stops audio capture and frees native resources.
     */
    fun stopListening() {
        isListening = false
        recordingJob?.cancel()
        recordingJob = null
        stopListeningInternal()
    }

    private fun stopListeningInternal() {
        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
            isListening = false
        }
    }
}
