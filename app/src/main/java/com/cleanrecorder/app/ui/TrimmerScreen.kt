package com.cleanrecorder.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

    // 44 deterministic studio waveform envelope bars
    val waveformBars = remember(recording.name) {
        val count = 44
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

    // Auto-loop preview inside selection
    LaunchedEffect(isPlaying, currentPositionMs, outPointMs) {
        if (isPlaying && currentPositionMs >= outPointMs) {
            playerManager.pause()
            playerManager.seekTo(inPointMs.toInt())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            // SAFE PADDING: statusBarsPadding & navigationBarsPadding keep everything away from notches & nav bars!
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    playerManager.stop()
                    onCancel()
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppColors.SurfaceRaised)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = AppColors.Text)
            }

            Text(
                text = "Audio Trimmer",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp,
                color = AppColors.Text
            )

            // Prominent Save Trim button
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
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
            ) {
                if (isTrimming) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AppColors.Text, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text("Save Trim", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Region Duration Info Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.card))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
                .padding(vertical = AppSpacing.md, horizontal = AppSpacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("IN POINT", color = AppColors.TextFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(formatTime(inPointMs), color = AppColors.Accent, fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CLIP LENGTH", color = AppColors.TextFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(formatTime(outPointMs - inPointMs), color = AppColors.Warning, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OUT POINT", color = AppColors.TextFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(formatTime(outPointMs), color = AppColors.Warning, fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Waveform Container with Interactive In/Out Dragging & Tap-to-Seek
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(AppRadius.card))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
                .onSizeChanged { canvasWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(totalDurationMs) {
                    detectTapGestures { offset ->
                        val touchX = offset.x.coerceIn(0f, canvasWidthPx)
                        val touchMs = ((touchX / canvasWidthPx) * totalDurationMs).toLong()
                        playerManager.seekTo(touchMs.toInt())
                    }
                }
                .pointerInput(totalDurationMs) {
                    detectDragGestures { change, _ ->
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
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 14.dp)) {
                val w = size.width
                val h = size.height

                val inX = (inPointMs.toFloat() / totalDurationMs) * w
                val outX = (outPointMs.toFloat() / totalDurationMs) * w
                val playheadX = (currentPositionMs.toFloat() / totalDurationMs) * w

                // Dimmed trimmed-away outer regions
                drawRect(color = Color(0xB3000000), topLeft = Offset(0f, 0f), size = Size(inX, h))
                drawRect(color = Color(0xB3000000), topLeft = Offset(outX, 0f), size = Size(w - outX, h))

                // Active glowing region
                drawRect(color = Color(0x225C4FEE), topLeft = Offset(inX, 0f), size = Size(outX - inX, h))

                // Waveform bars
                val barWidth = w / waveformBars.size
                waveformBars.forEachIndexed { idx, barAmp ->
                    val barX = idx * barWidth + barWidth / 2f
                    val barH = h * barAmp * 0.78f
                    val barColor = if (barX in inX..outX) AppColors.Text else AppColors.TextFaint

                    drawLine(
                        color = barColor,
                        start = Offset(barX, (h - barH) / 2f),
                        end = Offset(barX, (h + barH) / 2f),
                        strokeWidth = barWidth * 0.6f
                    )
                }

                // IN Handle (Electric Violet)
                drawLine(
                    color = AppColors.Accent,
                    start = Offset(inX, 0f),
                    end = Offset(inX, h),
                    strokeWidth = 4.dp.toPx()
                )
                drawCircle(color = AppColors.Accent, radius = 9.dp.toPx(), center = Offset(inX, 16.dp.toPx()))

                // OUT Handle (Vibrant Magenta)
                drawLine(
                    color = AppColors.Warning,
                    start = Offset(outX, 0f),
                    end = Offset(outX, h),
                    strokeWidth = 4.dp.toPx()
                )
                drawCircle(color = AppColors.Warning, radius = 9.dp.toPx(), center = Offset(outX, h - 16.dp.toPx()))

                // Playhead Needle
                drawLine(
                    color = Color.White,
                    start = Offset(playheadX, 0f),
                    end = Offset(playheadX, h),
                    strokeWidth = 2.5.dp.toPx()
                )
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(playheadX, h / 2f))
            }
        }

        // Dedicated Manual Playhead Scrubber Slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.md))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.md))
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PLAYHEAD SCRUBBER", color = AppColors.TextFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${formatTime(currentPositionMs.toLong())} / ${formatTime(totalDurationMs)}",
                    color = AppColors.Text,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = currentPositionMs.toFloat(),
                onValueChange = { targetMs ->
                    playerManager.seekTo(targetMs.toInt())
                },
                valueRange = 0f..totalDurationMs.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = AppColors.Accent,
                    inactiveTrackColor = AppColors.Border
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Quick Seek and In/Out Snapping Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = {
                    inPointMs = currentPositionMs.toLong().coerceIn(0L, (outPointMs - 1000L).coerceAtLeast(0L))
                },
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Accent),
                shape = RoundedCornerShape(AppRadius.md)
            ) {
                Text("Set IN to Current", color = AppColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    outPointMs = currentPositionMs.toLong().coerceIn((inPointMs + 1000L).coerceAtMost(totalDurationMs), totalDurationMs)
                },
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Warning),
                shape = RoundedCornerShape(AppRadius.md)
            ) {
                Text("Set OUT to Current", color = AppColors.Warning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Playback Control Cluster: -5s, Play/Pause Toggle, +5s
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppSpacing.sm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Jump back 5 sec
            IconButton(
                onClick = {
                    val prev = (currentPositionMs - 5000).coerceAtLeast(inPointMs.toInt())
                    playerManager.seekTo(prev)
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppColors.SurfaceRaised)
            ) {
                Icon(Icons.Default.Replay5, contentDescription = "-5s", tint = AppColors.Text)
            }

            Spacer(modifier = Modifier.width(AppSpacing.xl))

            // Primary Play / Pause Toggle Button
            IconButton(
                onClick = {
                    if (isPlaying) {
                        playerManager.pause()
                    } else {
                        // Start playing from current position or from inPoint if outside
                        val startPos = if (currentPositionMs in inPointMs..outPointMs) currentPositionMs else inPointMs.toInt()
                        playerManager.play(
                            file = recording.file,
                            startFromMs = startPos,
                            loopToMs = outPointMs.toInt()
                        )
                    }
                },
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) AppColors.Warning else AppColors.Accent)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = AppColors.Text,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.width(AppSpacing.xl))

            // Jump forward 5 sec
            IconButton(
                onClick = {
                    val next = (currentPositionMs + 5000).coerceAtMost(outPointMs.toInt())
                    playerManager.seekTo(next)
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppColors.SurfaceRaised)
            ) {
                Icon(Icons.Default.Forward5, contentDescription = "+5s", tint = AppColors.Text)
            }
        }
    }
}
