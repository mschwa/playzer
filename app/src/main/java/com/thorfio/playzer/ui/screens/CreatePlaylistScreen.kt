package com.thorfio.playzer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistScreen(nav: NavController, prefilledTrackIds: List<String>) {
    val playlistStore = ServiceLocator.playlistStore
    val musicRepo = ServiceLocator.musicLibrary
    val tracksState = musicRepo.tracks.collectAsState()
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var creating by remember { mutableStateOf(false) }
    val canCreate = name.text.isNotBlank() && !creating

    // Convert track IDs to file URIs
    val fileUris = remember(prefilledTrackIds, tracksState.value) {
        if (prefilledTrackIds.isEmpty() || prefilledTrackIds.firstOrNull() == "_") {
            emptyList()
        } else {
            tracksState.value
                .filter { it.id in prefilledTrackIds }
                .map { it.fileUri }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("New Playlist") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
        )
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (prefilledTrackIds.isNotEmpty()) {
                AssistChip(onClick = {}, label = { Text("Will include ${fileUris.size} track(s)") })
            }
            Button(
                onClick = {
                    creating = true
                    scope.launch {
                        val created = if (fileUris.isEmpty()) {
                            playlistStore.createReturning(name.text.trim())
                        } else playlistStore.createAndAdd(name.text.trim(), fileUris)
                        nav.popBackStack()
                        nav.navigate(Routes.PLAYLIST.replace("{playlistId}", created.id))
                    }
                },
                enabled = canCreate,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create") }
        }
    }
}
