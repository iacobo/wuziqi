package com.iacobo.wuziqi.ai

import com.iacobo.wuziqi.data.GameState

/**
 * Interface for AI implementations for different game types.
 * This allows for a consistent approach to AI across different games.
 */
interface GameAI {
    /**
     * Finds the best move for the AI to make given the current game state.
     * 
     * @param gameState Current state of the game
     * @return A pair of (row, column) coordinates for the AI's move, or null if no move is available
     */
    fun findBestMove(gameState: GameState): Pair<Int, Int>?
}
