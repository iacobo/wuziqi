package com.iacobo.wuziqi.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iacobo.wuziqi.ai.AIFactory
import com.iacobo.wuziqi.audio.SoundManager
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.data.GameStateRepository
import com.iacobo.wuziqi.data.GameType
import com.iacobo.wuziqi.data.UserPreferencesRepository
import com.iacobo.wuziqi.ui.Opponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Constants for player results
const val DRAW = -1

/** Represents a position on the board. */
data class Position(val row: Int, val col: Int)

/** Represents a move in the game with position and player information. */
data class Move(val row: Int, val col: Int, val player: Int)

/**
 * ViewModel that manages the game state and provides actions and state for the UI. Final refactored
 * version with proper separation of concerns.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {
    // Game state
    var gameState by mutableStateOf(GameState())
        private set

    // Winner state (null means no winner yet, -1 means draw)
    var winner by mutableStateOf<Int?>(null)
        private set

    // Last placed position for highlighting
    var lastPlacedPosition by mutableStateOf<Position?>(null)
        private set

    // Move history for undo functionality
    var moveHistory by mutableStateOf(emptyList<Move>())
        private set

    // Loading state for AI thinking
    var isLoading by mutableStateOf(false)
        private set

    // State data repositories
    private val gameStateRepository = GameStateRepository(application)
    private val userPreferencesRepository = UserPreferencesRepository(application)

    // Sound management
    private val soundManager = SoundManager(application)
    private var soundEnabled = false

    // Easter eggs tracking
    var discoveredEasterEggs by mutableStateOf(emptySet<String>())
        private set

    init {
        // Observe sound settings
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collectLatest { preferences ->
                soundEnabled = preferences.soundEnabled
            }
        }

        // Load discovered easter eggs
        discoveredEasterEggs = gameStateRepository.getDiscoveredEasterEggs()

        // Try to load saved game state
        viewModelScope.launch { gameStateRepository.loadGameState(gameState) }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }

    /** Sets up a new game with the specified parameters */
    fun setupGame(
            boardSize: Int,
            winLength: Int,
            opponent: Opponent,
            skipStartSound: Boolean = false
    ) {
        // Clear any existing game
        winner = null
        lastPlacedPosition = null
        moveHistory = emptyList()

        // Create new game state
        gameState =
                GameState(
                        boardSize = boardSize,
                        winCondition = winLength,
                        againstComputer = opponent == Opponent.COMPUTER
                )

        // Check for easter eggs
        when {
            boardSize == 3 && winLength == 3 -> {
                gameStateRepository.addDiscoveredEasterEgg("tictactoe")
            }
            boardSize == 7 && winLength == 4 -> {
                gameStateRepository.addDiscoveredEasterEgg("connect4")
            }
            boardSize == 11 && winLength == 8 -> {
                gameStateRepository.addDiscoveredEasterEgg("hex")
            }
        }
        discoveredEasterEggs = gameStateRepository.getDiscoveredEasterEggs()

        // Play sound effect if enabled AND we should not skip it
        if (soundEnabled && !skipStartSound) {
            soundManager.playResetSound()
        }

        // Save initial state
        viewModelScope.launch { gameStateRepository.saveGameState(gameState) }
    }

    /** Places a tile at the specified position if valid. */
    fun placeTile(row: Int, col: Int, bypassLoading: Boolean = false) {
        // Skip the isLoading check if bypassLoading is true (AI move)
        if (!bypassLoading && (winner != null || !gameState.isTileEmpty(row, col) || isLoading)) {
            return
        }

        // Still check the other conditions even for AI moves
        if (winner != null || !gameState.isTileEmpty(row, col)) {
            return
        }

        // Get the current player before placing the tile
        val currentPlayer = gameState.currentPlayer

        // Save the move to history before making it
        moveHistory = moveHistory + Move(row, col, currentPlayer)

        // Place the tile
        gameState.placeTile(row, col)
        lastPlacedPosition = Position(row, col)

        // Play sound effect if enabled
        if (soundEnabled) {
            val gameType = GameType.fromGameState(gameState)
            soundManager.playTileSound(gameType)
        }

        // Determine the game type to handle special win conditions
        val gameType = GameType.fromGameState(gameState)

        // Check for win condition
        when (gameType) {
            GameType.Hex -> {
                // For Hex game, use its special win condition
                if (gameState.checkHexWin(currentPlayer)) {
                    handleWin(currentPlayer)
                    return
                }
            }
            GameType.Havannah -> {
                // For Havannah game, use its special win conditions
                if (gameState.checkHavannahWin(currentPlayer)) {
                    handleWin(currentPlayer)
                    return
                }
            }
            else -> {
                // Standard win check for other games
                if (gameState.checkWin(row, col, currentPlayer)) {
                    handleWin(currentPlayer)
                    return
                }
            }
        }

        // Check for draw
        if (gameState.isBoardFull()) {
            handleDraw()
            return
        }

        // Save state after move
        viewModelScope.launch { gameStateRepository.saveGameState(gameState) }

        // Trigger computer move if playing against computer
        if (gameState.againstComputer && winner == null && !bypassLoading) {
            makeComputerMove()
        }
    }

    /** Finds the bottom-most empty row in a column for Connect4 */
    private fun findBottomEmptyRow(col: Int): Int {
        // For Connect4 (7x6 board), we need to only check the 6 rows (0-5)
        val maxRow =
                if (gameState.boardSize == 7 && gameState.winCondition == 4) {
                    5 // 6 rows (0-5) for Connect4
                } else {
                    gameState.boardSize - 1
                }

        for (row in maxRow downTo 0) {
            if (gameState.board[row][col] == GameState.EMPTY) {
                return row
            }
        }
        return -1 // Column is full
    }

    /** Places a Connect4 tile in the specified column */
    fun placeConnect4Tile(col: Int, bypassLoading: Boolean = false) {
        // Skip the isLoading check if bypassLoading is true (AI move)
        if (!bypassLoading && (winner != null || isLoading)) {
            return
        }

        // Other checks still apply even for AI moves
        if (winner != null) {
            return
        }

        // Find the bottom-most empty row in the column
        val row = findBottomEmptyRow(col)
        if (row == -1) return // Column is full

        // Place the tile using the standard placeTile method
        placeTile(row, col, bypassLoading)
    }

    /** Makes a computer move based on the current game type. */
    private fun makeComputerMove() {
        if (winner != null || isLoading) return

        isLoading = true

        viewModelScope.launch {
            // Short delay to simulate thinking (and avoid UI flicker)
            delay(700)

            // Get the current game type
            val gameType = GameType.fromGameState(gameState)

            // Get the appropriate AI based on game type
            val ai = AIFactory.createAI(gameType)

            // Get best move from the AI
            val bestMove = ai.findBestMove(gameState)

            if (bestMove != null) {
                val (row, col) = bestMove

                // Check for Connect4 special case
                if (gameType == GameType.Connect4) {
                    placeConnect4Tile(col, bypassLoading = true)
                } else {
                    placeTile(row, col, bypassLoading = true)
                }
            }

            isLoading = false
        }
    }

    /** Handle a win by a player. */
    private fun handleWin(player: Int) {
        winner = player

        // Play win sound if enabled
        if (soundEnabled) {
            soundManager.playWinSound()
        }

        // Clear saved state if game is over
        viewModelScope.launch { gameStateRepository.clearSavedState() }
    }

    /** Handle a draw game. */
    private fun handleDraw() {
        winner = DRAW

        // Clear saved state if game is over
        viewModelScope.launch { gameStateRepository.clearSavedState() }
    }

    /** Undoes the last move if possible. */
    fun undoMove() {
        if (moveHistory.isEmpty() || winner != null || isLoading) {
            return
        }

        // Get last move
        val lastMove = moveHistory.last()

        // Remove the move from history before making changes to the board
        moveHistory = moveHistory.dropLast(1)

        // Remove it from board
        gameState.board[lastMove.row][lastMove.col] = GameState.EMPTY

        // If playing against computer, we need to handle both moves
        if (gameState.againstComputer) {
            // Only perform a second undo if there's another move AND the player is still the same
            // This prevents recursion from continuing after both moves are undone
            if (moveHistory.isNotEmpty() && moveHistory.last().player != lastMove.player) {
                // Get previous move (computer's move)
                val previousMove = moveHistory.last()

                // Remove it from history
                moveHistory = moveHistory.dropLast(1)

                // Remove it from board
                gameState.board[previousMove.row][previousMove.col] = GameState.EMPTY

                // Always set current player back to the human player (PLAYER_ONE)
                gameState.currentPlayer = GameState.PLAYER_ONE
            } else {
                // Set current player back to the one who made the last move
                gameState.currentPlayer = lastMove.player
            }
        } else {
            // Set current player back to the one who made the last move
            gameState.currentPlayer = lastMove.player
        }

        // Update last placed position
        lastPlacedPosition =
                if (moveHistory.isNotEmpty()) {
                    val previousMove = moveHistory.last()
                    Position(previousMove.row, previousMove.col)
                } else {
                    null
                }

        // Play sound effect if enabled
        if (soundEnabled) {
            soundManager.playUndoSound()
        }

        // Save state after undo
        viewModelScope.launch { gameStateRepository.saveGameState(gameState) }
    }

    /** Resets the game to initial state. */
    fun resetGame() {
        gameState.reset()
        winner = null
        lastPlacedPosition = null
        moveHistory = emptyList()

        // Play sound effect if enabled
        if (soundEnabled) {
            soundManager.playResetSound()
        }

        // Clear saved state
        viewModelScope.launch { gameStateRepository.clearSavedState() }
    }

    /** Checks if the undo operation is available. */
    fun canUndo(): Boolean {
        return moveHistory.isNotEmpty() && winner == null && !isLoading
    }

    /** Checks if the reset confirmation should be shown. */
    fun shouldConfirmReset(): Boolean {
        return winner == null && moveHistory.isNotEmpty()
    }
}
