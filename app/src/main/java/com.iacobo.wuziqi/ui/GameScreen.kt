package com.iacobo.wuziqi.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.R
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.data.ThemeMode
import com.iacobo.wuziqi.viewmodel.DRAW
import com.iacobo.wuziqi.viewmodel.GameViewModel
import com.iacobo.wuziqi.viewmodel.Position

/**
 * Main game screen composable.
 * Displays the game board, status, and controls.
 * Supports standard game and easter eggs.
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
    val winLength = gameState.winCondition
    
    // Determine game type for easter eggs
    val isXandO = boardSize == 3 && winLength == 3
    val isConnect4 = boardSize == 7 && winLength == 4

    // Determine app title based on easter egg mode
    val appTitle = when {
        isXandO -> "X's & O's"
        isConnect4 -> "Connect 4"
        else -> stringResource(R.string.app_name)
    }

    // Determine if we're in dark theme based on the theme mode
    val isDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = appTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    // Home button
                    IconButton(onClick = onNavigateToHome) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = stringResource(R.string.home)
                        )
                    }
                    
                    // Undo button (disabled if no moves or game frozen)
                    IconButton(
                        onClick = { viewModel.undoMove() },
                        enabled = moveHistory.isNotEmpty() && !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.undo)
                        )
                    }
                    
                    // Reset button
                    IconButton(onClick = { viewModel.resetGame() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.reset)
                        )
                    }
                    
                    Spacer(Modifier.weight(1f))
                    
                    // Settings button (right aligned)
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fixed height container for player indicator to prevent layout shifts
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                    }
                } else if (winner == DRAW) {
                    // Show draw message
                    Text(
                        text = stringResource(R.string.draw),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (winner != null) {
                    // Show winner with fireworks emoji
                    val winnerText = when {
                        isXandO -> if (winner == GameState.PLAYER_ONE) "X Wins! 🎆" else "O Wins! 🎆"
                        isConnect4 -> if (winner == GameState.PLAYER_ONE) "Red Wins! 🎆" else "Yellow Wins! 🎆"
                        else -> {
                            // Handle computer opponent case
                            if (gameState.againstComputer) {
                                if (winner == GameState.PLAYER_ONE) 
                                    stringResource(R.string.you_won)
                                else 
                                    stringResource(R.string.computer_won)
                            } else {
                                stringResource(
                                    R.string.winner_format,
                                    if (winner == GameState.PLAYER_ONE) 
                                        stringResource(R.string.player_black)
                                    else 
                                        stringResource(R.string.player_white)
                                )
                            }
                        }
                    }
                    
                    val winnerColor = when {
                        isXandO -> if (winner == GameState.PLAYER_ONE) Color.Black else Color.White
                        isConnect4 -> if (winner == GameState.PLAYER_ONE) Color.Red else Color(0xFFFFD700) // Gold
                        else -> if (winner == GameState.PLAYER_ONE) 
                            Color.Black
                        else 
                            Color.White
                    }
                    
                    // Ensure good contrast in dark theme
                    val displayColor = if (isDarkTheme && (winner == GameState.PLAYER_TWO && !isConnect4 || 
                                                        winner == GameState.PLAYER_ONE && !isConnect4 && !isXandO)) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        winnerColor
                    }
                    
                    Text(
                        text = winnerText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = displayColor
                    )
                } else {
                    // Show current player
                    val playerText = when {
                        isXandO -> if (gameState.currentPlayer == GameState.PLAYER_ONE) "X's Turn" else "O's Turn"
                        isConnect4 -> if (gameState.currentPlayer == GameState.PLAYER_ONE) "Red's Turn" else "Yellow's Turn"
                        else -> {
                            // Handle computer opponent case
                            if (gameState.againstComputer) {
                                if (gameState.currentPlayer == GameState.PLAYER_ONE) 
                                    stringResource(R.string.your_turn)
                                else 
                                    stringResource(R.string.computer_thinking)
                            } else {
                                stringResource(
                                    R.string.player_turn_format,
                                    if (gameState.currentPlayer == GameState.PLAYER_ONE) 
                                        stringResource(R.string.player_black)
                                    else 
                                        stringResource(R.string.player_white)
                                )
                            }
                        }
                    }
                    
                    val playerColor = when {
                        isXandO -> if (gameState.currentPlayer == GameState.PLAYER_ONE) Color.Black else Color.White
                        isConnect4 -> if (gameState.currentPlayer == GameState.PLAYER_ONE) Color.Red else Color(0xFFFFD700) // Gold
                        else -> if (gameState.currentPlayer == GameState.PLAYER_ONE) 
                            Color.Black
                        else 
                            Color.White
                    }
                    
                    // Ensure good contrast in dark theme
                    val displayColor = if (isDarkTheme && (gameState.currentPlayer == GameState.PLAYER_TWO && !isConnect4 || 
                                                          gameState.currentPlayer == GameState.PLAYER_ONE && !isConnect4 && !isXandO)) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        playerColor
                    }
                    
                    Text(
                        text = playerText,
                        style = MaterialTheme.typography.titleMedium,
                        color = displayColor
                    )
                }
            }

            // Game Board - fills remaining space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isXandO -> TicTacToeBoard(
                        gameState = gameState,
                        lastPlacedPosition = lastPlacedPosition,
                        isDarkTheme = isDarkTheme,
                        isGameFrozen = winner != null || isLoading,
                        onTileClick = { row, col ->
                            viewModel.placeTile(row, col)
                        }
                    )
                    isConnect4 -> Connect4Board(
                        gameState = gameState,
                        lastPlacedPosition = lastPlacedPosition,
                        isDarkTheme = isDarkTheme,
                        isGameFrozen = winner != null || isLoading,
                        onColumnClick = { col ->
                            viewModel.placeConnect4Tile(col)
                        }
                    )
                    else -> GameBoard(
                        gameState = gameState,
                        lastPlacedPosition = lastPlacedPosition,
                        isDarkTheme = isDarkTheme,
                        isGameFrozen = winner != null || isLoading,
                        onTileClick = { row, col ->
                            viewModel.placeTile(row, col)
                        }
                    )
                }
            }
        }
    }
}