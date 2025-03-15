package com.iacobo.wuziqi.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iacobo.wuziqi.viewmodel.GameViewModel
import com.iacobo.wuziqi.viewmodel.SettingsViewModel

/**
 * Navigation routes for the app.
 */
object Routes {
    const val GAME = "game"
    const val SETTINGS = "settings"
}

/**
 * Main navigation component for the app.
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    // Create a shared ViewModel instance that persists across navigation
    val settingsViewModel: SettingsViewModel = viewModel()
    val gameViewModel: GameViewModel = viewModel()
    
    // Observe user preferences for theme
    val preferences by settingsViewModel.userPreferences.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.GAME
    ) {
        composable(Routes.GAME) {
            GameScreen(
                viewModel = gameViewModel,
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                themeMode = preferences.themeMode
            )
        }
        
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}