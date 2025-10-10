package com.thorfio.playzer.data.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.thorfio.playzer.data.model.Album
import com.thorfio.playzer.data.model.Artist
import com.thorfio.playzer.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import androidx.core.net.toUri

/**
 * Utility class for common audio file operations used across scanning components
 */
object AudioFileUtils {
    private const val TAG = "AudioFileUtils"

    // Supported audio file extensions
    val supportedExtensions = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a")

    /**
     * Helper data class for tracking album information during scanning
     */
    data class AlbumInfo(
        val id: String,
        val title: String,
        val artistName: String,
        val trackIds: MutableList<String>
    )

    /**
     * Determines if a file is an audio file based on MIME type or extension
     * @param file The DocumentFile to check
     * @return true if the file is an audio file, false otherwise
     */
    fun isAudioFile(file: DocumentFile): Boolean {
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

    /**
     * Processes an audio file to extract metadata and create Track information
     * @return The created Track object or null if processing failed
     */
    suspend fun processAudioFile(
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

                return@withContext track
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    // Ignore errors on release
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio file: ${file.uri}", e)
            return@withContext null
        }
    }

    /**
     * Recursively scans a directory for audio files
     */
    suspend fun scanDirectory(
        context: Context,
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
                    scanDirectory(context, file, tracks, artistMap, albumMap)
                } else if (isAudioFile(file)) {
                    // Process audio file
                    Log.d(TAG, "Found audio file: ${file.name}")
                    processAudioFile(context, file, tracks, artistMap, albumMap)
                } else {
                    Log.d(TAG, "Skipping non-audio file: ${file.name}, type: ${file.type}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory: ${directory.uri}", e)
            e.printStackTrace()
        }
    }

    /**
     * Creates artists from the collected artist data
     */
    fun createArtists(artistMap: Map<String, MutableList<String>>, albumMap: Map<String, AlbumInfo>): List<Artist> {
        return artistMap.map { (name, trackIds) ->
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
    }

    /**
     * Creates albums from the collected album data
     */
    fun createAlbums(albumMap: Map<String, AlbumInfo>): List<Album> {
        return albumMap.values.map { info ->
            Album(
                id = info.id,
                title = info.title,
                artistId = info.artistName.toArtistId(),
                artistName = info.artistName,
                trackIds = info.trackIds,
                coverTrackId = info.trackIds.firstOrNull()
            )
        }
    }

    /**
     * Deletes an audio file from the file system
     * @param context The application context
     * @param fileUri The URI of the file to delete
     * @return true if the file was successfully deleted, false otherwise
     */
    suspend fun deleteAudioFile(context: Context, fileUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to delete audio file: $fileUri")

            val uri = fileUri.toUri()
            val file = DocumentFile.fromSingleUri(context, uri)

            if (file == null) {
                Log.e(TAG, "Failed to access file: DocumentFile is null for URI $fileUri")
                return@withContext false
            }

            if (!file.exists()) {
                Log.e(TAG, "File does not exist: $fileUri")
                return@withContext false
            }

            val fileName = file.name
            val result = file.delete()

            if (result) {
                Log.d(TAG, "Successfully deleted audio file: $fileName")
            } else {
                Log.e(TAG, "Failed to delete audio file: $fileName")
            }

            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting audio file: $fileUri", e)
            return@withContext false
        }
    }

    /**
     * Extension function to generate artist IDs
     */
    fun String.toArtistId(): String {
        return "artist_${this.hashCode()}"
    }

    /**
     * Extension function to generate album IDs
     */
    fun String.toAlbumId(): String {
        return "album_${this.hashCode()}"
    }
}
