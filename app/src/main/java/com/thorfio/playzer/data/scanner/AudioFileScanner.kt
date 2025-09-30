package com.thorfio.playzer.data.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.thorfio.playzer.data.model.Album
import com.thorfio.playzer.data.model.Artist
import com.thorfio.playzer.data.model.Track
import com.thorfio.playzer.data.prefs.AppPreferencesRepository
import com.thorfio.playzer.data.repo.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

/**
 * Handles scanning of the file system for audio files
 */
class AudioFileScanner(
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val preferencesRepository: AppPreferencesRepository
) {
    companion object {
        private const val TAG = "AudioFileScanner"

        // Static function to help debugging
        suspend fun testScanDirectory(context: Context): String {
            val prefsRepo = com.thorfio.playzer.core.ServiceLocator.appPreferencesRepository
            val musicFolderUri = prefsRepo.musicFolderPath.first()

            if (musicFolderUri == null) {
                return "No music folder selected"
            }

            val uri = Uri.parse(musicFolderUri)
            val stringBuilder = StringBuilder()
            stringBuilder.append("Testing scan of: $musicFolderUri\n")

            try {
                val rootDir = DocumentFile.fromTreeUri(context, uri)
                if (rootDir == null) {
                    return "Failed to access root directory"
                }

                if (!rootDir.exists()) {
                    return "Root directory does not exist"
                }

                if (!rootDir.isDirectory) {
                    return "Not a directory"
                }

                val files = rootDir.listFiles()
                stringBuilder.append("Found ${files.size} items in root directory:\n")

                var audioCount = 0

                files.forEach { file ->
                    val name = file.name ?: "unknown"
                    val type = file.type ?: "unknown type"
                    val isDir = if (file.isDirectory) "DIR" else "FILE"
                    stringBuilder.append("- $name ($isDir) [$type]\n")

                    // Count audio files
                    if (type.startsWith("audio/") ||
                        (name.contains('.') &&
                         setOf("mp3", "wav", "ogg", "flac", "aac", "m4a").contains(
                            name.substringAfterLast('.', "").lowercase()))) {
                        audioCount++
                    }
                }

                stringBuilder.append("\nTotal audio files found: $audioCount")

                Log.d("AudioFileTester", stringBuilder.toString())
                return stringBuilder.toString()

            } catch (e: Exception) {
                stringBuilder.append("Error: ${e.message}")
                Log.e("AudioFileTester", "Error scanning", e)
                return stringBuilder.toString()
            }
        }
    }

    // Supported audio file extensions
    private val supportedExtensions = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a")

    // Map to track scanned files for diff checking
    private val scannedFiles = mutableMapOf<String, String>() // uri -> trackId

    /**
     * Initiates a scan of the music folder if it's configured
     * @return Number of files processed or -1 if scanning couldn't be performed
     */
    suspend fun scanMusicFolder(): Int = withContext(Dispatchers.IO) {
        val musicFolderUri = preferencesRepository.musicFolderPath.first() ?: return@withContext -1
        Log.d(TAG, "Starting scan of music folder: $musicFolderUri")

        try {
            val uri = Uri.parse(musicFolderUri)
            val rootDir = DocumentFile.fromTreeUri(context, uri)

            if (rootDir == null) {
                Log.e(TAG, "Failed to access root directory from URI: $uri")
                return@withContext -1
            }

            if (!rootDir.exists()) {
                Log.e(TAG, "Root directory does not exist: ${rootDir.uri}")
                return@withContext -1
            }

            if (!rootDir.isDirectory) {
                Log.e(TAG, "Root path is not a directory: ${rootDir.uri}")
                return@withContext -1
            }

            Log.d(TAG, "Root directory access confirmed: ${rootDir.uri}")
            val prevScannedFiles = scannedFiles.toMap() // Copy of current map for comparison
            scannedFiles.clear()

            val discoveredTracks = mutableListOf<Track>()
            val artistMap = mutableMapOf<String, MutableList<String>>() // artistName -> trackIds
            val albumMap = mutableMapOf<String, AlbumInfo>() // albumName+artistName -> info

            // Recursively scan for audio files
            scanDirectory(rootDir, discoveredTracks, artistMap, albumMap)
            Log.d(TAG, "Scan completed. Found ${discoveredTracks.size} tracks")

            // Find and remove tracks that no longer exist
            val currentPaths = scannedFiles.keys
            val removedFilePaths = prevScannedFiles.keys.filter { it !in currentPaths }
            val removedTrackIds = removedFilePaths.mapNotNull { prevScannedFiles[it] }

            if (removedTrackIds.isNotEmpty()) {
                Log.d(TAG, "Removing ${removedTrackIds.size} tracks that no longer exist")
                musicRepository.deleteTracks(removedTrackIds)
            }

            // Create artists and albums from collected data
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

            // Log the data we're about to update to the repository
            Log.d(TAG, "Updating repository with: ${discoveredTracks.size} tracks, ${albums.size} albums, ${artists.size} artists")

            // Update repository with new data
            musicRepository.updateLibrary(discoveredTracks, albums, artists)
            Log.d(TAG, "Repository updated successfully")

            // Update timestamp of last scan
            preferencesRepository.updateLastScanTimestamp(Instant.now().toEpochMilli())

            return@withContext discoveredTracks.size
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning music folder", e)
            e.printStackTrace()
            return@withContext -1
        }
    }

    private suspend fun scanDirectory(
        directory: DocumentFile,
        tracks: MutableList<Track>,
        artistMap: MutableMap<String, MutableList<String>>,
        albumMap: MutableMap<String, AlbumInfo>
    ) {
        try {
            val files = directory.listFiles()
            Log.d(TAG, "Scanning directory: ${directory.uri}, found ${files.size} items")

            files.forEach { file ->
                if (file.isDirectory) {
                    // Recursively scan subdirectories
                    Log.d(TAG, "Found subdirectory: ${file.name}")
                    scanDirectory(file, tracks, artistMap, albumMap)
                } else if (isAudioFile(file)) {
                    // Process audio file
                    Log.d(TAG, "Found audio file: ${file.name}")
                    processAudioFile(file, tracks, artistMap, albumMap)
                } else {
                    Log.d(TAG, "Skipping non-audio file: ${file.name}, type: ${file.type}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory: ${directory.uri}", e)
            e.printStackTrace()
        }
    }

    private suspend fun processAudioFile(
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
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: file.name?.substringBeforeLast('.') ?: "Unknown Title"
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull() ?: 0

                retriever.release()

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

                Log.d(TAG, "Processed track: $title by $artist from album $album")
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

                // Add to scanned files map for tracking
                scannedFiles[fileUri.toString()] = trackId
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    // Ignore errors on release
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio file: ${file.uri}", e)
            e.printStackTrace()
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
        val isSupported = extension in supportedExtensions

        if (isSupported) {
            Log.d(TAG, "Identified audio file by extension: $name")
        }

        return isSupported
    }

    private fun String.toArtistId(): String {
        return "artist_${this.hashCode()}"
    }

    private fun String.toAlbumId(): String {
        return "album_${this.hashCode()}"
    }

    // Helper data class for tracking album information during scanning
    private data class AlbumInfo(
        val id: String,
        val title: String,
        val artistName: String,
        val trackIds: MutableList<String>
    )
}
