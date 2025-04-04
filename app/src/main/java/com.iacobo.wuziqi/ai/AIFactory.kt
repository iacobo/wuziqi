package com.iacobo.wuziqi.ai

import com.iacobo.wuziqi.data.GameType
import java.util.Random

/** Factory for creating appropriate AI implementations based on game type. */
object AIFactory {
    private val random = Random()
    private val wuziqiAI by lazy { WuziqiAIEngine(random) }
    private val tictactoeAI by lazy { TicTacToeAIEngine(random) }
    private val connect4AI by lazy { Connect4AIEngine(random) }
    private val hexAI by lazy { HexAlphaBetaEngine(random) }
    private val havannahAI by lazy { HavannahAIEngine(random) }

    /**
     * Returns the appropriate AI implementation for the given game type.
     *
     * @param gameType The type of game being played
     * @return An instance of the appropriate AI implementation
     */
    fun createAI(gameType: GameType): GameAI {
        return when (gameType) {
            GameType.Standard -> wuziqiAI
            GameType.TicTacToe -> tictactoeAI
            GameType.Connect4 -> connect4AI
            GameType.Hex -> hexAI
            GameType.Havannah, GameType.HavannahSmall -> havannahAI
        }
    }
}
