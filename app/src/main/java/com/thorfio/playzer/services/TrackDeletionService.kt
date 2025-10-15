package com.thorfio.playzer.services

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.thorfio.playzer.data.persistence.MusicLibrary
import com.thorfio.playzer.data.persistence.PlaylistStore
import com.thorfio.playzer.data.queue.InternalQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrackDeletionService(
    private val internalQueue: InternalQueue,
    private val musicLibrary: MusicLibrary,
    private val playlistStore: PlaylistStore
) {
    companion object {
        private const val TAG = "TrackDeletionService"
    }

    /**
     * Deletes an audio file using MediaStore API
     * @param context The application context
     * @param fileUri The URI of the file to delete
     * @return true if the file was successfully deleted, false otherwise
     */
    private suspend fun deleteAudioFile(context: Context, fileUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to delete audio file: $fileUri")

            val uri = fileUri.toUri()

            // Use MediaStore to delete the file
            val deletedRows = context.contentResolver.delete(uri, null, null)

            if (deletedRows > 0) {
                Log.d(TAG, "Successfully deleted audio file: $fileUri")
                return@withContext true
            } else {
                Log.e(TAG, "Failed to delete audio file (0 rows deleted): $fileUri")
                return@withContext false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when deleting audio file: $fileUri", e)
            // On Android 10+, user confirmation may be required
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting audio file: $fileUri", e)
            return@withContext false
        }
    }

    suspend fun deleteTrack(context: Context, trackId: Long?) {

        val track = musicLibrary.tracks.value.find { it.id == trackId } ?: return

        if (internalQueue.currentTrack?.id == trackId) {
            // TODO: Notify user that they cannot delete the currently playing track
            return
        } else {

            deleteAudioFile(context, track.fileUri)

            musicLibrary.deleteTrackFromLibrary(trackId)

            // Efficiently update all playlists that contain this track
            // by removing the track in a single operation per playlist
            playlistStore.playlists.value.forEach { playlist ->
                if (playlist.mediaStoreIds.contains(track.id)) {
                    playlistStore.removeTrack(playlist.id, track.id)
                }
            }
        }
    }

    suspend fun deleteAlbum(context: Context, albumId: String?) {

        val album = musicLibrary.albums.value.find { it.id == albumId } ?: return

        if (internalQueue.currentTrack?.id in album.trackIds) {
            // TODO: Notify user that they cannot delete the currently playing track
            return
        } else {
            // First delete all audio files belonging to the album
            album.trackIds.forEach { trackId ->
                val track = musicLibrary.getTrackById(trackId)
                if (track != null) {
                    deleteAudioFile(context, track.fileUri)
                }
            }

            // Collect all file URIs from tracks that belonged to the deleted album
            val deletedTrackMediaStoreIds = album.trackIds
                .mapNotNull { musicLibrary.getTrackById(it)?.id }

            // Delete the album from the repository
            musicLibrary.deleteAlbumFromLibrary(album)

            // Efficiently update all playlists that contain any of the deleted tracks
            playlistStore.playlists.value.forEach { playlist ->
                // Find which of the deleted track URIs are in this playlist
                val urisToRemove = deletedTrackMediaStoreIds.filter { mediaStoreId ->
                    playlist.mediaStoreIds.contains(mediaStoreId)
                }

                // Only update the playlist if it contains any of the deleted tracks
                if (urisToRemove.isNotEmpty()) {
                    playlistStore.removeTracks(playlist.id, urisToRemove)
                }
            }
        }
    }

    suspend fun deleteArtist(context: Context, artistId: String?) {

        val artist = musicLibrary.artists.value.find { it.id == artistId } ?: return

        if (internalQueue.currentTrack?.id in artist.trackIds) {
            // TODO: Notify user that they cannot delete the currently playing track
            return
        } else {

            artist.trackIds.forEach { ar ->
                val track = musicLibrary.getTrackById(ar)
                if (track != null) {
                    deleteAudioFile(context, track.fileUri)
                }
            }
        }

        musicLibrary.deleteArtistFromLibrary(artist)

        // Collect all file URIs from tracks that belonged to the deleted artist
        val deletedTrackMediaStoreIds = artist.trackIds
            .mapNotNull { musicLibrary.getTrackById(it)?.id }

        // Efficiently update all playlists that contain any of the deleted tracks
        // by removing those tracks in a single operation per playlist
        playlistStore.playlists.value.forEach { playlist ->
            // Find which of the deleted track URIs are in this playlist
            val mediaStoreIdsToRemove = deletedTrackMediaStoreIds.filter { mediaStoreId ->
                playlist.mediaStoreIds.contains(mediaStoreId)
            }

            // Only update the playlist if it contains any of the deleted tracks
            if (mediaStoreIdsToRemove.isNotEmpty()) {
                playlistStore.removeTracks(playlist.id, mediaStoreIdsToRemove)
            }
        }
    }
}
