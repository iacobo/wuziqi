package com.iacobo.wuziqi.ui.board

import androidx.compose.runtime.Composable
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.data.GameType
import com.iacobo.wuziqi.viewmodel.Position

/**
 * A unified game board renderer that uses the BoardFactory to create the appropriate board
 * implementation based on the game type.
 */
@Composable
fun GameBoardRenderer(
        gameType: GameType,
        gameState: GameState,
        lastPlacedPosition: Position?,
        isDarkTheme: Boolean,
        isGameFrozen: Boolean,
        onMoveSelected: (Int, Int) -> Unit
) {
    // Use factory to create the appropriate board for this game type
    val board = BoardFactory.createBoard(gameType)

    // Render the board using its implementation
    board.Render(
            gameState = gameState,
            lastPlacedPosition = lastPlacedPosition,
            isDarkTheme = isDarkTheme,
            isGameFrozen = isGameFrozen,
            onMoveSelected = { row, col ->
                if (row < 0 && gameType == GameType.Connect4) {
                    // Special case for Connect4 which only needs column input
                    onMoveSelected(-1, col)
                } else {
                    onMoveSelected(row, col)
                }
            }
    )
}
