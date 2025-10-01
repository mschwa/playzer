package com.thorfio.playzer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thorfio.playzer.data.model.Track
import com.thorfio.playzer.ui.theme.Charcoal
import com.thorfio.playzer.ui.theme.DarkGrey

/**
 * A reusable track list component that can be used across different screens.
 * Matches the exact layout and functionality of MainScreen's track listing.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackListComponent(
    tracks: List<Track>,
    onPlay: (Track, Int) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onDeleteTrack: (Track) -> Unit,
    selectionMode: Boolean = false,
    selectedIds: List<String> = emptyList(),
    onToggleSelect: (Track) -> Unit = {},
    onEnterSelection: (Track) -> Unit = {},
    rowHeight: Dp = 72.dp,
    headerContent: (@Composable () -> Unit)? = null,
    footerContent: (LazyListScope.() -> Unit)? = null,
    showTrackNumber: Boolean = false,
    showAlbumName: Boolean = false,
    useAlternateBackground: Boolean = true
) {
    var menuForTrackId by remember { mutableStateOf<String?>(null) }

    LazyColumn(Modifier.fillMaxSize()) {
        if (headerContent != null) {
            stickyHeader { headerContent() }
        }

        if (tracks.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("No tracks found", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                val selected = selectedIds.contains(track.id)
                val selectBg = MaterialTheme.colorScheme.primaryContainer

                // Switch alternating row colors (DarkGrey for even rows, Charcoal for odd rows)
                val rowColor = if (useAlternateBackground) {
                    if (index % 2 == 0) DarkGrey else Charcoal
                } else {
                    MaterialTheme.colorScheme.surface
                }

                // Define row click behavior to match MainScreen
                val onRowClick = { onPlay(track, index) }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .background(if (selected) selectBg else rowColor)
                        .combinedClickable(
                            onClick = { onRowClick() },
                            onLongClick = { if (!selectionMode) onEnterSelection(track) else onToggleSelect(track) }
                        )
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showTrackNumber) {
                        Text(
                            "${track.trackNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp).width(24.dp)
                        )
                    }

                    if (selectionMode) {
                        Icon(
                            imageVector = if (selected) Icons.Filled.Check else Icons.Filled.MoreVert,
                            contentDescription = if (selected) "Selected" else "Not selected",
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }

                    TrackAlbumArt(track = track, size = 48.dp, modifier = Modifier.padding(end = 12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (showAlbumName) track.albumTitle else track.artistName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!selectionMode) {
                        Text(
                            formatTime(track.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        // Use a dropdown menu like in MainScreen instead of individual buttons
                        Box {
                            IconButton(onClick = { menuForTrackId = track.id }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Track Options")
                            }
                            DropdownMenu(
                                expanded = menuForTrackId == track.id,
                                onDismissRequest = { if (menuForTrackId == track.id) menuForTrackId = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Play") },
                                    onClick = {
                                        onPlay(track, index)
                                        menuForTrackId = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add to Playlist") },
                                    onClick = {
                                        onAddToPlaylist(track)
                                        menuForTrackId = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        onDeleteTrack(track)
                                        menuForTrackId = null
                                    }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
            }
        }

        // Optional footer content
        footerContent?.invoke(this)

        // Default bottom spacer if no custom footer
        if (footerContent == null) {
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// Helper for time formatting
private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
