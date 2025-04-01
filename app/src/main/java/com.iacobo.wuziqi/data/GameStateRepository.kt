package com.iacobo.wuziqi.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for saving and loading game state to/from persistent storage.
 */
class GameStateRepository(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "wuziqi_game_state"
        private const val KEY_BOARD = "board"
        private const val KEY_CURRENT_PLAYER = "current_player"
        private const val KEY_BOARD_SIZE = "board_size"
        private const val KEY_WIN_CONDITION = "win_condition"
        private const val KEY_AGAINST_COMPUTER = "against_computer"
        private const val KEY_EASTER_EGGS = "easter_eggs"
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Saves the current game state to persistent storage.
     * 
     * @param gameState The current game state to save
     */
    suspend fun saveGameState(gameState: GameState) = withContext(Dispatchers.IO) {
        prefs.edit {
            // Convert board to string
            val boardStr = StringBuilder()
            for (row in 0 until gameState.boardSize) {
                for (col in 0 until gameState.boardSize) {
                    boardStr.append(gameState.board[row][col])
                    if (col < gameState.boardSize - 1) boardStr.append(",")
                }
                if (row < gameState.boardSize - 1) boardStr.append(";")
            }

            putString(KEY_BOARD, boardStr.toString())
            putInt(KEY_CURRENT_PLAYER, gameState.currentPlayer)
            putInt(KEY_BOARD_SIZE, gameState.boardSize)
            putInt(KEY_WIN_CONDITION, gameState.winCondition)
            putBoolean(KEY_AGAINST_COMPUTER, gameState.againstComputer)
        }
    }
    
    /**
     * Loads a saved game state from persistent storage.
     * 
     * @param gameState The game state object to populate with loaded data
     * @return True if a state was loaded, false otherwise
     */
    suspend fun loadGameState(gameState: GameState): Boolean = withContext(Dispatchers.IO) {
        val boardStr = prefs.getString(KEY_BOARD, null) ?: return@withContext false

        // Load board configuration
        gameState.boardSize = prefs.getInt(KEY_BOARD_SIZE, GameState.DEFAULT_BOARD_SIZE)
        gameState.winCondition = prefs.getInt(KEY_WIN_CONDITION, GameState.DEFAULT_WIN_CONDITION)
        gameState.againstComputer = prefs.getBoolean(KEY_AGAINST_COMPUTER, false)

        // Parse board from string
        val newBoard = Array(gameState.boardSize) { IntArray(gameState.boardSize) { GameState.EMPTY } }
        val rows = boardStr.split(";")
        for (i in rows.indices) {
            val cols = rows[i].split(",")
            for (j in cols.indices) {
                newBoard[i][j] = cols[j].toInt()
            }
        }

        // Update board
        for (row in 0 until gameState.boardSize) {
            for (col in 0 until gameState.boardSize) {
                gameState.board[row][col] = newBoard[row][col]
            }
        }

        gameState.currentPlayer = prefs.getInt(KEY_CURRENT_PLAYER, GameState.PLAYER_ONE)
        true
    }
    
    /**
     * Clears any saved game state.
     */
    suspend fun clearSavedState() = withContext(Dispatchers.IO) {
        prefs.edit { 
            remove(KEY_BOARD) 
        }
    }
    
    /**
     * Gets the set of discovered easter eggs.
     */
    fun getDiscoveredEasterEggs(): Set<String> {
        return prefs.getStringSet(KEY_EASTER_EGGS, emptySet()) ?: emptySet()
    }
    
    /**
     * Adds a new discovered easter egg.
     * 
     * @param eggName The name/identifier of the discovered easter egg
     */
    fun addDiscoveredEasterEgg(eggName: String) {
        val currentEggs = getDiscoveredEasterEggs().toMutableSet()
        currentEggs.add(eggName)
        prefs.edit { 
            putStringSet(KEY_EASTER_EGGS, currentEggs) 
        }
    }
}