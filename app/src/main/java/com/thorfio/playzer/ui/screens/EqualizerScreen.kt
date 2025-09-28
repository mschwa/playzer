@file:OptIn(ExperimentalMaterial3Api::class)

package com.thorfio.playzer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(nav: NavController) {
    var enabled by remember { mutableStateOf(false) }
    // Five band pseudo-EQ values normalized -1f..1f
    val bandValues = remember { mutableStateListOf(0f,0f,0f,0f,0f) }
    var bass by remember { mutableStateOf(0f) }
    var virtualizer by remember { mutableStateOf(0f) }

    val freqLabels = listOf("31 Hz","125 Hz","500 Hz","2 kHz","8 kHz")

    fun resetBands() { for (i in bandValues.indices) bandValues[i] = 0f }

    fun applyPreset(preset: String) {
        when (preset) {
            "Flat" -> resetBands()
            "Bass" -> { bandValues[0]=0.8f; bandValues[1]=0.5f; bandValues[2]=0f; bandValues[3]=-0.2f; bandValues[4]=-0.3f }
            "Treble" -> { bandValues[0]=-0.3f; bandValues[1]=-0.2f; bandValues[2]=0f; bandValues[3]=0.5f; bandValues[4]=0.8f }
            "V" -> { bandValues[0]=0.6f; bandValues[1]=0.2f; bandValues[2]=-0.4f; bandValues[3]=0.2f; bandValues[4]=0.6f }
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Equalizer") }, navigationIcon = {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        }, actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (enabled) "On" else "Off")
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
        })
    }) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Effects (stub values – would hook into actual audio session if available)
            Column { Text("Bass Boost"); Slider(value = bass, onValueChange = { bass = it }, enabled = enabled) }
            Column { Text("Virtualizer"); Slider(value = virtualizer, onValueChange = { virtualizer = it }, enabled = enabled) }

            Text("Graphic Equalizer", style = MaterialTheme.typography.titleMedium)
            // Vertical slider row
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                bandValues.forEachIndexed { index, value ->
                    VerticalEqBand(
                        label = freqLabels[index],
                        value = value,
                        enabled = enabled,
                        onChange = { bandValues[index] = it }
                    )
                }
            }
            // Presets
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Flat","Bass","Treble","V").forEach { preset ->
                    AssistChip(onClick = { applyPreset(preset) }, label = { Text(preset) })
                }
                AssistChip(onClick = { resetBands() }, label = { Text("Reset") })
            }
            // Individual reset button for all
            OutlinedButton(onClick = { resetBands() }, enabled = enabled) { Text("Center All Bands") }
        }
    }
}

@Composable
private fun VerticalEqBand(label: String, value: Float, enabled: Boolean, onChange: (Float) -> Unit) {
    // Map -1..1 to -12..+12 dB for display
    val db = (value * 12f).roundToInt()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Container to constrain width; we rotate the Slider  -90 degrees so it becomes vertical.
        Box(
            modifier = Modifier
                .height(170.dp)
                .width(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = -1f..1f,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxHeight()
                    .rotate(-90f)
            )
        }
        Text("${db} dB", style = MaterialTheme.typography.labelSmall)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
