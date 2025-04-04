package com.iacobo.wuziqi.ai

import com.iacobo.wuziqi.data.GameState
import java.util.Random
import kotlin.math.abs

/** Connect4 AI engine */
class Connect4AIEngine(private val random: Random = Random()) : GameAI {

    companion object {
        // Player constants
        const val EMPTY = GameState.EMPTY
        const val PLAYER_ONE = GameState.PLAYER_ONE // Human
        const val PLAYER_TWO = GameState.PLAYER_TWO // Computer
    }

    /** Finds the best move for the current game state. */
    override fun findBestMove(gameState: GameState): Pair<Int, Int>? {
        // Handle special game variants
        return findConnect4Move(gameState)
    }

    /** Finds the bottom-most empty row in a column for Connect4. */
    private fun findBottomEmptyRow(gameState: GameState, col: Int): Int {
        // Connect4 is considered to be a 7x6 board with 4-in-a-row to win
        val maxRow =
                if (gameState.boardSize == 7 && gameState.winCondition == 4) {
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

    /** Finds the best move for Connect4 using a specialized approach for vertical boards. */
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
     * Specifically handles blocking open two threats on the bottom row in Connect4. For example:
     * "--rr--" needs to be blocked at position 3 (0-indexed, between the two 'r's).
     */
    private fun findConnect4OpenTwoBlock(
            gameState: GameState,
            bottomRow: Int,
            playerValue: Int
    ): Pair<Int, Int>? {
        val boardSize = gameState.boardSize

        // Check horizontal open two threats on bottom row
        for (col in 0 until boardSize - 3) {
            // Look for two adjacent opponent pieces with spaces on either side
            if (gameState.board[bottomRow][col] == EMPTY &&
                            gameState.board[bottomRow][col + 1] == playerValue &&
                            gameState.board[bottomRow][col + 2] == playerValue &&
                            col + 3 < boardSize &&
                            gameState.board[bottomRow][col + 3] == EMPTY
            ) {

                // Block by playing immediately before or after the pair
                // Prefer blocking before if both are available
                if (gameState.isTileEmpty(bottomRow, col)) {
                    return Pair(bottomRow, col)
                } else if (col + 3 < boardSize && gameState.isTileEmpty(bottomRow, col + 3)) {
                    return Pair(bottomRow, col + 3)
                }
            }
        }

        // Check for pattern with a gap: "r-r"
        for (col in 0 until boardSize - 2) {
            if (gameState.board[bottomRow][col] == playerValue &&
                            gameState.board[bottomRow][col + 1] == EMPTY &&
                            gameState.board[bottomRow][col + 2] == playerValue
            ) {

                // Block the gap
                return Pair(bottomRow, col + 1)
            }
        }

        // Also check for patterns like "-rr-" where we need to block one of the ends
        for (col in 1 until boardSize - 2) {
            if (gameState.board[bottomRow][col - 1] == EMPTY &&
                            gameState.board[bottomRow][col] == playerValue &&
                            gameState.board[bottomRow][col + 1] == playerValue &&
                            gameState.board[bottomRow][col + 2] == EMPTY
            ) {

                // Block one of the ends, prioritize the center side
                val centerCol = boardSize / 2
                if (Math.abs(col - 1 - centerCol) < Math.abs(col + 2 - centerCol)) {
                    return Pair(bottomRow, col - 1)
                } else {
                    return Pair(bottomRow, col + 2)
                }
            }
        }

        return null
    }

    /**
     * Finds forcing moves specific to Connect4 strategy. Looks for potential "trap" setups where
     * playing one move forces a win.
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

    /** Evaluates a Connect4 position with specific heuristics for the game. */
    private fun evaluateConnect4Position(
            gameState: GameState,
            row: Int,
            col: Int,
            playerValue: Int
    ): Int {
        val boardSize = gameState.boardSize
        val opponent = if (playerValue == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        var score = 0

        // Prioritize center columns
        val centerScore = boardSize - 2 * abs(col - boardSize / 2)
        score += centerScore * 3

        // Count potential winning lines through this position
        val directions =
                arrayOf(
                        Pair(0, 1), // Horizontal
                        Pair(1, 0), // Vertical
                        Pair(1, 1), // Diagonal \
                        Pair(1, -1) // Diagonal /
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
                    3 -> score += 1000 // 3 in a row - very strong position
                    2 -> score += 100 // 2 in a row - good position
                    1 -> score += 10 // Just this piece - weak but potential
                }
            } else if (playerCount == 0 && opponentCount > 0) {
                // Defensive evaluation - block opponent's potential lines
                when (opponentCount) {
                    3 -> score -= 800 // Critical to block, but not as valuable as winning
                    2 -> score -= 50 // Should consider blocking
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

    /** Checks if there's a connect-4 win in a specific direction. */
    private fun checkConnect4Line(
            board: Array<IntArray>,
            row: Int,
            col: Int,
            deltaRow: Int,
            deltaCol: Int,
            playerValue: Int
    ): Boolean {
        val boardSize = board.size
        var count = 1 // Start with the current piece

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
