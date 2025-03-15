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
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.data.UserPreferencesRepository
import com.iacobo.wuziqi.ui.Opponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.random.Random

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
 * Now supports custom board sizes, computer opponent, and game variants.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {
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
        
    // Game is loading state (for computer moves)
    var isLoading by mutableStateOf(false)
        private set

    // Sound settings
    private val userPreferencesRepository = UserPreferencesRepository(application)
    private var soundEnabled = false
    
    // SoundPool for softer, more natural sounds
    private val soundPool: SoundPool
    private val soundPlaceTile: Int
    private val soundWin: Int
    private val soundUndo: Int
    private val soundReset: Int

    init {
        // Initialize SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
            
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()
        
        // Load sound effects
        soundPlaceTile = soundPool.load(application, R.raw.soft_tap, 1)
        soundWin = soundPool.load(application, R.raw.soft_success, 1)
        soundUndo = soundPool.load(application, R.raw.soft_pop, 1)
        soundReset = soundPool.load(application, R.raw.soft_click, 1)
        
        // Observe sound settings
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collectLatest { preferences ->
                soundEnabled = preferences.soundEnabled
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundPool.release()
    }
    
    /**
     * Sets up a new game with specified parameters.
     * 
     * @param boardSize The size of the board (e.g., 15 for a 15x15 board)
     * @param winLength Number of consecutive pieces needed to win
     * @param opponent The type of opponent (HUMAN or COMPUTER)
     */
    fun setupGame(boardSize: Int, winLength: Int, opponent: Opponent) {
        gameState = GameState(boardSize, winLength)
        gameState.againstComputer = opponent == Opponent.COMPUTER
        winner = null
        lastPlacedPosition = null
        moveHistory = emptyList()
        
        // Play sound effect if enabled
        if (soundEnabled) {
            soundPool.play(soundReset, 0.7f, 0.7f, 1, 0, 1.0f)
        }
    }

    /**
     * Places a tile at the specified position if valid.
     * Updates state and checks for win condition.
     * If against computer, triggers computer move after player move.
     */
    fun placeTile(row: Int, col: Int) {
        if (winner != null || !gameState.isTileEmpty(row, col) || isLoading) {
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
            return
        }
        
        // Check for draw (board full)
        if (isBoardFull()) {
            // No winner, but game is over
            winner = GameState.EMPTY
            return
        }
        
        // Computer's turn if enabled and game is still ongoing
        if (gameState.againstComputer && winner == null && gameState.currentPlayer == GameState.PLAYER_TWO) {
            makeComputerMove()
        }
    }
    
    /**
     * Places a tile in Connect 4 mode - automatically finds the lowest available
     * row in the selected column.
     * 
     * @param col The column to place the piece in
     */
    fun placeConnect4Tile(col: Int) {
        if (winner != null || isLoading) {
            return
        }
        
        // Find the lowest empty position in the column
        val row = findLowestEmptyRow(col)
        if (row == -1) {
            // Column is full
            return
        }
        
        // Place the tile at the found position
        placeTile(row, col)
    }
    
    /**
     * Finds the lowest empty row in a column for Connect 4 gameplay.
     * 
     * @param col The column to check
     * @return The row index of the lowest empty position, or -1 if column is full
     */
    private fun findLowestEmptyRow(col: Int): Int {
        val boardSize = gameState.boardSize
        
        // Start from the bottom row and move up
        for (row in boardSize - 1 downTo 0) {
            if (gameState.isTileEmpty(row, col)) {
                return row
            }
        }
        
        // Column is full
        return -1
    }
    
    /**
     * Checks if the board is completely full.
     */
    private fun isBoardFull(): Boolean {
        val boardSize = gameState.boardSize
        
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    return false
                }
            }
        }
        
        return true
    }

    /**
     * Makes a move for the computer player.
     * Uses a simple algorithm to find a good move.
     */
    private fun makeComputerMove() {
        isLoading = true
        
        viewModelScope.launch {
            // Add a small delay to make it seem like the computer is "thinking"
            delay(800)
            
            // Is this a Connect 4 game?
            val isConnect4 = gameState.boardSize == 6 && gameState.winCondition == 4
            
            // Find a move using appropriate strategy
            val computerMove = if (isConnect4) {
                findConnect4Move()
            } else {
                findComputerMove()
            }
            
            // Place the tile for the computer
            if (computerMove != null) {
                val (row, col) = computerMove
                
                // Save the move to history
                moveHistory = moveHistory + Move(row, col, GameState.PLAYER_TWO)
                
                // Place the tile
                gameState.placeTile(row, col)
                lastPlacedPosition = Position(row, col)
                
                // Play sound effect if enabled
                if (soundEnabled) {
                    playTileSound()
                }
                
                // Check for win
                if (gameState.checkWin(row, col, GameState.PLAYER_TWO)) {
                    winner = GameState.PLAYER_TWO
                    
                    // Play win sound if enabled
                    if (soundEnabled) {
                        playWinSound()
                    }
                }
            }
            
            isLoading = false
        }
    }
    
    /**
     * Finds a good move for the computer in Connect 4.
     */
    private fun findConnect4Move(): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        
        // First, check for each column if we can place a piece
        val availableColumns = mutableListOf<Int>()
        for (col in 0 until boardSize) {
            val row = findLowestEmptyRow(col)
            if (row != -1) {
                availableColumns.add(col)
                
                // Check if placing here would win
                gameState.board[row][col] = GameState.PLAYER_TWO
                val isWinningMove = gameState.checkWin(row, col, GameState.PLAYER_TWO)
                gameState.board[row][col] = GameState.EMPTY
                
                if (isWinningMove) {
                    return row to col
                }
                
                // Check if opponent would win by placing here
                gameState.board[row][col] = GameState.PLAYER_ONE
                val wouldBeWinningMove = gameState.checkWin(row, col, GameState.PLAYER_ONE)
                gameState.board[row][col] = GameState.EMPTY
                
                if (wouldBeWinningMove) {
                    return row to col
                }
            }
        }
        
        // If no immediate win or block, choose a random column
        return if (availableColumns.isNotEmpty()) {
            val col = availableColumns[Random.nextInt(availableColumns.size)]
            findLowestEmptyRow(col) to col
        } else {
            null // No valid moves
        }
    }
    
    /**
     * Finds a good move for the computer.
     * This is a simplified algorithm that:
     * 1. Checks for winning moves
     * 2. Checks for blocking opponent's winning moves
     * 3. Otherwise makes a move near existing pieces
     * 
     * @return A pair of (row, col) for the computer's move, or null if no moves available
     */
    private fun findComputerMove(): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val winCondition = gameState.winCondition
        
        // Is this a Tic-Tac-Toe game?
        val isTicTacToe = boardSize == 3 && winCondition == 3
        
        // Create a list of all empty positions
        val emptyPositions = mutableListOf<Pair<Int, Int>>()
        val nearPiecesPositions = mutableListOf<Pair<Int, Int>>()
        
        // Check each position on the board
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    emptyPositions.add(row to col)
                    
                    // Check if this position is adjacent to any existing piece
                    var hasAdjacentPiece = false
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            val newRow = row + dr
                            val newCol = col + dc
                            if (newRow in 0 until boardSize && newCol in 0 until boardSize && 
                                !gameState.isTileEmpty(newRow, newCol)) {
                                hasAdjacentPiece = true
                                break
                            }
                        }
                        if (hasAdjacentPiece) break
                    }
                    
                    if (hasAdjacentPiece) {
                        nearPiecesPositions.add(row to col)
                    }
                }
            }
        }
        
        if (emptyPositions.isEmpty()) return null
        
        // 1. Look for winning moves
        for (pos in if (nearPiecesPositions.isNotEmpty()) nearPiecesPositions else emptyPositions) {
            val (row, col) = pos
            
            // Temporarily place a piece and check if it's a winning move
            gameState.board[row][col] = GameState.PLAYER_TWO
            val isWinningMove = gameState.checkWin(row, col, GameState.PLAYER_TWO)
            gameState.board[row][col] = GameState.EMPTY
            
            if (isWinningMove) {
                return pos
            }
        }
        
        // 2. Look for opponent's winning moves to block
        for (pos in if (nearPiecesPositions.isNotEmpty()) nearPiecesPositions else emptyPositions) {
            val (row, col) = pos
            
            // Temporarily place opponent's piece and check if it would be a winning move
            gameState.board[row][col] = GameState.PLAYER_ONE
            val wouldBeWinningMove = gameState.checkWin(row, col, GameState.PLAYER_ONE)
            gameState.board[row][col] = GameState.EMPTY
            
            if (wouldBeWinningMove) {
                return pos
            }
        }
        
        // 3. For Tic-Tac-Toe, try to take the center if available
        if (isTicTacToe && gameState.isTileEmpty(1, 1)) {
            return 1 to 1
        }
        
        // 4. If there's no immediate winning or blocking move, make a strategic move
        // For simplicity, we'll just choose a random position near existing pieces
        return if (nearPiecesPositions.isNotEmpty()) {
            nearPiecesPositions[Random.nextInt(nearPiecesPositions.size)]
        } else {
            // If no pieces on board yet, choose center or near-center position
            val center = boardSize / 2
            val offset = Random.nextInt(-2, 3)
            (center + offset) to (center + Random.nextInt(-2, 3))
        }
    }

    /**
     * Plays a soft sound effect for placing a tile.
     */
    private fun playTileSound() {
        soundPool.play(soundPlaceTile, 0.7f, 0.7f, 1, 0, 1.0f)
    }

    /**
     * Plays a sound effect for winning the game.
     */
    private fun playWinSound() {
        soundPool.play(soundWin, 0.7f, 0.7f, 1, 0, 1.0f)
    }

    /**
     * Undoes the last move if possible.
     * If against computer, undoes both player and computer moves.
     */
    fun undoMove() {
        if (moveHistory.isEmpty() || winner != null || isLoading) {
            return
        }

        // If against computer, undo both player's and computer's moves
        val movesToUndo = if (gameState.againstComputer && moveHistory.size >= 2 && 
                              gameState.currentPlayer == GameState.PLAYER_ONE) {
            2
        } else {
            1
        }
        
        repeat(movesToUndo) {
            if (moveHistory.isEmpty()) return@repeat
            
            // Get last move
            val lastMove = moveHistory.last()

            // Remove it from board
            gameState.board[lastMove.row][lastMove.col] = GameState.EMPTY

            // Update current player to the one who made the last move
            gameState.currentPlayer = lastMove.player

            // Remove the move from history
            moveHistory = moveHistory.dropLast(1)
        }
        
        // Update last placed position
        lastPlacedPosition = if (moveHistory.isNotEmpty()) {
            val previousMove = moveHistory.last()
            Position(previousMove.row, previousMove.col)
        } else {
            null
        }

        // Play sound effect if enabled
        if (soundEnabled) {
            soundPool.play(soundUndo, 0.7f, 0.7f, 1, 0, 1.0f)
        }
    }

    /**
     * Resets the game to initial state while maintaining current settings.
     */
    fun resetGame() {
        gameState.reset()
        winner = null
        lastPlacedPosition = null
        moveHistory = emptyList()

        // Play sound effect if enabled
        if (soundEnabled) {
            soundPool.play(soundReset, 0.7f, 0.7f, 1, 0, 1.0f)
        }
    }

    /**
     * Dismisses the winner dialog without resetting the game.
     */
    fun dismissWinnerDialog() {
        winner = null
    }
}