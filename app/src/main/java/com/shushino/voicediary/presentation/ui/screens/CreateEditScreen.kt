package com.shushino.voicediary.presentation.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.shushino.voicediary.data.manager.AudioUploadManager
import com.shushino.voicediary.domain.model.Mood
import com.shushino.voicediary.domain.model.VoiceNote
import com.shushino.voicediary.presentation.ui.components.MoodChip
import com.shushino.voicediary.presentation.ui.components.RecordingBottomSheet
import com.shushino.voicediary.presentation.viewmodel.CreateEditEvent
import com.shushino.voicediary.presentation.viewmodel.CreateEditViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
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
    val photos by viewModel.photosForEntry.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showRecordingSheet by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            if (state.entryId == null || state.entryId == -1L) {
                viewModel.saveEntry(autoSave = true)
                scope.launch {
                    snackbarHostState.showSnackbar("Saving entry... Try adding the image again.")
                }
            } else {
                state.entryId?.let { entryId ->
                    viewModel.copyImageToInternal(context, it)?.let { path ->
                        viewModel.addPhoto(entryId, path)
                    }
                }
            }
        }
    }

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

    LaunchedEffect(state.showDraftRestoredBanner) {
        if (state.showDraftRestoredBanner) {
            snackbarHostState.showSnackbar("Draft restored — tap Save to keep or Back to discard")
            viewModel.dismissBanner()
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

                FloatingActionButton(
                    onClick = {
                        imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Add Image")
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

            if (photos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Photos", fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(photos) { photo ->
                        Box {
                            AsyncImage(
                                model = photo.filePath,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.deletePhoto(photo.id) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Delete photo",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
