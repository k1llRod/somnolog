package com.antigravity.somnolog.ui.player

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.antigravity.somnolog.domain.model.AudioEvent
import com.antigravity.somnolog.ui.theme.CyanAccent
import com.antigravity.somnolog.ui.theme.DarkCardBorder
import com.antigravity.somnolog.ui.theme.DarkSurface
import com.antigravity.somnolog.ui.theme.DarkSurfaceVariant
import com.antigravity.somnolog.ui.theme.OledBlack
import com.antigravity.somnolog.ui.theme.PurplePrimary
import com.antigravity.somnolog.ui.theme.RedRecording
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipPlayerSheet(
    clip: AudioEvent,
    playerManager: AudioPlayerManager,
    onDismiss: () -> Unit,
    onDelete: (AudioEvent) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPosition by playerManager.currentPositionMs.collectAsState()
    val duration by playerManager.durationMs.collectAsState()
    val playbackSpeed by playerManager.playbackSpeed.collectAsState()

    val totalDuration = if (duration > 0) duration else clip.durationMs
    val progress = if (totalDuration > 0) (currentPosition.toFloat() / totalDuration).coerceIn(0f, 1f) else 0f

    // Generate pseudo-waveform bars seeded by clip id and peak dB
    val waveformBars = remember(clip.id) {
        val random = Random(clip.id)
        List(40) {
            val normalizedPeak = (clip.peakDb / 100f).coerceIn(0.2f, 1f)
            (random.nextFloat() * 0.7f + 0.3f) * normalizedPeak
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title and Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Grabación Nocturna",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy • HH:mm:ss", Locale.getDefault()).format(Date(clip.timestamp)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    // Share clip button
                    IconButton(
                        onClick = {
                            val file = File(clip.filePath)
                            if (file.exists()) {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/mp4"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir clip de audio"))
                                } catch (_: Exception) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/*"
                                        putExtra(Intent.EXTRA_TEXT, "Clip de audio SomnoLog: ${clip.filePath}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir audio"))
                                }
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Compartir", tint = CyanAccent)
                    }

                    // Delete button
                    IconButton(
                        onClick = {
                            onDelete(clip)
                            onDismiss()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = RedRecording)
                    }

                    // Close button
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Peak badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nivel de pico: ",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "%.1f dB".format(clip.peakDb),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Interactive Waveform visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OledBlack)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    val totalBars = waveformBars.size
                    val spacing = 3.dp.toPx()
                    val totalSpacing = spacing * (totalBars - 1)
                    val barWidth = (size.width - totalSpacing) / totalBars
                    val currentBarProgress = progress * totalBars

                    for (i in 0 until totalBars) {
                        val barHeight = size.height * waveformBars[i]
                        val x = i * (barWidth + spacing)
                        val y = (size.height - barHeight) / 2f

                        val isPlayed = i <= currentBarProgress
                        val color = if (isPlayed) PurplePrimary else Color(0xFF2C2E40)

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slider & Time Display
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { playerManager.seekTo(it.toLong()) },
                valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = PurplePrimary,
                    activeTrackColor = PurplePrimary,
                    inactiveTrackColor = DarkSurfaceVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(currentPosition),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatMs(totalDuration),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Seek -5s
                IconButton(
                    onClick = { playerManager.seekTo((currentPosition - 5000L).coerceAtLeast(0L)) }
                ) {
                    Icon(imageVector = Icons.Default.FastRewind, contentDescription = "-5s", tint = MaterialTheme.colorScheme.onSurface)
                }

                // Play / Pause central button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary)
                        .clickable { playerManager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Seek +5s
                IconButton(
                    onClick = { playerManager.seekTo((currentPosition + 5000L).coerceAtMost(totalDuration)) }
                ) {
                    Icon(imageVector = Icons.Default.FastForward, contentDescription = "+5s", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speed Selector (1.0x, 1.25x, 1.5x, 2.0x)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    val isSelected = playbackSpeed == speed
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) PurplePrimary else Color.Transparent)
                            .clickable { playerManager.setSpeed(speed) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${speed}x",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
