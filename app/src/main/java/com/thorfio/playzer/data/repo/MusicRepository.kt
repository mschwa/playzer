package com.thorfio.playzer.data.repo

import android.util.Log
import com.thorfio.playzer.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/** Repository managing music library data. */
class MusicRepository {
    companion object {
        private const val TAG = "MusicRepository"
    }

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    // Track whether we've loaded data - either from sample or from scanning
    private var hasLoadedData = false

    init { seedSampleIfNeeded() }

    private fun seedSampleIfNeeded() {
        if (hasLoadedData || _tracks.value.isNotEmpty()) return
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
        hasLoadedData = true
        Log.d(TAG, "Seeded sample data: ${sampleTracks.size} tracks")
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

    /**
     * Updates the entire library with data from scanned files
     */
    fun updateLibrary(newTracks: List<Track>, newAlbums: List<Album>, newArtists: List<Artist>) {
        // Only replace the sample data if we have actual files
        if (newTracks.isEmpty()) {
            Log.d(TAG, "No new tracks to update")
            return
        }

        Log.d(TAG, "Updating library with ${newTracks.size} tracks, ${newAlbums.size} albums, ${newArtists.size} artists")

        // If we previously had sample data only, replace everything
        if (_tracks.value.all { it.fileUri.startsWith("sample://") }) {
            Log.d(TAG, "Replacing sample data with real tracks")
            _tracks.value = newTracks
            _albums.value = newAlbums
            _artists.value = newArtists
            hasLoadedData = true
            return
        }

        // Otherwise, merge with existing real data
        val existingRealTracks = _tracks.value.filter { !it.fileUri.startsWith("sample://") }
        val existingTrackIds = existingRealTracks.map { it.id }.toSet()

        Log.d(TAG, "Merging with existing data. Current real tracks: ${existingRealTracks.size}")

        // Add new tracks that don't exist yet
        val tracksToAdd = newTracks.filter { it.id !in existingTrackIds }
        Log.d(TAG, "Adding ${tracksToAdd.size} new tracks")

        // Important: Store the updated tracks
        _tracks.value = existingRealTracks + tracksToAdd

        // Update albums
        val existingAlbumIds = _albums.value
            .filter { album -> album.trackIds.any { trackId ->
                _tracks.value.find { it.id == trackId }?.fileUri?.startsWith("sample://") == false
            }}
            .map { it.id }.toSet()

        val albumsToAdd = newAlbums.filter { it.id !in existingAlbumIds }
        Log.d(TAG, "Adding ${albumsToAdd.size} new albums")

        _albums.value = _albums.value
            .filter { it.id in existingAlbumIds }
            .map { album ->
                // Find new tracks for this album
                val newTrackIdsForAlbum = newTracks
                    .filter { it.albumId == album.id }
                    .map { it.id }

                // Update track IDs and cover if needed
                album.copy(
                    trackIds = (album.trackIds + newTrackIdsForAlbum).distinct(),
                    coverTrackId = album.coverTrackId ?: newTrackIdsForAlbum.firstOrNull()
                )
            } + albumsToAdd

        // Update artists
        val existingArtistIds = _artists.value
            .filter { artist -> artist.trackIds.any { trackId ->
                _tracks.value.find { it.id == trackId }?.fileUri?.startsWith("sample://") == false
            }}
            .map { it.id }.toSet()

        val artistsToAdd = newArtists.filter { it.id !in existingArtistIds }
        Log.d(TAG, "Adding ${artistsToAdd.size} new artists")

        _artists.value = _artists.value
            .filter { it.id in existingArtistIds }
            .map { artist ->
                // Find new tracks for this artist
                val newTrackIdsForArtist = newTracks
                    .filter { it.artistId == artist.id }
                    .map { it.id }

                // Find new albums for this artist
                val newAlbumIdsForArtist = newAlbums
                    .filter { it.artistId == artist.id }
                    .map { it.id }

                // Update IDs
                artist.copy(
                    trackIds = (artist.trackIds + newTrackIdsForArtist).distinct(),
                    albumIds = (artist.albumIds + newAlbumIdsForArtist).distinct()
                )
            } + artistsToAdd

        Log.d(TAG, "Library update complete. Total tracks: ${_tracks.value.size}")
        hasLoadedData = true
    }

    /**
     * Forces a notification that data has changed to trigger UI updates
     */
    fun notifyDataChanged() {
        Log.d(TAG, "Forcing data change notification")
        // Re-emit the current values to trigger observers
        _tracks.value = _tracks.value
        _albums.value = _albums.value
        _artists.value = _artists.value
    }
}
