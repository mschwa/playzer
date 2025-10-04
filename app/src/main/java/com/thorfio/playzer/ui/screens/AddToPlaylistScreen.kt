package com.thorfio.playzer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.ui.theme.Charcoal
import com.thorfio.playzer.ui.theme.DarkGrey
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistScreen(nav: NavController, trackIds: List<String>) {
    val playlistStore = ServiceLocator.playlistStore
    val playlists by playlistStore.playlists.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Playlist") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            // Single full-width create button per requirements
            Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("Create New Playlist") }
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(playlists, key = { _, it -> it.id }) { index, p ->
                    val rowColor = if (index % 2 == 0) Charcoal else DarkGrey
                    ListItem(
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                        headlineContent = { Text(p.name) },
                        supportingContent = { Text("${p.trackIds.size} tracks") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowColor)
                            .clickable {
                                if (trackIds.isNotEmpty()) {
                                    playlistStore.addTracks(p.id, trackIds)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Added to ${p.name}")
                                    }
                                }
                                nav.popBackStack()
                            }
                    )
                    HorizontalDivider()
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            confirmButton = {
                TextButton(onClick = {
                    playlistStore.create(name.ifBlank { "New Playlist" })
                    scope.launch { snackbarHostState.showSnackbar("Playlist Created") }
                    name = ""; showCreate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
            title = { Text("Create New Playlist") },
            text = { OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true
            ) }
        )
    }
}
