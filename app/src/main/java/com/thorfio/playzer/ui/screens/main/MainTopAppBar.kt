package com.thorfio.playzer.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thorfio.playzer.ui.navigation.Routes
import com.thorfio.playzer.ui.theme.Charcoal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MainTopAppBar(
    drawerState: DrawerState,
    scope: CoroutineScope,
    nav: NavController,
    height: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(Charcoal)
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side with menu button and title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left-aligned menu button (hamburger menu)
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }

                // Title positioned next to menu button
                Text(
                    "Music Library",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Right-aligned search icon (in the main Row)
            IconButton(onClick = { nav.navigate(Routes.SEARCH) }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        }
    }
}
