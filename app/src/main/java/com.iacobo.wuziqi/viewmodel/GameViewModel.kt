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
import kotlin.math.abs
import kotlin.math.pow

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
        
        // Only trigger computer move if this wasn't already a computer move
        // This prevents infinite recursion
        if (gameState.againstComputer && winner == null && !bypassLoading) {
            makeComputerMove()
        }
    }
    
    /**
    * Finds the bottom-most empty row in a column for Connect4
    * Updated to handle a 7x6 board size (width x height)
    */
    private fun findBottomEmptyRow(col: Int): Int {
        // For Connect4 (7x6 board), we need to only check the 6 rows (0-5)
        val maxRow = if (gameState.boardSize == 7 && gameState.winCondition == 4) {
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
    * Places a Connect4 tile in the specified column
    * Updated with bypassLoading parameter for AI moves
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
            viewModelScope.launch {
                gameState.clearSavedState(getApplication())
            }
            
            return
        }
        
        // Check for draw - for Connect4, we need to check if all columns are full
        var isFull = true
        for (c in 0 until min(gameState.boardSize, BOARD_WIDTH)) {
            if (findBottomEmptyRow(c) != -1) {
                isFull = false
                break
            }
        }
        
        if (isFull) {
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
        // Only do this for human moves (not AI moves)
        if (gameState.againstComputer && winner == null && !bypassLoading) {
            makeComputerMoveConnect4WithBitboard()
        }
    }
    
    /**
    * Makes a computer move based on the current game type.
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
                        placeTile(row, col, bypassLoading = true)  // Use bypassLoading
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
                        placeTile(row, col, bypassLoading = true)  // Use bypassLoading
                        return
                    }
                    // Undo try
                    gameState.board[row][col] = GameState.EMPTY
                }
            }
        }
        
        // Take center if available
        if (gameState.board[1][1] == GameState.EMPTY) {
            placeTile(1, 1, bypassLoading = true)  // Use bypassLoading
            return
        }
        
        // Take a corner if available
        val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
        val availableCorners = corners.filter { (row, col) -> 
            gameState.board[row][col] == GameState.EMPTY 
        }
        
        if (availableCorners.isNotEmpty()) {
            val (row, col) = availableCorners[random.nextInt(availableCorners.size)]
            placeTile(row, col, bypassLoading = true)  // Use bypassLoading
            return
        }
        
        // Take any available edge
        val edges = listOf(Pair(0, 1), Pair(1, 0), Pair(1, 2), Pair(2, 1))
        val availableEdges = edges.filter { (row, col) -> 
            gameState.board[row][col] == GameState.EMPTY 
        }
        
        if (availableEdges.isNotEmpty()) {
            val (row, col) = availableEdges[random.nextInt(availableEdges.size)]
            placeTile(row, col, bypassLoading = true)  // Use bypassLoading
            return
        }
    }

    /**
    * Helper method to check win condition on a temporary board
    */
    private fun checkWinOnTempBoard(
        board: Array<IntArray>,
        row: Int,
        col: Int,
        playerValue: Int
    ): Boolean {
        // Check horizontal
        var count = 0
        for (c in 0 until board.size) {
            if (board[row][c] == playerValue) {
                count++
                if (count >= gameState.winCondition) return true
            } else {
                count = 0
            }
        }
        
        // Check vertical
        count = 0
        for (r in 0 until board.size) {
            if (board[r][col] == playerValue) {
                count++
                if (count >= gameState.winCondition) return true
            } else {
                count = 0
            }
        }
        
        // Check diagonal \
        count = 0
        val minDiag1 = row.coerceAtMost(col)
        var r = row - minDiag1
        var c = col - minDiag1
        while (r < board.size && c < board.size) {
            if (board[r][c] == playerValue) {
                count++
                if (count >= gameState.winCondition) return true
            } else {
                count = 0
            }
            r++
            c++
        }
        
        // Check diagonal /
        count = 0
        val minDiag2 = row.coerceAtMost(board.size - 1 - col)
        r = row - minDiag2
        c = col + minDiag2
        while (r < board.size && c >= 0) {
            if (board[r][c] == playerValue) {
                count++
                if (count >= gameState.winCondition) return true
            } else {
                count = 0
            }
            r++
            c--
        }
        
        return false
    }

    /**
    * Helper method to check for a win on a temporary board without modifying game state.
    * This prevents state mutation issues during AI trial moves.
    */
    private fun checkWinOnBoard(
        board: Array<IntArray>,
        row: Int,
        col: Int,
        playerValue: Int,
        winLength: Int
    ): Boolean {
        // Check horizontal, vertical, diagonal \, and diagonal /
        return checkDirectionOnBoard(board, row, col, 1, 0, playerValue, winLength) || // Horizontal
            checkDirectionOnBoard(board, row, col, 0, 1, playerValue, winLength) || // Vertical
            checkDirectionOnBoard(board, row, col, 1, 1, playerValue, winLength) || // Diagonal \
            checkDirectionOnBoard(board, row, col, 1, -1, playerValue, winLength)   // Diagonal /
    }

    /**
    * Counts pieces in a direction on a temporary board
    */
    private fun checkDirectionOnBoard(
        board: Array<IntArray>,
        row: Int,
        col: Int,
        deltaRow: Int,
        deltaCol: Int,
        playerValue: Int,
        winLength: Int
    ): Boolean {
        val boardSize = board.size
        var count = 1 // Start with 1 for the piece itself

        // Check in positive direction
        count += countInDirectionOnBoard(board, row, col, deltaRow, deltaCol, playerValue, boardSize)
        
        // Check in negative direction
        count += countInDirectionOnBoard(board, row, col, -deltaRow, -deltaCol, playerValue, boardSize)

        return count >= winLength
    }

    /**
    * Helper method to count consecutive pieces in a direction on a temporary board
    */
    private fun countInDirectionOnBoard(
        board: Array<IntArray>,
        row: Int,
        col: Int,
        deltaRow: Int,
        deltaCol: Int,
        playerValue: Int,
        boardSize: Int
    ): Int {
        var count = 0
        var r = row + deltaRow
        var c = col + deltaCol

        while (r in 0 until boardSize && c in 0 until boardSize && board[r][c] == playerValue) {
            count++
            r += deltaRow
            c += deltaCol
        }

        return count
    }

    /**
    * Implementation of Connect4 AI using bitboards and alpha-beta pruning.
    * Based on the optimizations described in the article.
    */

    // Constants for the bitboard representation
    private const val BOARD_WIDTH = 7
    private const val BOARD_HEIGHT = 6
    private const val BOARD_SIZE = BOARD_WIDTH * BOARD_HEIGHT

    // Scoring constants
    private const val WIN_SCORE = 1000000
    private const val DRAW_SCORE = 0
    private const val LOSE_SCORE = -WIN_SCORE

    // Search depth (adjust as needed for performance)
    private const val SEARCH_DEPTH = 7

    /**
    * BitboardConnect4 encapsulates the Connect4 game logic using bitboards
    * for efficient representation and move generation.
    */
    class BitboardConnect4 {
        // Two bitboards - one for each player
        private var bitboards = LongArray(2) { 0L }
        
        // Counter for tracking whose turn it is
        private var counter = 0
        
        // Heights array - tracks the next available position in each column
        private var heights = IntArray(BOARD_WIDTH) { col -> col * (BOARD_HEIGHT + 1) }
        
        // Top positions for each column - used to check if a column is full
        private val topPositions = IntArray(BOARD_WIDTH) { col -> col * (BOARD_HEIGHT + 1) + BOARD_HEIGHT }
        
        // Scoring map for heuristic evaluation
        private val scoreMap = arrayOf(
            intArrayOf(3, 4, 5, 7, 5, 4, 3),
            intArrayOf(4, 6, 8, 9, 8, 6, 4),
            intArrayOf(5, 8, 11, 13, 11, 8, 5),
            intArrayOf(5, 8, 11, 13, 11, 8, 5),
            intArrayOf(4, 6, 8, 9, 8, 6, 4),
            intArrayOf(3, 4, 5, 7, 5, 4, 3)
        )
        
        /**
        * Reset the game state
        */
        fun reset() {
            bitboards[0] = 0L
            bitboards[1] = 0L
            counter = 0
            for (i in 0 until BOARD_WIDTH) {
                heights[i] = i * (BOARD_HEIGHT + 1)
            }
        }
        
        /**
        * Get a copy of the current game state
        */
        fun copy(): BitboardConnect4 {
            val copy = BitboardConnect4()
            copy.bitboards[0] = bitboards[0]
            copy.bitboards[1] = bitboards[1]
            copy.counter = counter
            System.arraycopy(heights, 0, copy.heights, 0, BOARD_WIDTH)
            return copy
        }
        
        /**
        * Check if a column is valid for a move
        */
        fun isValidMove(col: Int): Boolean {
            return col in 0 until BOARD_WIDTH && heights[col] < topPositions[col]
        }
        
        /**
        * Make a move for the current player in the specified column
        */
        fun makeMove(col: Int): Boolean {
            if (!isValidMove(col)) return false
            
            // Set the appropriate bit for the current player
            val currentPlayer = counter and 1
            bitboards[currentPlayer] = bitboards[currentPlayer] or (1L shl heights[col])
            
            // Update the counter and the height of the column
            counter++
            heights[col]++
            
            return true
        }
        
        /**
        * Undo the last move
        */
        fun undoMove(col: Int): Boolean {
            if (counter == 0 || heights[col] <= col * (BOARD_HEIGHT + 1)) return false
            
            // Decrement the counter and height
            counter--
            heights[col]--
            
            // Clear the appropriate bit for the player who just moved
            val currentPlayer = counter and 1
            bitboards[currentPlayer] = bitboards[currentPlayer] and (1L shl heights[col]).inv()
            
            return true
        }
        
        /**
        * Check if the last move resulted in a win
        */
        fun checkWin(col: Int): Boolean {
            val player = (counter - 1) and 1
            val board = bitboards[player]
            
            // Check horizontal
            if ((board and (board shr 1) and (board shr 2) and (board shr 3)) != 0L) return true
            
            // Check vertical
            if ((board and (board shr (BOARD_HEIGHT + 1)) and (board shr (2 * (BOARD_HEIGHT + 1))) and (board shr (3 * (BOARD_HEIGHT + 1)))) != 0L) return true
            
            // Check positive diagonal
            if ((board and (board shr (BOARD_HEIGHT + 2)) and (board shr (2 * (BOARD_HEIGHT + 2))) and (board shr (3 * (BOARD_HEIGHT + 2)))) != 0L) return true
            
            // Check negative diagonal
            if ((board and (board shr BOARD_HEIGHT) and (board shr (2 * BOARD_HEIGHT)) and (board shr (3 * BOARD_HEIGHT))) != 0L) return true
            
            return false
        }
        
        /**
        * Check if the board is full
        */
        fun isFull(): Boolean {
            for (col in 0 until BOARD_WIDTH) {
                if (heights[col] < topPositions[col]) return false
            }
            return true
        }
        
        /**
        * Convert the bitboard to a standard 2D array board representation
        */
        fun toBoardArray(): Array<IntArray> {
            val board = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }
            
            for (row in 0 until BOARD_HEIGHT) {
                for (col in 0 until BOARD_WIDTH) {
                    val bitPosition = col * (BOARD_HEIGHT + 1) + row
                    val bit1 = (bitboards[0] shr bitPosition) and 1L
                    val bit2 = (bitboards[1] shr bitPosition) and 1L
                    
                    board[row][col] = when {
                        bit1 == 1L -> 1 // Player 1
                        bit2 == 1L -> 2 // Player 2
                        else -> 0      // Empty
                    }
                }
            }
            
            return board
        }
        
        /**
        * Evaluate the current board position using the heuristic from the article
        */
        fun evaluate(): Int {
            // Check for terminal states first
            for (col in 0 until BOARD_WIDTH) {
                if (heights[col] > col * (BOARD_HEIGHT + 1)) {
                    val prevHeight = heights[col] - 1
                    val prevRow = prevHeight % (BOARD_HEIGHT + 1)
                    val prevCol = prevHeight / (BOARD_HEIGHT + 1)
                    
                    // Temporarily decrement to check the last move
                    heights[col]--
                    counter--
                    
                    if (checkWin(prevCol)) {
                        // Restore the state
                        heights[col]++
                        counter++
                        
                        // Return the win/loss score
                        val winner = (counter - 1) and 1
                        return if (winner == 0) WIN_SCORE else -WIN_SCORE
                    }
                    
                    // Restore the state
                    heights[col]++
                    counter++
                }
            }
            
            // If the board is full, it's a draw
            if (isFull()) return DRAW_SCORE
            
            // Use the heuristic from the article
            var score = 0
            
            // Convert to 2D array for easier heuristic calculation
            val board = toBoardArray()
            
            // Calculate the score for each player
            var player1Score = 0
            var player2Score = 0
            
            for (row in 0 until BOARD_HEIGHT) {
                for (col in 0 until BOARD_WIDTH) {
                    when (board[row][col]) {
                        1 -> player1Score += scoreMap[row][col] // Player 1 (White)
                        2 -> player2Score += scoreMap[row][col] // Player 2 (Black)
                    }
                }
            }
            
            // Return the score difference
            score = player1Score - player2Score
            
            // Adjust based on current player
            return if (counter and 1 == 0) score else -score
        }
    }
    
    /**
    * Connect4 AI that uses the bitboard representation and alpha-beta pruning
    */
    fun makeComputerMoveConnect4WithBitboard() {
        // If game is over or loading, return
        if (winner != null || isLoading) return
        
        isLoading = true
        
        viewModelScope.launch {
            // Short delay for UI responsiveness
            kotlinx.coroutines.delay(300)
            
            try {
                // Create a bitboard representation from the current game state
                val bitboardGame = BitboardConnect4()
                
                // Convert the current board to a bitboard
                val board = gameState.board
                for (row in 0 until gameState.boardSize) {
                    for (col in 0 until gameState.boardSize) {
                        if (board[row][col] != GameState.EMPTY) {
                            // Find the actual row in Connect4 representation (bottom-up)
                            val connect4Row = gameState.boardSize - 1 - row
                            if (connect4Row < BOARD_HEIGHT && col < BOARD_WIDTH) {
                                bitboardGame.makeMove(col)
                            }
                        }
                    }
                }
                
                // Find the best move using alpha-beta pruning
                val (_, bestCol) = alphaBetaNegamax(bitboardGame, SEARCH_DEPTH, Int.MIN_VALUE, Int.MAX_VALUE, true)
                
                // Convert column to the actual place in our game board
                // For Connect4, we need to find the bottom-most empty row in the column
                val row = findBottomEmptyRow(bestCol)
                if (row != -1) {
                    // Make the move
                    placeTile(row, bestCol, bypassLoading = true)
                } else {
                    // Column is full, fallback to any valid column
                    for (col in 0 until gameState.boardSize) {
                        val r = findBottomEmptyRow(col)
                        if (r != -1) {
                            placeTile(r, col, bypassLoading = true)
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to the original Connect4 AI in case of errors
                makeComputerMoveConnect4()
            } finally {
                isLoading = false
            }
        }
    }

    /**
    * Alpha-Beta Negamax algorithm for finding the best move
    * 
    * @param game The current game state
    * @param depth How deep to search
    * @param alpha The alpha value for pruning
    * @param beta The beta value for pruning
    * @param maximizingPlayer Whether the current player is maximizing or minimizing
    * @return Pair of (score, best column)
    */
    private fun alphaBetaNegamax(
        game: BitboardConnect4,
        depth: Int,
        alpha: Int,
        beta: Int,
        maximizingPlayer: Boolean
    ): Pair<Int, Int> {
        // If we've reached our search depth or the game is over
        if (depth == 0 || game.isFull()) {
            return Pair(game.evaluate(), -1)
        }
        
        var bestScore = if (maximizingPlayer) Int.MIN_VALUE else Int.MAX_VALUE
        var bestCol = -1
        var newAlpha = alpha
        var newBeta = beta
        
        // Try each column
        for (col in 0 until BOARD_WIDTH) {
            if (!game.isValidMove(col)) continue
            
            // Make a move
            game.makeMove(col)
            
            // Check for immediate win
            if (game.checkWin(col)) {
                // We found a winning move - use the maximum score
                val score = if (maximizingPlayer) WIN_SCORE else -WIN_SCORE
                
                // Undo move
                game.undoMove(col)
                
                // Return immediately with the winning move
                return Pair(score, col)
            }
            
            // Recursively evaluate the position
            val (score, _) = alphaBetaNegamax(game, depth - 1, newAlpha, newBeta, !maximizingPlayer)
            
            // Undo move
            game.undoMove(col)
            
            // Update best score and column
            if (maximizingPlayer) {
                if (score > bestScore) {
                    bestScore = score
                    bestCol = col
                }
                newAlpha = max(newAlpha, bestScore)
            } else {
                if (score < bestScore) {
                    bestScore = score
                    bestCol = col
                }
                newBeta = min(newBeta, bestScore)
            }
            
            // Alpha-beta pruning
            if (newBeta <= newAlpha) {
                break
            }
        }
        
        return Pair(bestScore, bestCol)
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
                    placeConnect4Tile(col, bypassLoading = true)  // Add bypassLoading parameter
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
                    placeConnect4Tile(col, bypassLoading = true)  // Add bypassLoading parameter
                    return
                }
                gameState.board[row][col] = GameState.EMPTY
            }
        }
        
        // Favor center column
        val centerCol = boardSize / 2
        if (findBottomEmptyRow(centerCol) != -1) {
            placeConnect4Tile(centerCol, bypassLoading = true)  // Add bypassLoading parameter
            return
        }
        
        // Otherwise, prioritize columns near the center
        val colPriorities = (0 until boardSize).sortedBy { abs(it - centerCol) }
        for (col in colPriorities) {
            if (findBottomEmptyRow(col) != -1) {
                placeConnect4Tile(col, bypassLoading = true)  // Add bypassLoading parameter
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
        
        // First check for immediate win or forced defense
        
        // 1. Look for winning moves (five or open four)
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    // Temporarily place computer's stone
                    gameState.board[row][col] = computerPlayer
                    
                    // Check for an immediate win
                    if (gameState.checkWin(row, col, computerPlayer)) {
                        // Win found, make this move
                        gameState.board[row][col] = GameState.EMPTY // Reset for proper handling
                        placeTile(row, col, bypassLoading = true)
                        return
                    }
                    
                    // Look for open four (which guarantees a win next move)
                    if (hasOpenFour(row, col, computerPlayer)) {
                        // Open four found, make this move
                        gameState.board[row][col] = GameState.EMPTY
                        placeTile(row, col, bypassLoading = true)
                        return
                    }
                    
                    // Reset board
                    gameState.board[row][col] = GameState.EMPTY
                }
            }
        }
        
        // 2. Check if opponent has a winning move that we must block
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    // Check if opponent would win here
                    gameState.board[row][col] = humanPlayer
                    if (gameState.checkWin(row, col, humanPlayer) || hasOpenFour(row, col, humanPlayer)) {
                        // Must block this threat
                        gameState.board[row][col] = GameState.EMPTY
                        placeTile(row, col, bypassLoading = true)
                        return
                    }
                    gameState.board[row][col] = GameState.EMPTY
                }
            }
        }
        
        // 3. Look for forcing threats (simple four, open three, broken three)
        var bestForcingMove: Pair<Int, Int>? = null
        var bestForcingScore = Int.MIN_VALUE
        
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    // Temporarily place stone and evaluate
                    gameState.board[row][col] = computerPlayer
                    val score = evaluatePosition(computerPlayer)
                    gameState.board[row][col] = GameState.EMPTY
                    
                    // For forcing threats, we're looking for scores above a certain threshold
                    if (score > 900000 && score > bestForcingScore) {
                        bestForcingScore = score
                        bestForcingMove = Pair(row, col)
                    }
                }
            }
        }
        
        if (bestForcingMove != null) {
            placeTile(bestForcingMove.first, bestForcingMove.second, bypassLoading = true)
            return
        }
        
        // 4. Find the best move based on the heuristic pattern evaluation
        var bestScore = Int.MIN_VALUE
        var bestMove: Pair<Int, Int>? = null
        
        // Only consider positions near existing pieces to reduce search space
        val candidates = findCandidateMoves()
        
        // If no candidate moves (empty board), place in center
        if (candidates.isEmpty()) {
            val center = boardSize / 2
            placeTile(center, center, bypassLoading = true)
            return
        }
        
        // Evaluate each candidate position
        for ((row, col) in candidates) {
            if (gameState.isTileEmpty(row, col)) {
                // Temporarily place stone and evaluate
                gameState.board[row][col] = computerPlayer
                val computerScore = evaluatePosition(computerPlayer)
                val humanScore = evaluatePosition(humanPlayer)
                gameState.board[row][col] = GameState.EMPTY
                
                // Final score is the difference (how good for computer minus how good for human)
                val score = computerScore - humanScore
                
                if (score > bestScore) {
                    bestScore = score
                    bestMove = Pair(row, col)
                }
            }
        }
        
        // Make the best move
        bestMove?.let { (row, col) ->
            placeTile(row, col, bypassLoading = true)
        } ?: run {
            // Fall back to any valid move if no best move found
            val emptyPositions = mutableListOf<Pair<Int, Int>>()
            for (row in 0 until boardSize) {
                for (col in 0 until boardSize) {
                    if (gameState.isTileEmpty(row, col)) {
                        emptyPositions.add(Pair(row, col))
                    }
                }
            }
            
            if (emptyPositions.isNotEmpty()) {
                val (row, col) = emptyPositions[random.nextInt(emptyPositions.size)]
                placeTile(row, col, bypassLoading = true)
            }
        }
    }

    /**
    * Helper function to check for open four at a position
    */
    private fun hasOpenFour(row: Int, col: Int, playerValue: Int): Boolean {
        val directions = listOf(
            Pair(1, 0),    // Horizontal
            Pair(0, 1),    // Vertical
            Pair(1, 1),    // Diagonal \
            Pair(1, -1)    // Diagonal /
        )
        
        for ((deltaRow, deltaCol) in directions) {
            if (hasOpenFourInDirection(row, col, deltaRow, deltaCol, playerValue)) {
                return true
            }
        }
        
        return false
    }

    /**
    * Check for open four in a specific direction
    */
    private fun hasOpenFourInDirection(row: Int, col: Int, deltaRow: Int, deltaCol: Int, playerValue: Int): Boolean {
        val boardSize = gameState.boardSize
        val opponent = if (playerValue == GameState.PLAYER_ONE) GameState.PLAYER_TWO else GameState.PLAYER_ONE
        
        // Extract the line in this direction
        val line = StringBuilder()
        
        // Look 5 spaces in each direction
        for (i in -5..5) {
            val r = row + i * deltaRow
            val c = col + i * deltaCol
            
            if (r in 0 until boardSize && c in 0 until boardSize) {
                when (gameState.board[r][c]) {
                    playerValue -> line.append("x")
                    opponent -> line.append("o")
                    else -> line.append("-")
                }
            } else {
                // Off-board positions are treated as blocked
                line.append("o")
            }
        }
        
        // Check for open four patterns
        val lineStr = line.toString()
        return lineStr.contains("-xxxx-") || 
            lineStr.contains("xx-xx") || 
            lineStr.contains("xxx-x") || 
            lineStr.contains("x-xxx")
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
    * Improved evaluation for checking all lines on the board.
    * This follows the approach described in the article where we evaluate 
    * each empty intersection and consider the two best threats in any direction.
    */
    private fun evaluatePosition(playerValue: Int): Int {
        val opponent = if (playerValue == GameState.PLAYER_ONE) GameState.PLAYER_TWO else GameState.PLAYER_ONE
        var totalScore = 0
        
        // For each empty intersection
        for (row in 0 until gameState.boardSize) {
            for (col in 0 until gameState.boardSize) {
                if (gameState.board[row][col] == GameState.EMPTY) {
                    // Calculate threat values if player places a stone here
                    val threatValues = mutableListOf<Int>()
                    
                    // Temporarily place player's stone to evaluate position
                    gameState.board[row][col] = playerValue
                    
                    // Check all 4 directions
                    threatValues.add(evaluateDirectionFromPosition(row, col, 1, 0, playerValue))  // Horizontal
                    threatValues.add(evaluateDirectionFromPosition(row, col, 0, 1, playerValue))  // Vertical
                    threatValues.add(evaluateDirectionFromPosition(row, col, 1, 1, playerValue))  // Diagonal \
                    threatValues.add(evaluateDirectionFromPosition(row, col, 1, -1, playerValue)) // Diagonal /
                    
                    // Remove temporary stone
                    gameState.board[row][col] = GameState.EMPTY
                    
                    // Sort threat values in descending order
                    threatValues.sortDescending()
                    
                    // Take the two best threats
                    val bestThreat = threatValues.getOrElse(0) { 0 }
                    val secondBestThreat = threatValues.getOrElse(1) { 0 }
                    
                    // Using the formula from the article: 1.5 * 1.8^a + 1.8^b
                    // where a and b are the threat values
                    if (bestThreat > 0 || secondBestThreat > 0) {
                        // Scale down the original threat values to work with the formula
                        val a = 16.coerceAtMost(bestThreat / 10000)
                        val b = 16.coerceAtMost(secondBestThreat / 10000)
                        
                        totalScore += (1.5 * 1.8.pow(a.toDouble()) + 1.8.pow(b.toDouble())).toInt()
                    }
                }
            }
        }
        
        // Add a bonus for the side to move as mentioned in the article
        if (gameState.currentPlayer == playerValue) {
            totalScore += 100
        }
        
        return totalScore
    }

    /**
    * Evaluates threats in a single direction from a position.
    * This extracts the line around the position and evaluates patterns.
    */
    private fun evaluateDirectionFromPosition(row: Int, col: Int, deltaRow: Int, deltaCol: Int, playerValue: Int): Int {
        val opponent = if (playerValue == GameState.PLAYER_ONE) GameState.PLAYER_TWO else GameState.PLAYER_ONE
        val boardSize = gameState.boardSize
        
        // Extract the line in this direction (with current player's stone at center)
        val line = StringBuilder()
        
        // Look 5 spaces in each direction
        for (i in -5..5) {
            val r = row + i * deltaRow
            val c = col + i * deltaCol
            
            if (r in 0 until boardSize && c in 0 until boardSize) {
                when (gameState.board[r][c]) {
                    playerValue -> line.append("x")
                    opponent -> line.append("o")
                    else -> line.append("-")
                }
            } else {
                // Off-board positions are treated as opponent stones (can't make five)
                line.append("o")
            }
        }
        
        // Evaluate patterns on this line
        return evaluateLinePatterns(line.toString())
    }
    
    /**
    * Evaluates a line for pattern matching using the threat classification from the article.
    * Each threat has a severity (number of stones out of 5) and a completion count (ways to make 5).
    * 
    * Threat categories from most to least severe:
    * - Winning threats:
    *   - Five (5,1): immediate win with 5 in a row
    *   - Open Four (4,2): two ways to make 5, can't be defended
    * - Forcing threats (require response):
    *   - Simple Four (4,1): one way to make 5, can be blocked
    *   - Open Three (3,3): three ways to complete, but only two are valid defenses
    *   - Broken Three (3,2): two ways to complete
    * - Non-forcing threats:
    *   - Simple Three (3,1): one way to complete
    *   - Various Twos (2,n): n ways to complete
    *   - Various Singles (1,n): n ways to complete
    */
    private fun evaluateLinePatterns(line: String): Int {
        var score = 0
        
        // WINNING THREATS - highest priority
        
        // Five (5,1) - immediate win
        if (line.contains("xxxxx")) score += 10000000  // Five in a row (win)
        
        // Open Four (4,2) - guaranteed win next move
        if (line.contains("-xxxx-")) score += 9000000  // Standard open four
        if (line.contains("xx-xx")) score += 9000000   // Split open four
        if (line.contains("xxx-x")) score += 9000000   // Non-standard open four with gap
        if (line.contains("x-xxx")) score += 9000000   // Non-standard open four with gap
        
        // FORCING THREATS - require immediate response
        
        // Simple Four (4,1) - one way to make 5
        if (line.contains("xxxx-")) score += 900000    // Simple four (one side)
        if (line.contains("-xxxx")) score += 900000    // Simple four (other side)
        if (line.contains("xx-xx-")) score += 900000   // Non-standard four (blocked on one side)
        if (line.contains("-xx-xx")) score += 900000   // Non-standard four (blocked on one side)
        if (line.contains("xxx-x-")) score += 900000   // Non-standard four (blocked on one side)
        if (line.contains("-xxx-x")) score += 900000   // Non-standard four (blocked on one side)
        if (line.contains("x-xxx-")) score += 900000   // Non-standard four (blocked on one side)
        if (line.contains("-x-xxx")) score += 900000   // Non-standard four (blocked on one side)
        
        // Open Three (3,3) - three ways to complete
        if (line.contains("--xxx--")) score += 90000   // Standard open three
        if (line.contains("-x-xx-")) score += 90000    // Non-standard open three, as in article example
        if (line.contains("-xx-x-")) score += 90000    // Non-standard open three, as in article example
        if (line.contains("-x--x-x--")) score += 90000 // Beautiful pattern from article example
        
        // Broken Three (3,2) - two ways to complete, mentioned as weaker than open three
        if (line.contains("-xxx-o")) score += 9000     // Broken three, blocked on one side
        if (line.contains("o-xxx-")) score += 9000     // Broken three, blocked on one side
        if (line.contains("-xx-x")) score += 9000      // Non-standard broken three
        if (line.contains("x-xx-")) score += 9000      // Non-standard broken three
        if (line.contains("-x-xx")) score += 9000      // Non-standard broken three
        if (line.contains("xx-x-")) score += 9000      // Non-standard broken three
        
        // NON-FORCING THREATS - don't require immediate response but still valuable
        
        // Simple Three (3,1) - one way to complete
        if (line.contains("xxx--o")) score += 900      // Simple three, blocked on one side
        if (line.contains("o--xxx")) score += 900      // Simple three, blocked on one side
        if (line.contains("xx-x--o")) score += 900     // Non-standard simple three
        if (line.contains("o--x-xx")) score += 900     // Non-standard simple three
        
        // Two (2,n) - two stones that can be extended n ways to five
        if (line.contains("--xx--")) score += 90       // Two that can be extended in 4 ways (2,4)
        if (line.contains("-xx---")) score += 80       // Two that can be extended in 3 ways (2,3)
        if (line.contains("---xx-")) score += 80       // Two that can be extended in 3 ways (2,3)
        if (line.contains("-x-x--")) score += 80       // Split two that can be extended in 3 ways
        if (line.contains("--x-x-")) score += 80       // Split two that can be extended in 3 ways
        if (line.contains("-xx--o")) score += 70       // Two that can be extended in 2 ways (2,2)
        if (line.contains("o--xx-")) score += 70       // Two that can be extended in 2 ways (2,2)
        if (line.contains("o-xx--o")) score += 60      // Two that can be extended in 1 way (2,1)
        
        // Single (1,n) - single stone with n ways to make 5
        if (line.contains("--x--")) score += 50        // Single stone with many ways to extend (1,5)
        if (line.contains("-x---")) score += 40        // Single stone with several ways to extend (1,4)
        if (line.contains("---x-")) score += 40        // Single stone with several ways to extend (1,4)
        
        // OPPONENT PATTERNS (defensive - negative scores)
        // Same patterns as above but for opponent (mirrored scores)
        
        // Winning threats
        if (line.contains("ooooo")) score -= 10000000  // Five in a row (opponent win)
        
        // Open Four (4,2) - guaranteed win next move
        if (line.contains("-oooo-")) score -= 9000000  // Standard open four
        if (line.contains("oo-oo")) score -= 9000000   // Split open four
        if (line.contains("ooo-o")) score -= 9000000   // Non-standard open four with gap
        if (line.contains("o-ooo")) score -= 9000000   // Non-standard open four with gap
        
        // FORCING THREATS - require immediate response
        
        // Simple Four (4,1) - one way to make 5
        if (line.contains("oooo-")) score -= 900000    // Simple four (one side)
        if (line.contains("-oooo")) score -= 900000    // Simple four (other side)
        if (line.contains("oo-oo-")) score -= 900000   // Non-standard four (blocked on one side)
        if (line.contains("-oo-oo")) score -= 900000   // Non-standard four (blocked on one side)
        if (line.contains("ooo-o-")) score -= 900000   // Non-standard four (blocked on one side)
        if (line.contains("-ooo-o")) score -= 900000   // Non-standard four (blocked on one side)
        if (line.contains("o-ooo-")) score -= 900000   // Non-standard four (blocked on one side)
        if (line.contains("-o-ooo")) score -= 900000   // Non-standard four (blocked on one side)
        
        // Open Three (3,3) - three ways to complete
        if (line.contains("--ooo--")) score -= 90000   // Standard open three
        if (line.contains("-o-oo-")) score -= 90000    // Non-standard open three, as in article example
        if (line.contains("-oo-o-")) score -= 90000    // Non-standard open three, as in article example
        if (line.contains("-o--o-o--")) score -= 90000 // Beautiful pattern from article example
        
        // Broken Three (3,2) - two ways to complete, mentioned as weaker than open three
        if (line.contains("-ooo-x")) score -= 9000     // Broken three, blocked on one side
        if (line.contains("x-ooo-")) score -= 9000     // Broken three, blocked on one side
        if (line.contains("-oo-o")) score -= 9000      // Non-standard broken three
        if (line.contains("o-oo-")) score -= 9000      // Non-standard broken three
        if (line.contains("-o-oo")) score -= 9000      // Non-standard broken three
        if (line.contains("oo-o-")) score -= 9000      // Non-standard broken three
        
        // NON-FORCING THREATS - don't require immediate response but still valuable
        
        // Simple Three (3,1) - one way to complete
        if (line.contains("ooo--x")) score -= 900      // Simple three, blocked on one side
        if (line.contains("x--ooo")) score -= 900      // Simple three, blocked on one side
        if (line.contains("oo-o--x")) score -= 900     // Non-standard simple three
        if (line.contains("x--o-oo")) score -= 900     // Non-standard simple three
        
        // Two (2,n) - two stones that can be extended n ways to five
        if (line.contains("--oo--")) score -= 90       // Two that can be extended in 4 ways (2,4)
        if (line.contains("-oo---")) score -= 80       // Two that can be extended in 3 ways (2,3)
        if (line.contains("---oo-")) score -= 80       // Two that can be extended in 3 ways (2,3)
        if (line.contains("-o-o--")) score -= 80       // Split two that can be extended in 3 ways
        if (line.contains("--o-o-")) score -= 80       // Split two that can be extended in 3 ways
        if (line.contains("-oo--x")) score -= 70       // Two that can be extended in 2 ways (2,2)
        if (line.contains("x--oo-")) score -= 70       // Two that can be extended in 2 ways (2,2)
        if (line.contains("x-oo--x")) score -= 60      // Two that can be extended in 1 way (2,1)
        
        // Single (1,n) - single stone with n ways to make 5
        if (line.contains("--o--")) score -= 50        // Single stone with many ways to extend (1,5)
        if (line.contains("-o---")) score -= 40        // Single stone with several ways to extend (1,4)
        if (line.contains("---o-")) score -= 40        // Single stone with several ways to extend (1,4)
        
        return score
    }
}