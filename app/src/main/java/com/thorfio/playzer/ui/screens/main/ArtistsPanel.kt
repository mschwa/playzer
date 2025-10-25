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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.data.model.Artist
import com.thorfio.playzer.ui.theme.Charcoal
import com.thorfio.playzer.ui.theme.DarkGrey

@Composable
fun ArtistsPanel(
    artists: List<Artist>,
    nav: NavController,
    sortControls: (@Composable () -> Unit)? = null,
    onPlay: (Artist) -> Unit,
    onAddToPlaylist: (Artist) -> Unit,
    onDelete: (Artist) -> Unit,
) {
    var menuForId by remember { mutableStateOf<Long?>(null) }
    LazyColumn(Modifier.fillMaxSize()) {
        stickyHeader { if (sortControls != null) sortControls() }
        if (artists.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No artists") } }
        itemsIndexed(artists, key = { _, it -> it.id }) { index, artist ->
            // Switch alternating row colors (DarkGrey for even rows, Charcoal for odd rows)
            val rowColor = if (index % 2 == 0) DarkGrey else Charcoal
            Row(Modifier.fillMaxWidth().background(rowColor).padding(16.dp).clickable { nav.navigate("artist/${artist.id}") }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(artist.name)
                    Text("${artist.albumIds.size} Albums", style = MaterialTheme.typography.labelSmall)
                }
                Text("${artist.trackIds.size} Tracks", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 8.dp))
                Box {
                    IconButton(onClick = { menuForId = artist.id }) { Icon(Icons.Filled.MoreVert, contentDescription = "Artist Options") }
                    DropdownMenu(expanded = menuForId == artist.id, onDismissRequest = { if (menuForId == artist.id) menuForId = null }) {
                        DropdownMenuItem(text = { Text("Play") }, onClick = { onPlay(artist); menuForId = null })
                        DropdownMenuItem(text = { Text("Add to Playlist") }, onClick = { onAddToPlaylist(artist); menuForId = null })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(artist); menuForId = null })
                    }
                }
            }
            HorizontalDivider()
        }
    }
}