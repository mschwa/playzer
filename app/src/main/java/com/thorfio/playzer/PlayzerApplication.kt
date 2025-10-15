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
        // Automatically scan for audio files at startup using MediaStore
        // Use ProcessLifecycleOwner to get a lifecycle-aware coroutine scope
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            try {
                val result = AudioFileScanner.scanAndUpdateLibrary(applicationContext)
                Log.d("PlayzerApp", "Audio scan result: ${result.message}")
            } catch (e: Exception) {
                Log.e("PlayzerApp", "Error running audio scan", e)
            }
        }
    }
}
