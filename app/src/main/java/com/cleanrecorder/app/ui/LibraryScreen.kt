package com.cleanrecorder.app.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
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

    // Multi-selection state
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedPaths = remember { mutableStateListOf<String>() }

    var renamingItem by remember { mutableStateOf<Recording?>(null) }
    var deletingItem by remember { mutableStateOf<Recording?>(null) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = AppSpacing.lg)
    ) {
        // TOP HEADER: Title, Count, and Select / Done
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isSelectMode) "Select Files" else "Recordings",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 22.sp,
                    color = AppColors.Text
                )
                Text(
                    text = if (isSelectMode) "${selectedPaths.size} of ${recordings.size} selected" else "${recordings.size} ${if (recordings.size == 1) "item" else "items"}",
                    color = AppColors.TextFaint,
                    fontSize = 12.sp
                )
            }

            if (recordings.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isSelectMode) {
                        Button(
                            onClick = {
                                isSelectMode = false
                                selectedPaths.clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151C33)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Done", color = AppColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = { isSelectMode = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151C33)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border),
                            shape = RoundedCornerShape(AppRadius.pill),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Select", color = AppColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // BATCH ACTION BAR: Select All / Deselect All + Delete (X) [NO DELETE ALL BUTTON]
        AnimatedVisibility(visible = isSelectMode && recordings.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(AppRadius.md))
                    .background(Color(0xFF151C33))
                    .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.md))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Select All / Deselect All Toggle
                TextButton(
                    onClick = {
                        if (selectedPaths.size == recordings.size) {
                            selectedPaths.clear()
                        } else {
                            selectedPaths.clear()
                            selectedPaths.addAll(recordings.map { it.file.absolutePath })
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = if (selectedPaths.size == recordings.size) "Deselect All" else "Select All",
                        color = AppColors.Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Delete Selected Button
                Button(
                    onClick = { showBatchDeleteDialog = true },
                    enabled = selectedPaths.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Delete (${selectedPaths.size})",
                        color = AppColors.Text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // RECORDINGS LIST (COMPACT CARDS)
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
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Text(
                        text = "No recordings yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 18.sp,
                        color = AppColors.TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Recorded lectures will appear here.",
                        color = AppColors.TextFaint,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(recordings, key = { it.file.absolutePath }) { recording ->
                    val isThisPlaying = isPlaying && currentPlayingPath == recording.file.absolutePath
                    val isSelected = selectedPaths.contains(recording.file.absolutePath)

                    CompactRecordingCard(
                        recording = recording,
                        isPlaying = isThisPlaying,
                        isSelectMode = isSelectMode,
                        isSelected = isSelected,
                        currentPositionMs = if (isThisPlaying) currentPositionMs else 0,
                        durationMs = if (isThisPlaying) durationMs else recording.durationMs.toInt(),
                        onCardClick = {
                            if (isSelectMode) {
                                if (isSelected) selectedPaths.remove(recording.file.absolutePath)
                                else selectedPaths.add(recording.file.absolutePath)
                            } else {
                                if (isThisPlaying) playerManager.pause()
                                else playerManager.play(recording.file)
                            }
                        },
                        onPlayToggle = {
                            if (isThisPlaying) playerManager.pause()
                            else playerManager.play(recording.file)
                        },
                        onSeek = { targetMs -> playerManager.seekTo(targetMs) },
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

    // Single Delete Dialog
    deletingItem?.let { item ->
        DeleteConfirmDialog(
            itemName = item.name,
            onConfirm = {
                if (currentPlayingPath == item.file.absolutePath) playerManager.stop()
                repository.deleteRecording(item)
                deletingItem = null
                refreshList()
            },
            onDismiss = { deletingItem = null }
        )
    }

    // Batch Delete Dialog
    if (showBatchDeleteDialog) {
        DeleteConfirmDialog(
            itemName = "${selectedPaths.size} selected recordings",
            onConfirm = {
                playerManager.stop()
                selectedPaths.forEach { path ->
                    val file = java.io.File(path)
                    file.delete()
                }
                selectedPaths.clear()
                isSelectMode = false
                showBatchDeleteDialog = false
                refreshList()
            },
            onDismiss = { showBatchDeleteDialog = false }
        )
    }
}

@Composable
fun CompactRecordingCard(
    recording: Recording,
    isPlaying: Boolean,
    isSelectMode: Boolean,
    isSelected: Boolean,
    currentPositionMs: Int,
    durationMs: Int,
    onCardClick: () -> Unit,
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
            .clip(RoundedCornerShape(AppRadius.md))
            .background(if (isPlaying) Color(0xFF131A33) else AppColors.Surface)
            .border(1.dp, if (isPlaying) AppColors.Accent else if (isSelected) AppColors.Accent else AppColors.Border, RoundedCornerShape(AppRadius.md))
            .clickable { onCardClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onCardClick() },
                        colors = CheckboxDefaults.colors(checkedColor = AppColors.Accent),
                        modifier = Modifier.size(32.dp).padding(end = 4.dp)
                    )
                } else {
                    IconButton(
                        onClick = onPlayToggle,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) AppColors.Accent else Color(0xFF151C33))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = AppColors.Text,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = recording.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = AppColors.Text,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppRadius.pill))
                                .background(Color(0xFF151C33))
                                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.pill))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = recording.format,
                                color = AppColors.Accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${recording.formattedDuration} · ${recording.formattedSize} · ${recording.formattedDate}",
                        fontSize = 11.sp,
                        color = AppColors.TextFaint,
                        maxLines = 1
                    )
                }

                if (!isSelectMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onTrim, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.ContentCut, contentDescription = "Trim", tint = AppColors.Accent, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onShare, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = AppColors.TextMuted, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onRename, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename", tint = AppColors.TextMuted, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppColors.Danger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (isPlaying) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val curSec = currentPositionMs / 1000
                    val totalSec = durationMs / 1000
                    Text(
                        text = String.format("%02d:%02d", curSec / 60, curSec % 60),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AppColors.Accent
                    )
                    Slider(
                        value = currentPositionMs.toFloat(),
                        onValueChange = { onSeek(it.toInt()) },
                        valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = AppColors.Accent,
                            activeTrackColor = AppColors.Accent,
                            inactiveTrackColor = AppColors.Border
                        ),
                        modifier = Modifier.weight(1f).height(18.dp).padding(horizontal = 6.dp)
                    )
                    Text(
                        text = String.format("%02d:%02d", totalSec / 60, totalSec % 60),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AppColors.TextMuted
                    )
                }
            }
        }
    }
}
