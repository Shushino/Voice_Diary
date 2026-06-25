package com.shushino.voicediary.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.shushino.voicediary.data.manager.AudioPlayerManager
import com.shushino.voicediary.domain.model.VoiceNote
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VoiceNoteCard(
    voiceNote: VoiceNote,
    audioPlayerManager: AudioPlayerManager,
    isTranscribing: Boolean,
    onDelete: (VoiceNote) -> Unit,
    onTranscribe: (VoiceNote) -> Unit,
    onUpdateLabel: (Long, String) -> Unit
) {
    val context = LocalContext.current
    val fileExists = remember(voiceNote.filePath) { File(voiceNote.filePath).exists() }

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
        if (it == SwipeToDismissBoxValue.EndToStart) {
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
                TextButton(onClick = { 
                    showDeleteConfirmation = false
                    // Reset swipe state
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset swipe state when dialog is dismissed
    LaunchedEffect(showDeleteConfirmation) {
        if (!showDeleteConfirmation && swipeToDismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            swipeToDismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = swipeToDismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateFloatAsState(targetValue = if (swipeToDismissState.targetValue != SwipeToDismissBoxValue.Settled) 1f else 0f, label = "")
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Red.copy(alpha = color))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (isEditingLabel) {
                            OutlinedTextField(
                                value = newLabelText,
                                onValueChange = { newLabelText = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    onUpdateLabel(voiceNote.id, newLabelText)
                                    isEditingLabel = false
                                    keyboardController?.hide()
                                })
                            )
                        } else {
                            Text(
                                text = voiceNote.label ?: "Voice Note",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { isEditingLabel = true }
                            )
                        }
                    }
                    
                    Text(
                        text = formatDuration(currentPlaybackDuration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Seek bar
                Slider(
                    value = currentPlaybackPosition.toFloat(),
                    onValueChange = { audioPlayerManager.seekTo(it.toLong()) },
                    valueRange = 0f..currentPlaybackDuration.toFloat().coerceAtLeast(1f),
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
                                if (fileExists) {
                                    audioPlayerManager.play(voiceNote.filePath)
                                }
                            }
                        },
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(
                            if (currentPlayingFilePath == voiceNote.filePath && isPlaying) 
                                Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (currentPlayingFilePath == voiceNote.filePath && isPlaying) "Pause" else "Play"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (currentPlayingFilePath == voiceNote.filePath && isPlaying) "Pause" else "Play")
                    }

                    // Skip forward 10s
                    IconButton(onClick = { audioPlayerManager.seekTo(currentPlaybackPosition + 10000L) }) {
                        Icon(Icons.Default.Forward10, contentDescription = "Skip forward 10 seconds")
                    }
                }

                if (!voiceNote.transcript.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ClosedCaption,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Transcript",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = voiceNote.transcript,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Bottom actions: Share & Transcribe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (fileExists) {
                        IconButton(onClick = {
                            val file = File(voiceNote.filePath)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "audio/mp4"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                putExtra(android.content.Intent.EXTRA_SUBJECT, voiceNote.label ?: "Voice Note")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Audio Recording"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share recording")
                        }

                        if (voiceNote.transcript.isNullOrBlank()) {
                            if (isTranscribing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(onClick = { onTranscribe(voiceNote) }) {
                                    Icon(
                                        Icons.Default.ClosedCaption,
                                        contentDescription = "Transcribe"
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
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

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
