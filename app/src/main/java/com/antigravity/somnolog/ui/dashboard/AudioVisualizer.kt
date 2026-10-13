package com.antigravity.somnolog.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.somnolog.service.AudioMonitoringService
import com.antigravity.somnolog.ui.theme.CyanAccent
import com.antigravity.somnolog.ui.theme.GreenSafe
import com.antigravity.somnolog.ui.theme.PurplePrimary
import com.antigravity.somnolog.ui.theme.RedRecording
import com.antigravity.somnolog.ui.theme.AmberWarning
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated OLED decibel ring gauge with real-time level, threshold indicator, and status pulse.
 */
@Composable
fun AudioVisualizer(
    currentDb: Float,
    thresholdDb: Float,
    baselineDb: Float,
    monitoringState: AudioMonitoringService.MonitoringState,
    modifier: Modifier = Modifier
) {
    val animatedDb by animateFloatAsState(
        targetValue = currentDb.coerceIn(0f, 100f),
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "animatedDb"
    )

    val ringColor by animateColorAsState(
        targetValue = when {
            monitoringState == AudioMonitoringService.MonitoringState.RECORDING -> RedRecording
            animatedDb >= thresholdDb -> AmberWarning
            animatedDb > baselineDb + 5f -> CyanAccent
            else -> GreenSafe
        },
        animationSpec = tween(150),
        label = "ringColor"
    )

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth * 2
            val radius = diameter / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Start angle at 135 degrees (bottom left) to 405 degrees (bottom right), total 270 deg
            val startAngle = 135f
            val sweepTotal = 270f

            // Background inactive arc
            drawArc(
                color = Color(0xFF1E2030),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active level arc
            val activeSweep = (animatedDb / 100f) * sweepTotal
            if (activeSweep > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Threshold marker
            val thresholdRatio = (thresholdDb.coerceIn(0f, 100f)) / 100f
            val thresholdAngle = (startAngle + thresholdRatio * sweepTotal) * (PI / 180.0)
            val markerInnerRadius = radius - strokeWidth * 0.9f
            val markerOuterRadius = radius + strokeWidth * 0.9f

            val startMarker = Offset(
                x = (center.x + markerInnerRadius * cos(thresholdAngle)).toFloat(),
                y = (center.y + markerInnerRadius * sin(thresholdAngle)).toFloat()
            )
            val endMarker = Offset(
                x = (center.x + markerOuterRadius * cos(thresholdAngle)).toFloat(),
                y = (center.y + markerOuterRadius * sin(thresholdAngle)).toFloat()
            )

            drawLine(
                color = AmberWarning,
                start = startMarker,
                end = endMarker,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Center readout text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.1f".format(animatedDb),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "dB SPL",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (monitoringState) {
                    AudioMonitoringService.MonitoringState.RECORDING -> "● GRABANDO"
                    AudioMonitoringService.MonitoringState.MONITORING -> "ESCUCHANDO"
                    AudioMonitoringService.MonitoringState.PAUSED -> "PAUSADO"
                    AudioMonitoringService.MonitoringState.IDLE -> "INACTIVO"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = when (monitoringState) {
                    AudioMonitoringService.MonitoringState.RECORDING -> RedRecording
                    AudioMonitoringService.MonitoringState.MONITORING -> CyanAccent
                    AudioMonitoringService.MonitoringState.PAUSED -> AmberWarning
                    AudioMonitoringService.MonitoringState.IDLE -> PurplePrimary
                }
            )
        }
    }
}
