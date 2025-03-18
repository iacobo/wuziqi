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
        
        // Pattern definitions for efficient lookup
        // 'x' represents player's stones, 'o' represents opponent's stones, '-' represents empty
        val WIN_PATTERNS = arrayOf("xxxxx")
        
        val OPEN_FOUR_PATTERNS = arrayOf(
            "-xxxx-",    // Standard open four
            "xx-xx"      // Split open four
        )
        
        val SIMPLE_FOUR_PATTERNS = arrayOf(
            "xxxx-",     // Simple four
            "-xxxx",     // Simple four
            "xxx-x",     // Non-standard with gap
            "x-xxx",     // Non-standard with gap
            "xx-xx-",    // Split with edge
            "-xx-xx"     // Split with edge
        )
        
        val OPEN_THREE_PATTERNS = arrayOf(
            "--xxx--",    // Standard open three
            "-x-xx-",     // Non-standard with gap
            "-xx-x-"      // Non-standard with gap
        )
        
        val BROKEN_THREE_PATTERNS = arrayOf(
            "-xxx-o",     // Broken three with right block
            "o-xxx-",     // Broken three with left block
            "x-xx-",      // Left-side variation
            "-xx-x",      // Right-side variation
            "-x-xx",      // Non-standard 
            "xx-x-"       // Non-standard
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
     * Finds the best move for a standard Wuziqi position using threat-based analysis.
     */
    private fun findWuziqiMove(gameState: GameState): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val computerPlayer = PLAYER_TWO
        val humanPlayer = PLAYER_ONE
        val center = boardSize / 2

        // Handle opening moves based on game state
        val stoneCount = countStones(gameState)
        
        if (stoneCount == 0) {
            // If this is the first move of the game (empty board), play at center
            return Pair(center, center)
        } else if (stoneCount == 1) {
            // If this is the second move (player went first)
            
            // Check if the player played at the center
            if (gameState.board[center][center] == humanPlayer) {
                // Two standard book responses:
                // 1. Play 3 positions away from center (traditional)
                // 2. Play 1 position diagonally from center (modern variation)
                
                val traditionalOptions = listOf(
                    Pair(center - 3, center),  // 3 up from center
                    Pair(center + 3, center),  // 3 down from center
                    Pair(center, center - 3),  // 3 left from center
                    Pair(center, center + 3)   // 3 right from center
                )
                
                val diagonalOptions = listOf(
                    Pair(center - 1, center - 1),  // Top-left diagonal
                    Pair(center - 1, center + 1),  // Top-right diagonal
                    Pair(center + 1, center - 1),  // Bottom-left diagonal
                    Pair(center + 1, center + 1)   // Bottom-right diagonal
                )
                
                // Combine both options, with slight preference for traditional responses
                val allOptions = traditionalOptions + diagonalOptions
                
                // Randomize the choice among valid options
                val validOptions = allOptions.filter { (r, c) -> 
                    gameState.isValidPosition(r, c) && gameState.isTileEmpty(r, c)
                }
                
                if (validOptions.isNotEmpty()) {
                    return validOptions[random.nextInt(validOptions.size)]
                }
            } else {
                // If player didn't play center, we play at center
                if (gameState.isTileEmpty(center, center)) {
                    return Pair(center, center)
                }
            }
        } else if (stoneCount < 5) {
            // For early game (but not first 2 moves), prefer central area
            // Get coordinates within a reasonable distance from center
            val minDist = 2  // Minimum distance from center
            val maxDist = 5  // Maximum distance from center
            
            val candidates = mutableListOf<Pair<Int, Int>>()
            
            // First, check if any threats need to be handled before playing positionally
            val threatResponse = handleThreatsOrCreateOwn(gameState, computerPlayer, humanPlayer)
            if (threatResponse != null) {
                return threatResponse
            }
            
            // If no threats, collect candidate moves near existing stones
            // but not too close to the board edge
            val edgeDistance = 3  // Stay at least this far from the edge
            
            for (row in edgeDistance until boardSize - edgeDistance) {
                for (col in edgeDistance until boardSize - edgeDistance) {
                    if (gameState.isTileEmpty(row, col)) {
                        // Calculate Manhattan distance from center
                        val distFromCenter = Math.abs(row - center) + Math.abs(col - center)
                        
                        // Consider positions that are at a good distance from center
                        if (distFromCenter in minDist..maxDist) {
                            candidates.add(Pair(row, col))
                        }
                    }
                }
            }
            
            // If we have candidates, choose one randomly
            if (candidates.isNotEmpty()) {
                return candidates[random.nextInt(candidates.size)]
            }
        }
        
        // Handle threats or proceed with standard evaluation
        return handleThreatsOrCreateOwn(gameState, computerPlayer, humanPlayer)
    }
    
    /**
     * Handles all threat checking logic in the correct order.
     * This helps avoid duplicating code in multiple places.
     */
    private fun handleThreatsOrCreateOwn(
        gameState: GameState,
        computerPlayer: Int,
        humanPlayer: Int
    ): Pair<Int, Int>? {
        // 1. Check for immediate win (five)
        for (row in 0 until gameState.boardSize) {
            for (col in 0 until gameState.boardSize) {
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

        // 2. Check for opponent's immediate win and block it
        for (row in 0 until gameState.boardSize) {
            for (col in 0 until gameState.boardSize) {
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

        // 3. Check for open four creation (guaranteed win next move)
        val openFourMove = findThreateningMove(gameState, computerPlayer, OPEN_FOUR_PATTERNS)
        if (openFourMove != null) {
            return openFourMove
        }

        // 4. Block opponent's open four
        val blockOpenFourMove = findThreateningMove(gameState, humanPlayer, OPEN_FOUR_PATTERNS)
        if (blockOpenFourMove != null) {
            return blockOpenFourMove
        }

        // 5. Check for simple four creation
        val simpleFourMove = findThreateningMove(gameState, computerPlayer, SIMPLE_FOUR_PATTERNS)
        if (simpleFourMove != null) {
            return simpleFourMove
        }

        // 6. Block opponent's simple four
        val blockSimpleFourMove = findThreateningMove(gameState, humanPlayer, SIMPLE_FOUR_PATTERNS)
        if (blockSimpleFourMove != null) {
            return blockSimpleFourMove
        }

        // 7. Handle opponent's open three threats
        val blockOpenThreeMove = findOpenThreeBlock(gameState, humanPlayer)
        if (blockOpenThreeMove != null) {
            return blockOpenThreeMove
        }
        
        // 8. Handle opponent's broken three threats
        val blockBrokenThreeMove = findBrokenThreeBlock(gameState, humanPlayer)
        if (blockBrokenThreeMove != null) {
            return blockBrokenThreeMove
        }

        // 9. Create our own open three if possible
        val createOpenThreeMove = findThreateningMove(gameState, computerPlayer, OPEN_THREE_PATTERNS)
        if (createOpenThreeMove != null) {
            return createOpenThreeMove
        }

        // 10. Create our own broken three if possible
        val createBrokenThreeMove = findThreateningMove(gameState, computerPlayer, BROKEN_THREE_PATTERNS)
        if (createBrokenThreeMove != null) {
            return createBrokenThreeMove
        }

        // 11. Fallback to positional evaluation if no threats found
        return findBestPositionalMove(gameState)
    }

    /**
     * Finds moves that create specific threatening patterns.
     * Used for creating our own threats or blocking opponent's threats.
     */
    private fun findThreateningMove(
        gameState: GameState,
        playerValue: Int,
        patterns: Array<String>
    ): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val opponent = if (playerValue == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE

        // First, scan the board for patterns that are almost complete
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.board[row][col] == playerValue) {
                    for ((deltaRow, deltaCol) in DIRECTIONS) {
                        // Normalize pattern representation
                        val linePattern = extractLinePattern(
                            gameState, row, col, deltaRow, deltaCol, playerValue
                        )
                        
                        for (pattern in patterns) {
                            val index = linePattern.lineString.indexOf(pattern)
                            if (index != -1) {
                                // Find all empty positions in the pattern and check if placing a stone would complete it
                                for (i in pattern.indices) {
                                    if (pattern[i] == '-') {
                                        val pos = linePattern.getPositionAt(index + i)
                                        if (pos != null && 
                                            gameState.isValidPosition(pos.first, pos.second) && 
                                            gameState.isTileEmpty(pos.first, pos.second)) {
                                            return pos
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // If no immediate threats found, look for creating threats
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    // Try placing a stone here
                    gameState.board[row][col] = playerValue

                    // Check if this move creates any of the specified patterns
                    for ((deltaRow, deltaCol) in DIRECTIONS) {
                        val linePattern = extractLinePattern(
                            gameState, row, col, deltaRow, deltaCol, playerValue
                        ).lineString

                        if (patterns.any { pattern -> linePattern.contains(pattern) }) {
                            gameState.board[row][col] = EMPTY
                            return Pair(row, col)
                        }
                    }

                    gameState.board[row][col] = EMPTY
                }
            }
        }

        return null
    }

    /**
     * Specifically handles open three threats.
     * Focused on properly blocking the key position to prevent a follow-up open four.
     */
    private fun findOpenThreeBlock(gameState: GameState, playerValue: Int): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        
        // Key insight: In extractLinePattern, when we pass playerValue, the function maps:
        // - playerValue stones to 'o'
        // - opponent stones to 'x'
        // - empty spaces to '-'
        // So we need to look for "ooo" patterns, not "xxx" patterns!
        
        // Look specifically for open three patterns (critical threat)
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.board[row][col] == playerValue) {
                    for ((deltaRow, deltaCol) in DIRECTIONS) {
                        val linePattern = extractLinePattern(
                            gameState, row, col, deltaRow, deltaCol, playerValue
                        )
                        
                        // Look for the standard open three pattern - key fix: using "ooo" not "xxx"
                        val pattern = "--ooo--"
                        val idx = linePattern.lineString.indexOf(pattern)
                        if (idx != -1) {
                            // The most critical positions to block are adjacent to the three stones
                            // First try position right after the three stones
                            val blockPos1 = linePattern.getPositionAt(idx + 5)
                            if (blockPos1 != null && 
                                gameState.isValidPosition(blockPos1.first, blockPos1.second) && 
                                gameState.isTileEmpty(blockPos1.first, blockPos1.second)) {
                                return blockPos1
                            }
                            
                            // Then try position right before the three stones
                            val blockPos2 = linePattern.getPositionAt(idx + 1)
                            if (blockPos2 != null && 
                                gameState.isValidPosition(blockPos2.first, blockPos2.second) && 
                                gameState.isTileEmpty(blockPos2.first, blockPos2.second)) {
                                return blockPos2
                            }
                            
                            // As a last resort, block the outer spaces
                            val blockPos3 = linePattern.getPositionAt(idx)
                            if (blockPos3 != null && 
                                gameState.isValidPosition(blockPos3.first, blockPos3.second) && 
                                gameState.isTileEmpty(blockPos3.first, blockPos3.second)) {
                                return blockPos3
                            }
                            
                            val blockPos4 = linePattern.getPositionAt(idx + 6)
                            if (blockPos4 != null && 
                                gameState.isValidPosition(blockPos4.first, blockPos4.second) && 
                                gameState.isTileEmpty(blockPos4.first, blockPos4.second)) {
                                return blockPos4
                            }
                        }
                        
                        // Also check for non-standard open three patterns with gaps
                        val gapPatterns = arrayOf("-o-oo-", "-oo-o-")
                        for (gapPattern in gapPatterns) {
                            val gapIdx = linePattern.lineString.indexOf(gapPattern)
                            if (gapIdx != -1) {
                                // In these patterns, the most critical position is the gap
                                for (i in gapPattern.indices) {
                                    if (gapPattern[i] == '-' && i > 0 && i < gapPattern.length - 1) {
                                        val blockPos = linePattern.getPositionAt(gapIdx + i)
                                        if (blockPos != null && 
                                            gameState.isValidPosition(blockPos.first, blockPos.second) && 
                                            gameState.isTileEmpty(blockPos.first, blockPos.second)) {
                                            return blockPos
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * Specifically handles broken three threats.
     * These are three stones in a row with one end blocked, but the other end open.
     */
    private fun findBrokenThreeBlock(gameState: GameState, playerValue: Int): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        
        // Look for patterns like "-ooo-x" or "x-ooo-" - note the pattern is from the player's perspective
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.board[row][col] == playerValue) {
                    for ((deltaRow, deltaCol) in DIRECTIONS) {
                        val linePattern = extractLinePattern(
                            gameState, row, col, deltaRow, deltaCol, playerValue
                        )
                        
                        // Check both standard broken three patterns - key fix: using "ooo" not "xxx"
                        val patterns = arrayOf("-ooo-x", "x-ooo-")
                        for (pattern in patterns) {
                            val idx = linePattern.lineString.indexOf(pattern)
                            if (idx != -1) {
                                // The critical position to block is the open side
                                val blockPos = if (pattern == "-ooo-x") {
                                    linePattern.getPositionAt(idx)
                                } else { // "x-ooo-"
                                    linePattern.getPositionAt(idx + 5)
                                }
                                
                                if (blockPos != null && 
                                    gameState.isValidPosition(blockPos.first, blockPos.second) && 
                                    gameState.isTileEmpty(blockPos.first, blockPos.second)) {
                                    return blockPos
                                }
                            }
                        }
                        
                        // Check broken three patterns with gaps - key fix: using "o" not "x"
                        val gapPatterns = arrayOf("o-oo-", "-oo-o", "-o-oo", "oo-o-")
                        for (gapPattern in gapPatterns) {
                            val gapIdx = linePattern.lineString.indexOf(gapPattern)
                            if (gapIdx != -1) {
                                // For patterns with gaps, block the gap first
                                for (i in gapPattern.indices) {
                                    if (gapPattern[i] == '-' && 
                                        ((i > 0 && i < gapPattern.length - 1) || // Internal gap
                                         (i == 0 && gapPattern[1] == 'o') ||     // Left edge if next is 'o'
                                         (i == gapPattern.length - 1 && gapPattern[i-1] == 'o'))) { // Right edge if prev is 'o'
                                        
                                        val blockPos = linePattern.getPositionAt(gapIdx + i)
                                        if (blockPos != null && 
                                            gameState.isValidPosition(blockPos.first, blockPos.second) && 
                                            gameState.isTileEmpty(blockPos.first, blockPos.second)) {
                                            return blockPos
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return null
    }

    /**
     * Data class to represent a line pattern extracted from the board.
     */
    private data class LinePattern(
        val lineString: String,
        val positions: List<Pair<Int, Int>?>
    ) {
        /**
         * Get the board position corresponding to a specific index in the pattern.
         */
        fun getPositionAt(index: Int): Pair<Int, Int>? {
            return if (index in positions.indices) positions[index] else null
        }
    }

    /**
     * Extracts a line pattern from the board in a given direction.
     * Includes mapping between pattern positions and board positions.
     */
    private fun extractLinePattern(
        gameState: GameState,
        row: Int,
        col: Int,
        deltaRow: Int,
        deltaCol: Int,
        playerValue: Int
    ): LinePattern {
        val boardSize = gameState.boardSize
        val opponent = if (playerValue == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        val lineBuilder = StringBuilder()
        val positions = mutableListOf<Pair<Int, Int>?>()

        // Look 6 spaces in each direction to capture all possible threat patterns
        for (i in -6..6) {
            val r = row + i * deltaRow
            val c = col + i * deltaCol

            if (r in 0 until boardSize && c in 0 until boardSize) {
                when (gameState.board[r][c]) {
                    playerValue -> {
                        lineBuilder.append("o") // Current player's stones
                        positions.add(Pair(r, c))
                    }
                    opponent -> {
                        lineBuilder.append("x") // Opponent's stones 
                        positions.add(Pair(r, c))
                    }
                    else -> {
                        lineBuilder.append("-") // Empty spaces
                        positions.add(Pair(r, c))
                    }
                }
            } else {
                // Off-board positions are treated as blocked
                lineBuilder.append("x") // Treat edge as opponent's piece
                positions.add(null) // null indicates off-board
            }
        }

        return LinePattern(lineBuilder.toString(), positions)
    }

    /**
     * When no threats are found, use positional evaluation to find the best move.
     */
    private fun findBestPositionalMove(gameState: GameState): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val computerPlayer = PLAYER_TWO
        val humanPlayer = PLAYER_ONE
        val center = boardSize / 2

        // If the board is nearly empty, play near the center but avoid edges
        if (countStones(gameState) < 10) {
            // Avoid playing too close to the edge
            val edgeBuffer = 3
            
            // Generate positions that are not too close to the edge and prioritize those near the center
            val candidates = mutableListOf<Pair<Pair<Int, Int>, Int>>() // Pair of (position, score)
            
            for (row in edgeBuffer until boardSize - edgeBuffer) {
                for (col in edgeBuffer until boardSize - edgeBuffer) {
                    if (gameState.isTileEmpty(row, col)) {
                        // Calculate Manhattan distance from center - lower is better
                        val distFromCenter = Math.abs(row - center) + Math.abs(col - center)
                        // Convert to a score where higher is better (closer to center)
                        val positionScore = boardSize - distFromCenter
                        
                        candidates.add(Pair(Pair(row, col), positionScore))
                    }
                }
            }
            
            // Sort candidates by score (higher is better)
            candidates.sortByDescending { it.second }
            
            // Take the top 5 positions (closest to center) and choose randomly among them
            // This adds some variety while still favoring good positions
            val topCandidates = candidates.take(5.coerceAtMost(candidates.size))
            if (topCandidates.isNotEmpty()) {
                return topCandidates[random.nextInt(topCandidates.size)].first
            }
        }

        // Look for positions adjacent to existing stones
        val candidates = findCandidateMoves(gameState)
        if (candidates.isNotEmpty()) {
            var bestScore = Int.MIN_VALUE
            var bestMove: Pair<Int, Int>? = null
            val bestMoves = mutableListOf<Pair<Int, Int>>()

            for ((row, col) in candidates) {
                if (gameState.isTileEmpty(row, col)) {
                    // Check if this position is too close to the edge
                    val edgeDistance = minOf(row, col, boardSize - 1 - row, boardSize - 1 - col)
                    if (edgeDistance < 2) {
                        // Skip positions that are too close to the edge
                        continue
                    }
                    
                    // Evaluate this position
                    val score = evaluatePosition(gameState, row, col, computerPlayer) -
                            evaluatePosition(gameState, row, col, humanPlayer)

                    if (score > bestScore) {
                        bestScore = score
                        bestMoves.clear()
                        bestMoves.add(Pair(row, col))
                    } else if (score == bestScore) {
                        bestMoves.add(Pair(row, col))
                    }
                }
            }

            // If we have multiple moves with the same score, choose one randomly
            if (bestMoves.isNotEmpty()) {
                return bestMoves[random.nextInt(bestMoves.size)]
            }
            
            // If all our candidates were too close to the edge, try again without the edge restriction
            if (bestMoves.isEmpty()) {
                for ((row, col) in candidates) {
                    if (gameState.isTileEmpty(row, col)) {
                        // Evaluate this position
                        val score = evaluatePosition(gameState, row, col, computerPlayer) -
                                evaluatePosition(gameState, row, col, humanPlayer)
    
                        if (score > bestScore) {
                            bestScore = score
                            bestMove = Pair(row, col)
                        }
                    }
                }
                
                return bestMove
            }
        }

        // If no good candidates, just find any empty spot (prioritize center area)
        for (distance in 0 until boardSize) {
            for (dr in -distance..distance) {
                for (dc in -distance..distance) {
                    // Only check positions that are exactly 'distance' away from center
                    if (Math.abs(dr) == distance || Math.abs(dc) == distance) {
                        val r = center + dr
                        val c = center + dc
                        if (gameState.isValidPosition(r, c) && gameState.isTileEmpty(r, c)) {
                            return Pair(r, c)
                        }
                    }
                }
            }
        }

        return null // No valid move found (shouldn't happen unless board is full)
    }

    /**
     * Counts the number of stones on the board.
     */
    private fun countStones(gameState: GameState): Int {
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
     * Finds candidate moves (positions adjacent to existing stones).
     */
    private fun findCandidateMoves(gameState: GameState): List<Pair<Int, Int>> {
        val boardSize = gameState.boardSize
        val candidates = mutableSetOf<Pair<Int, Int>>()

        // Directions for checking adjacent cells (8 directions)
        val directions = listOf(
            -1 to -1, -1 to 0, -1 to 1,
            0 to -1, 0 to 1,
            1 to -1, 1 to 0, 1 to 1
        )

        // Find all empty positions adjacent to existing stones
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.board[row][col] != EMPTY) {
                    // Check all adjacent positions
                    for ((dr, dc) in directions) {
                        val r = row + dr
                        val c = col + dc

                        if (gameState.isValidPosition(r, c) && gameState.board[r][c] == EMPTY) {
                            candidates.add(Pair(r, c))
                        }
                    }
                }
            }
        }

        return candidates.toList()
    }

    /**
     * Evaluates a position for a given player.
     * Enhanced version of the evaluation from the OOOOO bot description.
     */
    private fun evaluatePosition(gameState: GameState, row: Int, col: Int, playerValue: Int): Int {
        // Skip if the position is not empty
        if (!gameState.isTileEmpty(row, col)) {
            return 0
        }

        // Place a stone temporarily
        gameState.board[row][col] = playerValue

        // Get threat values in all 4 directions
        val threatValues = mutableListOf<Int>()
        for ((deltaRow, deltaCol) in DIRECTIONS) {
            val pattern = extractLinePattern(gameState, row, col, deltaRow, deltaCol, playerValue).lineString
            threatValues.add(evaluatePatternScore(pattern))
        }

        // Remove the temporary stone
        gameState.board[row][col] = EMPTY

        // Sort threat values in descending order
        threatValues.sortDescending()

        // Take the two best threats
        val a = 16.coerceAtMost(threatValues.getOrElse(0) { 0 } / 10)
        val b = 16.coerceAtMost(threatValues.getOrElse(1) { 0 } / 10)

        // Use the formula from the OOOOO bot: 1.5 * 1.8^a + 1.8^b
        return (1.5 * 1.8.pow(a.toDouble()) + 1.8.pow(b.toDouble())).toInt()
    }

    /**
     * Evaluates a pattern string and returns a score.
     * Handles all pattern variations for efficient evaluation.
     */
    private fun evaluatePatternScore(pattern: String): Int {
        var score = 0

        // Winning threats
        if (pattern.contains("ooooo")) score += FIVE

        // Open Four threats
        if (pattern.contains("-oooo-")) score += OPEN_FOUR
        if (pattern.contains("oo-oo")) score += OPEN_FOUR

        // Simple Four threats
        if (pattern.contains("oooo-")) score += SIMPLE_FOUR
        if (pattern.contains("-oooo")) score += SIMPLE_FOUR
        if (pattern.contains("ooo-o")) score += SIMPLE_FOUR
        if (pattern.contains("o-ooo")) score += SIMPLE_FOUR
        if (pattern.contains("oo-oo-")) score += SIMPLE_FOUR
        if (pattern.contains("-oo-oo")) score += SIMPLE_FOUR

        // Open Three threats
        if (pattern.contains("--ooo--")) score += OPEN_THREE
        if (pattern.contains("-o-oo-")) score += OPEN_THREE
        if (pattern.contains("-oo-o-")) score += OPEN_THREE

        // Broken Three threats
        if (pattern.contains("-ooo-x")) score += BROKEN_THREE
        if (pattern.contains("x-ooo-")) score += BROKEN_THREE
        if (pattern.contains("-oo-o")) score += BROKEN_THREE
        if (pattern.contains("o-oo-")) score += BROKEN_THREE

        // Simple Three threats
        if (pattern.contains("ooo--x")) score += SIMPLE_THREE
        if (pattern.contains("x--ooo")) score += SIMPLE_THREE

        // Two threats
        if (pattern.contains("--oo--")) score += TWO * 4 // (2,4)
        if (pattern.contains("-oo---")) score += TWO * 3 // (2,3)
        if (pattern.contains("---oo-")) score += TWO * 3 // (2,3)

        // One threats
        if (pattern.contains("--o--")) score += ONE * 5 // (1,5)

        return score
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