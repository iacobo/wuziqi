package com.iacobo.wuziqi.ui

import android.content.res.Configuration
import android.view.Surface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.iacobo.wuziqi.R
import com.iacobo.wuziqi.data.GameType
import com.iacobo.wuziqi.data.ThemeMode
import com.iacobo.wuziqi.ui.board.GameBoardRenderer
import com.iacobo.wuziqi.ui.components.GameControls
import com.iacobo.wuziqi.ui.components.GameStatusBar
import com.iacobo.wuziqi.ui.layout.GameLayoutScaffold
import com.iacobo.wuziqi.viewmodel.GameViewModel

/**
 * Main game screen that displays the game board and controls. This is a refactored version with
 * better separation of concerns.
 */
@Composable
fun GameScreen(
        viewModel: GameViewModel,
        onNavigateToSettings: () -> Unit,
        onNavigateToHome: () -> Unit,
        themeMode: ThemeMode = ThemeMode.SYSTEM
) {
        // Handle reset confirmation dialog
        var showResetConfirmation by remember { mutableStateOf(false) }

        // Get the device orientation
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Get device rotation from the window manager
        val context = LocalContext.current
        val windowManager = context.getSystemService(android.view.WindowManager::class.java)
        val rotation = windowManager.defaultDisplay.rotation

        // Extract game state
        val gameState = viewModel.gameState
        val gameType = GameType.fromGameState(gameState)
        val winner = viewModel.winner
        val lastPlacedPosition = viewModel.lastPlacedPosition
        val moveHistory = viewModel.moveHistory
        val isLoading = viewModel.isLoading

        // Get the appropriate game title
        val gameTitle = stringResource(id = gameType.titleResId)

        // Determine the appropriate theme mode (light/dark)
        val isDarkTheme =
                when (themeMode) {
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK -> true
                        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                }

        // Use the unified game layout scaffold
        GameLayoutScaffold(
                title = gameTitle,
                isLandscape = isLandscape,
                rotation = rotation,
                statusContent = {
                        GameStatusBar(
                                gameState = gameState,
                                gameType = gameType,
                                winner = winner,
                                isLoading = isLoading
                        )
                },
                boardContent = {
                        GameBoardRenderer(
                                gameType = gameType,
                                gameState = gameState,
                                lastPlacedPosition = lastPlacedPosition,
                                isDarkTheme = isDarkTheme,
                                isGameFrozen = winner != null || isLoading,
                                onMoveSelected = { row, col ->
                                        if (row < 0 && gameType == GameType.Connect4) {
                                                // Special case for Connect4 which only needs column
                                                // input
                                                viewModel.placeConnect4Tile(col)
                                        } else {
                                                viewModel.placeTile(row, col)
                                        }
                                }
                        )
                },
                controlsContent = {
                        GameControls(
                                isLandscape = isLandscape,
                                isAppBarOnLeft = rotation == Surface.ROTATION_270,
                                onHome = onNavigateToHome,
                                onUndo = { viewModel.undoMove() },
                                onReset = {
                                        if (winner == null && moveHistory.isNotEmpty()) {
                                                showResetConfirmation = true
                                        } else {
                                                viewModel.resetGame()
                                        }
                                },
                                onSettings = onNavigateToSettings,
                                canUndo = moveHistory.isNotEmpty(),
                                isLoading = isLoading
                        )
                }
        )

        // Show confirmation dialog if needed
        if (showResetConfirmation) {
                ResetConfirmationDialog(
                        onConfirm = {
                                viewModel.resetGame()
                                showResetConfirmation = false
                        },
                        onDismiss = { showResetConfirmation = false }
                )
        }
}

/** Dialog to confirm game reset when the game is still in progress. */
@Composable
fun ResetConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.confirm_reset_title)) },
                text = { Text(stringResource(R.string.confirm_reset_message)) },
                confirmButton = {
                        Button(onClick = onConfirm) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
        )
}
