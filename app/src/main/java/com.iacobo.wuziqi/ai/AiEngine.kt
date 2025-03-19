package com.iacobo.wuziqi.ai

import com.iacobo.wuziqi.data.GameState
import java.util.Random
import kotlin.math.abs
import kotlin.math.pow

/**
 * A threat-based Wuziqi (Gomoku) AI engine based on the principles described in:
 * - Victor Allis' Ph.D. thesis "Searching for solutions in games and artificial intelligence"
 * - The CodeCup 2020 winning bot "OOOOO"
 *
 * This AI uses pattern recognition and threat-sequence searches to play Wuziqi (Gomoku).
 */
class WuziqiAIEngine(private val random: Random = Random()) {

    companion object {
        // Player constants
        const val EMPTY = GameState.EMPTY
        const val PLAYER_ONE = GameState.PLAYER_ONE  // Human
        const val PLAYER_TWO = GameState.PLAYER_TWO  // Computer

        // Threat categories and their scores
        const val FIVE = 10000000          // (5,1): Immediate win
        const val OPEN_FOUR = 1000000      // (4,2): Guaranteed win next move
        const val SIMPLE_FOUR = 100000     // (4,1): Forces a response
        const val OPEN_THREE = 10000       // (3,3): Forces a response
        const val BROKEN_THREE = 1000      // (3,2): Forces a response
        const val SIMPLE_THREE = 100       // (3,1): Non-forcing
        const val TWO = 10                 // (2,n): Non-forcing
        const val ONE = 1                  // (1,n): Non-forcing

        // Direction vectors for line detection
        val DIRECTIONS = arrayOf(
            Pair(1, 0),   // Horizontal
            Pair(0, 1),   // Vertical
            Pair(1, 1),   // Diagonal \
            Pair(1, -1)   // Diagonal /
        )
    }

    /**
     * Finds the best move for the current game state.
     *
     * @param gameState The current game state
     * @return The best move as a Pair(row, col) or null if no valid move found
     */
    fun findBestMove(gameState: GameState): Pair<Int, Int>? {
        // Special cases for TicTacToe and Connect4
        return when {
            gameState.boardSize == 3 && gameState.winCondition == 3 -> {
                findTicTacToeMove(gameState)
            }
            gameState.boardSize == 7 && gameState.winCondition == 4 -> {
                findConnect4Move(gameState)
            }
            else -> {
                findWuziqiMove(gameState)
            }
        }
    }

    /**
     * Finds the best move for a standard Wuziqi position using improved threat-based analysis.
     */
    private fun findWuziqiMove(gameState: GameState): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val computerPlayer = PLAYER_TWO
        val humanPlayer = PLAYER_ONE
        
        // Store candidate moves with their scores for multi-objective evaluation
        val moveScores = mutableMapOf<Pair<Int, Int>, MoveScore>()
        
        // Track if we've found any moves of each category
        var foundWinningMove = false
        var foundBlockingMove = false
        var foundForcingMove = false
        
        // 1. Scan for immediate winning moves and opponent's immediate winning moves
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    val movePos = Pair(row, col)
                    val moveScore = MoveScore()
                    moveScores[movePos] = moveScore
                    
                    // Check if this is a winning move for us
                    gameState.board[row][col] = computerPlayer
                    if (gameState.checkWin(row, col, computerPlayer)) {
                        moveScore.isWinningMove = true
                        foundWinningMove = true
                    }
                    gameState.board[row][col] = EMPTY
                    
                    // Check if this blocks opponent's winning move
                    gameState.board[row][col] = humanPlayer
                    if (gameState.checkWin(row, col, humanPlayer)) {
                        moveScore.isBlockingMove = true
                        foundBlockingMove = true
                    }
                    gameState.board[row][col] = EMPTY
                }
            }
        }
        
        // 2. If we found winning moves, pick the best one and return immediately
        if (foundWinningMove) {
            return moveScores.entries
                .filter { it.value.isWinningMove }
                .maxByOrNull { evaluatePositionalValue(gameState, it.key.first, it.key.second) }
                ?.key
        }
        
        // 3. If we found blocking moves, pick the best one and return immediately
        if (foundBlockingMove) {
            return moveScores.entries
                .filter { it.value.isBlockingMove }
                .maxByOrNull { evaluatePositionalValue(gameState, it.key.first, it.key.second) }
                ?.key
        }
        
        // 4. Search for forcing moves (creating open fours or multiple threats)
        for ((movePos, score) in moveScores) {
            val (row, col) = movePos
            
            // Check if this creates an open four or multiple threats for us
            gameState.board[row][col] = computerPlayer
            val ourThreats = findThreats(gameState, row, col, computerPlayer)
            if (ourThreats.containsOpenFour() || ourThreats.countMultipleThreats() >= 2) {
                score.createsForcingThreat = true
                score.threatScore += ourThreats.getTotalScore()
                foundForcingMove = true
            }
            
            // Check if this blocks an open four or multiple threats for opponent
            gameState.board[row][col] = humanPlayer
            val opponentThreats = findThreats(gameState, row, col, humanPlayer)
            if (opponentThreats.containsOpenFour() || opponentThreats.countMultipleThreats() >= 2) {
                score.blocksOpponentThreat = true
                score.threatBlockScore += opponentThreats.getTotalScore()
            }
            
            // Reset the board
            gameState.board[row][col] = EMPTY
        }
        
        // 5. If we found forcing moves, pick the best one
        if (foundForcingMove) {
            return moveScores.entries
                .filter { it.value.createsForcingThreat }
                .maxByOrNull { (pos, score) -> 
                    score.threatScore + 
                    (if (score.blocksOpponentThreat) score.threatBlockScore else 0) +
                    evaluatePositionalValue(gameState, pos.first, pos.second) 
                }
                ?.key
        }
        
        // 6. Calculate comprehensive scores for all remaining moves
        for ((movePos, score) in moveScores) {
            val (row, col) = movePos
            
            // Skip already processed high-priority moves
            if (score.isWinningMove || score.isBlockingMove || score.createsForcingThreat) {
                continue
            }
            
            // Evaluate the threat-creation potential
            gameState.board[row][col] = computerPlayer
            val ourThreats = findThreats(gameState, row, col, computerPlayer)
            score.threatScore = ourThreats.getTotalScore()
            
            // Evaluate the threat-blocking potential
            gameState.board[row][col] = humanPlayer
            val opponentThreats = findThreats(gameState, row, col, humanPlayer)
            score.threatBlockScore = opponentThreats.getTotalScore()
            
            // Reset the board
            gameState.board[row][col] = EMPTY
            
            // Calculate positional score (closeness to existing pieces, center bias, etc.)
            score.positionalScore = evaluatePositionalValue(gameState, row, col)
            
            // Calculate the composite score
            score.calculateTotalScore()
        }
        
        // 7. Return the move with the highest total score
        return moveScores.maxByOrNull { it.value.totalScore }?.key ?: 
            // Fallback to a random adjacent move if no good moves found
            findAdjacentMove(gameState)
    }

    /**
     * Evaluates threats created or blocked by a move.
     * Returns a collection of threats found in all directions.
     */
    private fun findThreats(gameState: GameState, row: Int, col: Int, player: Int): ThreatsCollection {
        val threats = ThreatsCollection()
        val opponent = if (player == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        
        // Temporarily place the stone
        val originalValue = gameState.board[row][col]
        gameState.board[row][col] = player
        
        // Check all 4 directions
        for ((deltaRow, deltaCol) in DIRECTIONS) {
            // Extract the line in this direction
            val line = extractLine(gameState, row, col, deltaRow, deltaCol, player)
            
            // Analyze the line for threats
            val threatType = analyzeThreatType(line, player, opponent)
            if (threatType != ThreatType.NONE) {
                threats.addThreat(threatType)
            }
        }
        
        // Restore the board
        gameState.board[row][col] = originalValue
        
        return threats
    }

    /**
     * Analyzes a line to determine what kind of threat it contains.
     */
    private fun analyzeThreatType(line: String, player: Int, opponent: Int): ThreatType {
        // Convert the line to a pattern string where:
        // 'x' = player's stone, 'o' = opponent's stone, '_' = empty
        val pattern = line.map { 
            when (it.toInt()) {
                player -> 'x'
                opponent -> 'o'
                else -> '_'
            }
        }.joinToString("")
        
        // Check for win (five in a row)
        if (pattern.contains("xxxxx")) {
            return ThreatType.FIVE
        }
        
        // Check for open four (guaranteed win next move)
        if (pattern.contains("_xxxx_") || pattern.contains("xx_xx")) {
            return ThreatType.OPEN_FOUR
        }
        
        // Check for simple four (one end blocked)
        if (pattern.contains("xxxx_") || pattern.contains("_xxxx") || 
            pattern.contains("xxx_x") || pattern.contains("x_xxx")) {
            return ThreatType.SIMPLE_FOUR
        }
        
        // Check for open three
        if (pattern.contains("_xxx__") || pattern.contains("__xxx_") || 
            pattern.contains("_xx_x_") || pattern.contains("_x_xx_")) {
            return ThreatType.OPEN_THREE
        }
        
        // Check for broken three (one end blocked)
        if (pattern.contains("_xxx_o") || pattern.contains("o_xxx_") ||
            pattern.contains("_xx_xo") || pattern.contains("ox_xx_")) {
            return ThreatType.BROKEN_THREE
        }
        
        // Check for simple three
        if (pattern.contains("xxx__") || pattern.contains("__xxx") ||
            pattern.contains("xx_x_") || pattern.contains("_x_xx")) {
            return ThreatType.SIMPLE_THREE
        }
        
        // Check for open two
        if (pattern.contains("__xx__")) {
            return ThreatType.OPEN_TWO
        }
        
        // No significant threat found
        return ThreatType.NONE
    }

    /**
     * Extracts a line of stones in a given direction.
     */
    private fun extractLine(
        gameState: GameState, 
        row: Int, 
        col: Int, 
        deltaRow: Int, 
        deltaCol: Int,
        player: Int
    ): String {
        val boardSize = gameState.boardSize
        val result = StringBuilder()
        
        // Look 5 spaces in each direction (enough for Wuziqi patterns)
        for (i in -5..5) {
            val r = row + i * deltaRow
            val c = col + i * deltaCol
            
            if (r in 0 until boardSize && c in 0 until boardSize) {
                result.append(gameState.board[r][c])
            } else {
                // Off-board positions are marked differently
                result.append(-1)
            }
        }
        
        return result.toString()
    }

    /**
     * Evaluates the positional value of a move based on:
     * 1. Proximity to center
     * 2. Proximity to existing pieces
     * 3. Pattern formation potential
     */
    private fun evaluatePositionalValue(gameState: GameState, row: Int, col: Int): Int {
        val boardSize = gameState.boardSize
        var score = 0
        
        // Center proximity (higher value closer to center)
        val centerDist = Math.abs(row - boardSize / 2) + Math.abs(col - boardSize / 2)
        score += (boardSize - centerDist) * 2
        
        // Proximity to existing pieces
        val adjacentStones = countAdjacentStones(gameState, row, col, 2)
        score += adjacentStones * 10
        
        return score
    }

    /**
     * Counts stones within a certain radius of a position.
     */
    private fun countAdjacentStones(gameState: GameState, row: Int, col: Int, radius: Int): Int {
        val boardSize = gameState.boardSize
        var count = 0
        
        for (r in (row - radius).coerceAtLeast(0)..(row + radius).coerceAtMost(boardSize - 1)) {
            for (c in (col - radius).coerceAtLeast(0)..(col + radius).coerceAtMost(boardSize - 1)) {
                if (gameState.board[r][c] != EMPTY) {
                    count++
                }
            }
        }
        
        return count
    }

    /**
     * Finds a move adjacent to existing pieces when no good tactical moves are found.
     */
    private fun findAdjacentMove(gameState: GameState): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val candidates = mutableListOf<Pair<Int, Int>>()
        
        // If board is empty, play at or near center
        if (isBoardEmpty(gameState)) {
            val center = boardSize / 2
            return Pair(center, center)
        }
        
        // Collect all empty positions adjacent to existing stones
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.board[row][col] != EMPTY) {
                    // Check 8 adjacent directions
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            if (dr == 0 && dc == 0) continue
                            
                            val newRow = row + dr
                            val newCol = col + dc
                            
                            if (newRow in 0 until boardSize && 
                                newCol in 0 until boardSize && 
                                gameState.board[newRow][newCol] == EMPTY) {
                                candidates.add(Pair(newRow, newCol))
                            }
                        }
                    }
                }
            }
        }
        
        // Return the best candidate based on positional evaluation
        return candidates.maxByOrNull { (row, col) -> 
            evaluatePositionalValue(gameState, row, col)
        } ?: findDefaultMove(gameState)
    }

    /**
     * Checks if the board is completely empty.
     */
    private fun isBoardEmpty(gameState: GameState): Boolean {
        val boardSize = gameState.boardSize
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.board[row][col] != EMPTY) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Returns a default move (center or nearest to center) when no other move is found.
     */
    private fun findDefaultMove(gameState: GameState): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val center = boardSize / 2
        
        // Try center first
        if (gameState.isTileEmpty(center, center)) {
            return Pair(center, center)
        }
        
        // Try positions close to center
        for (distance in 1 until boardSize) {
            for (dr in -distance..distance) {
                for (dc in -distance..distance) {
                    // Only check positions exactly at 'distance'
                    if (Math.abs(dr) == distance || Math.abs(dc) == distance) {
                        val r = center + dr
                        val c = center + dc
                        
                        if (r in 0 until boardSize && 
                            c in 0 until boardSize && 
                            gameState.isTileEmpty(r, c)) {
                            return Pair(r, c)
                        }
                    }
                }
            }
        }
        
        // If we get here, there's no valid move (board is full)
        return null
    }

    /**
     * Data class to track different types of scores for a candidate move.
     */
    private class MoveScore {
        var isWinningMove = false
        var isBlockingMove = false
        var createsForcingThreat = false
        var blocksOpponentThreat = false
        var threatScore = 0
        var threatBlockScore = 0
        var positionalScore = 0
        var totalScore = 0
        
        fun calculateTotalScore() {
            totalScore = when {
                isWinningMove -> Int.MAX_VALUE
                isBlockingMove -> Int.MAX_VALUE - 1
                createsForcingThreat -> 1000000 + threatScore + threatBlockScore + positionalScore
                blocksOpponentThreat -> 100000 + threatBlockScore + threatScore + positionalScore
                else -> threatScore + threatBlockScore/2 + positionalScore
            }
        }
    }

    /**
     * Enum to classify different types of threats.
     */
    private enum class ThreatType(val score: Int) {
        FIVE(10000000),            // Win
        OPEN_FOUR(1000000),        // Guaranteed win next move
        SIMPLE_FOUR(100000),       // Forces a response
        OPEN_THREE(10000),         // Potentially creates a four next move
        BROKEN_THREE(1000),        // One end blocked three
        SIMPLE_THREE(100),         // Non-forcing three
        OPEN_TWO(10),              // Open two in a row
        NONE(0)                    // No significant threat
    }

    /**
     * Collection class to track threats found in a position.
     */
    private class ThreatsCollection {
        private val threats = mutableMapOf<ThreatType, Int>()
        
        fun addThreat(type: ThreatType) {
            threats[type] = (threats[type] ?: 0) + 1
        }
        
        fun containsOpenFour(): Boolean {
            return (threats[ThreatType.OPEN_FOUR] ?: 0) > 0
        }
        
        fun countMultipleThreats(): Int {
            return threats.entries.count { (type, count) -> 
                type == ThreatType.OPEN_THREE && count > 0 || 
                type == ThreatType.SIMPLE_FOUR && count > 0
            }
        }
        
        fun getTotalScore(): Int {
            return threats.entries.sumOf { (type, count) -> type.score * count }
        }
    }

    /**
     * Finds the best move for TicTacToe.
     */
    private fun findTicTacToeMove(gameState: GameState): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val computerPlayer = PLAYER_TWO
        val humanPlayer = PLAYER_ONE

        // 1. Check for winning move
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    gameState.board[row][col] = computerPlayer
                    if (gameState.checkWin(row, col, computerPlayer)) {
                        gameState.board[row][col] = EMPTY
                        return Pair(row, col)
                    }
                    gameState.board[row][col] = EMPTY
                }
            }
        }

        // 2. Block opponent's winning move
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    gameState.board[row][col] = humanPlayer
                    if (gameState.checkWin(row, col, humanPlayer)) {
                        gameState.board[row][col] = EMPTY
                        return Pair(row, col)
                    }
                    gameState.board[row][col] = EMPTY
                }
            }
        }

        // 3. Take center if available
        if (gameState.isTileEmpty(1, 1)) {
            return Pair(1, 1)
        }

        // 4. Take corners
        val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
        for ((row, col) in corners) {
            if (gameState.isTileEmpty(row, col)) {
                return Pair(row, col)
            }
        }

        // 5. Take any available edge
        val edges = listOf(Pair(0, 1), Pair(1, 0), Pair(1, 2), Pair(2, 1))
        for ((row, col) in edges) {
            if (gameState.isTileEmpty(row, col)) {
                return Pair(row, col)
            }
        }

        return null
    }

    /**
     * Finds the bottom-most empty row in a column for Connect4.
     */
    private fun findBottomEmptyRow(gameState: GameState, col: Int): Int {
        // Connect4 is considered to be a 7x6 board with 4-in-a-row to win
        val maxRow = if (gameState.boardSize == 7 && gameState.winCondition == 4) {
            5 // 6 rows (0-5) for Connect4
        } else {
            gameState.boardSize - 1
        }

        for (row in maxRow downTo 0) {
            if (gameState.board[row][col] == EMPTY) {
                return row
            }
        }
        return -1 // Column is full
    }

    /**
     * Finds the best move for Connect4 using a specialized approach for vertical boards.
     */
    private fun findConnect4Move(gameState: GameState): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val computerPlayer = PLAYER_TWO
        val humanPlayer = PLAYER_ONE

        // 1. Check for immediate win
        for (col in 0 until boardSize) {
            val row = findBottomEmptyRow(gameState, col)
            if (row != -1) {
                gameState.board[row][col] = computerPlayer
                if (gameState.checkWin(row, col, computerPlayer)) {
                    gameState.board[row][col] = EMPTY
                    return Pair(row, col)
                }
                gameState.board[row][col] = EMPTY
            }
        }

        // 2. Block opponent's win
        for (col in 0 until boardSize) {
            val row = findBottomEmptyRow(gameState, col)
            if (row != -1) {
                gameState.board[row][col] = humanPlayer
                if (gameState.checkWin(row, col, humanPlayer)) {
                    gameState.board[row][col] = EMPTY
                    return Pair(row, col)
                }
                gameState.board[row][col] = EMPTY
            }
        }
        
        // 3. Critical: Block open two threats on the bottom row
        val maxRow = if (gameState.boardSize == 7) 5 else gameState.boardSize - 1
        val result = findConnect4OpenTwoBlock(gameState, maxRow, humanPlayer)
        if (result != null) {
            return result
        }

        // 4. Look for Connect4-specific forced win setups
        val forcingMoveResult = findConnect4ForcingMove(gameState, computerPlayer)
        if (forcingMoveResult != null) {
            return forcingMoveResult
        }

        // 5. Block opponent's forcing moves
        val blockForcingMoveResult = findConnect4ForcingMove(gameState, humanPlayer)
        if (blockForcingMoveResult != null) {
            return blockForcingMoveResult
        }

        // 6. Evaluate positions with Connect4-specific heuristics
        var bestScore = Int.MIN_VALUE
        var bestMove: Pair<Int, Int>? = null
        
        // Column preference: center > sides
        val centerCol = boardSize / 2
        val colValues = (0 until boardSize).sortedBy { abs(it - centerCol) }
        
        for (col in colValues) {
            val row = findBottomEmptyRow(gameState, col)
            if (row != -1) {
                // Place a temporary stone
                gameState.board[row][col] = computerPlayer
                
                // Evaluate this position using Connect4-specific evaluation
                val score = evaluateConnect4Position(gameState, row, col, computerPlayer)
                
                // Remove temporary stone
                gameState.board[row][col] = EMPTY
                
                if (score > bestScore) {
                    bestScore = score
                    bestMove = Pair(row, col)
                }
            }
        }
        
        return bestMove
    }
    
    /**
     * Specifically handles blocking open two threats on the bottom row in Connect4.
     * For example: "--rr--" needs to be blocked at position 3 (0-indexed, between the two 'r's).
     */
    private fun findConnect4OpenTwoBlock(gameState: GameState, bottomRow: Int, playerValue: Int): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        
        // Check horizontal open two threats on bottom row
        for (col in 0 until boardSize - 3) {
            // Look for two adjacent opponent pieces with spaces on either side
            if (gameState.board[bottomRow][col] == EMPTY &&
                gameState.board[bottomRow][col+1] == playerValue &&
                gameState.board[bottomRow][col+2] == playerValue &&
                col+3 < boardSize && gameState.board[bottomRow][col+3] == EMPTY) {
                
                // Block by playing immediately before or after the pair
                // Prefer blocking before if both are available
                if (gameState.isTileEmpty(bottomRow, col)) {
                    return Pair(bottomRow, col)
                } else if (col+3 < boardSize && gameState.isTileEmpty(bottomRow, col+3)) {
                    return Pair(bottomRow, col+3)
                }
            }
        }
        
        // Check for pattern with a gap: "r-r"
        for (col in 0 until boardSize - 2) {
            if (gameState.board[bottomRow][col] == playerValue &&
                gameState.board[bottomRow][col+1] == EMPTY &&
                gameState.board[bottomRow][col+2] == playerValue) {
                
                // Block the gap
                return Pair(bottomRow, col+1)
            }
        }
        
        // Also check for patterns like "-rr-" where we need to block one of the ends
        for (col in 1 until boardSize - 2) {
            if (gameState.board[bottomRow][col-1] == EMPTY &&
                gameState.board[bottomRow][col] == playerValue &&
                gameState.board[bottomRow][col+1] == playerValue &&
                gameState.board[bottomRow][col+2] == EMPTY) {
                
                // Block one of the ends, prioritize the center side
                val centerCol = boardSize / 2
                if (Math.abs(col-1 - centerCol) < Math.abs(col+2 - centerCol)) {
                    return Pair(bottomRow, col-1)
                } else {
                    return Pair(bottomRow, col+2)
                }
            }
        }
        
        return null
    }
    
    /**
     * Finds forcing moves specific to Connect4 strategy.
     * Looks for potential "trap" setups where playing one move forces a win.
     */
    private fun findConnect4ForcingMove(gameState: GameState, playerValue: Int): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val opponent = if (playerValue == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        
        // Check for potential double-threat setups (two winning moves in a single turn)
        for (col in 0 until boardSize) {
            val row = findBottomEmptyRow(gameState, col)
            if (row != -1) {
                // Place a temporary stone
                gameState.board[row][col] = playerValue
                
                // Count how many winning moves would be available after this move
                var winningCols = 0
                var firstWinningCol = -1
                
                // Check if this creates a potential winning move in the next turn
                for (nextCol in 0 until boardSize) {
                    val nextRow = findBottomEmptyRow(gameState, nextCol)
                    if (nextRow != -1) {
                        gameState.board[nextRow][nextCol] = playerValue
                        if (gameState.checkWin(nextRow, nextCol, playerValue)) {
                            winningCols++
                            if (firstWinningCol == -1) {
                                firstWinningCol = nextCol
                            }
                        }
                        gameState.board[nextRow][nextCol] = EMPTY
                    }
                }
                
                // Remove the temporary stone
                gameState.board[row][col] = EMPTY
                
                // If this move creates two or more winning possibilities, it's a forcing move
                if (winningCols >= 2) {
                    return Pair(row, col)
                }
            }
        }
        
        return null
    }
    
    /**
     * Evaluates a Connect4 position with specific heuristics for the game.
     */
    private fun evaluateConnect4Position(gameState: GameState, row: Int, col: Int, playerValue: Int): Int {
        val boardSize = gameState.boardSize
        val opponent = if (playerValue == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        var score = 0
        
        // Prioritize center columns
        val centerScore = boardSize - 2 * abs(col - boardSize / 2)
        score += centerScore * 3
        
        // Count potential winning lines through this position
        val directions = arrayOf(
            Pair(0, 1),   // Horizontal
            Pair(1, 0),   // Vertical
            Pair(1, 1),   // Diagonal \
            Pair(1, -1)   // Diagonal /
        )
        
        for ((deltaRow, deltaCol) in directions) {
            // Look for sequences of pieces and empty spaces that could lead to wins
            var playerCount = 0
            var emptyCount = 0
            var opponentCount = 0
            
            // Check 3 in each direction (4 total pieces including the current position)
            for (i in -3..3) {
                val r = row + i * deltaRow
                val c = col + i * deltaCol
                
                if (r in 0 until boardSize && c in 0 until boardSize) {
                    when (gameState.board[r][c]) {
                        playerValue -> playerCount++
                        EMPTY -> emptyCount++
                        opponent -> opponentCount++
                    }
                }
            }
            
            // Score based on piece configurations
            if (opponentCount == 0) {
                // No opponents in this line, so it's a potential win
                when (playerCount) {
                    3 -> score += 1000  // 3 in a row - very strong position
                    2 -> score += 100   // 2 in a row - good position
                    1 -> score += 10    // Just this piece - weak but potential
                }
            } else if (playerCount == 0 && opponentCount > 0) {
                // Defensive evaluation - block opponent's potential lines
                when (opponentCount) {
                    3 -> score -= 800   // Critical to block, but not as valuable as winning
                    2 -> score -= 50    // Should consider blocking
                }
            }
        }
        
        // Look for "trap" setups - situations where dropping a piece creates a fork
        val tempBoard = Array(boardSize) { r -> IntArray(boardSize) { c -> gameState.board[r][c] } }
        
        // Check if we can create a trap by playing here
        tempBoard[row][col] = playerValue
        
        // Check for potential next-move wins in multiple columns
        var winningColumns = 0
        for (nextCol in 0 until boardSize) {
            val nextRow = findBottomEmptyRow(gameState, nextCol)
            if (nextRow != -1) {
                tempBoard[nextRow][nextCol] = playerValue
                
                // Check if this would be a win
                var isWin = false
                for ((dr, dc) in directions) {
                    if (checkConnect4Line(tempBoard, nextRow, nextCol, dr, dc, playerValue)) {
                        isWin = true
                        break
                    }
                }
                
                if (isWin) {
                    winningColumns++
                }
                
                // Restore board
                tempBoard[nextRow][nextCol] = EMPTY
            }
        }
        
        // Significant bonus for moves that create multiple winning possibilities
        if (winningColumns >= 2) {
            score += 5000
        }
        
        return score
    }
    
    /**
     * Checks if there's a connect-4 win in a specific direction.
     */
    private fun checkConnect4Line(
        board: Array<IntArray>,
        row: Int, 
        col: Int, 
        deltaRow: Int, 
        deltaCol: Int, 
        playerValue: Int
    ): Boolean {
        val boardSize = board.size
        var count = 1  // Start with the current piece
        
        // Check in the positive direction
        for (i in 1..3) {
            val r = row + i * deltaRow
            val c = col + i * deltaCol
            
            if (r in 0 until boardSize && c in 0 until boardSize && board[r][c] == playerValue) {
                count++
            } else {
                break
            }
        }
        
        // Check in the negative direction
        for (i in 1..3) {
            val r = row - i * deltaRow
            val c = col - i * deltaCol
            
            if (r in 0 until boardSize && c in 0 until boardSize && board[r][c] == playerValue) {
                count++
            } else {
                break
            }
        }
        
        return count >= 4
    }
}