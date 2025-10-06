@file:OptIn(ExperimentalMaterial3Api::class)

package com.thorfio.playzer.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.scanner.AudioFileScanner
import com.thorfio.playzer.data.scanner.MusicScannerService
import com.thorfio.playzer.ui.navigation.Routes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    nav: NavController,
    darkEnabledCurrent: Boolean,
    dynamicEnabledCurrent: Boolean,
    onToggleDark: () -> Unit,
    onToggleDynamic: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepository = ServiceLocator.appPreferencesRepository

    // UI States
    var darkEnabled by remember(darkEnabledCurrent) { mutableStateOf(darkEnabledCurrent) }
    var dynamicEnabled by remember(dynamicEnabledCurrent) { mutableStateOf(dynamicEnabledCurrent) }
    var musicFolder by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var lastScanTimestamp by remember { mutableStateOf(0L) }

    // Load preferences
    LaunchedEffect(Unit) {
        musicFolder = prefsRepository.musicFolderPath.first()
        lastScanTimestamp = prefsRepository.lastScanTimestamp.first()
    }

    // Folder selection launcher
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let {
                // Persist permission for this URI
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)

                // Save the folder path
                val path = uri.toString()
                scope.launch {
                    prefsRepository.setMusicFolderPath(path)
                    musicFolder = path
                }
            }
        }
    )

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

            // Music Folder Setting
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Music Folder",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (musicFolder != null) {
                        val folderName = remember(musicFolder) {
                            try {
                                val uri = Uri.parse(musicFolder)
                                val docFile = DocumentFile.fromTreeUri(context, uri)
                                docFile?.name ?: "Selected Folder"
                            } catch (e: Exception) {
                                "Selected Folder"
                            }
                        }

                        Text(
                            folderName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            "No music folder selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { folderPicker.launch(null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Select Folder")
                        }

                        FilledTonalButton(
                            onClick = {
                                if (!isScanning && musicFolder != null) {
                                    isScanning = true
                                    scope.launch {
                                        try {
                                            // Use the new method that forces a scan immediately
                                            MusicScannerService.startScanNow(context)
                                            // Artificial delay to show scanning state
                                            kotlinx.coroutines.delay(1000)
                                            lastScanTimestamp = prefsRepository.lastScanTimestamp.first()
                                        } finally {
                                            isScanning = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = musicFolder != null && !isScanning
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isScanning) "Scanning..." else "Scan Now")
                        }
                    }

                    if (lastScanTimestamp > 0) {
                        val lastScanText = remember(lastScanTimestamp) {
                            val date = java.util.Date(lastScanTimestamp)
                            val format = java.text.SimpleDateFormat("MMM d, yyyy HH:mm", java.util.Locale.getDefault())
                            "Last scan: ${format.format(date)}"
                        }

                        Text(
                            lastScanText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Add direct file loading option
                    Spacer(modifier = Modifier.height(16.dp))

                    var isDirectLoading by remember { mutableStateOf(false) }

                    Button(
                        onClick = {
                            if (!isDirectLoading && musicFolder != null) {
                                isDirectLoading = true
                                scope.launch {
                                    try {
                                        val filesFound = com.thorfio.playzer.data.scanner.DirectFileLoader.loadFilesFromFolder(context)
                                        if (filesFound > 0) {
                                            Toast.makeText(context, "Found $filesFound audio files", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No audio files found", Toast.LENGTH_SHORT).show()
                                        }
                                        lastScanTimestamp = prefsRepository.lastScanTimestamp.first()
                                    } finally {
                                        isDirectLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = musicFolder != null && !isDirectLoading
                    ) {
                        Text(if (isDirectLoading) "Loading files..." else "Direct Load Files")
                    }

                    // Debug section
                    var debugResult by remember { mutableStateOf("") }
                    var isTestingFolder by remember { mutableStateOf(false) }

                    if (musicFolder != null) {
                        Spacer(modifier = Modifier.height(16.dp))

                        FilledTonalButton(
                            onClick = {
                                if (!isTestingFolder) {
                                    isTestingFolder = true
                                    scope.launch {
                                        try {
                                            debugResult = AudioFileScanner.testScanDirectory(context)
                                        } catch (e: Exception) {
                                            debugResult = "Error: ${e.message}\n${e.stackTraceToString()}"
                                        } finally {
                                            isTestingFolder = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isTestingFolder
                        ) {
                            Text(if (isTestingFolder) "Testing folder..." else "Debug: Test Folder Access")
                        }

                        if (debugResult.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Debug Results:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary)

                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    debugResult,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
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
