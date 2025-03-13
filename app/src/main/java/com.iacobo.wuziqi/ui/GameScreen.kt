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
        Text(
            text = "Wuziqi",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
        // Board grid lines
        for (i in 0 until GameState.BOARD_SIZE) {
            // Horizontal lines
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridLineWidth)
                    .background(gridLineColor)
                    .align(Alignment.TopStart)
                    .offset(y = ((i * (1f / (GameState.BOARD_SIZE - 1)) * 100).dp))
            )
            
            // Vertical lines
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(gridLineWidth)
                    .background(gridLineColor)
                    .align(Alignment.TopStart)
                    .offset(x = ((i * (1f / (GameState.BOARD_SIZE - 1)) * 100).dp))
            )
        }
        
        // Tiles and pieces
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            for (row in 0 until GameState.BOARD_SIZE) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    for (col in 0 until GameState.BOARD_SIZE) {
                        Tile(
                            state = gameState.board[row][col],
                            isLastPlaced = lastPlacedPosition?.let { it.first == row && it.second == col } ?: false,
                            modifier = Modifier.weight(1f),
                            isDarkTheme = isDarkTheme,
                            onClick = { onTileClick(row, col) }
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
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Define darker blue for player pieces
    val playerOneColor = MaterialTheme.colorScheme.primary
    val playerTwoColor = if (isDarkTheme) Color(0xFF191970) else Color(0xFF000080) // Dark navy blue
    
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
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        when (state) {
                            GameState.PLAYER_ONE -> playerOneColor
                            else -> playerTwoColor
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