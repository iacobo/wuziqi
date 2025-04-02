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
     * Special win checker for Havannah game that checks for rings, bridges, and forks.
     *
     * @param playerValue The player value to check for
     * @return True if the player has won, false otherwise
     */
    fun checkHavannahWin(playerValue: Int): Boolean {
        // Only run Havannah win check for 10x10 board with 9 win condition
        if (boardSize != 10 || winCondition != 9) {
            return false
        }

        // Reset winning path before checking
        winningPath.clear()

        // Reset the win type
        havannahWinType = 0

        // Check for rings
        if (checkHavannahRing(playerValue)) {
            havannahWinType = 1
            return true
        }

        // Check for bridges (connections between corners)
        if (checkHavannahBridge(playerValue)) {
            havannahWinType = 2
            return true
        }

        // Check for forks (connections between three sides)
        if (checkHavannahFork(playerValue)) {
            havannahWinType = 3
            return true
        }

        return false
    }

    /** Variable to store the win type for Havannah (1=ring, 2=bridge, 3=fork) */
    private var havannahWinType: Int = 0

    /** Gets the Havannah win type (1=ring, 2=bridge, 3=fork) */
    fun getWinningType(): Int = havannahWinType

    /**
     * Checks if a player has formed a ring in Havannah. A ring is a loop that surrounds at least
     * one cell (can be empty, opponent's, or anything).
     */
    private fun checkHavannahRing(playerValue: Int): Boolean {
        // Reset the winning path
        winningPath.clear()

        // Create a visited array
        val visited = Array(boardSize) { BooleanArray(boardSize) { false } }

        // For each player's stone, try to find a ring starting from it
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] == playerValue && isValidHavannahPosition(row, col)) {
                    // Reset visited array for each starting position
                    for (r in 0 until boardSize) {
                        for (c in 0 until boardSize) {
                            visited[r][c] = false
                        }
                    }

                    // Track the path being built
                    val currentPath = mutableSetOf<Pair<Int, Int>>()
                    val exploredEdges = mutableSetOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()

                    // Try to find a ring starting from this stone
                    if (findRingDFS(
                                    row,
                                    col, // starting position
                                    row,
                                    col, // current position
                                    playerValue,
                                    visited,
                                    currentPath,
                                    exploredEdges,
                                    0 // depth
                            )
                    ) {
                        // Check if this is a true ring (must be at least 6 cells to form a ring)
                        if (currentPath.size >= 6) {
                            // Verify that the ring actually encloses at least one cell
                            if (verifyRingEnclosesAnything(currentPath)) {
                                winningPath.addAll(currentPath)
                                return true
                            }
                        }
                    }
                }
            }
        }

        return false
    }

    /** Verifies that a hex position is valid for the Havannah game (within the hexagonal board). */
    private fun isValidHavannahPosition(row: Int, col: Int): Boolean {
        // Convert to axial coordinates centered on the middle of the board
        val q = col - boardSize / 2
        val r = row - boardSize / 2
        val s = -q - r

        // Use the max coordinate to define a regular hexagon
        return maxOf(kotlin.math.abs(q), kotlin.math.abs(r), kotlin.math.abs(s)) <= boardSize / 2
    }

    /** DFS to find a ring (cycle) in the player's connected stones. */
    private fun findRingDFS(
            startRow: Int,
            startCol: Int, // Starting position
            currentRow: Int,
            currentCol: Int, // Current position
            playerValue: Int, // Player we're checking for
            visited: Array<BooleanArray>, // Visited positions
            currentPath: MutableSet<Pair<Int, Int>>, // Current path being built
            exploredEdges:
                    MutableSet<
                            Pair<Pair<Int, Int>, Pair<Int, Int>>>, // Edges we've already explored
            depth: Int // Current depth of search
    ): Boolean {
        // Add current position to path
        currentPath.add(Pair(currentRow, currentCol))

        // Mark current position as visited
        visited[currentRow][currentCol] = true

        // Directions for a hexagonal grid
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
            val newRow = currentRow + dr
            val newCol = currentCol + dc

            // Skip invalid positions
            if (newRow < 0 ||
                            newRow >= boardSize ||
                            newCol < 0 ||
                            newCol >= boardSize ||
                            !isValidHavannahPosition(newRow, newCol)
            ) {
                continue
            }

            // Skip positions that don't have the player's stone
            if (board[newRow][newCol] != playerValue) {
                continue
            }

            // Check if this completes a ring - only if depth is high enough to form a real ring
            if ((newRow == startRow && newCol == startCol) && depth >= 5) {
                // We found a ring (cycle) with at least 6 stones
                return true
            }

            // Create an edge representation (sorted pair of positions)
            val edge =
                    if (currentRow < newRow || (currentRow == newRow && currentCol < newCol)) {
                        Pair(Pair(currentRow, currentCol), Pair(newRow, newCol))
                    } else {
                        Pair(Pair(newRow, newCol), Pair(currentRow, currentCol))
                    }

            // Skip if we've already explored this edge
            if (edge in exploredEdges) {
                continue
            }

            // Mark this edge as explored
            exploredEdges.add(edge)

            // If the neighbor is unvisited, recursively search from it
            if (!visited[newRow][newCol]) {
                if (findRingDFS(
                                startRow,
                                startCol,
                                newRow,
                                newCol,
                                playerValue,
                                visited.map { it.clone() }.toTypedArray(),
                                currentPath,
                                exploredEdges,
                                depth + 1
                        )
                ) {
                    return true
                }
            }
        }

        // Remove current position from path (backtrack)
        currentPath.remove(Pair(currentRow, currentCol))

        return false
    }

    /**
     * Verifies that a ring actually encloses something - doesn't matter what. This is critical for
     * fixing the ring detection issue.
     */
    private fun verifyRingEnclosesAnything(ring: Set<Pair<Int, Int>>): Boolean {
        // Find a cell that might be inside the ring
        val potentialInnerCells = mutableListOf<Pair<Int, Int>>()

        // Find the bounding box of the ring
        val minRow = ring.minOf { it.first }
        val maxRow = ring.maxOf { it.first }
        val minCol = ring.minOf { it.second }
        val maxCol = ring.maxOf { it.second }

        // Check all cells within the bounding box
        for (row in minRow..maxRow) {
            for (col in minCol..maxCol) {
                // Skip cells that are part of the ring
                if (Pair(row, col) in ring) {
                    continue
                }

                // Skip cells that are outside the board bounds or not valid Havannah positions
                if (row < 0 ||
                                row >= boardSize ||
                                col < 0 ||
                                col >= boardSize ||
                                !isValidHavannahPosition(row, col)
                ) {
                    continue
                }

                // This is a potential inner cell
                potentialInnerCells.add(Pair(row, col))
            }
        }

        // If there are no potential inner cells, this can't be a valid ring
        if (potentialInnerCells.isEmpty()) {
            return false
        }

        // Pick a test cell near the center of the ring
        val testCell =
                potentialInnerCells.minByOrNull {
                    (it.first - ((minRow + maxRow) / 2)).let { it * it } +
                            (it.second - ((minCol + maxCol) / 2)).let { it * it }
                }
                        ?: return false

        // Check if the test cell is enclosed by conducting a flood fill
        val visited = Array(boardSize) { BooleanArray(boardSize) { false } }
        val enclosedCells = floodFillFromCell(testCell.first, testCell.second, visited, ring)

        // Check if any cell in the flood fill reached the edge of the board
        val reachedEdge =
                enclosedCells.any { (row, col) ->
                    // Check if it's on the edge of the valid Havannah hexagon
                    isOnHavannahBoardEdge(row, col)
                }

        // If we didn't reach an edge, then the cells are enclosed by the ring
        return !reachedEdge && enclosedCells.isNotEmpty()
    }

    /** Checks if a position is on the edge of the Havannah hexagonal board. */
    private fun isOnHavannahBoardEdge(row: Int, col: Int): Boolean {
        // Convert to axial coordinates centered on the middle of the board
        val q = col - boardSize / 2
        val r = row - boardSize / 2
        val s = -q - r

        // The border of a hexagon has at least one coordinate at maximum value
        return maxOf(kotlin.math.abs(q), kotlin.math.abs(r), kotlin.math.abs(s)) == boardSize / 2
    }

    /** Performs a flood fill from a starting cell, avoiding ring cells. */
    private fun floodFillFromCell(
            startRow: Int,
            startCol: Int,
            visited: Array<BooleanArray>,
            ringCells: Set<Pair<Int, Int>>
    ): Set<Pair<Int, Int>> {
        // The cells found by the flood fill
        val result = mutableSetOf<Pair<Int, Int>>()

        // Skip if invalid cell
        if (startRow < 0 ||
                        startRow >= boardSize ||
                        startCol < 0 ||
                        startCol >= boardSize ||
                        !isValidHavannahPosition(startRow, startCol)
        ) {
            return result
        }

        // Skip if already visited or part of the ring
        if (visited[startRow][startCol] || Pair(startRow, startCol) in ringCells) {
            return result
        }

        // Mark as visited and add to result
        visited[startRow][startCol] = true
        result.add(Pair(startRow, startCol))

        // Check all six neighbors in a hexagonal grid
        val directions =
                arrayOf(
                        Pair(-1, 0), // Top-left
                        Pair(-1, 1), // Top-right
                        Pair(0, -1), // Left
                        Pair(0, 1), // Right
                        Pair(1, -1), // Bottom-left
                        Pair(1, 0) // Bottom-right
                )

        // Recursively flood fill from each neighbor
        for ((dr, dc) in directions) {
            val newRow = startRow + dr
            val newCol = startCol + dc

            val newCells = floodFillFromCell(newRow, newCol, visited, ringCells)
            result.addAll(newCells)
        }

        return result
    }

    /** Checks if a player has formed a bridge in Havannah (connecting any two corners). */
    private fun checkHavannahBridge(playerValue: Int): Boolean {
        // Define the six corners of the hexagonal board correctly for a 10x10 board
        // The hexRange equals boardSize/2 = 5
        val hexRange = boardSize / 2
        val corners =
                listOf(
                        Pair(0, 0), // top-left
                        Pair(0, boardSize - 1), // top-right
                        Pair(hexRange, boardSize - 1), // right
                        Pair(boardSize - 1, hexRange), // bottom-right
                        Pair(boardSize - 1, 0), // bottom-left
                        Pair(hexRange, 0) // left
                )

        // Clear the winning path
        winningPath.clear()

        // Check each corner to see if the player occupies it
        val occupiedCorners =
                corners.filter { (row, col) ->
                    row in 0 until boardSize &&
                            col in 0 until boardSize &&
                            isValidHavannahPosition(row, col) &&
                            board[row][col] == playerValue
                }

        // Need at least 2 corners to form a bridge
        if (occupiedCorners.size < 2) {
            return false
        }

        // Check all pairs of occupied corners
        for (i in 0 until occupiedCorners.size - 1) {
            for (j in i + 1 until occupiedCorners.size) {
                val corner1 = occupiedCorners[i]
                val corner2 = occupiedCorners[j]

                // Create a visited array
                val visited = Array(boardSize) { BooleanArray(boardSize) { false } }
                val path = mutableSetOf<Pair<Int, Int>>()

                // Check if there's a path between these corners
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
                    // We found a bridge!
                    winningPath.addAll(path)
                    return true
                }
            }
        }

        return false
    }

    /** Checks if a player has formed a fork in Havannah (connecting any three edges). */
    private fun checkHavannahFork(playerValue: Int): Boolean {
        // Define the six edge segments of the hexagonal board (excluding corners)
        // For a 10x10 board with hexRange = 5
        val hexRange = boardSize / 2

        // Get a set of all edge positions (excluding corners)
        val edges = mutableMapOf<String, MutableSet<Pair<Int, Int>>>()

        // Initialize edge sets
        edges["top"] = mutableSetOf()
        edges["topRight"] = mutableSetOf()
        edges["bottomRight"] = mutableSetOf()
        edges["bottom"] = mutableSetOf()
        edges["bottomLeft"] = mutableSetOf()
        edges["topLeft"] = mutableSetOf()

        // Populate the edge cells
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (!isValidHavannahPosition(row, col)) continue

                // Convert to cubic coordinates to check if this is on an edge
                val q = col - hexRange
                val r = row - hexRange
                val s = -q - r

                // Skip if not on the edge
                if (maxOf(kotlin.math.abs(q), kotlin.math.abs(r), kotlin.math.abs(s)) < hexRange) {
                    continue
                }

                // Skip corners
                if (isHavannahCorner(row, col)) {
                    continue
                }

                // Determine which edge this belongs to
                when {
                    row == 0 -> edges["top"]?.add(Pair(row, col))
                    col == boardSize - 1 && row < hexRange -> edges["topRight"]?.add(Pair(row, col))
                    col == boardSize - 1 && row > hexRange ->
                            edges["bottomRight"]?.add(Pair(row, col))
                    row == boardSize - 1 -> edges["bottom"]?.add(Pair(row, col))
                    col == 0 && row > hexRange -> edges["bottomLeft"]?.add(Pair(row, col))
                    col == 0 && row < hexRange -> edges["topLeft"]?.add(Pair(row, col))
                }
            }
        }

        // Find player's pieces on each edge
        val occupiedEdges = mutableMapOf<String, Set<Pair<Int, Int>>>()

        for ((edgeName, edgeCells) in edges) {
            val occupiedCells =
                    edgeCells.filter { (row, col) -> board[row][col] == playerValue }.toSet()

            if (occupiedCells.isNotEmpty()) {
                occupiedEdges[edgeName] = occupiedCells
            }
        }

        // Need at least 3 occupied edges for a fork
        if (occupiedEdges.size < 3) {
            return false
        }

        // Check all combinations of 3 edges
        val edgeNames = occupiedEdges.keys.toList()
        for (i in 0 until edgeNames.size - 2) {
            for (j in i + 1 until edgeNames.size - 1) {
                for (k in j + 1 until edgeNames.size) {
                    val edge1 = edgeNames[i]
                    val edge2 = edgeNames[j]
                    val edge3 = edgeNames[k]

                    if (checkForkConnection(
                                    occupiedEdges[edge1]!!,
                                    occupiedEdges[edge2]!!,
                                    occupiedEdges[edge3]!!,
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

    /** Checks if a position is a corner of the Havannah board. */
    private fun isHavannahCorner(row: Int, col: Int): Boolean {
        val hexRange = boardSize / 2
        return (row == 0 && col == 0) || // top-left
        (row == 0 && col == boardSize - 1) || // top-right
                (row == hexRange && col == boardSize - 1) || // right
                (row == boardSize - 1 && col == hexRange) || // bottom-right
                (row == boardSize - 1 && col == 0) || // bottom-left
                (row == hexRange && col == 0) // left
    }

    /** Checks if three edges have a common connection point. */
    private fun checkForkConnection(
            edge1: Set<Pair<Int, Int>>,
            edge2: Set<Pair<Int, Int>>,
            edge3: Set<Pair<Int, Int>>,
            playerValue: Int
    ): Boolean {
        // Find a path connecting any cell from edge1 to any cell from edge2
        val visited = Array(boardSize) { BooleanArray(boardSize) }
        val path12 = mutableSetOf<Pair<Int, Int>>()

        // Try different starting points from edge1
        for (start in edge1) {
            // Try different ending points from edge2
            for (end in edge2) {
                // Reset visited array
                for (r in 0 until boardSize) {
                    for (c in 0 until boardSize) {
                        visited[r][c] = false
                    }
                }

                path12.clear()

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
                    // Found a path from edge1 to edge2, now check if it connects to edge3
                    for (cellOnPath in path12) {
                        // Reset visited array
                        for (r in 0 until boardSize) {
                            for (c in 0 until boardSize) {
                                visited[r][c] = false
                            }
                        }

                        val path23 = mutableSetOf<Pair<Int, Int>>()

                        // Try different ending points from edge3
                        for (end3 in edge3) {
                            if (isConnected(
                                            cellOnPath.first,
                                            cellOnPath.second,
                                            end3.first,
                                            end3.second,
                                            playerValue,
                                            visited,
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

    /** Checks if there's a path connecting two points on the board. */
    private fun isConnected(
            startRow: Int,
            startCol: Int,
            endRow: Int,
            endCol: Int,
            playerValue: Int,
            visited: Array<BooleanArray>,
            path: MutableSet<Pair<Int, Int>>
    ): Boolean {
        // Base case: we've reached the destination
        if (startRow == endRow && startCol == endCol) {
            path.add(Pair(startRow, startCol))
            return true
        }

        // Mark current cell as visited
        visited[startRow][startCol] = true
        path.add(Pair(startRow, startCol))

        // Check all six neighbors
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

            // Check if the neighbor is valid, unvisited, and belongs to the player
            if (newRow in 0 until boardSize &&
                            newCol in 0 until boardSize &&
                            isValidHavannahPosition(newRow, newCol) &&
                            !visited[newRow][newCol] &&
                            board[newRow][newCol] == playerValue
            ) {
                // Recursively check if this neighbor leads to the destination
                if (isConnected(newRow, newCol, endRow, endCol, playerValue, visited, path)) {
                    return true
                }
            }
        }

        // Backtrack
        path.remove(Pair(startRow, startCol))
        return false
    }
}
