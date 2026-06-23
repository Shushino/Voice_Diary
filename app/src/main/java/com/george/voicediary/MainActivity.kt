package com.george.voicediary

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.george.voicediary.data.manager.AudioUploadManager
import com.george.voicediary.data.manager.LockManager
import com.george.voicediary.presentation.ui.screens.CreateEditScreen
import com.george.voicediary.presentation.ui.screens.EntryDetailScreen
import com.george.voicediary.presentation.ui.screens.HomeScreen
import com.george.voicediary.presentation.ui.screens.LockScreen
import com.george.voicediary.presentation.ui.screens.SettingsScreen
import com.george.voicediary.presentation.ui.screens.SetupPinScreen
import com.george.voicediary.presentation.ui.theme.VoiceDiaryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import com.george.voicediary.data.SettingsDataStore
import com.george.voicediary.presentation.ui.screens.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var lockManager: LockManager

    @Inject
    lateinit var audioUploadManager: AudioUploadManager

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
            val themeMode by settingsDataStore.themeMode.collectAsState(initial = com.george.voicediary.data.ThemeMode.SYSTEM)
            val fontSize by settingsDataStore.fontSize.collectAsState(initial = com.george.voicediary.data.FontSize.MEDIUM)

            VoiceDiaryTheme(themeMode = themeMode, fontSize = fontSize) {
                val navController = rememberNavController()
                var startDestination by remember { mutableStateOf("loading") }

                LaunchedEffect(Unit) {
                    try {
                        startDestination = if (lockManager.isPinSet()) "lock" else "setup_pin"
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Crash in LaunchedEffect", e)
                        startDestination = "setup_pin"
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
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToTrash = { navController.navigate("trash") },
                                onNavigateToChangePin = { navController.navigate("setup_pin?isChange=true") }
                            )
                        }
                        composable("trash") {
                            TrashScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "setup_pin?isChange={isChange}",
                            arguments = listOf(navArgument("isChange") {
                                type = NavType.BoolType
                                defaultValue = false
                            })
                        ) {
                            SetupPinScreen(
                                onSetupSuccess = {
                                    val isChange = it.arguments?.getBoolean("isChange") ?: false
                                    if (isChange) {
                                        navController.popBackStack()
                                    } else {
                                        navController.navigate("home") {
                                            popUpTo("setup_pin") { inclusive = true }
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
                                onNavigateBack = { navController.popBackStack() },
                                audioUploadManager = audioUploadManager
                            )
                        }
                        composable(
                            route = "detail/{entryId}",
                            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
                        ) {
                            EntryDetailScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToEdit = { entryId -> navController.navigate("create?entryId=$entryId") }
                            )
                        }
                    }

                    val isUnlocked by lockManager.isUnlocked.collectAsStateWithLifecycle()

                    LaunchedEffect(isUnlocked) {
                        if (!isUnlocked) {
                            val currentRoute = navController.currentBackStackEntry?.destination?.route
                            if (currentRoute != null && currentRoute != "lock" && currentRoute != "setup_pin") {
                                val destination = if (lockManager.isPinSet()) "lock" else "setup_pin"
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
