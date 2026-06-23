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

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.george.voicediary.data.FontSize
import com.george.voicediary.data.ThemeMode
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToChangePin: () -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onUpdateReminderTime: (Int, Int) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onFontSizeSelected: (FontSize) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onExportEntries: () -> Unit,
    isPermissionGranted: Boolean = false,
    onLaunchPermissionRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    var showLicensesDialog by remember { mutableStateOf(false) }

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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // APPEARANCE SECTION
            SettingsSectionHeader("Appearance")
            
            Text("Theme", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = uiState.themeMode == mode,
                        onClick = { onThemeSelected(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size)
                    ) {
                        Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Font Size", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                FontSize.entries.forEachIndexed { index, size ->
                    SegmentedButton(
                        selected = uiState.fontSize == size,
                        onClick = { onFontSizeSelected(size) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = FontSize.entries.size)
                    ) {
                        Text(size.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // LOCK SECTION
            SettingsSectionHeader("Security")
            ListItem(
                headlineContent = { Text("Change PIN") },
                trailingContent = {
                    OutlinedButton(onClick = onNavigateToChangePin) {
                        Text("Change")
                    }
                }
            )
            ListItem(
                headlineContent = { Text("Biometric Unlock") },
                trailingContent = {
                    Switch(
                        checked = uiState.biometricEnabled,
                        onCheckedChange = onToggleBiometric
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // REMINDERS SECTION
            SettingsSectionHeader("Reminders")
            ListItem(
                headlineContent = { Text("Daily Reminder") },
                trailingContent = {
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
            )

            if (uiState.reminderEnabled) {
                ListItem(
                    modifier = Modifier.clickable {
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> onUpdateReminderTime(hour, minute) },
                            uiState.hour,
                            uiState.minute,
                            true
                        ).show()
                    },
                    headlineContent = { Text("Reminder Time") },
                    trailingContent = {
                        Text(
                            text = String.format("%02d:%02d", uiState.hour, uiState.minute),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DATA SECTION
            SettingsSectionHeader("Data")
            ListItem(
                headlineContent = { Text("Trash") },
                supportingContent = { Text("View and restore deleted entries") },
                trailingContent = {
                    OutlinedButton(onClick = onNavigateToTrash) {
                        Text("View")
                    }
                }
            )
            ListItem(
                headlineContent = { Text("Export all entries") },
                supportingContent = { Text("Save as Markdown ZIP in Downloads") },
                trailingContent = {
                    OutlinedButton(onClick = onExportEntries) {
                        Text("Export")
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ABOUT SECTION
            SettingsSectionHeader("About")
            ListItem(
                headlineContent = { Text("Version") },
                trailingContent = { Text("1.0") } // Hardcoded for now if BuildConfig fails
            )
            ListItem(
                headlineContent = { Text("Open source licenses") },
                modifier = Modifier.clickable { showLicensesDialog = true }
            )
        }
    }

    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open Source Licenses") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "This app uses the following open source libraries:\n\n" +
                        "• Jetpack Compose\n" +
                        "• Hilt & Dagger\n" +
                        "• Room Persistence Library\n" +
                        "• SQLCipher (Zetetic)\n" +
                        "• Media3 ExoPlayer\n" +
                        "• Kotlin Coroutines & Flow\n" +
                        "• DataStore\n" +
                        "• WorkManager\n" +
                        "• Google Gson\n" +
                        "• Accompanist Permissions\n"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToChangePin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.exportStatus.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            SettingsContent(
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onNavigateToTrash = onNavigateToTrash,
                onNavigateToChangePin = onNavigateToChangePin,
                onToggleReminder = { viewModel.toggleReminder(it) },
                onUpdateReminderTime = { h, m -> viewModel.updateReminderTime(h, m) },
                onThemeSelected = { viewModel.setThemeMode(it) },
                onFontSizeSelected = { viewModel.setFontSize(it) },
                onToggleBiometric = { viewModel.setBiometricEnabled(it) },
                onExportEntries = { viewModel.exportAllEntries() },
                isPermissionGranted = notificationPermissionState?.status?.isGranted ?: true,
                onLaunchPermissionRequest = { notificationPermissionState?.launchPermissionRequest() }
            )
        }
    }

}
