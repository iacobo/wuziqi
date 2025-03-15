package com.iacobo.wuziqi.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Represents the state of a Wuziqi game.
 * Manages the board, current player, and win condition checking.
 */
class GameState {
    companion object {
        const val EMPTY = 0
        const val PLAYER_ONE = 1 // Black
        const val PLAYER_TWO = 2 // White
        const val DEFAULT_BOARD_SIZE = 15 // Standard wuziqi board size
        const val DEFAULT_WIN_CONDITION = 5 // Number of consecutive pieces needed to win
        
        // Keys for saved state
        private const val PREFS_NAME = "wuziqi_game_state"
        private const val KEY_BOARD = "board"
        private const val KEY_CURRENT_PLAYER = "current_player"
        private const val KEY_BOARD_SIZE = "board_size"
        private const val KEY_WIN_CONDITION = "win_condition"
        private const val KEY_AGAINST_COMPUTER = "against_computer"
        private const val KEY_EASTER_EGGS = "easter_eggs"
    }

    var board: Array<IntArray>
    var currentPlayer: Int = PLAYER_ONE
    var boardSize: Int = DEFAULT_BOARD_SIZE
    var winCondition: Int = DEFAULT_WIN_CONDITION
    var againstComputer: Boolean = false

    // Initialize with default values
    init {
        board = Array(boardSize) { IntArray(boardSize) { EMPTY } }
    }
    
    // Constructor with custom board size and win condition
    constructor(boardSize: Int = DEFAULT_BOARD_SIZE, winCondition: Int = DEFAULT_WIN_CONDITION, againstComputer: Boolean = false) {
        this.boardSize = boardSize
        this.winCondition = winCondition
        this.againstComputer = againstComputer
        this.board = Array(boardSize) { IntArray(boardSize) { EMPTY } }
    }

    /**
     * Places a tile on the board and switches the current player.
     *
     * @param row The row position
     * @param col The column position
     * @return True if the tile was placed successfully, false otherwise
     */
    fun placeTile(row: Int, col: Int): Boolean {
        if (!isTileEmpty(row, col)) return false
        
        board[row][col] = currentPlayer
        currentPlayer = if (currentPlayer == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        return true
    }

    /**
     * Checks if a tile is empty.
     *
     * @param row The row position
     * @param col The column position
     * @return True if the tile is empty, false otherwise
     */
    fun isTileEmpty(row: Int, col: Int): Boolean = board[row][col] == EMPTY

    /**
     * Resets the game state to initial values.
     */
    fun reset() {
        board = Array(boardSize) { IntArray(boardSize) { EMPTY } }
        currentPlayer = PLAYER_ONE
    }

    /**
     * Checks if the last placed piece created a winning condition.
     *
     * @param row The row of the last placed piece
     * @param col The column of the last placed piece
     * @param playerValue The player value to check for
     * @return True if the player has won, false otherwise
     */
    fun checkWin(row: Int, col: Int, playerValue: Int): Boolean {
        return checkDirection(row, col, 1, 0, playerValue) || // Horizontal
               checkDirection(row, col, 0, 1, playerValue) || // Vertical
               checkDirection(row, col, 1, 1, playerValue) || // Diagonal \
               checkDirection(row, col, 1, -1, playerValue)   // Diagonal /
    }

    /**
     * Checks for win condition in a specific direction.
     */
    private fun checkDirection(row: Int, col: Int, deltaRow: Int, deltaCol: Int, playerValue: Int): Boolean {
        var count = 1 // Start with 1 for the piece itself

        // Check in the positive direction
        count += countInDirection(row, col, deltaRow, deltaCol, playerValue)

        // Check in the negative direction
        count += countInDirection(row, col, -deltaRow, -deltaCol, playerValue)

        return count >= winCondition
    }

    /**
     * Counts consecutive pieces in a specific direction.
     */
    private fun countInDirection(row: Int, col: Int, deltaRow: Int, deltaCol: Int, playerValue: Int): Int {
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
     * Checks if the board is full (draw condition)
     */
    fun isBoardFull(): Boolean {
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] == EMPTY) {
                    return false
                }
            }
        }
        return true
    }
    
    /**
     * Checks if a position is valid on the board
     */
    fun isValidPosition(row: Int, col: Int): Boolean {
        return row in 0 until boardSize && col in 0 until boardSize
    }
    
    /**
     * Saves the current game state to persistent storage
     */
    suspend fun saveState(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        // Convert board to string
        val boardStr = StringBuilder()
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                boardStr.append(board[row][col])
                if (col < boardSize - 1) boardStr.append(",")
            }
            if (row < boardSize - 1) boardStr.append(";")
        }
        
        editor.putString(KEY_BOARD, boardStr.toString())
        editor.putInt(KEY_CURRENT_PLAYER, currentPlayer)
        editor.putInt(KEY_BOARD_SIZE, boardSize)
        editor.putInt(KEY_WIN_CONDITION, winCondition)
        editor.putBoolean(KEY_AGAINST_COMPUTER, againstComputer)
        editor.apply()
    }
    
    /**
     * Loads a saved game state from persistent storage
     * Returns true if a state was loaded, false otherwise
     */
    suspend fun loadState(context: Context): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val boardStr = prefs.getString(KEY_BOARD, null) ?: return@withContext false
        
        // Load board configuration
        boardSize = prefs.getInt(KEY_BOARD_SIZE, DEFAULT_BOARD_SIZE)
        winCondition = prefs.getInt(KEY_WIN_CONDITION, DEFAULT_WIN_CONDITION)
        againstComputer = prefs.getBoolean(KEY_AGAINST_COMPUTER, false)
        
        // Parse board from string
        board = Array(boardSize) { IntArray(boardSize) { EMPTY } }
        val rows = boardStr.split(";")
        for (i in rows.indices) {
            val cols = rows[i].split(",")
            for (j in cols.indices) {
                board[i][j] = cols[j].toInt()
            }
        }
        
        currentPlayer = prefs.getInt(KEY_CURRENT_PLAYER, PLAYER_ONE)
        true
    }
    
    /**
     * Clears saved game state
     */
    suspend fun clearSavedState(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_BOARD).apply()
    }
    
    /**
     * Helper class for persistent storage of discovered easter eggs
     */
    class EasterEggManager(private val context: Context) {
        private val prefs: SharedPreferences by lazy {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        
        fun getDiscoveredEasterEggs(): Set<String> {
            return prefs.getStringSet(KEY_EASTER_EGGS, emptySet()) ?: emptySet()
        }
        
        fun addDiscoveredEasterEgg(eggName: String) {
            val currentEggs = getDiscoveredEasterEggs().toMutableSet()
            currentEggs.add(eggName)
            prefs.edit().putStringSet(KEY_EASTER_EGGS, currentEggs).apply()
        }
    }
}