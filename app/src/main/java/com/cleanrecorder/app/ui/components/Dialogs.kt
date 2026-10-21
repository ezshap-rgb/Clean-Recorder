package com.cleanrecorder.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cleanrecorder.app.theme.AppColors
import com.cleanrecorder.app.theme.AppRadius
import com.cleanrecorder.app.theme.AppSpacing

data class BitrateOption(
    val kbps: Int,
    val title: String,
    val description: String
)

val BITRATE_OPTIONS = listOf(
    BitrateOption(16, "16 kbps (Ultra Compact)", "~7 MB / hour · Ideal for voice & lectures"),
    BitrateOption(32, "32 kbps (Balanced)", "~14 MB / hour · Clear voice with small footprint"),
    BitrateOption(64, "64 kbps (High Clarity)", "~28 MB / hour · High speech fidelity"),
    BitrateOption(128, "128 kbps (Studio Quality)", "~57 MB / hour · Full spectrum audio")
)

@Composable
fun SettingsDialog(
    currentFormat: String,
    currentBitrateKbps: Int,
    onSaveSettings: (format: String, bitrateKbps: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(currentFormat) }
    var selectedBitrate by remember { mutableIntStateOf(currentBitrateKbps) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.card))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
                .padding(AppSpacing.xl)
        ) {
            Column {
                Text(
                    text = "Recording Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.Text
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure audio container and bitrate.",
                    fontSize = 13.sp,
                    color = AppColors.TextMuted
                )

                Spacer(modifier = Modifier.height(AppSpacing.lg))

                // Format selector
                Text(
                    text = "CONTAINER FORMAT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextFaint
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    listOf("AAC", "MP3").forEach { fmt ->
                        val isSelected = selectedFormat.equals(fmt, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(AppRadius.md))
                                .background(if (isSelected) AppColors.SurfaceRaised else AppColors.Surface)
                                .border(1.dp, if (isSelected) AppColors.Accent else AppColors.Border, RoundedCornerShape(AppRadius.md))
                                .clickable { selectedFormat = fmt }
                                .padding(vertical = AppSpacing.md),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fmt,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) AppColors.Accent else AppColors.TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.lg))

                // Bitrate selector
                Text(
                    text = "BITRATE (COMPRESSION)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextFaint
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BITRATE_OPTIONS.forEach { option ->
                        val isSelected = option.kbps == selectedBitrate
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(AppRadius.md))
                                .background(if (isSelected) AppColors.SurfaceRaised else AppColors.Surface)
                                .border(1.dp, if (isSelected) AppColors.Accent else AppColors.Border, RoundedCornerShape(AppRadius.md))
                                .clickable { selectedBitrate = option.kbps }
                                .padding(horizontal = AppSpacing.md, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) AppColors.Accent else AppColors.Text
                                )
                                Text(
                                    text = option.description,
                                    fontSize = 11.sp,
                                    color = AppColors.TextFaint
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = AppColors.Accent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.lg))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = AppColors.TextMuted)
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Button(
                        onClick = {
                            onSaveSettings(selectedFormat, selectedBitrate)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
                    ) {
                        Text("Save Settings", color = AppColors.Text)
                    }
                }
            }
        }
    }
}

@Composable
fun NameDialog(
    initialName: String,
    title: String = "Save Lecture Recording",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialName) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.card))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
                .padding(AppSpacing.xl)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.Text
                )
                Spacer(modifier = Modifier.height(AppSpacing.md))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppColors.Text,
                        unfocusedTextColor = AppColors.Text,
                        focusedBorderColor = AppColors.Accent,
                        unfocusedBorderColor = AppColors.Border,
                        focusedContainerColor = AppColors.SurfaceRaised,
                        unfocusedContainerColor = AppColors.SurfaceRaised
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(AppSpacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = AppColors.TextMuted)
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Button(
                        onClick = { onConfirm(text.trim()) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
                    ) {
                        Text("Save", color = AppColors.Text)
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.card))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
                .padding(AppSpacing.xl)
        ) {
            Column {
                Text(
                    text = "Delete Recording?",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.Text
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Text(
                    text = "Are you sure you want to delete \"$itemName\"? This cannot be undone.",
                    color = AppColors.TextMuted
                )
                Spacer(modifier = Modifier.height(AppSpacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = AppColors.TextMuted)
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger)
                    ) {
                        Text("Delete", color = AppColors.Text)
                    }
                }
            }
        }
    }
}
