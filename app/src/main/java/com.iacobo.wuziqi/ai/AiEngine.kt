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
            "x-xxx-",     // Left-side variation
            "-xxx-x",     // Right-side variation
            "x-ooo--",    // Left side blocked, must block right
            "--ooo-x",    // Right side blocked, must block left
            "x-ooo-x",    // Both sides blocked, gap in middle
            "xxooo--",    // Special case
            "--ooox",     // Special case
            "-xx-x",      // Non-standard 
            "x-xx-",      // Non-standard
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

        // 1. Check for immediate win (five)
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

        // 2. Check for opponent's immediate win and block it
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

        // 7. Handle opponent's forcing threats (open three, broken three)
        val blockForcingMove = findForcingThreatBlock(gameState, humanPlayer)
        if (blockForcingMove != null) {
            return blockForcingMove
        }

        // 8. Create our own open three if possible
        val createOpenThreeMove = findThreateningMove(gameState, computerPlayer, OPEN_THREE_PATTERNS)
        if (createOpenThreeMove != null) {
            return createOpenThreeMove
        }

        // 9. Create our own broken three if possible
        val createBrokenThreeMove = findThreateningMove(gameState, computerPlayer, BROKEN_THREE_PATTERNS)
        if (createBrokenThreeMove != null) {
            return createBrokenThreeMove
        }

        // 10. Fallback to positional evaluation if no threats found
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
     * Finds a move that blocks forcing threats (open threes or broken threes).
     * Prioritizes specific blocking positions based on the threat type.
     */
    private fun findForcingThreatBlock(gameState: GameState, playerValue: Int): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val opponentValue = if (playerValue == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE

        // Block structure to define how to block specific patterns
        data class ThreatPattern(val pattern: String, val blockOffsets: List<Int>)
        
        // Define threat patterns and their corresponding blocking positions
        val threatPatterns = listOf(
            // Open Three patterns (critical to block correctly!)
            ThreatPattern("--ooo--", listOf(1, 5)),  // Standard open three: block adjacent to the three stones
            
            // Broken Three patterns - now with consistent handling
            ThreatPattern("x-ooo--", listOf(1, 6)),  // Left blocked: block at gap or right end
            ThreatPattern("--ooo-x", listOf(0, 5)),  // Right blocked: block at left end or gap
            ThreatPattern("x-ooo-x", listOf(1, 5)),  // Both sides blocked: block either gap
            
            // Special cases
            ThreatPattern("xxooo--", listOf(5)),     // Special case: must block right
            ThreatPattern("--ooox", listOf(1)),      // Special case: must block left
            
            // Non-standard patterns with gaps
            ThreatPattern("-o-oo-", listOf(2)),      // Gap in middle
            ThreatPattern("-oo-o-", listOf(3)),      // Gap in middle
            ThreatPattern("o--oo-", listOf(2)),      // Second gap
            ThreatPattern("-oo--o", listOf(3))       // First gap
        )

        // Debug information for pattern matching
        var debugInfo = StringBuilder()

        // Scan the board for threat patterns
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.board[row][col] == playerValue) {
                    for ((deltaRow, deltaCol) in DIRECTIONS) {
                        val linePattern = extractLinePattern(gameState, row, col, deltaRow, deltaCol, playerValue)
                        
                        debugInfo.append("Position ($row, $col): Pattern = ${linePattern.lineString}\n")
                        
                        // Check each threat pattern
                        for (threat in threatPatterns) {
                            val patternStr = threat.pattern
                            val idx = linePattern.lineString.indexOf(patternStr)
                            if (idx != -1) {
                                debugInfo.append("Found pattern '${patternStr}' at index $idx\n")
                                
                                // Try each possible blocking position
                                for (offset in threat.blockOffsets) {
                                    val blockPos = linePattern.getPositionAt(idx + offset)
                                    debugInfo.append("  Trying block at offset $offset -> position $blockPos\n")
                                    
                                    if (blockPos != null &&
                                        gameState.isValidPosition(blockPos.first, blockPos.second) &&
                                        gameState.isTileEmpty(blockPos.first, blockPos.second)) {
                                        
                                        // Debugging: print pattern before and after blocking
                                        val beforeStr = linePattern.lineString
                                        val afterStr = StringBuilder(beforeStr).also { 
                                            if (idx + offset < beforeStr.length) {
                                                it.setCharAt(idx + offset, 'x') 
                                            }
                                        }.toString()
                                        debugInfo.append("  Block successful! Pattern would change from $beforeStr to $afterStr\n")
                                        
                                        // Check if this actually matches what we expect
                                        if (patternStr == "--ooo--" && offset == 5) {
                                            // This should produce "--ooox-", not "--ooo-x"
                                            val expectedResult = "--ooox-"
                                            val actualResult = afterStr.substring(idx, idx + patternStr.length)
                                            debugInfo.append("  For open three: Expected $expectedResult, Actual $actualResult\n")
                                        }
                                        
                                        return blockPos
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Print debug info if we couldn't find a blocking move
        //println("DEBUG: $debugInfo")
        
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

        // If the board is empty or nearly empty, play near the center
        if (countStones(gameState) < 5) {
            // Play close to center
            val center = boardSize / 2

            // Try the center first
            if (gameState.isTileEmpty(center, center)) {
                return Pair(center, center)
            }

            // Try spots around the center
            for (dr in -1..1) {
                for (dc in -1..1) {
                    val r = center + dr
                    val c = center + dc
                    if (gameState.isValidPosition(r, c) && gameState.isTileEmpty(r, c)) {
                        return Pair(r, c)
                    }
                }
            }
        }

        // Look for positions adjacent to existing stones
        val candidates = findCandidateMoves(gameState)
        if (candidates.isNotEmpty()) {
            var bestScore = Int.MIN_VALUE
            var bestMove: Pair<Int, Int>? = null

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

        // If no good candidates, just find any empty spot
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    return Pair(row, col)
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
     * Based on the evaluation formula from the OOOOO bot description.
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
        if (pattern.contains("ooo-o")) score += OPEN_FOUR
        if (pattern.contains("o-ooo")) score += OPEN_FOUR

        // Simple Four threats
        if (pattern.contains("oooo-")) score += SIMPLE_FOUR
        if (pattern.contains("-oooo")) score += SIMPLE_FOUR
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

        // 3. Look for Connect4-specific forced win setups
        val forcingMoveResult = findConnect4ForcingMove(gameState, computerPlayer)
        if (forcingMoveResult != null) {
            return forcingMoveResult
        }

        // 4. Block opponent's forcing moves
        val blockForcingMoveResult = findConnect4ForcingMove(gameState, humanPlayer)
        if (blockForcingMoveResult != null) {
            return blockForcingMoveResult
        }

        // 5. Evaluate positions with Connect4-specific heuristics
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