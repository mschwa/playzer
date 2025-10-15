package com.thorfio.playzer.data.queue

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thorfio.playzer.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.random.Random

/** Simplified internal playback queue (backed by a list + index with persistence). */
class InternalQueue(private val context: Context) {
    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // Add last position tracking
    private val _lastPosition = MutableStateFlow(0L)
    val lastPosition: StateFlow<Long> = _lastPosition.asStateFlow()

    val currentTrack: Track? get() = _queue.value.getOrNull(_currentIndex.value)

    // File to store queue data
    private val queueFile: File = File(context.filesDir, QUEUE_FILENAME)
    private val gson = Gson()

    init {
        // Load persisted queue on initialization
        loadFromDisk()
    }

    fun load(tracks: List<Track>, startAt: Int = 0) {
        _queue.value = tracks
        _currentIndex.value = if (tracks.isEmpty()) -1 else startAt.coerceIn(tracks.indices)
        saveToDisk()
    }

    fun play(trackId: Long) {
        val idx = _queue.value.indexOfFirst { it.id == trackId }
        if (idx >= 0) {
            _currentIndex.value = idx
            saveToDisk()
        }
    }

    fun next(): Track? {
        if (_queue.value.isEmpty()) return null
        val newIndex = (_currentIndex.value + 1).coerceAtMost(_queue.value.lastIndex)
        _currentIndex.value = newIndex
        saveToDisk()
        return currentTrack
    }

    fun previous(): Track? {
        if (_queue.value.isEmpty()) return null
        val newIndex = (_currentIndex.value - 1).coerceAtLeast(0)
        _currentIndex.value = newIndex
        saveToDisk()
        return currentTrack
    }

    fun shuffle() {
        if (_queue.value.size < 2) return
        val current = currentTrack
        val shuffled = _queue.value.shuffled(Random(System.currentTimeMillis()))
        _queue.value = shuffled
        current?.let { play(it.id) }
        saveToDisk()
    }

    fun addNext(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val list = _queue.value.toMutableList()
        val insertPos = (_currentIndex.value + 1).coerceAtMost(list.size)
        list.addAll(insertPos, tracks)
        val currentId = currentTrack?.id
        _queue.value = list
        currentId?.let { play(it) }
        saveToDisk()
    }

    fun clear() {
        _queue.value = emptyList()
        _currentIndex.value = -1
        _lastPosition.value = 0L
        saveToDisk()
    }

    /**
     * Update the last position of the current track
     */
    fun updatePosition(positionMs: Long) {
        _lastPosition.value = positionMs
        saveToDisk()
    }

    /**
     * Save the queue state to disk
     */
    private fun saveToDisk() {
        try {
            val queueData = QueueData(
                tracks = _queue.value,
                currentIndex = _currentIndex.value,
                lastPosition = _lastPosition.value
            )
            val json = gson.toJson(queueData)
            queueFile.writeText(json)
        } catch (e: Exception) {
            // Handle potential saving errors
            e.printStackTrace()
        }
    }

    /**
     * Load the queue state from disk
     */
    private fun loadFromDisk() {
        try {
            if (queueFile.exists()) {
                val json = queueFile.readText()
                val typeToken = object : TypeToken<QueueData>() {}.type
                val queueData = gson.fromJson<QueueData>(json, typeToken)

                _queue.value = queueData.tracks
                _currentIndex.value = queueData.currentIndex
                _lastPosition.value = queueData.lastPosition
            }
        } catch (e: Exception) {
            // Handle potential loading errors
            e.printStackTrace()
            // Reset to defaults on error
            _queue.value = emptyList()
            _currentIndex.value = -1
            _lastPosition.value = 0L
        }
    }

    companion object {
        private const val QUEUE_FILENAME = "internal_queue.json"
    }

    /**
     * Data class for persisting queue state
     */
    private data class QueueData(
        val tracks: List<Track>,
        val currentIndex: Int,
        val lastPosition: Long
    )
}
