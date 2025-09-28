package com.thorfio.playzer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.model.Track
import com.thorfio.playzer.ui.navigation.Routes
import com.thorfio.playzer.ui.navigation.RouteBuilder
import com.thorfio.playzer.ui.theme.Charcoal
import com.thorfio.playzer.ui.theme.DarkGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(nav: NavController, artistId: String) {
    val repo = ServiceLocator.musicRepository
    val artist = repo.artists.collectAsState().value.firstOrNull { it.id == artistId }
    val albums = repo.albums.collectAsState().value.filter { it.artistId == artistId }
    val tracks: List<Track> = artist?.trackIds?.let { repo.tracksByIds(it) } ?: emptyList()

    Scaffold(topBar = {
        TopAppBar(title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Artist Icon", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(artist?.name ?: "Artist")
            }
        }, navigationIcon = {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        }, actions = {
            if (tracks.isNotEmpty()) IconButton(onClick = {
                ServiceLocator.playbackController.loadAndPlay(tracks)
                nav.navigate(Routes.PLAYER)
            }) { Icon(Icons.Default.PlayArrow, contentDescription = "Play All") }
        })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            ArtistAlbumsRow(albums = albums)
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(tracks, key = { _, t -> t.id }) { index, t ->
                    val rowColor = if (index % 2 == 0) Charcoal else DarkGrey
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(rowColor)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t.albumTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        IconButton(onClick = {
                            ServiceLocator.playbackController.loadAndPlay(tracks, index)
                            nav.navigate(Routes.PLAYER)
                        }) { Icon(Icons.Default.PlayArrow, contentDescription = "Play") }
                        IconButton(onClick = { nav.navigate(RouteBuilder.addToPlaylist(listOf(t.id))) }) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to Playlist") }
                    }
                    HorizontalDivider()
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun ArtistAlbumsRow(albums: List<com.thorfio.playzer.data.model.Album>) {
    if (albums.isEmpty()) return
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        albums.forEach { a ->
            Surface(tonalElevation = 2.dp, modifier = Modifier.size(width = 120.dp, height = 80.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(a.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}
