package com.thorfio.playzer

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.observers.AppLifecycleObserver
import com.thorfio.playzer.services.LifecycleEventLogger
import com.thorfio.playzer.data.scanner.AudioFileScanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class PlayzerApplication : Application() {

    companion object {
        private const val TAG = "PlayzerApp"
        // Scan staleness threshold: 6 hours in milliseconds
        private const val SCAN_STALENESS_THRESHOLD = 6 * 60 * 60 * 1000L
    }

    private val scanMutex = Mutex()

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)

        // Initialize lifecycle event logging
        initLifecycleLogging()

        Log.d(TAG, "PlayzerApplication initialized")

        // Load cache first, then conditionally scan
        preloadLibraryAndMaybeScan()
    }

    private fun initLifecycleLogging() {
        val eventLogger = LifecycleEventLogger.getInstance(this)
        AppLifecycleObserver.register(eventLogger, this)
    }

    /**
     * Implements intelligent caching strategy:
     * 1. Load cached library immediately for fast startup
     * 2. Check if scan is needed (empty library or stale data)
     * 3. Only scan if necessary to minimize startup time
     */
    private fun preloadLibraryAndMaybeScan() {
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            val musicLibrary = ServiceLocator.musicLibrary
            val appPrefs = ServiceLocator.appPreferencesRepository

            // Step 1: Load cached data first (fast path)
            try {
                Log.d(TAG, "Loading cached music library...")
                musicLibrary.loadFromDisk(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cached library", e)
            }

            // Step 2: Determine if scan is needed
            val lastScanTimestamp = appPrefs.getLastScanTimestamp()
            val timeSinceLastScan = System.currentTimeMillis() - lastScanTimestamp
            val isStale = timeSinceLastScan > SCAN_STALENESS_THRESHOLD
            val isEmpty = musicLibrary.isEmpty()

            val scanReason = when {
                isEmpty && lastScanTimestamp == 0L -> "INITIAL" // First run
                isEmpty -> "EMPTY" // Cache was cleared or failed to load
                isStale -> "STALE" // Data is older than threshold
                else -> null // No scan needed
            }

            // Step 3: Conditionally run scan
            if (scanReason != null) {
                Log.d(TAG, "Scan required: $scanReason (last scan: ${timeSinceLastScan / 1000 / 60} minutes ago)")
                runAudioScan(scanReason)
            } else {
                Log.d(TAG, "Using cached library (${musicLibrary.tracks.value.size} tracks, last scan: ${timeSinceLastScan / 1000 / 60} minutes ago)")
            }
        }
    }

    /**
     * Runs audio scan with mutex protection to prevent duplicate scans
     */
    private fun runAudioScan(reason: String) {
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            // Try to acquire lock, skip if another scan is running
            if (!scanMutex.tryLock()) {
                Log.d(TAG, "Scan already in progress, skipping...")
                return@launch
            }

            try {
                Log.d(TAG, "Starting audio scan ($reason)...")
                val result = AudioFileScanner.scanAndUpdateLibrary(applicationContext)

                if (result.success) {
                    // Update last scan timestamp on successful scan
                    ServiceLocator.appPreferencesRepository.updateLastScanTimestampToNow()
                    Log.d(TAG, "Audio scan completed: ${result.message}")
                } else {
                    Log.w(TAG, "Audio scan failed: ${result.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error running audio scan ($reason)", e)
            } finally {
                scanMutex.unlock()
            }
        }
    }

    /**
     * Exposed method for manual refresh (e.g., pull-to-refresh in UI)
     */
    fun requestManualScan() {
        Log.d(TAG, "Manual scan requested")
        runAudioScan("MANUAL")
    }
}
