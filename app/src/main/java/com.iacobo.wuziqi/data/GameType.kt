package com.iacobo.wuziqi.data

import androidx.annotation.StringRes
import com.iacobo.wuziqi.R

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

    /** Havannah game with hexagonal board (size 10) */
    object Havannah :
            GameType(R.string.havannah_title, 10, 9) // Using 9 as a special code for Havannah rules

    /** Havannah game with smaller hexagonal board (size 8) for beginners */
    object HavannahSmall :
            GameType(R.string.havannah_title, 8, 9) // Using same title as regular Havannah

    companion object {
        /** Determine the game type from a game state */
        fun fromGameState(gameState: GameState): GameType {
            return when {
                gameState.boardSize == 3 && gameState.winCondition == 3 -> TicTacToe
                gameState.boardSize == 7 && gameState.winCondition == 4 -> Connect4
                gameState.boardSize == 11 && gameState.winCondition == 8 -> Hex
                gameState.boardSize == 8 && gameState.winCondition == 9 -> HavannahSmall
                gameState.boardSize == 10 && gameState.winCondition == 9 -> Havannah
                else -> Standard
            }
        }
    }
}
