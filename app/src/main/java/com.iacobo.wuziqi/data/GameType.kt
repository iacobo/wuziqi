package com.iacobo.wuziqi.data

import androidx.annotation.StringRes
import com.iacobo.wuziqi.R
import kotlin.math.abs

/** Represents the different game types supported by the application. */
sealed class GameType(@StringRes val titleResId: Int, val boardSize: Int, val winCondition: Int) {
    /** Standard Wuziqi (Gomoku) game with 15x15 board and 5-in-a-row win condition */
    object Standard : GameType(R.string.app_name, 15, 5)

    /** Tic-Tac-Toe game with 3x3 board and 3-in-a-row win condition */
    object TicTacToe : GameType(R.string.tictactoe_title, 3, 3)

    /** Connect4 game with 7x7 board and 4-in-a-row win condition */
    object Connect4 : GameType(R.string.connect4_title, 7, 4)

    /** Hex game with 11x11 board and 8-in-a-row win condition */
    object Hex : GameType(R.string.hex_title, 11, 8)

    /**
     * Havannah game with hexagonal board (edge length 10) Using 19x19 array to represent the board
     * (2*edgeLength - 1)
     */
    object Havannah : GameType(R.string.havannah_title, 19, 9)

    /**
     * Havannah game with smaller hexagonal board (edge length 8) for beginners Using 15x15 array to
     * represent the board (2*edgeLength - 1)
     */
    object HavannahSmall : GameType(R.string.havannah_title, 15, 9)

    /** Gets the edge length for hexagonal games, or the board size for other games */
    fun getEdgeLength(): Int {
        return when (this) {
            is Havannah -> 10
            is HavannahSmall -> 8
            else -> boardSize
        }
    }

    /**
     * Checks if a given position is valid for this game type. For Havannah, this involves checking
     * if the position is within the hexagon.
     */
    fun isValidHexPosition(row: Int, col: Int): Boolean {
        if (this !is Havannah && this !is HavannahSmall) {
            // For non-hex games, just do a simple bounds check
            return row in 0 until boardSize && col in 0 until boardSize
        }

        // For hex games, convert to hex coordinates and check if within the hexagon
        val edgeLength = getEdgeLength()
        val range = edgeLength - 1

        // Convert from array indices to hexagonal coordinates
        val center = boardSize / 2
        val q = col - center
        val r = row - center
        val s = -q - r

        // Check if the position is within the hexagonal board
        return abs(q) + abs(r) + abs(s) <= 2 * range
    }

    companion object {
        /** Determine the game type from a game state */
        fun fromGameState(gameState: GameState): GameType {
            return when {
                gameState.boardSize == 3 && gameState.winCondition == 3 -> TicTacToe
                gameState.boardSize == 7 && gameState.winCondition == 4 -> Connect4
                gameState.boardSize == 11 && gameState.winCondition == 8 -> Hex
                gameState.boardSize == 15 && gameState.winCondition == 9 -> HavannahSmall
                gameState.boardSize == 19 && gameState.winCondition == 9 -> Havannah
                else -> Standard
            }
        }
    }
}
