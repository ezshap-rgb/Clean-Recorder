package com.cleanrecorder.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cleanrecorder.app.theme.AppColors
import com.cleanrecorder.app.theme.AppRadius
import com.cleanrecorder.app.theme.AppSpacing

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
