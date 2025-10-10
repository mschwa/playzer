package com.thorfio.playzer.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
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
import com.thorfio.playzer.data.model.Playlist
import com.thorfio.playzer.ui.theme.Charcoal
import com.thorfio.playzer.ui.theme.DarkGrey

@Composable
fun PlaylistsPanel(
    playlists: List<Playlist>,
    onPlay: (Playlist) -> Unit,
    onRename: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    onOpen: (Playlist) -> Unit
) {
    var menuForId by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize()) {
        if (playlists.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No playlists yet") } }
        }
        itemsIndexed(playlists, key = { _, it -> it.id }) { index, p ->
            // Switch alternating row colors (DarkGrey for even rows, Charcoal for odd rows)
            val rowColor = if (index % 2 == 0) DarkGrey else Charcoal
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(rowColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f).clickable { onOpen(p) }) {
                    Text(p.name, maxLines = 1)
                    Text("${p.fileUris.size} tracks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box { // menu anchor
                    IconButton(onClick = { menuForId = p.id }) { Icon(Icons.Filled.MoreVert, contentDescription = "Playlist Options") }
                    DropdownMenu(expanded = menuForId == p.id, onDismissRequest = { if (menuForId == p.id) menuForId = null }) {
                        DropdownMenuItem(text = { Text("Play") }, onClick = { onPlay(p); menuForId = null })
                        DropdownMenuItem(text = { Text("Edit Playlist Name") }, onClick = { onRename(p); menuForId = null })
                        DropdownMenuItem(text = { Text("Delete Playlist") }, onClick = { onDelete(p); menuForId = null })
                    }
                }
            }
            HorizontalDivider()
        }
        item { Spacer(Modifier.height(100.dp)) }
    }
}
