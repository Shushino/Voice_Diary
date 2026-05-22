package com.george.voicediary.presentation.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.george.voicediary.data.manager.AudioUploadManager
import com.george.voicediary.domain.model.Mood
import com.george.voicediary.domain.model.VoiceNote
import com.george.voicediary.presentation.ui.components.MoodChip
import com.george.voicediary.presentation.ui.components.RecordingBottomSheet
import com.george.voicediary.presentation.viewmodel.CreateEditEvent
import com.george.voicediary.presentation.viewmodel.CreateEditViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CreateEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateEditViewModel = hiltViewModel(),
    audioUploadManager: AudioUploadManager
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showRecordingSheet by remember { mutableStateOf(false) }

    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) {
        it?.let { uri ->
            scope.launch {
                state.entryId?.let { entryId ->
                    audioUploadManager.validateAndCopyAudioFile(uri).onSuccess { (filePath, duration) ->
                        viewModel.addVoiceNote(
                            VoiceNote(
                                entryId = entryId,
                                filePath = filePath,
                                durationMs = duration,
                                label = null,
                                transcript = null,
                                createdAt = System.currentTimeMillis(),
                                deletedAt = null
                            )
                        )
                        snackbarHostState.showSnackbar("Audio file added.")
                    }.onFailure { error ->
                        snackbarHostState.showSnackbar(error.message ?: "Failed to add audio file.")
                    }
                } ?: snackbarHostState.showSnackbar("Please save the entry before uploading audio.")
            }
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is CreateEditEvent.Saved -> {
                    onNavigateBack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.entryId == null) "New Entry" else "Edit Entry") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveEntry() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        if (state.entryId == null || state.entryId == -1L) {
                            viewModel.saveEntry(autoSave = true)
                            scope.launch {
                                snackbarHostState.showSnackbar("Saving entry... Try recording in a second.")
                            }
                        } else {
                            showRecordingSheet = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Record Audio")
                }
                
                FloatingActionButton(onClick = { audioLauncher.launch(arrayOf("audio/*")) }) {
                    Icon(Icons.Default.AudioFile, contentDescription = "Upload Audio")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (showRecordingSheet) {
                state.entryId?.let { id ->
                    RecordingBottomSheet(
                        entryId = id,
                        onDismiss = { showRecordingSheet = false },
                        onSaved = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Recording saved.")
                            }
                        }
                    )
                }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("How are you feeling?", fontWeight = FontWeight.Bold)
            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Mood.values()) { mood ->
                    MoodChip(
                        mood = mood,
                        isSelected = state.selectedMood == mood,
                        onClick = { viewModel.setMood(mood) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.body,
                onValueChange = { viewModel.onBodyChange(it) },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
            )
        }
    }
}
