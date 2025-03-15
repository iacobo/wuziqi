package com.iacobo.wuziqi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.R
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.data.ThemeMode
import com.iacobo.wuziqi.ui.theme.BoardDarkColor
import com.iacobo.wuziqi.ui.theme.BoardLightColor
import com.iacobo.wuziqi.ui.theme.GridDarkColor
import com.iacobo.wuziqi.ui.theme.GridLightColor
import com.iacobo.wuziqi.viewmodel.GameViewModel
import com.iacobo.wuziqi.viewmodel.Position

/**
 * Main game screen composable.
 * Displays the game board, status, and controls.
 * Now supports custom board sizes and computer opponent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToHome: () -> Unit,
    themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val gameState = viewModel.gameState
    val winner = viewModel.winner
    val lastPlacedPosition = viewModel.lastPlacedPosition
    val moveHistory = viewModel.moveHistory
    val isLoading = viewModel.isLoading
    val boardSize = gameState.boardSize

    // Determine if we're in dark theme based on the theme mode
    val isDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar with home button and title
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateToHome) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = stringResource(R.string.home)
                    )
                }
            },
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )
        
        // Space between app bar and player turn indicator (for vertical centering)
        Spacer(modifier = Modifier.weight(0.5f))

        // Player turn indicator with loading indicator when computer is thinking
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.computer_thinking),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                PlayerTurnIndicator(gameState.currentPlayer)
            }
        }

        // Winner dialog
        if (winner != null) {
            WinnerDialog(
                winner = winner,
                onRematch = { viewModel.resetGame() },
                onDismiss = { viewModel.dismissWinnerDialog() }
            )
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Game Board (centered with weight)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(8f),
            contentAlignment = Alignment.Center
        ) {
            GameBoard(
                gameState = gameState,
                lastPlacedPosition = lastPlacedPosition,
                isDarkTheme = isDarkTheme,
                isGameFrozen = winner != null || isLoading,
                onTileClick = { row, col ->
                    viewModel.placeTile(row, col)
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Control buttons
        GameControls(
            canUndo = moveHistory.isNotEmpty() && winner == null && !isLoading,
            onUndo = { viewModel.undoMove() },
            onReset = { viewModel.resetGame() },
            onNavigateToSettings = onNavigateToSettings,
            showSettingsButton = false  // we already have it in the app bar
        )
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Displays the current player's turn.
 */
@Composable
private fun PlayerTurnIndicator(currentPlayer: Int) {
    val playerName = if (currentPlayer == GameState.PLAYER_ONE)
        stringResource(R.string.player_black)
    else
        stringResource(R.string.player_white)

    val playerColor = if (currentPlayer == GameState.PLAYER_ONE)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.secondary

    Text(
        text = stringResource(R.string.player_turn_format, playerName),
        style = MaterialTheme.typography.titleMedium,
        color = playerColor
    )
}

/**
 * Dialog shown when a player wins.
 */
@Composable
private fun WinnerDialog(
    winner: Int,
    onRematch: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.game_over)) },
        text = {
            Text(
                stringResource(
                    R.string.winner_format,
                    if (winner == GameState.PLAYER_ONE)
                        stringResource(R.string.player_black)
                    else
                        stringResource(R.string.player_white)
                )
            )
        },
        confirmButton = {
            Button(onClick = onRematch) {
                Text(stringResource(R.string.rematch))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

/**
 * Game control buttons (Undo, Reset, Settings).
 */
@Composable
private fun GameControls(
    canUndo: Boolean,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onNavigateToSettings: () -> Unit,
    showSettingsButton: Boolean = true
) {
    val controlButtons = mutableListOf<@Composable () -> Unit>()
    
    // Undo Button
    controlButtons.add {
        ControlButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            label = stringResource(R.string.undo),
            onClick = onUndo,
            enabled = canUndo
        )
    }
    
    // Reset Button
    controlButtons.add {
        ControlButton(
            icon = Icons.Filled.Refresh,
            label = stringResource(R.string.reset),
            onClick = onReset,
            enabled = true
        )
    }
    
    // Settings Button - optional
    if (showSettingsButton) {
        controlButtons.add {
            ControlButton(
                icon = Icons.Default.Settings,
                label = stringResource(R.string.settings),
                onClick = onNavigateToSettings,
                enabled = true
            )
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        controlButtons.forEach { it() }
    }
}

/**
 * Reusable control button with icon and label.
 */
@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * The game board composable.
 * Displays the grid and pieces.
 * Now supports variable board sizes.
 */
@Composable
fun GameBoard(
    gameState: GameState,
    lastPlacedPosition: Position?,
    isDarkTheme: Boolean,
    isGameFrozen: Boolean,
    onTileClick: (Int, Int) -> Unit
) {
    // Use theme-appropriate colors for grid lines and board background
    val gridLineColor = if (isDarkTheme) GridDarkColor else GridLightColor
    val boardColor = if (isDarkTheme) BoardDarkColor else BoardLightColor
    val gridLineWidth = 1.dp
    val boardSize = gameState.boardSize
    
    // Adjust piece size based on board size
    val pieceSize = when {
        boardSize <= 10 -> 32.dp
        boardSize <= 13 -> 28.dp
        boardSize <= 15 -> 24.dp
        boardSize <= 17 -> 20.dp
        else -> 18.dp
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(8.dp)
            .background(boardColor)
    ) {
        // Draw gridlines
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(12.dp)
        ) {
            // Horizontal grid lines
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until boardSize) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridLineWidth)
                            .background(gridLineColor)
                    )

                    if (i < boardSize - 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Vertical grid lines
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until boardSize) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(gridLineWidth)
                            .background(gridLineColor)
                    )

                    if (i < boardSize - 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Tiles and pieces
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0 until boardSize) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    for (col in 0 until boardSize) {
                        Tile(
                            state = gameState.board[row][col],
                            isLastPlaced = lastPlacedPosition?.row == row && lastPlacedPosition?.col == col,
                            pieceSize = pieceSize,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (!isGameFrozen) {
                                    onTileClick(row, col)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Represents a single tile on the game board.
 */
@Composable
fun Tile(
    state: Int,
    isLastPlaced: Boolean,
    pieceSize: androidx.compose.ui.unit.Dp = 24.dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (state != GameState.EMPTY) {
            Box(
                modifier = Modifier
                    .size(pieceSize)
                    .clip(CircleShape)
                    .background(
                        when (state) {
                            GameState.PLAYER_ONE -> Color.Black
                            else -> Color.White
                        }
                    )
                    .border(
                        width = if (isLastPlaced) 2.dp else 0.dp,
                        color = if (isLastPlaced) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                        shape = CircleShape
                    )
            )
        }
    }
}