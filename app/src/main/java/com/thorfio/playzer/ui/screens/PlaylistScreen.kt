package com.thorfio.playzer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.zIndex
import kotlinx.coroutines.coroutineScope

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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            PlaylistHeaderArt(playlist = playlist, tracks = tracks, nav = nav)
            // HeaderStatsRow has been removed as its functionality is now in PlaylistHeaderArt
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
private fun PlaylistHeaderArt(playlist: Playlist?, tracks: List<Track>, nav: NavController) {
    // Get a random track for the background if tracks are available, otherwise use the cover track
    val coverTrack = tracks.firstOrNull { it.id == playlist?.coverTrackId } ?: tracks.firstOrNull()
    val randomTrack = remember(tracks) {
        if (tracks.isNotEmpty()) tracks.random() else null
    }

    // Use either the random track or the cover track
    val displayTrack = randomTrack ?: coverTrack

    // Increased height to match AlbumScreen (180dp)
    val height = 180.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Album art as background image
        if (displayTrack != null) {
            TrackAlbumArt(
                track = displayTrack,
                size = 500.dp, // Large size to ensure good quality
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                highQuality = true, // Set to true for highest quality image rendering
                squareCorners = true // Set to true for 90-degree corners like in AlbumScreen
            )

            // Gradient overlay for better text visibility - matches AlbumScreen style
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x73000000) // 45% opacity black
                            )
                        )
                    )
            )

            // Navigation icons overlay at the top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button (left aligned)
                IconButton(
                    onClick = { nav.popBackStack() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x33000000), shape = androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Search button (right aligned)
                IconButton(
                    onClick = { nav.navigate(Routes.SEARCH) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x33000000), shape = androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
            }

            // Playlist information positioned at bottom - matches AlbumScreen layout
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                PlaylistHeaderInfo(
                    playlist = playlist,
                    tracks = tracks
                )
            }
        } else {
            // Fallback if no tracks
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Playlist Icon",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            }

            // Add navigation buttons even when there are no tracks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button (left aligned)
                IconButton(
                    onClick = { nav.popBackStack() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x33000000), shape = androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Search button (right aligned)
                IconButton(
                    onClick = { nav.navigate(Routes.SEARCH) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x33000000), shape = androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistHeaderInfo(playlist: Playlist?, tracks: List<Track>) {
    val totalDuration = tracks.sumOf { it.durationMs } / 1000
    val mins = totalDuration / 60
    val secs = totalDuration % 60

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Left side with playlist info
        Column(modifier = Modifier.weight(1f)) {
            // Playlist name
            Text(
                playlist?.name ?: "--",
                style = MaterialTheme.typography.headlineSmall,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize * 0.835f,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Track count and duration
            Text(
                "${tracks.size} tracks • ${"%d:%02d".format(mins, secs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        // Right side with Play All button
        if (tracks.isNotEmpty()) {
            FilledTonalButton(
                onClick = { ServiceLocator.playbackController.loadAndPlay(tracks) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Play All")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
    val scope = rememberCoroutineScope()
    val playlistStore = ServiceLocator.playlistStore

    // Mutable state to track dragging operations
    var isDragging by remember { mutableStateOf(false) }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var currentDropIndex by remember { mutableStateOf<Int?>(null) }
    var offsetY by remember { mutableStateOf(0f) }

    // Track ordering state
    var trackOrder by remember(tracks) { mutableStateOf(tracks) }

    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState
    ) {
        itemsIndexed(
            items = trackOrder,
            key = { _, item -> item.id }
        ) { index, track ->
            val isBeingDragged = index == draggedItemIndex
            // Changed from DarkGrey to LightGrey for all rows
            val rowColor = DarkGrey
            val rowModifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .background(rowColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .then(
                    if (isBeingDragged) {
                        Modifier
                            .graphicsLayer {
                                alpha = 0.9f
                                scaleX = 1.05f
                                scaleY = 1.05f
                                shadowElevation = 8f
                                translationY = offsetY
                            }
                            .zIndex(1f)
                    } else if (currentDropIndex == index && isDragging) {
                        // Create space for the target drop position
                        Modifier
                            .padding(top = 32.dp)
                            .zIndex(0f)
                    } else {
                        Modifier.zIndex(0f)
                    }
                )

            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drag handle - this will be used to initiate drags
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .pointerInput(track.id) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    draggedItemIndex = index
                                },
                                onDragEnd = {
                                    // Apply the reordering when drag ends
                                    if (draggedItemIndex != null && currentDropIndex != null &&
                                        draggedItemIndex != currentDropIndex &&
                                        draggedItemIndex!! < trackOrder.size &&
                                        currentDropIndex!! < trackOrder.size) {

                                        val draggedItem = trackOrder[draggedItemIndex!!]
                                        val newList = trackOrder.toMutableList()
                                        newList.removeAt(draggedItemIndex!!)
                                        newList.add(currentDropIndex!!, draggedItem)
                                        trackOrder = newList

                                        // Update the playlist store with the new order
                                        scope.launch {
                                            val trackIds = newList.map { it.id }
                                            playlistStore.updateTrackOrder(playlistId, trackIds)
                                        }
                                    }

                                    // Reset drag state
                                    isDragging = false
                                    draggedItemIndex = null
                                    currentDropIndex = null
                                    offsetY = 0f
                                },
                                onDragCancel = {
                                    isDragging = false
                                    draggedItemIndex = null
                                    currentDropIndex = null
                                    offsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()

                                    // Update vertical offset for visual feedback
                                    offsetY += dragAmount.y

                                    // Determine potential drop position based on drag direction and current scroll position
                                    if (draggedItemIndex != null) {
                                        val draggedIdx = draggedItemIndex!!

                                        // Calculate which index we're hovering over
                                        val visibleItems = listState.layoutInfo.visibleItemsInfo
                                        val dragPosition = change.position.y + listState.firstVisibleItemScrollOffset

                                        // Find the item we're dragging over
                                        for (item in visibleItems) {
                                            if (dragPosition > item.offset && dragPosition < item.offset + item.size) {
                                                if (item.index != draggedIdx && item.index != currentDropIndex) {
                                                    currentDropIndex = item.index
                                                }
                                                break
                                            }
                                        }

                                        // Auto-scroll when near edges
                                        val sensitivity = 50
                                        when {
                                            change.position.y < sensitivity -> {
                                                // Scroll up
                                                scope.launch {
                                                    listState.scrollBy(-20f)
                                                }
                                            }
                                            change.position.y > this.size.height - sensitivity -> {
                                                // Scroll down
                                                scope.launch {
                                                    listState.scrollBy(20f)
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                )

                TrackAlbumArt(track = track, size = 48.dp, modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize * 0.835f
                    )
                    Text(track.artistName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onPlay(index) }) { Icon(Icons.Filled.PlayArrow, contentDescription = "Play") }
                IconButton(onClick = { menuForTrackId = track.id }) { Icon(Icons.Filled.MoreVert, contentDescription = "Track Options") }
                DropdownMenu(expanded = menuForTrackId == track.id, onDismissRequest = { menuForTrackId = null }) {
                    DropdownMenuItem(text = { Text("Play") }, onClick = { onPlay(index); menuForTrackId = null })
                    DropdownMenuItem(text = { Text("Add to Playlist") }, onClick = {
                        nav.navigate(RouteBuilder.addToPlaylist(listOf(track.id)))
                        menuForTrackId = null
                    })
                    DropdownMenuItem(text = { Text("Remove") }, onClick = { onRemove(track.id); menuForTrackId = null })
                }
            }
            HorizontalDivider()
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

data class DragInfo(
    val trackId: String,
    val currentIndex: Int,
    val isDragging: Boolean = false
)

private fun Modifier.dragTarget(
    trackId: String,
    currentIndex: Int,
    dragState: MutableState<DragInfo?>,
    playlistId: String,
    onDragFinished: (fromIndex: Int, toIndex: Int) -> Unit
) = composed {
    val isDragging = dragState.value?.trackId == trackId
    val elevation by animateFloatAsState(if (isDragging) 8f else 0f, label = "elevation")
    val scale by animateFloatAsState(if (isDragging) 1.1f else 1.0f, label = "scale")
    val alpha by animateFloatAsState(if (isDragging) 0.9f else 1f, label = "alpha")

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .zIndex(if (isDragging) 1f else 0f)
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    dragState.value = DragInfo(trackId, currentIndex, true)
                },
                onDragEnd = {
                    val fromIndex = dragState.value?.currentIndex ?: return@detectDragGestures
                    val toIndex = dragState.value?.currentIndex ?: return@detectDragGestures
                    if (fromIndex != toIndex) {
                        onDragFinished(fromIndex, toIndex)
                    }
                    dragState.value = null
                },
                onDragCancel = {
                    dragState.value = null
                },
                onDrag = { change, _ ->
                    change.consume()
                }
            )
        }
}

private fun Modifier.dragContainer(
    currentIndex: Int,
    dragState: MutableState<DragInfo?>,
    listState: LazyListState,
    scope: CoroutineScope
) = composed {
    this.pointerInput(currentIndex) {
        detectDragGestures(
            onDrag = { change, dragAmount ->
                change.consume()
                val dragInfo = dragState.value ?: return@detectDragGestures

                // Auto-scroll when near edges
                val scrollThreshold = 50f
                when {
                    change.position.y < scrollThreshold -> scope.launch {
                        listState.scrollBy(-10f)
                    }
                    change.position.y > size.height - scrollThreshold -> scope.launch {
                        listState.scrollBy(10f)
                    }
                }

                // Update current position and reorder if needed
                val yChange = dragAmount.y
                if (yChange > 8 && currentIndex < dragInfo.currentIndex) {
                    // Moving down
                    dragState.value = dragInfo.copy(currentIndex = currentIndex)
                } else if (yChange < -8 && currentIndex > dragInfo.currentIndex) {
                    // Moving up
                    dragState.value = dragInfo.copy(currentIndex = currentIndex)
                }
            },
            onDragStart = { },
            onDragEnd = { },
            onDragCancel = { }
        )
    }
}

suspend fun PointerInputScope.detectImmediateDrag(
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit = { _, _ -> }
) = coroutineScope {
    awaitPointerEventScope {
        val down = awaitFirstDown(requireUnconsumed = false)
        onDragStart(down.position)

        drag(down.id) { change ->
            onDrag(change, change.positionChange())
            change.consume()
        }

        onDragEnd()
    }
}
