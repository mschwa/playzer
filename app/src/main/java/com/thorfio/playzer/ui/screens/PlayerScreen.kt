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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.ui.navigation.RouteBuilder
import com.thorfio.playzer.ui.navigation.Routes

@Composable
fun PlayerScreen(nav: NavController) {
    val playback = ServiceLocator.playbackController
    val track by playback.currentTrack.collectAsState()
    val isPlaying by playback.isPlaying.collectAsState()
    var positionMs by remember { mutableStateOf(0L) }
    var menuExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { nav.navigate(Routes.EQUALIZER) }) { Icon(Icons.Filled.Equalizer, contentDescription = "EQ") }
            Box {
                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Options") }
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
        Spacer(Modifier.height(12.dp))
        // Album art placeholder
        Box(Modifier.fillMaxWidth().weight(0.35f).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(track?.albumTitle ?: "--", color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(track?.title ?: "--", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track?.artistName ?: "--", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        // Seekbar placeholder
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(positionMs), style = MaterialTheme.typography.labelSmall)
            Slider(value = if ((track?.durationMs ?: 0L) == 0L) 0f else positionMs / (track!!.durationMs.toFloat()), onValueChange = { v ->
                val dur = track?.durationMs ?: 0L
                positionMs = (v * dur).toLong()
            }, modifier = Modifier.weight(1f))
            Text(formatTime(track?.durationMs ?: 0L), style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { playback.previous() }) { Icon(Icons.Filled.SkipPrevious, contentDescription = "Prev") }
            IconButton(onClick = { playback.togglePlayPause() }) { Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play/Pause") }
            IconButton(onClick = { playback.next() }) { Icon(Icons.Filled.SkipNext, contentDescription = "Next") }
        }
        Spacer(Modifier.weight(1f))
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
