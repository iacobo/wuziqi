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
import androidx.compose.ui.layout.layout

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
    val gridLineWidth = 1.dp
    val boardSize = GameState.BOARD_SIZE
    val boardColor = if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFE6C47A)
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(16.dp)
            .background(boardColor)
    ) {
        // This is where we'll draw the grid and pieces
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val maxWidthPx = constraints.maxWidth
            val cellSizePx = maxWidthPx / (boardSize - 1)
            
            // Draw the grid lines first
            // Horizontal lines
            for (i in 0 until boardSize) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridLineWidth)
                        .align(Alignment.TopStart)
                        .offset(y = (i * cellSizePx).dp)
                        .background(gridLineColor)
                )
            }
            
            // Vertical lines
            for (i in 0 until boardSize) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(gridLineWidth)
                        .align(Alignment.TopStart)
                        .offset(x = (i * cellSizePx).dp)
                        .background(gridLineColor)
                )
            }
            
            // Now place the stones at the intersections
            for (row in 0 until boardSize) {
                for (col in 0 until boardSize) {
                    // Position stones exactly at intersections
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.TopStart)
                            .offset(
                                x = (col * cellSizePx).dp - 14.dp,
                                y = (row * cellSizePx).dp - 14.dp
                            )
                            .clickable { onTileClick(row, col) },
                        contentAlignment = Alignment.Center
                    ) {
                        // Draw the stone if this intersection is occupied
                        if (gameState.board[row][col] != GameState.EMPTY) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (gameState.board[row][col]) {
                                            GameState.PLAYER_ONE -> Color.Black
                                            else -> Color.White
                                        }
                                    )
                                    .border(
                                        width = if (lastPlacedPosition?.first == row && lastPlacedPosition.second == col) 2.dp else 0.dp,
                                        color = if (lastPlacedPosition?.first == row && lastPlacedPosition.second == col) Color.Red else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                        
                        // Draw star points (hoshi)
                        if ((row == 3 && col == 3) || 
                            (row == 3 && col == 11) || 
                            (row == 11 && col == 3) || 
                            (row == 11 && col == 11) ||
                            (row == 7 && col == 7)) {
                            if (gameState.board[row][col] == GameState.EMPTY) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(gridLineColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}