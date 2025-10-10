package com.thorfio.playzer.ui.screens.main

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.thorfio.playzer.core.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun TrackDeletionDialog(
    showDialog: Boolean,
    trackId: String?,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    val context = LocalContext.current
    val trackDeletion = ServiceLocator.trackDeletionService
    val message = "Allow Playzer to Permanently Delete this Audio File?"

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    trackDeletion.deleteTrack(context, trackId)
                }

                onDismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Artist Track(s) Permanently Deleted",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                }
            }) { Text("Allow") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Deny") }
        },
        title = { Text(message) },
        text = { Text("This action cannot be undone") }
    )
}

@Composable
fun DeleteArtistDialog(
    showDialog: Boolean,
    artistId: String?,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    val context = LocalContext.current
    val trackDeletion = ServiceLocator.trackDeletionService
    val message = "Allow this App to Permanently Delete all Audio Files for this Artist?"

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(onClick = {

                // Use TrackDeletionService instead of direct repository call
                scope.launch {
                    trackDeletion.deleteArtist(context, artistId)
                }

                onDismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Artist Track(s) Permanently Deleted",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                }
            }) { Text("Allow") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Deny") }
        },
        title = { Text(message) },
        text = { Text("This action cannot be undone") }
    )
}

@Composable
fun DeleteAlbumDialog(
    showDialog: Boolean,
    albumId: String?,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    val context = LocalContext.current
    val trackDeletion = ServiceLocator.trackDeletionService
    val message = "Allow this App to Permanently Delete all Audio Files for this Album?"

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(onClick = {

                // Use TrackDeletionService instead of direct repository call
                scope.launch {
                    trackDeletion.deleteAlbum(context, albumId)
                }

                onDismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Album Track(s) Permanently Deleted",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                }
            }) { Text("Allow") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Deny") }
        },
        title = { Text(message) },
        text = { Text("This action cannot be undone") }
    )
}

@Composable
fun CreatePlaylistDialog(
    showDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    val playlistStore = ServiceLocator.playlistStore
    val newPlaylistName = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
            newPlaylistName.value = ""
        },
        confirmButton = {
            TextButton(onClick = {
                val name = newPlaylistName.value.ifBlank { "New Playlist" }
                playlistStore.create(name)
                scope.launch { snackbarHostState.showSnackbar("Playlist Created") }
                newPlaylistName.value = ""
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                newPlaylistName.value = ""
            }) { Text("CANCEL") }
        },
        title = { Text("Create New Playlist") },
        text = {
            OutlinedTextField(
                value = newPlaylistName.value,
                onValueChange = { newPlaylistName.value = it },
                label = { Text("Name") },
                singleLine = true
            )
        }
    )
}

@Composable
fun RenamePlaylistDialog(
    playlistId: String?,
    initialName: String,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onDismiss: () -> Unit
) {
    if (playlistId == null) return

    val playlistStore = ServiceLocator.playlistStore
    val renamePlaylistValue = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                playlistStore.rename(playlistId, renamePlaylistValue.value.ifBlank { "Playlist" })
                scope.launch { snackbarHostState.showSnackbar("Playlist Renamed") }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
        title = { Text("Edit Playlist Name") },
        text = {
            OutlinedTextField(
                value = renamePlaylistValue.value,
                onValueChange = { renamePlaylistValue.value = it }
            )
        }
    )
}

@Composable
fun DeletePlaylistDialog(
    playlistId: String?,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onDismiss: () -> Unit
) {
    if (playlistId == null) return

    val playlistStore = ServiceLocator.playlistStore

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                playlistStore.delete(playlistId)
                scope.launch { snackbarHostState.showSnackbar("Playlist Deleted") }
                onDismiss()
            }) { Text("Yes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("No") }
        },
        title = { Text("Confirm Delete") },
        text = { Text("Are you sure you want to permanently delete this Playlist?") }
    )
}
