package com.cleanrecorder.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.cleanrecorder.app.CleanRecorderApp
import com.cleanrecorder.app.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class RecordingService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val EXTRA_BITRATE_KBPS = "EXTRA_BITRATE_KBPS"
        const val EXTRA_FORMAT = "EXTRA_FORMAT"
        const val NOTIFICATION_ID = 1001

        private val _isRecording = MutableStateFlow(false)
        val isRecording = _isRecording.asStateFlow()

        private val _isPaused = MutableStateFlow(false)
        val isPaused = _isPaused.asStateFlow()

        private val _elapsedSeconds = MutableStateFlow(0L)
        val elapsedSeconds = _elapsedSeconds.asStateFlow()

        private val _amplitude = MutableStateFlow(0f)
        val amplitude = _amplitude.asStateFlow()

        private val _lastFinishedFile = MutableStateFlow<File?>(null)
        val lastFinishedFile = _lastFinishedFile.asStateFlow()

        fun clearLastFinishedFile() {
            _lastFinishedFile.value = null
            _elapsedSeconds.value = 0L // Reset resting timer immediately to 0
        }
    }

    private var recorder: MediaRecorder? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentOutputFile: File? = null
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val bitrate = intent.getIntExtra(EXTRA_BITRATE_KBPS, 16)
                val format = intent.getStringExtra(EXTRA_FORMAT) ?: "AAC"
                startRecording(bitrate, format)
            }
            ACTION_STOP -> stopRecording()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(bitrateKbps: Int, format: String) {
        if (_isRecording.value) return

        acquireWakeLock()
        startForeground(NOTIFICATION_ID, buildNotification("Recording lecture ($bitrateKbps kbps $format)..."))

        val recordingsDir = File(getExternalFilesDir(null), "Recordings").apply { mkdirs() }
        val ext = if (format.equals("MP3", ignoreCase = true)) "mp3" else "m4a"
        val fileName = "Lecture_${System.currentTimeMillis()}.$ext"
        currentOutputFile = File(recordingsDir, fileName)

        val sampleRate = if (bitrateKbps <= 32) 16_000 else 44_100

        try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(bitrateKbps * 1000)
                setAudioSamplingRate(sampleRate)
                setAudioChannels(1)
                setOutputFile(currentOutputFile?.absolutePath)
                prepare()
                start()
            }

            _isRecording.value = true
            _isPaused.value = false
            _elapsedSeconds.value = 0L

            startTicker()
        } catch (e: Exception) {
            stopRecording()
        }
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && _isRecording.value && !_isPaused.value) {
            try {
                recorder?.pause()
                _isPaused.value = true
                updateNotification("Recording paused")
            } catch (_: Exception) {}
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && _isRecording.value && _isPaused.value) {
            try {
                recorder?.resume()
                _isPaused.value = false
                updateNotification("Recording lecture...")
            } catch (_: Exception) {}
        }
    }

    private fun stopRecording() {
        serviceJob?.cancel()
        serviceJob = null

        try {
            recorder?.stop()
            recorder?.release()
        } catch (_: Exception) {}
        recorder = null

        _isRecording.value = false
        _isPaused.value = false
        _amplitude.value = 0f
        _elapsedSeconds.value = 0L // Reset timer to 0 immediately upon stop

        currentOutputFile?.let {
            if (it.exists() && it.length() > 0) {
                _lastFinishedFile.value = it
            }
        }

        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTicker() {
        serviceJob?.cancel()
        serviceJob = serviceScope.launch {
            while (isActive && _isRecording.value) {
                delay(100)
                if (!_isPaused.value) {
                    _elapsedSeconds.value += 1L
                    val amp = recorder?.maxAmplitude ?: 0
                    val normalized = (amp / 32767f).coerceIn(0f, 1f)
                    _amplitude.value = normalized
                } else {
                    _amplitude.value = 0f
                }
            }
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CleanRecorder::RecordingWakeLock"
        ).apply {
            acquire(4 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun buildNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CleanRecorderApp.RECORDING_CHANNEL_ID)
            .setContentTitle("Clean Recorder")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    override fun onDestroy() {
        serviceScope.cancel()
        stopRecording()
        super.onDestroy()
    }
}
