@file:OptIn(ExperimentalMaterial3Api::class)

package com.thorfio.playzer.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.data.scanner.AudioFileScanner
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

    // State for scan status
    var isScanning by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf("") }
    var isMediaScanning by remember { mutableStateOf(false) }

    // Check permission status
    val hasPermission = remember {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    // Get current library stats
    val tracks by ServiceLocator.musicLibrary.tracks.collectAsState()
    val albums by ServiceLocator.musicLibrary.albums.collectAsState()
    val artists by ServiceLocator.musicLibrary.artists.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Settings
            OutlinedCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Theme",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Mode")
                        Switch(
                            checked = darkEnabledCurrent,
                            onCheckedChange = { onToggleDark() }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dynamic Colors")
                        Switch(
                            checked = dynamicEnabledCurrent,
                            onCheckedChange = { onToggleDynamic() }
                        )
                    }
                }
            }

            // Music Library Settings
            OutlinedCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Music Library",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Permission Status
                    if (!hasPermission) {
                        Text(
                            "⚠️ Audio permission not granted. Please restart the app and grant permission.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            "✓ Audio permission granted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Library Stats
                    Text(
                        "Current Library: ${tracks.size} tracks, ${albums.size} albums, ${artists.size} artists",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        "MediaStore API automatically scans all audio files on your device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (scanStatus.isNotEmpty()) {
                        Text(
                            scanStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Trigger Media Scan Button
                    FilledTonalButton(
                        onClick = {
                            if (!hasPermission) {
                                Toast.makeText(context, "Please grant audio permission first", Toast.LENGTH_SHORT).show()
                                return@FilledTonalButton
                            }

                            isMediaScanning = true
                            val musicPath = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_MUSIC
                            ).absolutePath

                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(musicPath),
                                null
                            ) { path, uri ->
                                scope.launch {
                                    isMediaScanning = false
                                    val message = "Media scan completed for: $path"
                                    scanStatus = message
                                    Toast.makeText(context, "Media scan complete. Now tap 'Scan Audio Files'", Toast.LENGTH_LONG).show()
                                }
                            }
                            Toast.makeText(context, "Triggering media scan for Music folder...", Toast.LENGTH_SHORT).show()
                        },
                        enabled = !isMediaScanning && hasPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isMediaScanning) "Scanning Media..." else "Trigger Media Scan")
                    }

                    Text(
                        "Use 'Trigger Media Scan' first if files don't appear, then 'Scan Audio Files'",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Scan Button
                    Button(
                        onClick = {
                            if (!hasPermission) {
                                Toast.makeText(context, "Please grant audio permission first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            scope.launch {
                                isScanning = true
                                scanStatus = "Scanning audio files..."
                                try {
                                    val result = AudioFileScanner.scanAndUpdateLibrary(context)
                                    scanStatus = result.message
                                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    scanStatus = "Scan failed: ${e.message}"
                                    Toast.makeText(context, "Scan failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isScanning = false
                                }
                            }
                        },
                        enabled = !isScanning && hasPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isScanning) "Scanning..." else "Scan Audio Files")
                    }
                }
            }
        }
    }
}

