package com.antigravity.somnolog.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.antigravity.somnolog.MainActivity
import com.antigravity.somnolog.R
import com.antigravity.somnolog.data.local.AppDatabase
import com.antigravity.somnolog.data.repository.SleepRepositoryImpl
import com.antigravity.somnolog.domain.usecase.CalculateRmsUseCase
import com.antigravity.somnolog.domain.usecase.SaveClipUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Foreground Service responsible for continuous nighttime audio monitoring.
 *
 * Runs with foregroundServiceType="microphone" and maintains a PARTIAL_WAKE_LOCK to prevent
 * CPU deep sleep while keeping battery consumption minimal.
 */
class AudioMonitoringService : Service() {

    enum class MonitoringState {
        IDLE,
        MONITORING,
        RECORDING,
        PAUSED
    }

    companion object {
        private const val TAG = "AudioMonitoringService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "somnolog_monitoring_channel"

        const val ACTION_START = "com.antigravity.somnolog.ACTION_START"
        const val ACTION_STOP = "com.antigravity.somnolog.ACTION_STOP"
        const val ACTION_PAUSE = "com.antigravity.somnolog.ACTION_PAUSE"
        const val ACTION_RESUME = "com.antigravity.somnolog.ACTION_RESUME"

        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_BASELINE_DB = "extra_baseline_db"
        const val EXTRA_THRESHOLD_DB = "extra_threshold_db"

        // Rule parameters:
        // 2 seconds pre-trigger buffer at 16000 Hz = 32000 samples
        private const val SAMPLE_RATE = 16000
        private const val PRE_TRIGGER_SECONDS = 2
        private const val SILENCE_TIMEOUT_MS = 2800L    // 2.8s continuous silence ends clip
        private const val MAX_CLIP_DURATION_MS = 60000L // 60s max clip duration

        // Observables for Compose UI
        private val _serviceState = MutableStateFlow(MonitoringState.IDLE)
        val serviceState: StateFlow<MonitoringState> = _serviceState.asStateFlow()

        private val _currentDb = MutableStateFlow(30.0f)
        val currentDb: StateFlow<Float> = _currentDb.asStateFlow()

        private val _sessionClipCount = MutableStateFlow(0)
        val sessionClipCount: StateFlow<Int> = _sessionClipCount.asStateFlow()

        private val _lastEventPeakDb = MutableStateFlow<Float?>(null)
        val lastEventPeakDb: StateFlow<Float?> = _lastEventPeakDb.asStateFlow()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var audioRecordManager: AudioRecordManager
    private lateinit var circularBuffer: CircularAudioBuffer
    private lateinit var calculateRmsUseCase: CalculateRmsUseCase
    private lateinit var saveClipUseCase: SaveClipUseCase
    private var audioEncoder: AudioEncoder? = null

    private var currentSessionId: Long = 0L
    private var triggerThresholdDb: Float = 55.0f
    private var baselineDb: Float = 40.0f

    // Trigger state tracking
    private var isTriggerActive = false
    private var currentRecordingFile: File? = null
    private var recordingStartTime = 0L
    private var silenceDurationMs = 0L
    private var clipPeakDb = 0.0f

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(applicationContext)
        val repository = SleepRepositoryImpl(database)
        saveClipUseCase = SaveClipUseCase(repository)
        calculateRmsUseCase = CalculateRmsUseCase()

        circularBuffer = CircularAudioBuffer(SAMPLE_RATE * PRE_TRIGGER_SECONDS)
        audioRecordManager = AudioRecordManager(sampleRate = SAMPLE_RATE)

        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentSessionId = intent.getLongExtra(EXTRA_SESSION_ID, 0L)
                baselineDb = intent.getFloatExtra(EXTRA_BASELINE_DB, 40.0f)
                triggerThresholdDb = intent.getFloatExtra(EXTRA_THRESHOLD_DB, 55.0f)
                _sessionClipCount.value = 0
                _lastEventPeakDb.value = null

                startForegroundServiceCompat()
                startMonitoring()
            }
            ACTION_PAUSE -> pauseMonitoring()
            ACTION_RESUME -> resumeMonitoring()
            ACTION_STOP -> stopMonitoringAndSelf()
        }
        return START_STICKY
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SomnoLog:AudioMonitoringWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
            Log.d(TAG, "PARTIAL_WAKE_LOCK acquired")
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "PARTIAL_WAKE_LOCK released")
            }
        }
        wakeLock = null
    }

    private fun startForegroundServiceCompat() {
        val notification = buildNotification(
            title = getString(R.string.service_monitoring_title),
            content = getString(R.string.service_monitoring_text),
            isRecording = false
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startMonitoring() {
        _serviceState.value = MonitoringState.MONITORING
        circularBuffer.clear()
        isTriggerActive = false
        silenceDurationMs = 0L

        audioRecordManager.startListening { samples, readCount ->
            processAudioChunk(samples, readCount)
        }
    }

    private fun pauseMonitoring() {
        if (_serviceState.value == MonitoringState.RECORDING) {
            finishCurrentRecording()
        }
        audioRecordManager.stopListening()
        _serviceState.value = MonitoringState.PAUSED
        updateNotification("En pausa", false)
    }

    private fun resumeMonitoring() {
        if (_serviceState.value == MonitoringState.PAUSED) {
            startMonitoring()
            updateNotification(getString(R.string.service_monitoring_text), false)
        }
    }

    private fun stopMonitoringAndSelf() {
        if (isTriggerActive) {
            finishCurrentRecording()
        }
        audioRecordManager.stopListening()
        _serviceState.value = MonitoringState.IDLE
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Core real-time audio pipeline:
     * 1. Feed circular ring buffer (for pre-trigger capture).
     * 2. Compute RMS & dB.
     * 3. Evaluate threshold and trigger state machine.
     */
    private fun processAudioChunk(samples: ShortArray, readCount: Int) {
        // 1. Maintain pre-trigger RAM circular buffer
        circularBuffer.write(samples, 0, readCount)

        // 2. Calculate RMS & decibel level
        val dbLevel = calculateRmsUseCase.calculateNormalizedDb(samples, readCount)
        _currentDb.value = dbLevel

        val chunkDurationMs = (readCount * 1000L) / SAMPLE_RATE

        if (!isTriggerActive) {
            // Check trigger: RMS > threshold dB
            if (dbLevel >= triggerThresholdDb) {
                startRecordingTrigger(samples, readCount, dbLevel)
            }
        } else {
            // Ongoing recording
            clipPeakDb = maxOf(clipPeakDb, dbLevel)

            // Feed live chunk into AAC encoder
            audioEncoder?.encode(samples, 0, readCount)

            val currentClipDuration = System.currentTimeMillis() - recordingStartTime

            // Silence detection logic
            if (dbLevel < triggerThresholdDb) {
                silenceDurationMs += chunkDurationMs
            } else {
                silenceDurationMs = 0L
            }

            // Check if recording should terminate:
            // Condition 1: Continuous silence >= 2.8 seconds
            // Condition 2: Max clip duration >= 60 seconds
            if (silenceDurationMs >= SILENCE_TIMEOUT_MS || currentClipDuration >= MAX_CLIP_DURATION_MS) {
                finishCurrentRecording()
            }
        }
    }

    private fun startRecordingTrigger(
        triggerChunk: ShortArray,
        triggerChunkCount: Int,
        initialDb: Float
    ) {
        try {
            isTriggerActive = true
            _serviceState.value = MonitoringState.RECORDING
            recordingStartTime = System.currentTimeMillis()
            silenceDurationMs = 0L
            clipPeakDb = initialDb

            // Prepare output file in app's private files directory
            val recordingsDir = File(filesDir, "recordings").apply { mkdirs() }
            val outputFile = File(recordingsDir, "clip_${currentSessionId}_$recordingStartTime.m4a")
            currentRecordingFile = outputFile

            // Initialize AAC-LC Encoder
            val encoder = AudioEncoder(sampleRate = SAMPLE_RATE, channelCount = 1, bitRate = 48000)
            encoder.start(outputFile)
            audioEncoder = encoder

            // Dump PRE-TRIGGER RAM Buffer to file (preserves prior ~1.5 - 2s of speech)
            val preTriggerSamples = circularBuffer.getSnapshot()
            if (preTriggerSamples.isNotEmpty()) {
                encoder.encode(preTriggerSamples, 0, preTriggerSamples.size)
            }

            // Encode current triggering chunk
            encoder.encode(triggerChunk, 0, triggerChunkCount)

            updateNotification(getString(R.string.service_recording_text), isRecording = true)
            Log.d(TAG, "Audio trigger ACTIVATED! Level: $initialDb dB >= $triggerThresholdDb dB")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording trigger", e)
            isTriggerActive = false
            _serviceState.value = MonitoringState.MONITORING
        }
    }

    private fun finishCurrentRecording() {
        if (!isTriggerActive) return
        isTriggerActive = false

        val file = currentRecordingFile
        val durationMs = System.currentTimeMillis() - recordingStartTime
        val peak = clipPeakDb

        try {
            audioEncoder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio encoder", e)
        } finally {
            audioEncoder = null
        }

        _serviceState.value = MonitoringState.MONITORING
        updateNotification(getString(R.string.service_monitoring_text), isRecording = false)

        if (file != null) {
            serviceScope.launch {
                val savedEvent = saveClipUseCase(
                    sessionId = currentSessionId,
                    timestamp = recordingStartTime,
                    durationMs = durationMs,
                    peakDb = peak,
                    filePath = file.absolutePath
                )

                if (savedEvent != null) {
                    _sessionClipCount.value += 1
                    _lastEventPeakDb.value = peak
                    Log.d(TAG, "Clip saved: duration=${durationMs}ms, peak=${peak}dB")
                } else {
                    Log.d(TAG, "Clip discarded (shorter than 0.8s): ${durationMs}ms")
                }
            }
        }

        currentRecordingFile = null
        silenceDurationMs = 0L
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String, isRecording: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioMonitoringService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.action_stop),
                stopPendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(if (isRecording) 0xFFFF5252.toInt() else 0xFF7C4DFF.toInt())
            .build()
    }

    private fun updateNotification(content: String, isRecording: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification(
            title = getString(R.string.service_monitoring_title),
            content = content,
            isRecording = isRecording
        )
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (isTriggerActive) {
            finishCurrentRecording()
        }
        audioRecordManager.stopListening()
        releaseWakeLock()
        serviceScope.cancel()
        _serviceState.value = MonitoringState.IDLE
        Log.d(TAG, "AudioMonitoringService destroyed")
    }
}
