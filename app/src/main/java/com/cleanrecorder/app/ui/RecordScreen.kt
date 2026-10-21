package com.cleanrecorder.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanrecorder.app.theme.AppColors
import com.cleanrecorder.app.theme.AppRadius
import com.cleanrecorder.app.theme.AppSpacing

@Composable
fun RecordScreen() {
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

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
            .padding(AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Clean Recorder",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.Text
            )
            // Quality pill badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppRadius.pill))
                    .background(AppColors.SurfaceRaised)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.pill))
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
            ) {
                Text(
                    text = "AAC High",
                    color = AppColors.Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Timer & Waveform Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.card))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
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
                        !isRecording -> "Tap record to begin"
                        isPaused -> "Paused"
                        else -> "Recording live"
                    },
                    color = when {
                        !isRecording -> AppColors.TextFaint
                        isPaused -> AppColors.Warning
                        else -> AppColors.Record
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Control buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppSpacing.xxl),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRecording) {
                // Pause / Resume
                IconButton(
                    onClick = { isPaused = !isPaused },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AppColors.SurfaceRaised)
                        .border(1.dp, AppColors.Border, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = AppColors.Text
                    )
                }

                // Stop button
                IconButton(
                    onClick = {
                        isRecording = false
                        isPaused = false
                        elapsedSeconds = 0
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
                // Big Record Button with Gradient Border
                IconButton(
                    onClick = {
                        isRecording = true
                        isPaused = false
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
}
