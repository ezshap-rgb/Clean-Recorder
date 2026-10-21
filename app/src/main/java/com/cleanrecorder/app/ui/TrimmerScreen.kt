package com.cleanrecorder.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanrecorder.app.audio.AudioPlayerManager
import com.cleanrecorder.app.audio.AudioTrimmer
import com.cleanrecorder.app.data.Recording
import com.cleanrecorder.app.theme.AppColors
import com.cleanrecorder.app.theme.AppRadius
import com.cleanrecorder.app.theme.AppSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun TrimmerScreen(
    recording: Recording,
    playerManager: AudioPlayerManager,
    onFinished: () -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val totalDurationMs = remember { recording.durationMs.coerceAtLeast(1000L) }

    var inPointMs by remember { mutableLongStateOf(0L) }
    var outPointMs by remember { mutableLongStateOf(totalDurationMs) }
    var canvasWidthPx by remember { mutableFloatStateOf(1f) }
    var isTrimming by remember { mutableStateOf(false) }

    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPositionMs by playerManager.currentPositionMs.collectAsState()

    // Deterministic waveform envelope bars matching original React Native algorithm
    val waveformBars = remember(recording.name) {
        val count = 48
        val bars = mutableListOf<Float>()
        var hash = 0
        for (ch in recording.name) {
            hash = (hash * 31 + ch.code) ushr 0
        }
        for (i in 0 until count) {
            val angle = (i.toFloat() / count) * Math.PI * 6 + (hash % 10)
            val base = 0.28f + 0.5f * Math.abs(Math.sin(angle) * Math.cos(angle * 0.7)).toFloat()
            val variation = (((hash xor (i * 997)) % 100) / 100f) * 0.22f
            bars.add((base + variation).coerceIn(0.15f, 1f))
        }
        bars
    }

    val formatTime: (Long) -> String = { ms ->
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        String.format("%02d:%02d", mins, secs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(AppSpacing.lg),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                playerManager.stop()
                onCancel()
            }) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = AppColors.Text)
            }
            Text(
                text = "Audio Trimmer",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.Text
            )
            Button(
                onClick = {
                    if (isTrimming) return@Button
                    isTrimming = true
                    playerManager.stop()
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val tempDest = File(recording.file.parentFile, "trim_${System.currentTimeMillis()}.${recording.file.extension}")
                            try {
                                AudioTrimmer.trimLossless(
                                    sourceFile = recording.file,
                                    destinationFile = tempDest,
                                    inPointMs = inPointMs,
                                    outPointMs = outPointMs
                                )
                                recording.file.delete()
                                tempDest.renameTo(recording.file)
                            } catch (_: Exception) {}
                        }
                        isTrimming = false
                        onFinished()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
            ) {
                if (isTrimming) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AppColors.Text)
                } else {
                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text("Save Trim")
                }
            }
        }

        // Clip Info Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.card))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
                .padding(AppSpacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("IN POINT", color = AppColors.TextFaint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(formatTime(inPointMs), color = AppColors.Text, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CLIP DURATION", color = AppColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(formatTime(outPointMs - inPointMs), color = AppColors.Warning, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OUT POINT", color = AppColors.TextFaint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(formatTime(outPointMs), color = AppColors.Text, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Waveform & Draggable Handles Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(AppRadius.card))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
                .onSizeChanged { canvasWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val touchX = change.position.x.coerceIn(0f, canvasWidthPx)
                        val touchMs = ((touchX / canvasWidthPx) * totalDurationMs).toLong()

                        val distToIn = Math.abs(touchMs - inPointMs)
                        val distToOut = Math.abs(touchMs - outPointMs)

                        if (distToIn < distToOut) {
                            inPointMs = touchMs.coerceIn(0L, (outPointMs - 1000L).coerceAtLeast(0L))
                        } else {
                            outPointMs = touchMs.coerceIn((inPointMs + 1000L).coerceAtMost(totalDurationMs), totalDurationMs)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 16.dp)) {
                val w = size.width
                val h = size.height

                val inX = (inPointMs.toFloat() / totalDurationMs) * w
                val outX = (outPointMs.toFloat() / totalDurationMs) * w
                val playheadX = (currentPositionMs.toFloat() / totalDurationMs) * w

                // Draw dimmed outer regions
                drawRect(color = Color(0x99000000), topLeft = Offset(0f, 0f), size = Size(inX, h))
                drawRect(color = Color(0x99000000), topLeft = Offset(outX, 0f), size = Size(w - outX, h))

                // Draw active highlighted region
                drawRect(color = Color(0x225C4FEE), topLeft = Offset(inX, 0f), size = Size(outX - inX, h))

                // Draw waveform bars
                val barWidth = w / waveformBars.size
                waveformBars.forEachIndexed { idx, barAmp ->
                    val barX = idx * barWidth + barWidth / 2f
                    val barH = h * barAmp * 0.8f
                    val barColor = if (barX in inX..outX) AppColors.Text else AppColors.TextFaint

                    drawLine(
                        color = barColor,
                        start = Offset(barX, (h - barH) / 2f),
                        end = Offset(barX, (h + barH) / 2f),
                        strokeWidth = barWidth * 0.6f
                    )
                }

                // In handle (Electric Violet)
                drawLine(
                    color = AppColors.Accent,
                    start = Offset(inX, 0f),
                    end = Offset(inX, h),
                    strokeWidth = 4.dp.toPx()
                )
                drawCircle(color = AppColors.Accent, radius = 8.dp.toPx(), center = Offset(inX, h / 2f))

                // Out handle (Magenta Warning)
                drawLine(
                    color = AppColors.Warning,
                    start = Offset(outX, 0f),
                    end = Offset(outX, h),
                    strokeWidth = 4.dp.toPx()
                )
                drawCircle(color = AppColors.Warning, radius = 8.dp.toPx(), center = Offset(outX, h / 2f))

                // Playhead (Gold/White line)
                if (currentPositionMs in inPointMs..outPointMs) {
                    drawLine(
                        color = Color.White,
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, h),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }

        // Quick Set In / Set Out buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    inPointMs = currentPositionMs.toLong().coerceIn(0L, (outPointMs - 1000L).coerceAtLeast(0L))
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceRaised)
            ) {
                Text("Set IN to Playhead", color = AppColors.Text, fontSize = 12.sp)
            }
            Button(
                onClick = {
                    outPointMs = currentPositionMs.toLong().coerceIn((inPointMs + 1000L).coerceAtMost(totalDurationMs), totalDurationMs)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceRaised)
            ) {
                Text("Set OUT to Playhead", color = AppColors.Text, fontSize = 12.sp)
            }
        }

        // Play / Pause preview selection
        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = AppSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        playerManager.pause()
                    } else {
                        playerManager.play(
                            file = recording.file,
                            startFromMs = inPointMs.toInt(),
                            loopToMs = outPointMs.toInt()
                        )
                    }
                },
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(AppColors.Accent)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Preview",
                    tint = AppColors.Text,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
