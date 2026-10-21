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
    val description: String,
    val recommended: Boolean = false
)

val BITRATE_OPTIONS = listOf(
    BitrateOption(16, "16 kbps (Ultra Compact)", "~7 MB / hour · Optimal for speech & lectures", recommended = true),
    BitrateOption(32, "32 kbps (Balanced Voice)", "~14 MB / hour · Clear voice with tiny footprint"),
    BitrateOption(64, "64 kbps (High Clarity)", "~28 MB / hour · Excellent speech fidelity"),
    BitrateOption(128, "128 kbps (Studio Quality)", "~57 MB / hour · Full spectrum audio")
)

@Composable
fun BitrateSelectionDialog(
    currentBitrateKbps: Int,
    onSelect: (Int) -> Unit,
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
                    text = "Recording Quality",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.Text
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select bitrate for lecture capture (AAC audio container).",
                    fontSize = 13.sp,
                    color = AppColors.TextMuted
                )
                Spacer(modifier = Modifier.height(AppSpacing.lg))

                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    BITRATE_OPTIONS.forEach { option ->
                        val isSelected = option.kbps == currentBitrateKbps
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(AppRadius.md))
                                .background(if (isSelected) AppColors.SurfaceRaised else AppColors.Surface)
                                .border(1.dp, if (isSelected) AppColors.Accent else AppColors.Border, RoundedCornerShape(AppRadius.md))
                                .clickable { onSelect(option.kbps) }
                                .padding(AppSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = option.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = if (isSelected) AppColors.Accent else AppColors.Text
                                    )
                                    if (option.recommended) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(AppRadius.pill))
                                                .background(AppColors.Accent)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("BEST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppColors.Text)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = option.description,
                                    fontSize = 12.sp,
                                    color = AppColors.TextFaint
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = AppColors.Accent)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.lg))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Done", color = AppColors.Accent)
                    }
                }
            }
        }
    }
}

@Composable
fun NameDialog(
    initialName: String,
    title: String = "Rename Recording",
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
