package com.iacobo.wuziqi.ui.board

import com.iacobo.wuziqi.data.GameType

/** Factory for creating board implementations based on game type. */
object BoardFactory {
    /**
     * Creates the appropriate board implementation for the given game type.
     *
     * @param gameType The type of game being played
     * @return An implementation of the GameBoard interface for the specified game type
     */
    fun createBoard(gameType: GameType): GameBoard {
        return when (gameType) {
            GameType.Standard -> StandardBoard()
            GameType.TicTacToe -> TicTacToeBoard()
            GameType.Connect4 -> Connect4Board()
            GameType.Hex -> HexBoard()
            GameType.Havannah -> HavannahBoard()
        }
    }
}
