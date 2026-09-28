package com.shushino.voicediary

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shushino.voicediary.data.manager.AudioImportManager
import com.shushino.voicediary.data.manager.LockManager
import com.shushino.voicediary.presentation.ui.screens.CreateEditScreen
import com.shushino.voicediary.presentation.ui.screens.EntryDetailScreen
import com.shushino.voicediary.presentation.ui.screens.HomeScreen
import com.shushino.voicediary.presentation.ui.screens.LockScreen
import com.shushino.voicediary.presentation.ui.screens.SettingsScreen
import com.shushino.voicediary.presentation.ui.screens.SetupPinScreen
import com.shushino.voicediary.presentation.ui.theme.VoiceDiaryTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import com.shushino.voicediary.data.SettingsDataStore
import com.shushino.voicediary.presentation.ui.screens.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var lockManager: LockManager

    @Inject
    lateinit var audioImportManager: AudioImportManager

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            lifecycle.addObserver(AppLifecycleObserver(lockManager))
        } catch (e: Exception) {
            Log.e("MainActivity", "Crash during AppLifecycleObserver registration", e)
        }

        setContent {
            val themeMode by settingsDataStore.themeMode.collectAsState(initial = com.shushino.voicediary.data.ThemeMode.SYSTEM)
            val fontSize by settingsDataStore.fontSize.collectAsState(initial = com.shushino.voicediary.data.FontSize.MEDIUM)
            val colorPalette by settingsDataStore.colorPalette.collectAsState(initial = com.shushino.voicediary.data.ColorPalette.DEFAULT)

            VoiceDiaryTheme(themeMode = themeMode, fontSize = fontSize, colorPalette = colorPalette) {
                val navController = rememberNavController()
                var startDestination by remember { mutableStateOf("loading") }

                // A fast second back-tap during the 300ms screen transition used to pop
                // one destination too many, leaving an empty back stack = blank white
                // screen until the app was restarted. Ignore back taps while the current
                // screen is still animating in/out (its lifecycle is below RESUMED).
                val popBackStackSafely: () -> Unit = {
                    val entry = navController.currentBackStackEntry
                    if (entry == null || entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        navController.popBackStack()
                    }
                }

                LaunchedEffect(Unit) {
                    try {
                        startDestination = if (lockManager.isPinSet()) "lock" else "home"
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Crash in LaunchedEffect", e)
                        startDestination = "home"
                    }
                }

                if (startDestination != "loading") {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        enterTransition = { fadeIn(animationSpec = tween(300)) },
                        exitTransition = { fadeOut(animationSpec = tween(300)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                        popExitTransition = { fadeOut(animationSpec = tween(300)) }
                    ) {
                        composable("lock") {
                            LockScreen(
                                onUnlockSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("lock") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                onNavigateToCreate = { navController.navigate("create") },
                                onNavigateToDetail = { entryId -> navController.navigate("detail/$entryId") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = popBackStackSafely,
                                onNavigateToTrash = { navController.navigate("trash") },
                                onNavigateToChangePin = { navController.navigate("setup_pin?isChange=true") },
                                onNavigateToRemovePin = { navController.navigate("setup_pin?remove=true") }
                            )
                        }
                        composable("trash") {
                            TrashScreen(
                                onNavigateBack = popBackStackSafely
                            )
                        }
                        composable(
                            route = "setup_pin?isChange={isChange}&remove={remove}",
                            arguments = listOf(
                                navArgument("isChange") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                },
                                navArgument("remove") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { backStackEntry ->
                            SetupPinScreen(
                                onSetupSuccess = {
                                    val isChange = backStackEntry.arguments?.getBoolean("isChange") ?: false
                                    val isRemove = backStackEntry.arguments?.getBoolean("remove") ?: false
                                    if (isChange || isRemove) {
                                        navController.popBackStack()
                                    } else {
                                        navController.navigate("home") {
                                            popUpTo("setup_pin?isChange={isChange}&remove={remove}") {
                                                inclusive = true
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        composable(
                            route = "create?entryId={entryId}",
                            arguments = listOf(navArgument("entryId") {
                                type = NavType.LongType
                                defaultValue = -1L
                            })
                        ) {
                            CreateEditScreen(
                                onNavigateBack = popBackStackSafely,
                                audioImportManager = audioImportManager
                            )
                        }
                        composable(
                            route = "detail/{entryId}",
                            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
                        ) {
                            EntryDetailScreen(
                                onNavigateBack = popBackStackSafely,
                                onNavigateToEdit = { entryId -> navController.navigate("create?entryId=$entryId") }
                            )
                        }
                    }

                    val isUnlocked by lockManager.isUnlocked.collectAsStateWithLifecycle()
                    val isPinSet by lockManager.isPinSetFlow.collectAsStateWithLifecycle(initialValue = false)

                    // Hide diary content from screenshots & the recents/app-switcher preview
                    // whenever the PIN gate is active.
                    DisposableEffect(isUnlocked, isPinSet) {
                        val secure = isPinSet && !isUnlocked
                        if (secure) {
                            window.setFlags(
                                WindowManager.LayoutParams.FLAG_SECURE,
                                WindowManager.LayoutParams.FLAG_SECURE
                            )
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                        onDispose { }
                    }

                    LaunchedEffect(isUnlocked) {
                        if (!isUnlocked) {
                            val currentRoute = navController.currentBackStackEntry?.destination?.route
                            if (currentRoute != null && currentRoute != "lock" && currentRoute != "setup_pin") {
                                val destination = if (lockManager.isPinSet()) "lock" else "home"
                                navController.navigate(destination) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class AppLifecycleObserver(
    private val lockManager: LockManager
) : DefaultLifecycleObserver {
    private var lastBackgroundTime: Long = 0L
    private val BACKGROUND_THRESHOLD = 30 * 1000L // 30 seconds

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        lastBackgroundTime = System.currentTimeMillis()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (System.currentTimeMillis() - lastBackgroundTime > BACKGROUND_THRESHOLD) {
            // In a real app, you\'d trigger lock screen here, for now, we just reset unlocked state
            lockManager.setUnlocked(false)
        }
    }
}
