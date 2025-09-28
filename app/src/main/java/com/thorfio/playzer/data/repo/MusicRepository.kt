package com.thorfio.playzer.data.repo

import com.thorfio.playzer.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/** In-memory placeholder repository. A future implementation would scan device storage. */
class MusicRepository {
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    init { seedSample() }

    private fun seedSample() {
        if (_tracks.value.isNotEmpty()) return
        val artistA = Artist(id = UUID.randomUUID().toString(), name = "Aria Nova", albumIds = emptyList(), trackIds = emptyList())
        val artistB = Artist(id = UUID.randomUUID().toString(), name = "Echo Drift", albumIds = emptyList(), trackIds = emptyList())
        val albumA = Album(id = UUID.randomUUID().toString(), title = "Midnight Waves", artistId = artistA.id, artistName = artistA.name, trackIds = emptyList(), coverTrackId = null)
        val albumB = Album(id = UUID.randomUUID().toString(), title = "Neon Sky", artistId = artistB.id, artistName = artistB.name, trackIds = emptyList(), coverTrackId = null)
        val sampleTracks = listOf(
            Track(title = "Ocean Pulse", artistId = artistA.id, artistName = artistA.name, albumId = albumA.id, albumTitle = albumA.title, durationMs = 210000, fileUri = "sample://ocean_pulse"),
            Track(title = "Shoreline", artistId = artistA.id, artistName = artistA.name, albumId = albumA.id, albumTitle = albumA.title, durationMs = 189000, fileUri = "sample://shoreline"),
            Track(title = "Chromatic Dawn", artistId = artistB.id, artistName = artistB.name, albumId = albumB.id, albumTitle = albumB.title, durationMs = 243000, fileUri = "sample://chromatic_dawn"),
            Track(title = "Starlit Drift", artistId = artistB.id, artistName = artistB.name, albumId = albumB.id, albumTitle = albumB.title, durationMs = 201000, fileUri = "sample://starlit_drift"),
        )
        val updatedAlbumA = albumA.copy(trackIds = sampleTracks.filter { it.albumId == albumA.id }.map { it.id }, coverTrackId = sampleTracks.first().id)
        val updatedAlbumB = albumB.copy(trackIds = sampleTracks.filter { it.albumId == albumB.id }.map { it.id }, coverTrackId = sampleTracks[2].id)
        val updatedArtistA = artistA.copy(trackIds = sampleTracks.filter { it.artistId == artistA.id }.map { it.id }, albumIds = listOf(updatedAlbumA.id))
        val updatedArtistB = artistB.copy(trackIds = sampleTracks.filter { it.artistId == artistB.id }.map { it.id }, albumIds = listOf(updatedAlbumB.id))
        _tracks.value = sampleTracks
        _albums.value = listOf(updatedAlbumA, updatedAlbumB)
        _artists.value = listOf(updatedArtistA, updatedArtistB)
    }

    fun tracksByIds(ids: List<String>) = _tracks.value.filter { it.id in ids }
    fun album(id: String) = _albums.value.firstOrNull { it.id == id }
    fun artist(id: String) = _artists.value.firstOrNull { it.id == id }

    fun search(query: String): Triple<List<Track>, List<Album>, List<Artist>> {
        if (query.isBlank()) return Triple(emptyList(), emptyList(), emptyList())
        val q = query.lowercase()
        return Triple(
            _tracks.value.filter { it.title.lowercase().contains(q) || it.artistName.lowercase().contains(q) },
            _albums.value.filter { it.title.lowercase().contains(q) || it.artistName.lowercase().contains(q) },
            _artists.value.filter { it.name.lowercase().contains(q) }
        )
    }

    fun deleteTracks(ids: List<String>) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        val remainingTracks = _tracks.value.filterNot { it.id in idSet }
        _tracks.value = remainingTracks
        _albums.value = _albums.value.map { al ->
            val newTrackIds = al.trackIds.filterNot { it in idSet }
            al.copy(trackIds = newTrackIds, coverTrackId = newTrackIds.firstOrNull() ?: al.coverTrackId)
        }
        _artists.value = _artists.value.map { ar ->
            val newTrackIds = ar.trackIds.filterNot { it in idSet }
            ar.copy(trackIds = newTrackIds)
        }
    }

    fun restoreTracks(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val existingIds = _tracks.value.map { it.id }.toSet()
        val toAdd = tracks.filter { it.id !in existingIds }
        if (toAdd.isEmpty()) return
        _tracks.value = _tracks.value + toAdd
        // Update albums
        _albums.value = _albums.value.map { al ->
            val added = toAdd.filter { it.albumId == al.id }.map { it.id }
            if (added.isEmpty()) al else al.copy(trackIds = (al.trackIds + added).distinct(), coverTrackId = al.coverTrackId ?: added.firstOrNull())
        }
        // Update artists
        _artists.value = _artists.value.map { ar ->
            val added = toAdd.filter { it.artistId == ar.id }.map { it.id }
            if (added.isEmpty()) ar else ar.copy(trackIds = (ar.trackIds + added).distinct())
        }
    }
}
