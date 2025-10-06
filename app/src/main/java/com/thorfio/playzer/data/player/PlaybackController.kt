package com.thorfio.playzer.data.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.thorfio.playzer.data.model.Track
import com.thorfio.playzer.data.queue.InternalQueue
import com.thorfio.playzer.data.repo.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    // Save state periodically job
    private var saveStateJob: Job? = null

    init {
        scope.launch {
            internalQueue.currentIndex.collect {
                val track = internalQueue.currentTrack
                _currentTrack.value = track

                // Update position when track changes
                track?.let {
                    _currentPositionMs.value = internalQueue.lastPosition.value
                }
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
                updateStateSaving(isPlaying)
            }
        })

        // Set the player position from persisted state on initialization
        restorePlaybackState()

        // Start periodic state saving
        updateStateSaving(false)
    }

    private fun restorePlaybackState() {
        // Queue is already loaded from disk by InternalQueue init
        val currentTrack = internalQueue.currentTrack
        val lastPosition = internalQueue.lastPosition.value

        if (currentTrack != null) {
            // Prepare the player with the queue
            rebuildPlayerQueue()
            // Seek to the last position
            seekTo(lastPosition)
            // Set current track
            _currentTrack.value = currentTrack
        }
    }

    // Start or stop position tracking based on playback state
    private fun updatePositionTracking() {
        positionUpdateJob?.cancel()

        if (player.isPlaying) {
            positionUpdateJob = scope.launch {
                while (isActive) {
                    val position = player.currentPosition
                    _currentPositionMs.value = position
                    delay(100) // Update position 10 times per second
                }
            }
        } else {
            // Update position one last time when paused
            _currentPositionMs.value = player.currentPosition
        }
    }

    // Start or stop periodic state saving
    private fun updateStateSaving(isPlaying: Boolean) {
        saveStateJob?.cancel()

        // Save state immediately when paused
        if (!isPlaying) {
            internalQueue.updatePosition(_currentPositionMs.value)
        }

        // Set up periodic saving while playing
        saveStateJob = scope.launch {
            while (isActive) {
                internalQueue.updatePosition(_currentPositionMs.value)
                delay(5000) // Save every 5 seconds
            }
        }
    }

    // New method to load a track without automatically playing it
    fun loadTrack(tracks: List<Track>, startAt: Int = 0, autoPlay: Boolean = true) {
        internalQueue.load(tracks, startAt)
        _currentTrack.value = internalQueue.currentTrack
        rebuildPlayerQueue()
        player.prepare()
        player.playWhenReady = autoPlay
    }

    fun loadAndPlay(tracks: List<Track>, startAt: Int = 0) {
        loadTrack(tracks, startAt, true)
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

        // Update position in internalQueue
        internalQueue.updatePosition(positionMs)
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
        // Save position before releasing
        internalQueue.updatePosition(_currentPositionMs.value)

        positionUpdateJob?.cancel()
        saveStateJob?.cancel()
        player.release()
        scope.cancel()
    }
}
