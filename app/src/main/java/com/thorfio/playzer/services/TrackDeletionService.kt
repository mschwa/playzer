package com.thorfio.playzer.services

import android.content.Context
import com.thorfio.playzer.data.persistence.MusicRepository
import com.thorfio.playzer.data.persistence.PlaylistStore
import com.thorfio.playzer.data.queue.InternalQueue
import com.thorfio.playzer.data.scanner.AudioFileUtils

class TrackDeletionService(
    context: Context,
    private val internalQueue: InternalQueue,
    private val musicRepository: MusicRepository,
    private val playlistStore: PlaylistStore
) {

    suspend fun deleteTrack(context: Context, trackId: String?) {

        val track = musicRepository.tracks.value.find { it.id == trackId } ?: return

        if (internalQueue.currentTrack?.id == trackId) {
            // TODO: Notify user that they cannot delete the currently playing track
            return
        } else {

            AudioFileUtils.deleteAudioFile(context, track.fileUri)

            musicRepository.deleteTrackFromLibrary(trackId)

            // Efficiently update all playlists that contain this track
            // by removing the track in a single operation per playlist
            playlistStore.playlists.value.forEach { playlist ->
                if (playlist.fileUris.contains(track.fileUri)) {
                    playlistStore.removeTrack(playlist.id, track.fileUri)
                }
            }
        }
    }

    suspend fun deleteAlbum(context: Context, albumId: String?) {

        val album = musicRepository.albums.value.find { it.id == albumId } ?: return

        if (internalQueue.currentTrack?.id in album.trackIds) {
            // TODO: Notify user that they cannot delete the currently playing track
            return
        } else {
            // First delete all audio files belonging to the album
            album.trackIds.forEach { trackId ->
                val track = musicRepository.getTrackById(trackId)
                if (track != null) {
                    AudioFileUtils.deleteAudioFile(context, track.fileUri)
                }
            }

            // Collect all file URIs from tracks that belonged to the deleted album
            val deletedTrackFileUris = album.trackIds
                .mapNotNull { musicRepository.getTrackById(it)?.fileUri }

            // Delete the album from the repository
            musicRepository.deleteAlbumFromLibrary(album)

            // Efficiently update all playlists that contain any of the deleted tracks
            playlistStore.playlists.value.forEach { playlist ->
                // Find which of the deleted track URIs are in this playlist
                val urisToRemove = deletedTrackFileUris.filter { uri ->
                    playlist.fileUris.contains(uri)
                }

                // Only update the playlist if it contains any of the deleted tracks
                if (urisToRemove.isNotEmpty()) {
                    playlistStore.removeTracks(playlist.id, urisToRemove)
                }
            }
        }
    }

    suspend fun deleteArtist(context: Context, artistId: String?) {

        val artist = musicRepository.artists.value.find { it.id == artistId } ?: return

        if (internalQueue.currentTrack?.id in artist.trackIds) {
            // TODO: Notify user that they cannot delete the currently playing track
            return
        } else {

            artist.trackIds.forEach { ar ->
                val track = musicRepository.getTrackById(ar)
                if (track != null) {
                    AudioFileUtils.deleteAudioFile(context, track.fileUri)
                }
            }
        }

        musicRepository.deleteArtistFromLibrary(artist)

        // Collect all file URIs from tracks that belonged to the deleted artist
        val deletedTrackFileUris = artist.trackIds
            .mapNotNull { musicRepository.getTrackById(it)?.fileUri }

        // Efficiently update all playlists that contain any of the deleted tracks
        // by removing those tracks in a single operation per playlist
        playlistStore.playlists.value.forEach { playlist ->
            // Find which of the deleted track URIs are in this playlist
            val urisToRemove = deletedTrackFileUris.filter { uri ->
                playlist.fileUris.contains(uri)
            }

            // Only update the playlist if it contains any of the deleted tracks
            if (urisToRemove.isNotEmpty()) {
                playlistStore.removeTracks(playlist.id, urisToRemove)
            }
        }
    }
}
