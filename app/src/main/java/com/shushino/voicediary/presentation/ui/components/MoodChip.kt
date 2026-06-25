package com.shushino.voicediary.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shushino.voicediary.domain.model.Mood

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodChip(
    mood: Mood,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        FilterChip(
            selected = true,
            onClick = onClick,
            label = { Text("${mood.emoji} ${mood.name.lowercase().replaceFirstChar { it.uppercase() }}") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            ),
            border = null,
            modifier = modifier.padding(end = 8.dp)
        )
    } else {
        OutlinedCard(
            onClick = onClick,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = modifier.padding(end = 8.dp),
            shape = FilterChipDefaults.shape
        ) {
            Text(
                text = "${mood.emoji} ${mood.name.lowercase().replaceFirstChar { it.uppercase() }}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
