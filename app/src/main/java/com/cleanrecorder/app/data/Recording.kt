package com.cleanrecorder.app.data

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Recording(
    val file: File,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val createdAtMs: Long,
    val format: String = "M4A"
) {
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format(Locale.US, "%.1f MB", mb)
            } else {
                String.format(Locale.US, "%.0f KB", kb)
            }
        }

    val formattedDate: String
        get() {
            val formatter = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
            return formatter.format(Date(createdAtMs))
        }
}
