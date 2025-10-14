package com.thorfio.playzer.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.model.Album
import com.thorfio.playzer.ui.components.TrackAlbumArt
import com.thorfio.playzer.ui.theme.Charcoal
import com.thorfio.playzer.ui.theme.DarkGrey

@Composable
fun AlbumsPanel(
    albums: List<Album>,
    nav: NavController,
    sortControls: (@Composable () -> Unit)? = null,
    onPlay: (Album) -> Unit,
    onAddToPlaylist: (Album) -> Unit,
    onDelete: (Album) -> Unit
) {
    val repo = ServiceLocator.musicLibrary
    var menuForId by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize()) {
        stickyHeader { if (sortControls != null) sortControls() }
        if (albums.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No albums") } }
        itemsIndexed(albums, key = { _, it -> it.id }) { index, a ->
            // Switch alternating row colors (DarkGrey for even rows, Charcoal for odd rows)
            val rowColor = if (index % 2 == 0) DarkGrey else Charcoal
            Row(Modifier.fillMaxWidth().background(rowColor).padding(16.dp).clickable { nav.navigate("album/${a.id}") }, verticalAlignment = Alignment.CenterVertically) {
                val artTrack = a.trackIds.firstOrNull()?.let { id -> repo.tracks.collectAsState().value.firstOrNull { it.id == id } }
                TrackAlbumArt(track = artTrack, size = 48.dp, modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.title)
                    Text(a.artistName, style = MaterialTheme.typography.labelSmall)
                }
                Box {
                    IconButton(onClick = { menuForId = a.id }) { Icon(Icons.Filled.MoreVert, contentDescription = "Album Options") }
                    DropdownMenu(expanded = menuForId == a.id, onDismissRequest = { if (menuForId == a.id) menuForId = null }) {
                        DropdownMenuItem(text = { Text("Play") }, onClick = { onPlay(a); menuForId = null })
                        DropdownMenuItem(text = { Text("Add to Playlist") }, onClick = { onAddToPlaylist(a); menuForId = null })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(a); menuForId = null })
                    }
                }
            }
            HorizontalDivider()
        }
    }
}