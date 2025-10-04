package com.thorfio.playzer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.model.Album
import com.thorfio.playzer.data.model.Artist
import com.thorfio.playzer.data.model.Track
import com.thorfio.playzer.ui.navigation.RouteBuilder
import com.thorfio.playzer.ui.navigation.Routes
import com.thorfio.playzer.ui.theme.Charcoal
import com.thorfio.playzer.ui.theme.DarkGrey
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(nav: NavController) {
    var query by remember { mutableStateOf("") }
    val repo = ServiceLocator.musicRepository
    val (tracks, albums, artists) = remember(query, repo.tracks.collectAsState().value) { repo.search(query) }

    var tracksExpanded by remember { mutableStateOf(true) }
    var albumsExpanded by remember { mutableStateOf(true) }
    var artistsExpanded by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var menuForTrackId by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteTrack by remember { mutableStateOf<Track?>(null) }
    var lastDeletedTrack by remember { mutableStateOf<Track?>(null) }

    Scaffold(topBar = {
        TopAppBar(title = {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search Music Library…") },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotBlank()) IconButton(onClick = { query = "" }) { Text("X") }
                }
            )
        }, navigationIcon = {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        })
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            // Tracks section header
            item(key = "header_tracks") {
                SectionHeader(title = "Tracks", count = tracks.size, expanded = tracksExpanded, onToggle = { tracksExpanded = !tracksExpanded })
            }
            if (tracksExpanded) {
                itemsIndexed(tracks, key = { _, it -> it.id }) { index, t ->
                    val rowColor = if (index % 2 == 0) Charcoal else DarkGrey
                    TrackRowSearch(
                        t = t,
                        bg = rowColor,
                        isMenuOpen = menuForTrackId == t.id,
                        onOpenMenu = { menuForTrackId = t.id },
                        onDismissMenu = { if (menuForTrackId == t.id) menuForTrackId = null },
                        onPlay = {
                            ServiceLocator.playbackController.loadAndPlay(listOf(t))
                            nav.navigate(Routes.PLAYER)
                            menuForTrackId = null
                        },
                        onAddToPlaylist = {
                            nav.navigate(RouteBuilder.addToPlaylist(listOf(t.id)))
                            menuForTrackId = null
                        },
                        onDelete = {
                            pendingDeleteTrack = t
                            showDeleteDialog = true
                            menuForTrackId = null
                        }
                    )
                }
            }
            // Albums section header
            item(key = "header_albums") {
                SectionHeader(title = "Albums", count = albums.size, expanded = albumsExpanded, onToggle = { albumsExpanded = !albumsExpanded })
            }
            if (albumsExpanded) {
                itemsIndexed(albums, key = { _, it -> it.id }) { index, a ->
                    val rowColor = if (index % 2 == 0) Charcoal else DarkGrey
                    AlbumRowSimple(a, rowColor)
                }
            }
            // Artists section header
            item(key = "header_artists") {
                SectionHeader(title = "Artists", count = artists.size, expanded = artistsExpanded, onToggle = { artistsExpanded = !artistsExpanded })
            }
            if (artistsExpanded) {
                itemsIndexed(artists, key = { _, it -> it.id }) { index, a ->
                    val rowColor = if (index % 2 == 0) Charcoal else DarkGrey
                    ArtistRowSimple(a, rowColor)
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showDeleteDialog && pendingDeleteTrack != null) {
        val track = pendingDeleteTrack!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; pendingDeleteTrack = null },
            confirmButton = {
                TextButton(onClick = {
                    lastDeletedTrack = track
                    ServiceLocator.musicRepository.deleteTracks(listOf(track.id))
                    showDeleteDialog = false
                    pendingDeleteTrack = null
                    scope.launch {
                        val res = snackbarHostState.showSnackbar(
                            message = "Track(s) Permanently Deleted",
                            actionLabel = "UNDO",
                            withDismissAction = true
                        )
                        if (res == SnackbarResult.ActionPerformed) {
                            lastDeletedTrack?.let { ServiceLocator.musicRepository.restoreTracks(listOf(it)) }
                        }
                        lastDeletedTrack = null
                    }
                }) { Text("Allow") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false; pendingDeleteTrack = null }) { Text("Deny") } },
            title = { Text("Allow this App to Permanently Delete this Audio File?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Surface(tonalElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("$title ($count)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onToggle) { Text(if (expanded) "Hide" else "Show") }
        }
    }
}

@Composable
private fun TrackRowSearch(
    t: Track,
    bg: Color,
    isMenuOpen: Boolean,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable { onPlay() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                t.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize * 0.835f
            )
            Text(t.artistName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatTime(t.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
        Box { // anchor for menu
            IconButton(onClick = onOpenMenu) { Icon(Icons.Filled.MoreVert, contentDescription = "Track Options") }
            DropdownMenu(expanded = isMenuOpen, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(text = { Text("Play") }, onClick = { onPlay() })
                DropdownMenuItem(text = { Text("Add to Playlist") }, onClick = { onAddToPlaylist() })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete() })
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun AlbumRowSimple(a: Album, bg: Color) {
    Column(Modifier.fillMaxWidth().background(bg).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(a.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(a.artistName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider()
}

@Composable
private fun ArtistRowSimple(a: Artist, bg: Color) {
    Column(Modifier.fillMaxWidth().background(bg).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${a.albumIds.size} Albums • ${a.trackIds.size} Tracks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider()
}

// Helper reused formatting
private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
