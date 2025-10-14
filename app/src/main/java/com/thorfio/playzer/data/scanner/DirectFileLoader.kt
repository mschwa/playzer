package com.thorfio.playzer.data.scanner

import android.content.Context
import android.util.Log
import com.thorfio.playzer.core.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Direct file loader using MediaStore API - replaces DocumentFile approach
 */
object DirectFileLoader {
    private const val TAG = "DirectFileLoader"

    /**
     * Scans the music library using MediaStore API
     */
    suspend fun scanMusicFolder(context: Context): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting MediaStore music scan...")

        try {
            val (tracks, albums, artists) = MediaStoreAudioScanner.scanAudioFiles(context)

            if (tracks.isNotEmpty()) {
                ServiceLocator.musicLibrary.updateLibrary(tracks, albums, artists)
                val message = "MediaStore scan completed: ${tracks.size} tracks, ${albums.size} albums, ${artists.size} artists"
                Log.d(TAG, message)
                return@withContext message
            } else {
                val message = "No audio files found in MediaStore"
                Log.w(TAG, message)
                return@withContext message
            }
        } catch (e: Exception) {
            val message = "MediaStore scan failed: ${e.message}"
            Log.e(TAG, message, e)
            return@withContext message
        }
    }

    /**
     * Quick scan that loads cached data first, then scans in background
     */
    suspend fun quickScan(context: Context): String = withContext(Dispatchers.IO) {
        try {
            // Try to load cached data first
            ServiceLocator.musicLibrary.loadFromDisk(context)

            // Then perform MediaStore scan in background
            val result = scanMusicFolder(context)

            return@withContext "Quick scan completed. $result"
        } catch (e: Exception) {
            Log.e(TAG, "Quick scan failed", e)
            return@withContext "Quick scan failed: ${e.message}"
        }
    }
}
