package com.thorfio.playzer.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thorfio.playzer.ui.navigation.Routes
import com.thorfio.playzer.ui.screens.MainScreen
import com.thorfio.playzer.ui.screens.SearchScreen
import com.thorfio.playzer.ui.screens.PlayerScreen
import com.thorfio.playzer.ui.screens.SettingsScreen
import com.thorfio.playzer.ui.screens.EqualizerScreen
import com.thorfio.playzer.ui.screens.PlaylistScreen
import com.thorfio.playzer.ui.screens.ArtistScreen
import com.thorfio.playzer.ui.screens.AlbumScreen
import com.thorfio.playzer.ui.screens.AddToPlaylistScreen
import com.thorfio.playzer.ui.screens.CreatePlaylistScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    dark: Boolean,
    dynamic: Boolean,
    onToggleDark: () -> Unit,
    onToggleDynamic: () -> Unit,
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Playzer", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                NavigationDrawerItem(label = { Text("Settings") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Routes.SETTINGS)
                })
                NavigationDrawerItem(label = { Text("Equalizer") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Routes.EQUALIZER)
                })
            }
        }
    ) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text("My Music") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }) { padding ->
            AppNavHost(navController, padding, dark, dynamic, onToggleDark, onToggleDynamic)
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    padding: PaddingValues,
    dark: Boolean,
    dynamic: Boolean,
    onToggleDark: () -> Unit,
    onToggleDynamic: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
        modifier = Modifier.fillMaxSize().padding(padding) // Added padding so tabs are visible below TopAppBar
    ) {
        composable(Routes.MAIN) { MainScreen(navController) }
        composable(Routes.SEARCH) { SearchScreen(navController) }
        composable(Routes.PLAYER) { PlayerScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController, dark, dynamic, onToggleDark, onToggleDynamic) }
        composable(Routes.EQUALIZER) { EqualizerScreen(navController) }
        // Param screens simplified placeholders
        composable(Routes.PLAYLIST) { backStack -> PlaylistScreen(navController, playlistId = backStack.arguments?.getString("playlistId") ?: "") }
        composable(Routes.ARTIST) { backStack -> ArtistScreen(navController, artistId = backStack.arguments?.getString("artistId") ?: "") }
        composable(Routes.ALBUM) { backStack -> AlbumScreen(navController, albumId = backStack.arguments?.getString("albumId") ?: "") }
        composable(Routes.ADD_TO_PLAYLIST) { backStack -> AddToPlaylistScreen(navController, trackIds = parseIds(backStack.arguments?.getString("trackIds"))) }
        composable(Routes.CREATE_PLAYLIST) { backStack -> CreatePlaylistScreen(navController, prefilledTrackIds = parseIds(backStack.arguments?.getString("trackIds"))) }
    }
}

private fun parseIds(raw: String?): List<String> = when {
    raw == null -> emptyList()
    raw.isBlank() -> emptyList()
    raw == "_" -> emptyList()
    else -> raw.split(',').filter { it.isNotBlank() && it != "_" }
}
