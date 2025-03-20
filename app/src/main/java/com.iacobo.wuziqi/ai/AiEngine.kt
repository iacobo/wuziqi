package com.iacobo.wuziqi.ai

import com.iacobo.wuziqi.data.GameState
import java.util.Random
import kotlin.math.abs

/** Types of moves with their strategic significance */
enum class MoveType {
    WIN, // Immediate win
    FORCING, // Forces a win in the next move
    URGENT, // Requires immediate response
    TACTICAL, // Creates tactical advantage
    DEVELOPMENT, // Develops position
    POSITIONAL // General positional play
}

/** Defines a pattern to be recognized on the board */
class PatternDefinition(val pattern: String, val priority: Int, val moveType: MoveType)

/** Represents a detected pattern match */
class PatternMatch(val pattern: PatternDefinition, val priority: Int)

/** Evaluation data for a potential move */
class MoveEvaluation(
        val position: Pair<Int, Int>,
        val score: Int,
        val aiPattern: PatternMatch?,
        val opponentPattern: PatternMatch?
)

/**
 * A pattern-based Wuziqi (Gomoku) AI that focuses on recognizing and creating strategic patterns
 * rather than deep tree searching.
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

    /** Finds the best move for the current game state. */
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

    /** Core pattern-based move finding algorithm. */
    private fun findPatternBasedMove(gameState: GameState): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val aiPlayer = PLAYER_TWO
        val humanPlayer = PLAYER_ONE

        // 1. If board is empty or nearly empty, play near center
        if (countStones(gameState) < 3) {
            return playNearCenter(gameState)
        }

        // Get candidate moves (empty spaces near existing stones)
        val candidatePositions = findCandidateMoves(gameState)
        if (candidatePositions.isEmpty()) {
            return playNearCenter(gameState)
        }

        // 2. Check for immediate winning move
        for ((row, col) in candidatePositions) {
            if (!gameState.isTileEmpty(row, col)) continue

            gameState.board[row][col] = aiPlayer
            if (gameState.checkWin(row, col, aiPlayer)) {
                gameState.board[row][col] = EMPTY
                return Pair(row, col)
            }
            gameState.board[row][col] = EMPTY
        }

        // 3. Check for opponent's winning move and block it
        for ((row, col) in candidatePositions) {
            if (!gameState.isTileEmpty(row, col)) continue

            gameState.board[row][col] = humanPlayer
            if (gameState.checkWin(row, col, humanPlayer)) {
                gameState.board[row][col] = EMPTY
                return Pair(row, col)
            }
            gameState.board[row][col] = EMPTY
        }

        // 4. Evaluate each move by pattern matching
        val moveEvaluations = mutableListOf<MoveEvaluation>()

        for ((row, col) in candidatePositions) {
            if (!gameState.isTileEmpty(row, col)) continue

            // Calculate offensive score - patterns we can create
            val offensivePatterns = findPatternsAtPosition(gameState, row, col, aiPlayer)
            val bestOffensivePattern = offensivePatterns.maxByOrNull { it.priority }

            // Calculate defensive score - opponent patterns we can block
            val defensivePatterns = findPatternsAtPosition(gameState, row, col, humanPlayer)
            val bestDefensivePattern = defensivePatterns.maxByOrNull { it.priority }

            // Calculate position score
            val positionScore = calculatePositionalScore(row, col, gameState)

            // Calculate total score for this move
            val totalScore =
                    calculateMoveScore(bestOffensivePattern, bestDefensivePattern, positionScore)

            moveEvaluations.add(
                    MoveEvaluation(
                            Pair(row, col),
                            totalScore,
                            bestOffensivePattern,
                            bestDefensivePattern
                    )
            )
        }

        // 5. Choose the best move
        moveEvaluations.sortByDescending { it.score }

        if (moveEvaluations.isEmpty()) {
            return playNearCenter(gameState)
        }

        // Check for dual-purpose moves that create a threat AND block a threat
        val dualPurposeMoves =
                moveEvaluations.filter {
                    it.aiPattern != null &&
                            it.opponentPattern != null &&
                            it.aiPattern.pattern.moveType in
                                    setOf(MoveType.TACTICAL, MoveType.FORCING) &&
                            it.opponentPattern.pattern.moveType in
                                    setOf(MoveType.TACTICAL, MoveType.FORCING)
                }

        // If we have dual-purpose moves, prioritize them
        if (dualPurposeMoves.isNotEmpty()) {
            return dualPurposeMoves[0].position
        }

        // Return the move with highest score
        return moveEvaluations[0].position
    }

    /** Finds all patterns that would be created or blocked by placing a stone. */
    private fun findPatternsAtPosition(
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

        // Check in all 4 directions
        for ((deltaRow, deltaCol) in DIRECTIONS) {
            // Extract line of 11 cells centered on the newly placed stone
            val line = extractLine(gameState, row, col, deltaRow, deltaCol)

            // Convert line to pattern string (x for player, o for opponent, . for empty)
            val patternString = lineToPatternString(line, player, opponent)

            // Match patterns
            patterns.addAll(matchPatterns(patternString))
        }

        // Restore the board
        gameState.board[row][col] = originalValue

        return patterns
    }

    /** Calculates a score for a potential move. */
    private fun calculateMoveScore(
            offensivePattern: PatternMatch?,
            defensivePattern: PatternMatch?,
            positionScore: Int
    ): Int {
        var score = positionScore

        // Add offensive score
        if (offensivePattern != null) {
            // Critical patterns (win, forcing)
            if (offensivePattern.pattern.moveType in setOf(MoveType.WIN, MoveType.FORCING)) {
                score += offensivePattern.priority
            } else {
                // Regular pattern creation
                score += offensivePattern.priority / 2
            }
        }

        // Add defensive score
        if (defensivePattern != null) {
            // Critical blocks (win, forcing)
            if (defensivePattern.pattern.moveType in
                            setOf(MoveType.WIN, MoveType.FORCING, MoveType.URGENT)
            ) {
                score += defensivePattern.priority
            } else {
                // Regular blocks
                score += defensivePattern.priority / 3
            }
        }

        return score
    }

    /** Matches a pattern string against known patterns. */
    private fun matchPatterns(patternString: String): List<PatternMatch> {
        val matches = mutableListOf<PatternMatch>()

        // Check for five in a row (winning pattern)
        if (patternString.contains(FIVE_IN_ROW.pattern)) {
            matches.add(PatternMatch(FIVE_IN_ROW, FIVE_IN_ROW.priority))
            return matches // Early return since this is the highest priority
        }

        // Check for open four patterns (forcing)
        if (patternString.contains(OPEN_FOUR.pattern)) {
            matches.add(PatternMatch(OPEN_FOUR, OPEN_FOUR.priority))
            return matches
        }

        if (patternString.contains(SPLIT_OPEN_FOUR.pattern)) {
            matches.add(PatternMatch(SPLIT_OPEN_FOUR, SPLIT_OPEN_FOUR.priority))
            return matches
        }

        // Check for simple four patterns
        for (pattern in SIMPLE_FOUR) {
            if (patternString.contains(pattern.pattern)) {
                matches.add(PatternMatch(pattern, pattern.priority))
                return matches
            }
        }

        // Check for open three patterns (critical for creating future threats)
        for (pattern in OPEN_THREE) {
            if (patternString.contains(pattern.pattern)) {
                matches.add(PatternMatch(pattern, pattern.priority))
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

    /** Calculates a positional score for a move. */
    private fun calculatePositionalScore(row: Int, col: Int, gameState: GameState): Int {
        val boardSize = gameState.boardSize
        var score = 0

        // 1. Prefer center positions
        val centerDistance = abs(row - boardSize / 2) + abs(col - boardSize / 2)
        score += (boardSize - centerDistance) * 5

        // 2. Prefer positions adjacent to existing stones
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

    /** Finds valid candidate moves (positions adjacent to existing stones). */
    private fun findCandidateMoves(gameState: GameState): List<Pair<Int, Int>> {
        val boardSize = gameState.boardSize
        val candidates = mutableSetOf<Pair<Int, Int>>()

        // Loop through the board
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                // If we find an existing stone
                if (gameState.board[row][col] != EMPTY) {
                    // Add all empty adjacent cells as candidates
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

        // Try positions near the center
        for (dr in -1..1) {
            for (dc in -1..1) {
                val r = center + dr
                val c = center + dc

                if (r in 0 until boardSize && c in 0 until boardSize && gameState.isTileEmpty(r, c)
                ) {
                    return Pair(r, c)
                }
            }
        }

        // Try a slightly wider area
        for (dr in -2..2) {
            for (dc in -2..2) {
                val r = center + dr
                val c = center + dc

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

        // This should never happen if there's at least one empty space
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

    /** Implementation for Tic-Tac-Toe variant (3x3 board). */
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

    /** Implementation for Connect4 variant (7x7 board). */
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

        // 3. Look for forcing moves (creating a double threat)
        for (col in 0 until boardSize) {
            val row = findBottomEmptyRow(gameState, col)
            if (row != -1) {
                // Place a temporary stone
                gameState.board[row][col] = computerPlayer

                // Count potential winning columns after this move
                var winningCols = 0

                for (nextCol in 0 until boardSize) {
                    val nextRow = findBottomEmptyRow(gameState, nextCol)
                    if (nextRow != -1) {
                        gameState.board[nextRow][nextCol] = computerPlayer
                        if (gameState.checkWin(nextRow, nextCol, computerPlayer)) {
                            winningCols++
                        }
                        gameState.board[nextRow][nextCol] = EMPTY
                    }
                }

                // Remove the temporary stone
                gameState.board[row][col] = EMPTY

                // If this creates multiple winning possibilities, it's a forcing move
                if (winningCols >= 2) {
                    return Pair(row, col)
                }
            }
        }

        // 4. Prefer center columns
        val centerCol = boardSize / 2

        // Score each possible move
        val columnScores = mutableMapOf<Int, Int>()
        for (col in 0 until boardSize) {
            // Base score favors center columns
            val row = findBottomEmptyRow(gameState, col)
            if (row != -1) {
                // Calculate a score for this column
                // Higher score for columns closer to center
                val centerDistance = abs(col - centerCol)
                val score = 100 - (centerDistance * 10)
                columnScores[col] = score
            }
        }

        // Find the highest scored column
        val bestCol = columnScores.entries.maxByOrNull { it.value }?.key ?: centerCol
        val bestRow = findBottomEmptyRow(gameState, bestCol)

        return if (bestRow != -1) Pair(bestRow, bestCol) else null
    }

    /** Finds the bottom-most empty row in a column for Connect4. */
    private fun findBottomEmptyRow(gameState: GameState, col: Int): Int {
        // Connect4 is 7x6 (width x height)
        val maxRow = if (gameState.boardSize == 7) 5 else gameState.boardSize - 1

        for (row in maxRow downTo 0) {
            if (gameState.board[row][col] == EMPTY) {
                return row
            }
        }

        return -1 // Column is full
    }
}
