package com.iacobo.wuziqi.data

/**
 * Represents the state of a Wuziqi game.
 * Manages the board, current player, and win condition checking.
 */
class GameState {
    companion object {
        const val EMPTY = 0
        const val PLAYER_ONE = 1 // Black
        const val PLAYER_TWO = 2 // White
        const val BOARD_SIZE = 15 // Standard wuziqi board size
        const val WIN_CONDITION = 5 // Number of consecutive pieces needed to win
    }

    var board: Array<IntArray> = Array(BOARD_SIZE) { IntArray(BOARD_SIZE) { EMPTY } }
    var currentPlayer: Int = PLAYER_ONE

    /**
     * Places a tile on the board and switches the current player.
     *
     * @param row The row position
     * @param col The column position
     * @return True if the tile was placed successfully, false otherwise
     */
    fun placeTile(row: Int, col: Int): Boolean {
        if (!isTileEmpty(row, col)) return false
        
        board[row][col] = currentPlayer
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
     * Resets the game state to initial values.
     */
    fun reset() {
        board = Array(BOARD_SIZE) { IntArray(BOARD_SIZE) { EMPTY } }
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
               checkDirection(row, col, 1, -1, playerValue)   // Diagonal /
    }

    /**
     * Checks for win condition in a specific direction.
     */
    private fun checkDirection(row: Int, col: Int, deltaRow: Int, deltaCol: Int, playerValue: Int): Boolean {
        var count = 1 // Start with 1 for the piece itself

        // Check in the positive direction
        count += countInDirection(row, col, deltaRow, deltaCol, playerValue)

        // Check in the negative direction
        count += countInDirection(row, col, -deltaRow, -deltaCol, playerValue)

        return count >= WIN_CONDITION
    }

    /**
     * Counts consecutive pieces in a specific direction.
     */
    private fun countInDirection(row: Int, col: Int, deltaRow: Int, deltaCol: Int, playerValue: Int): Int {
        var count = 0
        var r = row + deltaRow
        var c = col + deltaCol

        while (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE && board[r][c] == playerValue) {
            count++
            r += deltaRow
            c += deltaCol
        }

        return count
    }
}