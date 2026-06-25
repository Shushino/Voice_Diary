package com.shushino.voicediary.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shushino.voicediary.domain.model.DiaryEntry
import com.shushino.voicediary.presentation.viewmodel.TrashViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val entries by viewModel.trashedEntries.collectAsState()
    var entryToDeletePermanently by remember { mutableStateOf<DiaryEntry?>(null) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (entries.isNotEmpty()) {
                        TextButton(onClick = { showEmptyTrashDialog = true }) {
                            Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🗑️", fontSize = 80.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Trash is empty.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries) { entry ->
                    TrashEntryCard(
                        entry = entry,
                        onRestore = { viewModel.restoreEntry(entry) },
                        onDeletePermanently = { entryToDeletePermanently = entry }
                    )
                }
            }
        }
    }

    if (entryToDeletePermanently != null) {
        AlertDialog(
            onDismissRequest = { entryToDeletePermanently = null },
            title = { Text("Delete Permanently?") },
            text = { Text("This action cannot be undone. Are you sure you want to delete this entry forever?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        entryToDeletePermanently?.let { viewModel.deleteEntryPermanently(it) }
                        entryToDeletePermanently = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDeletePermanently = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash?") },
            text = { Text("All entries in the trash will be permanently deleted. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyTrashDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Empty Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TrashEntryCard(
    entry: DiaryEntry,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title ?: "Untitled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Deleted: ${sdf.format(Date(entry.deletedAt ?: entry.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDeletePermanently) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete permanently", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
