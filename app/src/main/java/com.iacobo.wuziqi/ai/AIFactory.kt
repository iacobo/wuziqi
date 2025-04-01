package com.iacobo.wuziqi.ai

import com.iacobo.wuziqi.data.GameType
import java.util.Random

/** Factory for creating appropriate AI implementations based on game type. */
object AIFactory {
    private val random = Random()
    private val wuziqiAI by lazy { WuziqiAIEngine(random) }
    private val hexAI by lazy { HexAlphaBetaEngine(random) }

    /**
     * Returns the appropriate AI implementation for the given game type.
     *
     * @param gameType The type of game being played
     * @return An instance of the appropriate AI implementation
     */
    fun createAI(gameType: GameType): GameAI {
        return when (gameType) {
            GameType.Standard -> wuziqiAI
            GameType.TicTacToe -> wuziqiAI // Wuziqi AI works for TicTacToe
            GameType.Connect4 -> wuziqiAI // Wuziqi AI handles Connect4 special case
            GameType.Hex -> hexAI
        }
    }
}
