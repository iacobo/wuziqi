package com.iacobo.wuziqi.ui.board

import androidx.compose.runtime.Composable
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.viewmodel.Position

/**
 * Interface for all game board renderers. This ensures consistent behavior across different game
 * types.
 */
interface GameBoard {
    /**
     * Renders the game board UI
     *
     * @param gameState Current state of the game
     * @param lastPlacedPosition Position of the last placed piece for highlighting
     * @param isDarkTheme Whether the app is in dark mode
     * @param isGameFrozen Whether the game is in a non-interactive state (game over or AI thinking)
     * @param onMoveSelected Callback for when a move is selected
     */
    @Composable
    fun Render(
            gameState: GameState,
            lastPlacedPosition: Position?,
            isDarkTheme: Boolean,
            isGameFrozen: Boolean,
            onMoveSelected: (Int, Int) -> Unit
    )
}
