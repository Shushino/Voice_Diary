package com.shushino.voicediary.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shushino.voicediary.domain.model.Mood
import com.shushino.voicediary.domain.model.WritingStats

fun Mood.color(): Color = when (this) {
    Mood.HAPPY -> Color(0xFFFFD700) // Gold/Yellow
    Mood.SAD -> Color(0xFF6495ED) // Cornflower Blue
    Mood.CALM -> Color(0xFF90EE90) // Light Green
    Mood.ANGRY -> Color(0xFFFF6347) // Tomato/Red
    Mood.ANXIOUS -> Color(0xFFFF8C00) // Dark Orange
    Mood.NEUTRAL -> Color(0xFFD3D3D3) // Light Gray
}

@Composable
fun WritingStatsCard(
    stats: WritingStats,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📊 Writing Stats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "rotation")
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotation)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Collapsed view summary
            if (!expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total: ${stats.totalEntries} entries",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "🔥 ${stats.currentStreakDays} day streak",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Expanded content
            AnimatedVisibility(visible = expanded) {
                Column {
                    StatRow("Total entries", "${stats.totalEntries}")
                    StatRow("Total words", "${stats.totalWords}")
                    StatRow("Longest entry", "${stats.longestEntryWords} words")
                    StatRow("Average per entry", "${stats.avgWordsPerEntry} words")
                    StatRow("Current streak", "🔥 ${stats.currentStreakDays} days")

                    if (stats.moodDistribution.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Mood Distribution (Last 30 days)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val totalMoodEntries = stats.moodDistribution.values.sum().toFloat()
                        
                        stats.moodDistribution.forEach { (mood, count) ->
                            MoodDistributionRow(mood, count, totalMoodEntries)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MoodDistributionRow(mood: Mood, count: Int, total: Float) {
    val percentage = (count / total) * 100
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = mood.emoji, modifier = Modifier.width(24.dp))
        
        Box(modifier = Modifier.weight(1f).height(12.dp).padding(horizontal = 8.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = size.width * (count / total)
                drawRoundRect(
                    color = mood.color(),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
        
        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(32.dp)
        )
    }
}
