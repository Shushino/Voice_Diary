package com.shushino.voicediary.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.shushino.voicediary.presentation.viewmodel.RecordingViewModel
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RecordingBottomSheet(
    entryId: Long,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    var showPermissionDeniedMessage by remember { mutableStateOf(false) }

    // Observe permission status changes
    LaunchedEffect(audioPermissionState.status) {
        if (audioPermissionState.status.isGranted && !state.isRecording && !state.isFinished) {
            val voiceNotesDir = File(context.filesDir, "voicenotes")
            if (!voiceNotesDir.exists()) voiceNotesDir.mkdirs()
            val fileName = "rec_${System.currentTimeMillis()}.m4a"
            viewModel.start(File(voiceNotesDir, fileName).absolutePath)
        } else if (!audioPermissionState.status.isGranted && !audioPermissionState.status.shouldShowRationale) {
            showPermissionDeniedMessage = true
        }
    }

    // Initial permission request when the sheet opens
    LaunchedEffect(Unit) {
        if (!audioPermissionState.status.isGranted) {
            audioPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                RecordingViewModel.RecordingEvent.Saved -> {
                    onSaved()
                    onDismiss()
                }
                RecordingViewModel.RecordingEvent.MaxDurationReached -> {
                    viewModel.stopAndSave(entryId)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (state.isRecording) {
                viewModel.discard()
            }
            viewModel.reset()
            onDismiss()
        },
        sheetState = sheetState,
        dragHandle = { if (!state.isRecording) BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WaveformVisualizer(
                amplitudes = state.amplitudes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            val minutes = (state.elapsedMs / 1000) / 60
            val seconds = (state.elapsedMs / 1000) % 60
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (state.showWarning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "1 minute remaining",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Discard
                IconButton(
                    onClick = {
                        viewModel.discard()
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Discard", modifier = Modifier.size(32.dp))
                }

                if (showPermissionDeniedMessage) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, contentDescription = "Permission denied", tint = MaterialTheme.colorScheme.error)
                        Text("Mic permission denied.", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }) {
                            Text("Enable in Settings")
                        }
                    }
                } else {
                    // Record/Pause Toggle
                    Button(
                        onClick = {
                            if (state.isPaused) viewModel.resume() else viewModel.pause()
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (state.isPaused) "Resume" else "Pause",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }

                    // Stop and Save
                    IconButton(
                        onClick = { viewModel.stopAndSave(entryId) },
                        enabled = state.isRecording
                    ) {
                        Surface(
                            color = if (state.isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = "Stop and Save",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
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
fun WaveformVisualizer(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier
) {
    val purple = MaterialTheme.colorScheme.primary
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / 100f // 50 bars, with gaps
        val maxAmplitude = 32767f // MediaRecorder max amplitude approx

        amplitudes.forEachIndexed { index, amplitude ->
            val normalizedAmplitude = (amplitude / maxAmplitude).coerceIn(0.05f, 1f)
            val barHeight = height * normalizedAmplitude
            
            val x = (index * width / 50f) + (width / 100f)
            val y = (height - barHeight) / 2

            drawRect(
                color = purple,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
        }
    }
}
