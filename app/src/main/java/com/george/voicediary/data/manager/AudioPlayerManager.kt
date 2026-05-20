package com.george.voicediary.data.manager

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) : Player.Listener {

    private var player: ExoPlayer? = null
    private var updateJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _currentFilePath = MutableStateFlow<String?>(null)
    val currentFilePath: StateFlow<String?> = _currentFilePath.asStateFlow()

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        Handler(Looper.getMainLooper()).post {
            player = ExoPlayer.Builder(context).build().apply {
                addListener(this@AudioPlayerManager)
            }
        }
    }

    fun play(filePath: String) {
        Handler(Looper.getMainLooper()).post {
            if (_currentFilePath.value != filePath) {
                player?.apply {
                    val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
                _currentFilePath.value = filePath
            } else {
                player?.play()
            }
            _isPlaying.value = true
            startProgressUpdate()
        }
    }

    fun pause() {
        Handler(Looper.getMainLooper()).post {
            player?.pause()
            _isPlaying.value = false
            stopProgressUpdate()
        }
    }

    fun resume() {
        Handler(Looper.getMainLooper()).post {
            player?.play()
            _isPlaying.value = true
            startProgressUpdate()
        }
    }

    fun seekTo(positionMs: Long) {
        Handler(Looper.getMainLooper()).post {
            player?.seekTo(positionMs)
            _currentPositionMs.value = positionMs
        }
    }

    fun setSpeed(speed: Float) {
        Handler(Looper.getMainLooper()).post {
            player?.setPlaybackSpeed(speed)
        }
    }

    fun release() {
        Handler(Looper.getMainLooper()).post {
            player?.release()
            player = null
            stopProgressUpdate()
            _isPlaying.value = false
            _currentPositionMs.value = 0L
            _durationMs.value = 0L
            _currentFilePath.value = null
        }
    }

    private fun startProgressUpdate() {
        updateJob?.cancel()
        updateJob = CoroutineScope(Dispatchers.Default).launch {
            while (_isPlaying.value) {
                withContext(Dispatchers.Main) {
                    _currentPositionMs.value = player?.currentPosition ?: 0L
                    _durationMs.value = player?.duration?.coerceAtLeast(0L) ?: 0L
                }
                delay(500)
            }
        }
    }

    private fun stopProgressUpdate() {
        updateJob?.cancel()
        updateJob = null
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            _isPlaying.value = false
            _currentPositionMs.value = 0L
            stopProgressUpdate()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        // Log the error or handle it appropriately
        _isPlaying.value = false
        stopProgressUpdate()
    }
}