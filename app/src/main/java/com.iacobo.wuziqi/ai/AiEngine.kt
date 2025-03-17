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

        // 3. Check for open four creation
        val openFourMove = findOpenFourMove(gameState, computerPlayer)
        if (openFourMove != null) {
            return openFourMove
        }

        // 4. Block opponent's open four
        val blockOpenFourMove = findOpenFourMove(gameState, humanPlayer)
        if (blockOpenFourMove != null) {
            return blockOpenFourMove
        }

        // 5. Check for simple four creation
        val simpleFourMove = findSimpleFourMove(gameState, computerPlayer)
        if (simpleFourMove != null) {
            return simpleFourMove
        }

        // 6. Block opponent's simple four
        val blockSimpleFourMove = findSimpleFourMove(gameState, humanPlayer)
        if (blockSimpleFourMove != null) {
            return blockSimpleFourMove
        }

        // 7. Handle open three threats - CRITICAL for correct play
        // First check if we need to defend against opponent's open three
        val blockOpenThreeMove = findOpenThreeBlockingMove(gameState, humanPlayer)
        if (blockOpenThreeMove != null) {
            return blockOpenThreeMove
        }

        // 8. Create our own open three if possible
        val createOpenThreeMove = findCreateOpenThreeMove(gameState, computerPlayer)
        if (createOpenThreeMove != null) {
            return createOpenThreeMove
        }

        // 9. Create our own broken three if possible
        val createBrokenThreeMove = findCreateBrokenThreeMove(gameState, computerPlayer)
        if (createBrokenThreeMove != null) {
            return createBrokenThreeMove
        }

        // 10. Fallback to positional evaluation if no threats found
        return findBestPositionalMove(gameState)
    }

    /**
     * Finds a move that properly blocks an open three threat.
     * This is the critical function that ensures correct response to "--ooo--" patterns.
     */
    private fun findOpenThreeBlockingMove(gameState: GameState, playerValue: Int): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val opponentValue = if (playerValue == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE

        // Scan the board for open three patterns
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                // Only consider positions that already have the player's stone
                if (gameState.board[row][col] == playerValue) {
                    // Check all four directions for open three patterns
                    for ((deltaRow, deltaCol) in DIRECTIONS) {
                        val pattern = extractLinePattern(gameState, row, col, deltaRow, deltaCol, playerValue)

                        // Look for standard open three pattern: "--ooo--"
                        val openThreeIndex = pattern.lineString.indexOf("--ooo--")
                        if (openThreeIndex != -1) {
                            // We found an open three! The correct response is to block ADJACENT to the three stones
                            // In "--ooo--", these are positions 1 and 5 (0-indexed)

                            // Find the actual board positions for these critical blocking spots
                            val leftBlockPos = pattern.getPositionAt(openThreeIndex + 1)
                            val rightBlockPos = pattern.getPositionAt(openThreeIndex + 5)

                            // Verify the positions are valid and empty
                            if (leftBlockPos != null &&
                                gameState.isValidPosition(leftBlockPos.first, leftBlockPos.second) &&
                                gameState.isTileEmpty(leftBlockPos.first, leftBlockPos.second)) {
                                return leftBlockPos
                            }

                            if (rightBlockPos != null &&
                                gameState.isValidPosition(rightBlockPos.first, rightBlockPos.second) &&
                                gameState.isTileEmpty(rightBlockPos.first, rightBlockPos.second)) {
                                return rightBlockPos
                            }
                        }

                        // Check for non-standard open three patterns like "-o-oo-" and "-oo-o-"
                        val nonStandardPatterns = listOf("-o-oo-", "-oo-o-")
                        for (patternStr in nonStandardPatterns) {
                            val idx = pattern.lineString.indexOf(patternStr)
                            if (idx != -1) {
                                // Find the gap in the middle that needs to be blocked
                                val gapOffset = when (patternStr) {
                                    "-o-oo-" -> 2
                                    "-oo-o-" -> 3
                                    else -> continue
                                }

                                val gapPos = pattern.getPositionAt(idx + gapOffset)
                                if (gapPos != null &&
                                    gameState.isValidPosition(gapPos.first, gapPos.second) &&
                                    gameState.isTileEmpty(gapPos.first, gapPos.second)) {
                                    return gapPos
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
     * Finds a move that creates an open four pattern.
     */
    private fun findOpenFourMove(gameState: GameState, playerValue: Int): Pair<Int, Int>? {
        val boardSize = gameState.boardSize

        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    // Try placing our stone here
                    gameState.board[row][col] = playerValue

                    // Check if this creates an open four
                    if (hasOpenFour(gameState, row, col, playerValue)) {
                        gameState.board[row][col] = EMPTY
                        return Pair(row, col)
                    }

                    gameState.board[row][col] = EMPTY
                }
            }
        }

        return null
    }

    /**
     * Finds a move that creates a simple four pattern.
     */
    private fun findSimpleFourMove(gameState: GameState, playerValue: Int): Pair<Int, Int>? {
        val boardSize = gameState.boardSize

        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    // Try placing our stone here
                    gameState.board[row][col] = playerValue

                    // Check if this creates a simple four
                    if (hasSimpleFour(gameState, row, col, playerValue)) {
                        gameState.board[row][col] = EMPTY
                        return Pair(row, col)
                    }

                    gameState.board[row][col] = EMPTY
                }
            }
        }

        return null
    }

    /**
     * Finds a move that creates an open three pattern.
     */
    private fun findCreateOpenThreeMove(gameState: GameState, playerValue: Int): Pair<Int, Int>? {
        val boardSize = gameState.boardSize

        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    // Try placing our stone here
                    gameState.board[row][col] = playerValue

                    // Check if this creates an open three
                    if (hasOpenThree(gameState, row, col, playerValue)) {
                        gameState.board[row][col] = EMPTY
                        return Pair(row, col)
                    }

                    gameState.board[row][col] = EMPTY
                }
            }
        }

        return null
    }

    /**
     * Finds a move that creates a broken three pattern.
     */
    private fun findCreateBrokenThreeMove(gameState: GameState, playerValue: Int): Pair<Int, Int>? {
        val boardSize = gameState.boardSize

        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    // Try placing our stone here
                    gameState.board[row][col] = playerValue

                    // Check if this creates a broken three
                    if (hasBrokenThree(gameState, row, col, playerValue)) {
                        gameState.board[row][col] = EMPTY
                        return Pair(row, col)
                    }

                    gameState.board[row][col] = EMPTY
                }
            }
        }

        return null
    }

    /**
     * Checks if a move creates an open four threat.
     */
    private fun hasOpenFour(gameState: GameState, row: Int, col: Int, playerValue: Int): Boolean {
        for ((deltaRow, deltaCol) in DIRECTIONS) {
            val pattern = extractLinePattern(gameState, row, col, deltaRow, deltaCol, playerValue).lineString

            // Check various patterns for open four
            if (pattern.contains("-xxxx-") ||   // Standard open four
                pattern.contains("xx-xx") ||    // Split open four
                pattern.contains("xxx-x") ||    // Non-standard with gap
                pattern.contains("x-xxx")) {    // Non-standard with gap
                return true
            }
        }
        return false
    }

    /**
     * Checks if a move creates a simple four threat.
     */
    private fun hasSimpleFour(gameState: GameState, row: Int, col: Int, playerValue: Int): Boolean {
        for ((deltaRow, deltaCol) in DIRECTIONS) {
            val pattern = extractLinePattern(gameState, row, col, deltaRow, deltaCol, playerValue).lineString

            // Check various patterns for simple four
            if (pattern.contains("xxxx-") ||    // Standard simple four
                pattern.contains("-xxxx") ||    // Standard simple four
                pattern.contains("xx-xx-") ||   // Split with block
                pattern.contains("-xx-xx") ||   // Split with block
                pattern.contains("xxx-x-") ||   // Non-standard with gap
                pattern.contains("-xxx-x") ||   // Non-standard with gap
                pattern.contains("x-xxx-") ||   // Non-standard with gap
                pattern.contains("-x-xxx")) {   // Non-standard with gap
                return true
            }
        }
        return false
    }

    /**
     * Checks if a move creates an open three threat.
     */
    private fun hasOpenThree(gameState: GameState, row: Int, col: Int, playerValue: Int): Boolean {
        for ((deltaRow, deltaCol) in DIRECTIONS) {
            val pattern = extractLinePattern(gameState, row, col, deltaRow, deltaCol, playerValue).lineString

            // Check various patterns for open three
            if (pattern.contains("--xxx--") ||   // Standard open three
                pattern.contains("-x-xx-") ||    // Non-standard with gap
                pattern.contains("-xx-x-") ||    // Non-standard with gap
                pattern.contains("-x--x-x--")) { // Special pattern
                return true
            }
        }
        return false
    }

    /**
     * Checks if a move creates a broken three threat.
     */
    private fun hasBrokenThree(gameState: GameState, row: Int, col: Int, playerValue: Int): Boolean {
        for ((deltaRow, deltaCol) in DIRECTIONS) {
            val pattern = extractLinePattern(gameState, row, col, deltaRow, deltaCol, playerValue).lineString

            // Check various patterns for broken three
            if (pattern.contains("-xxx-o") ||   // Broken three
                pattern.contains("o-xxx-") ||   // Broken three
                pattern.contains("-xx-x") ||    // Non-standard
                pattern.contains("x-xx-") ||    // Non-standard
                pattern.contains("-x-xx") ||    // Non-standard
                pattern.contains("xx-x-")) {    // Non-standard
                return true
            }
        }
        return false
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
                        lineBuilder.append("x")
                        positions.add(Pair(r, c))
                    }
                    opponent -> {
                        lineBuilder.append("o")
                        positions.add(Pair(r, c))
                    }
                    else -> {
                        lineBuilder.append("-")
                        positions.add(Pair(r, c))
                    }
                }
            } else {
                // Off-board positions are treated as blocked
                lineBuilder.append("o")
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
     */
    private fun evaluatePatternScore(pattern: String): Int {
        var score = 0

        // Winning threats
        if (pattern.contains("xxxxx")) score += FIVE

        // Open Four threats
        if (pattern.contains("-xxxx-")) score += OPEN_FOUR
        if (pattern.contains("xx-xx")) score += OPEN_FOUR
        if (pattern.contains("xxx-x")) score += OPEN_FOUR
        if (pattern.contains("x-xxx")) score += OPEN_FOUR

        // Simple Four threats
        if (pattern.contains("xxxx-")) score += SIMPLE_FOUR
        if (pattern.contains("-xxxx")) score += SIMPLE_FOUR
        if (pattern.contains("xx-xx-")) score += SIMPLE_FOUR
        if (pattern.contains("-xx-xx")) score += SIMPLE_FOUR

        // Open Three threats
        if (pattern.contains("--xxx--")) score += OPEN_THREE
        if (pattern.contains("-x-xx-")) score += OPEN_THREE
        if (pattern.contains("-xx-x-")) score += OPEN_THREE

        // Broken Three threats
        if (pattern.contains("-xxx-o")) score += BROKEN_THREE
        if (pattern.contains("o-xxx-")) score += BROKEN_THREE
        if (pattern.contains("-xx-x")) score += BROKEN_THREE
        if (pattern.contains("x-xx-")) score += BROKEN_THREE

        // Simple Three threats
        if (pattern.contains("xxx--o")) score += SIMPLE_THREE
        if (pattern.contains("o--xxx")) score += SIMPLE_THREE

        // Two threats
        if (pattern.contains("--xx--")) score += TWO * 4 // (2,4)
        if (pattern.contains("-xx---")) score += TWO * 3 // (2,3)
        if (pattern.contains("---xx-")) score += TWO * 3 // (2,3)

        // One threats
        if (pattern.contains("--x--")) score += ONE * 5 // (1,5)

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
     * Finds the best move for Connect4.
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

        // 3. Prioritize center column
        val centerCol = boardSize / 2
        val centerRow = findBottomEmptyRow(gameState, centerCol)
        if (centerRow != -1) {
            return Pair(centerRow, centerCol)
        }

        // 4. Play in columns near the center
        val colOrder = (0 until boardSize).sortedBy { abs(it - centerCol) }
        for (col in colOrder) {
            val row = findBottomEmptyRow(gameState, col)
            if (row != -1) {
                return Pair(row, col)
            }
        }

        return null // No valid move (shouldn't happen unless board is full)
    }
}