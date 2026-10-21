package com.cleanrecorder.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cleanrecorder.app.theme.AppColors
import com.cleanrecorder.app.theme.AppSpacing

enum class ScreenTab {
    RECORD, LIBRARY
}

@Composable
fun MainScreen() {
    var currentTab by remember { mutableStateOf(ScreenTab.RECORD) }

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
                ScreenTab.RECORD -> RecordScreen()
                ScreenTab.LIBRARY -> LibraryScreen()
            }
        }
    }
}
