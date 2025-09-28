package com.thorfio.playzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.thorfio.playzer.core.ServiceLocator
import com.thorfio.playzer.ui.AppRoot
import com.thorfio.playzer.ui.theme.PlayzerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}
