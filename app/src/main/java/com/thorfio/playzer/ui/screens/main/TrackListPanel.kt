package com.thorfio.playzer.ui.screens.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thorfio.playzer.data.model.Track
import com.thorfio.playzer.ui.components.TrackListComponent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackListPanel(
    tracks: List<Track>,
    selectionMode: Boolean,
    selectedIds: List<Long>,
    onToggleSelect: (Track) -> Unit,
    onEnterSelection: (Track) -> Unit,
    onRowClick: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onDelete: (Track) -> Unit,
    rowHeight: Dp,
    sortControls: @Composable () -> Unit
) {
    var menuForTrackId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(Modifier.fillMaxSize()) {
        stickyHeader { sortControls() }
        if (tracks.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("No tracks found", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        itemsIndexed(tracks) { index, track ->
            val selected = selectedIds.contains(track.id)

            TrackListComponent(
                track = track,
                index = index,
                isSelected = selected,
                isSelectionMode = selectionMode,
                rowHeight = rowHeight,
                onClick = { onRowClick(track) },
                onLongClick = { if (!selectionMode) onEnterSelection(track) else onToggleSelect(track) },
                onMenuClick = { menuForTrackId = track.id },
                menuContent = {
                    DropdownMenu(expanded = menuForTrackId == track.id, onDismissRequest = { if (menuForTrackId == track.id) menuForTrackId = null }) {
                        DropdownMenuItem(text = { Text("Add to Playlist") }, onClick = { onAddToPlaylist(track); menuForTrackId = null })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(track); menuForTrackId = null })
                    }
                }
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}
