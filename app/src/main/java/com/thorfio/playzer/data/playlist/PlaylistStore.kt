package com.thorfio.playzer.data.playlist

import android.content.Context
import com.thorfio.playzer.data.model.Playlist
import com.thorfio.playzer.data.model.PlaylistsDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.util.UUID

/** Simple JSON file backed playlist store. */
class PlaylistStore(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File by lazy { File(context.filesDir, "playlists.json") }

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init { load() }

    private fun load() {
        scope.launch {
            if (!file.exists()) {
                saveInternal()
            } else runCatching {
                val txt = file.readText()
                val doc: PlaylistsDocument = json.decodeFromString(txt)
                _playlists.value = doc.playlists
            }.onFailure { /* ignore */ }
        }
    }

    private fun saveInternal() {
        runCatching {
            val payload: String = json.encodeToString(PlaylistsDocument(_playlists.value))
            file.writeText(payload)
        }
    }

    private fun persistAsync() { scope.launch { saveInternal() } }

    fun create(name: String) {
        val now = Instant.now().toEpochMilli()
        val p = Playlist(id = UUID.randomUUID().toString(), name = name, creationDate = now, lastUpdated = now)
        _playlists.value = _playlists.value + p
        persistAsync()
    }

    fun rename(id: String, newName: String) {
        _playlists.value = _playlists.value.map { if (it.id == id) it.copy(name = newName, lastUpdated = Instant.now().toEpochMilli()) else it }
        persistAsync()
    }

    fun delete(id: String) {
        _playlists.value = _playlists.value.filterNot { it.id == id }
        persistAsync()
    }

    fun addTracks(id: String, trackIds: List<String>) {
        _playlists.value = _playlists.value.map {
            if (it.id == id) it.copy(trackIds = (it.trackIds + trackIds).distinct(), lastUpdated = Instant.now().toEpochMilli()) else it
        }
        persistAsync()
    }

    fun removeTrack(id: String, trackId: String) {
        _playlists.value = _playlists.value.map {
            if (it.id == id) it.copy(trackIds = it.trackIds.filterNot { t -> t == trackId }, lastUpdated = Instant.now().toEpochMilli()) else it
        }
        persistAsync()
    }

    fun removeTracks(id: String, trackIds: List<String>) {
        if (trackIds.isEmpty()) return
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == id) pl.copy(
                trackIds = pl.trackIds.filterNot { it in trackIds },
                lastUpdated = Instant.now().toEpochMilli()
            ) else pl
        }
        persistAsync()
    }

    fun playlist(id: String) = _playlists.value.firstOrNull { it.id == id }

    fun setCover(id: String, trackId: String?) {
        _playlists.value = _playlists.value.map { pl -> if (pl.id == id) pl.copy(coverTrackId = trackId, lastUpdated = Instant.now().toEpochMilli()) else pl }
        persistAsync()
    }

    fun createReturning(name: String): Playlist {
        val now = Instant.now().toEpochMilli()
        val p = Playlist(id = UUID.randomUUID().toString(), name = name, creationDate = now, lastUpdated = now, coverTrackId = null)
        _playlists.value = _playlists.value + p
        persistAsync()
        return p
    }

    fun createAndAdd(name: String, trackIds: List<String>): Playlist {
        val now = Instant.now().toEpochMilli()
        val dedup = trackIds.distinct()
        val p = Playlist(
            id = UUID.randomUUID().toString(),
            name = name,
            creationDate = now,
            lastUpdated = now,
            trackIds = dedup,
            coverTrackId = dedup.firstOrNull()
        )
        _playlists.value = _playlists.value + p
        persistAsync()
        return p
    }

    fun insertTrackAt(id: String, trackId: String, index: Int) {
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == id) {
                val newList = pl.trackIds.toMutableList()
                val safeIndex = index.coerceIn(0, newList.size)
                if (!newList.contains(trackId)) {
                    newList.add(safeIndex, trackId)
                }
                pl.copy(trackIds = newList, lastUpdated = Instant.now().toEpochMilli())
            } else pl
        }
        persistAsync()
    }

    fun moveTrack(id: String, trackId: String, toIndex: Int) {
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == id) {
                val currentIdx = pl.trackIds.indexOf(trackId)
                if (currentIdx == -1) return@map pl
                val mutable = pl.trackIds.toMutableList()
                val maxDest = mutable.size - 1
                val targetRaw = toIndex.coerceIn(0, mutable.size) // allow == size for append
                if (currentIdx != targetRaw) {
                    val item = mutable.removeAt(currentIdx)
                    val safeIndex = if (targetRaw > mutable.size) mutable.size else targetRaw
                    mutable.add(safeIndex, item)
                    pl.copy(trackIds = mutable, lastUpdated = Instant.now().toEpochMilli(), coverTrackId = pl.coverTrackId ?: trackId)
                } else pl
            } else pl
        }
        persistAsync()
    }

    fun updateTrackOrder(playlistId: String, newTrackOrder: List<String>) {
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                pl.copy(trackIds = newTrackOrder, lastUpdated = Instant.now().toEpochMilli())
            } else pl
        }
        persistAsync()
    }

    fun getTrackIds(playlistId: String): List<String> {
        return _playlists.value.firstOrNull { it.id == playlistId }?.trackIds ?: emptyList()
    }
}
