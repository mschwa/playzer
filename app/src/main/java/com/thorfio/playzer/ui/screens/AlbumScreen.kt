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
fun AlbumScreen(nav: NavController, albumId: String) {
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
                                    Color(0x73000000) // Changed from 0x99 (60% opacity) to 0x73 (45% opacity)
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
                        isSelected = false, // Not in selection mode for album screen
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
                    // Add some space at the bottom for better UX
                    Spacer(Modifier.height(80.dp))
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
}

@Composable
private fun AlbumHeader(albumTitle: String?, artistName: String?, trackCount: Int) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Album Icon", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    albumTitle ?: "--",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize * 0.835f,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(artistName ?: "--", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$trackCount tracks", style = MaterialTheme.typography.labelSmall)
        }
    }
}

// Helper function to extract album art from a track
private suspend fun extractAlbumArtFromTrack(context: android.content.Context, track: Track): ImageBitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, android.net.Uri.parse(track.fileUri))
            val bytes = retriever.embeddedPicture
            retriever.release()

            if (bytes != null) {
                // Decode at a reasonable size for header background
                val targetSize = 400

                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)

                val (w, h) = opts.outWidth to opts.outHeight
                if (w <= 0 || h <= 0) return@withContext null

                var sample = 1
                while ((w / sample) > targetSize * 2 || (h / sample) > targetSize * 2) sample *= 2

                val loadOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, loadOpts)
                bitmap?.asImageBitmap()
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
