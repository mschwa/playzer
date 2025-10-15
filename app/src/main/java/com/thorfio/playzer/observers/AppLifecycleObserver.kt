package com.thorfio.playzer.observers

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.model.LifecycleEventType
import com.thorfio.playzer.services.LifecycleEventLogger
import kotlinx.coroutines.launch

class AppLifecycleObserver(
    private val eventLogger: LifecycleEventLogger,
    private val context: Context
) : DefaultLifecycleObserver {

    companion object {
        fun register(eventLogger: LifecycleEventLogger, context: Context) {
            val observer = AppLifecycleObserver(eventLogger, context)
            ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            eventLogger.logEvent(LifecycleEventType.APP_CREATED, "Application process created")
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            eventLogger.logEvent(LifecycleEventType.APP_STARTED, "Application moved to foreground")

            // Start observing MediaStore for changes
            ServiceLocator.musicLibrary.startObservingMediaStore(context)
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            eventLogger.logEvent(LifecycleEventType.APP_RESUMED, "Application resumed")
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            eventLogger.logEvent(LifecycleEventType.APP_PAUSED, "Application paused")
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            eventLogger.logEvent(LifecycleEventType.APP_STOPPED, "Application moved to background")

            // Stop observing MediaStore
            ServiceLocator.musicLibrary.stopObservingMediaStore()

            // Save MusicRepository data to disk when app goes to background
            try {
                ServiceLocator.musicLibrary.saveToDisk(context)
            } catch (e: Exception) {
                android.util.Log.e("AppLifecycleObserver", "Failed to save music library to disk", e)
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            eventLogger.logEvent(LifecycleEventType.APP_DESTROYED, "Application process destroyed")
        }
    }
}
