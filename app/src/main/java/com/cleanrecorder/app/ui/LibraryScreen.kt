package com.cleanrecorder.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cleanrecorder.app.theme.AppColors
import com.cleanrecorder.app.theme.AppSpacing

@Composable
fun LibraryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Recordings Library",
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.Text
        )
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text(
            text = "Your lectures will appear here.",
            color = AppColors.TextMuted
        )
    }
}
