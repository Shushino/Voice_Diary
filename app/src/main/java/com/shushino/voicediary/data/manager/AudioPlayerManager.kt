package com.shushino.voicediary.data.manager

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.shushino.voicediary.data.service.AudioPlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var controller: MediaController? = null
    private var updateJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _currentFilePath = MutableStateFlow<String?>(null)
    val currentFilePath: StateFlow<String?> = _currentFilePath.asStateFlow()

    private val actionQueue = mutableListOf<(MediaController) -> Unit>()

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                val mediaController = controllerFuture.get()
                controller = mediaController
                mediaController.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) startProgressUpdate() else stopProgressUpdate()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _durationMs.value = mediaController.duration
                        } else if (playbackState == Player.STATE_ENDED) {
                            _isPlaying.value = false
                            _currentPositionMs.value = 0
                            stopProgressUpdate()
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        _currentFilePath.value = mediaItem?.mediaId
                        _currentPositionMs.value = 0
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _isPlaying.value = false
                        stopProgressUpdate()
                    }
                })
                
                // Process queued actions
                actionQueue.forEach { it(mediaController) }
                actionQueue.clear()
            } catch (e: Exception) {
                // Log error
            }
        }, MoreExecutors.directExecutor())
    }

    private fun runOnController(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            action(c)
        } else {
            actionQueue.add(action)
        }
    }

    fun play(filePath: String) {
        runOnController { controller ->
            if (_currentFilePath.value == filePath) {
                controller.play()
            } else {
                val mediaItem = MediaItem.Builder()
                    .setMediaId(filePath)
                    .setUri(filePath)
                    .build()
                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.play()
                _currentFilePath.value = filePath
            }
        }
    }

    fun pause() {
        runOnController { it.pause() }
    }

    fun resume() {
        runOnController { it.play() }
    }

    fun seekTo(positionMs: Long) {
        runOnController { it.seekTo(positionMs) }
    }

    fun setSpeed(speed: Float) {
        runOnController { it.setPlaybackSpeed(speed) }
    }

    fun release() {
        stopProgressUpdate()
        scope.cancel()
        controller?.let {
            it.release()
            controller = null
        }
    }

    private fun startProgressUpdate() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                controller?.let {
                    _currentPositionMs.value = it.currentPosition
                }
                delay(100)
            }
        }
    }

    private fun stopProgressUpdate() {
        updateJob?.cancel()
        updateJob = null
    }
}
