package com.thorfio.playzer.data.queue

import com.thorfio.playzer.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/** Simplified internal playback queue (backed by a list + index). */
class InternalQueue {
    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    val currentTrack: Track? get() = _queue.value.getOrNull(_currentIndex.value)

    fun load(tracks: List<Track>, startAt: Int = 0) {
        _queue.value = tracks
        _currentIndex.value = if (tracks.isEmpty()) -1 else startAt.coerceIn(tracks.indices)
    }

    fun play(trackId: String) {
        val idx = _queue.value.indexOfFirst { it.id == trackId }
        if (idx >= 0) _currentIndex.value = idx
    }

    fun next(): Track? {
        if (_queue.value.isEmpty()) return null
        val newIndex = (_currentIndex.value + 1).coerceAtMost(_queue.value.lastIndex)
        _currentIndex.value = newIndex
        return currentTrack
    }

    fun previous(): Track? {
        if (_queue.value.isEmpty()) return null
        val newIndex = (_currentIndex.value - 1).coerceAtLeast(0)
        _currentIndex.value = newIndex
        return currentTrack
    }

    fun shuffle() {
        if (_queue.value.size < 2) return
        val current = currentTrack
        val shuffled = _queue.value.shuffled(Random(System.currentTimeMillis()))
        _queue.value = shuffled
        current?.let { play(it.id) }
    }

    fun addNext(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val list = _queue.value.toMutableList()
        val insertPos = (_currentIndex.value + 1).coerceAtMost(list.size)
        list.addAll(insertPos, tracks)
        val currentId = currentTrack?.id
        _queue.value = list
        currentId?.let { play(it) }
    }

    fun clear() {
        _queue.value = emptyList()
        _currentIndex.value = -1
    }
}

