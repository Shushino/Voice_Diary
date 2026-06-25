package com.shushino.voicediary.presentation.ui.screens

import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.shushino.voicediary.presentation.viewmodel.SettingsUiState
import com.shushino.voicediary.presentation.viewmodel.SettingsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import com.shushino.voicediary.data.ColorPalette
import com.shushino.voicediary.data.FontSize
import com.shushino.voicediary.data.ThemeMode
import com.shushino.voicediary.BuildConfig
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyRow
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    isPinSet: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToChangePin: () -> Unit,
    onRemovePin: () -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onUpdateReminderTime: (Int, Int) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onFontSizeSelected: (FontSize) -> Unit,
    onPaletteSelected: (ColorPalette) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onExportEntries: (Boolean, Boolean) -> Unit,
    onImportBackup: () -> Unit,
    isPermissionGranted: Boolean = false,
    onLaunchPermissionRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var includeAudio by remember { mutableStateOf(true) }
    var includeImages by remember { mutableStateOf(true) }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Options") },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { includeAudio = !includeAudio }
                    ) {
                        Checkbox(checked = includeAudio, onCheckedChange = { includeAudio = it })
                        Text("Include audio recordings")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { includeImages = !includeImages }
                    ) {
                        Checkbox(checked = includeImages, onCheckedChange = { includeImages = it })
                        Text("Include images")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onExportEntries(includeAudio, includeImages)
                    showExportDialog = false
                }) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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

            ColorPaletteSelector(
                selectedPalette = uiState.colorPalette,
                onPaletteSelected = onPaletteSelected
            )

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
            if (isPinSet) {
                ListItem(
                    headlineContent = { Text("Remove PIN") },
                    trailingContent = {
                        OutlinedButton(onClick = onRemovePin) {
                            Text("Remove")
                        }
                    }
                )
            }
            ListItem(
                headlineContent = { Text("Biometric Unlock") },
                trailingContent = {
                    Switch(
                        checked = uiState.biometricEnabled,
                        onCheckedChange = onToggleBiometric,
                        enabled = uiState.pinSet
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
                            text = String.format(Locale.getDefault(), "%02d:%02d", uiState.hour, uiState.minute),
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
                supportingContent = { Text("Save as .vdiary backup in Downloads") },
                trailingContent = {
                    OutlinedButton(onClick = { showExportDialog = true }) {
                        Text("Export")
                    }
                }
            )
            ListItem(
                headlineContent = { Text("Import backup") },
                supportingContent = { Text("Restore from .vdiary file") },
                trailingContent = {
                    OutlinedButton(onClick = onImportBackup) {
                        Text("Import")
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ABOUT SECTION
            SettingsSectionHeader("About")
            ListItem(
                headlineContent = { Text("Version") },
                trailingContent = { Text(BuildConfig.VERSION_NAME) }
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
                        "• Accompanist Permissions\n" +
                        "• Coil\n" +
                        "• Biometric\n"
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
private fun ColorPaletteSelector(
    selectedPalette: ColorPalette,
    onPaletteSelected: (ColorPalette) -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))
    Text("Colour Palette", style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ColorPalette.entries) { palette ->
            FilterChip(
                selected = selectedPalette == palette,
                onClick = { onPaletteSelected(palette) },
                label = { Text(palette.name.lowercase().replaceFirstChar { it.uppercase() }) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(getPalettePreviewColor(palette), CircleShape)
                    )
                }
            )
        }
    }
}

@Composable
fun getPalettePreviewColor(palette: ColorPalette): Color {
    return when (palette) {
        ColorPalette.DEFAULT -> MaterialTheme.colorScheme.primary
        ColorPalette.OCEAN -> Color(0xFF0077B6)
        ColorPalette.FOREST -> Color(0xFF2D6A4F)
        ColorPalette.SUNSET -> Color(0xFFE76F51)
        ColorPalette.ROSE -> Color(0xFFC9184A)
        ColorPalette.SLATE -> Color(0xFF495057)
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
    val isPinSet by viewModel.isPinSet.collectAsState()
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

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            SettingsContent(
                uiState = uiState,
                isPinSet = isPinSet,
                onNavigateBack = onNavigateBack,
                onNavigateToTrash = onNavigateToTrash,
                onNavigateToChangePin = onNavigateToChangePin,
                onRemovePin = { viewModel.removePin() },
                onToggleReminder = { viewModel.toggleReminder(it) },
                onUpdateReminderTime = { h, m -> viewModel.updateReminderTime(h, m) },
                onThemeSelected = { viewModel.setThemeMode(it) },
                onFontSizeSelected = { viewModel.setFontSize(it) },
                onPaletteSelected = { viewModel.setColorPalette(it) },
                onToggleBiometric = { viewModel.setBiometricEnabled(it) },
                onExportEntries = { audio, images -> viewModel.exportAllEntries(audio, images) },
                onImportBackup = { importLauncher.launch(arrayOf("application/octet-stream", "application/zip")) },
                isPermissionGranted = notificationPermissionState?.status?.isGranted ?: true,
                onLaunchPermissionRequest = { notificationPermissionState?.launchPermissionRequest() }
            )
        }
    }

}
