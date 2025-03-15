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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * Represents a move in the game with position and player information.
 */
data class Move(val row: Int, val col: Int, val player: Int)

/**
 * Represents a position on the board.
 */
data class Position(val row: Int, val col: Int)

// Constants for player results
const val DRAW = -1

/**
 * ViewModel that manages the game state and provides actions and state for the UI.
 * Handles move history and state persistence across configuration changes.
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
    private val soundPlaceTile: Int
    private val soundWin: Int
    private val soundUndo: Int
    private val soundReset: Int
    
    // Random for AI
    private val random = Random()

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
        
        // Load discovered easter eggs
        discoveredEasterEggs = easterEggManager.getDiscoveredEasterEggs()
        
        // Try to load saved game state
        viewModelScope.launch {
            gameState.loadState(application)
        }
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
        gameState = GameState(
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
        
        // Play sound effect if enabled (but skip on initial launch)
        if (soundEnabled && !skipStartSound) {
            soundPool.play(soundReset, 0.7f, 0.7f, 1, 0, 1.0f)
        }
        
        // Save initial state
        viewModelScope.launch {
            gameState.saveState(getApplication())
        }
    }

    /**
     * Places a tile at the specified position if valid.
     * Updates state and checks for win condition.
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
            
            // Clear saved state if game is over
            viewModelScope.launch {
                gameState.clearSavedState(getApplication())
            }
            
            return
        }
        
        // Check for draw
        if (gameState.isBoardFull()) {
            winner = DRAW
            
            // Clear saved state if game is over
            viewModelScope.launch {
                gameState.clearSavedState(getApplication())
            }
            
            return
        }
        
        // Save state after move
        viewModelScope.launch {
            gameState.saveState(getApplication())
        }
        
        // Make computer move if playing against computer
        if (gameState.againstComputer && winner == null) {
            makeComputerMove()
        }
    }
    
    /**
     * Places a Connect4 tile in the specified column
     */
    fun placeConnect4Tile(col: Int) {
        if (winner != null || isLoading) {
            return
        }
        
        // Find the bottom-most empty row in the column
        val row = findBottomEmptyRow(col)
        if (row == -1) return // Column is full
        
        // Place the tile normally
        placeTile(row, col)
    }
    
    /**
     * Finds the bottom-most empty row in a column for Connect4
     */
    private fun findBottomEmptyRow(col: Int): Int {
        for (row in gameState.boardSize - 1 downTo 0) {
            if (gameState.board[row][col] == GameState.EMPTY) {
                return row
            }
        }
        return -1 // Column is full
    }
    
    /**
     * Makes a computer move based on the current game type
     */
    private fun makeComputerMove() {
        if (winner != null || isLoading) return
        
        isLoading = true
        
        viewModelScope.launch {
            // Short delay to simulate thinking (and avoid UI flicker)
            kotlinx.coroutines.delay(700)
            
            when {
                // Perfect play for Tic Tac Toe (3x3)
                gameState.boardSize == 3 && gameState.winCondition == 3 -> {
                    makeComputerMoveTicTacToe()
                }
                // Basic AI for Connect 4 (7x7)
                gameState.boardSize == 7 && gameState.winCondition == 4 -> {
                    makeComputerMoveConnect4()
                }
                // Advanced heuristic-based AI for Wuziqi
                else -> {
                    makeComputerMoveWuziqi()
                }
            }
            
            isLoading = false
        }
    }
    
    /**
     * Plays the sound effect for placing a tile.
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
     * If playing against the computer, undoes both player's and computer's moves.
     */
    fun undoMove() {
        if (moveHistory.isEmpty() || winner != null || isLoading) {
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

        // Play sound effect if enabled
        if (soundEnabled) {
            soundPool.play(soundUndo, 0.7f, 0.7f, 1, 0, 1.0f)
        }

        // Remove the move from history
        moveHistory = moveHistory.dropLast(1)
        
        // If playing against computer, undo the computer's move as well
        if (gameState.againstComputer && moveHistory.isNotEmpty() && 
            moveHistory.last().player != gameState.currentPlayer) {
            undoMove()
        }
        
        // Save state after undo
        viewModelScope.launch {
            gameState.saveState(getApplication())
        }
    }

    /**
     * Resets the game to initial state.
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
        
        // Clear saved state
        viewModelScope.launch {
            gameState.clearSavedState(getApplication())
        }
    }

    /**
     * Dismisses the winner dialog without resetting the game.
     */
    fun dismissWinnerDialog() {
        winner = null
    }
    
    /**
     * Makes an optimal computer move for Tic Tac Toe using minimax algorithm
     */
    private fun makeComputerMoveTicTacToe() {
        val boardSize = gameState.boardSize
        val computerPlayer = GameState.PLAYER_TWO
        val humanPlayer = GameState.PLAYER_ONE
        
        // First, try to win
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.board[row][col] == GameState.EMPTY) {
                    // Try this move
                    gameState.board[row][col] = computerPlayer
                    if (gameState.checkWin(row, col, computerPlayer)) {
                        // We can win, make this move
                        gameState.board[row][col] = GameState.EMPTY // Reset for proper handling
                        placeTile(row, col)
                        return
                    }
                    // Undo try
                    gameState.board[row][col] = GameState.EMPTY
                }
            }
        }
        
        // Next, block the human from winning
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.board[row][col] == GameState.EMPTY) {
                    // Try this move for the human
                    gameState.board[row][col] = humanPlayer
                    if (gameState.checkWin(row, col, humanPlayer)) {
                        // Block this winning move
                        gameState.board[row][col] = GameState.EMPTY // Reset for proper handling
                        placeTile(row, col)
                        return
                    }
                    // Undo try
                    gameState.board[row][col] = GameState.EMPTY
                }
            }
        }
        
        // Take center if available
        if (gameState.board[1][1] == GameState.EMPTY) {
            placeTile(1, 1)
            return
        }
        
        // Take a corner if available
        val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
        val availableCorners = corners.filter { (row, col) -> 
            gameState.board[row][col] == GameState.EMPTY 
        }
        
        if (availableCorners.isNotEmpty()) {
            val (row, col) = availableCorners[random.nextInt(availableCorners.size)]
            placeTile(row, col)
            return
        }
        
        // Take any available edge
        val edges = listOf(Pair(0, 1), Pair(1, 0), Pair(1, 2), Pair(2, 1))
        val availableEdges = edges.filter { (row, col) -> 
            gameState.board[row][col] == GameState.EMPTY 
        }
        
        if (availableEdges.isNotEmpty()) {
            val (row, col) = availableEdges[random.nextInt(availableEdges.size)]
            placeTile(row, col)
            return
        }
    }
    
    /**
     * Makes a reasonable computer move for Connect 4
     */
    private fun makeComputerMoveConnect4() {
        val boardSize = gameState.boardSize
        val computerPlayer = GameState.PLAYER_TWO
        val humanPlayer = GameState.PLAYER_ONE
        
        // First check for immediate win
        for (col in 0 until boardSize) {
            val row = findBottomEmptyRow(col)
            if (row != -1) {
                // Try move
                gameState.board[row][col] = computerPlayer
                if (gameState.checkWin(row, col, computerPlayer)) {
                    // We can win with this move
                    gameState.board[row][col] = GameState.EMPTY
                    placeConnect4Tile(col)
                    return
                }
                gameState.board[row][col] = GameState.EMPTY
            }
        }
        
        // Block human win
        for (col in 0 until boardSize) {
            val row = findBottomEmptyRow(col)
            if (row != -1) {
                // Try move for human
                gameState.board[row][col] = humanPlayer
                if (gameState.checkWin(row, col, humanPlayer)) {
                    // Block this winning move
                    gameState.board[row][col] = GameState.EMPTY
                    placeConnect4Tile(col)
                    return
                }
                gameState.board[row][col] = GameState.EMPTY
            }
        }
        
        // Favor center column
        val centerCol = boardSize / 2
        if (findBottomEmptyRow(centerCol) != -1) {
            placeConnect4Tile(centerCol)
            return
        }
        
        // Otherwise, prioritize columns near the center
        val colPriorities = (0 until boardSize).sortedBy { Math.abs(it - centerCol) }
        for (col in colPriorities) {
            if (findBottomEmptyRow(col) != -1) {
                placeConnect4Tile(col)
                return
            }
        }
    }
    
    /**
     * Makes a strategic computer move for Wuziqi using the pattern-based heuristic approach
     */
    private fun makeComputerMoveWuziqi() {
        val boardSize = gameState.boardSize
        val computerPlayer = GameState.PLAYER_TWO
        val humanPlayer = GameState.PLAYER_ONE
        
        // 1. Find the best move based on the heuristic pattern evaluation
        var bestScore = Int.MIN_VALUE
        var bestMove: Pair<Int, Int>? = null
        
        // Only consider positions near existing pieces to reduce search space
        val candidates = findCandidateMoves()
        
        // If no candidate moves (empty board), place in center
        if (candidates.isEmpty()) {
            val center = boardSize / 2
            placeTile(center, center)
            return
        }
        
        // Evaluate each candidate position
        for ((row, col) in candidates) {
            if (gameState.board[row][col] == GameState.EMPTY) {
                // Simulate placing the piece
                gameState.board[row][col] = computerPlayer
                
                // Evaluate the position
                val score = evaluatePosition(computerPlayer)
                
                // Restore the board
                gameState.board[row][col] = GameState.EMPTY
                
                // Update best move
                if (score > bestScore) {
                    bestScore = score
                    bestMove = Pair(row, col)
                }
            }
        }
        
        // Make the best move
        bestMove?.let { (row, col) ->
            placeTile(row, col)
        } ?: run {
            // Fall back to any valid move if no best move found
            val emptyPositions = mutableListOf<Pair<Int, Int>>()
            for (row in 0 until boardSize) {
                for (col in 0 until boardSize) {
                    if (gameState.board[row][col] == GameState.EMPTY) {
                        emptyPositions.add(Pair(row, col))
                    }
                }
            }
            
            if (emptyPositions.isNotEmpty()) {
                val (row, col) = emptyPositions[random.nextInt(emptyPositions.size)]
                placeTile(row, col)
            }
        }
    }
    
    /**
     * Finds candidate moves for the Wuziqi AI (positions adjacent to existing pieces)
     */
    private fun findCandidateMoves(): List<Pair<Int, Int>> {
        val boardSize = gameState.boardSize
        val candidates = mutableSetOf<Pair<Int, Int>>()
        
        // Directions to check (8 directions)
        val directions = listOf(
            -1 to -1, -1 to 0, -1 to 1,
            0 to -1, 0 to 1,
            1 to -1, 1 to 0, 1 to 1
        )
        
        // Find all positions adjacent to existing pieces
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.board[row][col] != GameState.EMPTY) {
                    // Check all adjacent positions
                    for ((dr, dc) in directions) {
                        val newRow = row + dr
                        val newCol = col + dc
                        
                        // Add valid empty positions
                        if (gameState.isValidPosition(newRow, newCol) && 
                            gameState.board[newRow][newCol] == GameState.EMPTY) {
                            candidates.add(Pair(newRow, newCol))
                        }
                    }
                }
            }
        }
        
        return candidates.toList()
    }
    
    /**
     * Evaluates a board position for the given player using pattern-based heuristics
     */
    private fun evaluatePosition(player: Int): Int {
        val opponent = if (player == GameState.PLAYER_ONE) GameState.PLAYER_TWO else GameState.PLAYER_ONE
        var totalScore = 0
        
        // Evaluate horizontal, vertical, and diagonal lines
        totalScore += evaluateLines(player, opponent)
        
        return totalScore
    }
    
    /**
     * Evaluates a line for pattern matching
     * Using the pattern-based approach described in requirements:
     * x = player's stone, o = opponent's stone, - = empty position
     */
    private fun evaluateLinePatterns(line: String): Int {
        var score = 0
        
        // Player patterns (offensive)
        if (line.contains("xxxxx")) score += 10000000  // Five in a row (win)
        if (line.contains("-xxxx-")) score += 9999999  // Open four
        if (line.contains("xxxx-")) score += 999999    // Closed four (one side)
        if (line.contains("-xxxx")) score += 999999    // Closed four (other side)
        if (line.contains("-xxx-")) score += 50000     // Open three
        if (line.contains("-xx-")) score += 500        // Open two
        
        // Opponent patterns (defensive - negative scores)
        if (line.contains("ooooo")) score -= 10000000  // Five in a row (opponent win)
        if (line.contains("-oooo-")) score -= 9999999  // Open four (urgent defense needed)
        if (line.contains("oooo-")) score -= 999999    // Closed four (one side)
        if (line.contains("-oooo")) score -= 999999    // Closed four (other side)
        if (line.contains("-ooo-")) score -= 999999    // Open three (urgent threat)
        if (line.contains("-ooo--")) score -= 999999   // Open three (with extra space)
        if (line.contains("--ooo-")) score -= 999999   // Open three (with extra space other side)
        if (line.contains("-oo-")) score -= 2000       // Open two
        
        return score
    }
    
    /**
     * Evaluates all lines on the board (horizontal, vertical, diagonals)
     */
    private fun evaluateLines(player: Int, opponent: Int): Int {
        var score = 0
        val boardSize = gameState.boardSize
        val winLength = gameState.winCondition
        
        // Horizontal lines
        for (row in 0 until boardSize) {
            val line = StringBuilder()
            for (col in 0 until boardSize) {
                line.append(when (gameState.board[row][col]) {
                    player -> 'x'
                    opponent -> 'o'
                    else -> '-'
                })
            }
            score += evaluateLinePatterns(line.toString())
        }
        
        // Vertical lines
        for (col in 0 until boardSize) {
            val line = StringBuilder()
            for (row in 0 until boardSize) {
                line.append(when (gameState.board[row][col]) {
                    player -> 'x'
                    opponent -> 'o'
                    else -> '-'
                })
            }
            score += evaluateLinePatterns(line.toString())
        }
        
        // Diagonal lines (top-left to bottom-right)
        for (start in 0 until 2 * boardSize - 1) {
            val line = StringBuilder()
            val row = max(0, start - boardSize + 1)
            val col = min(start, boardSize - 1)
            
            var r = row
            var c = col
            while (r < boardSize && c >= 0) {
                line.append(when (gameState.board[r][c]) {
                    player -> 'x'
                    opponent -> 'o'
                    else -> '-'
                })
                r++
                c--
            }
            
            if (line.length >= winLength) {
                score += evaluateLinePatterns(line.toString())
            }
        }
        
        // Diagonal lines (top-right to bottom-left)
        for (start in 0 until 2 * boardSize - 1) {
            val line = StringBuilder()
            val row = max(0, start - boardSize + 1)
            val col = max(0, boardSize - 1 - start)
            
            var r = row
            var c = col
            while (r < boardSize && c < boardSize) {
                line.append(when (gameState.board[r][c]) {
                    player -> 'x'
                    opponent -> 'o'
                    else -> '-'
                })
                r++
                c++
            }
            
            if (line.length >= winLength) {
                score += evaluateLinePatterns(line.toString())
            }
        }
        
        return score
    }
}