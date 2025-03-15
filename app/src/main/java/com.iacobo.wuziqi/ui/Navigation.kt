package com.iacobo.wuziqi.ui

import androidx.compose.runtime.Composable
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
    NavHost(
        navController = navController,
        startDestination = Routes.GAME
    ) {
        composable(Routes.GAME) {
            GameScreenWrapper(
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }
        
        composable(Routes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Wrapper for the GameScreen that adds a settings button.
 */
@Composable
fun GameScreenWrapper(
    onNavigateToSettings: () -> Unit
) {
    val gameViewModel: GameViewModel = viewModel()
    GameScreen(
        viewModel = gameViewModel,
        onNavigateToSettings = onNavigateToSettings
    )
}
