package com.thorfio.playzer.services

import android.content.Context
import android.util.Log
import com.thorfio.playzer.data.model.LifecycleEvent
import com.thorfio.playzer.data.model.LifecycleEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LifecycleEventLogger private constructor(private val context: Context) {

    companion object {
        private const val TAG = "LifecycleEventLogger"
        private const val EVENTS_FILE_NAME = "lifecycle_events.json"
        private const val MAX_EVENTS = 1000 // Limit stored events to prevent file from growing too large

        @Volatile
        private var INSTANCE: LifecycleEventLogger? = null

        fun getInstance(context: Context): LifecycleEventLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LifecycleEventLogger(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val eventsFile = File(context.filesDir, EVENTS_FILE_NAME)
    private val mutex = Mutex()

    private val _events = MutableStateFlow<List<LifecycleEvent>>(emptyList())
    val events: StateFlow<List<LifecycleEvent>> = _events.asStateFlow()

    init {
        loadEventsFromDisk()
    }

    suspend fun logEvent(eventType: LifecycleEventType, details: String? = null) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val event = LifecycleEvent(
                    id = UUID.randomUUID().toString(),
                    eventType = eventType,
                    timestamp = System.currentTimeMillis(),
                    details = details
                )

                val currentEvents = _events.value.toMutableList()
                currentEvents.add(0, event) // Add to beginning for chronological order

                // Keep only the most recent events
                if (currentEvents.size > MAX_EVENTS) {
                    val trimmedEvents = currentEvents.take(MAX_EVENTS)
                    _events.value = trimmedEvents
                } else {
                    _events.value = currentEvents
                }

                Log.d(TAG, "Logged event: ${eventType.name} at ${formatTimestamp(event.timestamp)}")

                // Save to disk
                saveEventsToDisk()
            }
        }
    }

    private fun loadEventsFromDisk() {
        try {
            if (eventsFile.exists()) {
                val jsonString = eventsFile.readText()
                if (jsonString.isNotBlank()) {
                    val loadedEvents = json.decodeFromString<List<LifecycleEvent>>(jsonString)
                    _events.value = loadedEvents.sortedByDescending { it.timestamp }
                    Log.d(TAG, "Loaded ${loadedEvents.size} events from disk")
                }
            } else {
                Log.d(TAG, "No existing events file found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading events from disk", e)
        }
    }

    private fun saveEventsToDisk() {
        try {
            val jsonString = json.encodeToString(_events.value)
            eventsFile.writeText(jsonString)
            Log.d(TAG, "Saved ${_events.value.size} events to disk")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving events to disk", e)
        }
    }

    suspend fun clearAllEvents() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                _events.value = emptyList()
                if (eventsFile.exists()) {
                    eventsFile.delete()
                }
                Log.d(TAG, "Cleared all events")
            }
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm:ss.SSS", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    fun getEventCount(): Int = _events.value.size

    fun getEventsByType(type: LifecycleEventType): List<LifecycleEvent> {
        return _events.value.filter { it.eventType == type }
    }
}
