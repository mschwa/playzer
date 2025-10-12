package com.thorfio.playzer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thorfio.playzer.data.model.LifecycleEvent
import com.thorfio.playzer.data.model.LifecycleEventType
import com.thorfio.playzer.services.LifecycleEventLogger
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventLogScreen(
    eventLogger: LifecycleEventLogger,
    onNavigateBack: () -> Unit
) {
    val events by eventLogger.events.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header with back button and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Event Log",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Clear events button
                IconButton(
                    onClick = {
                        scope.launch {
                            eventLogger.clearAllEvents()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Events",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Event count summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Total Events: ${events.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (events.isNotEmpty()) {
                        Text(
                            text = "Latest: ${eventLogger.formatTimestamp(events.first().timestamp)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Events list
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No events logged yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(events) { event ->
                        EventLogItem(
                            event = event,
                            eventLogger = eventLogger
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventLogItem(
    event: LifecycleEvent,
    eventLogger: LifecycleEventLogger
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getEventTypeColor(event.eventType)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = formatEventTypeName(event.eventType),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = eventLogger.formatTimestamp(event.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (event.details != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun getEventTypeColor(eventType: LifecycleEventType): Color {
    return when (eventType) {
        LifecycleEventType.APP_CREATED, LifecycleEventType.ACTIVITY_CREATED ->
            MaterialTheme.colorScheme.primaryContainer
        LifecycleEventType.APP_STARTED, LifecycleEventType.ACTIVITY_STARTED ->
            MaterialTheme.colorScheme.secondaryContainer
        LifecycleEventType.APP_RESUMED, LifecycleEventType.ACTIVITY_RESUMED ->
            MaterialTheme.colorScheme.tertiaryContainer
        LifecycleEventType.APP_PAUSED, LifecycleEventType.ACTIVITY_PAUSED ->
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        LifecycleEventType.APP_STOPPED, LifecycleEventType.ACTIVITY_STOPPED ->
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        LifecycleEventType.APP_DESTROYED, LifecycleEventType.ACTIVITY_DESTROYED ->
            MaterialTheme.colorScheme.errorContainer
    }
}

private fun formatEventTypeName(eventType: LifecycleEventType): String {
    return when (eventType) {
        LifecycleEventType.APP_CREATED -> "App Created"
        LifecycleEventType.APP_STARTED -> "App Started"
        LifecycleEventType.APP_RESUMED -> "App Resumed"
        LifecycleEventType.APP_PAUSED -> "App Paused"
        LifecycleEventType.APP_STOPPED -> "App Stopped"
        LifecycleEventType.APP_DESTROYED -> "App Destroyed"
        LifecycleEventType.ACTIVITY_CREATED -> "Activity Created"
        LifecycleEventType.ACTIVITY_STARTED -> "Activity Started"
        LifecycleEventType.ACTIVITY_RESUMED -> "Activity Resumed"
        LifecycleEventType.ACTIVITY_PAUSED -> "Activity Paused"
        LifecycleEventType.ACTIVITY_STOPPED -> "Activity Stopped"
        LifecycleEventType.ACTIVITY_DESTROYED -> "Activity Destroyed"
    }
}
