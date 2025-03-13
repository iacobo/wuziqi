package com.iacobo.wuziqi.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
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
    val gridLineWidth = 2.dp
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(8.dp)
    ) {
        // We need to properly calculate positions for a 15x15 grid
        val boardSize = GameState.BOARD_SIZE.toFloat()

        // Draw the grid lines with absolute positioning
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val cellWidth = canvasWidth / (boardSize - 1f)
            val cellHeight = canvasHeight / (boardSize - 1f)
            
            // Draw horizontal lines
            for (i in 0 until GameState.BOARD_SIZE) {
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, i * cellHeight),
                    end = Offset(canvasWidth, i * cellHeight),
                    strokeWidth = gridLineWidth.toPx()
                )
            }
            
            // Draw vertical lines
            for (i in 0 until GameState.BOARD_SIZE) {
                drawLine(
                    color = gridLineColor,
                    start = Offset(i * cellWidth, 0f),
                    end = Offset(i * cellWidth, canvasHeight),
                    strokeWidth = gridLineWidth.toPx()
                )
            }
        }
        
        // Overlay the tiles and pieces
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val boxWidth = maxWidth
            val cellWidth = boxWidth / (boardSize - 1f)
            
            // Place pieces at grid intersections
            for (row in 0 until GameState.BOARD_SIZE) {
                for (col in 0 until GameState.BOARD_SIZE) {
                    if (gameState.board[row][col] != GameState.EMPTY) {
                        val xPos = col * cellWidth
                        val yPos = row * cellWidth
                        
                        Box(
                            modifier = Modifier
                                .offset(x = xPos - 10.dp, y = yPos - 10.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    when (gameState.board[row][col]) {
                                        GameState.PLAYER_ONE -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.secondary
                                    }
                                )
                                .border(
                                    width = if (lastPlacedPosition?.let { it.first == row && it.second == col } ?: false) 2.dp else 0.dp,
                                    color = if (lastPlacedPosition?.let { it.first == row && it.second == col } ?: false) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                    
                    // Clickable area for placing stones
                    Box(
                        modifier = Modifier
                            .offset(x = col * cellWidth - 20.dp, y = row * cellWidth - 20.dp)
                            .size(40.dp)
                            .clickable { onTileClick(row, col) }
                    )
                }
            }
        }
    }
}