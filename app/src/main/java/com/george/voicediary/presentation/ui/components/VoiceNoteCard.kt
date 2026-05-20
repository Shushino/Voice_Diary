package com.george.voicediary.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.george.voicediary.domain.model.VoiceNote
import com.george.voicediary.presentation.viewmodel.EntryDetailViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VoiceNoteCard(
    voiceNote: VoiceNote,
    onDelete: (VoiceNote) -> Unit,
    viewModel: EntryDetailViewModel = hiltViewModel() // Assuming shared AudioPlayerManager is in EntryDetailViewModel
) {
    val audioPlayerManager = remember { viewModel.audioPlayerManager }
    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
    val currentPositionMs by audioPlayerManager.currentPositionMs.collectAsState()
    val durationMs by audioPlayerManager.durationMs.collectAsState()
    val currentPlayingFilePath by audioPlayerManager.currentFilePath.collectAsState()

    val currentPlaybackPosition = if (currentPlayingFilePath == voiceNote.filePath) currentPositionMs else 0L
    val currentPlaybackDuration = if (currentPlayingFilePath == voiceNote.filePath) durationMs else voiceNote.durationMs

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isEditingLabel by remember { mutableStateOf(false) }
    var newLabelText by remember { mutableStateOf(voiceNote.label ?: "") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val swipeToDismissState = rememberSwipeToDismissBoxState(confirmValueChange = {
        if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
            showDeleteConfirmation = true
            true
        } else {
            false
        }
    })

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete voice note?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(voiceNote)
                    showDeleteConfirmation = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    SwipeToDismissBox(
        state = swipeToDismissState,
        backgroundContent = {
            val color by animateFloatAsState(targetValue = if (swipeToDismissState.targetValue != SwipeToDismissBoxValue.Settled) 1f else 0f,
                label = "")
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Red.copy(alpha = color)))
        },
        enableDismissFromStartToEnd = false // Only allow swipe from end to start
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Label
                if (isEditingLabel) {
                    OutlinedTextField(
                        value = newLabelText,
                        onValueChange = { newLabelText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Voice Note Label")},
                        singleLine = true,
                        keyboardActions = KeyboardActions(onDone = { 
                            isEditingLabel = false
                            keyboardController?.hide()
                            viewModel.updateVoiceNoteLabel(voiceNote.id, newLabelText)
                        })
                    )
                } else {
                    Text(
                        text = voiceNote.label.let { if (it.isNullOrBlank()) "Voice Note" else it },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().clickable { isEditingLabel = true }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Duration badge
                Text(
                    text = formatDuration(voiceNote.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Seek bar
                Slider(
                    value = currentPlaybackPosition.toFloat(),
                    onValueChange = { audioPlayerManager.seekTo(it.toLong()) },
                    valueRange = 0f..currentPlaybackDuration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Skip back 10s
                    IconButton(onClick = { audioPlayerManager.seekTo(currentPlaybackPosition - 10000L) }) {
                        Icon(Icons.Default.Replay10, contentDescription = "Skip back 10 seconds")
                    }

                    // Play/Pause
                    Button(
                        onClick = {
                            if (currentPlayingFilePath == voiceNote.filePath && isPlaying) {
                                audioPlayerManager.pause()
                            } else {
                                audioPlayerManager.play(voiceNote.filePath)
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            if (currentPlayingFilePath == voiceNote.filePath && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Skip forward 10s
                    IconButton(onClick = { audioPlayerManager.seekTo(currentPlaybackPosition + 10000L) }) {
                        Icon(Icons.Default.Forward10, contentDescription = "Skip forward 10 seconds")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Speed button
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    var playbackSpeed by remember { mutableStateOf(1.0f) }
                    TextButton(onClick = {
                        playbackSpeed = when (playbackSpeed) {
                            0.5f -> 1.0f
                            1.0f -> 1.5f
                            1.5f -> 2.0f
                            else -> 0.5f
                        }
                        audioPlayerManager.setSpeed(playbackSpeed)
                    }) {
                        Text("${playbackSpeed}x", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return String.format("%02d:%02d", minutes, seconds)
}