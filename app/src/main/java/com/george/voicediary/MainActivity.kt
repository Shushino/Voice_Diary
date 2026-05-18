package com.george.voicediary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.george.voicediary.presentation.ui.screens.CreateEditScreen
import com.george.voicediary.presentation.ui.screens.HomeScreen
import com.george.voicediary.presentation.ui.theme.VoiceDiaryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoiceDiaryTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(
                            onNavigateToCreate = { navController.navigate("create") },
                            onNavigateToDetail = { entryId -> navController.navigate("create?entryId=$entryId") }
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
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("create") { // Keep simple route for FAB
                        CreateEditScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "detail/{entryId}",
                        arguments = listOf(navArgument("entryId") { type = NavType.LongType })
                    ) {
                        Text("Detail")
                    }
                }
            }
        }
    }
}
