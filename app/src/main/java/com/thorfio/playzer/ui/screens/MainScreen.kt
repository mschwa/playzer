package com.thorfio.playzer.ui.screens

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.model.Track
import com.thorfio.playzer.ui.navigation.Routes
import com.thorfio.playzer.ui.screens.main.AlbumSortOrder
import com.thorfio.playzer.ui.screens.main.AlbumsPanel
import com.thorfio.playzer.ui.screens.main.ArtistSortOrder
import com.thorfio.playzer.ui.screens.main.ArtistsPanel
import com.thorfio.playzer.ui.screens.main.CreatePlaylistDialog
import com.thorfio.playzer.ui.screens.main.DeleteAlbumDialog
import com.thorfio.playzer.ui.screens.main.DeleteArtistDialog
import com.thorfio.playzer.ui.screens.main.DeletePlaylistDialog
import com.thorfio.playzer.ui.screens.main.MainTab
import com.thorfio.playzer.ui.screens.main.MainTopAppBar
import com.thorfio.playzer.ui.screens.main.MinimizedPlayerBar
import com.thorfio.playzer.ui.screens.main.PlayListSortOrder
import com.thorfio.playzer.ui.screens.main.PlaylistsPanel
import com.thorfio.playzer.ui.screens.main.RenamePlaylistDialog
import com.thorfio.playzer.ui.screens.main.SelectionToolbar
import com.thorfio.playzer.ui.screens.main.SimpleSortHeader
import com.thorfio.playzer.ui.screens.main.TrackSortOrder
import com.thorfio.playzer.ui.screens.main.TrackDeletionDialog
import com.thorfio.playzer.ui.screens.main.TrackListPanel
import com.thorfio.playzer.ui.screens.main.TrackSortField
import com.thorfio.playzer.ui.screens.main.TracksSortHeader
import com.thorfio.playzer.ui.theme.DarkGrey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    nav: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    val context = LocalContext.current

    // Services and repositories
    val repo = ServiceLocator.musicLibrary
    val tracks by repo.tracks.collectAsState()
    val albums by repo.albums.collectAsState()
    val artists by repo.artists.collectAsState()
    val playlistStore = ServiceLocator.playlistStore
    val playlists by playlistStore.playlists.collectAsState()
    val playback = ServiceLocator.playbackService
    val prefsRepo = ServiceLocator.appPreferencesRepository

    // Tab state
    val savedTabIndex by prefsRepo.selectedMainTab.collectAsState(initial = 0)
    var currentTab by remember(savedTabIndex) {
        mutableStateOf(MainTab.entries.toTypedArray().getOrElse(savedTabIndex) { MainTab.TRACKS })
    }

    // Sort states
    var trackSortField by remember { mutableStateOf(TrackSortField.TITLE) }
    var trackSortOrder by remember { mutableStateOf(TrackSortOrder.ASC) }
    var playlistSortOrder by remember { mutableStateOf(PlayListSortOrder.ASC) }
    var albumSortOrder by remember { mutableStateOf(AlbumSortOrder.ASC) }
    var artistSortOrder by remember { mutableStateOf(ArtistSortOrder.ASC) }

    // Selection state
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    // Dialog item pointers
    var renamingPlaylistId by remember { mutableStateOf<String?>(null) }
    var renamePlaylistValue by remember { mutableStateOf("") }
    var deletingTrackId by remember { mutableStateOf<Long?>(null) }
    var deletingTrackUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var deletingPlaylistId by remember { mutableStateOf<String?>(null) }
    var deletingArtistId by remember { mutableStateOf<String?>(null) }
    var deletingAlbumId by remember { mutableStateOf<String?>(null) }

    // Control showing of dialogs
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showDeleteTrackDialog by remember { mutableStateOf(false) }
    var showDeleteArtistDialog by remember { mutableStateOf(false) }
    var showDeleteAlbumDialog by remember { mutableStateOf(false) }

    // UI metrics
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val headerRowHeight: Dp = (screenHeightDp / 24f).dp
    val miniPlayerHeight: Dp = (screenHeightDp / 14f).dp
    val tabRowHeight: Dp = (screenHeightDp / 24f).dp
    val trackRowHeight: Dp = (screenHeightDp / 12f).dp

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission launcher for Android 10+ delete confirmation
    val deletePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // User granted permission, now retry the deletion
            deletingTrackId?.let { trackId ->
                deletingTrackUri?.let { uri ->
                    scope.launch {
                        try {
                            val deletedRows = context.contentResolver.delete(uri, null, null)
                            if (deletedRows > 0) {
                                repo.removeTrackFromLibrary(trackId)
                                repo.saveToDisk(context)
                            }
                        } catch (e: Exception) {
                            // Handle any remaining errors
                            e.printStackTrace()
                        } finally {
                            deletingTrackId = null
                            deletingTrackUri = null
                        }
                    }
                }
            }
        } else {
            // User denied permission, clean up state
            deletingTrackId = null
            deletingTrackUri = null
        }
    }

    //region Helper functions
    val updateTab: (MainTab) -> Unit = { tab ->
        if (currentTab != tab) {
            currentTab = tab
            scope.launch {
                prefsRepo.setSelectedMainTab(tab.ordinal)
            }
        }
    }

    val toggleSelect: (Track) -> Unit = {
        if (selectedIds.contains(it.id)) selectedIds.remove(it.id) else selectedIds.add(it.id)
        if (selectedIds.isEmpty()) selectionMode = false
    }

    val toggleSelectAll: () -> Unit = {
        if (selectedIds.size == tracks.size) {
            selectedIds.clear()
            selectionMode = false
        } else {
            selectedIds.clear()
            selectedIds.addAll(tracks.map { it.id })
            selectionMode = true
        }
    }

    fun deleteAudioTrack(audioTrackId: Long) {
        // Construct Uri for the specific audio track
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioTrackId)

        scope.launch {
            try {
                // Attempt to delete the audio track
                val deletedRows = context.contentResolver.delete(uri, null, null)
                if (deletedRows > 0) {
                    // Successfully deleted, remove from library
                    repo.removeTrackFromLibrary(audioTrackId)
                }
            } catch (e: SecurityException) {
                // Permission Handling for Android 10+ (API 29+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException = e as? RecoverableSecurityException
                    val intentSender = recoverableSecurityException?.userAction?.actionIntent?.intentSender

                    if (intentSender != null) {
                        // Save the track ID and URI for retry after permission is granted
                        deletingTrackId = audioTrackId
                        deletingTrackUri = uri
                        // Launch IntentSender to prompt user for permission
                        deletePermissionLauncher.launch(
                            IntentSenderRequest.Builder(intentSender).build()
                        )
                    }
                }
            }
        }
    }
    //endregion

    Scaffold(
        floatingActionButton = {
            when (currentTab) {
                MainTab.TRACKS -> {
                    FloatingActionButton(
                        onClick = { if (tracks.isNotEmpty()) playback.loadAndPlay(tracks.shuffled()) },
                        containerColor = Color(0xFFB71C1C)
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle")
                    }
                }
                MainTab.PLAYLISTS -> {
                    FloatingActionButton(
                        onClick = { showCreatePlaylistDialog = true },
                        containerColor = Color(0xFFB71C1C)
                    ) {
                        Text("+")
                    }
                }
                else -> { /* No FAB for other tabs */ }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Custom top app bar
            MainTopAppBar(drawerState, scope, nav, headerRowHeight)

            // Player bar
            MinimizedPlayerBar(onClick = { nav.navigate(Routes.PLAYER) }, modifier = Modifier.height(miniPlayerHeight))

            // Tab row
            Surface(tonalElevation = 2.dp) {
                TabRow(
                    selectedTabIndex = currentTab.ordinal,
                    modifier = Modifier.height(tabRowHeight),
                    containerColor = DarkGrey
                ) {
                    MainTab.entries.forEach { tab ->
                        Tab(
                            text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            selected = tab == currentTab,
                            onClick = { updateTab(tab) }
                        )
                    }
                }
            }

            // Tab content
            when (currentTab) {
                MainTab.TRACKS -> {
                    val sortedTracks = remember(tracks, trackSortField, trackSortOrder) {
                        val base = when (trackSortField) {
                            TrackSortField.TITLE -> tracks.sortedBy { it.title.lowercase() }
                            TrackSortField.DATE_ADDED -> tracks.sortedBy { it.dateAdded }
                            TrackSortField.ALBUM -> tracks.sortedBy { it.albumTitle.lowercase() }
                            TrackSortField.ARTIST -> tracks.sortedBy { it.artistName.lowercase() }
                        }
                        if (trackSortOrder == TrackSortOrder.ASC) base else base.reversed()
                    }

                    TrackListPanel(
                        tracks = sortedTracks,
                        selectionMode = selectionMode,
                        selectedIds = selectedIds,
                        onToggleSelect = { t -> toggleSelect(t) },
                        onEnterSelection = { selectionMode = true; toggleSelect(it) },
                        onRowClick = {
                            if (selectionMode) toggleSelect(it) else {
                                playback.loadAndPlay(sortedTracks, sortedTracks.indexOf(it))
                                nav.navigate(Routes.PLAYER)
                            }
                        },
                        onAddToPlaylist = { t ->
                            nav.navigate(com.thorfio.playzer.ui.navigation.RouteBuilder.addToPlaylist(listOf(t.id)))
                        },
                        onDelete = { t ->
                            deleteAudioTrack(t.id)
                        },
                        rowHeight = trackRowHeight,
                        sortControls = {
                            TracksSortHeader(
                                count = tracks.size,
                                field = trackSortField,
                                order = trackSortOrder,
                                onChangeField = { trackSortField = it },
                                onToggleOrder = {
                                    trackSortOrder = if (trackSortOrder == TrackSortOrder.ASC)
                                        TrackSortOrder.DESC else TrackSortOrder.ASC
                                }
                            )
                        }
                    )
                }

                MainTab.PLAYLISTS -> {
                    val sorted = remember(playlists, playlistSortOrder) {
                        val base = playlists.sortedBy { it.name.lowercase() }
                        if (playlistSortOrder == PlayListSortOrder.ASC) base else base.reversed()
                    }
                    PlaylistsPanel(
                        playlists = sorted,
                        sortControls = {
                            SimpleSortHeader(
                                label = "${playlists.size} Playlists",
                                asc = playlistSortOrder == PlayListSortOrder.ASC
                            ) {
                                playlistSortOrder = if (playlistSortOrder == PlayListSortOrder.ASC)
                                    PlayListSortOrder.DESC else PlayListSortOrder.ASC
                            }
                        },
                        onPlay = { pl ->
                            val list = repo.tracksByIds(pl.mediaStoreIds)
                            if (list.isNotEmpty()) {
                                playback.loadAndPlay(list)
                                nav.navigate(Routes.PLAYER)
                            }
                        },
                        onRename = { pl ->
                            renamingPlaylistId = pl.id
                            renamePlaylistValue = pl.name
                        },
                        onDelete = { pl -> deletingPlaylistId = pl.id },
                        onOpen = { pl -> nav.navigate("playlist/${pl.id}") }
                    )
                }

                MainTab.ALBUMS -> {
                    val sorted = remember(albums, albumSortOrder) {
                        val base = albums.sortedBy { it.title.lowercase() }
                        if (albumSortOrder == AlbumSortOrder.ASC) base else base.reversed()
                    }
                    AlbumsPanel(
                        albums = sorted,
                        nav = nav,
                        sortControls = {
                            SimpleSortHeader(
                                label = "${albums.size} Albums",
                                asc = albumSortOrder == AlbumSortOrder.ASC
                            ) {
                                albumSortOrder = if (albumSortOrder == AlbumSortOrder.ASC)
                                    AlbumSortOrder.DESC else AlbumSortOrder.ASC
                            }
                        },
                        onPlay = { album ->
                            val list = repo.tracksByIds(album.trackIds)
                            if (list.isNotEmpty()) {
                                playback.loadAndPlay(list)
                                nav.navigate(Routes.PLAYER)
                            }
                        },
                        onAddToPlaylist = { album ->
                            nav.navigate(com.thorfio.playzer.ui.navigation.RouteBuilder.addToPlaylist(album.trackIds))
                        },
                        onDelete = { album ->
                            deletingAlbumId = album.id
                            showDeleteAlbumDialog = true
                        }
                    )
                }

                MainTab.ARTISTS -> {
                    val sorted = remember(artists, artistSortOrder) {
                        val base = artists.sortedBy { it.name.lowercase() }
                        if (artistSortOrder == ArtistSortOrder.ASC) base else base.reversed()
                    }

                    ArtistsPanel(
                        artists = sorted,
                        nav = nav,
                        sortControls = {
                            SimpleSortHeader(
                                label = "${artists.size} Artists",
                                asc = artistSortOrder == ArtistSortOrder.ASC
                            ) {
                                artistSortOrder = if (artistSortOrder == ArtistSortOrder.ASC)
                                    ArtistSortOrder.DESC else ArtistSortOrder.ASC
                            }
                        },
                        onPlay = { artist ->
                            val list = repo.tracksByIds(artist.trackIds)
                            if (list.isNotEmpty()) {
                                playback.loadAndPlay(list)
                                nav.navigate(Routes.PLAYER)
                            }
                        },
                        onAddToPlaylist = { artist ->
                            nav.navigate(com.thorfio.playzer.ui.navigation.RouteBuilder.addToPlaylist(artist.trackIds))
                        },
                        onDelete = { artist ->
                            deletingArtistId = artist.id
                            showDeleteArtistDialog = true
                        }
                    )
                }
            }
        }

        // Selection toolbar (when active)
        if (selectionMode && currentTab == MainTab.TRACKS) {
            SelectionToolbar(
                selectedIds = selectedIds,
                totalTracks = tracks.size,
                onClearSelection = {
                    selectionMode = false
                    selectedIds.clear()
                },
                onToggleSelectAll = toggleSelectAll,
                onDeleteSelected = {
                    //TODO: Safe Delete - pendingDeleteIds = selectedIds.toList()
                    //TODO: Safe Delete - showDeleteDialog = true
                },
                nav = nav
            )
        }

        // Dialogs
        TrackDeletionDialog(
            showDialog = showDeleteTrackDialog,
            trackId = deletingTrackId,
            snackbarHostState = snackbarHostState,
            scope = scope,
            onDismiss = { showDeleteTrackDialog = false }
        )

        CreatePlaylistDialog(
            showDialog = showCreatePlaylistDialog,
            snackbarHostState = snackbarHostState,
            scope = scope,
            onDismiss = { showCreatePlaylistDialog = false }
        )

        RenamePlaylistDialog(
            playlistId = renamingPlaylistId,
            initialName = renamePlaylistValue,
            snackbarHostState = snackbarHostState,
            scope = scope,
            onDismiss = { renamingPlaylistId = null }
        )

        if (deletingPlaylistId != null) {
            val pl = playlists.firstOrNull { it.id == deletingPlaylistId }
            if (pl != null) {
                DeletePlaylistDialog(
                    playlistId = deletingPlaylistId,
                    snackbarHostState = snackbarHostState,
                    scope = scope,
                    onDismiss = { deletingPlaylistId = null }
                )
            }
        }

        DeleteArtistDialog(
            showDialog = showDeleteArtistDialog,
            artistId = deletingArtistId,
            snackbarHostState = snackbarHostState,
            scope = scope,
            onDismiss = {
                showDeleteArtistDialog = false
                deletingArtistId = null
            }
        )

        DeleteAlbumDialog(
            showDialog = showDeleteAlbumDialog,
            albumId = deletingAlbumId,
            snackbarHostState = snackbarHostState,
            scope = scope,
            onDismiss = {
                showDeleteAlbumDialog = false
                deletingAlbumId = null
            }
        )
    }
}
