package com.iacobo.wuziqi.viewmodel

import android.app.Application
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iacobo.wuziqi.R
import com.iacobo.wuziqi.ai.WuziqiAIEngine
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.data.UserPreferencesRepository
import com.iacobo.wuziqi.ui.Opponent
import java.util.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val aiEngine = WuziqiAIEngine(Random())

/** Represents a move in the game with position and player information. */
data class Move(val row: Int, val col: Int, val player: Int)

/** Represents a position on the board. */
data class Position(val row: Int, val col: Int)

// Constants for player results
const val DRAW = -1

/**
 * ViewModel that manages the game state and provides actions and state for the UI. Handles move
 * history and state persistence across configuration changes.
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

    // Easter eggs tracking
    private val easterEggManager = GameState.EasterEggManager(application)
    var discoveredEasterEggs by mutableStateOf(emptySet<String>())
        private set

    // Sound settings
    private val userPreferencesRepository = UserPreferencesRepository(application)
    private var soundEnabled = false

    // SoundPool for softer, more natural sounds
    private val soundPool: SoundPool
    private val soundWin: Int
    private val soundUndo: Int
    private val soundReset: Int
    private val soundPlaceTile: Int
    private val soundPlaceTicTacToe: Int
    private val soundPlaceConnect4: Int

    init {
        // Initialize SoundPool
        val audioAttributes =
                AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()

        soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(audioAttributes).build()

        // Load sound effects
        soundPlaceTile = soundPool.load(application, R.raw.soft_tap, 1)
        soundPlaceTicTacToe = soundPool.load(application, R.raw.soft_scratch, 1)
        soundPlaceConnect4 = soundPool.load(application, R.raw.soft_drop, 1)
        soundWin = soundPool.load(application, R.raw.soft_success, 1)
        soundUndo = soundPool.load(application, R.raw.soft_pop, 1)
        soundReset = soundPool.load(application, R.raw.soft_click, 1)

        // Observe sound settings
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collectLatest { preferences ->
                soundEnabled = preferences.soundEnabled
            }
        }

        // Load discovered easter eggs
        discoveredEasterEggs = easterEggManager.getDiscoveredEasterEggs()

        // Try to load saved game state
        viewModelScope.launch { gameState.loadState(application) }
    }

    override fun onCleared() {
        super.onCleared()
        soundPool.release()
    }

    /**
     * Sets up a new game with the specified parameters
     *
     * @param boardSize The size of the game board
     * @param winLength The number of consecutive pieces needed to win
     * @param opponent The type of opponent (human or computer)
     * @param skipStartSound Flag to skip playing the reset sound on initial setup
     */
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
        if (boardSize == 3 && winLength == 3) {
            easterEggManager.addDiscoveredEasterEgg("tictactoe")
            discoveredEasterEggs = easterEggManager.getDiscoveredEasterEggs()
        } else if (boardSize == 7 && winLength == 4) {
            easterEggManager.addDiscoveredEasterEgg("connect4")
            discoveredEasterEggs = easterEggManager.getDiscoveredEasterEggs()
        }

        // Play sound effect if enabled AND we should not skip it
        if (soundEnabled && !skipStartSound) {
            soundPool.play(soundReset, 0.7f, 0.7f, 1, 0, 1.0f)
        }

        // Save initial state
        viewModelScope.launch { gameState.saveState(getApplication()) }
    }

    /**
     * Places a tile at the specified position if valid. Updates state and checks for win condition.
     *
     * @param row The row position
     * @param col The column position
     * @param bypassLoading Set to true for AI moves to bypass the isLoading check
     */
    fun placeTile(row: Int, col: Int, bypassLoading: Boolean = false) {
        // Skip the isLoading check if bypassLoading is true (AI move)
        if (!bypassLoading && (winner != null || !gameState.isTileEmpty(row, col) || isLoading)) {
            return
        }

        // Still check the other conditions even for AI moves
        if (winner != null || !gameState.isTileEmpty(row, col)) {
            return
        }

        val currentPlayer = gameState.currentPlayer

        // Save the move to history before making it
        moveHistory = moveHistory + Move(row, col, currentPlayer)

        // Place the tile
        gameState.placeTile(row, col)
        lastPlacedPosition = Position(row, col)

        // Play sound effect if enabled
        if (soundEnabled) {
            playTileSound()
        }

        // Check for win
        if (gameState.checkWin(row, col, currentPlayer)) {
            winner = currentPlayer

            // Play win sound if enabled
            if (soundEnabled) {
                playWinSound()
            }

            // Clear saved state if game is over
            viewModelScope.launch { gameState.clearSavedState(getApplication()) }

            return
        }

        // Check for draw
        if (gameState.isBoardFull()) {
            winner = DRAW

            // Clear saved state if game is over
            viewModelScope.launch { gameState.clearSavedState(getApplication()) }

            return
        }

        // Save state after move
        viewModelScope.launch { gameState.saveState(getApplication()) }

        // Only trigger computer move if this wasn't already a computer move
        // This prevents infinite recursion
        if (gameState.againstComputer && winner == null && !bypassLoading) {
            makeComputerMove()
        }
    }

    /**
     * Finds the bottom-most empty row in a column for Connect4 Updated to handle a 7x6 board size
     * (width x height)
     */
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

    /**
     * Places a Connect4 tile in the specified column Updated with bypassLoading parameter for AI
     * moves
     */
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

        // Place the tile normally
        val currentPlayer = gameState.currentPlayer

        // Save the move to history before making it
        moveHistory = moveHistory + Move(row, col, currentPlayer)

        // Place the tile
        gameState.placeTile(row, col)
        lastPlacedPosition = Position(row, col)

        // Play sound effect if enabled
        if (soundEnabled) {
            playTileSound()
        }

        // Check for win
        if (gameState.checkWin(row, col, currentPlayer)) {
            winner = currentPlayer

            // Play win sound if enabled
            if (soundEnabled) {
                playWinSound()
            }

            // Clear saved state if game is over
            viewModelScope.launch { gameState.clearSavedState(getApplication()) }

            return
        }

        // Check for draw - for Connect4, we need to check if all columns are full
        var isFull = true
        for (c in 0 until gameState.boardSize) {
            if (findBottomEmptyRow(c) != -1) {
                isFull = false
                break
            }
        }

        if (isFull) {
            winner = DRAW

            // Clear saved state if game is over
            viewModelScope.launch { gameState.clearSavedState(getApplication()) }

            return
        }

        // Save state after move
        viewModelScope.launch { gameState.saveState(getApplication()) }

        // Make computer move if playing against computer
        // Only do this for human moves (not AI moves)
        if (gameState.againstComputer && winner == null && !bypassLoading) {
            makeComputerMove()
        }
    }

    /** Makes a computer move based on the current game type. */
    private fun makeComputerMove() {
        if (winner != null || isLoading) return

        isLoading = true

        viewModelScope.launch {
            // Short delay to simulate thinking (and avoid UI flicker)
            delay(700)

            // Get best move from AI engine
            val bestMove = aiEngine.findBestMove(gameState)

            if (bestMove != null) {
                val (row, col) = bestMove

                // Check for Connect4 special case
                if (gameState.boardSize == 7 && gameState.winCondition == 4) {
                    placeConnect4Tile(col, bypassLoading = true)
                } else {
                    placeTile(row, col, bypassLoading = true)
                }
            }

            isLoading = false
        }
    }

    /** Plays the appropriate sound effect for placing a tile based on the game type. */
    private fun playTileSound() {
        val soundId =
                when {
                    // TicTacToe game - use soft_scratch
                    gameState.boardSize == 3 && gameState.winCondition == 3 -> soundPlaceTicTacToe
                    // Connect4 game - use soft_drop
                    gameState.boardSize == 7 && gameState.winCondition == 4 -> soundPlaceConnect4
                    // Default Wuziqi game - use soft_tap
                    else -> soundPlaceTile
                }

        soundPool.play(soundId, 0.7f, 0.7f, 1, 0, 1.0f)
    }

    /** Plays a sound effect for winning the game. */
    private fun playWinSound() {
        soundPool.play(soundWin, 0.7f, 0.7f, 1, 0, 1.0f)
    }

    /**
     * Undoes the last move if possible. If playing against the computer, undoes both player's and
     * computer's moves.
     */
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

                // Set current player back to the human player
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
            soundPool.play(soundUndo, 0.7f, 0.7f, 1, 0, 1.0f)
        }

        // Save state after undo
        viewModelScope.launch { gameState.saveState(getApplication()) }
    }

    /** Resets the game to initial state. */
    fun resetGame() {
        gameState.reset()
        winner = null
        lastPlacedPosition = null
        moveHistory = emptyList()

        // Play sound effect if enabled
        if (soundEnabled) {
            soundPool.play(soundReset, 0.7f, 0.7f, 1, 0, 1.0f)
        }

        // Clear saved state
        viewModelScope.launch { gameState.clearSavedState(getApplication()) }
    }
}
