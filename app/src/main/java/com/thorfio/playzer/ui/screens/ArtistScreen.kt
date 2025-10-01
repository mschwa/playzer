package com.thorfio.playzer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.model.Track
import com.thorfio.playzer.ui.components.TrackListComponent
import com.thorfio.playzer.ui.navigation.Routes
import com.thorfio.playzer.ui.navigation.RouteBuilder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(nav: NavController, artistId: String) {
    val repo = ServiceLocator.musicRepository
    val artist = repo.artists.collectAsState().value.firstOrNull { it.id == artistId }
    val albums = repo.albums.collectAsState().value.filter { it.artistId == artistId }
    val tracks: List<Track> = artist?.trackIds?.let { repo.tracksByIds(it) } ?: emptyList()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for track deletion
    var showDeleteDialog by remember { mutableStateOf(false) }
    var trackToDelete by remember { mutableStateOf<Track?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Artist Icon", tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(artist?.name ?: "Artist")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (tracks.isNotEmpty()) IconButton(onClick = {
                        ServiceLocator.playbackController.loadAndPlay(tracks)
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

            // Use the reusable track list component
            TrackListComponent(
                tracks = tracks,
                onPlay = { track, index ->
                    ServiceLocator.playbackController.loadAndPlay(tracks, index)
                    nav.navigate(Routes.PLAYER)
                },
                onAddToPlaylist = { track ->
                    nav.navigate(RouteBuilder.addToPlaylist(listOf(track.id)))
                },
                onDeleteTrack = { track ->
                    trackToDelete = track
                    showDeleteDialog = true
                },
                rowHeight = 72.dp,
                showAlbumName = true, // Show album name instead of artist name
                useAlternateBackground = true
            )
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
                tonalElevation = 2.dp,
                modifier = Modifier
                    .size(width = 120.dp, height = 80.dp)
                    .clickable { onAlbumClick(album) }
            ) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        album.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
