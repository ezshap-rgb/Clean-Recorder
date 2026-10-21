package com.cleanrecorder.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.core.content.FileProvider
import com.cleanrecorder.app.audio.AudioPlayerManager
import com.cleanrecorder.app.data.Recording
import com.cleanrecorder.app.data.RecordingRepository
import com.cleanrecorder.app.theme.AppColors
import com.cleanrecorder.app.theme.AppRadius
import com.cleanrecorder.app.theme.AppSpacing
import com.cleanrecorder.app.ui.components.DeleteConfirmDialog
import com.cleanrecorder.app.ui.components.NameDialog

@Composable
fun LibraryScreen(
    onOpenTrimmer: (Recording) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { RecordingRepository(context) }
    val playerManager = remember { AudioPlayerManager(scope) }

    var recordings by remember { mutableStateOf(emptyList<Recording>()) }

    fun refreshList() {
        recordings = repository.getRecordings()
    }

    LaunchedEffect(Unit) {
        refreshList()
    }

    DisposableEffect(Unit) {
        onDispose {
            playerManager.stop()
        }
    }

    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPositionMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val currentPlayingPath by playerManager.currentPlayingPath.collectAsState()

    var renamingItem by remember { mutableStateOf<Recording?>(null) }
    var deletingItem by remember { mutableStateOf<Recording?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.lg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recordings",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.Text
            )
            Text(
                text = "${recordings.size} ${if (recordings.size == 1) "item" else "items"}",
                color = AppColors.TextFaint,
                fontSize = 14.sp
            )
        }

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = AppColors.TextFaint,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Text(
                        text = "No recordings yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.TextMuted
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Text(
                        text = "Recorded lectures will show up here.",
                        color = AppColors.TextFaint
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                contentPadding = PaddingValues(bottom = AppSpacing.xxl)
            ) {
                items(recordings, key = { it.file.absolutePath }) { recording ->
                    val isThisPlaying = isPlaying && currentPlayingPath == recording.file.absolutePath

                    RecordingCard(
                        recording = recording,
                        isPlaying = isThisPlaying,
                        currentPositionMs = if (currentPlayingPath == recording.file.absolutePath) currentPositionMs else 0,
                        durationMs = if (currentPlayingPath == recording.file.absolutePath) durationMs else recording.durationMs.toInt(),
                        onPlayToggle = {
                            if (isThisPlaying) {
                                playerManager.pause()
                            } else {
                                playerManager.play(recording.file)
                            }
                        },
                        onSeek = { targetMs ->
                            playerManager.seekTo(targetMs)
                        },
                        onTrim = {
                            playerManager.stop()
                            onOpenTrimmer(recording)
                        },
                        onShare = {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "com.cleanrecorder.app.fileprovider",
                                    recording.file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "audio/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Recording"))
                            } catch (_: Exception) {}
                        },
                        onRename = { renamingItem = recording },
                        onDelete = { deletingItem = recording }
                    )
                }
            }
        }
    }

    // Rename Dialog
    renamingItem?.let { item ->
        NameDialog(
            initialName = item.name,
            onConfirm = { newName ->
                repository.renameRecording(item, newName)
                renamingItem = null
                refreshList()
            },
            onDismiss = { renamingItem = null }
        )
    }

    // Delete Confirm Dialog
    deletingItem?.let { item ->
        DeleteConfirmDialog(
            itemName = item.name,
            onConfirm = {
                if (currentPlayingPath == item.file.absolutePath) {
                    playerManager.stop()
                }
                repository.deleteRecording(item)
                deletingItem = null
                refreshList()
            },
            onDismiss = { deletingItem = null }
        )
    }
}

@Composable
fun RecordingCard(
    recording: Recording,
    isPlaying: Boolean,
    currentPositionMs: Int,
    durationMs: Int,
    onPlayToggle: () -> Unit,
    onSeek: (Int) -> Unit,
    onTrim: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.card))
            .background(AppColors.Surface)
            .border(1.dp, if (isPlaying) AppColors.Accent else AppColors.Border, RoundedCornerShape(AppRadius.card))
            .padding(AppSpacing.lg)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Text,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = recording.formattedDate,
                        fontSize = 12.sp,
                        color = AppColors.TextFaint
                    )
                }

                // Format badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppRadius.pill))
                        .background(AppColors.SurfaceRaised)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.pill))
                        .padding(horizontal = AppSpacing.sm, vertical = 2.dp)
                ) {
                    Text(
                        text = recording.format,
                        color = AppColors.Accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Player Scrubber row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPlayToggle,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) AppColors.Accent else AppColors.SurfaceRaised)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = AppColors.Text
                    )
                }

                Spacer(modifier = Modifier.width(AppSpacing.sm))

                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = currentPositionMs.toFloat(),
                        onValueChange = { onSeek(it.toInt()) },
                        valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = AppColors.Accent,
                            activeTrackColor = AppColors.Accent,
                            inactiveTrackColor = AppColors.Border
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val currentSec = currentPositionMs / 1000
                        val durSec = durationMs / 1000
                        Text(
                            text = String.format("%02d:%02d", currentSec / 60, currentSec % 60),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AppColors.TextMuted
                        )
                        Text(
                            text = "${recording.formattedSize} · ${recording.formattedDuration}",
                            fontSize = 11.sp,
                            color = AppColors.TextFaint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Actions row: Trim, Share, Rename, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onTrim) {
                    Icon(Icons.Default.ContentCut, contentDescription = "Trim", tint = AppColors.Accent)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = AppColors.TextMuted)
                }
                IconButton(onClick = onRename) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = AppColors.TextMuted)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppColors.Danger)
                }
            }
        }
    }
}
