package com.iacobo.wuziqi.ai

import com.iacobo.wuziqi.data.GameState
import java.util.Random
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * AI implementation for the Havannah game.
 * This AI uses a combination of pattern recognition and strategic evaluation.
 */
class HavannahAIEngine(private val random: Random) : GameAI {

    companion object {
        // Player constants
        const val EMPTY = GameState.EMPTY
        const val PLAYER_ONE = GameState.PLAYER_ONE // Human
        const val PLAYER_TWO = GameState.PLAYER_TWO // AI
        
        // Directions for neighbor cells in hexagonal grid
        val DIRECTIONS = arrayOf(
            Pair(-1, 0), // Top-left
            Pair(-1, 1), // Top-right
            Pair(0, -1), // Left
            Pair(0, 1),  // Right
            Pair(1, -1), // Bottom-left
            Pair(1, 0)   // Bottom-right
        )
        
        // Special positions with high strategic value for the 10x10 board
        val CORNER_POSITIONS = listOf(
            Pair(0, 0),                  // Top-left corner
            Pair(0, 9),                  // Top-right corner
            Pair(5, 9),                  // Right corner
            Pair(9, 5),                  // Bottom-right corner
            Pair(9, 0),                  // Bottom-left corner
            Pair(5, 0)                   // Left corner
        )
        
        // Edge positions (excluding corners) for the 10x10 board
        val EDGE_POSITIONS = listOf(
            // Top edge: row 0, cols 1-8
            (1..8).map { Pair(0, it) },
            // Top-right edge: cols 9, rows 1-4
            (1..4).map { Pair(it, 9) },
            // Bottom-right edge: cols 9, rows 6-8
            (6..8).map { Pair(it, 9) },
            // Bottom edge: row 9, cols 1-8
            (1..8).map { Pair(9, it) },
            // Bottom-left edge: col 0, rows 6-8
            (6..8).map { Pair(it, 0) },
            // Top-left edge: col 0, rows 1-4
            (1..4).map { Pair(it, 0) }
        ).flatten()
    }
    
    /**
     * Implementation of the GameAI interface method to find the best move.
     */
    override fun findBestMove(gameState: GameState): Pair<Int, Int>? {
        // If this is the first move, place near center
        if (isBoardEmpty(gameState)) {
            val center = gameState.boardSize / 2
            // Slightly randomize the first move
            val offset = random.nextInt(2) - 1
            return Pair(center + offset, center)
        }
        
        // Check for immediate winning move
        val winningMove = findWinningMove(gameState, PLAYER_TWO)
        if (winningMove != null) {
            return winningMove
        }
        
        // Check for immediate blocking move
        val blockingMove = findWinningMove(gameState, PLAYER_ONE)
        if (blockingMove != null) {
            return blockingMove
        }
        
        // Strategic move selection
        return findStrategicMove(gameState)
    }
    
    /**
     * Checks if the board is empty.
     */
    private fun isBoardEmpty(gameState: GameState): Boolean {
        for (row in 0 until gameState.boardSize) {
            for (col in 0 until gameState.boardSize) {
                if (gameState.board[row][col] != EMPTY) {
                    return false
                }
            }
        }
        return true
    }
    
    /**
     * Finds a move that would result in an immediate win.
     */
    private fun findWinningMove(gameState: GameState, player: Int): Pair<Int, Int>? {
        val validMoves = findValidMoves(gameState)
        
        for ((row, col) in validMoves) {
            val testState = cloneGameState(gameState)
            testState.board[row][col] = player
            
            // Check if this move creates a win
            if (testState.checkHavannahWin(player)) {
                return Pair(row, col)
            }
        }
        
        return null
    }
    
    /**
     * Finds a strategic move based on various heuristics.
     */
    private fun findStrategicMove(gameState: GameState): Pair<Int, Int>? {
        val validMoves = findValidMoves(gameState)
        if (validMoves.isEmpty()) return null
        
        // Map to store move evaluations
        val moveScores = mutableMapOf<Pair<Int, Int>, Double>()
        
        // Evaluate each valid move
        for (move in validMoves) {
            moveScores[move] = evaluateMove(gameState, move)
        }
        
        // Find the best move
        val bestMoves = moveScores.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        // Return the best move with a little randomness
        return if (bestMoves.isNotEmpty()) {
            bestMoves[random.nextInt(bestMoves.size.coerceAtMost(3))]
        } else {
            validMoves[random.nextInt(validMoves.size)]
        }
    }
    
    /**
     * Finds all valid moves on the board.
     */
    private fun findValidMoves(gameState: GameState): List<Pair<Int, Int>> {
        val moves = mutableListOf<Pair<Int, Int>>()
        val boardSize = gameState.boardSize
        
        // Only consider cells that are within the hexagonal shape of the Havannah board
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                // Skip positions that aren't part of the hexagonal board
                if (!isValidHavannahPosition(row, col, boardSize)) {
                    continue
                }
                
                if (gameState.board[row][col] == EMPTY) {
                    moves.add(Pair(row, col))
                }
            }
        }
        
        return moves
    }
    
    /**
     * Checks if a given position is valid on the hexagonal Havannah board.
     * FIXED: Align with rendering validation logic
     */
    private fun isValidHavannahPosition(row: Int, col: Int, boardSize: Int): Boolean {
        // Convert from array indices to hexagonal coordinates
        val q = col - (boardSize - 1) / 2
        val r = row - (boardSize - 1) / 2
        val s = -q - r
        
        // Check if the position is within the hexagonal board
        // Match the same criteria used in the rendering code
        return abs(q) + abs(r) + abs(s) <= boardSize - 1
    }
    
    /**
     * Evaluates a move based on various strategic criteria.
     */
    private fun evaluateMove(gameState: GameState, move: Pair<Int, Int>): Double {
        var score = 0.0
        val (row, col) = move
        
        // Bonus for corner positions (important for bridge win)
        if (CORNER_POSITIONS.contains(move)) {
            score += 10.0
        }
        
        // Bonus for edge positions (important for fork win)
        if (EDGE_POSITIONS.contains(move)) {
            score += 5.0
        }
        
        // Bonus for positions near existing pieces (connectivity)
        score += evaluateConnectivity(gameState, row, col, PLAYER_TWO)
        
        // Bonus for blocking opponent connectivity
        score += evaluateConnectivity(gameState, row, col, PLAYER_ONE) * 0.8
        
        // Penalize moves that are too far from the center early in the game
        val pieceCount = countPieces(gameState)
        if (pieceCount < 10) {
            val center = gameState.boardSize / 2
            val distanceFromCenter = sqrt((row - center).toDouble().pow(2) + 
                                          (col - center).toDouble().pow(2))
            score -= distanceFromCenter * 0.5
        }
        
        // Add a small random component to prevent deterministic play
        score += random.nextDouble() * 0.5
        
        return score
    }
    
    /**
     * Evaluates the connectivity value of a move.
     */
    private fun evaluateConnectivity(gameState: GameState, row: Int, col: Int, player: Int): Double {
        var score = 0.0
        
        // Check all neighbors
        for ((dr, dc) in DIRECTIONS) {
            val newRow = row + dr
            val newCol = col + dc
            
            if (newRow in 0 until gameState.boardSize && 
                newCol in 0 until gameState.boardSize && 
                gameState.board[newRow][newCol] == player) {
                
                // Score higher if the neighbor has other connected pieces
                var connectedNeighbors = 0
                for ((dr2, dc2) in DIRECTIONS) {
                    val r2 = newRow + dr2
                    val c2 = newCol + dc2
                    
                    if (r2 in 0 until gameState.boardSize && 
                        c2 in 0 until gameState.boardSize && 
                        gameState.board[r2][c2] == player) {
                        connectedNeighbors++
                    }
                }
                
                score += 1.0 + (connectedNeighbors * 0.5)
            }
        }
        
        return score
    }
    
    /**
     * Counts the total number of pieces on the board.
     */
    private fun countPieces(gameState: GameState): Int {
        var count = 0
        
        for (row in 0 until gameState.boardSize) {
            for (col in 0 until gameState.boardSize) {
                if (gameState.board[row][col] != EMPTY) {
                    count++
                }
            }
        }
        
        return count
    }
    
    /**
     * Creates a deep copy of the game state.
     */
    private fun cloneGameState(gameState: GameState): GameState {
        val clone = GameState(
            boardSize = gameState.boardSize,
            winCondition = gameState.winCondition,
            againstComputer = gameState.againstComputer
        )
        
        // Copy the board state
        for (row in 0 until gameState.boardSize) {
            for (col in 0 until gameState.boardSize) {
                clone.board[row][col] = gameState.board[row][col]
            }
        }
        
        // Copy the current player
        clone.currentPlayer = gameState.currentPlayer
        
        return clone
    }
    
    // Extension function for Double.pow() which doesn't exist in Kotlin standard library
    private fun Double.pow(exponent: Int): Double = Math.pow(this, exponent.toDouble())
}
