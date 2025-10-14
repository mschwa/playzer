package com.thorfio.playzer

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.observers.AppLifecycleObserver
import com.thorfio.playzer.services.LifecycleEventLogger
import com.thorfio.playzer.services.MusicScannerService
import kotlinx.coroutines.launch

class PlayzerApplication : Application() {

    private val lifecycleOwner = ProcessLifecycleOwner.get()

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)

        // Initialize lifecycle event logging
        initLifecycleLogging()

        Log.d("PlayzerApp", "PlayzerApplication initialized")

        // Initialize automatic music scanning
        initMusicScanning()
    }

    private fun initLifecycleLogging() {
        val eventLogger = LifecycleEventLogger.getInstance(this)
        AppLifecycleObserver.register(eventLogger, this)
    }

    fun initMusicScanning() {
        // Use ProcessLifecycleOwner to get a lifecycle-aware coroutine scope
        // This ensures we don't run scanning in the background when app is not in use
        lifecycleOwner.lifecycleScope.launch {
            try {
                // Start the scanner service if needed
                MusicScannerService.startScanIfNeeded(applicationContext)
            } catch (e: Exception) {
                Log.e("PlayzerApp", "Error starting music scanner", e)
            }
        }

        // Automatically scan for audio files at startup using DirectFileLoader
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            try {
                val result = com.thorfio.playzer.data.scanner.DirectFileLoader.scanMusicFolder(applicationContext)
                Log.d("PlayzerApp", "DirectFileLoader scan result: $result")
            } catch (e: Exception) {
                Log.e("PlayzerApp", "Error running DirectFileLoader scan", e)
            }
        }
    }
}
