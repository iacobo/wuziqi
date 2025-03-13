package com.iacobo.wuziqi.data

class GameState {
    companion object {
        const val EMPTY = 0
        const val PLAYER_ONE = 1
        const val PLAYER_TWO = 2
        const val BOARD_SIZE = 15 // Standard wuziqi board size
    }

    var board: Array<IntArray> = Array(BOARD_SIZE) { IntArray(BOARD_SIZE) { EMPTY } }
    var currentPlayer: Int = PLAYER_ONE

    // Function to place a tile on the board
    fun placeTile(row: Int, col: Int) {
        if (isTileEmpty(row, col)) {
            board[row][col] = currentPlayer
            currentPlayer = if (currentPlayer == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        }
    }

    // Function to check if a tile is empty
    fun isTileEmpty(row: Int, col: Int): Boolean {
        return board[row][col] == EMPTY
    }

    // Function to reset the game state
    fun reset() {
        board = Array(BOARD_SIZE) { IntArray(BOARD_SIZE) { EMPTY } }
        currentPlayer = PLAYER_ONE
    }

    // Function to check for a win condition
    fun checkWin(row: Int, col: Int): Boolean {
        return checkDirection(row, col, 1, 0) || // Horizontal
               checkDirection(row, col, 0, 1) || // Vertical
               checkDirection(row, col, 1, 1) || // Diagonal \
               checkDirection(row, col, 1, -1)   // Diagonal /
    }

    // Function to check a specific direction for a win
    private fun checkDirection(row: Int, col: Int, deltaRow: Int, deltaCol: Int): Boolean {
        var count = 1

        // Check in the positive direction
        count += countInDirection(row, col, deltaRow, deltaCol)

        // Check in the negative direction
        count += countInDirection(row, col, -deltaRow, -deltaCol)

        return count >= 5 // Win if there are 5 in a row
    }

    // Helper function to count tiles in a specific direction
    private fun countInDirection(row: Int, col: Int, deltaRow: Int, deltaCol: Int): Int {
        var count = 0
        var r = row + deltaRow
        var c = col + deltaCol

        while (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE && board[r][c] == currentPlayer) {
            count++
            r += deltaRow
            c += deltaCol
        }

        return count
    }
}