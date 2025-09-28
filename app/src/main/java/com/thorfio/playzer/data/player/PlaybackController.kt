package com.thorfio.playzer.data.player

import android.content.Context
import androidx.media3.common.MediaItem
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

    init {
        scope.launch {
            internalQueue.currentIndex.collect {
                _currentTrack.value = internalQueue.currentTrack
            }
        }
    }

    fun loadAndPlay(tracks: List<Track>, startAt: Int = 0) {
        internalQueue.load(tracks, startAt)
        _currentTrack.value = internalQueue.currentTrack
        rebuildPlayerQueue()
        player.prepare()
        player.playWhenReady = true
        _isPlaying.value = true
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause(); _isPlaying.value = false
        } else {
            player.play(); _isPlaying.value = true
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

    private fun rebuildPlayerQueue() {
        player.clearMediaItems()
        internalQueue.queue.value.forEach { t ->
            player.addMediaItem(MediaItem.Builder().setMediaId(t.id).setUri(t.fileUri).setTag(t).build())
        }
        val idx = internalQueue.currentIndex.value
        if (idx >= 0) player.seekToDefaultPosition(idx)
    }

    fun release() {
        player.release()
        scope.cancel()
    }
}
