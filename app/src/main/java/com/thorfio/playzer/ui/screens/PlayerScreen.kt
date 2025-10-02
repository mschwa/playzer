package com.thorfio.playzer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.ui.navigation.RouteBuilder
import com.thorfio.playzer.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(nav: NavController) {
    val playback = ServiceLocator.playbackController
    val track by playback.currentTrack.collectAsState()
    val isPlaying by playback.isPlaying.collectAsState()

    // Track the current playback position from the controller
    val playbackPositionMs by playback.currentPositionMs.collectAsState()

    // State for user's manual seeking
    var userSeekingActive by remember { mutableStateOf(false) }
    var userSeekPositionMs by remember { mutableStateOf(0L) }

    // The position to display - use user's seek position when dragging, otherwise use playback position
    val displayPositionMs: Long = if (userSeekingActive) userSeekPositionMs else playbackPositionMs

    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Add to Playlist") }, onClick = {
                                track?.let { nav.navigate(RouteBuilder.addToPlaylist(listOf(it.id))) }
                                menuExpanded = false
                            })
                            DropdownMenuItem(text = { Text("Go to Album") }, onClick = {
                                track?.let { nav.navigate("album/${it.albumId}") }
                                menuExpanded = false
                            })
                            DropdownMenuItem(text = { Text("Go to Artist") }, onClick = {
                                track?.let { nav.navigate("artist/${it.artistId}") }
                                menuExpanded = false
                            })
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween // This ensures content is distributed across the entire height
        ) {
            // Top section
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(60.dp))

                // Album art section with proper cover art display
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .aspectRatio(1f)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    // Use TrackAlbumArt to display the proper cover art
                    if (track != null) {
                        com.thorfio.playzer.ui.components.TrackAlbumArt(
                            track = track,
                            size = 300.dp,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.large),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            highQuality = true  // Enable high quality mode for the PlayerScreen
                        )
                    } else {
                        // Fallback when no track is loaded
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Text("No track playing", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            // Middle section - controls
            Column(Modifier.fillMaxWidth()) {
                // Play controls row with equalizer on left and volume on right
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, // Space between for left/right alignment
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side - Equalizer
                    IconButton(onClick = { nav.navigate(Routes.EQUALIZER) }) {
                        Icon(Icons.Filled.Equalizer, contentDescription = "EQ", modifier = Modifier.size(24.dp))
                    }

                    // Center - Play controls
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { playback.previous() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Filled.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = { playback.togglePlayPause() },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = { playback.next() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Right side - Volume
                    val context = androidx.compose.ui.platform.LocalContext.current
                    IconButton(
                        onClick = {
                            // Open device volume controls
                            val audioManager = androidx.core.content.ContextCompat.getSystemService(
                                context,
                                android.media.AudioManager::class.java
                            )
                            audioManager?.adjustVolume(
                                android.media.AudioManager.ADJUST_SAME,
                                android.media.AudioManager.FLAG_SHOW_UI
                            )
                        }
                    ) {
                        Icon(
                            Icons.Filled.VolumeUp,
                            contentDescription = "Volume",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp)) // Reduced spacing

                // Seekbar - more compact
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp), // Add small horizontal padding
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatTime(displayPositionMs), style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = if ((track?.durationMs ?: 0L) == 0L) 0f else displayPositionMs / (track!!.durationMs.toFloat()),
                        onValueChange = { v ->
                            // Set seeking active when the user starts dragging
                            userSeekingActive = true
                            val dur = track?.durationMs ?: 0L
                            userSeekPositionMs = (v * dur).toLong()
                        },
                        onValueChangeFinished = {
                            // Seek to the new position when the user finishes changing the slider
                            playback.seekTo(userSeekPositionMs)

                            // Reset seeking state after a short delay to allow position to update
                            kotlinx.coroutines.MainScope().launch {
                                kotlinx.coroutines.delay(200)
                                userSeekingActive = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text(formatTime(track?.durationMs ?: 0L), style = MaterialTheme.typography.labelSmall)
                }
            }

            // Bottom section - track info
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp), // Reduced bottom padding
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center // Add compact vertical arrangement
            ) {
                Text(
                    track?.title ?: "--",
                    style = MaterialTheme.typography.titleMedium, // Changed from titleLarge to titleMedium
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track?.artistName ?: "--",
                    style = MaterialTheme.typography.bodySmall, // Changed from bodyMedium to bodySmall
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
