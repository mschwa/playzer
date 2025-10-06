package com.thorfio.playzer.data.repo

import android.util.Log
import com.thorfio.playzer.data.model.Album
import com.thorfio.playzer.data.model.Artist
import com.thorfio.playzer.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    // Track whether we've loaded data from scanning
    private var hasLoadedData = false

    fun tracksByIds(ids: List<String>) = _tracks.value.filter { it.id in ids }

    /**
     * Get tracks by their file URIs
     */
    fun tracksByFileUris(fileUris: List<String>) = _tracks.value.filter { it.fileUri in fileUris }

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
        // Check if we have tracks to add
        if (newTracks.isEmpty()) {
            Log.d(TAG, "No new tracks to update")
            return
        }

        Log.d(TAG, "Updating library with ${newTracks.size} tracks, ${newAlbums.size} albums, ${newArtists.size} artists")

        // Get existing track IDs to avoid duplicates
        val existingTrackIds = _tracks.value.map { it.id }.toSet()

        // Add new tracks that don't exist yet
        val tracksToAdd = newTracks.filter { it.id !in existingTrackIds }
        Log.d(TAG, "Adding ${tracksToAdd.size} new tracks")

        // Store the updated tracks
        _tracks.value = _tracks.value + tracksToAdd

        // Update albums
        val existingAlbumIds = _albums.value.map { it.id }.toSet()
        val albumsToAdd = newAlbums.filter { it.id !in existingAlbumIds }
        Log.d(TAG, "Adding ${albumsToAdd.size} new albums")

        _albums.value = _albums.value
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
        val existingArtistIds = _artists.value.map { it.id }.toSet()
        val artistsToAdd = newArtists.filter { it.id !in existingArtistIds }
        Log.d(TAG, "Adding ${artistsToAdd.size} new artists")

        _artists.value = _artists.value
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
