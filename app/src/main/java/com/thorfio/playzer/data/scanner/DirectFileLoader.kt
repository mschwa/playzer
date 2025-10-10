package com.thorfio.playzer.data.scanner

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.thorfio.playzer.core.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Used to scan the file system for audio files
 */
object DirectFileLoader {
    private const val TAG = "DirectFileLoader"

    suspend fun scanMusicFolder(context: Context): Int = withContext(Dispatchers.IO) {

        val prefsRepository = ServiceLocator.appPreferencesRepository
        val musicRepository = ServiceLocator.musicRepository

        val musicFolderUri = prefsRepository.musicFolderPath.first()

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

            Log.d(TAG, "Starting scan of music folder: $musicFolderUri")

            val discoveredTracks = mutableListOf<com.thorfio.playzer.data.model.Track>()
            val artistMap = mutableMapOf<String, MutableList<String>>() // artistName -> trackIds
            val albumMap = mutableMapOf<String, AudioFileUtils.AlbumInfo>() // albumName+artistName -> info

            // Scan the directory and its subdirectories for audio files
            AudioFileUtils.scanDirectory(context, rootFolder, discoveredTracks, artistMap, albumMap)

            Log.d(TAG, "Scan completed. Found ${discoveredTracks.size} tracks")

            if (discoveredTracks.isNotEmpty()) {
                // Create artists and albums from the data
                val artists = AudioFileUtils.createArtists(artistMap, albumMap)
                val albums = AudioFileUtils.createAlbums(albumMap)

                // Log the data we're about to update to the repository
                Log.d(TAG, "Updating repository with: ${discoveredTracks.size} tracks, ${albums.size} albums, ${artists.size} artists")

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
