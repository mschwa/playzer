package com.thorfio.playzer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.observers.ActivityLifecycleObserver
import com.thorfio.playzer.services.LifecycleEventLogger
import com.thorfio.playzer.ui.AppRoot
import com.thorfio.playzer.ui.theme.PlayzerTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Audio permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Audio permission denied. App cannot access music files.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request audio permission
        requestAudioPermission()

        // Initialize activity lifecycle tracking
        val eventLogger = LifecycleEventLogger.getInstance(this)
        val activityObserver = ActivityLifecycleObserver(eventLogger, "MainActivity")
        lifecycle.addObserver(activityObserver)

        val themePrefs = ServiceLocator.themePreferences
        setContent {
            val dark by themePrefs.darkThemeFlow.collectAsState(initial = false)
            val dynamic by themePrefs.dynamicColorFlow.collectAsState(initial = true)
            PlayzerTheme(darkTheme = dark, dynamicColor = dynamic) {
                AppRoot(
                    dark = dark,
                    dynamic = dynamic,
                    onToggleDark = { themePrefs.setDark(!dark) },
                    onToggleDynamic = { themePrefs.setDynamic(!dynamic) }
                )
            }
        }
    }

    private fun requestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
            }
            else -> {
                // Request permission
                permissionLauncher.launch(permission)
            }
        }
    }
}

