package com.thorfio.playzer.ui.screens

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.model.Track
import com.thorfio.playzer.ui.components.TrackListComponent
import com.thorfio.playzer.ui.navigation.Routes
import com.thorfio.playzer.ui.navigation.RouteBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(nav: NavController, albumId: Long) {
    val repo = ServiceLocator.musicLibrary
    val album = repo.albums.collectAsState().value.firstOrNull { it.id == albumId }
    val tracks: List<Track> = album?.trackIds?.let { repo.tracksByIds(it) } ?: emptyList()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for track deletion confirmation
    var showDeleteDialog by remember { mutableStateOf(false) }
    var trackToDelete by remember { mutableStateOf<Track?>(null) }

    // State for track menu
    var menuForTrackId by remember { mutableStateOf<Long?>(null) }

    // State for album art
    var albumArt by remember { mutableStateOf<ImageBitmap?>(null) }

    // Extract album art from the first track
    LaunchedEffect(albumId) {
        if (tracks.isNotEmpty()) {
            val firstTrack = tracks.firstOrNull()
            firstTrack?.let {
                val art = extractAlbumArtFromTrack(context, it)
                albumArt = art
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            // Album cover art panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Background image (if available)
                albumArt?.let { art ->
                    Image(
                        bitmap = art,
                        contentDescription = "Album Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    // Fallback icon if no album art is available
                    Box(
                        modifier = Modifier.matchParentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Album Icon",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    }
                }

                // Gradient overlay for better text visibility
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x73000000)
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

                // Album information positioned at bottom
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    AlbumHeader(
                        albumTitle = album?.title,
                        artistName = album?.artistName,
                        trackCount = tracks.size
                    )
                }
            }

            HorizontalDivider()

            // Track list using LazyColumn
            LazyColumn {
                itemsIndexed(tracks) { index, track ->
                    TrackListComponent(
                        track = track,
                        index = index,
                        isSelected = false,
                        isSelectionMode = false,
                        rowHeight = 72.dp,
                        onClick = {
                            ServiceLocator.playbackService.loadAndPlay(tracks, index)
                            nav.navigate(Routes.PLAYER)
                        },
                        onLongClick = { /* No-op */ },
                        onMenuClick = { menuForTrackId = track.id },
                        menuContent = {
                            DropdownMenu(
                                expanded = menuForTrackId == track.id,
                                onDismissRequest = { menuForTrackId = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add to Playlist") },
                                    onClick = {
                                        nav.navigate(RouteBuilder.addToPlaylist(listOf(track.id)))
                                        menuForTrackId = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        trackToDelete = track
                                        showDeleteDialog = true
                                        menuForTrackId = null
                                    }
                                )
                            }
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog && trackToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    trackToDelete = null
                },
                title = { Text("Delete Track") },
                text = { Text("Are you sure you want to permanently delete '${trackToDelete?.title}'?") },
                confirmButton = {
                    TextButton(onClick = {
                        trackToDelete?.let { track ->
                            val deletedTrack = track
                            repo.deleteTracks(listOf(track.id))
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Track deleted",
                                    actionLabel = "UNDO",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    repo.restoreTracks(listOf(deletedTrack))
                                }
                            }
                        }
                        showDeleteDialog = false
                        trackToDelete = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        trackToDelete = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun AlbumHeader(albumTitle: String?, artistName: String?, trackCount: Int) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
        Text(
            text = albumTitle ?: "Unknown Album",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artistName ?: "Unknown Artist",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "$trackCount ${if (trackCount == 1) "track" else "tracks"}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

private suspend fun extractAlbumArtFromTrack(context: android.content.Context, track: Track): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, android.net.Uri.parse(track.fileUri))
        val art = retriever.embeddedPicture
        retriever.release()
        art?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
        }
    } catch (e: Exception) {
        null
    }
}

