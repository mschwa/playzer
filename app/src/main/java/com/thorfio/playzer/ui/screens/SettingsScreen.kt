@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.thorfio.playzer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SettingsScreen(
    nav: NavController,
    darkEnabledCurrent: Boolean,
    dynamicEnabledCurrent: Boolean,
    onToggleDark: () -> Unit,
    onToggleDynamic: () -> Unit
) {
    var darkEnabled by remember(darkEnabledCurrent) { mutableStateOf(darkEnabledCurrent) }
    var dynamicEnabled by remember(dynamicEnabledCurrent) { mutableStateOf(dynamicEnabledCurrent) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Settings") }, navigationIcon = {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SettingRow(
                title = "Dark Theme",
                desc = "Toggle dark / light theme",
                checked = darkEnabled,
                onChecked = {
                    darkEnabled = it
                    onToggleDark()
                }
            )
            SettingRow(
                title = "Dynamic Color",
                desc = "Use system palette (Android 12+)",
                checked = dynamicEnabled,
                onChecked = {
                    dynamicEnabled = it
                    onToggleDynamic()
                }
            )
            SettingRow(title = "Rescan Library", desc = "Trigger a file system rescan (placeholder)", checked = false, onChecked = { })
            SettingRow(title = "Periodic Playlist Backup", desc = "Persist playlists occasionally (placeholder)", checked = false, onChecked = { })
        }
    }
}

@Composable
private fun SettingRow(title: String, desc: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
