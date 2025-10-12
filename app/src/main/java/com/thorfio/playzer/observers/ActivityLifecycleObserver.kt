package com.thorfio.playzer.observers

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.thorfio.playzer.data.model.LifecycleEventType
import com.thorfio.playzer.services.LifecycleEventLogger
import kotlinx.coroutines.launch

class ActivityLifecycleObserver(
    private val eventLogger: LifecycleEventLogger,
    private val activityName: String
) : DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        owner.lifecycleScope.launch {
            eventLogger.logEvent(LifecycleEventType.ACTIVITY_CREATED, "$activityName created")
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        owner.lifecycleScope.launch {
            eventLogger.logEvent(LifecycleEventType.ACTIVITY_STARTED, "$activityName started")
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        owner.lifecycleScope.launch {
            eventLogger.logEvent(LifecycleEventType.ACTIVITY_RESUMED, "$activityName resumed")
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        owner.lifecycleScope.launch {
            eventLogger.logEvent(LifecycleEventType.ACTIVITY_PAUSED, "$activityName paused")
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        owner.lifecycleScope.launch {
            eventLogger.logEvent(LifecycleEventType.ACTIVITY_STOPPED, "$activityName stopped")
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        owner.lifecycleScope.launch {
            eventLogger.logEvent(LifecycleEventType.ACTIVITY_DESTROYED, "$activityName destroyed")
        }
    }
}
