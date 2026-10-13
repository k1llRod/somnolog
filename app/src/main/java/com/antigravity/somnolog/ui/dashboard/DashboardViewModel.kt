package com.antigravity.somnolog.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.somnolog.SomnoLogApp
import com.antigravity.somnolog.service.AudioMonitoringService
import com.antigravity.somnolog.service.AudioRecordManager
import com.antigravity.somnolog.domain.usecase.CalculateRmsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel : ViewModel() {

    private val repository = SomnoLogApp.instance.repository
    private val calculateRmsUseCase = CalculateRmsUseCase()

    val serviceState = AudioMonitoringService.serviceState
    val currentDb = AudioMonitoringService.currentDb
    val sessionClipCount = AudioMonitoringService.sessionClipCount
    val lastEventPeakDb = AudioMonitoringService.lastEventPeakDb

    // Baseline noise and trigger sensitivity
    private val _baselineNoiseDb = MutableStateFlow(38.0f)
    val baselineNoiseDb: StateFlow<Float> = _baselineNoiseDb.asStateFlow()

    private val _thresholdDeltaDb = MutableStateFlow(12.0f)
    val thresholdDeltaDb: StateFlow<Float> = _thresholdDeltaDb.asStateFlow()

    val triggerThresholdDb: Float
        get() = _baselineNoiseDb.value + _thresholdDeltaDb.value

    // Interactive Calibration state
    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()

    private val _calibrationProgress = MutableStateFlow(0f)
    val calibrationProgress: StateFlow<Float> = _calibrationProgress.asStateFlow()

    private var activeSessionId: Long? = null
    private var calibrationJob: Job? = null

    init {
        viewModelScope.launch {
            val active = repository.getActiveSession()
            if (active != null) {
                activeSessionId = active.id
                _baselineNoiseDb.value = active.baselineNoiseDb
                _thresholdDeltaDb.value = (active.triggerThresholdDb - active.baselineNoiseDb).coerceAtLeast(5f)
            }
        }
    }

    fun setThresholdDelta(delta: Float) {
        _thresholdDeltaDb.value = delta.coerceIn(5f, 30f)
    }

    fun setBaselineNoise(baseline: Float) {
        _baselineNoiseDb.value = baseline.coerceIn(20f, 70f)
    }

    /**
     * Calibrates ambient bedroom noise over 3 seconds to establish the baseline.
     */
    fun startAmbientCalibration() {
        if (_isCalibrating.value || serviceState.value != AudioMonitoringService.MonitoringState.IDLE) return

        _isCalibrating.value = true
        _calibrationProgress.value = 0f

        calibrationJob = viewModelScope.launch(Dispatchers.IO) {
            val samplesCollector = mutableListOf<Float>()
            val recorder = AudioRecordManager(sampleRate = 16000)

            recorder.startListening { samples, count ->
                val db = calculateRmsUseCase.calculateNormalizedDb(samples, count)
                samplesCollector.add(db)
            }

            val totalSteps = 30 // 3 seconds total
            for (step in 1..totalSteps) {
                if (!isActive) break
                delay(100)
                _calibrationProgress.value = step.toFloat() / totalSteps
            }

            recorder.stopListening()

            if (samplesCollector.isNotEmpty()) {
                val sorted = samplesCollector.sorted()
                // Use 40th percentile to avoid transient spikes inflating the floor
                val p40Index = (sorted.size * 0.40).toInt().coerceIn(0, sorted.size - 1)
                val calculatedBaseline = sorted[p40Index].coerceIn(25f, 65f)

                withContext(Dispatchers.Main) {
                    _baselineNoiseDb.value = calculatedBaseline
                }
            }

            _isCalibrating.value = false
            _calibrationProgress.value = 0f
        }
    }

    fun startMonitoring(context: Context) {
        viewModelScope.launch {
            val sessionId = repository.startSession(
                baselineDb = _baselineNoiseDb.value,
                thresholdDb = triggerThresholdDb
            )
            activeSessionId = sessionId

            val intent = Intent(context, AudioMonitoringService::class.java).apply {
                action = AudioMonitoringService.ACTION_START
                putExtra(AudioMonitoringService.EXTRA_SESSION_ID, sessionId)
                putExtra(AudioMonitoringService.EXTRA_BASELINE_DB, _baselineNoiseDb.value)
                putExtra(AudioMonitoringService.EXTRA_THRESHOLD_DB, triggerThresholdDb)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun stopMonitoring(context: Context) {
        viewModelScope.launch {
            activeSessionId?.let { id ->
                repository.endSession(id)
                activeSessionId = null
            }

            val intent = Intent(context, AudioMonitoringService::class.java).apply {
                action = AudioMonitoringService.ACTION_STOP
            }
            context.startService(intent)
        }
    }

    fun pauseMonitoring(context: Context) {
        val intent = Intent(context, AudioMonitoringService::class.java).apply {
            action = AudioMonitoringService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeMonitoring(context: Context) {
        val intent = Intent(context, AudioMonitoringService::class.java).apply {
            action = AudioMonitoringService.ACTION_RESUME
        }
        context.startService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        calibrationJob?.cancel()
    }
}
