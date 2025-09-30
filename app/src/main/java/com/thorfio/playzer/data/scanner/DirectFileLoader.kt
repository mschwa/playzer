package com.thorfio.playzer.data.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.model.Album
import com.thorfio.playzer.data.model.Artist
import com.thorfio.playzer.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID
import androidx.core.net.toUri

/**
 * A utility class to directly load audio files from a folder URI
 * and add them to the repository, bypassing the regular scanner
 */
object DirectFileLoader {
    private const val TAG = "DirectFileLoader"
    private val supportedExtensions = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a")

    /**
     * Directly scans a folder for audio files and adds them to the repository
     * @return The number of files found, or -1 if there was an error
     */
    suspend fun loadFilesFromFolder(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            val prefsRepository = ServiceLocator.appPreferencesRepository
            val musicRepository = ServiceLocator.musicRepository

            val folderUriString = prefsRepository.musicFolderPath.first()
            if (folderUriString.isNullOrEmpty()) {
                Log.w(TAG, "No music folder selected")
                return@withContext -1
            }

            Log.d(TAG, "Starting direct file load from: $folderUriString")
            val uri = folderUriString.toUri()
            val rootFolder = DocumentFile.fromTreeUri(context, uri)

            if (rootFolder == null || !rootFolder.exists() || !rootFolder.isDirectory) {
                Log.e(TAG, "Invalid root folder: $folderUriString")
                return@withContext -1
            }

            val discoveredTracks = mutableListOf<Track>()
            val artistMap = mutableMapOf<String, MutableList<String>>() // artistName -> trackIds
            val albumMap = mutableMapOf<String, AlbumInfo>() // albumName+artistName -> info

            // Scan the directory and its subdirectories for audio files
            scanFolderRecursively(context, rootFolder, discoveredTracks, artistMap, albumMap)

            Log.d(TAG, "Found ${discoveredTracks.size} audio files in folder")

            if (discoveredTracks.isNotEmpty()) {
                // Create artists and albums from the data
                val artists = artistMap.map { (name, trackIds) ->
                    val albumIds = albumMap.values
                        .filter { it.artistName == name }
                        .map { it.id }

                    Artist(
                        id = name.toArtistId(),
                        name = name,
                        trackIds = trackIds,
                        albumIds = albumIds
                    )
                }

                val albums = albumMap.values.map { info ->
                    Album(
                        id = info.id,
                        title = info.title,
                        artistId = info.artistName.toArtistId(),
                        artistName = info.artistName,
                        trackIds = info.trackIds,
                        coverTrackId = info.trackIds.firstOrNull()
                    )
                }

                // Update the repository with the new data
                musicRepository.updateLibrary(discoveredTracks, albums, artists)

                // Force UI update
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "Forcing repository refresh")
                    musicRepository.notifyDataChanged()
                }

                // Update the timestamp in preferences
                prefsRepository.updateLastScanTimestamp(System.currentTimeMillis())

                return@withContext discoveredTracks.size
            } else {
                Log.w(TAG, "No audio files found in folder")
                return@withContext 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading files", e)
            return@withContext -1
        }
    }

    private suspend fun scanFolderRecursively(
        context: Context,
        folder: DocumentFile,
        tracks: MutableList<Track>,
        artistMap: MutableMap<String, MutableList<String>>,
        albumMap: MutableMap<String, AlbumInfo>
    ) {
        try {
            val files = folder.listFiles()
            Log.d(TAG, "Scanning folder: ${folder.name}, found ${files.size} items")

            files.forEach { file ->
                if (file.isDirectory) {
                    // Recursively scan subdirectories
                    scanFolderRecursively(context, file, tracks, artistMap, albumMap)
                } else if (isAudioFile(file)) {
                    // Process the audio file
                    processAudioFile(context, file, tracks, artistMap, albumMap)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory: ${folder.uri}", e)
        }
    }

    private suspend fun processAudioFile(
        context: Context,
        file: DocumentFile,
        tracks: MutableList<Track>,
        artistMap: MutableMap<String, MutableList<String>>,
        albumMap: MutableMap<String, AlbumInfo>
    ) = withContext(Dispatchers.IO) {
        try {
            val fileUri = file.uri
            val retriever = MediaMetadataRetriever()

            try {
                // Use content resolver to open the file
                retriever.setDataSource(context, fileUri)

                // Extract metadata
                val fileName = file.name ?: "Unknown"
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: fileName.substringBeforeLast('.')
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull() ?: 0

                // Create unique IDs
                val artistId = artist.toArtistId()
                val albumKey = "$album:$artist"
                val albumId = albumKey.toAlbumId()
                val trackId = UUID.randomUUID().toString()

                // Create track
                val track = Track(
                    id = trackId,
                    title = title,
                    artistId = artistId,
                    artistName = artist,
                    albumId = albumId,
                    albumTitle = album,
                    durationMs = durationMs,
                    fileUri = fileUri.toString(),
                    trackNumber = trackNumber
                )

                Log.d(TAG, "Added track: $title by $artist from $album")
                tracks.add(track)

                // Add to artist map
                artistMap.getOrPut(artist) { mutableListOf() }.add(trackId)

                // Add to album map
                val albumInfo = albumMap.getOrPut(albumKey) {
                    AlbumInfo(
                        id = albumId,
                        title = album,
                        artistName = artist,
                        trackIds = mutableListOf()
                    )
                }
                albumInfo.trackIds.add(trackId)
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                    // Ignore errors on release
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio file: ${file.uri}", e)
        }
    }

    private fun isAudioFile(file: DocumentFile): Boolean {
        val name = file.name ?: return false
        val mimeType = file.type

        // First check by mime type
        if (mimeType != null && mimeType.startsWith("audio/")) {
            return true
        }

        // Then check by extension
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension in supportedExtensions
    }

    private fun String.toArtistId(): String {
        return "artist_${this.hashCode()}"
    }

    private fun String.toAlbumId(): String {
        return "album_${this.hashCode()}"
    }

    // Helper data class for tracking album information
    private data class AlbumInfo(
        val id: String,
        val title: String,
        val artistName: String,
        val trackIds: MutableList<String>
    )
}
