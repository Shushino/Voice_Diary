package com.george.voicediary.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.george.voicediary.domain.model.Mood
import com.george.voicediary.presentation.ui.components.CalendarView
import com.george.voicediary.presentation.ui.components.DiaryEntryCard
import com.george.voicediary.presentation.ui.components.WritingStatsCard
import com.george.voicediary.presentation.viewmodel.HomeViewModel
import com.george.voicediary.presentation.viewmodel.ViewMode

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var isSearching by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { Text("Search entries...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                focusManager.clearFocus()
                            })
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    } else {
                        Text("My Diary")
                    }
                },
                actions = {
                    if (isSearching) {
                        IconButton(onClick = {
                            isSearching = false
                            viewModel.onSearchQueryChange("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search")
                        }
                    } else {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        
                        if (state.searchQuery.isEmpty()) {
                            IconButton(onClick = { viewModel.setViewMode(ViewMode.LIST) }) {
                                Icon(
                                    imageVector = Icons.Default.ViewList,
                                    contentDescription = "List View",
                                    tint = if (state.viewMode == ViewMode.LIST) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { viewModel.setViewMode(ViewMode.CALENDAR) }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Calendar View",
                                    tint = if (state.viewMode == ViewMode.CALENDAR) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.92f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "FABScale"
            )

            FloatingActionButton(
                onClick = onNavigateToCreate,
                interactionSource = interactionSource,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.scale(scale)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AnimatedVisibility(
                visible = !isSearching && state.searchQuery.isEmpty() && state.viewMode == ViewMode.LIST,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                FilterChipRow(
                    selectedMood = state.selectedMoodFilter,
                    onMoodSelected = { viewModel.filterByMood(it) },
                    hasVoiceNoteFilter = state.hasVoiceNoteFilter,
                    onToggleVoiceNoteFilter = { viewModel.toggleHasVoiceNoteFilter() }
                )
            }

            AnimatedContent(
                targetState = if (isSearching || state.searchQuery.isNotEmpty()) ViewMode.LIST else state.viewMode,
                label = "ViewModeAnimation"
            ) { targetMode ->
                when (targetMode) {
                    ViewMode.LIST -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            if (state.searchQuery.isEmpty() && targetMode == ViewMode.LIST) {
                                item {
                                    state.writingStats?.let { stats ->
                                        WritingStatsCard(
                                            stats = stats,
                                            expanded = state.statsExpanded,
                                            onToggle = { viewModel.toggleStatsExpanded() }
                                        )
                                    }
                                }
                            }

                            if (state.entries.isEmpty() && !state.isLoading) {
                                item {
                                    Box(
                                        modifier = Modifier.fillParentMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (state.searchQuery.isNotEmpty()) {
                                            SearchEmptyState(state.searchQuery)
                                        } else {
                                            EmptyState()
                                        }
                                    }
                                }
                            } else {
                                items(state.entries) { entry ->
                                    DiaryEntryCard(
                                        entry = entry,
                                        onClick = { onNavigateToDetail(entry.id) }
                                    )
                                }
                            }
                        }
                    }
                    ViewMode.CALENDAR -> {
                        CalendarView(
                            currentMonth = state.currentMonth,
                            selectedDate = state.selectedDate,
                            monthEntryDates = state.monthEntryDates,
                            entriesOnSelectedDate = state.entriesOnSelectedDate,
                            onDaySelected = { viewModel.selectDate(it) },
                            onNavigateMonth = { viewModel.navigateMonth(it) },
                            onEntryClick = { onNavigateToDetail(it.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipRow(
    selectedMood: Mood?,
    onMoodSelected: (Mood?) -> Unit,
    hasVoiceNoteFilter: Boolean,
    onToggleVoiceNoteFilter: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedMood == null,
                onClick = { onMoodSelected(null) },
                label = { Text("All") }
            )
        }
        items(Mood.entries.filter { it != Mood.NEUTRAL }) { mood ->
            FilterChip(
                selected = selectedMood == mood,
                onClick = { onMoodSelected(mood) },
                label = { Text("${mood.emoji} ${mood.name.lowercase().replaceFirstChar { it.uppercase() }}") }
            )
        }
        item {
            FilterChip(
                selected = hasVoiceNoteFilter,
                onClick = onToggleVoiceNoteFilter,
                label = { Text("Has Voice Note") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
fun EmptyState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "📖",
            fontSize = 80.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nothing here yet. Start writing.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SearchEmptyState(query: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "🔍",
            fontSize = 80.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No results for \"$query\"",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun EmptyStatePreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState()
        }
    }
}
