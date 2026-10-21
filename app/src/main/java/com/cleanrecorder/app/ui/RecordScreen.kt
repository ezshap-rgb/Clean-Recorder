package com.cleanrecorder.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanrecorder.app.R
import com.cleanrecorder.app.service.RecordingService
import com.cleanrecorder.app.theme.AppColors
import com.cleanrecorder.app.theme.AppRadius
import com.cleanrecorder.app.theme.AppSpacing
import com.cleanrecorder.app.ui.components.LevelMeter
import com.cleanrecorder.app.ui.components.NameDialog
import com.cleanrecorder.app.ui.components.SettingsDialog

@Composable
fun RecordScreen(
    onRecordingSaved: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("clean_recorder_prefs", Context.MODE_PRIVATE) }

    var selectedFormat by remember {
        mutableStateOf(prefs.getString("audio_format", "AAC") ?: "AAC")
    }
    var selectedBitrate by remember {
        mutableIntStateOf(prefs.getInt("bitrate_kbps", 16))
    }

    val isRecording by RecordingService.isRecording.collectAsState()
    val isPaused by RecordingService.isPaused.collectAsState()
    val elapsedTenths by RecordingService.elapsedSeconds.collectAsState()
    val amplitude by RecordingService.amplitude.collectAsState()
    val lastFinishedFile by RecordingService.lastFinishedFile.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(lastFinishedFile) {
        if (lastFinishedFile != null) {
            showSaveDialog = true
        }
    }

    // Timer calculation: ALWAYS 00:00 when idle/resting!
    val elapsedSeconds = if (isRecording) elapsedTenths / 10 else 0L
    val formattedTime = remember(elapsedSeconds, isRecording) {
        if (!isRecording) {
            "00:00"
        } else {
            val hours = elapsedSeconds / 3600
            val mins = (elapsedSeconds % 3600) / 60
            val secs = elapsedSeconds % 60
            if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, mins, secs)
            } else {
                String.format("%02d:%02d", mins, secs)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HEADER MATCHING IMAGE 1 EXACTLY
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: App Icon + App Name + STUDIO badge + Subtitle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = "Clean Recorder Icon",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Clean Recorder",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // STUDIO badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppRadius.pill))
                                .background(Color(0xFF1B163B))
                                .border(1.dp, AppColors.Accent.copy(alpha = 0.6f), RoundedCornerShape(AppRadius.pill))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "STUDIO",
                                color = AppColors.Accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Subtitle: e.g. "16 kHz mono, 16 kbp..."
                        Text(
                            text = "16 kHz mono, $selectedBitrate kbp...",
                            color = AppColors.TextMuted,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // Right: Settings pill button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppRadius.pill))
                    .background(AppColors.SurfaceRaised)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.pill))
                    .clickable(enabled = !isRecording) { showSettingsDialog = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = AppColors.Text,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Settings",
                    color = AppColors.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // CENTER MAIN STUDIO CARD MATCHING IMAGE 1 EXACTLY
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = AppSpacing.lg)
                .clip(RoundedCornerShape(28.dp))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(28.dp))
                .padding(AppSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top status pill badge inside card: e.g. "● Tap to record"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppRadius.pill))
                        .background(Color(0xFF131A2E))
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !isRecording -> Color(0xFF64748B)
                                        isPaused -> AppColors.Warning
                                        else -> AppColors.Record
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                !isRecording -> "Tap to record"
                                isPaused -> "Recording paused"
                                else -> "Recording live"
                            },
                            color = AppColors.Text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Monospace Timer
                Text(
                    text = formattedTime,
                    fontSize = 62.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) AppColors.Text else Color(0xFF8E9CAE)
                )

                // Large Dual-Circle Record Button or Pause/Stop Controls
                if (isRecording) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pause / Resume
                        IconButton(
                            onClick = {
                                val action = if (isPaused) RecordingService.ACTION_RESUME else RecordingService.ACTION_PAUSE
                                val intent = Intent(context, RecordingService::class.java).apply { this.action = action }
                                context.startService(intent)
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AppColors.SurfaceRaised)
                                .border(1.dp, AppColors.Border, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = AppColors.Text,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Stop
                        IconButton(
                            onClick = {
                                val intent = Intent(context, RecordingService::class.java).apply {
                                    action = RecordingService.ACTION_STOP
                                }
                                context.startService(intent)
                            },
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(AppColors.Record)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = AppColors.Text,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                } else {
                    // Exact Image 1 Dual Ring Big Record Button
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .border(4.dp, AppColors.Record, CircleShape)
                            .clickable {
                                val intent = Intent(context, RecordingService::class.java).apply {
                                    action = RecordingService.ACTION_START
                                    putExtra(RecordingService.EXTRA_BITRATE_KBPS, selectedBitrate)
                                    putExtra(RecordingService.EXTRA_FORMAT, selectedFormat)
                                }
                                context.startForegroundService(intent)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .clip(CircleShape)
                                .background(AppColors.Record)
                        )
                    }
                }

                // Bottom dotted row / Live Level Meter
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isRecording && !isPaused) {
                        LevelMeter(
                            amplitude = amplitude,
                            isRecording = true,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    } else {
                        // Subtle dotted placeholder row matching Image 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until 28) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF18223C))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Saves automatically to device storage",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentFormat = selectedFormat,
            currentBitrateKbps = selectedBitrate,
            onSaveSettings = { newFmt, newBitrate ->
                selectedFormat = newFmt
                selectedBitrate = newBitrate
                prefs.edit()
                    .putString("audio_format", newFmt)
                    .putInt("bitrate_kbps", newBitrate)
                    .apply()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Save Name Dialog
    if (showSaveDialog && lastFinishedFile != null) {
        val file = lastFinishedFile!!
        NameDialog(
            initialName = file.nameWithoutExtension,
            title = "Save Lecture Recording",
            onConfirm = { newName ->
                val target = java.io.File(file.parentFile, "$newName.${file.extension}")
                if (newName.isNotBlank() && target.absolutePath != file.absolutePath) {
                    file.renameTo(target)
                }
                RecordingService.clearLastFinishedFile()
                showSaveDialog = false
                onRecordingSaved()
            },
            onDismiss = {
                RecordingService.clearLastFinishedFile()
                showSaveDialog = false
                onRecordingSaved()
            }
        )
    }
}
