package com.thorfio.playzer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.model.Playlist
import com.thorfio.playzer.data.model.Track
import com.thorfio.playzer.ui.components.TrackAlbumArt
import com.thorfio.playzer.ui.navigation.RouteBuilder
import com.thorfio.playzer.ui.navigation.Routes
import com.thorfio.playzer.ui.theme.Charcoal
import com.thorfio.playzer.ui.theme.DarkGrey
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(nav: NavController, playlistId: String) {
    val playlistStore = ServiceLocator.playlistStore
    val repo = ServiceLocator.musicRepository
    val playlistsState = playlistStore.playlists.collectAsState()
    val tracksState = repo.tracks.collectAsState()
    val playlist = playlistsState.value.firstOrNull { it.id == playlistId }
    val tracks: List<Track> = remember(playlist, tracksState.value) {
        if (playlist == null) emptyList() else repo.tracksByIds(playlist.trackIds)
    }
    var showRename by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf(playlist?.name ?: "") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var lastRemoved: Pair<Track, Int>? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist?.name ?: "Playlist") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (playlist != null) {
                        IconButton(onClick = { showRename = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Rename") }
                        IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete Playlist") }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            PlaylistHeaderArt(playlist = playlist, tracks = tracks)
            HeaderStatsRow(playlist, tracks)
            HorizontalDivider()
            TrackListingForPlaylist(
                playlistId = playlistId,
                tracks = tracks,
                nav = nav,
                onPlay = { idx ->
                    if (tracks.isNotEmpty()) ServiceLocator.playbackController.loadAndPlay(tracks, idx)
                    nav.navigate(Routes.PLAYER)
                },
                onRemove = { trackId ->
                    val pl = playlist ?: return@TrackListingForPlaylist
                    val idx = pl.trackIds.indexOf(trackId)
                    val t = tracks.firstOrNull { it.id == trackId } ?: return@TrackListingForPlaylist
                    lastRemoved = t to idx
                    playlistStore.removeTrack(pl.id, trackId)
                    // If cover removed, update cover to next remaining first track
                    if (pl.coverTrackId == trackId) {
                        val newCover = pl.trackIds.firstOrNull { it != trackId }
                        playlistStore.setCover(pl.id, newCover)
                    }
                    scope.launch {
                        val res = snackbarHostState.showSnackbar(
                            message = "Removed '${t.title}'",
                            actionLabel = "UNDO",
                            withDismissAction = true
                        )
                        if (res == SnackbarResult.ActionPerformed) {
                            lastRemoved?.let { (tr, originalIndex) ->
                                playlistStore.insertTrackAt(pl.id, tr.id, originalIndex)
                                if (pl.coverTrackId == null) playlistStore.setCover(pl.id, tr.id)
                            }
                        }
                        lastRemoved = null
                    }
                }
            )
        }
    }

    if (showRename && playlist != null) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            confirmButton = {
                TextButton(onClick = {
                    playlistStore.rename(playlist.id, renameValue.ifBlank { "Playlist" })
                    showRename = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } },
            title = { Text("Edit Playlist Name") },
            text = { OutlinedTextField(value = renameValue, onValueChange = { renameValue = it }, label = { Text("Name") }) }
        )
    }

    if (showDeleteDialog && playlist != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    playlistStore.delete(playlist.id)
                    showDeleteDialog = false
                    nav.popBackStack()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
            title = { Text("Delete Playlist") },
            text = { Text("Permanently delete '${playlist.name}'?") }
        )
    }
}

@Composable
private fun HeaderStatsRow(playlist: Playlist?, tracks: List<Track>) {
    val totalDuration = tracks.sumOf { it.durationMs } / 1000
    val mins = totalDuration / 60
    val secs = totalDuration % 60
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Playlist Icon", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(playlist?.name ?: "--", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("${tracks.size} tracks • ${"%d:%02d".format(mins, secs)}", style = MaterialTheme.typography.bodySmall)
        }
        if (tracks.isNotEmpty()) FilledTonalButton(onClick = { ServiceLocator.playbackController.loadAndPlay(tracks) }) { Text("Play All") }
    }
}

@Composable
private fun PlaylistHeaderArt(playlist: Playlist?, tracks: List<Track>) {
    val coverTrack = tracks.firstOrNull { it.id == playlist?.coverTrackId } ?: tracks.firstOrNull()
    val height = 140.dp
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth().height(height)) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer)) {
            Text(
                text = coverTrack?.albumTitle ?: playlist?.name ?: "--",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun TrackListingForPlaylist(
    playlistId: String,
    tracks: List<Track>,
    nav: NavController,
    onPlay: (Int) -> Unit,
    onRemove: (String) -> Unit
) {
    var menuForTrackId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        itemsIndexed(tracks, key = { _, t -> t.id }) { index, t ->
            val rowColor = if (index % 2 == 0) Charcoal else DarkGrey
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(rowColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackAlbumArt(track = t, size = 48.dp, modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(t.artistName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onPlay(index) }) { Icon(Icons.Filled.PlayArrow, contentDescription = "Play") }
                IconButton(onClick = { menuForTrackId = t.id }) { Icon(Icons.Filled.MoreVert, contentDescription = "Track Options") }
                DropdownMenu(expanded = menuForTrackId == t.id, onDismissRequest = { menuForTrackId = null }) {
                    DropdownMenuItem(text = { Text("Play") }, onClick = { onPlay(index); menuForTrackId = null })
                    DropdownMenuItem(text = { Text("Add to Playlist") }, onClick = {
                        nav.navigate(RouteBuilder.addToPlaylist(listOf(t.id)))
                        menuForTrackId = null
                    })
                    DropdownMenuItem(text = { Text("Remove") }, onClick = { onRemove(t.id); menuForTrackId = null })
                }
            }
            HorizontalDivider()
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}
