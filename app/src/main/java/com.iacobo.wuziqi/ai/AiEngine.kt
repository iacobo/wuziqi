package com.iacobo.wuziqi.ai

import com.iacobo.wuziqi.data.GameState
import kotlin.math.abs
import kotlin.math.pow
import java.util.Random

/**
 * AI Engine for Wuziqi game.
 * Handles computer move selection for different game variants.
 */
class WuziqiAIEngine(private val random: Random = Random()) {

    companion object {
        const val PLAYER_ONE = GameState.PLAYER_ONE
        const val PLAYER_TWO = GameState.PLAYER_TWO
        const val EMPTY = GameState.EMPTY
    }

    /**
     * Finds the best move for the AI based on the current game state and type.
     * @return Position to play as Pair(row, col) or null if no valid move found
     */
    fun findBestMove(gameState: GameState): Pair<Int, Int>? {
        return when {
            // TicTacToe (3x3)
            gameState.boardSize == 3 && gameState.winCondition == 3 -> {
                findTicTacToeMove(gameState)
            }
            // Connect4 (7x7 with 4-in-a-row)
            gameState.boardSize == 7 && gameState.winCondition == 4 -> {
                findConnect4Move(gameState)
            }
            // Standard Wuziqi
            else -> {
                findWuziqiMove(gameState)
            }
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
                if (gameState.board[row][col] == EMPTY) {
                    // Try this move
                    gameState.board[row][col] = computerPlayer
                    if (gameState.checkWin(row, col, computerPlayer)) {
                        // We can win, make this move
                        gameState.board[row][col] = EMPTY // Reset for proper handling
                        return Pair(row, col)
                    }
                    // Undo try
                    gameState.board[row][col] = EMPTY
                }
            }
        }
        
        // 2. Block opponent's winning move
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.board[row][col] == EMPTY) {
                    // Try this move for the human
                    gameState.board[row][col] = humanPlayer
                    if (gameState.checkWin(row, col, humanPlayer)) {
                        // Block this winning move
                        gameState.board[row][col] = EMPTY
                        return Pair(row, col)
                    }
                    // Undo try
                    gameState.board[row][col] = EMPTY
                }
            }
        }
        
        // 3. Take center if available
        if (gameState.board[1][1] == EMPTY) {
            return Pair(1, 1)
        }
        
        // 4. Take a corner if available
        val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
        val availableCorners = corners.filter { (row, col) -> 
            gameState.board[row][col] == EMPTY 
        }
        
        if (availableCorners.isNotEmpty()) {
            return availableCorners[random.nextInt(availableCorners.size)]
        }
        
        // 5. Take any available edge
        val edges = listOf(Pair(0, 1), Pair(1, 0), Pair(1, 2), Pair(2, 1))
        val availableEdges = edges.filter { (row, col) -> 
            gameState.board[row][col] == EMPTY 
        }
        
        if (availableEdges.isNotEmpty()) {
            return availableEdges[random.nextInt(availableEdges.size)]
        }
        
        return null // No valid move found (shouldn't happen)
    }

    /**
     * Finds the bottom-most empty row in a column for Connect4
     */
    private fun findBottomEmptyRow(gameState: GameState, col: Int): Int {
        // For Connect4 (7x6 board), we need to only check the 6 rows (0-5)
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
                // Try move
                gameState.board[row][col] = computerPlayer
                if (gameState.checkWin(row, col, computerPlayer)) {
                    // We can win with this move
                    gameState.board[row][col] = EMPTY
                    return Pair(row, col)
                }
                gameState.board[row][col] = EMPTY
            }
        }
        
        // 2. Block human win
        for (col in 0 until boardSize) {
            val row = findBottomEmptyRow(gameState, col)
            if (row != -1) {
                // Try move for human
                gameState.board[row][col] = humanPlayer
                if (gameState.checkWin(row, col, humanPlayer)) {
                    // Block this winning move
                    gameState.board[row][col] = EMPTY
                    return Pair(row, col)
                }
                gameState.board[row][col] = EMPTY
            }
        }
        
        // 3. Favor center column
        val centerCol = boardSize / 2
        val centerRow = findBottomEmptyRow(gameState, centerCol)
        if (centerRow != -1) {
            return Pair(centerRow, centerCol)
        }
        
        // 4. Otherwise, prioritize columns near the center
        val colPriorities = (0 until boardSize).sortedBy { abs(it - centerCol) }
        for (col in colPriorities) {
            val row = findBottomEmptyRow(gameState, col)
            if (row != -1) {
                return Pair(row, col)
            }
        }
        
        return null // No valid move found (shouldn't happen)
    }

    /**
     * Finds the best move for standard Wuziqi game.
     */
    private fun findWuziqiMove(gameState: GameState): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val computerPlayer = PLAYER_TWO
        val humanPlayer = PLAYER_ONE
        
        // 1. Check for immediate win or forced defense
        
        // 1a. Look for winning moves (five or open four)
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    // Temporarily place computer's stone
                    gameState.board[row][col] = computerPlayer
                    
                    // Check for an immediate win
                    if (gameState.checkWin(row, col, computerPlayer)) {
                        // Win found, make this move
                        gameState.board[row][col] = EMPTY
                        return Pair(row, col)
                    }
                    
                    // Look for open four (which guarantees a win next move)
                    if (hasOpenFour(gameState, row, col, computerPlayer)) {
                        // Open four found, make this move
                        gameState.board[row][col] = EMPTY
                        return Pair(row, col)
                    }
                    
                    // Reset board
                    gameState.board[row][col] = EMPTY
                }
            }
        }
        
        // 1b. Check if opponent has a winning move that we must block
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    // Check if opponent would win here
                    gameState.board[row][col] = humanPlayer
                    if (gameState.checkWin(row, col, humanPlayer) || hasOpenFour(gameState, row, col, humanPlayer)) {
                        // Must block this threat
                        gameState.board[row][col] = EMPTY
                        return Pair(row, col)
                    }
                    gameState.board[row][col] = EMPTY
                }
            }
        }
        
        // 1c. Improved check for opponent's forcing threats, particularly open three
        val openThreeBlock = findOpenThreeBlockingMove(gameState, humanPlayer)
        if (openThreeBlock != null) {
            return openThreeBlock
        }
        
        // 2. Look for forcing threats (simple four, open three, broken three)
        var bestForcingMove: Pair<Int, Int>? = null
        var bestForcingScore = Int.MIN_VALUE
        
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    // Temporarily place stone and evaluate
                    gameState.board[row][col] = computerPlayer
                    val score = evaluatePosition(gameState, computerPlayer)
                    gameState.board[row][col] = EMPTY
                    
                    // For forcing threats, we're looking for scores above a certain threshold
                    if (score > 900000 && score > bestForcingScore) {
                        bestForcingScore = score
                        bestForcingMove = Pair(row, col)
                    }
                }
            }
        }
        
        if (bestForcingMove != null) {
            return bestForcingMove
        }
        
        // 3. Find the best move based on the heuristic pattern evaluation
        var bestScore = Int.MIN_VALUE
        var bestMove: Pair<Int, Int>? = null
        
        // Only consider positions near existing pieces to reduce search space
        val candidates = findCandidateMoves(gameState)
        
        // If no candidate moves (empty board), place in center
        if (candidates.isEmpty()) {
            val center = boardSize / 2
            return Pair(center, center)
        }
        
        // Evaluate each candidate position
        for ((row, col) in candidates) {
            if (gameState.isTileEmpty(row, col)) {
                // Temporarily place stone and evaluate
                gameState.board[row][col] = computerPlayer
                val computerScore = evaluatePosition(gameState, computerPlayer)
                val humanScore = evaluatePosition(gameState, humanPlayer)
                gameState.board[row][col] = EMPTY
                
                // Final score is the difference (how good for computer minus how good for human)
                val score = computerScore - humanScore
                
                if (score > bestScore) {
                    bestScore = score
                    bestMove = Pair(row, col)
                }
            }
        }
        
        // Make the best move
        if (bestMove != null) {
            return bestMove
        }
        
        // Fall back to any valid move if no best move found
        val emptyPositions = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    emptyPositions.add(Pair(row, col))
                }
            }
        }
        
        return if (emptyPositions.isNotEmpty()) {
            emptyPositions[random.nextInt(emptyPositions.size)]
        } else null
    }

    /**
     * Find a move that blocks opponent's open three threat
     */
    private fun findOpenThreeBlockingMove(gameState: GameState, opponentPlayer: Int): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val computerPlayer = if (opponentPlayer == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    // Check if this position blocks an open three threat
                    // Create a temporary board with our move here
                    val tempBoard = Array(boardSize) { r ->
                        IntArray(boardSize) { c ->
                            gameState.board[r][c]
                        }
                    }
                    tempBoard[row][col] = computerPlayer
                    
                    // Check if opponent has open three threats on this board
                    var blocksOpenThree = false
                    
                    // Check all empty positions to see if opponent would create an open three there
                    for (r in 0 until boardSize) {
                        for (c in 0 until boardSize) {
                            if (r == row && c == col) continue // Skip our move
                            if (tempBoard[r][c] != EMPTY) continue
                            
                            // Try opponent move here
                            tempBoard[r][c] = opponentPlayer
                            
                            // Check specifically for open three patterns
                            val threatScore = evaluateDirectionFromPosition(gameState, tempBoard, r, c, 1, 0, opponentPlayer) +
                                            evaluateDirectionFromPosition(gameState, tempBoard, r, c, 0, 1, opponentPlayer) +
                                            evaluateDirectionFromPosition(gameState, tempBoard, r, c, 1, 1, opponentPlayer) +
                                            evaluateDirectionFromPosition(gameState, tempBoard, r, c, 1, -1, opponentPlayer)
                            
                            // Open three has score of 90000 in evaluateLinePatterns
                            if (threatScore >= 90000) {
                                // This is a threatening move by opponent
                                // Check if placing our piece at (row,col) prevents this threat
                                tempBoard[r][c] = EMPTY
                                
                                // Try the move without our stone
                                tempBoard[row][col] = EMPTY
                                
                                // Calculate opponent's threat score before our move
                                tempBoard[r][c] = opponentPlayer
                                val beforeBlockScore = evaluateDirectionFromPosition(gameState, tempBoard, r, c, 1, 0, opponentPlayer) +
                                                    evaluateDirectionFromPosition(gameState, tempBoard, r, c, 0, 1, opponentPlayer) +
                                                    evaluateDirectionFromPosition(gameState, tempBoard, r, c, 1, 1, opponentPlayer) +
                                                    evaluateDirectionFromPosition(gameState, tempBoard, r, c, 1, -1, opponentPlayer)
                                
                                tempBoard[r][c] = EMPTY
                                
                                // Restore our stone
                                tempBoard[row][col] = computerPlayer
                                
                                // Calculate opponent's threat score after our move
                                tempBoard[r][c] = opponentPlayer
                                val afterBlockScore = evaluateDirectionFromPosition(gameState, tempBoard, r, c, 1, 0, opponentPlayer) +
                                                    evaluateDirectionFromPosition(gameState, tempBoard, r, c, 0, 1, opponentPlayer) +
                                                    evaluateDirectionFromPosition(gameState, tempBoard, r, c, 1, 1, opponentPlayer) +
                                                    evaluateDirectionFromPosition(gameState, tempBoard, r, c, 1, -1, opponentPlayer)
                                
                                tempBoard[r][c] = EMPTY
                                
                                // If our move reduces the threat score significantly
                                if (afterBlockScore < 90000 && beforeBlockScore >= 90000) {
                                    blocksOpenThree = true
                                    break
                                }
                            }
                            
                            // Reset for next check
                            tempBoard[r][c] = EMPTY
                        }
                        if (blocksOpenThree) break
                    }
                    
                    if (blocksOpenThree) {
                        // This move blocks an open three threat
                        return Pair(row, col)
                    }
                }
            }
        }
        
        return null
    }

    /**
     * Helper function to check for open four at a position
     */
    private fun hasOpenFour(gameState: GameState, row: Int, col: Int, playerValue: Int): Boolean {
        val directions = listOf(
            Pair(1, 0),    // Horizontal
            Pair(0, 1),    // Vertical
            Pair(1, 1),    // Diagonal \
            Pair(1, -1)    // Diagonal /
        )
        
        for ((deltaRow, deltaCol) in directions) {
            if (hasOpenFourInDirection(gameState, row, col, deltaRow, deltaCol, playerValue)) {
                return true
            }
        }
        
        return false
    }

    /**
     * Check for open four in a specific direction
     */
    private fun hasOpenFourInDirection(gameState: GameState, row: Int, col: Int, deltaRow: Int, deltaCol: Int, playerValue: Int): Boolean {
        val boardSize = gameState.boardSize
        val opponent = if (playerValue == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        
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
    private fun findCandidateMoves(gameState: GameState): List<Pair<Int, Int>> {
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
                if (gameState.board[row][col] != EMPTY) {
                    // Check all adjacent positions
                    for ((dr, dc) in directions) {
                        val newRow = row + dr
                        val newCol = col + dc
                        
                        // Add valid empty positions
                        if (gameState.isValidPosition(newRow, newCol) && 
                            gameState.board[newRow][newCol] == EMPTY) {
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
     */
    private fun evaluatePosition(gameState: GameState, playerValue: Int): Int {
        var totalScore = 0
        val boardSize = gameState.boardSize
        
        // For each empty intersection
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.board[row][col] == EMPTY) {
                    // Calculate threat values if player places a stone here
                    val threatValues = mutableListOf<Int>()
                    
                    // Temporarily place player's stone to evaluate position
                    gameState.board[row][col] = playerValue
                    
                    // Check all 4 directions
                    threatValues.add(evaluateDirectionFromPosition(gameState, gameState.board, row, col, 1, 0, playerValue))  // Horizontal
                    threatValues.add(evaluateDirectionFromPosition(gameState, gameState.board, row, col, 0, 1, playerValue))  // Vertical
                    threatValues.add(evaluateDirectionFromPosition(gameState, gameState.board, row, col, 1, 1, playerValue))  // Diagonal \
                    threatValues.add(evaluateDirectionFromPosition(gameState, gameState.board, row, col, 1, -1, playerValue)) // Diagonal /
                    
                    // Remove temporary stone
                    gameState.board[row][col] = EMPTY
                    
                    // Sort threat values in descending order
                    threatValues.sortDescending()
                    
                    // Take the two best threats
                    val bestThreat = threatValues.getOrElse(0) { 0 }
                    val secondBestThreat = threatValues.getOrElse(1) { 0 }
                    
                    // Using the formula: 1.5 * 1.8^a + 1.8^b
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
     */
    private fun evaluateDirectionFromPosition(
        gameState: GameState,
        board: Array<IntArray>,
        row: Int, 
        col: Int, 
        deltaRow: Int, 
        deltaCol: Int, 
        playerValue: Int
    ): Int {
        val opponent = if (playerValue == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        val boardSize = gameState.boardSize
        
        // Extract the line in this direction (with current player's stone at center)
        val line = StringBuilder()
        
        // Look 5 spaces in each direction
        for (i in -5..5) {
            val r = row + i * deltaRow
            val c = col + i * deltaCol
            
            if (r in 0 until boardSize && c in 0 until boardSize) {
                when (board[r][c]) {
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
     * Evaluates a line for pattern matching using the threat classification.
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
        if (line.contains("-x-xx-")) score += 90000    // Non-standard open three
        if (line.contains("-xx-x-")) score += 90000    // Non-standard open three
        if (line.contains("-x--x-x--")) score += 90000 // Pattern from article example
        
        // Broken Three (3,2) - two ways to complete
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
        if (line.contains("-ooo--")) score -= 90000   // Standard open three
        if (line.contains("--ooo-")) score -= 90000   // Standard open three
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