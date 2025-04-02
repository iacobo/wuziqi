package com.iacobo.wuziqi.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
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

    var currentPlayer: Int = PLAYER_ONE

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
        return row in 0 until boardSize && col in 0 until boardSize
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
     * Completely rewritten win logic for Havannah. Checks all three win conditions:
     * 1. Ring: A closed loop of pieces that surrounds at least one cell
     * 2. Bridge: A connection between any two corners
     * 3. Fork: A connection between any three sides
     */
    fun checkHavannahWin(playerValue: Int): Boolean {
        // Only run for Havannah game
        if (boardSize != 10 || winCondition != 9) {
            return false
        }

        // Clear any previous winning path
        winningPath.clear()

        // Reset win type
        havannahWinType = 0

        // Check for rings first
        if (checkHavannahRing(playerValue)) {
            havannahWinType = 1 // Ring win
            return true
        }

        // Check for bridges between corners
        if (checkHavannahBridge(playerValue)) {
            havannahWinType = 2 // Bridge win
            return true
        }

        // Check for forks connecting three edges
        if (checkHavannahFork(playerValue)) {
            havannahWinType = 3 // Fork win
            return true
        }

        return false
    }

    /** Storage for the win type: 1 = Ring win 2 = Bridge win 3 = Fork win */
    private var havannahWinType: Int = 0

    /** Returns the type of win for display purposes */
    fun getWinningType(): Int = havannahWinType

    /**
     * RING WIN: Checks if player has formed a ring (loop) that surrounds at least one cell. The
     * surrounded cell can be empty or contain any piece.
     */
    private fun checkHavannahRing(playerValue: Int): Boolean {
        // We need to convert from array indices to cube coordinates for proper hexagonal geometry
        // Side length of the hexagon
        val sideLength = 10
        val range = sideLength - 1 // Range of cube coordinates

        // Create a visited array
        val visited = Array(boardSize) { BooleanArray(boardSize) { false } }

        // For each player's stone, try to find a ring
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] == playerValue) {
                    // Convert array indices to cube coordinates
                    val q = col - range
                    val r = row - range
                    val s = -q - r

                    // Skip positions outside the valid hexagon
                    if (maxOf(kotlin.math.abs(q), kotlin.math.abs(r), kotlin.math.abs(s)) > range) {
                        continue
                    }

                    // Reset visited array
                    for (i in 0 until boardSize) {
                        for (j in 0 until boardSize) {
                            visited[i][j] = false
                        }
                    }

                    // Path for this search attempt
                    val currentPath = mutableSetOf<Pair<Int, Int>>()

                    // Try to find a ring starting from this stone
                    if (findRing(
                                    row,
                                    col,
                                    row,
                                    col,
                                    playerValue,
                                    visited,
                                    currentPath,
                                    mutableSetOf(),
                                    0
                            )
                    ) {
                        // Found a potential ring! Verify that it encloses at least one cell
                        if (currentPath.size >= 6 && verifyRingEnclosesCell(currentPath)) {
                            winningPath.addAll(currentPath)
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    /** Depth-first search to find a ring (cycle) starting from a specific position. */
    private fun findRing(
            startRow: Int,
            startCol: Int, // Starting position
            currentRow: Int,
            currentCol: Int, // Current position
            playerValue: Int,
            visited: Array<BooleanArray>,
            currentPath: MutableSet<Pair<Int, Int>>,
            exploredEdges: MutableSet<Pair<Pair<Int, Int>, Pair<Int, Int>>>,
            depth: Int
    ): Boolean {
        // Add current position to the path
        currentPath.add(Pair(currentRow, currentCol))

        // Mark as visited
        visited[currentRow][currentCol] = true

        // Neighbors in a hexagonal grid
        val directions =
                arrayOf(
                        Pair(-1, 0), // Top-left
                        Pair(-1, 1), // Top-right
                        Pair(0, -1), // Left
                        Pair(0, 1), // Right
                        Pair(1, -1), // Bottom-left
                        Pair(1, 0) // Bottom-right
                )

        // Try each neighbor
        for ((dr, dc) in directions) {
            val newRow = currentRow + dr
            val newCol = currentCol + dc

            // Check if this position is valid
            if (newRow < 0 || newRow >= boardSize || newCol < 0 || newCol >= boardSize) {
                continue
            }

            // Check if it's the same player's piece
            if (board[newRow][newCol] != playerValue) {
                continue
            }

            // If we found our starting point and depth is sufficient for a ring, we're done!
            if (newRow == startRow && newCol == startCol && depth >= 5) {
                return true
            }

            // Create an edge representation to avoid reusing the same connection
            val edge =
                    if (currentRow < newRow || (currentRow == newRow && currentCol < newCol)) {
                        Pair(Pair(currentRow, currentCol), Pair(newRow, newCol))
                    } else {
                        Pair(Pair(newRow, newCol), Pair(currentRow, currentCol))
                    }

            // Skip if we've already used this edge
            if (edge in exploredEdges) {
                continue
            }

            // Mark edge as explored
            exploredEdges.add(edge)

            // Only visit unvisited cells (avoid small cycles)
            if (!visited[newRow][newCol]) {
                // Create new visited array to allow multiple paths through same cells
                val newVisited =
                        Array(boardSize) { r -> BooleanArray(boardSize) { c -> visited[r][c] } }

                // Recursively search from this cell
                if (findRing(
                                startRow,
                                startCol,
                                newRow,
                                newCol,
                                playerValue,
                                newVisited,
                                currentPath,
                                exploredEdges,
                                depth + 1
                        )
                ) {
                    return true
                }
            }
        }

        // Remove current position if no path was found (backtrack)
        currentPath.remove(Pair(currentRow, currentCol))

        return false
    }

    /**
     * Checks if a ring actually encloses at least one cell. Uses a flood fill algorithm to test if
     * there are cells completely surrounded by the ring.
     */
    private fun verifyRingEnclosesCell(ring: Set<Pair<Int, Int>>): Boolean {
        // Find the bounding box of the ring
        val minRow = ring.minOf { it.first }
        val maxRow = ring.maxOf { it.first }
        val minCol = ring.minOf { it.second }
        val maxCol = ring.maxOf { it.second }

        // Find potential inner cells
        val potentialInnerCells = mutableListOf<Pair<Int, Int>>()

        // Check all cells in the bounding box
        for (row in minRow..maxRow) {
            for (col in minCol..maxCol) {
                // Skip ring cells
                if (Pair(row, col) in ring) {
                    continue
                }

                // Skip invalid cells
                if (row < 0 || row >= boardSize || col < 0 || col >= boardSize) {
                    continue
                }

                // Potential inner cell
                potentialInnerCells.add(Pair(row, col))
            }
        }

        // If no potential inner cells, not a valid ring
        if (potentialInnerCells.isEmpty()) {
            return false
        }

        // Use a cell near the center of the ring as test point
        val centerRow = (minRow + maxRow) / 2
        val centerCol = (minCol + maxCol) / 2

        // Find the closest cell to the center that isn't part of the ring
        val testCell =
                potentialInnerCells.minByOrNull { cell ->
                    val rowDiff = cell.first - centerRow
                    val colDiff = cell.second - centerCol
                    rowDiff * rowDiff + colDiff * colDiff
                }
                        ?: return false

        // Do a flood fill from this cell to see if it can reach the board edge
        val visited = Array(boardSize) { BooleanArray(boardSize) { false } }
        val reachesEdge = floodFillReachesEdge(testCell.first, testCell.second, visited, ring)

        // If the flood fill can't reach the edge, the ring encloses this cell
        return !reachesEdge
    }

    /**
     * Flood fill algorithm to check if a cell can reach the board edge without crossing the ring.
     */
    private fun floodFillReachesEdge(
            row: Int,
            col: Int,
            visited: Array<BooleanArray>,
            ring: Set<Pair<Int, Int>>
    ): Boolean {
        // Skip invalid positions
        if (row < 0 || row >= boardSize || col < 0 || col >= boardSize) {
            return false
        }

        // Skip cells we've already visited or that are part of the ring
        if (visited[row][col] || Pair(row, col) in ring) {
            return false
        }

        // Mark as visited
        visited[row][col] = true

        // If we've reached the edge, we're done
        if (isHavannahBoardEdge(row, col)) {
            return true
        }

        // Check all six directions in a hexagonal grid
        val directions =
                arrayOf(
                        Pair(-1, 0), // Top-left
                        Pair(-1, 1), // Top-right
                        Pair(0, -1), // Left
                        Pair(0, 1), // Right
                        Pair(1, -1), // Bottom-left
                        Pair(1, 0) // Bottom-right
                )

        // Try each direction
        for ((dr, dc) in directions) {
            val newRow = row + dr
            val newCol = col + dc

            // If any neighbor can reach the edge, we can too
            if (floodFillReachesEdge(newRow, newCol, visited, ring)) {
                return true
            }
        }

        // Can't reach the edge from here
        return false
    }

    /** BRIDGE WIN: Checks if player has connected any two corners with their pieces. */
    private fun checkHavannahBridge(playerValue: Int): Boolean {
        // Define the six corners of the hexagonal board
        // For a size-10 board, the range of cube coordinates is -9 to 9
        val sideLength = 10
        val range = sideLength - 1

        // List of all corner positions in array coordinates
        val corners =
                listOf(
                        Pair(0, 0), // top-left
                        Pair(0, boardSize - 1), // top-right
                        Pair(range, boardSize - 1), // right
                        Pair(boardSize - 1, range), // bottom-right
                        Pair(boardSize - 1, 0), // bottom-left
                        Pair(range, 0) // left
                )

        // Find which corners have the player's pieces
        val playerCorners =
                corners.filter { (row, col) ->
                    row in 0 until boardSize &&
                            col in 0 until boardSize &&
                            board[row][col] == playerValue
                }

        // Need at least 2 corners to make a bridge
        if (playerCorners.size < 2) {
            return false
        }

        // Check all pairs of corners
        for (i in 0 until playerCorners.size - 1) {
            for (j in i + 1 until playerCorners.size) {
                val corner1 = playerCorners[i]
                val corner2 = playerCorners[j]

                // Create a visited array
                val visited = Array(boardSize) { BooleanArray(boardSize) { false } }

                // Find path between corners
                val path = mutableSetOf<Pair<Int, Int>>()
                if (isConnected(
                                corner1.first,
                                corner1.second,
                                corner2.first,
                                corner2.second,
                                playerValue,
                                visited,
                                path
                        )
                ) {
                    // Bridge found!
                    winningPath.addAll(path)
                    return true
                }
            }
        }

        return false
    }

    /** FORK WIN: Checks if player has connected any three different edges of the board. */
    private fun checkHavannahFork(playerValue: Int): Boolean {
        // Define the six edge segments (excluding corners)
        val sideLength = 10
        val range = sideLength - 1

        // Get all edge cells occupied by the player
        val edgeCells = findPlayerEdgeCells(playerValue)

        // Group them by which edge they belong to
        val edges = mutableMapOf<String, MutableList<Pair<Int, Int>>>()

        for ((row, col) in edgeCells) {
            // Convert to cube coordinates
            val q = col - range
            val r = row - range
            val s = -q - r

            // Determine which edge this belongs to
            val edgeName =
                    when {
                        r == range && q < 0 && q > -range -> "top-left"
                        r == range && q > 0 && q < range -> "top-right"
                        q == range && r < 0 && r > -range -> "right"
                        s == range && q > -range && q < 0 -> "bottom-right"
                        s == range && q < range && q > 0 -> "bottom-left"
                        q == -range && r < range && r > 0 -> "left"
                        else -> continue // Corners are excluded
                    }

            edges.getOrPut(edgeName) { mutableListOf() }.add(Pair(row, col))
        }

        // Need at least 3 different edges to make a fork
        if (edges.size < 3) {
            return false
        }

        // Try all combinations of 3 edges
        val edgeNames = edges.keys.toList()
        for (i in 0 until edgeNames.size - 2) {
            for (j in i + 1 until edgeNames.size - 1) {
                for (k in j + 1 until edgeNames.size) {
                    val edge1 = edgeNames[i]
                    val edge2 = edgeNames[j]
                    val edge3 = edgeNames[k]

                    // Try to find a common connection point
                    if (checkForkConnection(
                                    edges[edge1]!!,
                                    edges[edge2]!!,
                                    edges[edge3]!!,
                                    playerValue
                            )
                    ) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /** Finds all edge cells (excluding corners) occupied by the player. */
    private fun findPlayerEdgeCells(playerValue: Int): List<Pair<Int, Int>> {
        val edgeCells = mutableListOf<Pair<Int, Int>>()
        val sideLength = 10
        val range = sideLength - 1

        // Check every cell on the board
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                // Only consider player's pieces
                if (board[row][col] != playerValue) {
                    continue
                }

                // Convert to cube coordinates
                val q = col - range
                val r = row - range
                val s = -q - r

                // Check if it's on an edge but not a corner
                if (isHavannahEdge(q, r, s, range) && !isHavannahCorner(q, r, s, range)) {
                    edgeCells.add(Pair(row, col))
                }
            }
        }

        return edgeCells
    }

    /** Checks if cube coordinates represent an edge position. */
    private fun isHavannahEdge(q: Int, r: Int, s: Int, range: Int): Boolean {
        return kotlin.math.abs(q) == range ||
                kotlin.math.abs(r) == range ||
                kotlin.math.abs(s) == range
    }

    /** Checks if cube coordinates represent a corner position. */
    private fun isHavannahCorner(q: Int, r: Int, s: Int, range: Int): Boolean {
        return (q == -range && r == range) || // top-left
        (q == 0 && r == range) || // top
                (q == range && r == 0) || // top-right
                (q == range && r == -range) || // bottom-right
                (q == 0 && r == -range) || // bottom
                (q == -range && r == 0) // bottom-left
    }

    /** Checks if a position is on the edge of the board in array coordinates. */
    private fun isHavannahBoardEdge(row: Int, col: Int): Boolean {
        val sideLength = 10
        val range = sideLength - 1

        // Convert to cube coordinates
        val q = col - range
        val r = row - range
        val s = -q - r

        // Edge cells have at least one coordinate at exactly ±range
        return kotlin.math.abs(q) == range ||
                kotlin.math.abs(r) == range ||
                kotlin.math.abs(s) == range
    }

    /** Checks if three edges are connected by player's pieces. */
    private fun checkForkConnection(
            edge1: List<Pair<Int, Int>>,
            edge2: List<Pair<Int, Int>>,
            edge3: List<Pair<Int, Int>>,
            playerValue: Int
    ): Boolean {
        // Try to find a path from edge1 to edge2
        for (start in edge1) {
            for (end in edge2) {
                val visited = Array(boardSize) { BooleanArray(boardSize) { false } }
                val path12 = mutableSetOf<Pair<Int, Int>>()

                if (isConnected(
                                start.first,
                                start.second,
                                end.first,
                                end.second,
                                playerValue,
                                visited,
                                path12
                        )
                ) {
                    // Now try to connect this path to edge3
                    for (point in path12) {
                        for (end3 in edge3) {
                            val visited3 = Array(boardSize) { BooleanArray(boardSize) { false } }
                            val path23 = mutableSetOf<Pair<Int, Int>>()

                            if (isConnected(
                                            point.first,
                                            point.second,
                                            end3.first,
                                            end3.second,
                                            playerValue,
                                            visited3,
                                            path23
                                    )
                            ) {
                                // We found a fork!
                                winningPath.clear()
                                winningPath.addAll(path12)
                                winningPath.addAll(path23)
                                return true
                            }
                        }
                    }
                }
            }
        }

        return false
    }

    /** Checks if there's a connected path between two points made of the player's pieces. */
    private fun isConnected(
            startRow: Int,
            startCol: Int,
            endRow: Int,
            endCol: Int,
            playerValue: Int,
            visited: Array<BooleanArray>,
            path: MutableSet<Pair<Int, Int>>
    ): Boolean {
        // If we reached the destination, we're done
        if (startRow == endRow && startCol == endCol) {
            path.add(Pair(startRow, startCol))
            return true
        }

        // Mark as visited
        visited[startRow][startCol] = true
        path.add(Pair(startRow, startCol))

        // Try all six directions in a hexagonal grid
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
            val newRow = startRow + dr
            val newCol = startCol + dc

            // Check if this is a valid position with the player's piece
            if (newRow in 0 until boardSize &&
                            newCol in 0 until boardSize &&
                            !visited[newRow][newCol] &&
                            board[newRow][newCol] == playerValue
            ) {

                // Recursively check if this leads to the destination
                if (isConnected(newRow, newCol, endRow, endCol, playerValue, visited, path)) {
                    return true
                }
            }
        }

        // Backtrack if no path found
        path.remove(Pair(startRow, startCol))
        return false
    }
}
