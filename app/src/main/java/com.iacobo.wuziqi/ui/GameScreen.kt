package com.iacobo.wuziqi.ui

// Core Compose imports only
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.viewmodel.GameViewModel
import com.iacobo.wuziqi.viewmodel.Position

@Composable
fun GameScreen() {
    // Use the ViewModel approach for state persistence
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
        // Player turn indicator with black/white text instead of player numbers
        Text(
            text = "${if (gameState.currentPlayer == GameState.PLAYER_ONE) "Black" else "White"}'s Turn",
            style = MaterialTheme.typography.titleMedium,
            color = if (gameState.currentPlayer == GameState.PLAYER_ONE) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Display winner dialog if there is a winner
        if (winner != null) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Game Over") },
                text = { Text("${if (winner == GameState.PLAYER_ONE) "Black" else "White"} won!") },
                confirmButton = {
                    Button(onClick = { viewModel.resetGame() }) {
                        Text("Rematch")
                    }
                },
                dismissButton = {
                    Button(onClick = { viewModel.dismissWinnerDialog() }) {
                        Text("Close")
                    }
                }
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
        
        // Button Row with Undo and Reset using Material3 IconButtons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Undo Button with arrow back icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FilledIconButton(
                    onClick = { viewModel.undoMove() },
                    enabled = moveHistory.isNotEmpty() && winner == null,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Undo move"
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Undo",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Reset Button with refresh icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FilledIconButton(
                    onClick = { viewModel.resetGame() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reset game"
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

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
    
    // Draw board
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(8.dp)
            .background(if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFE6C47A))
    ) {

        // Draw gridlines half a tile width inwards
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(12.dp)
        ) {
            
            // Draw the board with grid lines
            // We'll calculate spacing between lines based on the available space
            
            // Game board layout
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Draw the horizontal grid lines
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
            
            // Draw the vertical grid lines
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Draw the vertical grid lines
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
            
        // Tiles and pieces - we place them at the intersections
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
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
            .clip(CircleShape) // Clip the clickable area to a circle
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