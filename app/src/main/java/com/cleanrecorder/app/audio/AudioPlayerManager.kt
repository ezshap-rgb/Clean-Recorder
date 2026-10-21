package com.cleanrecorder.app.audio

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayerManager(private val scope: CoroutineScope) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs = _durationMs.asStateFlow()

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath = _currentPlayingPath.asStateFlow()

    fun play(file: File, startFromMs: Int = 0, loopToMs: Int? = null) {
        if (_currentPlayingPath.value == file.absolutePath && mediaPlayer != null) {
            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressTracker(loopToMs)
            return
        }

        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                if (startFromMs > 0) seekTo(startFromMs)
                start()

                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0
                    stopProgressTracker()
                }
            }

            _currentPlayingPath.value = file.absolutePath
            _durationMs.value = mediaPlayer?.duration ?: 0
            _currentPositionMs.value = startFromMs
            _isPlaying.value = true

            startProgressTracker(loopToMs)
        } catch (_: Exception) {
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        _isPlaying.value = false
        stopProgressTracker()
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun stop() {
        stopProgressTracker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _currentPositionMs.value = 0
        _durationMs.value = 0
        _currentPlayingPath.value = null
    }

    private fun startProgressTracker(loopEndMs: Int? = null) {
        stopProgressTracker()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        val current = player.currentPosition
                        _currentPositionMs.value = current

                        if (loopEndMs != null && current >= loopEndMs) {
                            pause()
                        }
                    } catch (_: Exception) {}
                }
                delay(80)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }
}
