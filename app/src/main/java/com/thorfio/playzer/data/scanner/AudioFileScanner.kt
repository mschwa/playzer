package com.thorfio.playzer.data.scanner

import android.content.Context
import android.util.Log
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.model.Album
import com.thorfio.playzer.data.model.Artist
import com.thorfio.playzer.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans audio files using MediaStore API and updates the music repository
 */
object AudioFileScanner {
    private const val TAG = "AudioFileScanner"

    /**
     * Scans for audio files using MediaStore and updates the repository
     */
    suspend fun scanAndUpdateLibrary(context: Context): ScanResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting MediaStore audio scan...")

        try {
            // Use MediaStore to scan for audio files
            val (tracks, albums, artists) = MediaStoreAudioScanner.scanAudioFiles(context)

            if (tracks.isNotEmpty()) {
                // Update the music repository
                ServiceLocator.musicLibrary.updateLibrary(tracks, albums, artists)
                Log.d(TAG, "Library updated with ${tracks.size} tracks, ${albums.size} albums, ${artists.size} artists")

                return@withContext ScanResult(
                    success = true,
                    tracksFound = tracks.size,
                    albumsFound = albums.size,
                    artistsFound = artists.size,
                    message = "Successfully scanned ${tracks.size} audio files"
                )
            } else {
                Log.w(TAG, "No audio files found in MediaStore")
                return@withContext ScanResult(
                    success = true,
                    tracksFound = 0,
                    albumsFound = 0,
                    artistsFound = 0,
                    message = "No audio files found"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during MediaStore scan", e)
            return@withContext ScanResult(
                success = false,
                tracksFound = 0,
                albumsFound = 0,
                artistsFound = 0,
                message = "Scan failed: ${e.message}"
            )
        }
    }

    /**
     * Result of a scan operation
     */
    data class ScanResult(
        val success: Boolean,
        val tracksFound: Int,
        val albumsFound: Int,
        val artistsFound: Int,
        val message: String
    )
}