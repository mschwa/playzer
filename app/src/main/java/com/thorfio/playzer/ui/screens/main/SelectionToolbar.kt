package com.thorfio.playzer.ui.screens.main

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.ui.navigation.RouteBuilder
import com.thorfio.playzer.ui.navigation.Routes

@Composable
fun SelectionToolbar(
    selectedIds: List<String>,
    totalTracks: Int,
    onClearSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    nav: NavController
) {
    Surface(tonalElevation = 4.dp, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
            }

            IconButton(onClick = onToggleSelectAll) {
                if (selectedIds.size == totalTracks) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear All")
                } else {
                    Icon(Icons.Filled.DoneAll, contentDescription = "Select All")
                }
            }

            Text(
                "${selectedIds.size} selected",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            // Create playlist button
            IconButton(
                enabled = selectedIds.isNotEmpty(),
                onClick = {
                    val route = RouteBuilder.createPlaylist(selectedIds)
                    nav.navigate(route) {
                        launchSingleTop = true
                        popUpTo(Routes.MAIN) { inclusive = false; saveState = true }
                    }
                    onClearSelection()
                }
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Create Playlist from Selection"
                )
            }

            // Add to playlist button
            IconButton(
                enabled = selectedIds.isNotEmpty(),
                onClick = {
                    val route = RouteBuilder.addToPlaylist(selectedIds)
                    nav.navigate(route) {
                        launchSingleTop = true
                        popUpTo(Routes.MAIN) { inclusive = false; saveState = true }
                    }
                    onClearSelection()
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Add selected to Playlist"
                )
            }

            // Delete button
            IconButton(
                enabled = selectedIds.isNotEmpty(),
                onClick = onDeleteSelected
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
            }
        }
    }
}
