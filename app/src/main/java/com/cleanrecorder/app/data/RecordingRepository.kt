package com.cleanrecorder.app.data

import android.content.Context
import android.media.MediaMetadataRetriever
import java.io.File

class RecordingRepository(private val context: Context) {

    private val recordingsDir: File
        get() = File(context.getExternalFilesDir(null), "Recordings").apply { mkdirs() }

    fun getRecordings(): List<Recording> {
        val files = recordingsDir.listFiles { file ->
            file.isFile && (file.name.endsWith(".m4a", ignoreCase = true) ||
                    file.name.endsWith(".wav", ignoreCase = true) ||
                    file.name.endsWith(".mp3", ignoreCase = true))
        } ?: return emptyList()

        return files.mapNotNull { file ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMs = durStr?.toLongOrNull() ?: 0L
                retriever.release()

                val ext = file.extension.uppercase()
                val baseName = file.nameWithoutExtension

                Recording(
                    file = file,
                    name = baseName,
                    durationMs = durationMs,
                    sizeBytes = file.length(),
                    createdAtMs = file.lastModified(),
                    format = ext
                )
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.createdAtMs }
    }

    fun renameRecording(recording: Recording, newName: String): File? {
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) return null
        val target = File(recordingsDir, "$cleanName.${recording.file.extension}")
        if (target.exists() && target.absolutePath != recording.file.absolutePath) {
            return null // Collision
        }
        val success = recording.file.renameTo(target)
        return if (success) target else null
    }

    fun deleteRecording(recording: Recording): Boolean {
        return recording.file.delete()
    }
}
