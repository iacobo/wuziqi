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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iacobo.wuziqi.R
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.viewmodel.GameViewModel
import com.iacobo.wuziqi.viewmodel.Position

/**
 * Main game screen composable.
 * Displays the game board, status, and controls.
 */
@Composable
fun GameScreen() {
    val viewModel: GameViewModel = viewModel()
    val gameState = viewModel.gameState
    val winner = viewModel.winner
    val lastPlacedPosition = viewModel.lastPlacedPosition
    val moveHistory = viewModel.moveHistory
    
    val isDarkTheme = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Player turn indicator
        PlayerTurnIndicator(gameState.currentPlayer)
        
        Spacer(modifier = Modifier.height(24.dp))

        // Winner dialog
        if (winner != null) {
            WinnerDialog(
                winner = winner,
                onRematch = { viewModel.resetGame() },
                onDismiss = { viewModel.dismissWinnerDialog() }
            )
        }
        
        // Game Board
        GameBoard(
            gameState = gameState,
            lastPlacedPosition = lastPlacedPosition,
            isDarkTheme = isDarkTheme,
            isGameFrozen = winner != null,
            onTileClick = { row, col -> viewModel.placeTile(row, col) }
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        // Control buttons
        GameControls(
            canUndo = moveHistory.isNotEmpty() && winner == null,
            onUndo = { viewModel.undoMove() },
            onReset = { viewModel.resetGame() }
        )
    }
}

/**
 * Displays the current player's turn.
 */
@Composable
private fun PlayerTurnIndicator(currentPlayer: Int) {
    val playerName = if (currentPlayer == GameState.PLAYER_ONE) "Black" else "White"
    val playerColor = if (currentPlayer == GameState.PLAYER_ONE) 
        MaterialTheme.colorScheme.primary 
    else 
        MaterialTheme.colorScheme.secondary
    
    Text(
        text = "$playerName's Turn",
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
        title = { Text("Game Over") },
        text = { Text("${if (winner == GameState.PLAYER_ONE) "Black" else "White"} won!") },
        confirmButton = {
            Button(onClick = onRematch) {
                Text("Rematch")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Game control buttons (Undo, Reset).
 */
@Composable
private fun GameControls(
    canUndo: Boolean,
    onUndo: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Undo Button
        ControlButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            label = "Undo",
            onClick = onUndo,
            enabled = canUndo
        )
        
        // Reset Button
        ControlButton(
            icon = Icons.Filled.Refresh,
            label = "Reset",
            onClick = onReset,
            enabled = true
        )
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
 */
@Composable
fun GameBoard(
    gameState: GameState, 
    lastPlacedPosition: Position?,
    isDarkTheme: Boolean,
    isGameFrozen: Boolean,
    onTileClick: (Int, Int) -> Unit
) {
    val gridLineColor = if (isDarkTheme) Color(0xDDCCCCCC) else Color(0xDD333333)
    val gridLineWidth = 1.dp
    val boardSize = GameState.BOARD_SIZE
    val boardColor = if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFE6C47A)
    
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (state != GameState.EMPTY) {
            Box(
                modifier = Modifier
                    .size(24.dp)
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