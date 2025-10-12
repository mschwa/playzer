package com.thorfio.playzer.observers

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.thorfio.playzer.data.model.LifecycleEventType
import com.thorfio.playzer.services.LifecycleEventLogger
import kotlinx.coroutines.launch

class AppLifecycleObserver(private val eventLogger: LifecycleEventLogger) : DefaultLifecycleObserver {

    companion object {
        fun register(eventLogger: LifecycleEventLogger) {
            val observer = AppLifecycleObserver(eventLogger)
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
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            eventLogger.logEvent(LifecycleEventType.APP_DESTROYED, "Application process destroyed")
        }
    }
}
