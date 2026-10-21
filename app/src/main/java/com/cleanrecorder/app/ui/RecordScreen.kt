package com.cleanrecorder.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanrecorder.app.service.RecordingService
import com.cleanrecorder.app.theme.AppColors
import com.cleanrecorder.app.theme.AppRadius
import com.cleanrecorder.app.theme.AppSpacing
import com.cleanrecorder.app.ui.components.BitrateSelectionDialog
import com.cleanrecorder.app.ui.components.LevelMeter
import com.cleanrecorder.app.ui.components.NameDialog

@Composable
fun RecordScreen(
    onRecordingSaved: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("clean_recorder_prefs", Context.MODE_PRIVATE) }

    var selectedBitrate by remember {
        mutableIntStateOf(prefs.getInt("bitrate_kbps", 16))
    }

    val isRecording by RecordingService.isRecording.collectAsState()
    val isPaused by RecordingService.isPaused.collectAsState()
    val elapsedTenths by RecordingService.elapsedSeconds.collectAsState()
    val amplitude by RecordingService.amplitude.collectAsState()
    val lastFinishedFile by RecordingService.lastFinishedFile.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var showBitrateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(lastFinishedFile) {
        if (lastFinishedFile != null) {
            showSaveDialog = true
        }
    }

    val elapsedSeconds = elapsedTenths / 10
    val formattedTime = remember(elapsedSeconds) {
        val hours = elapsedSeconds / 3600
        val mins = (elapsedSeconds % 3600) / 60
        val secs = elapsedSeconds % 60
        if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Clean Recorder",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.Text
            )

            // Clickable Quality badge to change bitrate
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppRadius.pill))
                    .background(AppColors.SurfaceRaised)
                    .border(1.dp, AppColors.Accent, RoundedCornerShape(AppRadius.pill))
                    .clickable(enabled = !isRecording) { showBitrateDialog = true }
                    .padding(horizontal = AppSpacing.md, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AAC $selectedBitrate kbps",
                    color = AppColors.Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!isRecording) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Change Bitrate",
                        tint = AppColors.Accent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Timer & Animated Level Meter Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.card))
                .background(AppColors.Surface)
                .border(1.dp, if (isRecording) AppColors.Accent else AppColors.Border, RoundedCornerShape(AppRadius.card))
                .padding(vertical = AppSpacing.xxl, horizontal = AppSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formattedTime,
                    fontSize = 54.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) AppColors.Text else AppColors.TextMuted
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Text(
                    text = when {
                        !isRecording -> "Ready to record ($selectedBitrate kbps)"
                        isPaused -> "Paused"
                        else -> "Recording live ($selectedBitrate kbps)"
                    },
                    color = when {
                        !isRecording -> AppColors.TextFaint
                        isPaused -> AppColors.Warning
                        else -> AppColors.Record
                    },
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(AppSpacing.xl))

                // Real-time Level Meter
                LevelMeter(
                    amplitude = amplitude,
                    isRecording = isRecording && !isPaused
                )
            }
        }

        // Recording Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppSpacing.xl),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRecording) {
                // Pause / Resume
                IconButton(
                    onClick = {
                        val action = if (isPaused) RecordingService.ACTION_RESUME else RecordingService.ACTION_PAUSE
                        val intent = Intent(context, RecordingService::class.java).apply { this.action = action }
                        context.startService(intent)
                    },
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(AppColors.SurfaceRaised)
                        .border(1.dp, AppColors.Border, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = AppColors.Text,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Stop Button
                IconButton(
                    onClick = {
                        val intent = Intent(context, RecordingService::class.java).apply {
                            action = RecordingService.ACTION_STOP
                        }
                        context.startService(intent)
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(AppColors.Record)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = AppColors.Text,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else {
                // Big Record Button
                IconButton(
                    onClick = {
                        val intent = Intent(context, RecordingService::class.java).apply {
                            action = RecordingService.ACTION_START
                            putExtra(RecordingService.EXTRA_BITRATE_KBPS, selectedBitrate)
                        }
                        context.startForegroundService(intent)
                    },
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(AppColors.Record)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record",
                        tint = AppColors.Text,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }

    // Bitrate selection dialog
    if (showBitrateDialog) {
        BitrateSelectionDialog(
            currentBitrateKbps = selectedBitrate,
            onSelect = { newBitrate ->
                selectedBitrate = newBitrate
                prefs.edit().putInt("bitrate_kbps", newBitrate).apply()
                showBitrateDialog = false
            },
            onDismiss = { showBitrateDialog = false }
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
