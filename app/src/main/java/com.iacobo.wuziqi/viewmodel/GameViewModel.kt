package com.iacobo.wuziqi.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.iacobo.wuziqi.data.GameState

/**
 * Represents a move in the game with position and player information.
 */
data class Move(val row: Int, val col: Int, val player: Int)

/**
 * Represents a position on the board.
 */
data class Position(val row: Int, val col: Int)

/**
 * ViewModel that manages the game state and provides actions and state for the UI.
 * Handles move history and state persistence across configuration changes.
 */
class GameViewModel : ViewModel() {
    // Game state
    var gameState by mutableStateOf(GameState())
        private set
    
    // Winner state (null means no winner yet)
    var winner by mutableStateOf<Int?>(null)
        private set
    
    // Last placed position for highlighting
    var lastPlacedPosition by mutableStateOf<Position?>(null)
        private set
    
    // Move history for undo functionality
    var moveHistory by mutableStateOf(emptyList<Move>())
        private set

    /**
     * Places a tile at the specified position if valid.
     * Updates state and checks for win condition.
     */
    fun placeTile(row: Int, col: Int) {
        if (winner != null || !gameState.isTileEmpty(row, col)) {
            return
        }

        val currentPlayer = gameState.currentPlayer
        
        // Save the move to history before making it
        moveHistory = moveHistory + Move(row, col, currentPlayer)
        
        // Place the tile
        gameState.placeTile(row, col)
        lastPlacedPosition = Position(row, col)
        
        // Check for win
        if (gameState.checkWin(row, col, currentPlayer)) {
            winner = currentPlayer
        }
    }

    /**
     * Undoes the last move if possible.
     */
    fun undoMove() {
        if (moveHistory.isEmpty() || winner != null) {
            return
        }
        
        // Get last move
        val lastMove = moveHistory.last()
        
        // Remove it from board
        gameState.board[lastMove.row][lastMove.col] = GameState.EMPTY
        
        // Update current player to the one who made the last move
        gameState.currentPlayer = lastMove.player
        
        // Update last placed position
        lastPlacedPosition = if (moveHistory.size > 1) {
            val previousMove = moveHistory[moveHistory.size - 2]
            Position(previousMove.row, previousMove.col)
        } else {
            null
        }
        
        // Remove the move from history
        moveHistory = moveHistory.dropLast(1)
    }

    /**
     * Resets the game to initial state.
     */
    fun resetGame() {
        gameState = GameState()
        winner = null
        lastPlacedPosition = null
        moveHistory = emptyList()
    }

    /**
     * Dismisses the winner dialog without resetting the game.
     */
    fun dismissWinnerDialog() {
        winner = null
    }
}