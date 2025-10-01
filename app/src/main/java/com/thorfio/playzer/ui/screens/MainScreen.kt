@file:OptIn(ExperimentalFoundationApi::class)

package com.thorfio.playzer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.model.Track
import com.thorfio.playzer.ui.components.TrackAlbumArt
import com.thorfio.playzer.ui.navigation.Routes
import com.thorfio.playzer.ui.navigation.RouteBuilder
import com.thorfio.playzer.ui.theme.Charcoal
import com.thorfio.playzer.ui.theme.DarkGrey
import com.thorfio.playzer.ui.theme.LightGrey
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalConfiguration

private enum class MainTab { TRACKS, PLAYLISTS, ALBUMS, ARTISTS }
private enum class TrackSortField { TITLE, DATE_ADDED, ALBUM, ARTIST }
private enum class SortOrder { ASC, DESC }
private enum class AlbumSortOrder { ASC, DESC }
private enum class ArtistSortOrder { ASC, DESC }

@Composable
fun MainScreen(nav: NavController) {
    val repo = ServiceLocator.musicRepository
    val tracks by repo.tracks.collectAsState()
    val albums by repo.albums.collectAsState()
    val artists by repo.artists.collectAsState()
    val playlistStore = ServiceLocator.playlistStore
    val playlists by playlistStore.playlists.collectAsState()
    val playback = ServiceLocator.playbackController

    var currentTab by remember { mutableStateOf(MainTab.TRACKS) }

    // Sort state
    var trackSortField by remember { mutableStateOf(TrackSortField.TITLE) }
    var trackSortOrder by remember { mutableStateOf(SortOrder.ASC) }
    var albumSortOrder by remember { mutableStateOf(AlbumSortOrder.ASC) }
    var artistSortOrder by remember { mutableStateOf(ArtistSortOrder.ASC) }

    // Selection state
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    val toggleSelect: (Track) -> Unit = {
        if (selectedIds.contains(it.id)) selectedIds.remove(it.id) else selectedIds.add(it.id)
        if (selectedIds.isEmpty()) selectionMode = false
    }

    // Replaced rememberSnackbarHostState with manual remember due to unresolved reference
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastDeletedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }

    // Playlist tab dialogs/state
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var renamingPlaylistId by remember { mutableStateOf<String?>(null) }
    var renamePlaylistValue by remember { mutableStateOf("") }
    var deletingPlaylistId by remember { mutableStateOf<String?>(null) }

    // Album/Artist bulk delete state
    var bulkDeleteTrackIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var bulkDeleteMessage by remember { mutableStateOf("") }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val headerRowHeight: Dp = (screenHeightDp / 12f).dp   // First row (handled by outer TopAppBar) + we use same for mini player row
    val miniPlayerHeight: Dp = headerRowHeight            // Second row: minimized player
    val tabRowHeight: Dp = (screenHeightDp / 24f).dp      // Tab select row
    val trackRowHeight: Dp = (screenHeightDp / 12f).dp    // Approx row height spec

    Box(Modifier.fillMaxSize()) {
        // Snackbar host at natural bottom (mini player now at top, so no offset needed)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        Column(Modifier.fillMaxSize()) {
            // Second header row: Minimized player (top app bar is outside this composable in AppRoot)
            MinimizedPlayerBar(onClick = { nav.navigate(Routes.PLAYER) }, modifier = Modifier.height(miniPlayerHeight))
            // Tab row with specified height
            Surface(tonalElevation = 2.dp) {
                TabRow(selectedTabIndex = currentTab.ordinal, modifier = Modifier.height(tabRowHeight), containerColor = LightGrey) {
                    MainTab.entries.forEach { tab ->
                        Tab(
                            text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            selected = tab == currentTab,
                            onClick = { currentTab = tab }
                        )
                    }
                }
            }
            when (currentTab) {
                MainTab.TRACKS -> {
                    val sortedTracks = remember(tracks, trackSortField, trackSortOrder) {
                        val base = when (trackSortField) {
                            TrackSortField.TITLE -> tracks.sortedBy { it.title.lowercase() }
                            TrackSortField.DATE_ADDED -> tracks.sortedBy { it.dateAdded }
                            TrackSortField.ALBUM -> tracks.sortedBy { it.albumTitle.lowercase() }
                            TrackSortField.ARTIST -> tracks.sortedBy { it.artistName.lowercase() }
                        }
                        if (trackSortOrder == SortOrder.ASC) base else base.reversed()
                    }
                    TrackList(
                        tracks = sortedTracks,
                        selectionMode = selectionMode,
                        selectedIds = selectedIds,
                        onToggleSelect = { t -> toggleSelect(t) },
                        onEnterSelection = { selectionMode = true; toggleSelect(it) },
                        onPlay = { playback.loadAndPlay(sortedTracks, sortedTracks.indexOf(it)); nav.navigate(Routes.PLAYER) },
                        onRowClick = {
                            if (selectionMode) toggleSelect(it) else { playback.loadAndPlay(sortedTracks, sortedTracks.indexOf(it)); nav.navigate(Routes.PLAYER) }
                        },
                        onAddToPlaylist = { t -> nav.navigate(RouteBuilder.addToPlaylist(listOf(t.id))) },
                        onDelete = { t -> pendingDeleteIds = listOf(t.id); showDeleteDialog = true },
                        rowHeight = trackRowHeight,
                        sortControls = {
                            TracksSortHeader(
                                count = tracks.size,
                                field = trackSortField,
                                order = trackSortOrder,
                                onChangeField = { trackSortField = it },
                                onToggleOrder = { trackSortOrder = if (trackSortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC }
                            )
                        }
                    )
                }
                MainTab.PLAYLISTS -> PlaylistsPanel(
                    playlists = playlists,
                    nav = nav,
                    onPlay = { pl ->
                        val list = repo.tracksByIds(pl.trackIds)
                        if (list.isNotEmpty()) { playback.loadAndPlay(list); nav.navigate(Routes.PLAYER) }
                    },
                    onRename = { pl -> renamingPlaylistId = pl.id; renamePlaylistValue = pl.name },
                    onDelete = { pl -> deletingPlaylistId = pl.id },
                    onOpen = { pl -> nav.navigate("playlist/${pl.id}") }
                )
                MainTab.ALBUMS -> {
                    val sorted = remember(albums, albumSortOrder) {
                        val base = albums.sortedBy { it.title.lowercase() }
                        if (albumSortOrder == AlbumSortOrder.ASC) base else base.reversed()
                    }
                    AlbumsPanel(
                        albums = sorted,
                        nav = nav,
                        sortControls = {
                            SimpleSortHeader(label = "${albums.size} Albums", asc = albumSortOrder == AlbumSortOrder.ASC) {
                                albumSortOrder = if (albumSortOrder == AlbumSortOrder.ASC) AlbumSortOrder.DESC else AlbumSortOrder.ASC
                            }
                        },
                        onPlay = { album ->
                            val list = repo.tracksByIds(album.trackIds)
                            if (list.isNotEmpty()) { playback.loadAndPlay(list); nav.navigate(Routes.PLAYER) }
                        },
                        onAddToPlaylist = { album -> nav.navigate(RouteBuilder.addToPlaylist(album.trackIds)) },
                        onDelete = { album ->
                            val list = repo.tracksByIds(album.trackIds)
                            bulkDeleteTrackIds = list.map { it.id }
                            bulkDeleteMessage = "Allow this App to Permanently Delete all ${list.size} Number of Audio Files from this Album?"
                            showBulkDeleteDialog = true
                        }
                    )
                }
                MainTab.ARTISTS -> {
                    val sorted = remember(artists, artistSortOrder) {
                        val base = artists.sortedBy { it.name.lowercase() }
                        if (artistSortOrder == ArtistSortOrder.ASC) base else base.reversed()
                    }
                    ArtistsPanel(
                        artists = sorted,
                        nav = nav,
                        sortControls = {
                            SimpleSortHeader(label = "${artists.size} Artists", asc = artistSortOrder == ArtistSortOrder.ASC) {
                                artistSortOrder = if (artistSortOrder == ArtistSortOrder.ASC) ArtistSortOrder.DESC else ArtistSortOrder.ASC
                            }
                        },
                        onPlay = { artist ->
                            val list = repo.tracksByIds(artist.trackIds)
                            if (list.isNotEmpty()) { playback.loadAndPlay(list); nav.navigate(Routes.PLAYER) }
                        },
                        onAddToPlaylist = { artist -> nav.navigate(RouteBuilder.addToPlaylist(artist.trackIds)) },
                        onDelete = { artist ->
                            val list = repo.tracksByIds(artist.trackIds)
                            bulkDeleteTrackIds = list.map { it.id }
                            bulkDeleteMessage = "Allow this App to Permanently Delete all ${list.size} Number of Audio Files for this Artist?"
                            showBulkDeleteDialog = true
                        }
                    )
                }
            }
        }
        // Floating buttons back to standard bottom padding (mini player no longer at bottom)
        if (currentTab == MainTab.TRACKS) {
            FloatingActionButton(
                onClick = { if (tracks.isNotEmpty()) playback.loadAndPlay(tracks.shuffled()) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) { Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle") }
        }
        if (currentTab == MainTab.PLAYLISTS) {
            FloatingActionButton(
                onClick = { showCreatePlaylistDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) { Text("+") }
        }

        // Selection toolbar remains overlay at top
        if (selectionMode && currentTab == MainTab.TRACKS) {
            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp, modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectionMode = false; selectedIds.clear() }) { Icon(Icons.Filled.Close, contentDescription = "Cancel selection") }
                    IconButton(onClick = {
                        // Select All / Clear All toggle
                        if (selectedIds.size == tracks.size) {
                            selectedIds.clear(); selectionMode = false
                        } else {
                            selectedIds.clear(); selectedIds.addAll(tracks.map { it.id }); selectionMode = true
                        }
                    }) {
                        if (selectedIds.size == tracks.size) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear All")
                        } else {
                            Icon(Icons.Filled.DoneAll, contentDescription = "Select All")
                        }
                    }
                    Text("${selectedIds.size} selected", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(enabled = selectedIds.isNotEmpty(), onClick = {
                        // Create new playlist directly from current selection
                        val sel = selectedIds.toList()
                        selectionMode = false
                        val route = com.thorfio.playzer.ui.navigation.RouteBuilder.createPlaylist(sel)
                        nav.navigate(route) {
                            launchSingleTop = true
                            popUpTo(Routes.MAIN) { inclusive = false; saveState = true }
                        }
                        selectedIds.clear()
                    }) { Icon(Icons.Filled.Add, contentDescription = "Create Playlist from Selection") }
                    IconButton(enabled = selectedIds.isNotEmpty(), onClick = {
                        val route = RouteBuilder.addToPlaylist(selectedIds.toList())
                        selectionMode = false
                        nav.navigate(route) {
                            launchSingleTop = true
                            popUpTo(Routes.MAIN) { inclusive = false; saveState = true }
                        }
                        selectedIds.clear()
                    }) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add selected to Playlist") }
                    IconButton(enabled = selectedIds.isNotEmpty(), onClick = {
                        pendingDeleteIds = selectedIds.toList()
                        showDeleteDialog = true
                    }) { Icon(Icons.Filled.Delete, contentDescription = "Delete selected") }
                }
            }
        }

        if (showDeleteDialog) {
            val count = pendingDeleteIds.size
            val repo = ServiceLocator.musicRepository
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        lastDeletedTracks = repo.tracksByIds(pendingDeleteIds)
                        val deletedCount = pendingDeleteIds.size
                        repo.deleteTracks(pendingDeleteIds)
                        showDeleteDialog = false
                        selectionMode = false
                        selectedIds.clear()
                        val toRestore = lastDeletedTracks
                        scope.launch {
                            val res = snackbarHostState.showSnackbar(
                                message = "Deleted $deletedCount track" + if (deletedCount > 1) "s" else "",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Short
                            )
                            if (res == SnackbarResult.ActionPerformed) {
                                repo.restoreTracks(toRestore)
                            }
                            pendingDeleteIds = emptyList()
                            lastDeletedTracks = emptyList()
                        }
                    }) { Text("Allow") }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Deny") } },
                title = { Text(if (count == 1) "Delete Track" else "Delete $count Tracks") },
                text = { Text(if (count == 1) "Permanently delete this audio file?" else "Permanently delete these $count audio files?") }
            )
        }

        // Playlist create dialog
        if (showCreatePlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false; newPlaylistName = "" },
                confirmButton = {
                    TextButton(onClick = {
                        val name = newPlaylistName.ifBlank { "New Playlist" }
                        playlistStore.create(name)
                        scope.launch { snackbarHostState.showSnackbar("Playlist Created") }
                        newPlaylistName = ""; showCreatePlaylistDialog = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showCreatePlaylistDialog = false; newPlaylistName = "" }) { Text("CANCEL") } },
                title = { Text("Create New Playlist") },
                text = { OutlinedTextField(value = newPlaylistName, onValueChange = { newPlaylistName = it }, label = { Text("Name") }) }
            )
        }

        if (renamingPlaylistId != null) {
            val pl = playlists.firstOrNull { it.id == renamingPlaylistId }
            if (pl != null) {
                AlertDialog(
                    onDismissRequest = { renamingPlaylistId = null },
                    confirmButton = {
                        TextButton(onClick = {
                            playlistStore.rename(pl.id, renamePlaylistValue.ifBlank { "Playlist" })
                            scope.launch { snackbarHostState.showSnackbar("Playlist Renamed") }
                            renamingPlaylistId = null
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { renamingPlaylistId = null }) { Text("CANCEL") } },
                    title = { Text("Edit Playlist Name") },
                    text = { OutlinedTextField(value = renamePlaylistValue, onValueChange = { renamePlaylistValue = it }) }
                )
            }
        }

        if (deletingPlaylistId != null) {
            val pl = playlists.firstOrNull { it.id == deletingPlaylistId }
            if (pl != null) {
                AlertDialog(
                    onDismissRequest = { deletingPlaylistId = null },
                    confirmButton = {
                        TextButton(onClick = {
                            playlistStore.delete(pl.id)
                            scope.launch { snackbarHostState.showSnackbar("Playlist Deleted") }
                            deletingPlaylistId = null
                        }) { Text("Yes") }
                    },
                    dismissButton = { TextButton(onClick = { deletingPlaylistId = null }) { Text("No") } },
                    title = { Text("Confirm Delete") },
                    text = { Text("Are you sure you want to permanently delete this Playlist?") }
                )
            }
        }

        if (showBulkDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showBulkDeleteDialog = false; bulkDeleteTrackIds = emptyList() },
                confirmButton = {
                    TextButton(onClick = {
                        val repoLocal = ServiceLocator.musicRepository
                        val toRestore = repoLocal.tracksByIds(bulkDeleteTrackIds)
                        repoLocal.deleteTracks(bulkDeleteTrackIds)
                        showBulkDeleteDialog = false
                        scope.launch {
                            val res = snackbarHostState.showSnackbar(
                                message = "Track(s) Permanently Deleted",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Short
                            )
                            if (res == SnackbarResult.ActionPerformed) repoLocal.restoreTracks(toRestore)
                            bulkDeleteTrackIds = emptyList()
                        }
                    }) { Text("Allow") }
                },
                dismissButton = { TextButton(onClick = { showBulkDeleteDialog = false; bulkDeleteTrackIds = emptyList() }) { Text("Deny") } },
                title = { Text(bulkDeleteMessage) },
                text = { Text("This action cannot be undone") }
            )
        }
    }
}

@Composable
private fun MinimizedPlayerBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val playback = ServiceLocator.playbackController
    val track by playback.currentTrack.collectAsState()
    Surface(tonalElevation = 2.dp, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp).combinedClickable(onClick = onClick, onLongClick = {}), verticalAlignment = Alignment.CenterVertically) {
            TrackAlbumArt(track = track, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(track?.title ?: "--", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                Text(track?.artistName ?: "--", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val isPlaying by playback.isPlaying.collectAsState()
            IconButton(onClick = { playback.togglePlayPause() }) { Icon(Icons.Filled.PlayArrow, contentDescription = if (isPlaying) "Pause" else "Play") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackList(
    tracks: List<Track>,
    selectionMode: Boolean,
    selectedIds: List<String>,
    onToggleSelect: (Track) -> Unit,
    onEnterSelection: (Track) -> Unit,
    onPlay: (Track) -> Unit,
    onRowClick: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onDelete: (Track) -> Unit,
    rowHeight: Dp,
    sortControls: @Composable () -> Unit
) {
    var menuForTrackId by remember { mutableStateOf<String?>(null) }

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
            val selectBg = MaterialTheme.colorScheme.primaryContainer
            // Switch alternating row colors (DarkGrey for even rows, Charcoal for odd rows)
            val rowColor = if (index % 2 == 0) DarkGrey else Charcoal

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .background(if (selected) selectBg else rowColor)
                    .combinedClickable(
                        onClick = { onRowClick(track) },
                        onLongClick = { if (!selectionMode) onEnterSelection(track) else onToggleSelect(track) }
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    Text(track.artistName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (!selectionMode) {
                    Text(
                        formatTime(track.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Box {
                        IconButton(onClick = { menuForTrackId = track.id }) { Icon(Icons.Filled.MoreVert, contentDescription = "Track Options") }
                        DropdownMenu(expanded = menuForTrackId == track.id, onDismissRequest = { if (menuForTrackId == track.id) menuForTrackId = null }) {
                            DropdownMenuItem(text = { Text("Play") }, onClick = { onPlay(track); menuForTrackId = null })
                            DropdownMenuItem(text = { Text("Add to Playlist") }, onClick = { onAddToPlaylist(track); menuForTrackId = null })
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(track); menuForTrackId = null })
                        }
                    }
                }
            }
            HorizontalDivider()
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// Helper for time formatting (duplicate of player; localized here)
private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun PlaylistsPanel(
    playlists: List<com.thorfio.playzer.data.model.Playlist>,
    nav: NavController,
    onPlay: (com.thorfio.playzer.data.model.Playlist) -> Unit,
    onRename: (com.thorfio.playzer.data.model.Playlist) -> Unit,
    onDelete: (com.thorfio.playzer.data.model.Playlist) -> Unit,
    onOpen: (com.thorfio.playzer.data.model.Playlist) -> Unit
) {
    val repo = ServiceLocator.musicRepository
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
                    Text("${p.trackIds.size} tracks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

// NEW composables
@Composable
private fun TracksSortHeader(count: Int, field: TrackSortField, order: SortOrder, onChangeField: (TrackSortField) -> Unit, onToggleOrder: () -> Unit) {
    Surface(tonalElevation = 2.dp, color = Charcoal) {
        Row(Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$count Tracks", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            var menu by remember { mutableStateOf(false) }
            // Replace AssistChip with IconButton
            IconButton(
                onClick = { menu = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort by ${field.name.lowercase().replaceFirstChar { it.uppercase() }} (${if (order==SortOrder.ASC) "Ascending" else "Descending"})"
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                TrackSortField.values().forEach { f ->
                    DropdownMenuItem(text = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) }, onClick = { onChangeField(f); menu = false })
                }
                HorizontalDivider()
                DropdownMenuItem(text = { Text(if (order == SortOrder.ASC) "Descending" else "Ascending") }, onClick = { onToggleOrder(); menu = false })
            }
        }
    }
}

@Composable
private fun SimpleSortHeader(label: String, asc: Boolean, onToggle: () -> Unit) {
    Surface(tonalElevation = 2.dp, color = Charcoal) {
        Row(Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            // Replace AssistChip with IconButton
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Toggle sort order (currently ${if (asc) "Ascending" else "Descending"})"
                )
            }
        }
    }
}

// Albums panel extended options
@Composable
private fun AlbumsPanel(
    albums: List<com.thorfio.playzer.data.model.Album>,
    nav: NavController,
    sortControls: (@Composable () -> Unit)? = null,
    onPlay: (com.thorfio.playzer.data.model.Album) -> Unit,
    onAddToPlaylist: (com.thorfio.playzer.data.model.Album) -> Unit,
    onDelete: (com.thorfio.playzer.data.model.Album) -> Unit
) {
    val repo = ServiceLocator.musicRepository
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

// Artists panel extended options
@Composable
private fun ArtistsPanel(
    artists: List<com.thorfio.playzer.data.model.Artist>,
    nav: NavController,
    sortControls: (@Composable () -> Unit)? = null,
    onPlay: (com.thorfio.playzer.data.model.Artist) -> Unit,
    onAddToPlaylist: (com.thorfio.playzer.data.model.Artist) -> Unit,
    onDelete: (com.thorfio.playzer.data.model.Artist) -> Unit,
) {
    var menuForId by remember { mutableStateOf<String?>(null) }
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
