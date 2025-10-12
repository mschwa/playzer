package com.thorfio.playzer.data.scanner

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.model.Track
import com.thorfio.playzer.data.persistence.AppPreferencesRepository
import com.thorfio.playzer.data.persistence.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Used to scan the file system for audio files
 */
class AudioFileScanner(
    private val context: Context,
    private val musicRepositorMe: MusicRepository,
    private val preferencesRepository: AppPreferencesRepository
) {
    private val scannedFiles = mutableMapOf<String, String>() // uri -> trackId
    private val TAG = "AudioFileScanner"

    suspend fun scanMusicFolder(): Int = withContext(Dispatchers.IO) {

        val prefsRepository = preferencesRepository
        val musicRepository = musicRepositorMe

        val musicFolderUri = preferencesRepository.musicFolderPath.first()

        if (musicFolderUri.isNullOrEmpty()) {
            Log.w(TAG, "No music folder selected")
            return@withContext -1
        }

        try
        {
            val uri = musicFolderUri.toUri()
            val rootFolder = DocumentFile.fromTreeUri(context, uri)

            if (rootFolder == null || !rootFolder.exists() || !rootFolder.isDirectory) {
                Log.e(TAG, "Invalid root folder: $musicFolderUri")
                return@withContext -1
            }

            val prevScannedFiles = scannedFiles.toMap() // Copy of current map for comparison
            scannedFiles.clear()

            Log.d(TAG, "Starting scan of music folder: $musicFolderUri")

            val discoveredTracks = mutableListOf<Track>()
            val artistMap = mutableMapOf<String, MutableList<String>>() // artistName -> trackIds
            val albumMap = mutableMapOf<String, AudioFileUtils.AlbumInfo>() // albumName+artistName -> info

            // Scan the directory and its subdirectories for audio files
            AudioFileUtils.scanDirectory(context, rootFolder, discoveredTracks, artistMap, albumMap)

            Log.d(TAG, "Scan completed. Found ${discoveredTracks.size} tracks")

            // Find and remove tracks that no longer exist
            val currentPaths = scannedFiles.keys
            val removedFilePaths = prevScannedFiles.keys.filter { it !in currentPaths }
            val removedTrackIds = removedFilePaths.mapNotNull { prevScannedFiles[it] }

            if (removedTrackIds.isNotEmpty()) {
                Log.d(TAG, "Removing ${removedTrackIds.size} tracks that no longer exist")
                musicRepositorMe.deleteTracks(removedTrackIds)
            }

            if (discoveredTracks.isNotEmpty()) {
                // Create artists and albums from collected data
                val artists = AudioFileUtils.createArtists(artistMap, albumMap)
                val albums = AudioFileUtils.createAlbums(albumMap)

                // Log the data we're about to update to the repository
                Log.d(TAG, "Updating repository with: ${discoveredTracks.size} tracks, ${albums.size} albums, ${artists.size} artists")

                // Update repository with new data
                musicRepositorMe.updateLibrary(discoveredTracks, albums, artists)

                // Update timestamp of last scan
                prefsRepository.updateLastScanTimestamp(Instant.now().toEpochMilli())

                return@withContext discoveredTracks.size
            }
            else {
                Log.w(TAG, "No audio files found in folder")
                return@withContext 0
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error scanning music folder", e)
            e.printStackTrace()
            return@withContext -1
        }
    }
}