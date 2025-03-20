package com.iacobo.wuziqi.ai

import com.iacobo.wuziqi.data.GameState
import java.util.Random
import kotlin.math.abs

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
        const val PLAYER_ONE = GameState.PLAYER_ONE // Human
        const val PLAYER_TWO = GameState.PLAYER_TWO // Computer

        // Direction vectors for pattern detection
        val DIRECTIONS =
                arrayOf(
                        Pair(1, 0), // Horizontal
                        Pair(0, 1), // Vertical
                        Pair(1, 1), // Diagonal \
                        Pair(1, -1) // Diagonal /
                )

        // Pattern definitions with their respective priorities
        // x = AI stone, o = opponent stone, . = empty
        // Winning patterns
        val FIVE_IN_ROW = PatternDefinition("xxxxx", 10000000, MoveType.WIN)

        // Forcing patterns (guaranteed win in the next move)
        val OPEN_FOUR = PatternDefinition(".xxxx.", 1000000, MoveType.FORCING)
        val SPLIT_OPEN_FOUR = PatternDefinition("xx.xx", 1000000, MoveType.FORCING)

        // Forcing patterns (require a response from opponent)
        val SIMPLE_FOUR =
                listOf(
                        PatternDefinition("xxxx.", 100000, MoveType.URGENT),
                        PatternDefinition(".xxxx", 100000, MoveType.URGENT),
                        PatternDefinition("xxx.x", 100000, MoveType.URGENT),
                        PatternDefinition("x.xxx", 100000, MoveType.URGENT)
                )

        // Strong tactical patterns
        val OPEN_THREE =
                listOf(
                        PatternDefinition("..xxx..", 20000, MoveType.TACTICAL),
                        PatternDefinition(".xxx..", 20000, MoveType.TACTICAL),
                        PatternDefinition("..xxx.", 20000, MoveType.TACTICAL),
                        PatternDefinition(".xx.x.", 20000, MoveType.TACTICAL),
                        PatternDefinition(".x.xx.", 20000, MoveType.TACTICAL)
                )

        // Developmental patterns
        val OPEN_TWO =
                listOf(
                        PatternDefinition("..xx..", 1000, MoveType.DEVELOPMENT),
                        PatternDefinition(".x.x.", 1000, MoveType.DEVELOPMENT)
                )
    }

    /** Finds the best move for the current game state based on pattern recognition. */
    fun findBestMove(gameState: GameState): Pair<Int, Int>? {
        // Handle special game variants
        return when {
            gameState.boardSize == 3 && gameState.winCondition == 3 -> {
                findTicTacToeMove(gameState)
            }
            gameState.boardSize == 7 && gameState.winCondition == 4 -> {
                findConnect4Move(gameState)
            }
            else -> {
                findPatternBasedMove(gameState)
            }
        }
    }

    /**
     * Core pattern-based move finding algorithm. Prioritizes moves based on pattern matching and
     * strategic potential.
     */
    private fun findPatternBasedMove(gameState: GameState): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val aiPlayer = PLAYER_TWO
        val humanPlayer = PLAYER_ONE

        // 1. If board is empty or nearly empty, play near center
        if (countStones(gameState) < 3) {
            return playNearCenter(gameState)
        }

        // Create candidate move list
        val candidatePositions = findCandidateMoves(gameState)
        if (candidatePositions.isEmpty()) {
            return playNearCenter(gameState)
        }

        // Evaluate each move by pattern matching
        val moveEvaluations = mutableListOf<MoveEvaluation>()

        for (position in candidatePositions) {
            val (row, col) = position
            if (!gameState.isTileEmpty(row, col)) continue

            // Evaluate AI's patterns after placing here
            val aiPatterns = findPatterns(gameState, row, col, aiPlayer)

            // Evaluate opponent's patterns if they played here instead
            val opponentPatterns = findPatterns(gameState, row, col, humanPlayer)

            // Determine best patterns for both AI and opponent
            val bestAiPattern = aiPatterns.maxByOrNull { it.priority }
            val bestOpponentPattern = opponentPatterns.maxByOrNull { it.priority }

            // Calculate a comprehensive score for this move
            val score = calculateMoveScore(bestAiPattern, bestOpponentPattern, row, col, gameState)

            moveEvaluations.add(MoveEvaluation(position, score, bestAiPattern, bestOpponentPattern))
        }

        // Sort moves by score and get the best one
        moveEvaluations.sortByDescending { it.score }

        // Return best move, with tie breaking for moves that accomplish multiple goals
        return chooseBestMove(moveEvaluations, gameState)
    }

    /** Finds all patterns that would be created by placing a stone at the specified position. */
    private fun findPatterns(
            gameState: GameState,
            row: Int,
            col: Int,
            player: Int
    ): List<PatternMatch> {
        val patterns = mutableListOf<PatternMatch>()
        val opponent = if (player == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE

        // Temporarily place the stone
        val originalValue = gameState.board[row][col]
        gameState.board[row][col] = player

        // Check patterns in all 4 directions
        for ((deltaRow, deltaCol) in DIRECTIONS) {
            // Extract line of 11 cells centered on the newly placed stone
            val line = extractLine(gameState, row, col, deltaRow, deltaCol)

            // Convert line to pattern string (x for player, o for opponent, . for empty)
            val patternString = lineToPatternString(line, player, opponent)

            // Match against known patterns
            patterns.addAll(matchPatterns(patternString))
        }

        // Restore the board
        gameState.board[row][col] = originalValue

        return patterns
    }

    /** Extracts a line of cells in a specific direction. */
    private fun extractLine(
            gameState: GameState,
            row: Int,
            col: Int,
            deltaRow: Int,
            deltaCol: Int
    ): List<Int> {
        val line = mutableListOf<Int>()
        val boardSize = gameState.boardSize

        // Look 5 cells in each direction (11 cells total including the center)
        for (i in -5..5) {
            val r = row + i * deltaRow
            val c = col + i * deltaCol

            if (r in 0 until boardSize && c in 0 until boardSize) {
                line.add(gameState.board[r][c])
            } else {
                // Use a special value for off-board cells
                line.add(-1)
            }
        }

        return line
    }

    /** Converts a line of cells to a pattern string. */
    private fun lineToPatternString(line: List<Int>, player: Int, opponent: Int): String {
        return line
                .map { cell ->
                    when (cell) {
                        player -> 'x' // Player's stone
                        opponent -> 'o' // Opponent's stone
                        EMPTY -> '.' // Empty cell
                        else -> '#' // Off-board or invalid
                    }
                }
                .joinToString("")
    }

    /** Matches a pattern string against known patterns. */
    private fun matchPatterns(patternString: String): List<PatternMatch> {
        val matches = mutableListOf<PatternMatch>()

        // Check for five in a row (winning pattern)
        if (patternString.contains(FIVE_IN_ROW.pattern)) {
            matches.add(PatternMatch(FIVE_IN_ROW, FIVE_IN_ROW.priority))
            return matches // Early return since this is the highest priority
        }

        // Check for open four patterns
        if (patternString.contains(OPEN_FOUR.pattern)) {
            matches.add(PatternMatch(OPEN_FOUR, OPEN_FOUR.priority))
            return matches // Early return since this is very high priority
        }

        if (patternString.contains(SPLIT_OPEN_FOUR.pattern)) {
            matches.add(PatternMatch(SPLIT_OPEN_FOUR, SPLIT_OPEN_FOUR.priority))
            return matches // Early return since this is very high priority
        }

        // Check for simple four patterns
        for (pattern in SIMPLE_FOUR) {
            if (patternString.contains(pattern.pattern)) {
                matches.add(PatternMatch(pattern, pattern.priority))
                return matches // Early return since this is high priority
            }
        }

        // Check for open three patterns
        for (pattern in OPEN_THREE) {
            if (patternString.contains(pattern.pattern)) {
                matches.add(PatternMatch(pattern, pattern.priority))
                // Don't return early, as we might find multiple patterns
            }
        }

        // Check for open two patterns
        for (pattern in OPEN_TWO) {
            if (patternString.contains(pattern.pattern)) {
                matches.add(PatternMatch(pattern, pattern.priority))
            }
        }

        return matches
    }

    /** Calculates a comprehensive score for a potential move. */
    private fun calculateMoveScore(
            bestAiPattern: PatternMatch?,
            bestOpponentPattern: PatternMatch?,
            row: Int,
            col: Int,
            gameState: GameState
    ): Int {
        var score = 0

        // Score based on AI pattern creation
        if (bestAiPattern != null) {
            score += bestAiPattern.priority
        }

        // Score based on blocking opponent
        if (bestOpponentPattern != null) {
            // Blocking opponent's winning or forcing patterns is critical
            if (bestOpponentPattern.pattern.moveType == MoveType.WIN ||
                            bestOpponentPattern.pattern.moveType == MoveType.FORCING
            ) {
                score += bestOpponentPattern.priority
            } else {
                // Blocking non-critical patterns is good but not as important
                score += bestOpponentPattern.priority / 2
            }
        }

        // Add positional score
        score += calculatePositionalScore(row, col, gameState)

        return score
    }

    /** Calculates a positional score for a move based on board position. */
    private fun calculatePositionalScore(row: Int, col: Int, gameState: GameState): Int {
        val boardSize = gameState.boardSize
        var score = 0

        // Prefer center positions
        val centerDistance = abs(row - boardSize / 2) + abs(col - boardSize / 2)
        score += (boardSize - centerDistance) * 5

        // Prefer positions adjacent to existing stones
        val adjacentStones = countAdjacentStones(gameState, row, col)
        score += adjacentStones * 50

        return score
    }

    /** Counts stones adjacent to a position. */
    private fun countAdjacentStones(gameState: GameState, row: Int, col: Int): Int {
        var count = 0
        val boardSize = gameState.boardSize

        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue

                val r = row + dr
                val c = col + dc

                if (r in 0 until boardSize &&
                                c in 0 until boardSize &&
                                gameState.board[r][c] != EMPTY
                ) {
                    count++
                }
            }
        }

        return count
    }

    /**
     * Chooses the best move from evaluated candidates, with special handling for moves that
     * accomplish multiple goals.
     */
    private fun chooseBestMove(
            moveEvaluations: List<MoveEvaluation>,
            gameState: GameState
    ): Pair<Int, Int>? {
        if (moveEvaluations.isEmpty()) return null

        // Check if there's a dominant best move
        val bestScore = moveEvaluations[0].score
        val topCandidates = moveEvaluations.filter { it.score >= bestScore * 0.95 }

        // If we have multiple top candidates, prefer those that accomplish multiple goals
        if (topCandidates.size > 1) {
            // Prioritize moves that both create a good pattern AND block opponent's good pattern
            val dualPurposeMoves =
                    topCandidates.filter {
                        it.aiPattern != null &&
                                it.aiPattern.pattern.moveType in
                                        setOf(MoveType.TACTICAL, MoveType.DEVELOPMENT) &&
                                it.opponentPattern != null &&
                                it.opponentPattern.pattern.moveType in
                                        setOf(MoveType.FORCING, MoveType.TACTICAL)
                    }

            if (dualPurposeMoves.isNotEmpty()) {
                return dualPurposeMoves[0].position
            }

            // If no dual-purpose moves, pick the best one or randomly from top candidates
            return if (random.nextDouble() < 0.8) {
                // 80% chance to pick the absolute best move
                topCandidates[0].position
            } else {
                // 20% chance to pick a random top candidate for variety
                topCandidates[random.nextInt(topCandidates.size)].position
            }
        }

        // Return the best move
        return moveEvaluations[0].position
    }

    /** Finds all valid candidate moves (positions adjacent to existing stones). */
    private fun findCandidateMoves(gameState: GameState): List<Pair<Int, Int>> {
        val boardSize = gameState.boardSize
        val candidates = mutableSetOf<Pair<Int, Int>>()

        // Find all empty positions within 2 cells of any existing stone
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.board[row][col] != EMPTY) {
                    // Add empty cells within 2 steps
                    for (dr in -2..2) {
                        for (dc in -2..2) {
                            if (dr == 0 && dc == 0) continue

                            val r = row + dr
                            val c = col + dc

                            if (r in 0 until boardSize &&
                                            c in 0 until boardSize &&
                                            gameState.board[r][c] == EMPTY
                            ) {
                                candidates.add(Pair(r, c))
                            }
                        }
                    }
                }
            }
        }

        return candidates.toList()
    }

    /** Plays near the center of the board (for opening moves). */
    private fun playNearCenter(gameState: GameState): Pair<Int, Int> {
        val boardSize = gameState.boardSize
        val center = boardSize / 2

        // Try the center first
        if (gameState.isTileEmpty(center, center)) {
            return Pair(center, center)
        }

        // Try positions near the center with a slight random offset
        val offset = random.nextInt(3) - 1 // -1, 0, or 1

        for (dr in -2..2) {
            for (dc in -2..2) {
                val r = center + dr + offset
                val c = center + dc + offset

                if (r in 0 until boardSize && c in 0 until boardSize && gameState.isTileEmpty(r, c)
                ) {
                    return Pair(r, c)
                }
            }
        }

        // Fallback to any empty position
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    return Pair(row, col)
                }
            }
        }

        // Board is full (shouldn't happen)
        return Pair(0, 0)
    }

    /** Counts the total number of stones on the board. */
    private fun countStones(gameState: GameState): Int {
        var count = 0
        val boardSize = gameState.boardSize

        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.board[row][col] != EMPTY) {
                    count++
                }
            }
        }

        return count
    }

    /** Finds the best move for TicTacToe. */
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
