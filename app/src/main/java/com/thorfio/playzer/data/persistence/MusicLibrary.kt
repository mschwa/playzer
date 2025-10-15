package com.thorfio.playzer.data.persistence

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.thorfio.playzer.data.model.Album
import com.thorfio.playzer.data.model.Artist
import com.thorfio.playzer.data.model.Track
import com.thorfio.playzer.data.scanner.MediaStoreAudioClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.IOException

@Serializable
data class MusicLibraryData(
    val tracks: List<Track>,
    val albums: List<Album>,
    val artists: List<Artist>
)

/** In memory repository for managing music library data. */
class MusicLibrary {
    companion object {
        private const val TAG = "MusicRepository"
        private const val LIBRARY_CACHE_FILE = "music_library_cache.json"
    }

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    // Track whether we've loaded data from scanning
    private var hasLoadedData = false

    // Coroutine scope for ContentObserver operations
    private val observerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ContentObserver for monitoring MediaStore changes
    private var mediaStoreObserver: ContentObserver? = null
    private var observerContext: Context? = null

    /**
     * Returns true if the library has no tracks
     */
    fun isEmpty(): Boolean = _tracks.value.isEmpty()

    fun tracksByIds(ids: List<Long>) = _tracks.value.filter { it.id in ids }

    fun getTrackById(id: Long) = _tracks.value.find { it.id == id }

    fun search(query: String): Triple<List<Track>, List<Album>, List<Artist>> {
        if (query.isBlank()) return Triple(emptyList(), emptyList(), emptyList())
        val q = query.lowercase()
        return Triple(
            _tracks.value.filter { it.title.lowercase().contains(q) || it.artistName.lowercase().contains(q) },
            _albums.value.filter { it.title.lowercase().contains(q) || it.artistName.lowercase().contains(q) },
            _artists.value.filter { it.name.lowercase().contains(q) }
        )
    }

    fun deleteTracks(trackIds: List<Long>) {
        if (trackIds.isEmpty()) return
        val idSet = trackIds.toSet()
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

    fun removeTrackFromLibrary(trackId: Long?) {
        if (trackId == null) return
        val remainingTracks = _tracks.value.filterNot { it.id == trackId }
        _tracks.value = remainingTracks
        _albums.value = _albums.value.map { al ->
            val newTrackIds = al.trackIds.filterNot { it == trackId }
            al.copy(trackIds = newTrackIds, coverTrackId = newTrackIds.firstOrNull() ?: al.coverTrackId)
        }
        _artists.value = _artists.value.map { ar ->
            val newTrackIds = ar.trackIds.filterNot { it == trackId }
            ar.copy(trackIds = newTrackIds)
        }
    }

    fun removeAlbumFromLibrary(album: Album) {

        val remainingTracks = _tracks.value.filterNot { it.id in album.trackIds }
        _tracks.value = remainingTracks

        val remainingAlbums = _albums.value.filterNot { it.id == album.id }
        _albums.value = remainingAlbums

        val artist = _artists.value.find { it.id == album.artistId }
        if (artist != null) {
            // Check if this is the artist's only album AND if there are no tracks assigned to this artist
            // that still exist in _tracks but are not part of the album being deleted
            val hasOnlyThisAlbum = artist.albumIds.count() == 1
            val artistTracksNotInAlbum = _tracks.value.any { track ->
                track.artistId == artist.id && track.id !in album.trackIds
            }

            if (hasOnlyThisAlbum && !artistTracksNotInAlbum) {
                val remainingArtists = _artists.value.filterNot { it.id == artist.id }
                _artists.value = remainingArtists
            }
        }
    }

    fun removeArtistFromLibrary(artist: Artist) {
        val remainingTracks = _tracks.value.filterNot { it.artistId == artist.id }
        _tracks.value = remainingTracks

        val remainingAlbums = _albums.value.filterNot { it.artistId == artist.id }
        _albums.value = remainingAlbums

        val remainingArtists = _artists.value.filterNot { it.id == artist.id }
        _artists.value = remainingArtists
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
     * Updates the entire library with data from scanned files
     */
    fun refreshLibrary(newTracks: List<Track>, newAlbums: List<Album>, newArtists: List<Artist>) {
        _tracks.value = newTracks
        _albums.value = newAlbums
        _artists.value = newArtists
    }

    /**
     * Clears the entire library data
     */
    fun clearLibrary() {
        _tracks.value = emptyList()
        _albums.value = emptyList()
        _artists.value = emptyList()
    }

    /**
     * Saves the current music library data to disk as JSON
     */
    suspend fun saveToDisk(context: Context) {
        try {
            val libraryData = MusicLibraryData(
                tracks = _tracks.value,
                albums = _albums.value,
                artists = _artists.value
            )

            val json = Json.encodeToString(libraryData)
            val file = File(context.filesDir, LIBRARY_CACHE_FILE)
            file.writeText(json)

            Log.d(TAG, "Music library saved to disk: ${_tracks.value.size} tracks, ${_albums.value.size} albums, ${_artists.value.size} artists")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save music library to disk", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error saving music library", e)
        }
    }

    /**
     * Loads music library data from disk if available
     */
    suspend fun loadFromDisk(context: Context) {
        try {
            val file = File(context.filesDir, LIBRARY_CACHE_FILE)
            if (!file.exists()) {
                Log.d(TAG, "No cached music library found")
                return
            }

            val json = file.readText()
            val libraryData = Json.decodeFromString<MusicLibraryData>(json)

            _tracks.value = libraryData.tracks
            _albums.value = libraryData.albums
            _artists.value = libraryData.artists

            hasLoadedData = true
            Log.d(TAG, "Music library loaded from disk: ${libraryData.tracks.size} tracks, ${libraryData.albums.size} albums, ${libraryData.artists.size} artists")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load music library from disk", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading music library", e)
        }
    }

    /**
     * Starts observing MediaStore for changes to audio files
     */
    fun startObservingMediaStore(context: Context) {
        if (mediaStoreObserver != null) {
            Log.d(TAG, "MediaStore observer already registered")
            return
        }

        observerContext = context.applicationContext

        mediaStoreObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                Log.d(TAG, "MediaStore change detected: $uri")

                if (uri == null) {
                    Log.d(TAG, "URI is null, ignoring change")
                    return
                }

                // Launch coroutine to handle the change
                observerScope.launch {
                    handleMediaStoreChange(context, uri)
                }
            }
        }

        // Register observer for audio media
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaStoreObserver!!
        )

        Log.d(TAG, "MediaStore observer registered")
    }

    /**
     * Stops observing MediaStore changes
     */
    fun stopObservingMediaStore() {
        mediaStoreObserver?.let { observer ->
            observerContext?.contentResolver?.unregisterContentObserver(observer)
            mediaStoreObserver = null
            observerContext = null
            Log.d(TAG, "MediaStore observer unregistered")
        }
    }

    /**
     * Handles MediaStore changes by checking if audio was added or deleted
     */
    private suspend fun handleMediaStoreChange(context: Context, uri: Uri) {
        try {
            // Extract track ID from URI if possible
            val trackId = uri.lastPathSegment?.toLongOrNull()

            if (trackId == null) {
                Log.d(TAG, "Could not extract track ID from URI: $uri")
                return
            }

            // Check if track exists in MediaStore
            val track = MediaStoreAudioClient.getTrackByUri(context, uri)

            if (track != null) {
                // Track exists - it was added or modified
                handleTrackAdded(track)
            } else {
                // Track doesn't exist in MediaStore - it was deleted
                handleTrackDeleted(trackId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling MediaStore change", e)
        }
    }

    /**
     * Handles a track being added to MediaStore
     */
    private fun handleTrackAdded(track: Track) {
        val existingTrack = _tracks.value.find { it.id == track.id }

        if (existingTrack != null) {
            Log.d(TAG, "Track ${track.title} already exists in library, skipping")
            return
        }

        Log.d(TAG, "Adding new track to library: ${track.title}")

        // Add track to library
        _tracks.value = _tracks.value + track

        // Update or create album
        val existingAlbum = _albums.value.find { it.id == track.albumId }
        if (existingAlbum != null) {
            // Update existing album
            _albums.value = _albums.value.map { album ->
                if (album.id == track.albumId) {
                    album.copy(
                        trackIds = (album.trackIds + track.id).distinct(),
                        coverTrackId = album.coverTrackId ?: track.id
                    )
                } else {
                    album
                }
            }
        } else {
            // Create new album
            val newAlbum = Album(
                id = track.albumId,
                title = track.albumTitle,
                artistId = track.artistId,
                artistName = track.artistName,
                trackIds = listOf(track.id),
                coverTrackId = track.id
            )
            _albums.value = _albums.value + newAlbum
        }

        // Update or create artist
        val existingArtist = _artists.value.find { it.id == track.artistId }
        if (existingArtist != null) {
            // Update existing artist
            _artists.value = _artists.value.map { artist ->
                if (artist.id == track.artistId) {
                    val newAlbumIds = if (track.albumId !in artist.albumIds) {
                        artist.albumIds + track.albumId
                    } else {
                        artist.albumIds
                    }
                    artist.copy(
                        trackIds = (artist.trackIds + track.id).distinct(),
                        albumIds = newAlbumIds.distinct()
                    )
                } else {
                    artist
                }
            }
        } else {
            // Create new artist
            val newArtist = Artist(
                id = track.artistId,
                name = track.artistName,
                albumIds = listOf(track.albumId),
                trackIds = listOf(track.id)
            )
            _artists.value = _artists.value + newArtist
        }

        Log.d(TAG, "Track added successfully. Total tracks: ${_tracks.value.size}")
    }

    /**
     * Handles a track being deleted from MediaStore
     */
    private fun handleTrackDeleted(trackId: Long) {
        val track = _tracks.value.find { it.id == trackId }

        if (track == null) {
            Log.d(TAG, "Track ID $trackId not found in library, skipping deletion")
            return
        }

        Log.d(TAG, "Removing track from library: ${track.title}")

        // Use existing function to remove track
        removeTrackFromLibrary(trackId)

        // Clean up empty albums
        _albums.value = _albums.value.filter { it.trackIds.isNotEmpty() }

        // Clean up empty artists
        _artists.value = _artists.value.filter { it.trackIds.isNotEmpty() }

        Log.d(TAG, "Track deleted successfully. Total tracks: ${_tracks.value.size}")
    }
}
