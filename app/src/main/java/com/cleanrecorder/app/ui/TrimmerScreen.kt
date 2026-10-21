package com.cleanrecorder.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
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
    var isLooping by remember { mutableStateOf(true) }
    var canvasWidthPx by remember { mutableFloatStateOf(1f) }
    var isTrimming by remember { mutableStateOf(false) }

    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPositionMs by playerManager.currentPositionMs.collectAsState()

    // 44 deterministic waveform bars
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
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        if (hours > 0) {
            String.format("%d:%02d:%02d", hours, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    // Auto loop within region
    LaunchedEffect(isPlaying, currentPositionMs, outPointMs, isLooping) {
        if (isPlaying && currentPositionMs >= outPointMs) {
            if (isLooping) {
                playerManager.seekTo(inPointMs.toInt())
            } else {
                playerManager.pause()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            // EXTRA SAFE MARGIN ON TOP & GENEROUS BOTTOM CLEARANCE
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 18.dp, start = AppSpacing.lg, end = AppSpacing.lg, bottom = 28.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Audio Trimmer",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Text
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppRadius.pill))
                            .background(Color(0xFF1B163B))
                            .border(1.dp, AppColors.Accent.copy(alpha = 0.6f), RoundedCornerShape(AppRadius.pill))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "STUDIO",
                            color = AppColors.Accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${recording.name}.${recording.format.lowercase()} · ${recording.formattedSize}",
                    color = AppColors.TextMuted,
                    fontSize = 13.sp
                )
            }

            // Close Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppRadius.pill))
                    .background(AppColors.SurfaceRaised)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.pill))
                    .clickable {
                        playerManager.stop()
                        onCancel()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = AppColors.Text, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Close", color = AppColors.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // CARD 1: PLAYBACK CONTROLS (AMBER PLAY BUTTON)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.card))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Top Row: [ AMBER PLAY ] and [ Amber Playhead ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // AMBER PLAY / PAUSE BUTTON (#F59E0B)
                    Button(
                        onClick = {
                            if (isPlaying) {
                                playerManager.pause()
                            } else {
                                val startPos = if (currentPositionMs in inPointMs..outPointMs) currentPositionMs else inPointMs.toInt()
                                playerManager.play(
                                    file = recording.file,
                                    startFromMs = startPos,
                                    loopToMs = outPointMs.toInt()
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.DotGold),
                        shape = RoundedCornerShape(AppRadius.pill),
                        modifier = Modifier
                            .weight(0.42f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPlaying) "PAUSE" else "PLAY",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Amber Playhead Pill
                    Row(
                        modifier = Modifier
                            .weight(0.58f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(AppRadius.pill))
                            .background(Color(0xFF151C33))
                            .border(1.5.dp, AppColors.DotGold, RoundedCornerShape(AppRadius.pill))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(AppColors.DotGold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Playhead: ", color = AppColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = formatTime(currentPositionMs.toLong()),
                            color = AppColors.DotGold,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Bottom Row: [ Jump to IN ] [ Jump to OUT ] [ ↺ Loop Selection ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(AppRadius.md))
                            .background(Color(0xFF151C33))
                            .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.md))
                            .clickable { playerManager.seekTo(inPointMs.toInt()) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Jump to IN", color = AppColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(AppRadius.md))
                            .background(Color(0xFF151C33))
                            .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.md))
                            .clickable { playerManager.seekTo(outPointMs.toInt()) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Jump to OUT", color = AppColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(AppRadius.md))
                            .background(if (isLooping) Color(0xFF1E2142) else Color(0xFF151C33))
                            .border(1.dp, if (isLooping) AppColors.Accent else AppColors.Border, RoundedCornerShape(AppRadius.md))
                            .clickable { isLooping = !isLooping },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Repeat, contentDescription = null, tint = if (isLooping) AppColors.Accent else AppColors.TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Loop Selection",
                                color = if (isLooping) AppColors.Accent else AppColors.Text,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // CARD 2: WAVEFORM DISPLAY & SLIDER WITH AMBER PLAYHEAD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.card))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("00:00", color = AppColors.TextFaint, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("|   ·   |   ·   |", color = Color(0xFF263252), fontSize = 10.sp)
                    Text(formatTime(totalDurationMs), color = AppColors.TextFaint, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
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
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        val inX = (inPointMs.toFloat() / totalDurationMs) * w
                        val outX = (outPointMs.toFloat() / totalDurationMs) * w
                        val playheadX = (currentPositionMs.toFloat() / totalDurationMs) * w

                        drawRect(color = Color(0xCC070913), topLeft = Offset(0f, 0f), size = Size(inX, h))
                        drawRect(color = Color(0xCC070913), topLeft = Offset(outX, 0f), size = Size(w - outX, h))

                        val barWidth = w / waveformBars.size
                        waveformBars.forEachIndexed { idx, barAmp ->
                            val barX = idx * barWidth + barWidth / 2f
                            val barH = h * barAmp * 0.82f
                            val barColor = if (barX in inX..outX) Color(0xFF6366F1) else Color(0xFF2A3454)

                            drawLine(
                                color = barColor,
                                start = Offset(barX, (h - barH) / 2f),
                                end = Offset(barX, (h + barH) / 2f),
                                strokeWidth = barWidth * 0.65f
                            )
                        }

                        // IN Handle
                        drawRoundRect(
                            color = Color(0xFFF50ACE),
                            topLeft = Offset(inX - 4.dp.toPx(), 0f),
                            size = Size(6.dp.toPx(), h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )

                        // OUT Handle
                        drawRoundRect(
                            color = Color(0xFFF43F5E),
                            topLeft = Offset(outX - 2.dp.toPx(), 0f),
                            size = Size(6.dp.toPx(), h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )

                        // AMBER Playhead Needle
                        drawLine(
                            color = AppColors.DotGold,
                            start = Offset(playheadX, 0f),
                            end = Offset(playheadX, h),
                            strokeWidth = 2.5.dp.toPx()
                        )
                        drawCircle(color = AppColors.DotGold, radius = 5.dp.toPx(), center = Offset(playheadX, h / 2f))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Slider(
                    value = currentPositionMs.toFloat(),
                    onValueChange = { targetMs -> playerManager.seekTo(targetMs.toInt()) },
                    valueRange = 0f..totalDurationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = AppColors.DotGold,
                        activeTrackColor = AppColors.DotGold,
                        inactiveTrackColor = Color(0xFF1E2746)
                    ),
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                )
            }
        }

        // CARD 3: REGION TIME CONTROLS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.card))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // START ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(AppRadius.md))
                            .background(Color(0xFF0D1E24))
                            .border(1.dp, Color(0xFF0E4A40), RoundedCornerShape(AppRadius.md))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("START", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(formatTime(inPointMs), color = AppColors.Text, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(AppRadius.md))
                            .background(Color(0xFF0D1E24))
                            .border(1.dp, Color(0xFF0E4A40), RoundedCornerShape(AppRadius.md))
                            .clickable {
                                inPointMs = currentPositionMs.toLong().coerceIn(0L, (outPointMs - 1000L).coerceAtLeast(0L))
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("|‹ Set to Playhead", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // END ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(AppRadius.md))
                            .background(Color(0xFF24101A))
                            .border(1.dp, Color(0xFF5C142A), RoundedCornerShape(AppRadius.md))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFF43F5E)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("END", color = Color(0xFFF43F5E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(formatTime(outPointMs), color = AppColors.Text, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(AppRadius.md))
                            .background(Color(0xFF24101A))
                            .border(1.dp, Color(0xFF5C142A), RoundedCornerShape(AppRadius.md))
                            .clickable {
                                outPointMs = currentPositionMs.toLong().coerceIn((inPointMs + 1000L).coerceAtMost(totalDurationMs), totalDurationMs)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("›| Set to Playhead", color = Color(0xFFF43F5E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                val diffMs = outPointMs - inPointMs
                val diffSecs = diffMs / 1000f
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Selection Duration: ${formatTime(diffMs)}  (${String.format("%.1fs", diffSecs)})",
                        color = Color(0xFFA8B4D8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // CARD 4: ACTION / SAVE (SINGLE LINE INSTRUCTION & SINGLE LINE "SAVE TRIM")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.card))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Shortened single line instruction
                Text(
                    text = "Trim replaces original file.",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cancel
                    Button(
                        onClick = {
                            playerManager.stop()
                            onCancel()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151C33)),
                        shape = RoundedCornerShape(AppRadius.md),
                        modifier = Modifier
                            .weight(0.38f)
                            .height(46.dp)
                            .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.md))
                    ) {
                        Text("Cancel", color = AppColors.Text, fontWeight = FontWeight.SemiBold)
                    }

                    // Single-line "Save Trim" button
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
                        shape = RoundedCornerShape(AppRadius.md),
                        modifier = Modifier
                            .weight(0.62f)
                            .height(46.dp)
                    ) {
                        if (isTrimming) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AppColors.Text, strokeWidth = 2.dp)
                        } else {
                            Text("Save Trim", color = AppColors.Text, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
