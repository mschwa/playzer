package com.thorfio.playzer.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thorfio.playzer.ui.theme.Charcoal

@Composable
fun TracksSortHeader(
    count: Int,
    field: TrackSortField,
    order: TrackSortOrder,
    onChangeField: (TrackSortField) -> Unit,
    onToggleOrder: () -> Unit
) {
    Surface(tonalElevation = 2.dp, color = Charcoal) {
        Row(
            Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // This ensures proper spacing between left and right content
        ) {
            // Left side: Track count label
            Text("$count Tracks", style = MaterialTheme.typography.labelLarge)

            // Right side: Sort icon and dropdown
            Box {
                var menu by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { menu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort by ${field.name.lowercase().replaceFirstChar { it.uppercase() }} (${if (order==TrackSortOrder.ASC) "Ascending" else "Descending"})"
                    )
                }

                // Position the dropdown on the right side
                DropdownMenu(
                    expanded = menu,
                    onDismissRequest = { menu = false },
                    offset = androidx.compose.ui.unit.DpOffset(0.dp, 0.dp) // Default position (right-aligned with the icon)
                ) {
                    TrackSortField.entries.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = { onChangeField(f); menu = false }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(if (order == TrackSortOrder.ASC) "Descending" else "Ascending") },
                        onClick = { onToggleOrder(); menu = false }
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleSortHeader(label: String, asc: Boolean, onToggle: () -> Unit) {
    Surface(tonalElevation = 2.dp, color = Charcoal) {
        Row(
            Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // This ensures proper spacing between left and right content
        ) {
            // Left side: Album/Artist count label
            Text(label, style = MaterialTheme.typography.labelLarge)

            // Right side: Sort icon
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Toggle sort order (currently ${if (asc) "Ascending" else "Descending"})"
                )
            }
        }
    }
}
