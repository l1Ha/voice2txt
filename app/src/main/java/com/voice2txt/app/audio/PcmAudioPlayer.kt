package com.voice2txt.app.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PcmAudioPlayer(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private var stopAtMs: Long = -1L
    private var loopCurrentSegment = false
    private var loopStartMs = 0L

    init {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    if (playing) startPositionPolling()
                    else stopPositionPolling()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        _isPlaying.value = false
                        stopPositionPolling()
                    }
                }
            })
        }
    }

    private fun startPositionPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && exoPlayer?.isPlaying == true) {
                val pos = exoPlayer?.currentPosition ?: 0L
                _currentPositionMs.value = pos

                // If scheduled to stop at segment end
                if (stopAtMs > 0 && pos >= stopAtMs) {
                    if (loopCurrentSegment) {
                        exoPlayer?.seekTo(loopStartMs)
                        exoPlayer?.play()
                    } else {
                        exoPlayer?.pause()
                        stopAtMs = -1L
                    }
                }
                delay(80)
            }
        }
    }

    private fun stopPositionPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    fun prepare(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
    }

    fun play() {
        stopAtMs = -1L
        loopCurrentSegment = false
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    /**
     * Plays a specific segment interval [startMs, endMs], automatically stopping at endMs (or looping).
     */
    fun playSegment(startMs: Long, endMs: Long, loop: Boolean = false) {
        stopAtMs = endMs
        loopCurrentSegment = loop
        loopStartMs = startMs
        exoPlayer?.seekTo(startMs)
        exoPlayer?.play()
    }

    fun release() {
        stopPositionPolling()
        exoPlayer?.release()
        exoPlayer = null
    }
}
