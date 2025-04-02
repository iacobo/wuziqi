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
     * one cell.
     */
    private fun checkHavannahRing(playerValue: Int): Boolean {
        // Reset the winning path
        winningPath.clear()

        // Create a visited array
        val visited = Array(boardSize) { BooleanArray(boardSize) { false } }

        // For each player's stone, try to find a ring starting from it
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] == playerValue) {
                    // Reset visited array for each starting position
                    for (r in 0 until boardSize) {
                        for (c in 0 until boardSize) {
                            visited[r][c] = false
                        }
                    }

                    // Track the path being built
                    val currentPath = mutableSetOf<Pair<Int, Int>>()

                    // Try to find a ring starting from this stone
                    if (findRingDFS(
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
                        // Check if this is a true ring (must enclose at least one cell)
                        if (currentPath.size >= 6
                        ) { // Minimum size for a ring that can enclose a cell
                            // Verify that the ring actually encloses at least one cell
                            if (verifyRingEnclosesCell(currentPath, playerValue)) {
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

        // Check all six neighboring positions
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
            if (newRow < 0 || newRow >= boardSize || newCol < 0 || newCol >= boardSize) {
                continue
            }

            // Skip positions that don't have the player's stone
            if (board[newRow][newCol] != playerValue) {
                continue
            }

            // Check if this completes a ring
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

    /** Verifies that a ring actually encloses at least one cell. */
    private fun verifyRingEnclosesCell(ring: Set<Pair<Int, Int>>, playerValue: Int): Boolean {
        // Find a cell that's inside the ring
        val boardCenter = boardSize / 2
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

                // Skip cells that are outside the board bounds
                if (row < 0 || row >= boardSize || col < 0 || col >= boardSize) {
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

        // Check if any of the potential inner cells is truly inside the ring
        // For Havannah, a cell is inside the ring if it can't reach the edge of the board
        // without crossing the ring

        // Pick a test cell (preferably near the center of the bounding box)
        val testCell =
                potentialInnerCells.minByOrNull {
                    (it.first - ((minRow + maxRow) / 2)).let { it * it } +
                            (it.second - ((minCol + maxCol) / 2)).let { it * it }
                }
                        ?: return false

        // Check if the test cell is enclosed (cannot reach the edge without crossing the ring)
        val visited = Array(boardSize) { BooleanArray(boardSize) { false } }
        val enclosedCells = floodFillFromCell(testCell.first, testCell.second, visited, ring)

        // If any enclosed cell contains a board edge, the ring doesn't enclose anything
        for (cell in enclosedCells) {
            val (row, col) = cell
            if (row == 0 || row == boardSize - 1 || col == 0 || col == boardSize - 1) {
                return false
            }
        }

        // The ring encloses at least one cell
        return enclosedCells.isNotEmpty()
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
        if (startRow < 0 || startRow >= boardSize || startCol < 0 || startCol >= boardSize) {
            return result
        }

        // Skip if already visited or part of the ring
        if (visited[startRow][startCol] || Pair(startRow, startCol) in ringCells) {
            return result
        }

        // Mark as visited and add to result
        visited[startRow][startCol] = true
        result.add(Pair(startRow, startCol))

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

        // Recursively flood fill from each neighbor
        for ((dr, dc) in directions) {
            val newRow = startRow + dr
            val newCol = startCol + dc

            val newCells = floodFillFromCell(newRow, newCol, visited, ringCells)
            result.addAll(newCells)
        }

        return result
    }

    /**
     * Checks if a player has formed a bridge in Havannah. A bridge connects any two corners of the
     * hexagonal board.
     */
    private fun checkHavannahBridge(playerValue: Int): Boolean {
        // Define the six corners in our board representation
        val corners =
                listOf(
                        Pair(0, 0), // Top-left
                        Pair(0, boardSize - 1), // Top-right
                        Pair(boardSize / 2, boardSize - 1), // Right
                        Pair(boardSize - 1, boardSize / 2), // Bottom-right
                        Pair(boardSize - 1, 0), // Bottom-left
                        Pair(boardSize / 2, 0) // Left
                )

        // Reset the winning path
        winningPath.clear()

        // Check for a bridge between each pair of corners
        for (i in 0 until corners.size) {
            for (j in i + 1 until corners.size) {
                val corner1 = corners[i]
                val corner2 = corners[j]

                // Skip if either corner doesn't belong to the player
                if (board[corner1.first][corner1.second] != playerValue ||
                                board[corner2.first][corner2.second] != playerValue
                ) {
                    continue
                }

                // Create a visited array to track visited cells
                val visited = Array(boardSize) { BooleanArray(boardSize) }

                // Reset the temporary path
                val tempPath = mutableSetOf<Pair<Int, Int>>()

                // Check if there's a path between these two corners
                if (isConnected(
                                corner1.first,
                                corner1.second,
                                corner2.first,
                                corner2.second,
                                playerValue,
                                visited,
                                tempPath
                        )
                ) {
                    // We found a bridge
                    winningPath.addAll(tempPath)
                    havannahWinType = 2 // Bridge win
                    return true
                }
            }
        }

        return false
    }

    /**
     * Checks if a player has formed a fork in Havannah. A fork connects any three edges of the
     * hexagonal board.
     */
    private fun checkHavannahFork(playerValue: Int): Boolean {
        // Define the six edges in our board representation (excluding corners)
        val edges =
                listOf(
                        "top", // Top edge
                        "topright", // Top-right edge
                        "bottomright", // Bottom-right edge
                        "bottom", // Bottom edge
                        "bottomleft", // Bottom-left edge
                        "topleft" // Top-left edge
                )

        // Reset the winning path
        winningPath.clear()

        // First, check which edges the player has pieces on
        val connectedEdges = mutableSetOf<String>()

        // Check for top edge
        for (col in 1 until boardSize - 1) {
            if (board[0][col] == playerValue) {
                connectedEdges.add("top")
                break
            }
        }

        // Check for top-right edge
        for (row in 1 until boardSize / 2) {
            if (board[row][boardSize - 1] == playerValue) {
                connectedEdges.add("topright")
                break
            }
        }

        // Check for bottom-right edge
        for (row in boardSize / 2 + 1 until boardSize - 1) {
            if (board[row][boardSize - 1] == playerValue) {
                connectedEdges.add("bottomright")
                break
            }
        }

        // Check for bottom edge
        for (col in 1 until boardSize - 1) {
            if (board[boardSize - 1][col] == playerValue) {
                connectedEdges.add("bottom")
                break
            }
        }

        // Check for bottom-left edge
        for (row in boardSize / 2 + 1 until boardSize - 1) {
            if (board[row][0] == playerValue) {
                connectedEdges.add("bottomleft")
                break
            }
        }

        // Check for top-left edge
        for (row in 1 until boardSize / 2) {
            if (board[row][0] == playerValue) {
                connectedEdges.add("topleft")
                break
            }
        }

        // If the player has pieces on fewer than three edges, they can't have a fork
        if (connectedEdges.size < 3) {
            return false
        }

        // Check for all possible combinations of three edges
        for (i in 0 until edges.size) {
            for (j in i + 1 until edges.size) {
                for (k in j + 1 until edges.size) {
                    val edge1 = edges[i]
                    val edge2 = edges[j]
                    val edge3 = edges[k]

                    // Skip if the player doesn't have pieces on all three edges
                    if (!connectedEdges.contains(edge1) ||
                                    !connectedEdges.contains(edge2) ||
                                    !connectedEdges.contains(edge3)
                    ) {
                        continue
                    }

                    // Check if there's a common connection point for all three edges
                    if (areForkEdgesConnected(edge1, edge2, edge3, playerValue)) {
                        havannahWinType = 3 // Fork win
                        return true
                    }
                }
            }
        }

        return false
    }

    /** Checks if three edges are connected by the player's pieces. */
    private fun areForkEdgesConnected(
            edge1: String,
            edge2: String,
            edge3: String,
            playerValue: Int
    ): Boolean {
        // Find sample points on each edge
        val point1 = getSamplePointOnEdge(edge1, playerValue) ?: return false
        val point2 = getSamplePointOnEdge(edge2, playerValue) ?: return false
        val point3 = getSamplePointOnEdge(edge3, playerValue) ?: return false

        // Check if there's a path connecting all three points
        // Create a visited array to track visited cells
        val visited = Array(boardSize) { BooleanArray(boardSize) }

        // First check if points 1 and 2 are connected
        val path12 = mutableSetOf<Pair<Int, Int>>()
        if (!isConnected(
                        point1.first,
                        point1.second,
                        point2.first,
                        point2.second,
                        playerValue,
                        visited,
                        path12
                )
        ) {
            return false
        }

        // Reset visited array
        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                visited[r][c] = false
            }
        }

        // Now check if any point in the path from 1 to 2 connects to point 3
        val finalPath = mutableSetOf<Pair<Int, Int>>()
        for (point in path12) {
            // Reset visited again for each starting point
            for (r in 0 until boardSize) {
                for (c in 0 until boardSize) {
                    visited[r][c] = false
                }
            }

            val tempPath = mutableSetOf<Pair<Int, Int>>()
            if (isConnected(
                            point.first,
                            point.second,
                            point3.first,
                            point3.second,
                            playerValue,
                            visited,
                            tempPath
                    )
            ) {
                // We found a fork
                finalPath.addAll(path12)
                finalPath.addAll(tempPath)
                winningPath.addAll(finalPath)
                return true
            }
        }

        return false
    }

    /**
     * Gets a sample point on an edge where the player has a piece. (continued from previous
     * implementation)
     */
    private fun getSamplePointOnEdge(edge: String, playerValue: Int): Pair<Int, Int>? {
        when (edge) {
            "top" -> {
                for (col in 1 until boardSize - 1) {
                    if (board[0][col] == playerValue) {
                        return Pair(0, col)
                    }
                }
            }
            "topright" -> {
                for (row in 1 until boardSize / 2) {
                    if (board[row][boardSize - 1] == playerValue) {
                        return Pair(row, boardSize - 1)
                    }
                }
            }
            "bottomright" -> {
                for (row in boardSize / 2 + 1 until boardSize - 1) {
                    if (board[row][boardSize - 1] == playerValue) {
                        return Pair(row, boardSize - 1)
                    }
                }
            }
            "bottom" -> {
                for (col in 1 until boardSize - 1) {
                    if (board[boardSize - 1][col] == playerValue) {
                        return Pair(boardSize - 1, col)
                    }
                }
            }
            "bottomleft" -> {
                for (row in boardSize / 2 + 1 until boardSize - 1) {
                    if (board[row][0] == playerValue) {
                        return Pair(row, 0)
                    }
                }
            }
            "topleft" -> {
                for (row in 1 until boardSize / 2) {
                    if (board[row][0] == playerValue) {
                        return Pair(row, 0)
                    }
                }
            }
        }
        return null
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

        // Check all neighbors
        for ((dr, dc) in neighbors) {
            val newRow = startRow + dr
            val newCol = startCol + dc

            // Check if the neighbor is valid, unvisited, and belongs to the player
            if (newRow in 0 until boardSize &&
                            newCol in 0 until boardSize &&
                            !visited[newRow][newCol] &&
                            board[newRow][newCol] == playerValue
            ) {

                // Recursively check this neighbor
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
