package com.thorfio.playzer.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thorfio.playzer.services.LifecycleEventLogger
import com.thorfio.playzer.ui.navigation.Routes
import com.thorfio.playzer.ui.screens.AddToPlaylistScreen
import com.thorfio.playzer.ui.screens.AlbumScreen
import com.thorfio.playzer.ui.screens.ArtistScreen
import com.thorfio.playzer.ui.screens.CreatePlaylistScreen
import com.thorfio.playzer.ui.screens.EqualizerScreen
import com.thorfio.playzer.ui.screens.EventLogScreen
import com.thorfio.playzer.ui.screens.MainScreen
import com.thorfio.playzer.ui.screens.PlayerScreen
import com.thorfio.playzer.ui.screens.PlaylistScreen
import com.thorfio.playzer.ui.screens.SearchScreen
import com.thorfio.playzer.ui.screens.SettingsScreen
import kotlinx.coroutines.CoroutineScope
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
                NavigationDrawerItem(label = { Text("Event Log") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Routes.EVENT_LOG)
                })
            }
        }
    ) {
        AppNavHost(
            navController,
            PaddingValues(0.dp), // No padding from removed Scaffold
            dark,
            dynamic,
            onToggleDark,
            onToggleDynamic,
            drawerState, // Pass drawer state to be used in MainScreen
            scope // Pass scope to be used in MainScreen
        )
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
    drawerState: DrawerState, // Receive drawer state
    scope: CoroutineScope // Receive coroutine scope
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
        modifier = Modifier.fillMaxSize().padding(padding) // Added padding so tabs are visible below TopAppBar
    ) {
        composable(Routes.MAIN) { MainScreen(navController, drawerState, scope) }
        composable(Routes.SEARCH) { SearchScreen(navController) }
        composable(Routes.PLAYER) { PlayerScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController, dark, dynamic, onToggleDark, onToggleDynamic) }
        composable(Routes.EQUALIZER) { EqualizerScreen(navController) }
        composable(Routes.EVENT_LOG) {
            val eventLogger = LifecycleEventLogger.getInstance(context)
            EventLogScreen(
                eventLogger = eventLogger,
                onNavigateBack = { navController.popBackStack() }
            )
        }
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
