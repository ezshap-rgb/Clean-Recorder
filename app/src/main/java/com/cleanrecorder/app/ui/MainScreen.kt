package com.cleanrecorder.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanrecorder.app.audio.AudioPlayerManager
import com.cleanrecorder.app.data.Recording
import com.cleanrecorder.app.theme.AppColors
import com.cleanrecorder.app.theme.AppRadius

enum class ScreenTab {
    RECORD, LIBRARY
}

@Composable
fun MainScreen() {
    val scope = rememberCoroutineScope()
    val trimmerPlayerManager = remember { AudioPlayerManager(scope) }

    var currentTab by remember { mutableStateOf(ScreenTab.RECORD) }
    var activeTrimmerRecording by remember { mutableStateOf<Recording?>(null) }

    if (activeTrimmerRecording != null) {
        TrimmerScreen(
            recording = activeTrimmerRecording!!,
            playerManager = trimmerPlayerManager,
            onFinished = {
                activeTrimmerRecording = null
                currentTab = ScreenTab.LIBRARY
            },
            onCancel = {
                activeTrimmerRecording = null
            }
        )
    } else {
        Scaffold(
            containerColor = AppColors.Background,
            bottomBar = {
                // Exact bottom segmented toggle matching Image 1
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(AppRadius.pill))
                        .background(Color(0xFF0E1324))
                        .border(1.dp, Color(0xFF1E2746), RoundedCornerShape(AppRadius.pill))
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Record Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(AppRadius.pill))
                                .background(if (currentTab == ScreenTab.RECORD) AppColors.Accent else Color.Transparent)
                                .clickable { currentTab = ScreenTab.RECORD },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Record",
                                color = if (currentTab == ScreenTab.RECORD) Color.White else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Library Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(AppRadius.pill))
                                .background(if (currentTab == ScreenTab.LIBRARY) AppColors.Accent else Color.Transparent)
                                .clickable { currentTab = ScreenTab.LIBRARY },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Library",
                                color = if (currentTab == ScreenTab.LIBRARY) Color.White else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (currentTab) {
                    ScreenTab.RECORD -> RecordScreen(
                        onRecordingSaved = { currentTab = ScreenTab.LIBRARY }
                    )
                    ScreenTab.LIBRARY -> LibraryScreen(
                        onOpenTrimmer = { recording -> activeTrimmerRecording = recording }
                    )
                }
            }
        }
    }
}
