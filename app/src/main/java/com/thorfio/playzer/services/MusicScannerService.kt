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
 * Background service that handles scanning audio files using MediaStore API
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
            val preferencesRepository = ServiceLocator.appPreferencesRepository

            // Check last scan time to avoid too frequent scans
            val lastScan = preferencesRepository.lastScanTimestamp.first()
            val now = Instant.now().toEpochMilli()

            if (now - lastScan < SCAN_THRESHOLD_MS) {
                // Skip if we scanned recently
                Log.d(TAG, "Skipping scan, last scan was too recent: ${now - lastScan}ms ago")
                return
            }

            Log.d(TAG, "Starting MediaStore-based music scan...")

            // Start the service with scan action
            val intent = Intent(context, MusicScannerService::class.java).apply {
                action = ACTION_SCAN
            }
            context.startService(intent)
        }

        /**
         * Cancels any running scan
         */
        fun cancelScan(context: Context) {
            val intent = Intent(context, MusicScannerService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }

    private var scanJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SCAN -> startScan()
            ACTION_CANCEL -> cancelScan()
            else -> {
                Log.w(TAG, "Unknown action: ${intent?.action}")
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startScan() {
        if (scanJob?.isActive == true) {
            Log.d(TAG, "Scan already in progress")
            return
        }

        scanJob = serviceScope.launch {
            try {
                Log.d(TAG, "Starting MediaStore scan...")

                val result = AudioFileScanner.scanAndUpdateLibrary(applicationContext)

                if (result.success) {
                    Log.d(TAG, "Scan completed successfully: ${result.message}")

                    // Update last scan timestamp
                    val preferencesRepository = ServiceLocator.appPreferencesRepository
                    preferencesRepository.updateLastScanTimestamp(Instant.now().toEpochMilli())
                } else {
                    Log.e(TAG, "Scan failed: ${result.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during scan", e)
            } finally {
                stopSelf()
            }
        }
    }

    private fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        Log.d(TAG, "Scan cancelled")
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        scanJob?.cancel()
        serviceScope.cancel()
        Log.d(TAG, "MusicScannerService destroyed")
    }
}
