package com.thorfio.playzer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.model.Track
import com.thorfio.playzer.ui.components.TrackAlbumArt
import com.thorfio.playzer.ui.components.TrackListComponent
import com.thorfio.playzer.ui.navigation.Routes
import com.thorfio.playzer.ui.navigation.RouteBuilder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(nav: NavController, artistId: String) {
    val repo = ServiceLocator.musicLibrary
    val artist = repo.artists.collectAsState().value.firstOrNull { it.id == artistId }
    val albums = repo.albums.collectAsState().value.filter { it.artistId == artistId }
    val tracks: List<Track> = artist?.trackIds?.let { repo.tracksByIds(it) } ?: emptyList()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for track deletion
    var showDeleteDialog by remember { mutableStateOf(false) }
    var trackToDelete by remember { mutableStateOf<Track?>(null) }

    // State for track menu
    var menuForTrackId by remember { mutableStateOf<Long?>(null) }

    // State for selection mode
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = artist?.name ?: "Artist",
                        fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.835f,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (tracks.isNotEmpty()) IconButton(onClick = {
                        ServiceLocator.playbackService.loadAndPlay(tracks)
                        nav.navigate(Routes.PLAYER)
                    }) { Icon(Icons.Default.PlayArrow, contentDescription = "Play All") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            // Show albums in a horizontal row
            ArtistAlbumsRow(albums = albums, onAlbumClick = { album ->
                nav.navigate("album/${album.id}")
            })

            HorizontalDivider()

            // Track list using LazyColumn
            LazyColumn {
                itemsIndexed(tracks) { index, track ->
                    val selected = selectedIds.contains(track.id)

                    TrackListComponent(
                        track = track,
                        index = index,
                        isSelected = selected,
                        isSelectionMode = selectionMode,
                        rowHeight = 72.dp,
                        onClick = {
                            ServiceLocator.playbackService.loadAndPlay(tracks, index)
                            nav.navigate(Routes.PLAYER)
                        },
                        onLongClick = {
                            if (!selectionMode) {
                                selectionMode = true
                                selectedIds = setOf(track.id)
                            } else {
                                // Toggle selection of this track
                                selectedIds = if (selected) {
                                    selectedIds - track.id
                                } else {
                                    selectedIds + track.id
                                }

                                // If nothing is selected, exit selection mode
                                if (selectedIds.isEmpty()) {
                                    selectionMode = false
                                }
                            }
                        },
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
                    // Add some space at the bottom for better UX
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
                            // Store track for potential restoration
                            val deletedTrack = track

                            // Delete the track
                            repo.deleteTracks(listOf(track.id))

                            // Show snackbar with undo option
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Track deleted",
                                    actionLabel = "UNDO",
                                    duration = SnackbarDuration.Short
                                )

                                if (result == SnackbarResult.ActionPerformed) {
                                    // Restore the track if user clicked UNDO
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
private fun ArtistAlbumsRow(
    albums: List<com.thorfio.playzer.data.model.Album>,
    onAlbumClick: (com.thorfio.playzer.data.model.Album) -> Unit = {}
) {
    if (albums.isEmpty()) return

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        albums.forEach { album ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .size(120.dp) // Changed to square shape (120dp x 120dp)
                    .clickable { onAlbumClick(album) }
            ) {
                Box(
                    Modifier.fillMaxSize(),
                ) {
                    // Display album cover art
                    TrackAlbumArt(
                        track = null,
                        album = album,
                        size = 120.dp, // Match the container size
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient overlay at the bottom to make text readable
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp) // Reduced height for the gradient overlay
                            .align(Alignment.BottomCenter)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )

                    Text(
                        album.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
