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
    // Variables to store win information
    private var havannahWinType = 0

    /** Main check function that tests all win conditions */
    fun checkHavannahWin(playerValue: Int): Boolean {
        // Only for Havannah game
        if (boardSize != 10 || winCondition != 9) {
            return false
        }

        // Clear the winning path
        winningPath.clear()

        // Reset win type
        havannahWinType = 0

        // Check each win condition
        if (checkForRing(playerValue)) {
            havannahWinType = 1
            return true
        }

        if (checkForBridge(playerValue)) {
            havannahWinType = 2
            return true
        }

        if (checkForFork(playerValue)) {
            havannahWinType = 3
            return true
        }

        return false
    }

    fun getWinningType(): Int = havannahWinType

    /** Checks for a ring (closed loop that contains at least one cell) */
    private fun checkForRing(playerValue: Int): Boolean {
        val visited = Array(boardSize) { BooleanArray(boardSize) { false } }
        val tempPath = mutableSetOf<Pair<Int, Int>>()

        // Loop through all cells
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] == playerValue) {
                    // Reset variables for each starting point
                    for (r in 0 until boardSize) {
                        for (c in 0 until boardSize) {
                            visited[r][c] = false
                        }
                    }

                    tempPath.clear()

                    // Try to find a ring from this cell
                    if (findRing(row, col, row, col, playerValue, visited, tempPath, 0)) {
                        // We found a ring! Now check if it encloses any cells
                        if (tempPath.size >= 6 && ringEnclosesAnything(tempPath)) {
                            winningPath.addAll(tempPath)
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    /** Recursive DFS to find a ring */
    private fun findRing(
            startRow: Int,
            startCol: Int,
            currRow: Int,
            currCol: Int,
            playerValue: Int,
            visited: Array<BooleanArray>,
            path: MutableSet<Pair<Int, Int>>,
            depth: Int
    ): Boolean {
        // Add to path
        path.add(Pair(currRow, currCol))

        // Mark as visited
        visited[currRow][currCol] = true

        // Check all 6 neighbors
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
            val newRow = currRow + dr
            val newCol = currCol + dc

            // Skip invalid positions
            if (newRow < 0 || newRow >= boardSize || newCol < 0 || newCol >= boardSize) {
                continue
            }

            // If we found the starting point and we've moved at least 5 steps, we have a ring
            if (newRow == startRow && newCol == startCol && depth >= 5) {
                return true
            }

            // Only follow player's stones that haven't been visited
            if (board[newRow][newCol] == playerValue && !visited[newRow][newCol]) {
                if (findRing(
                                startRow,
                                startCol,
                                newRow,
                                newCol,
                                playerValue,
                                visited,
                                path,
                                depth + 1
                        )
                ) {
                    return true
                }
            }
        }

        // Backtrack
        path.remove(Pair(currRow, currCol))
        return false
    }

    /** Checks if a ring encloses any cells */
    private fun ringEnclosesAnything(ring: Set<Pair<Int, Int>>): Boolean {
        // Find a cell inside the ring
        val minRow = ring.minOf { it.first }
        val maxRow = ring.maxOf { it.first }
        val minCol = ring.minOf { it.second }
        val maxCol = ring.maxOf { it.second }

        // Find the center of the ring
        val centerRow = (minRow + maxRow) / 2
        val centerCol = (minCol + maxCol) / 2

        // Find a cell near the center that's not part of the ring
        var testRow = centerRow
        var testCol = centerCol

        // If the center is part of the ring, look for an adjacent cell
        if (Pair(testRow, testCol) in ring) {
            val directions =
                    arrayOf(
                            Pair(-1, 0),
                            Pair(-1, 1),
                            Pair(0, -1),
                            Pair(0, 1),
                            Pair(1, -1),
                            Pair(1, 0)
                    )

            var found = false
            for ((dr, dc) in directions) {
                val newRow = testRow + dr
                val newCol = testCol + dc

                if (newRow in minRow..maxRow &&
                                newCol in minCol..maxCol &&
                                Pair(newRow, newCol) !in ring
                ) {
                    testRow = newRow
                    testCol = newCol
                    found = true
                    break
                }
            }

            // If we couldn't find a non-ring cell, the ring doesn't enclose anything
            if (!found) {
                return false
            }
        }

        // Use flood fill to check if the test cell is enclosed
        val visited = Array(boardSize) { BooleanArray(boardSize) { false } }

        // If the flood fill can reach the edge, the cell is not enclosed
        return !canReachEdge(testRow, testCol, visited, ring)
    }

    /** Flood fill to check if a position can reach the edge of the board */
    private fun canReachEdge(
            row: Int,
            col: Int,
            visited: Array<BooleanArray>,
            blockedCells: Set<Pair<Int, Int>>
    ): Boolean {
        // Check if out of bounds or blocked
        if (row < 0 ||
                        row >= boardSize ||
                        col < 0 ||
                        col >= boardSize ||
                        visited[row][col] ||
                        Pair(row, col) in blockedCells
        ) {
            return false
        }

        // Check if we reached the edge
        if (row == 0 || row == boardSize - 1 || col == 0 || col == boardSize - 1) {
            return true
        }

        // Mark as visited
        visited[row][col] = true

        // Check all six directions
        val directions =
                arrayOf(Pair(-1, 0), Pair(-1, 1), Pair(0, -1), Pair(0, 1), Pair(1, -1), Pair(1, 0))

        for ((dr, dc) in directions) {
            if (canReachEdge(row + dr, col + dc, visited, blockedCells)) {
                return true
            }
        }

        return false
    }

    /** Checks for a bridge (connection between any two corners) */
    private fun checkForBridge(playerValue: Int): Boolean {
        // Define the 6 corners of the hexagon
        val corners =
                listOf(
                        Pair(0, 0), // top-left
                        Pair(0, boardSize - 1), // top-right
                        Pair(boardSize / 2, boardSize - 1), // right
                        Pair(boardSize - 1, boardSize / 2), // bottom-right
                        Pair(boardSize - 1, 0), // bottom-left
                        Pair(boardSize / 2, 0) // left
                )

        // Find which corners have the player's stones
        val occupiedCorners = corners.filter { (row, col) -> board[row][col] == playerValue }

        // Need at least 2 corners for a bridge
        if (occupiedCorners.size < 2) {
            return false
        }

        // Try all pairs of corners
        for (i in 0 until occupiedCorners.size - 1) {
            for (j in i + 1 until occupiedCorners.size) {
                val corner1 = occupiedCorners[i]
                val corner2 = occupiedCorners[j]

                val visited = Array(boardSize) { BooleanArray(boardSize) { false } }
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
                    winningPath.addAll(path)
                    return true
                }
            }
        }

        return false
    }

    /** Checks for a fork (connection between any three edges) */
    private fun checkForFork(playerValue: Int): Boolean {
        // Store pieces on each of the 6 edges (excluding corners)
        val edges = Array(6) { mutableListOf<Pair<Int, Int>>() }

        // Loop through the board to find edge pieces
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] != playerValue) {
                    continue
                }

                // Skip corners
                if ((row == 0 && col == 0) ||
                                (row == 0 && col == boardSize - 1) ||
                                (row == boardSize / 2 && col == boardSize - 1) ||
                                (row == boardSize - 1 && col == boardSize / 2) ||
                                (row == boardSize - 1 && col == 0) ||
                                (row == boardSize / 2 && col == 0)
                ) {
                    continue
                }

                // Check which edge this is on
                if (row == 0) {
                    edges[0].add(Pair(row, col)) // top edge
                } else if (col == boardSize - 1 && row < boardSize / 2) {
                    edges[1].add(Pair(row, col)) // top-right edge
                } else if (col == boardSize - 1 && row > boardSize / 2) {
                    edges[2].add(Pair(row, col)) // bottom-right edge
                } else if (row == boardSize - 1) {
                    edges[3].add(Pair(row, col)) // bottom edge
                } else if (col == 0 && row > boardSize / 2) {
                    edges[4].add(Pair(row, col)) // bottom-left edge
                } else if (col == 0 && row < boardSize / 2) {
                    edges[5].add(Pair(row, col)) // top-left edge
                }
            }
        }

        // Count edges with player's pieces
        val occupiedEdges = edges.filter { it.isNotEmpty() }

        // Need at least 3 edges for a fork
        if (occupiedEdges.size < 3) {
            return false
        }

        // Try all combinations of 3 edges
        for (i in 0 until occupiedEdges.size - 2) {
            for (j in i + 1 until occupiedEdges.size - 1) {
                for (k in j + 1 until occupiedEdges.size) {
                    // Find if these three edges are connected
                    if (checkEdgesConnected(
                                    occupiedEdges[i],
                                    occupiedEdges[j],
                                    occupiedEdges[k],
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

    /** Checks if three edges are connected by player's stones */
    private fun checkEdgesConnected(
            edge1: List<Pair<Int, Int>>,
            edge2: List<Pair<Int, Int>>,
            edge3: List<Pair<Int, Int>>,
            playerValue: Int
    ): Boolean {
        // First find a path from edge1 to edge2
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

                    // Now check if this path connects to edge3
                    for (pos3 in edge3) {
                        for (pos12 in path12) {
                            val visited3 = Array(boardSize) { BooleanArray(boardSize) { false } }
                            val path23 = mutableSetOf<Pair<Int, Int>>()

                            if (isConnected(
                                            pos12.first,
                                            pos12.second,
                                            pos3.first,
                                            pos3.second,
                                            playerValue,
                                            visited3,
                                            path23
                                    )
                            ) {
                                // Found a fork!
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

    /** Checks if two points are connected by player's stones */
    private fun isConnected(
            startRow: Int,
            startCol: Int,
            endRow: Int,
            endCol: Int,
            playerValue: Int,
            visited: Array<BooleanArray>,
            path: MutableSet<Pair<Int, Int>>
    ): Boolean {
        // We found the destination
        if (startRow == endRow && startCol == endCol) {
            path.add(Pair(startRow, startCol))
            return true
        }

        // Mark as visited
        visited[startRow][startCol] = true
        path.add(Pair(startRow, startCol))

        // Check all 6 neighbors
        val directions =
                arrayOf(Pair(-1, 0), Pair(-1, 1), Pair(0, -1), Pair(0, 1), Pair(1, -1), Pair(1, 0))

        for ((dr, dc) in directions) {
            val newRow = startRow + dr
            val newCol = startCol + dc

            if (newRow in 0 until boardSize &&
                            newCol in 0 until boardSize &&
                            !visited[newRow][newCol] &&
                            board[newRow][newCol] == playerValue
            ) {

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
