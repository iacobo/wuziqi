package com.iacobo.wuziqi.ai

import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.data.GameType
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
    }
    
    /**
     * Implementation of the GameAI interface method to find the best move.
     */
    override fun findBestMove(gameState: GameState): Pair<Int, Int>? {
        // Get game type and edge length for the hexagonal board
        val gameType = GameType.fromGameState(gameState)
        val edgeLength = gameType.getEdgeLength()
        val boardCenter = gameState.boardSize / 2
        
        // If this is the first move, place near center
        if (isBoardEmpty(gameState)) {
            // Return the center position in array coordinates
            return Pair(boardCenter, boardCenter)
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
        val gameType = GameType.fromGameState(gameState)
        
        for (row in 0 until gameState.boardSize) {
            for (col in 0 until gameState.boardSize) {
                // Only check valid hexagonal positions
                if (gameType.isValidHexPosition(row, col) && 
                    gameState.board[row][col] != EMPTY) {
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
        val gameType = GameType.fromGameState(gameState)
        
        // Consider all cells that are within the hexagonal shape
        for (row in 0 until gameState.boardSize) {
            for (col in 0 until gameState.boardSize) {
                // Skip positions that aren't valid hexagonal positions
                if (!gameType.isValidHexPosition(row, col)) {
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
     * Evaluates a move based on various strategic criteria.
     */
    private fun evaluateMove(gameState: GameState, move: Pair<Int, Int>): Double {
        var score = 0.0
        val (row, col) = move
        val gameType = GameType.fromGameState(gameState)
        val edgeLength = gameType.getEdgeLength()
        val boardCenter = gameState.boardSize / 2
        
        // Calculate relative position in hex coordinates
        val q = col - boardCenter
        val r = row - boardCenter
        val s = -q - r
        
        // Bonus for corner positions (important for bridge win)
        if (isCornerPosition(q, r, s, edgeLength)) {
            score += 10.0
        }
        
        // Bonus for edge positions (important for fork win)
        if (isEdgePosition(q, r, s, edgeLength) && !isCornerPosition(q, r, s, edgeLength)) {
            score += 5.0
        }
        
        // Bonus for positions near existing pieces (connectivity)
        score += evaluateConnectivity(gameState, row, col, PLAYER_TWO)
        
        // Bonus for blocking opponent connectivity
        score += evaluateConnectivity(gameState, row, col, PLAYER_ONE) * 0.8
        
        // Penalize moves that are too far from the center early in the game
        val pieceCount = countPieces(gameState)
        if (pieceCount < 10) {
            // Distance to center in hex coordinates
            val distanceToCenter = (abs(q) + abs(r) + abs(s)) / 2
            score -= distanceToCenter * 0.5
        }
        
        // Add a small random component to prevent deterministic play
        score += random.nextDouble() * 0.5
        
        return score
    }
    
    /**
     * Checks if a position is a corner in the hexagonal board.
     */
    private fun isCornerPosition(q: Int, r: Int, s: Int, edgeLength: Int): Boolean {
        val range = edgeLength - 1
        val corners = setOf(
            Triple(-range, range, 0),    // top-left
            Triple(0, range, -range),    // top
            Triple(range, 0, -range),    // top-right
            Triple(range, -range, 0),    // bottom-right
            Triple(0, -range, range),    // bottom
            Triple(-range, 0, range)     // bottom-left
        )
        
        return Triple(q, r, s) in corners
    }
    
    /**
     * Checks if a position is on the edge of the hexagonal board.
     */
    private fun isEdgePosition(q: Int, r: Int, s: Int, edgeLength: Int): Boolean {
        val range = edgeLength - 1
        return abs(q) == range || abs(r) == range || abs(s) == range
    }
    
    /**
     * Evaluates the connectivity value of a move.
     */
    private fun evaluateConnectivity(gameState: GameState, row: Int, col: Int, player: Int): Double {
        var score = 0.0
        val gameType = GameType.fromGameState(gameState)
        
        // Check all neighbors
        for ((dr, dc) in DIRECTIONS) {
            val newRow = row + dr
            val newCol = col + dc
            
            if (gameType.isValidHexPosition(newRow, newCol) && 
                gameState.board[newRow][newCol] == player) {
                
                // Score higher if the neighbor has other connected pieces
                var connectedNeighbors = 0
                for ((dr2, dc2) in DIRECTIONS) {
                    val r2 = newRow + dr2
                    val c2 = newCol + dc2
                    
                    if (gameType.isValidHexPosition(r2, c2) && 
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
        val gameType = GameType.fromGameState(gameState)
        
        for (row in 0 until gameState.boardSize) {
            for (col in 0 until gameState.boardSize) {
                if (gameType.isValidHexPosition(row, col) && 
                    gameState.board[row][col] != EMPTY) {
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
