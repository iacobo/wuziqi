package com.iacobo.wuziqi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.* // Import Material 3 components
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState

@Composable
fun GameScreen() {
    var gameState by remember { mutableStateOf(GameState()) }
    var winner by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("wuziqi", style = MaterialTheme.typography.headlineLarge)

        // Display winner dialog if there is a winner
        winner?.let { winnerId ->
            WinnerDialog(winner = winnerId) { rematch ->
                if (rematch) {
                    gameState.reset()
                    winner = null
                } else {
                    winner = null
                }
            }
        } ?: run {
            // Game Board
            GameBoard(gameState) { row: Int, col: Int ->
                if (gameState.isTileEmpty(row, col)) {
                    gameState.placeTile(row, col)
                    if (gameState.checkWin(row, col)) {
                        winner = gameState.currentPlayer
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reset Button
            Button(onClick = { gameState.reset(); winner = null }) {
                Text("Reset Game")
            }
        }
    }
}

@Composable
fun WinnerDialog(winner: Int, onDismiss: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss(false) },
        title = { Text("Game Over") },
        text = { Text("Player $winner won!") },
        confirmButton = {
            Button(onClick = { onDismiss(true) }) {
                Text("Rematch")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss(false) }) {
                Text("Quit")
            }
        }
    )
}

@Composable
fun GameBoard(gameState: GameState, onTileClick: (Int, Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(gameState.board.size) { row: Int ->
            Row {
                for (col in gameState.board[row].indices) {
                    Tile(
                        state = gameState.board[row][col],
                        onClick = { onTileClick(row, col) }
                    )
                }
            }
        }
    }
}

@Composable
fun Tile(state: Int, onClick: () -> Unit) {
    val color = when (state) {
        1 -> Color.Black
        2 -> Color.White
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(60.dp)
            .clickable(onClick = onClick)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    )
}
