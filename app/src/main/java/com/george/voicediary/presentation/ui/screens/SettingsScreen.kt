package com.george.voicediary.presentation.ui.screens

import android.app.TimePickerDialog
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.george.voicediary.presentation.viewmodel.SettingsUiState
import com.george.voicediary.presentation.viewmodel.SettingsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onUpdateReminderTime: (Int, Int) -> Unit,
    isPermissionGranted: Boolean = false,
    onLaunchPermissionRequest: () -> Unit = {}
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Reminders",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Daily Reminder")
                Switch(
                    checked = uiState.reminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isPermissionGranted) {
                            onLaunchPermissionRequest()
                        } else {
                            onToggleReminder(enabled)
                        }
                    }
                )
            }

            if (uiState.reminderEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    onUpdateReminderTime(hour, minute)
                                },
                                uiState.hour,
                                uiState.minute,
                                true
                            ).show()
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Reminder Time")
                    Text(
                        text = String.format("%02d:%02d", uiState.hour, uiState.minute),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    SettingsContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onToggleReminder = { viewModel.toggleReminder(it) },
        onUpdateReminderTime = { h, m -> viewModel.updateReminderTime(h, m) },
        isPermissionGranted = notificationPermissionState?.status?.isGranted ?: true,
        onLaunchPermissionRequest = { notificationPermissionState?.launchPermissionRequest() }
    )

    // Effect to handle permission grant
    if (notificationPermissionState?.status?.isGranted == true && !uiState.reminderEnabled) {
        LaunchedEffect(Unit) {
            viewModel.toggleReminder(true)
        }
    }
}