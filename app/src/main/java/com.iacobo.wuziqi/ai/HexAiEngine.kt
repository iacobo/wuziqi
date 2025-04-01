package com.iacobo.wuziqi.ai

import com.iacobo.wuziqi.data.GameState
import java.util.PriorityQueue
import java.util.Random
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * A strong AI player for Hex using Monte Carlo Tree Search (MCTS) with virtual connection analysis
 * and strategic patterns.
 *
 * Hex is a complex strategy game with a high branching factor, where the first player has a
 * theoretical winning strategy. This AI combines several techniques to create a strong, challenging
 * opponent:
 *
 * 1. Monte Carlo Tree Search - for balanced exploration/exploitation
 * 2. Virtual connection detection - to identify strong potential connections
 * 3. Strategic pattern recognition - for edge templates and common patterns
 * 4. Bridge and connection analysis - to maintain connectivity
 */
class HexAIEngine(private val random: Random = Random()) {

    companion object {
        // Board values for easy reference
        const val EMPTY = GameState.EMPTY
        const val PLAYER_ONE = GameState.PLAYER_ONE // Red, connects top-bottom
        const val PLAYER_TWO = GameState.PLAYER_TWO // Blue, connects left-right

        // Monte Carlo simulation parameters
        private const val SIMULATION_COUNT = 1000 // Number of simulations per move
        private const val UCT_CONSTANT = 1.41 // Exploration constant (sqrt(2))

        // For 11x11 boards, assign values to specific positions
        // 0-10 are normalized coordinates for any board size
        private val POSITION_VALUES =
                mapOf(
                        // Center has high value
                        Pair(5, 5) to 3.0,
                        // Adjacent to center
                        Pair(4, 5) to 2.0,
                        Pair(6, 5) to 2.0,
                        Pair(5, 4) to 2.0,
                        Pair(5, 6) to 2.0,
                        Pair(4, 4) to 1.8,
                        Pair(6, 6) to 1.8,
                        Pair(4, 6) to 1.8,
                        Pair(6, 4) to 1.8,
                        // Edge positions are important in Hex
                        Pair(0, 5) to 1.5,
                        Pair(10, 5) to 1.5,
                        Pair(5, 0) to 1.5,
                        Pair(5, 10) to 1.5
                )

        // Common Hex patterns (templates) for strong play
        // These represent common motifs in strong Hex play
        private val HEX_PATTERNS =
                listOf(
                        // Bridge patterns (two stones with one empty space between)
                        HexPattern(
                                layout = arrayOf(arrayOf(PLAYER_TWO, EMPTY, PLAYER_TWO)),
                                centerX = 1,
                                centerY = 0,
                                value = 2.0,
                                name = "Horizontal Bridge"
                        ),
                        HexPattern(
                                layout =
                                        arrayOf(
                                                arrayOf(PLAYER_TWO),
                                                arrayOf(EMPTY),
                                                arrayOf(PLAYER_TWO)
                                        ),
                                centerX = 0,
                                centerY = 1,
                                value = 2.0,
                                name = "Vertical Bridge"
                        ),
                        HexPattern(
                                layout =
                                        arrayOf(
                                                arrayOf(PLAYER_TWO, EMPTY),
                                                arrayOf(EMPTY, PLAYER_TWO)
                                        ),
                                centerX = 1,
                                centerY = 0,
                                value = 2.0,
                                name = "Diagonal Bridge"
                        ),
                        // Ladder breaker pattern
                        HexPattern(
                                layout =
                                        arrayOf(
                                                arrayOf(PLAYER_TWO, EMPTY, PLAYER_ONE),
                                                arrayOf(EMPTY, PLAYER_TWO, EMPTY),
                                                arrayOf(EMPTY, EMPTY, EMPTY)
                                        ),
                                centerX = 1,
                                centerY = 1,
                                value = 2.5,
                                name = "Ladder Breaker"
                        ),
                        // Edge template - strong edge play
                        HexPattern(
                                layout =
                                        arrayOf(
                                                arrayOf(PLAYER_TWO, EMPTY),
                                                arrayOf(EMPTY, PLAYER_TWO)
                                        ),
                                centerX = 0,
                                centerY = 0,
                                value = 1.8,
                                name = "Edge Template"
                        )
                )
    }

    /** Represents a pattern to match on the Hex board. */
    data class HexPattern(
            val layout: Array<Array<Int>>, // 2D array representing the pattern
            val centerX: Int, // X-coordinate of the focus point
            val centerY: Int, // Y-coordinate of the focus point
            val value: Double, // Strategic value of this pattern
            val name: String // Name for debugging
    ) {
        // Pattern dimensions
        val width = layout[0].size
        val height = layout.size

        // Make equals and hashCode explicitly deal with arrays
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HexPattern

            if (!layout.contentDeepEquals(other.layout)) return false
            if (centerX != other.centerX) return false
            if (centerY != other.centerY) return false
            if (value != other.value) return false
            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            var result = layout.contentDeepHashCode()
            result = 31 * result + centerX
            result = 31 * result + centerY
            result = 31 * result + value.hashCode()
            result = 31 * result + name.hashCode()
            return result
        }
    }

    /** Represents a node in the Monte Carlo search tree. */
    private class MCTSNode(
            val parent: MCTSNode?,
            val move: Pair<Int, Int>?,
            val playerJustMoved: Int
    ) {
        val children = mutableListOf<MCTSNode>()
        var visits = 0
        var wins = 0.0
        val unexploredMoves = mutableListOf<Pair<Int, Int>>()

        fun uct(parentVisits: Int): Double {
            if (visits == 0) return Double.MAX_VALUE

            val exploitation = wins / visits
            val exploration = UCT_CONSTANT * sqrt(ln(parentVisits.toDouble()) / visits)

            return exploitation + exploration
        }

        fun addChild(move: Pair<Int, Int>, playerJustMoved: Int): MCTSNode {
            val child = MCTSNode(this, move, playerJustMoved)
            children.add(child)
            unexploredMoves.remove(move)
            return child
        }

        fun bestChild(): MCTSNode? {
            if (children.isEmpty()) return null

            return children.maxByOrNull { it.visits }
        }
    }

    /** Get all neighbor positions for a given cell. */
    private fun getNeighbors(row: Int, col: Int, boardSize: Int): List<Pair<Int, Int>> {
        // In a hexagonal grid, each cell has 6 neighbors
        val neighbors = mutableListOf<Pair<Int, Int>>()

        // The six possible directions in a hex grid
        val directions =
                arrayOf(
                        Pair(-1, 0), // Top-left
                        Pair(-1, 1), // Top-right
                        Pair(0, -1), // Left
                        Pair(0, 1), // Right
                        Pair(1, -1), // Bottom-left
                        Pair(1, 0) // Bottom-right
                )

        for ((dr, dc) in directions) {
            val newRow = row + dr
            val newCol = col + dc

            // Check if the neighbor is within bounds
            if (newRow in 0 until boardSize && newCol in 0 until boardSize) {
                neighbors.add(Pair(newRow, newCol))
            }
        }

        return neighbors
    }

    /**
     * Respond to the first move by the opponent. In Hex, there are strategic responses to the
     * opening move.
     */
    private fun findSecondMove(gameState: GameState): Pair<Int, Int> {
        val board = gameState.board
        val boardSize = gameState.boardSize
        val center = boardSize / 2

        // Find the opponent's first move
        var firstMoveRow = -1
        var firstMoveCol = -1

        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] == PLAYER_ONE) {
                    firstMoveRow = row
                    firstMoveCol = col
                    break
                }
            }
            if (firstMoveRow != -1) break
        }

        // If opponent played in the center or adjacent to center,
        // use the "pie rule" concept and play on the other side
        if ((firstMoveRow == center && firstMoveCol == center) ||
                        (Math.abs(firstMoveRow - center) <= 1 &&
                                Math.abs(firstMoveCol - center) <= 1)
        ) {

            // Play on the opposite side
            val destRow = boardSize - 1 - firstMoveRow
            val destCol = boardSize - 1 - firstMoveCol

            return Pair(destRow, destCol)
        }

        // For other opening moves, play in the center
        if (board[center][center] == EMPTY) {
            return Pair(center, center)
        }

        // If center is taken, play adjacent to center
        val centerNeighbors = getNeighbors(center, center, boardSize)
        val emptyNeighbors = centerNeighbors.filter { (r, c) -> board[r][c] == EMPTY }

        return if (emptyNeighbors.isNotEmpty()) {
            emptyNeighbors.random()
        } else {
            // Fallback: find any empty position
            for (row in 0 until boardSize) {
                for (col in 0 until boardSize) {
                    if (board[row][col] == EMPTY) {
                        return Pair(row, col)
                    }
                }
            }
            // This should never happen if the board isn't full
            Pair(0, 0)
        }
    }

    /** Check if the board is empty. */
    private fun isBoardEmpty(gameState: GameState): Boolean {
        val board = gameState.board
        val boardSize = gameState.boardSize

        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] != EMPTY) {
                    return false
                }
            }
        }

        return true
    }

    /** Count the total number of stones on the board. */
    private fun countStones(gameState: GameState): Int {
        val board = gameState.board
        val boardSize = gameState.boardSize
        var count = 0

        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] != EMPTY) {
                    count++
                }
            }
        }

        return count
    }

    /** Create a deep copy of a 2D board array. */
    private fun copyBoard(board: Array<IntArray>): Array<IntArray> {
        return Array(board.size) { row -> IntArray(board[row].size) { col -> board[row][col] } }
    }

    /** Restore a board to match the original. */
    private fun restoreBoard(board: Array<IntArray>, original: Array<IntArray>) {
        for (row in board.indices) {
            for (col in board[row].indices) {
                board[row][col] = original[row][col]
            }
        }
    }

    /**
     * Find the best move for the AI player (PLAYER_TWO) in the current game state.
     *
     * @param gameState Current state of the game
     * @return Coordinates of the best move
     */
    fun findBestMove(gameState: GameState): Pair<Int, Int>? {
        // Start with basic checks before running expensive MCTS

        // 1. If the board is empty, play in the center
        if (isBoardEmpty(gameState)) {
            val center = gameState.boardSize / 2
            return Pair(center, center)
        }

        // 2. If this is the second move, use a strong response to the opponent's opening
        if (countStones(gameState) == 1) {
            return findSecondMove(gameState)
        }

        // 3. Check for immediate winning move
        val immediateWin = findImmediateWin(gameState, PLAYER_TWO)
        if (immediateWin != null) {
            return immediateWin
        }

        // 4. Check for opponent's immediate winning move to block
        val opponentWin = findImmediateWin(gameState, PLAYER_ONE)
        if (opponentWin != null) {
            return opponentWin
        }

        // 5. Look for virtual connections to complete
        val virtualConnection = findVirtualConnectionMove(gameState, PLAYER_TWO)
        if (virtualConnection != null) {
            return virtualConnection
        }

        // 6. Run Monte Carlo Tree Search for deep analysis
        return runMonteCarloTreeSearch(gameState)
    }

    /** Run Monte Carlo Tree Search to find the best move. */
    private fun runMonteCarloTreeSearch(gameState: GameState): Pair<Int, Int> {
        // Clone the game state to avoid modifying the original
        val board = copyBoard(gameState.board)
        val boardSize = gameState.boardSize

        // Create the root node
        val rootNode =
                MCTSNode(null, null, PLAYER_ONE) // Player ONE just moved, now it's TWO's turn

        // Initialize the unexplored moves
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] == EMPTY) {
                    rootNode.unexploredMoves.add(Pair(row, col))
                }
            }
        }

        // Shuffle to ensure variety in the beginning
        rootNode.unexploredMoves.shuffle(random)

        // Run simulations
        for (i in 0 until SIMULATION_COUNT) {
            // 1. Selection - navigate through the tree using UCT
            val selectedNode = selection(rootNode, board, boardSize)

            // 2. Expansion - add a new node to the tree
            val expandedNode =
                    if (selectedNode.unexploredMoves.isNotEmpty()) {
                        expansion(selectedNode, board, boardSize)
                    } else {
                        selectedNode
                    }

            // 3. Simulation - play a random game from the new node
            val winner = simulation(board, boardSize, expandedNode.playerJustMoved)

            // 4. Backpropagation - update stats up the tree
            backpropagation(expandedNode, winner)

            // Reset the board to its original state for the next simulation
            restoreBoard(board, gameState.board)
        }

        // Choose the best move based on most visited
        val bestChild = rootNode.bestChild()

        // If we somehow don't have a best move, fall back to the first available
        if (bestChild == null || bestChild.move == null) {
            for (row in 0 until boardSize) {
                for (col in 0 until boardSize) {
                    if (gameState.board[row][col] == EMPTY) {
                        return Pair(row, col)
                    }
                }
            }
            // Should never reach here if the board isn't full
            throw IllegalStateException("No valid move found on non-full board")
        }

        return bestChild.move
    }

    /** Selection phase of MCTS - traverse the tree based on UCT values. */
    private fun selection(node: MCTSNode, board: Array<IntArray>, boardSize: Int): MCTSNode {
        var current = node

        // Traverse down the tree until we reach a node that has unexplored moves
        // or a leaf node (no children)
        while (current.unexploredMoves.isEmpty() && current.children.isNotEmpty()) {
            // Select child with highest UCT value
            val nextPlayer = 3 - current.playerJustMoved // Toggle between 1 and 2
            val bestChild = current.children.maxByOrNull { it.uct(current.visits) }!!

            // Update the board with the move to this child
            val move = bestChild.move!!
            board[move.first][move.second] = nextPlayer

            current = bestChild
        }

        return current
    }

    /** Expansion phase of MCTS - add a new node to the tree. */
    private fun expansion(node: MCTSNode, board: Array<IntArray>, boardSize: Int): MCTSNode {
        // Choose a random unexplored move
        val moveIndex = random.nextInt(node.unexploredMoves.size)
        val move = node.unexploredMoves[moveIndex]

        // Determine the player for this move
        val nextPlayer = 3 - node.playerJustMoved // Toggle between 1 and 2

        // Update the board
        board[move.first][move.second] = nextPlayer

        // Add the new child node and return it
        return node.addChild(move, nextPlayer)
    }

    /** Simulation phase of MCTS - play a random game from the current state. */
    private fun simulation(board: Array<IntArray>, boardSize: Int, playerJustMoved: Int): Int {
        // Clone the board to avoid modifying the original during simulation
        val simBoard = copyBoard(board)
        var currentPlayer = 3 - playerJustMoved // Toggle between 1 and 2

        // Get available moves
        val availableMoves = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (simBoard[row][col] == EMPTY) {
                    availableMoves.add(Pair(row, col))
                }
            }
        }

        // Play random moves until the game is decided
        while (availableMoves.isNotEmpty()) {
            // Choose a move randomly, but with some basic strategy by using move scoring
            val scoredMoves =
                    availableMoves.map { move ->
                        val score = scoreMove(simBoard, move, currentPlayer, boardSize)
                        Pair(move, score)
                    }

            // Use probability distribution based on scores to choose a move
            val totalScore = scoredMoves.sumOf { it.second } + 0.001 // Avoid div by 0
            var moveIndex = 0
            var randomValue = random.nextDouble() * totalScore

            // Select a move based on weighted probability
            for (i in scoredMoves.indices) {
                randomValue -= scoredMoves[i].second
                if (randomValue <= 0.0) {
                    moveIndex = i
                    break
                }
            }

            val selectedMove = scoredMoves[moveIndex].first

            // Make the move
            simBoard[selectedMove.first][selectedMove.second] = currentPlayer
            availableMoves.remove(selectedMove)

            // Check if this player won
            if (checkHexWin(simBoard, boardSize, currentPlayer)) {
                return currentPlayer // This player won
            }

            // Switch players
            currentPlayer = 3 - currentPlayer
        }

        // If we reach here without a winner, the game is a draw (should not happen in Hex)
        // In Hex, there is always a winner. But for safety, we return EMPTY as a draw
        return EMPTY
    }

    /** Backpropagation phase of MCTS - update statistics up the tree. */
    private fun backpropagation(node: MCTSNode, winner: Int) {
        var current: MCTSNode? = node

        while (current != null) {
            current.visits++

            // Update wins based on the result
            if (winner != EMPTY) {
                // In Hex, the AI is always PLAYER_TWO (Blue, connecting left-right)
                if (current.playerJustMoved == winner) {
                    current.wins += 1.0
                } else if (current.playerJustMoved == 3 - winner) {
                    // The opponent of the winner
                    current.wins += 0.0
                } else {
                    // Neutral result (should not happen in Hex)
                    current.wins += 0.5
                }
            } else {
                // Draw (should not happen in Hex)
                current.wins += 0.5
            }

            current = current.parent
        }
    }

    /** Score a potential move for the heuristic-guided random simulation. */
    private fun scoreMove(
            board: Array<IntArray>,
            move: Pair<Int, Int>,
            player: Int,
            boardSize: Int
    ): Double {
        var score = 1.0 // Base score

        val (row, col) = move

        // Factor 1: Position value based on pre-calculated strategic positions
        val normalizedRow = (row * 10) / (boardSize - 1)
        val normalizedCol = (col * 10) / (boardSize - 1)
        val posValue = POSITION_VALUES[Pair(normalizedRow, normalizedCol)] ?: 1.0
        score *= posValue

        // Factor 2: Check neighbor cells - prefer to play near friendly stones
        val neighbors = getNeighbors(row, col, boardSize)
        var friendlyNeighbors = 0
        var enemyNeighbors = 0

        for ((nRow, nCol) in neighbors) {
            when (board[nRow][nCol]) {
                player -> friendlyNeighbors++
                3 - player -> enemyNeighbors++
            }
        }

        // Increase score for moves that connect to friendly stones
        score *= (1.0 + 0.3 * friendlyNeighbors)

        // Factor 3: Pattern matching
        val patternScore = evaluatePatterns(board, move, player, boardSize)
        score += patternScore

        // Factor 4: Edge connectivity - playing on edge is valuable in Hex
        if (row == 0 || row == boardSize - 1 || col == 0 || col == boardSize - 1) {
            score *= 1.5

            // For the AI (PLAYER_TWO), left and right edges are most valuable
            if (player == PLAYER_TWO && (col == 0 || col == boardSize - 1)) {
                score *= 1.5
            }
            // For PLAYER_ONE, top and bottom edges are most valuable
            else if (player == PLAYER_ONE && (row == 0 || row == boardSize - 1)) {
                score *= 1.5
            }
        }

        return score
    }

    /** Evaluate pattern-based strategies for a move. */
    private fun evaluatePatterns(
            board: Array<IntArray>,
            move: Pair<Int, Int>,
            player: Int,
            boardSize: Int
    ): Double {
        var patternScore = 0.0
        val (row, col) = move

        // Temporarily place the stone
        board[row][col] = player

        // Rotate patterns for player 1 (vertically oriented) if needed
        val patterns =
                HEX_PATTERNS.map { pattern ->
                    if (player == PLAYER_ONE) {
                        // Rotate the pattern for PLAYER_ONE by transposing the layout
                        val rotatedLayout = Array(pattern.width) { Array(pattern.height) { 0 } }

                        for (i in 0 until pattern.height) {
                            for (j in 0 until pattern.width) {
                                rotatedLayout[j][i] = pattern.layout[i][j]

                                // Also swap player values (1 and 2) in the pattern
                                if (rotatedLayout[j][i] == PLAYER_ONE)
                                        rotatedLayout[j][i] = PLAYER_TWO
                                else if (rotatedLayout[j][i] == PLAYER_TWO)
                                        rotatedLayout[j][i] = PLAYER_ONE
                            }
                        }

                        HexPattern(
                                layout = rotatedLayout,
                                centerX = pattern.centerY, // Swap center coordinates too
                                centerY = pattern.centerX,
                                value = pattern.value,
                                name = pattern.name + " (Rotated)"
                        )
                    } else {
                        pattern
                    }
                }

        // Check each pattern
        for (pattern in patterns) {
            val patternWidth = pattern.width
            val patternHeight = pattern.height

            // Calculate the starting position for pattern matching
            val startRow = row - pattern.centerY
            val startCol = col - pattern.centerX

            // Skip if the pattern would go out of bounds
            if (startRow < 0 ||
                            startRow + patternHeight > boardSize ||
                            startCol < 0 ||
                            startCol + patternWidth > boardSize
            ) {
                continue
            }

            // Check if the pattern matches
            var matches = true
            for (i in 0 until patternHeight) {
                for (j in 0 until patternWidth) {
                    val patternCell = pattern.layout[i][j]
                    val boardCell = board[startRow + i][startCol + j]

                    // Skip empty cells in the pattern (they can be anything)
                    if (patternCell != EMPTY && patternCell != boardCell) {
                        matches = false
                        break
                    }
                }
                if (!matches) break
            }

            if (matches) {
                patternScore += pattern.value
            }
        }

        // Remove the temporary stone
        board[row][col] = EMPTY

        return patternScore
    }

    /** Find a move that potentially completes a virtual connection. */
    private fun findVirtualConnectionMove(gameState: GameState, player: Int): Pair<Int, Int>? {
        val board = gameState.board
        val boardSize = gameState.boardSize

        // We'll use a different approach based on the player
        return if (player == PLAYER_TWO) { // Blue, left-right
            // Check for potential left-right connections
            findHorizontalConnection(board, boardSize, player)
        } else { // Red, top-bottom
            // Check for potential top-bottom connections
            findVerticalConnection(board, boardSize, player)
        }
    }

    /** Find a move that could establish a horizontal connection (left-right). */
    private fun findHorizontalConnection(
            board: Array<IntArray>,
            boardSize: Int,
            player: Int
    ): Pair<Int, Int>? {
        // Virtual connection analysis for left-right
        // Start by finding all stones on the left edge
        val leftStones = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until boardSize) {
            if (board[row][0] == player) {
                leftStones.add(Pair(row, 0))
            }
        }

        if (leftStones.isEmpty()) {
            // If no stones on left edge, try to place one
            for (row in 0 until boardSize) {
                if (board[row][0] == EMPTY) {
                    return Pair(row, 0)
                }
            }
        }

        // Stones on right edge
        val rightStones = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until boardSize) {
            if (board[row][boardSize - 1] == player) {
                rightStones.add(Pair(row, boardSize - 1))
            }
        }

        if (rightStones.isEmpty()) {
            // If no stones on right edge, try to place one
            for (row in 0 until boardSize) {
                if (board[row][boardSize - 1] == EMPTY) {
                    return Pair(row, boardSize - 1)
                }
            }
        }

        // Find the shortest path between left and right stones
        val candidateMoves = mutableListOf<Pair<Int, Int>>()

        // Try to connect each left stone to a right stone
        for (leftStone in leftStones) {
            for (rightStone in rightStones) {
                // Use a pathfinding approach to find good connection moves
                val path = findConnectionPath(board, boardSize, leftStone, rightStone, player)

                // Choose an empty spot along the path to place a stone
                for ((r, c) in path) {
                    if (board[r][c] == EMPTY) {
                        candidateMoves.add(Pair(r, c))
                    }
                }
            }
        }

        if (candidateMoves.isNotEmpty()) {
            return candidateMoves.random()
        }

        // If we can't find a good connection move, try to place a stone that extends
        // from existing friendly stones toward the other side
        for (row in 0 until boardSize) {
            for (col in 1 until boardSize - 1) { // Skip edges, we checked them already
                if (board[row][col] == EMPTY) {
                    // Check if this position is adjacent to a friendly stone
                    val neighbors = getNeighbors(row, col, boardSize)
                    if (neighbors.any { (nRow, nCol) -> board[nRow][nCol] == player }) {
                        // Prioritize moves that are closer to the other side
                        candidateMoves.add(Pair(row, col))
                    }
                }
            }
        }

        return if (candidateMoves.isNotEmpty()) {
            // Sort by progression toward the goal
            val sortedMoves =
                    candidateMoves.sortedByDescending { (_, col) ->
                        col // Higher column value means closer to right side
                    }
            sortedMoves.first()
        } else {
            null
        }
    }

    /** Check if there's a direct win by connecting edges. */
    private fun findImmediateWin(gameState: GameState, player: Int): Pair<Int, Int>? {
        val board = gameState.board
        val boardSize = gameState.boardSize

        // For each empty cell, check if placing a stone creates a win
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] == EMPTY) {
                    // Place a temporary stone
                    board[row][col] = player

                    // Check if this creates a win
                    if (checkHexWin(board, boardSize, player)) {
                        // Remove the stone and return this winning move
                        board[row][col] = EMPTY
                        return Pair(row, col)
                    }

                    // Remove the temporary stone
                    board[row][col] = EMPTY
                }
            }
        }

        return null
    }

    /** Check for a win in Hex (connecting opposite edges). */
    private fun checkHexWin(board: Array<IntArray>, boardSize: Int, player: Int): Boolean {
        // Create a visited array to track our search
        val visited = Array(boardSize) { BooleanArray(boardSize) }

        if (player == PLAYER_ONE) { // Red connects top to bottom
            // Check all pieces in the top row
            for (col in 0 until boardSize) {
                if (board[0][col] == player) {
                    // Reset visited array
                    for (r in 0 until boardSize) {
                        for (c in 0 until boardSize) {
                            visited[r][c] = false
                        }
                    }

                    // Check if this top piece connects to the bottom
                    if (isConnectedToBottom(board, boardSize, 0, col, player, visited)) {
                        return true
                    }
                }
            }
        } else { // player == PLAYER_TWO, Blue connects left to right
            // Check all pieces in the leftmost column
            for (row in 0 until boardSize) {
                if (board[row][0] == player) {
                    // Reset visited array
                    for (r in 0 until boardSize) {
                        for (c in 0 until boardSize) {
                            visited[r][c] = false
                        }
                    }

                    // Check if this left piece connects to the right
                    if (isConnectedToRight(board, boardSize, row, 0, player, visited)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /** DFS to check if there's a path from the current cell to the bottom edge. */
    private fun isConnectedToBottom(
            board: Array<IntArray>,
            boardSize: Int,
            row: Int,
            col: Int,
            player: Int,
            visited: Array<BooleanArray>
    ): Boolean {
        // If we reached the bottom row, we found a path
        if (row == boardSize - 1) {
            return true
        }

        // Mark current cell as visited
        visited[row][col] = true

        // Check all six neighbors
        for ((nRow, nCol) in getNeighbors(row, col, boardSize)) {
            // If the neighbor is valid, not visited, and belongs to the player
            if (nRow in 0 until boardSize &&
                            nCol in 0 until boardSize &&
                            !visited[nRow][nCol] &&
                            board[nRow][nCol] == player
            ) {
                // Recursively check if this neighbor leads to the bottom
                if (isConnectedToBottom(board, boardSize, nRow, nCol, player, visited)) {
                    return true
                }
            }
        }

        return false
    }

    /** DFS to check if there's a path from the current cell to the right edge. */
    private fun isConnectedToRight(
            board: Array<IntArray>,
            boardSize: Int,
            row: Int,
            col: Int,
            player: Int,
            visited: Array<BooleanArray>
    ): Boolean {
        // If we reached the rightmost column, we found a path
        if (col == boardSize - 1) {
            return true
        }

        // Mark current cell as visited
        visited[row][col] = true

        // Check all six neighbors
        for ((nRow, nCol) in getNeighbors(row, col, boardSize)) {
            // If the neighbor is valid, not visited, and belongs to the player
            if (nRow in 0 until boardSize &&
                            nCol in 0 until boardSize &&
                            !visited[nRow][nCol] &&
                            board[nRow][nCol] == player
            ) {
                // Recursively check if this neighbor leads to the right
                if (isConnectedToRight(board, boardSize, nRow, nCol, player, visited)) {
                    return true
                }
            }
        }

        return false
    }

    /** Find a move that could establish a vertical connection (top-bottom). */
    private fun findVerticalConnection(
            board: Array<IntArray>,
            boardSize: Int,
            player: Int
    ): Pair<Int, Int>? {
        // Virtual connection analysis for top-bottom
        // Start by finding all stones on the top edge
        val topStones = mutableListOf<Pair<Int, Int>>()
        for (col in 0 until boardSize) {
            if (board[0][col] == player) {
                topStones.add(Pair(0, col))
            }
        }

        if (topStones.isEmpty()) {
            // If no stones on top edge, try to place one
            for (col in 0 until boardSize) {
                if (board[0][col] == EMPTY) {
                    return Pair(0, col)
                }
            }
        }

        // Stones on bottom edge
        val bottomStones = mutableListOf<Pair<Int, Int>>()
        for (col in 0 until boardSize) {
            if (board[boardSize - 1][col] == player) {
                bottomStones.add(Pair(boardSize - 1, col))
            }
        }

        if (bottomStones.isEmpty()) {
            // If no stones on bottom edge, try to place one
            for (col in 0 until boardSize) {
                if (board[boardSize - 1][col] == EMPTY) {
                    return Pair(boardSize - 1, col)
                }
            }
        }

        // Find the shortest path between top and bottom stones
        val candidateMoves = mutableListOf<Pair<Int, Int>>()

        // Try to connect each top stone to a bottom stone
        for (topStone in topStones) {
            for (bottomStone in bottomStones) {
                // Use a pathfinding approach to find good connection moves
                val path = findConnectionPath(board, boardSize, topStone, bottomStone, player)

                // Choose an empty spot along the path to place a stone
                for ((r, c) in path) {
                    if (board[r][c] == EMPTY) {
                        candidateMoves.add(Pair(r, c))
                    }
                }
            }
        }

        if (candidateMoves.isNotEmpty()) {
            return candidateMoves.random()
        }

        // If we can't find a good connection move, try to place a stone that extends
        // from existing friendly stones toward the other side
        for (col in 0 until boardSize) {
            for (row in 1 until boardSize - 1) { // Skip edges, we checked them already
                if (board[row][col] == EMPTY) {
                    // Check if this position is adjacent to a friendly stone
                    val neighbors = getNeighbors(row, col, boardSize)
                    if (neighbors.any { (nRow, nCol) -> board[nRow][nCol] == player }) {
                        // Prioritize moves that are closer to the other side
                        candidateMoves.add(Pair(row, col))
                    }
                }
            }
        }

        return if (candidateMoves.isNotEmpty()) {
            // Sort by progression toward the goal
            val sortedMoves =
                    candidateMoves.sortedByDescending { (row, _) ->
                        row // Higher row value means closer to bottom side
                    }
            sortedMoves.first()
        } else {
            null
        }
    }

    /** Find a potential connection path between two stones. */
    private fun findConnectionPath(
            board: Array<IntArray>,
            boardSize: Int,
            start: Pair<Int, Int>,
            end: Pair<Int, Int>,
            player: Int
    ): List<Pair<Int, Int>> {
        // Use Dijkstra's algorithm to find the shortest path

        // Initialize distance map with infinity for all cells
        val distance = Array(boardSize) { Array(boardSize) { Double.POSITIVE_INFINITY } }
        val visited = Array(boardSize) { BooleanArray(boardSize) }

        // Start position has distance 0
        distance[start.first][start.second] = 0.0

        // Compare by distance
        val pq = PriorityQueue<Triple<Int, Int, Double>>(compareBy { it.third })
        pq.add(Triple(start.first, start.second, 0.0))

        // Parent map for reconstructing the path
        val parent = Array(boardSize) { Array<Pair<Int, Int>?>(boardSize) { null } }

        while (pq.isNotEmpty()) {
            val (row, col, dist) = pq.poll()

            // Skip if already visited
            if (visited[row][col]) continue

            // Mark as visited
            visited[row][col] = true

            // Check if we've reached the destination
            if (row == end.first && col == end.second) {
                break
            }

            // Check all neighbors
            for (neighbor in getNeighbors(row, col, boardSize)) {
                val (nRow, nCol) = neighbor

                // Calculate the cost to move to this neighbor
                // Empty cells or cells with our stones have lower cost
                val cost =
                        when (board[nRow][nCol]) {
                            EMPTY -> 1.0
                            player -> 0.5 // Our stone - very low cost
                            else -> 10.0 // Opponent stone - very high cost
                        }

                // Relax the edge
                val newDist = dist + cost
                if (newDist < distance[nRow][nCol]) {
                    distance[nRow][nCol] = newDist
                    parent[nRow][nCol] = Pair(row, col)
                    pq.add(Triple(nRow, nCol, newDist))
                }
            }
        }

        // Reconstruct the path
        val path = mutableListOf<Pair<Int, Int>>()
        var current: Pair<Int, Int>? = end

        while (current != null &&
                !(current.first == start.first && current.second == start.second)) {
            path.add(current)
            current = parent[current.first][current.second]
        }

        // Reverse the path to get from start to end
        return path.reversed()
    }
}
