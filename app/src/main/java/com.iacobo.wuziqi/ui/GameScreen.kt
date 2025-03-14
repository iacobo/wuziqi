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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState

// Simple data class to store move information
data class Move(val row: Int, val col: Int, val player: Int)

@Composable
fun GameScreen() {
    // Custom saver for the Move list
    val moveSaver = listSaver<List<Move>, Map<String, Int>>(
        save = { moveList ->
            moveList.map { move ->
                mapOf(
                    "row" to move.row,
                    "col" to move.col,
                    "player" to move.player
                )
            }
        },
        restore = { mapList ->
            mapList.map { item ->
                Move(
                    row = item["row"] ?: 0,
                    col = item["col"] ?: 0,
                    player = item["player"] ?: 0
                )
            }
        }
    )

    // Custom saver for the game state
    val gameStateSaver = run {
        val key = "GameState"
        mapSaver(
            save = { gameState ->
                val boardFlattened = mutableListOf<Int>()
                for (row in gameState.board) {
                    for (tile in row) {
                        boardFlattened.add(tile)
                    }
                }
                mapOf(
                    "boardFlattened" to boardFlattened,
                    "currentPlayer" to gameState.currentPlayer
                )
            },
            restore = { savedMap ->
                val boardSize = GameState.BOARD_SIZE
                val gameState = GameState()
                val boardFlattened = savedMap["boardFlattened"] as? List<Int> ?: List(boardSize * boardSize) { 0 }
                
                for (i in boardFlattened.indices) {
                    val row = i / boardSize
                    val col = i % boardSize
                    gameState.board[row][col] = boardFlattened[i]
                }
                gameState.currentPlayer = (savedMap["currentPlayer"] as? Int) ?: GameState.PLAYER_ONE
                gameState
            }
        )
    }

    // Custom saver for Pair<Int, Int>?
    val pairSaver = run {
        mapSaver(
            save = { pair: Pair<Int, Int>? ->
                if (pair == null) {
                    mapOf("isNull" to true)
                } else {
                    mapOf(
                        "isNull" to false,
                        "first" to pair.first,
                        "second" to pair.second
                    )
                }
            },
            restore = { savedMap ->
                val isNull = savedMap["isNull"] as? Boolean ?: true
                if (isNull) {
                    null
                } else {
                    Pair(
                        savedMap["first"] as? Int ?: 0,
                        savedMap["second"] as? Int ?: 0
                    )
                }
            }
        )
    }

    // Use the custom savers with rememberSaveable
    var gameState by rememberSaveable(stateSaver = gameStateSaver) { 
        mutableStateOf(GameState()) 
    }
    var winner by rememberSaveable { mutableStateOf<Int?>(null) }
    var lastPlacedPosition by rememberSaveable(stateSaver = pairSaver) { 
        mutableStateOf<Pair<Int, Int>?>(null) 
    }
    var moveHistory by rememberSaveable(stateSaver = moveSaver) { 
        mutableStateOf(listOf<Move>()) 
    }
    
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
                    Button(onClick = { 
                        gameState = GameState()
                        winner = null
                        lastPlacedPosition = null
                        moveHistory = listOf()
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
            isGameFrozen = winner != null,
            onTileClick = { row, col ->
                if (winner == null && gameState.isTileEmpty(row, col)) {
                    val currentPlayer = gameState.currentPlayer
                    // Save the move to history before making it
                    moveHistory = moveHistory + Move(row, col, currentPlayer)
                    
                    // Create a copy of the current game state and modify it
                    val updatedGameState = GameState()
                    // Copy the board
                    for (r in 0 until GameState.BOARD_SIZE) {
                        for (c in 0 until GameState.BOARD_SIZE) {
                            updatedGameState.board[r][c] = gameState.board[r][c]
                        }
                    }
                    // Copy the current player
                    updatedGameState.currentPlayer = gameState.currentPlayer
                    // Place the tile
                    updatedGameState.placeTile(row, col)
                    
                    // Update the game state with the new state
                    gameState = updatedGameState
                    lastPlacedPosition = Pair(row, col)
                    
                    // Check for win
                    val playerToCheck = currentPlayer
                    if (gameState.checkWin(row, col, playerToCheck)) {
                        winner = playerToCheck
                    }
                }
            }
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
                    onClick = {
                        if (moveHistory.isNotEmpty() && winner == null) {
                            // Get last move
                            val lastMove = moveHistory.last()
                            
                            // Create a copy of the current game state
                            val updatedGameState = GameState()
                            // Copy the board
                            for (r in 0 until GameState.BOARD_SIZE) {
                                for (c in 0 until GameState.BOARD_SIZE) {
                                    updatedGameState.board[r][c] = gameState.board[r][c]
                                }
                            }
                            
                            // Remove the last move from the board
                            updatedGameState.board[lastMove.row][lastMove.col] = GameState.EMPTY
                            // Set the current player to the one who made the last move
                            updatedGameState.currentPlayer = lastMove.player
                            
                            // Update the game state
                            gameState = updatedGameState
                            
                            // Update last placed position
                            lastPlacedPosition = if (moveHistory.size > 1) {
                                val previousMove = moveHistory[moveHistory.size - 2]
                                Pair(previousMove.row, previousMove.col)
                            } else {
                                null
                            }
                            
                            // Remove the move from history
                            moveHistory = moveHistory.dropLast(1)
                        }
                    },
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
                    onClick = { 
                        gameState = GameState()
                        winner = null
                        lastPlacedPosition = null
                        moveHistory = listOf()
                    },
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
    lastPlacedPosition: Pair<Int, Int>?,
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
                            isLastPlaced = lastPlacedPosition?.let { it.first == row && it.second == col } ?: false,
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