package com.thorfio.playzer.data.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.thorfio.playzer.data.model.Track
import com.thorfio.playzer.data.queue.InternalQueue
import com.thorfio.playzer.data.repo.MusicRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PlaybackController(
    context: Context,
    private val musicRepository: MusicRepository,
    private val internalQueue: InternalQueue
) {
    private val player = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    // Add position tracking
    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    // Position update job
    private var positionUpdateJob: Job? = null

    init {
        scope.launch {
            internalQueue.currentIndex.collect {
                _currentTrack.value = internalQueue.currentTrack
            }
        }

        // Listen for player state changes
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                updatePositionTracking()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                _isPlaying.value = isPlaying
                updatePositionTracking()
            }
        })
    }

    // Start or stop position tracking based on playback state
    private fun updatePositionTracking() {
        positionUpdateJob?.cancel()

        if (player.isPlaying) {
            positionUpdateJob = scope.launch {
                while (isActive) {
                    _currentPositionMs.value = player.currentPosition
                    delay(100) // Update position 10 times per second
                }
            }
        } else {
            // Update position one last time when paused
            _currentPositionMs.value = player.currentPosition
        }
    }

    fun loadAndPlay(tracks: List<Track>, startAt: Int = 0) {
        internalQueue.load(tracks, startAt)
        _currentTrack.value = internalQueue.currentTrack
        rebuildPlayerQueue()
        player.prepare()
        player.playWhenReady = true
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun next() {
        internalQueue.next()
        player.seekToNextMediaItem()
    }

    fun previous() {
        internalQueue.previous()
        player.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    private fun rebuildPlayerQueue() {
        player.clearMediaItems()
        internalQueue.queue.value.forEach { t ->
            player.addMediaItem(MediaItem.Builder().setMediaId(t.id).setUri(t.fileUri).setTag(t).build())
        }
        val idx = internalQueue.currentIndex.value
        if (idx >= 0) player.seekToDefaultPosition(idx)
    }

    fun release() {
        positionUpdateJob?.cancel()
        player.release()
        scope.cancel()
    }
}
