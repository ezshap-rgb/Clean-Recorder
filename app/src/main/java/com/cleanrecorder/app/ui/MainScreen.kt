package com.cleanrecorder.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cleanrecorder.app.audio.AudioPlayerManager
import com.cleanrecorder.app.data.Recording
import com.cleanrecorder.app.theme.AppColors

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
                NavigationBar(
                    containerColor = AppColors.Surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.RECORD,
                        onClick = { currentTab = ScreenTab.RECORD },
                        icon = { Icon(Icons.Default.Mic, contentDescription = "Record") },
                        label = { Text("Record") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppColors.Text,
                            selectedTextColor = AppColors.Text,
                            indicatorColor = AppColors.Accent,
                            unselectedIconColor = AppColors.TextFaint,
                            unselectedTextColor = AppColors.TextFaint
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.LIBRARY,
                        onClick = { currentTab = ScreenTab.LIBRARY },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Library") },
                        label = { Text("Library") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppColors.Text,
                            selectedTextColor = AppColors.Text,
                            indicatorColor = AppColors.Accent,
                            unselectedIconColor = AppColors.TextFaint,
                            unselectedTextColor = AppColors.TextFaint
                        )
                    )
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
