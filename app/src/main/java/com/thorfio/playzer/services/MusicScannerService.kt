package com.thorfio.playzer.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.scanner.AudioFileScanner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Background service that handles scanning the file system for audio files
 */
class MusicScannerService : Service() {
    companion object {
        private const val TAG = "MusicScannerService"
        private const val ACTION_SCAN = "com.thorfio.playzer.action.SCAN_MUSIC"
        private const val ACTION_CANCEL = "com.thorfio.playzer.action.CANCEL_SCAN"

        // Time threshold to avoid repeated scans (15 minutes)
        private const val SCAN_THRESHOLD_MS = 15 * 60 * 1000L

        /**
         * Starts the music scanner service if conditions are met
         */
        suspend fun startScanIfNeeded(context: Context) {
            // Get dependencies
            val preferencesRepository = ServiceLocator.appPreferencesRepository

            // Check if music folder is set
            val musicFolder = preferencesRepository.musicFolderPath.first()
            if (musicFolder.isNullOrEmpty()) {
                Log.w(TAG, "No music folder set, skipping scan")
                return
            }

            Log.d(TAG, "Music folder is set to: $musicFolder")

            // Check last scan time to avoid too frequent scans
            val lastScan = preferencesRepository.lastScanTimestamp.first()
            val now = Instant.now().toEpochMilli()

            if (now - lastScan < SCAN_THRESHOLD_MS) {
                // Skip if we scanned recently
                Log.d(TAG, "Skipping scan, last scan was too recent: ${now - lastScan}ms ago")
                return
            }

            // Start the service
            Log.i(TAG, "Starting music scanner service")
            val intent = Intent(context, MusicScannerService::class.java).apply {
                action = ACTION_SCAN
            }
            context.startService(intent)
        }

        /**
         * Forces a scan regardless of the time threshold
         */
        fun startScanNow(context: Context) {
            Log.i(TAG, "Forcing immediate music scan")
            val intent = Intent(context, MusicScannerService::class.java).apply {
                action = ACTION_SCAN
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isScanning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SCAN -> startScan()
            ACTION_CANCEL -> cancelScan()
        }
        return START_NOT_STICKY
    }

    private fun startScan() {
        if (isScanning) {
            Log.d(TAG, "Scan already in progress, ignoring duplicate request")
            return
        }

        isScanning = true
        Log.i(TAG, "Starting music scan")

        serviceScope.launch {
            try {
                val scanner = AudioFileScanner(
                    context = applicationContext,
                    musicRepositorMe = ServiceLocator.musicRepository,
                    preferencesRepository = ServiceLocator.appPreferencesRepository
                )

                val filesScanned = scanner.scanMusicFolder()

                if (filesScanned > 0) {
                    Log.i(TAG, "Scan complete, found $filesScanned files")
                    // Force the UI to refresh on the main thread
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "Forcing repository data refresh")
                        ServiceLocator.musicRepository.notifyDataChanged()

                        // Additional delay to ensure UI updates
                        delay(300)

                        // Notify again to ensure updates are applied
                        ServiceLocator.musicRepository.notifyDataChanged()
                    }
                } else if (filesScanned == 0) {
                    Log.w(TAG, "Scan complete, but no audio files were found")
                } else {
                    Log.e(TAG, "Scan failed with result code: $filesScanned")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during music scan", e)
                e.printStackTrace()
            } finally {
                isScanning = false
                stopSelf()
            }
        }
    }

    private fun cancelScan() {
        if (isScanning) {
            Log.i(TAG, "Cancelling ongoing music scan")
            serviceScope.coroutineContext.cancelChildren()
            isScanning = false
        }
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (isScanning) {
            Log.w(TAG, "Service destroyed while scan was in progress")
        }
        serviceScope.cancel()
    }
}
