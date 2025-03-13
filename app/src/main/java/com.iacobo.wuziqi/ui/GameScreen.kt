package com.iacobo.wuziqi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun GameScreen() {
    var gameState by remember { mutableStateOf(GameState()) }
    var winner by remember { mutableStateOf<Int?>(null) }
    var lastPlacedPosition by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val isDarkTheme = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Removed the "Wuziqi" title from here
        
        // Player turn indicator
        Text(
            text = "Player ${gameState.currentPlayer}'s Turn",
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
                text = { Text("Player ${if (winner == GameState.PLAYER_ONE) 1 else 2} won!") },
                confirmButton = {
                    Button(onClick = { 
                        gameState.reset()
                        winner = null
                        lastPlacedPosition = null
                    }) {
                        Text("Rematch")
                    }
                },
                dismissButton = {
                    Button(onClick = { winner = null }) {
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
            onTileClick = { row, col ->
                if (winner == null && gameState.isTileEmpty(row, col)) {
                    val currentPlayer = gameState.currentPlayer
                    gameState.placeTile(row, col)
                    lastPlacedPosition = Pair(row, col)
                    // We need to check if the previous player won (the one who just placed the piece)
                    val playerToCheck = if (currentPlayer == GameState.PLAYER_ONE) GameState.PLAYER_ONE else GameState.PLAYER_TWO
                    if (gameState.checkWin(row, col, playerToCheck)) {
                        winner = playerToCheck
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Reset Button
        Button(
            onClick = { 
                gameState.reset()
                winner = null
                lastPlacedPosition = null
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Text("Reset Game")
        }
    }
}

@Composable
fun GameBoard(
    gameState: GameState, 
    lastPlacedPosition: Pair<Int, Int>?,
    isDarkTheme: Boolean,
    onTileClick: (Int, Int) -> Unit
) {
    val gridLineColor = if (isDarkTheme) Color(0xDDCCCCCC) else Color(0xDD333333)
    val gridLineWidth = 1.dp
    val boardSize = GameState.BOARD_SIZE
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(8.dp)
            .background(if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFE6C47A))
    ) {
        // Game board layout with pieces
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
                            isLastPlaced = lastPlacedPosition?.let { it.first == row && it.second == col } ?: false,
                            modifier = Modifier.weight(1f),
                            onClick = { onTileClick(row, col) }
                        )
                    }
                }
            }
        }
        
        // Draw grid lines to match the piece placement
        // Instead of going edge to edge, we'll offset the grid lines to match the centers of the tile positions
        Box(modifier = Modifier.fillMaxSize()) {
            // Calculate the size of each cell
            val cellSize = 1f / boardSize
            
            // Draw horizontal grid lines
            for (i in 0 until boardSize) {
                // Position at the center of the cell row
                val yPosition = (i + 0.5f) * cellSize
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridLineWidth)
                        .align(Alignment.TopStart)
                        .offset(y = (yPosition * 100).dp) // Convert from percentage to dp
                        .background(gridLineColor)
                )
            }
            
            // Draw vertical grid lines
            for (i in 0 until boardSize) {
                // Position at the center of the cell column
                val xPosition = (i + 0.5f) * cellSize
                
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(gridLineWidth)
                        .align(Alignment.TopStart)
                        .offset(x = (xPosition * 100).dp) // Convert from percentage to dp
                        .background(gridLineColor)
                )
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