package com.thorfio.playzer.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LifecycleEvent(
    val id: String,
    val eventType: LifecycleEventType,
    val timestamp: Long,
    val details: String? = null
)

@Serializable
enum class LifecycleEventType {
    APP_CREATED,
    APP_STARTED,
    APP_RESUMED,
    APP_PAUSED,
    APP_STOPPED,
    APP_DESTROYED,
    ACTIVITY_CREATED,
    ACTIVITY_STARTED,
    ACTIVITY_RESUMED,
    ACTIVITY_PAUSED,
    ACTIVITY_STOPPED,
    ACTIVITY_DESTROYED
}
