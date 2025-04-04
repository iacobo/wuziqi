package com.iacobo.wuziqi.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Represents the state of a Wuziqi game. Manages the board, current player, and win condition
 * checking.
 */
class GameState // Constructor with custom board size and win condition
(
        var boardSize: Int = DEFAULT_BOARD_SIZE,
        var winCondition: Int = DEFAULT_WIN_CONDITION,
        var againstComputer: Boolean = false
) {
    companion object {
        const val EMPTY = 0
        const val PLAYER_ONE = 1 // Black
        const val PLAYER_TWO = 2 // White
        const val DEFAULT_BOARD_SIZE = 15 // Standard wuziqi board size
        const val DEFAULT_WIN_CONDITION = 5 // Number of consecutive pieces needed to win

        val DIRECTIONS =
                arrayOf(
                        Pair(-1, 0), // Top-left
                        Pair(-1, 1), // Top-right
                        Pair(0, -1), // Left
                        Pair(0, 1), // Right
                        Pair(1, -1), // Bottom-left
                        Pair(1, 0) // Bottom-right
                )

        // Keys for saved state
        private const val PREFS_NAME = "wuziqi_game_state"
        private const val KEY_BOARD = "board"
        private const val KEY_CURRENT_PLAYER = "current_player"
        private const val KEY_BOARD_SIZE = "board_size"
        private const val KEY_WIN_CONDITION = "win_condition"
        private const val KEY_AGAINST_COMPUTER = "against_computer"
        private const val KEY_EASTER_EGGS = "easter_eggs"
    }

    // Changed board to use mutableStateOf for proper observation
    private val _boardState = mutableStateOf(Array(boardSize) { IntArray(boardSize) { EMPTY } })
    private val winningPath = mutableSetOf<Pair<Int, Int>>()

    // Property for access to the board
    var board: Array<IntArray>
        get() = _boardState.value
        private set(value) {
            _boardState.value = value
        }

    private val _currentPlayerState = mutableStateOf(PLAYER_ONE)
    var currentPlayer: Int
        get() = _currentPlayerState.value
        set(value) {
            _currentPlayerState.value = value
        }

    fun getWinningPath(): Set<Pair<Int, Int>> = winningPath

    /**
     * Places a tile on the board and switches the current player. FIXED to create a new array for
     * proper reactivity in Compose.
     *
     * @param row The row position
     * @param col The column position
     * @return True if the tile was placed successfully, false otherwise
     */
    fun placeTile(row: Int, col: Int): Boolean {
        if (!isTileEmpty(row, col)) return false

        // Create a new array to trigger state changes
        val newBoard =
                Array(boardSize) { r ->
                    IntArray(boardSize) { c ->
                        if (r == row && c == col) currentPlayer else board[r][c]
                    }
                }

        // Update the board state with new array
        board = newBoard

        // Switch player
        currentPlayer = if (currentPlayer == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        return true
    }

    /**
     * Checks if a tile is empty.
     *
     * @param row The row position
     * @param col The column position
     * @return True if the tile is empty, false otherwise
     */
    fun isTileEmpty(row: Int, col: Int): Boolean = board[row][col] == EMPTY

    /**
     * Resets the game state to initial values. FIXED to create a new array for proper reactivity.
     */
    fun reset() {
        board = Array(boardSize) { IntArray(boardSize) { EMPTY } }
        currentPlayer = PLAYER_ONE
    }

    /**
     * Checks if the last placed piece created a winning condition.
     *
     * @param row The row of the last placed piece
     * @param col The column of the last placed piece
     * @param playerValue The player value to check for
     * @return True if the player has won, false otherwise
     */
    fun checkWin(row: Int, col: Int, playerValue: Int): Boolean {
        return checkDirection(row, col, 1, 0, playerValue) || // Horizontal
        checkDirection(row, col, 0, 1, playerValue) || // Vertical
                checkDirection(row, col, 1, 1, playerValue) || // Diagonal \
                checkDirection(row, col, 1, -1, playerValue) // Diagonal /
    }

    /** Checks for win condition in a specific direction. */
    private fun checkDirection(
            row: Int,
            col: Int,
            deltaRow: Int,
            deltaCol: Int,
            playerValue: Int
    ): Boolean {
        var count = 1 // Start with 1 for the piece itself

        // Check in the positive direction
        count += countInDirection(row, col, deltaRow, deltaCol, playerValue)

        // Check in the negative direction
        count += countInDirection(row, col, -deltaRow, -deltaCol, playerValue)

        return count >= winCondition
    }

    /** Counts consecutive pieces in a specific direction. */
    private fun countInDirection(
            row: Int,
            col: Int,
            deltaRow: Int,
            deltaCol: Int,
            playerValue: Int
    ): Int {
        var count = 0
        var r = row + deltaRow
        var c = col + deltaCol

        while (r in 0 until boardSize && c in 0 until boardSize && board[r][c] == playerValue) {
            count++
            r += deltaRow
            c += deltaCol
        }

        return count
    }

    /** Checks if the board is full (draw condition) */
    fun isBoardFull(): Boolean {
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] == EMPTY) {
                    return false
                }
            }
        }
        return true
    }

    /** Checks if a position is valid on the board */
    fun isValidPosition(row: Int, col: Int): Boolean {
        val gameType = GameType.fromGameState(this)
        return gameType.isValidHexPosition(row, col)
    }

    /**
     * Special win checker for Hex game. In Hex, a player wins by connecting their two opposite
     * edges. Player One connects top to bottom. Player Two connects left to right.
     *
     * FIXED: Properly check for any path (direct or indirect) between the edges
     *
     * @param playerValue The player value to check for
     * @return True if the player has won, false otherwise
     */
    fun checkHexWin(playerValue: Int): Boolean {
        // Only run hex win check for 11x11 board with 8 win condition
        if (boardSize != 11 || winCondition != 8) {
            return false
        }

        // Create a visited array to track visited cells in our search
        val visited = Array(boardSize) { BooleanArray(boardSize) }

        // Reset winning path before checking
        winningPath.clear()

        // For Player One (1), check connection from top row to bottom row
        if (playerValue == PLAYER_ONE) {
            // Check all pieces in the top row that belong to player 1
            for (col in 0 until boardSize) {
                if (board[0][col] == playerValue) {
                    // Reset visited array for each starting position
                    for (r in 0 until boardSize) {
                        for (c in 0 until boardSize) {
                            visited[r][c] = false
                        }
                    }

                    // Start DFS from this top row piece
                    if (isConnectedToBottom(0, col, playerValue, visited)) {
                        return true
                    }
                }
            }
            return false
        }
        // For Player Two (2), check connection from left column to right column
        else if (playerValue == PLAYER_TWO) {
            // Check all pieces in the leftmost column that belong to player 2
            for (row in 0 until boardSize) {
                if (board[row][0] == playerValue) {
                    // Reset visited array for each starting position
                    for (r in 0 until boardSize) {
                        for (c in 0 until boardSize) {
                            visited[r][c] = false
                        }
                    }

                    // Start DFS from this left column piece
                    if (isConnectedToRight(row, 0, playerValue, visited)) {
                        return true
                    }
                }
            }
            return false
        }

        return false
    }

    /**
     * Checks if a cell is connected to the bottom row using DFS. FIXED: Correct neighbor
     * calculation for hexagonal grid
     */
    private fun isConnectedToBottom(
            row: Int,
            col: Int,
            playerValue: Int,
            visited: Array<BooleanArray>
    ): Boolean {
        // If we reached the bottom row, we found a path
        if (row == boardSize - 1) {
            winningPath.add(Pair(row, col))
            return true
        }

        // Mark current cell as visited
        visited[row][col] = true

        // Define all six neighbor directions for a hexagonal grid
        val neighbors =
                arrayOf(
                        Pair(-1, 0), // Top-left
                        Pair(-1, 1), // Top-right
                        Pair(0, -1), // Left
                        Pair(0, 1), // Right
                        Pair(1, -1), // Bottom-left
                        Pair(1, 0) // Bottom-right
                )

        // Check all 6 neighbors
        for ((dr, dc) in neighbors) {
            val newRow = row + dr
            val newCol = col + dc

            // Check if the neighbor is valid, not visited, and belongs to the player
            if (newRow in 0 until boardSize &&
                            newCol in 0 until boardSize &&
                            !visited[newRow][newCol] &&
                            board[newRow][newCol] == playerValue
            ) {
                // Recursively check if this neighbor leads to the bottom
                if (isConnectedToBottom(newRow, newCol, playerValue, visited)) {
                    // Add current cell to the winning path when returning from a successful path
                    winningPath.add(Pair(row, col))
                    return true
                }
            }
        }

        return false
    }
    /**
     * Checks if a cell is connected to the right column using DFS. FIXED: Correct neighbor
     * calculation for hexagonal grid
     */
    private fun isConnectedToRight(
            row: Int,
            col: Int,
            playerValue: Int,
            visited: Array<BooleanArray>
    ): Boolean {
        // If we reached the rightmost column, we found a path
        if (col == boardSize - 1) {
            winningPath.add(Pair(row, col))
            return true
        }

        // Mark current cell as visited
        visited[row][col] = true

        // Define all six neighbor directions for a hexagonal grid
        val neighbors =
                arrayOf(
                        Pair(-1, 0), // Top-left
                        Pair(-1, 1), // Top-right
                        Pair(0, -1), // Left
                        Pair(0, 1), // Right
                        Pair(1, -1), // Bottom-left
                        Pair(1, 0) // Bottom-right
                )

        // Check all 6 neighbors
        for ((dr, dc) in neighbors) {
            val newRow = row + dr
            val newCol = col + dc

            // Check if the neighbor is valid, not visited, and belongs to the player
            if (newRow in 0 until boardSize &&
                            newCol in 0 until boardSize &&
                            !visited[newRow][newCol] &&
                            board[newRow][newCol] == playerValue
            ) {
                // Recursively check if this neighbor leads to the right edge
                if (isConnectedToRight(newRow, newCol, playerValue, visited)) {
                    // Add current cell to the winning path when returning from a successful path
                    winningPath.add(Pair(row, col))
                    return true
                }
            }
        }

        return false
    }

    /** Saves the current game state to persistent storage */
    suspend fun saveState(context: Context) =
            withContext(Dispatchers.IO) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit {

                    // Convert board to string
                    val boardStr = StringBuilder()
                    for (row in 0 until boardSize) {
                        for (col in 0 until boardSize) {
                            boardStr.append(board[row][col])
                            if (col < boardSize - 1) boardStr.append(",")
                        }
                        if (row < boardSize - 1) boardStr.append(";")
                    }

                    putString(KEY_BOARD, boardStr.toString())
                    putInt(KEY_CURRENT_PLAYER, currentPlayer)
                    putInt(KEY_BOARD_SIZE, boardSize)
                    putInt(KEY_WIN_CONDITION, winCondition)
                    putBoolean(KEY_AGAINST_COMPUTER, againstComputer)
                }
            }

    /**
     * Loads a saved game state from persistent storage Returns true if a state was loaded, false
     * otherwise
     */
    suspend fun loadState(context: Context): Boolean =
            withContext(Dispatchers.IO) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val boardStr = prefs.getString(KEY_BOARD, null) ?: return@withContext false

                // Load board configuration
                boardSize = prefs.getInt(KEY_BOARD_SIZE, DEFAULT_BOARD_SIZE)
                winCondition = prefs.getInt(KEY_WIN_CONDITION, DEFAULT_WIN_CONDITION)
                againstComputer = prefs.getBoolean(KEY_AGAINST_COMPUTER, false)

                // Parse board from string
                val newBoard = Array(boardSize) { IntArray(boardSize) { EMPTY } }
                val rows = boardStr.split(";")
                for (i in rows.indices) {
                    val cols = rows[i].split(",")
                    for (j in cols.indices) {
                        newBoard[i][j] = cols[j].toInt()
                    }
                }

                // Update board through the property to trigger state update
                board = newBoard

                currentPlayer = prefs.getInt(KEY_CURRENT_PLAYER, PLAYER_ONE)
                true
            }

    /** Clears saved game state */
    suspend fun clearSavedState(context: Context) =
            withContext(Dispatchers.IO) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit { remove(KEY_BOARD) }
            }

    /** Helper class for persistent storage of discovered easter eggs */
    class EasterEggManager(private val context: Context) {
        private val prefs: SharedPreferences by lazy {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        fun getDiscoveredEasterEggs(): Set<String> {
            return prefs.getStringSet(KEY_EASTER_EGGS, emptySet()) ?: emptySet()
        }

        fun addDiscoveredEasterEgg(eggName: String) {
            val currentEggs = getDiscoveredEasterEggs().toMutableSet()
            currentEggs.add(eggName)
            prefs.edit { putStringSet(KEY_EASTER_EGGS, currentEggs) }
        }
    }

    /**
     * Completely rewritten win logic for Havannah to properly detect all three win conditions:
     * 1. Ring: A closed loop of pieces that surrounds at least one cell
     * 2. Bridge: A connection between any two corners
     * 3. Fork: A connection between any three sides (not including corners)
     */
    fun checkHavannahWin(playerValue: Int): Boolean {
        // Get the game type and edge length
        val gameType = GameType.fromGameState(this)
        val edgeLength = gameType.getEdgeLength()

        // Only run for Havannah game
        if (edgeLength <= 0 || winCondition != 9) {
            return false
        }

        // Clear the winning path
        winningPath.clear()

        // Reset win type
        havannahWinType = 0

        // Check for a ring first
        if (checkForRing(playerValue)) {
            havannahWinType = 1
            return true
        }

        // Check for a bridge
        if (checkForBridge(playerValue)) {
            havannahWinType = 2
            return true
        }

        // Check for a fork
        if (checkForFork(playerValue)) {
            havannahWinType = 3
            return true
        }

        return false
    }

    // Store the win type for UI feedback
    private var havannahWinType = 0
    fun getWinningType(): Int = havannahWinType

    /**
     * Detects a ring pattern - a closed loop that surrounds at least one cell. The surrounded cells
     * can be empty or contain any player's pieces.
     */
    private fun checkForRing(playerValue: Int): Boolean {
        // Build a graph representation of the player's pieces
        val graph = buildPlayerGraph(playerValue)
        if (graph.isEmpty()) return false

        // For each piece, try to find a cycle
        for (startPos in graph.keys) {
            val visited = mutableSetOf<Pair<Int, Int>>()
            val path = mutableListOf<Pair<Int, Int>>()

            if (findCycle(startPos, startPos, graph, visited, path, 0)) {
                // We found a cycle, now check if it surrounds any cells
                if (path.size >= 6 && cycleEnclosesCell(path)) {
                    winningPath.clear()
                    winningPath.addAll(path)
                    return true
                }
            }
        }

        return false
    }

    /**
     * Builds a graph representation of all the player's pieces, where each piece is connected to
     * adjacent pieces of the same player.
     */
    private fun buildPlayerGraph(playerValue: Int): Map<Pair<Int, Int>, List<Pair<Int, Int>>> {
        val graph = mutableMapOf<Pair<Int, Int>, MutableList<Pair<Int, Int>>>()
        val gameType = GameType.fromGameState(this)

        // Add all player's pieces to the graph
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                // Skip positions that aren't valid hexagonal positions
                if (!gameType.isValidHexPosition(row, col)) {
                    continue
                }

                if (board[row][col] == playerValue) {
                    val pos = Pair(row, col)
                    graph[pos] = mutableListOf()

                    // Check all 6 neighbors
                    for ((dr, dc) in DIRECTIONS) {
                        val newRow = row + dr
                        val newCol = col + dc

                        if (gameType.isValidHexPosition(newRow, newCol) &&
                                        board[newRow][newCol] == playerValue
                        ) {
                            graph[pos]!!.add(Pair(newRow, newCol))
                        }
                    }
                }
            }
        }

        return graph
    }

    /**
     * Find a cycle in the graph using depth-first search. A cycle is a path that starts and ends at
     * the same position.
     */
    private fun findCycle(
            startPos: Pair<Int, Int>,
            currentPos: Pair<Int, Int>,
            graph: Map<Pair<Int, Int>, List<Pair<Int, Int>>>,
            visited: MutableSet<Pair<Int, Int>>,
            path: MutableList<Pair<Int, Int>>,
            depth: Int
    ): Boolean {
        // Add current position to path
        path.add(currentPos)
        visited.add(currentPos)

        // Get neighbors
        val neighbors = graph[currentPos] ?: emptyList()

        for (neighbor in neighbors) {
            // If we found the starting position and moved at least 3 steps, we have a cycle
            // (Minimum cycle in a hex grid is 6 hexagons, but we've already included
            // the start position so we need depth >= 5)
            if (neighbor == startPos && depth >= 5) {
                return true
            }

            // Skip visited neighbors
            if (neighbor in visited) continue

            // Recursively search from this neighbor
            if (findCycle(startPos, neighbor, graph, visited, path, depth + 1)) {
                return true
            }
        }

        // Backtrack if no cycle found
        path.removeAt(path.size - 1)
        return false
    }

    /**
     * Checks if a cycle encloses at least one cell. Uses a flood fill algorithm to check if any
     * cell inside the cycle cannot reach the edge of the board.
     */
    private fun cycleEnclosesCell(cycle: List<Pair<Int, Int>>): Boolean {
        if (cycle.size < 6) return false
        val gameType = GameType.fromGameState(this)

        // Find bounds of the cycle
        val minRow = cycle.minOf { it.first }
        val maxRow = cycle.maxOf { it.first }
        val minCol = cycle.minOf { it.second }
        val maxCol = cycle.maxOf { it.second }

        // Create a set of cycle positions for quick lookup
        val cycleSet = cycle.toSet()

        // Try to find an interior point
        for (row in minRow..maxRow) {
            for (col in minCol..maxCol) {
                val pos = Pair(row, col)

                // Skip points on the cycle or points outside the valid hex area
                if (pos in cycleSet || !gameType.isValidHexPosition(row, col)) continue

                // Check if this point is enclosed by the cycle
                if (isEnclosedByLoop(pos, cycleSet)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Checks if a position is enclosed by a loop of stones. Uses flood fill to check if it can
     * reach the edge of the board.
     */
    private fun isEnclosedByLoop(pos: Pair<Int, Int>, loop: Set<Pair<Int, Int>>): Boolean {
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        val gameType = GameType.fromGameState(this)

        queue.add(pos)
        visited.add(pos)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val (row, col) = current

            // If we've reached an edge position, this cell is not enclosed
            if (isEdgePosition(row, col)) {
                return false
            }

            // Check all 6 neighbors
            for ((dr, dc) in DIRECTIONS) {
                val newRow = row + dr
                val newCol = col + dc
                val newPos = Pair(newRow, newCol)

                // Skip invalid positions
                if (!gameType.isValidHexPosition(newRow, newCol)) {
                    continue
                }

                // Skip positions on the loop and already visited positions
                if (newPos in loop || newPos in visited) {
                    continue
                }

                queue.add(newPos)
                visited.add(newPos)
            }
        }

        // If we exhausted all reachable cells and didn't find the edge,
        // this cell is enclosed
        return true
    }

    /** Helper method to check if a position is on the edge of the hexagonal board. */
    private fun isEdgePosition(row: Int, col: Int): Boolean {
        val gameType = GameType.fromGameState(this)
        val edgeLength = gameType.getEdgeLength()
        val boardCenter = boardSize / 2
        val range = edgeLength - 1

        // Convert to hex coordinates
        val q = col - boardCenter
        val r = row - boardCenter
        val s = -q - r

        return abs(q) == range || abs(r) == range || abs(s) == range
    }

    /** Detects a bridge pattern - a path connecting any two corners. */
    private fun checkForBridge(playerValue: Int): Boolean {
        // Define the 6 corners of the hexagonal board
        val corners = getCornerPositions()

        // Get all corners occupied by the player
        val playerCorners = corners.filter { (row, col) -> board[row][col] == playerValue }

        // We need at least 2 corners for a bridge
        if (playerCorners.size < 2) return false

        // Build a graph of the player's pieces
        val graph = buildPlayerGraph(playerValue)

        // Check all pairs of corners
        for (i in 0 until playerCorners.size - 1) {
            for (j in i + 1 until playerCorners.size) {
                val corner1 = playerCorners[i]
                val corner2 = playerCorners[j]

                // Find a path between the corners
                val path = findPath(corner1, corner2, graph)
                if (path.isNotEmpty()) {
                    winningPath.clear()
                    winningPath.addAll(path)
                    return true
                }
            }
        }

        return false
    }

    /** Gets the 6 corner positions of the hexagonal board. */
    // Method to get corner positions of the hexagonal board
    private fun getCornerPositions(): List<Pair<Int, Int>> {
        val gameType = GameType.fromGameState(this)
        val edgeLength = gameType.getEdgeLength()
        val boardCenter = boardSize / 2
        val range = edgeLength - 1

        return listOf(
                Pair(boardCenter - range, boardCenter - range), // top-left
                Pair(boardCenter - range, boardCenter + range), // top-right
                Pair(boardCenter, boardCenter + range), // right
                Pair(boardCenter + range, boardCenter + range), // bottom-right
                Pair(boardCenter + range, boardCenter - range), // bottom-left
                Pair(boardCenter, boardCenter - range) // left
        )
    }

    /** Find a path between two positions in the graph using breadth-first search. */
    private fun findPath(
            start: Pair<Int, Int>,
            end: Pair<Int, Int>,
            graph: Map<Pair<Int, Int>, List<Pair<Int, Int>>>
    ): List<Pair<Int, Int>> {
        // Handle trivial case
        if (start == end) return listOf(start)

        // Keep track of visited nodes and paths
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Pair<Int, Int>, List<Pair<Int, Int>>>>()

        // Start BFS
        visited.add(start)
        queue.add(Pair(start, listOf(start)))

        while (queue.isNotEmpty()) {
            val (current, path) = queue.removeFirst()

            // Get neighbors
            val neighbors = graph[current] ?: emptyList()

            for (neighbor in neighbors) {
                if (neighbor == end) {
                    // Found the end, return the path
                    return path + neighbor
                }

                if (neighbor !in visited) {
                    visited.add(neighbor)
                    queue.add(Pair(neighbor, path + neighbor))
                }
            }
        }

        // No path found
        return emptyList()
    }

    /** Detects a fork pattern - a path connecting any three edges (not including corners). */
    private fun checkForFork(playerValue: Int): Boolean {
        // Get all edges of the hexagonal board (excluding corners)
        val edgeCells = getEdgeCells()

        // Group edge cells by which edge they belong to (0-5 for the six edges)
        val edges = Array(6) { mutableListOf<Pair<Int, Int>>() }

        // Populate edge groups
        for ((row, col) in edgeCells) {
            if (board[row][col] != playerValue) continue

            // Determine which edge this cell belongs to based on its position
            val edgeIndex = determineEdgeIndex(row, col)

            if (edgeIndex >= 0) {
                edges[edgeIndex].add(Pair(row, col))
            }
        }

        // Get edges with player's pieces
        val occupiedEdges = edges.filter { it.isNotEmpty() }

        // Need at least 3 edges for a fork
        if (occupiedEdges.size < 3) return false

        // Build player graph
        val graph = buildPlayerGraph(playerValue)

        // Check all combinations of 3 edges
        for (i in 0 until occupiedEdges.size - 2) {
            for (j in i + 1 until occupiedEdges.size - 1) {
                for (k in j + 1 until occupiedEdges.size) {
                    val edge1 = occupiedEdges[i]
                    val edge2 = occupiedEdges[j]
                    val edge3 = occupiedEdges[k]

                    // Check if these three edges are connected
                    if (areEdgesConnected(edge1, edge2, edge3, graph)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /** Determine which edge (0-5) a position belongs to. */
    private fun determineEdgeIndex(row: Int, col: Int): Int {
        val gameType = GameType.fromGameState(this)
        val edgeLength = gameType.getEdgeLength()
        val boardCenter = boardSize / 2
        val range = edgeLength - 1

        // Convert to hex coordinates
        val q = col - boardCenter
        val r = row - boardCenter
        val s = -q - r

        // Skip if it's not on an edge
        if (abs(q) != range && abs(r) != range && abs(s) != range) {
            return -1
        }

        // Skip if it's a corner
        if (isCornerPosition(row, col)) {
            return -1
        }

        // Determine which edge it belongs to
        return when {
            r == -range -> 0 // Top edge
            q == range -> 1 // Top-right edge
            s == -range -> 2 // Bottom-right edge
            r == range -> 3 // Bottom edge
            q == -range -> 4 // Bottom-left edge
            s == range -> 5 // Top-left edge
            else -> -1 // Not on an edge
        }
    }

    /** Helper function to check if a position is a corner. */
    private fun isCornerPosition(row: Int, col: Int): Boolean {
        return getCornerPositions().contains(Pair(row, col))
    }

    /** Gets all edge cells of the hexagonal board (excluding corners). */
    private fun getEdgeCells(): List<Pair<Int, Int>> {
        val gameType = GameType.fromGameState(this)
        val edgeLength = gameType.getEdgeLength()
        val boardCenter = boardSize / 2
        val range = edgeLength - 1
        val corners = getCornerPositions().toSet()
        val edgeCells = mutableListOf<Pair<Int, Int>>()

        // For each of the 6 edges, calculate the positions
        // Edge 1: top-left to top
        for (i in 1 until range) {
            val pos = Pair(boardCenter - range, boardCenter - range + i)
            if (pos !in corners) edgeCells.add(pos)
        }

        // Edge 2: top to top-right
        for (i in 1 until range) {
            val pos = Pair(boardCenter - range + i, boardCenter + range)
            if (pos !in corners) edgeCells.add(pos)
        }

        // Edge 3: top-right to right
        for (i in 1 until range) {
            val pos = Pair(boardCenter - range + i, boardCenter + range - i)
            if (pos !in corners) edgeCells.add(pos)
        }

        // Edge 4: right to bottom-right
        for (i in 1 until range) {
            val pos = Pair(boardCenter + i, boardCenter + range - i)
            if (pos !in corners) edgeCells.add(pos)
        }

        // Edge 5: bottom-right to bottom-left
        for (i in 1 until range) {
            val pos = Pair(boardCenter + range, boardCenter - range + i)
            if (pos !in corners) edgeCells.add(pos)
        }

        // Edge 6: bottom-left to left
        for (i in 1 until range) {
            val pos = Pair(boardCenter + range - i, boardCenter - range)
            if (pos !in corners) edgeCells.add(pos)
        }

        return edgeCells
    }

    /** Checks if three edges have a common junction point that connects them all. */
    private fun areEdgesConnected(
            edge1: List<Pair<Int, Int>>,
            edge2: List<Pair<Int, Int>>,
            edge3: List<Pair<Int, Int>>,
            graph: Map<Pair<Int, Int>, List<Pair<Int, Int>>>
    ): Boolean {
        // For each piece on edge1, find paths to edges 2 and 3
        for (start in edge1) {
            // Find all connected pieces from this starting point
            val connectedPieces = findAllConnectedPieces(start, graph)

            // Check if connected pieces include at least one piece from each of the other edges
            val reachesEdge2 = edge2.any { it in connectedPieces }
            val reachesEdge3 = edge3.any { it in connectedPieces }

            if (reachesEdge2 && reachesEdge3) {
                // Find the complete path for visualization
                val path = findForkPath(start, edge2, edge3, graph)
                winningPath.clear()
                winningPath.addAll(path)
                return true
            }
        }

        return false
    }

    /** Finds all pieces connected to a starting piece. */
    private fun findAllConnectedPieces(
            start: Pair<Int, Int>,
            graph: Map<Pair<Int, Int>, List<Pair<Int, Int>>>
    ): Set<Pair<Int, Int>> {
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()

        visited.add(start)
        queue.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val neighbors = graph[current] ?: emptyList()

            for (neighbor in neighbors) {
                if (neighbor !in visited) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }

        return visited
    }

    /** Finds a fork path connecting three edges for visualization. */
    private fun findForkPath(
            start: Pair<Int, Int>,
            edge2: List<Pair<Int, Int>>,
            edge3: List<Pair<Int, Int>>,
            graph: Map<Pair<Int, Int>, List<Pair<Int, Int>>>
    ): Set<Pair<Int, Int>> {
        // Find a path to edge2
        val path2 = findPathToAny(start, edge2, graph)

        // Find a path to edge3
        val path3 = findPathToAny(start, edge3, graph)

        // Combine paths
        val completePath = mutableSetOf<Pair<Int, Int>>()
        completePath.addAll(path2)
        completePath.addAll(path3)

        return completePath
    }

    /** Finds a path from a start position to any position in a target list. */
    private fun findPathToAny(
            start: Pair<Int, Int>,
            targets: List<Pair<Int, Int>>,
            graph: Map<Pair<Int, Int>, List<Pair<Int, Int>>>
    ): List<Pair<Int, Int>> {
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Pair<Int, Int>, List<Pair<Int, Int>>>>()

        visited.add(start)
        queue.add(Pair(start, listOf(start)))

        while (queue.isNotEmpty()) {
            val (current, path) = queue.removeFirst()

            if (current in targets) {
                return path
            }

            val neighbors = graph[current] ?: emptyList()
            for (neighbor in neighbors) {
                if (neighbor !in visited) {
                    visited.add(neighbor)
                    queue.add(Pair(neighbor, path + neighbor))
                }
            }
        }

        return emptyList()
    }
}
