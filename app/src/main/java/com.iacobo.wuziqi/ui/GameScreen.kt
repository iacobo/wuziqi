package com.iacobo.wuziqi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.utils.SoundManager

@Composable
fun GameScreen() {
    var gameState by remember { mutableStateOf(GameState()) }
    var isMuted by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("wuziqi", style = MaterialTheme.typography.h4)

        // Display winner dialog if there is a winner
        winner?.let {
            WinnerDialog(winner = it) { rematch ->
                if (rematch) {
                    gameState.reset()
                    winner = null
                } else {
                    // Optionally handle quitting the app
                    winner = null
                }
            }
        } ?: run {
            // Game Board
            GameBoard(gameState) { row, col ->
                if (gameState.isTileEmpty(row, col)) {
                    gameState.placeTile(row, col)
                    if (!isMuted) SoundManager.playTileSound()
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

            // Mute Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mute Sounds")
                Switch(checked = isMuted, onCheckedChange = { isMuted = it })
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
        cells = GridCells.Fixed(5),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(gameState.board.size) { row ->
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