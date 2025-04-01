package com.iacobo.wuziqi.ai

import com.iacobo.wuziqi.data.GameState
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import java.util.PriorityQueue
import java.util.Random

/**
 * A strong Hex AI player using Alpha-Beta pruning with flow-based heuristics.
 * Implements the GameAI interface for consistent interaction.
 */
class HexAlphaBetaEngine(private val random: Random = Random()) : GameAI {

    companion object {
        // Board values for easy reference
        const val EMPTY = GameState.EMPTY
        const val PLAYER_ONE = GameState.PLAYER_ONE // Red, connects top-bottom
        const val PLAYER_TWO = GameState.PLAYER_TWO // Blue, connects left-right
        
        // Depth for alpha-beta search
        private const val MAX_DEPTH = 3
        
        // Pattern search pruning patterns - cells around existing pieces to consider
        private val PRUNE_PATTERN = arrayOf(
            Pair(1, -2), Pair(-1, -1), Pair(0, -1), Pair(1, -1), Pair(2, -1),
            Pair(-1, 0), Pair(0, 0), Pair(1, 0),
            Pair(-2, 1), Pair(-1, 1), Pair(0, 1), Pair(1, 1), Pair(-1, 2)
        )
        
        // Bridge patterns - important connection patterns in Hex
        private val BRIDGE_PATTERNS = arrayOf(
            // Simple bridges (essential connections)
            HexPattern(
                size = Pair(2, 2),
                pattern = intArrayOf(1, 0, EMPTY, 1),
                interest = Pair(0, 1)
            ),
            HexPattern(
                size = Pair(2, 2),
                pattern = intArrayOf(1, EMPTY, 0, 1),
                interest = Pair(1, 0)
            ),
            // Extended bridges
            HexPattern(
                size = Pair(2, 3),
                pattern = intArrayOf(2, 1, 0, EMPTY, 1, 2),
                interest = Pair(1, 1)
            ),
            HexPattern(
                size = Pair(2, 3),
                pattern = intArrayOf(2, 1, EMPTY, 0, 1, 2),
                interest = Pair(0, 1)
            ),
            HexPattern(
                size = Pair(3, 2),
                pattern = intArrayOf(2, EMPTY, 1, 1, 0, 2),
                interest = Pair(1, 0)
            ),
            HexPattern(
                size = Pair(3, 2),
                pattern = intArrayOf(2, 0, 1, 1, EMPTY, 2),
                interest = Pair(1, 1)
            )
        )
        
        // Dead cell patterns - cells that are not useful to play in
        private val DEAD_CELL_PATTERNS = arrayOf(
            HexPattern(
                size = Pair(3, 2),
                pattern = intArrayOf(2, 1, 1, 1, EMPTY, 1),
                interest = Pair(1, 1)
            ),
            HexPattern(
                size = Pair(3, 2),
                pattern = intArrayOf(1, EMPTY, 1, 2, 1, 1),
                interest = Pair(1, 0)
            ),
            HexPattern(
                size = Pair(3, 3),
                pattern = intArrayOf(2, 1, 1, 2, EMPTY, 1, 0, 2, 2),
                interest = Pair(1, 1)
            ),
            HexPattern(
                size = Pair(3, 3),
                pattern = intArrayOf(0, 2, 2, 2, EMPTY, 1, 2, 1, 1),
                interest = Pair(1, 1)
            ),
            HexPattern(
                size = Pair(3, 3),
                pattern = intArrayOf(2, 2, 1, 0, EMPTY, 1, 0, 2, 2),
                interest = Pair(1, 1)
            ),
            HexPattern(
                size = Pair(3, 3),
                pattern = intArrayOf(0, 2, 2, 0, EMPTY, 1, 2, 2, 1),
                interest = Pair(1, 1)
            )
        )
    }
    
    /**
     * Represents a Hex pattern for pattern matching.
     */
    private data class HexPattern(
        val size: Pair<Int, Int>,     // Size of the pattern (width, height)
        val pattern: IntArray,       // The pattern itself (1=own stone, 0=opponent, 2=any, EMPTY=empty)
        val interest: Pair<Int, Int>  // The cell of interest within the pattern
    ) {
        // Make equals and hashCode properly handle IntArray
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as HexPattern
            
            if (size != other.size) return false
            if (!pattern.contentEquals(other.pattern)) return false
            if (interest != other.interest) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = size.hashCode()
            result = 31 * result + pattern.contentHashCode()
            result = 31 * result + interest.hashCode()
            return result
        }
    }
    
    // Statistics tracking
    private var nodesEvaluated = 0
    
    /**
     * Find the best move for the AI player.
     * Implements the GameAI interface method.
     */
    override fun findBestMove(gameState: GameState): Pair<Int, Int>? {
        nodesEvaluated = 0
        
        // Handle special cases first
        
        // 1. If the board is empty, play in the center
        if (isBoardEmpty(gameState)) {
            val center = gameState.boardSize / 2
            return Pair(center, center)
        }
        
        // 2. If this is the second move, use a strategic response
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
        
        // Run alpha-beta search
        val result = alphaBeta(gameState, MAX_DEPTH, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true)
        
        // If we couldn't find a good move, fall back to a random valid move
        if (result.second == null) {
            val validMoves = findValidMoves(gameState)
            if (validMoves.isNotEmpty()) {
                return validMoves[random.nextInt(validMoves.size)]
            }
        }
        
        return result.second
    }
    
    /**
     * Alpha-Beta search algorithm for Hex.
     * 
     * @param gameState Current game state
     * @param depth Remaining search depth
     * @param alpha Alpha value for pruning
     * @param beta Beta value for pruning
     * @param isMax True if maximizing player's turn, false otherwise
     * @return Pair of (score, move) - score is the heuristic value, move is the best move found
     */
    private fun alphaBeta(
        gameState: GameState,
        depth: Int,
        alpha: Double,
        beta: Double,
        isMax: Boolean
    ): Pair<Double, Pair<Int, Int>?> {
        nodesEvaluated++
        
        // Terminal conditions: depth reached or game over
        if (depth == 0 || isGameOver(gameState)) {
            return Pair(evaluate(gameState, depth), null)
        }
        
        // Get interesting moves from pattern search
        val patterns = getPatternMoves(gameState)
        
        // Identify dead cells (not worth playing)
        val deadCells = findDeadCells(gameState, PLAYER_TWO)
        
        // Remove dead cells from consideration
        val playablePatterns = patterns.filter { pos -> !deadCells.contains(pos) }
        
        // If no patterns found, consider all valid moves
        val moves = if (playablePatterns.isEmpty()) {
            findValidMoves(gameState)
        } else {
            playablePatterns
        }
        
        var bestMove: Pair<Int, Int>? = null
        
        if (isMax) {
            // Maximizing player (AI)
            var currentAlpha = alpha
            
            for (move in moves) {
                // Skip if cell is already occupied
                if (gameState.board[move.first][move.second] != EMPTY) {
                    continue
                }
                
                // Make the move on a copied board
                val newState = copyGameState(gameState)
                newState.board[move.first][move.second] = PLAYER_TWO
                
                // Recurse
                val evaluation = alphaBeta(newState, depth - 1, currentAlpha, beta, false)
                
                // Update alpha and best move
                if (evaluation.first > currentAlpha) {
                    currentAlpha = evaluation.first
                    bestMove = move
                }
                
                // Alpha-beta pruning
                if (currentAlpha >= beta) {
                    break
                }
            }
            
            return Pair(currentAlpha, bestMove)
        } else {
            // Minimizing player (opponent)
            var currentBeta = beta
            
            for (move in moves) {
                // Skip if cell is already occupied
                if (gameState.board[move.first][move.second] != EMPTY) {
                    continue
                }
                
                // Make the move on a copied board
                val newState = copyGameState(gameState)
                newState.board[move.first][move.second] = PLAYER_ONE
                
                // Recurse
                val evaluation = alphaBeta(newState, depth - 1, alpha, currentBeta, true)
                
                // Update beta and best move
                if (evaluation.first < currentBeta) {
                    currentBeta = evaluation.first
                    bestMove = move
                }
                
                // Alpha-beta pruning
                if (currentBeta <= alpha) {
                    break
                }
            }
            
            return Pair(currentBeta, bestMove)
        }
    }
    
    /**
     * Evaluate the current board state using flow-based heuristics.
     * 
     * @param gameState The current game state
     * @param depth The current search depth
     * @return A score for the current position
     */
    private fun evaluate(gameState: GameState, depth: Int): Double {
        // Check if the game is over
        if (isGameOver(gameState)) {
            val winner = getWinner(gameState)
            return if (winner == PLAYER_TWO) {
                // AI wins (good)
                10.0 * (depth + 1) // Higher score for quicker wins
            } else {
                // Opponent wins (bad)
                -10.0 * (depth + 1)
            }
        }
        
        // If game is not over, use flow-based heuristic
        val maxFlow = calculateFlow(gameState, PLAYER_TWO)
        val minFlow = calculateFlow(gameState, PLAYER_ONE)
        
        // Avoid division by zero
        val ratio = if (minFlow > 0) maxFlow / minFlow else maxFlow / 0.01
        
        return ln(ratio)
    }
    
    /**
     * Calculate the flow value for a player (how easily they can connect their sides).
     * Higher flow values indicate better connectivity.
     * 
     * @param gameState The current game state
     * @param player The player to calculate flow for
     * @return A flow value indicating connectivity strength
     */
    private fun calculateFlow(gameState: GameState, player: Int): Double {
        // Create a virtual flow graph
        val graph = createFlowGraph(gameState, player)
        
        // Calculate max flow using a simple DFS approach
        return calculateMaxFlow(graph, player)
    }
    
    /**
     * Create a flow graph representation for connectivity analysis.
     * 
     * @param gameState The current game state
     * @param player The player to analyze
     * @return A graph representation for flow calculation
     */
    private fun createFlowGraph(gameState: GameState, player: Int): Map<Pair<Int, Int>, Set<Pair<Int, Int>>> {
        val board = gameState.board
        val boardSize = gameState.boardSize
        val graph = mutableMapOf<Pair<Int, Int>, MutableSet<Pair<Int, Int>>>()
        
        // Add all empty cells and player stones to the graph
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] == EMPTY || board[row][col] == player) {
                    val cell = Pair(row, col)
                    graph[cell] = mutableSetOf()
                    
                    // Connect to adjacent cells
                    for (neighbor in getNeighbors(row, col, boardSize)) {
                        if (board[neighbor.first][neighbor.second] == EMPTY || 
                            board[neighbor.first][neighbor.second] == player) {
                            graph[cell]?.add(neighbor)
                        }
                    }
                }
            }
        }
        
        // Add virtual source and sink nodes for the player's edges
        val source = Pair(-1, -1)
        val sink = Pair(-2, -2)
        
        graph[source] = mutableSetOf()
        graph[sink] = mutableSetOf()
        
        // Connect source and sink to their respective edges
        if (player == PLAYER_ONE) {
            // Player One connects top to bottom
            for (col in 0 until boardSize) {
                // Connect source to top edge
                if (board[0][col] == EMPTY || board[0][col] == player) {
                    graph[source]?.add(Pair(0, col))
                }
                
                // Connect bottom edge to sink
                if (board[boardSize - 1][col] == EMPTY || board[boardSize - 1][col] == player) {
                    graph[Pair(boardSize - 1, col)]?.add(sink)
                }
            }
        } else {
            // Player Two connects left to right
            for (row in 0 until boardSize) {
                // Connect source to left edge
                if (board[row][0] == EMPTY || board[row][0] == player) {
                    graph[source]?.add(Pair(row, 0))
                }
                
                // Connect right edge to sink
                if (board[row][boardSize - 1] == EMPTY || board[row][boardSize - 1] == player) {
                    graph[Pair(row, boardSize - 1)]?.add(sink)
                }
            }
        }
        
        return graph
    }
    
    /**
     * Calculate the maximum flow through the graph.
     * This is a simplified implementation of a max-flow algorithm.
     * 
     * @param graph The flow graph
     * @param player The player being analyzed
     * @return A flow value
     */
    private fun calculateMaxFlow(graph: Map<Pair<Int, Int>, Set<Pair<Int, Int>>>, player: Int): Double {
        val source = Pair(-1, -1)
        val sink = Pair(-2, -2)
        
        // Simple breadth-first traversal to estimate connectivity
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = mutableListOf(source)
        val distances = mutableMapOf<Pair<Int, Int>, Int>()
        
        distances[source] = 0
        
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            
            if (current !in visited) {
                visited.add(current)
                
                for (neighbor in graph[current] ?: emptySet()) {
                    if (neighbor !in visited) {
                        queue.add(neighbor)
                        distances[neighbor] = (distances[current] ?: 0) + 1
                    }
                }
            }
        }
        
        // If sink is reachable, calculate flow based on distance and connectivity
        if (sink in distances) {
            // Shorter distance means better connectivity
            val pathLength = distances[sink] ?: Int.MAX_VALUE
            
            // Calculate how many different paths connect to the edges
            val connectivity = graph[source]?.size ?: 0
            
            // Better flow means shorter paths and more connectivity
            return connectivity.toDouble() / (pathLength.toDouble() + 1.0)
        }
        
        // If sink is not reachable, return minimal flow
        return 0.01
    }
    
    /**
     * Get pattern-based moves to consider in search.
     * This helps focus the search on moves that matter.
     * 
     * @param gameState The current game state
     * @return A list of positions to consider
     */
    private fun getPatternMoves(gameState: GameState): List<Pair<Int, Int>> {
        val board = gameState.board
        val boardSize = gameState.boardSize
        val moves = mutableSetOf<Pair<Int, Int>>()
        
        // Look for cells around existing pieces
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] != EMPTY) {
                    // Add cells around this piece based on the prune pattern
                    for ((dx, dy) in PRUNE_PATTERN) {
                        val newRow = row + dx
                        val newCol = col + dy
                        if (newRow in 0 until boardSize && newCol in 0 until boardSize &&
                            board[newRow][newCol] == EMPTY) {
                            moves.add(Pair(newRow, newCol))
                        }
                    }
                }
            }
        }
        
        // If no moves found, consider all empty cells
        if (moves.isEmpty()) {
            for (row in 0 until boardSize) {
                for (col in 0 until boardSize) {
                    if (board[row][col] == EMPTY) {
                        moves.add(Pair(row, col))
                    }
                }
            }
        }
        
        return moves.toList()
    }
    
    /**
     * Find bridge patterns on the board.
     * 
     * @param gameState The current game state
     * @param player The player to analyze
     * @return A list of important bridge positions
     */
    private fun findBridges(gameState: GameState, player: Int): List<Pair<Int, Int>> {
        return searchPatterns(gameState, player, BRIDGE_PATTERNS)
    }
    
    /**
     * Find cells that are not worth playing in.
     * 
     * @param gameState The current game state
     * @param player The player to analyze
     * @return A list of positions that are not strategically valuable
     */
    private fun findDeadCells(gameState: GameState, player: Int): List<Pair<Int, Int>> {
        return searchPatterns(gameState, player, DEAD_CELL_PATTERNS)
    }
    
    /**
     * Search for pattern matches on the board.
     * 
     * @param gameState The current game state
     * @param player The player to analyze
     * @param patterns The patterns to search for
     * @return A list of matched positions
     */
    private fun searchPatterns(
        gameState: GameState,
        player: Int,
        patterns: Array<HexPattern>
    ): List<Pair<Int, Int>> {
        val board = gameState.board
        val boardSize = gameState.boardSize
        val matches = mutableListOf<Pair<Int, Int>>()
        
        // Check each pattern across the board
        for (pattern in patterns) {
            for (row in 0 until boardSize - pattern.size.first + 1) {
                for (col in 0 until boardSize - pattern.size.second + 1) {
                    if (matchesPattern(gameState, row, col, pattern, player)) {
                        // Add the position of interest
                        matches.add(Pair(row + pattern.interest.first, col + pattern.interest.second))
                    }
                }
            }
        }
        
        return matches
    }
    
    /**
     * Check if a specific part of the board matches a pattern.
     * 
     * @param gameState The current game state
     * @param startRow Starting row
     * @param startCol Starting column
     * @param pattern The pattern to match
     * @param player The player to analyze
     * @return True if the pattern matches, false otherwise
     */
    private fun matchesPattern(
        gameState: GameState,
        startRow: Int,
        startCol: Int,
        pattern: HexPattern,
        player: Int
    ): Boolean {
        val board = gameState.board
        val opponent = if (player == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        
        var index = 0
        for (y in 0 until pattern.size.second) {
            for (x in 0 until pattern.size.first) {
                val cell = board[startRow + x][startCol + y]
                
                when (pattern.pattern[index]) {
                    2 -> {} // Any cell, matches regardless of content
                    1 -> {
                        // Must be player's stone
                        if (cell != player) return false
                    }
                    0 -> {
                        // Must be opponent's stone
                        if (cell != opponent) return false
                    }
                    EMPTY -> {
                        // Must be empty
                        if (cell != EMPTY) return false
                    }
                }
                index++
            }
        }
        
        return true
    }
    
    /**
     * Find valid moves on the board (empty cells).
     * 
     * @param gameState The current game state
     * @return A list of empty positions
     */
    private fun findValidMoves(gameState: GameState): List<Pair<Int, Int>> {
        val board = gameState.board
        val boardSize = gameState.boardSize
        val moves = mutableListOf<Pair<Int, Int>>()
        
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] == EMPTY) {
                    moves.add(Pair(row, col))
                }
            }
        }
        
        return moves
    }
    
    /**
     * Check if the board is empty.
     * 
     * @param gameState The current game state
     * @return True if the board is empty, false otherwise
     */
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
    
    /**
     * Count the total number of stones on the board.
     * 
     * @param gameState The current game state
     * @return The number of stones
     */
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
    
    /**
     * Check if the game is over.
     * 
     * @param gameState The current game state
     * @return True if the game is over, false otherwise
     */
    private fun isGameOver(gameState: GameState): Boolean {
        // Check if either player has a winning connection
        return checkHexWin(gameState, PLAYER_ONE) || checkHexWin(gameState, PLAYER_TWO)
    }
    
    /**
     * Get the winner of the game.
     * 
     * @param gameState The current game state
     * @return The player who won, or EMPTY if no winner
     */
    private fun getWinner(gameState: GameState): Int {
        if (checkHexWin(gameState, PLAYER_ONE)) return PLAYER_ONE
        if (checkHexWin(gameState, PLAYER_TWO)) return PLAYER_TWO
        return EMPTY
    }
    
    /**
     * Find an immediate winning move for a player.
     * 
     * @param gameState The current game state
     * @param player The player to check for
     * @return A winning move if available, null otherwise
     */
    private fun findImmediateWin(gameState: GameState, player: Int): Pair<Int, Int>? {
        val board = gameState.board
        val boardSize = gameState.boardSize
        
        // Try each empty cell to see if it creates a win
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] == EMPTY) {
                    // Place a temporary stone
                    board[row][col] = player
                    
                    // Check if this creates a win
                    if (checkHexWin(gameState, player)) {
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
    
    /**
     * Strategic response to the opponent's first move.
     * 
     * @param gameState The current game state
     * @return A strategic second move
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
        // use the "pie rule" concept and play on the opposite side
        if ((firstMoveRow == center && firstMoveCol == center) ||
            (Math.abs(firstMoveRow - center) <= 1 && 
             Math.abs(firstMoveCol - center) <= 1)) {
            
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
            emptyNeighbors[random.nextInt(emptyNeighbors.size)]
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
    
    /**
     * Check if a player has won by connecting their edges.
     * 
     * @param gameState The current game state
     * @param player The player to check for
     * @return True if the player has won, false otherwise
     */
    private fun checkHexWin(gameState: GameState, player: Int): Boolean {
        val board = gameState.board
        val boardSize = gameState.boardSize
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
                    if (isConnectedToBottom(gameState, 0, col, player, visited)) {
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
                    if (isConnectedToRight(gameState, row, 0, player, visited)) {
                        return true
                    }
                }
            }
        }
        
        return false
    }
    
    /**
     * Check if there's a connection from the top to the bottom.
     * 
     * @param gameState The current game state
     * @param row Starting row
     * @param col Starting column
     * @param player The player to check for
     * @param visited Visited cells tracker
     * @return True if connected, false otherwise
     */
    private fun isConnectedToBottom(
        gameState: GameState,
        row: Int,
        col: Int,
        player: Int,
        visited: Array<BooleanArray>
    ): Boolean {
        val board = gameState.board
        val boardSize = gameState.boardSize
        
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
                if (isConnectedToBottom(gameState, nRow, nCol, player, visited)) {
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Check if there's a connection from the left to the right.
     * 
     * @param gameState The current game state
     * @param row Starting row
     * @param col Starting column
     * @param player The player to check for
     * @param visited Visited cells tracker
     * @return True if connected, false otherwise
     */
    private fun isConnectedToRight(
        gameState: GameState,
        row: Int,
        col: Int,
        player: Int,
        visited: Array<BooleanArray>
    ): Boolean {
        val board = gameState.board
        val boardSize = gameState.boardSize
        
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
                if (isConnectedToRight(gameState, nRow, nCol, player, visited)) {
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Get all neighbor positions for a given cell in a hex grid.
     * 
     * @param row The row of the cell
     * @param col The column of the cell
     * @param boardSize The size of the board
     * @return A list of valid neighbor positions
     */
    private fun getNeighbors(row: Int, col: Int, boardSize: Int): List<Pair<Int, Int>> {
        // In a hexagonal grid, each cell has 6 neighbors
        val neighbors = mutableListOf<Pair<Int, Int>>()
        
        // The six possible directions in a hex grid
        val directions = arrayOf(
            Pair(-1, 0),  // Top-left
            Pair(-1, 1),  // Top-right
            Pair(0, -1),  // Left
            Pair(0, 1),   // Right
            Pair(1, -1),  // Bottom-left
            Pair(1, 0)    // Bottom-right
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
     * Create a deep copy of a game state.
     * 
     * @param gameState The original game state
     * @return A new copy of the game state
     */
    private fun copyGameState(gameState: GameState): GameState {
        val newState = GameState(
            boardSize = gameState.boardSize,
            winCondition = gameState.winCondition,
            againstComputer = gameState.againstComputer
        )
        
        // Copy the board state
        for (row in 0 until gameState.boardSize) {
            for (col in 0 until gameState.boardSize) {
                newState.board[row][col] = gameState.board[row][col]
            }
        }
        
        // Copy other state properties
        newState.currentPlayer = gameState.currentPlayer
        
        return newState
    }
}