package com.iacobo.wuziqi.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.viewmodel.GameViewModel
import com.iacobo.wuziqi.viewmodel.SettingsViewModel

/** Navigation routes for the app. */
object Routes {
    const val HOME = "home"
    const val GAME = "game"
    const val SETTINGS = "settings"

    // Route with arguments for game configuration
    // boardSize: size of the game board (e.g., 15 for a 15x15 board)
    // winLength: number of pieces in a row to win
    // isComputer: 1 if playing against computer, 0 if playing against human
    const val GAME_WITH_ARGS = "game/{boardSize}/{winLength}/{isComputer}"

    fun gameRoute(boardSize: Int, winLength: Int, isComputer: Boolean): String {
        return "game/$boardSize/$winLength/${if (isComputer) 1 else 0}"
    }
}

/**
 * Main navigation component for the app. Now preserves game state when navigating to settings.
 *
 * @param navController The navigation controller
 * @param isInitialLaunch Flag to suppress startup sound on initial app launch
 */
@Composable
fun AppNavigation(
        navController: NavHostController = rememberNavController(),
        isInitialLaunch: Boolean = true
) {
    // Create shared ViewModel instances that persist across navigation
    val settingsViewModel: SettingsViewModel = viewModel()
    val gameViewModel: GameViewModel = viewModel()

    // Observe user preferences for theme
    val preferences = settingsViewModel.userPreferences.value

    NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            enterTransition = {
                // Reduce default enter transition duration to half (was previously ~300ms)
                fadeIn(animationSpec = tween(150)) +
                        slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(150)
                        )
            },
            exitTransition = {
                // Reduce default exit transition duration to half
                fadeOut(animationSpec = tween(150)) +
                        slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(150)
                        )
            },
            popEnterTransition = {
                // Reduce default pop enter transition duration to half
                fadeIn(animationSpec = tween(150)) +
                        slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(150)
                        )
            },
            popExitTransition = {
                // Reduce default pop exit transition duration to half
                fadeOut(animationSpec = tween(150)) +
                        slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(150)
                        )
            }
    ) {
        // Home/Start Screen
        composable(Routes.HOME) {
            StartScreen(
                    viewModel = gameViewModel,
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToStandardGame = { opponent ->
                        // Standard Wuziqi game (15x15 board with 5-in-a-row win condition)
                        val boardSize = GameState.DEFAULT_BOARD_SIZE
                        val winLength = GameState.DEFAULT_WIN_CONDITION
                        val isComputer = opponent == Opponent.COMPUTER

                        // Navigate to game with proper arguments
                        navController.navigate(Routes.gameRoute(boardSize, winLength, isComputer))
                    },
                    onNavigateToCustomGame = { boardSize, winLength, opponent ->
                        // Fix: pass opponent in navigation
                        val isComputer = opponent == Opponent.COMPUTER
                        navController.navigate(Routes.gameRoute(boardSize, winLength, isComputer))
                    }
            )
        }

        // Game Screen with parameters
        composable(
                route = Routes.GAME_WITH_ARGS,
                arguments =
                        listOf(
                                navArgument("boardSize") { type = NavType.IntType },
                                navArgument("winLength") { type = NavType.IntType },
                                navArgument("isComputer") { type = NavType.IntType }
                        )
        ) { backStackEntry ->
            // Extract parameters from route
            val boardSize =
                    backStackEntry.arguments?.getInt("boardSize") ?: GameState.DEFAULT_BOARD_SIZE
            val winLength =
                    backStackEntry.arguments?.getInt("winLength") ?: GameState.DEFAULT_WIN_CONDITION
            val isComputer = backStackEntry.arguments?.getInt("isComputer") == 1

            // Create a more specific key to prevent recreation of the LaunchedEffect
            // We use a Route path component to ensure it only runs when the route changes
            // not when returning from other screens
            val routePath = backStackEntry.arguments?.getString("boardSize") ?: ""

            // Set up the game with specified parameters only on first navigation
            LaunchedEffect(key1 = "gameSetup-$routePath") {
                // Check if game is already set up with these parameters
                val currentGame = gameViewModel.gameState
                val needsSetup =
                        currentGame.boardSize != boardSize ||
                                currentGame.winCondition != winLength ||
                                currentGame.againstComputer != isComputer ||
                                // Also setup new game if there's a winner (game is complete)
                                gameViewModel.winner != null

                if (needsSetup) {
                    // Setup new game with the specified parameters
                    gameViewModel.setupGame(
                            boardSize = boardSize,
                            winLength = winLength,
                            opponent = if (isComputer) Opponent.COMPUTER else Opponent.HUMAN,
                            skipStartSound = isInitialLaunch
                    )
                }
            }

            GameScreen(
                    viewModel = gameViewModel,
                    onNavigateToSettings = {
                        // Navigate to settings without clearing the game state
                        navController.navigate(Routes.SETTINGS)
                    },
                    onNavigateToHome = {
                        // Navigate back to home screen
                        navController.popBackStack(Routes.HOME, false)
                    },
                    themeMode = preferences.themeMode
            )
        }

        // Simplified game route (for backward compatibility)
        composable(Routes.GAME) {
            // Redirect to standard game against human
            LaunchedEffect(Unit) {
                navController.navigate(
                        Routes.gameRoute(
                                GameState.DEFAULT_BOARD_SIZE,
                                GameState.DEFAULT_WIN_CONDITION,
                                false
                        )
                ) { popUpTo(Routes.GAME) { inclusive = true } }
            }
        }

        // Settings Screen
        composable(Routes.SETTINGS) {
            SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = {
                        // Navigate back to previous screen without clearing state
                        navController.popBackStack()
                    }
            )
        }
    }
}
