package com.iacobo.wuziqi.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.ui.theme.GridDarkColor
import com.iacobo.wuziqi.ui.theme.GridLightColor
import com.iacobo.wuziqi.viewmodel.Position

/** Implementation of a Tic-Tac-Toe game board. */
class TicTacToeBoard : GameBoard {
    @Composable
    override fun Render(
            gameState: GameState,
            lastPlacedPosition: Position?,
            isDarkTheme: Boolean,
            isGameFrozen: Boolean,
            onMoveSelected: (Int, Int) -> Unit
    ) {
        val gridLineColor = if (isDarkTheme) GridDarkColor else GridLightColor
        val gridLineWidth = 4.dp // Thicker grid lines for tic-tac-toe

        Box(modifier = Modifier.aspectRatio(1f).padding(16.dp).background(Color.Transparent)) {
            // Draw just the internal grid lines, not the outer edges
            Box(modifier = Modifier.fillMaxSize()) {
                // Vertical inner lines (2)
                Box(
                        modifier =
                                Modifier.fillMaxHeight()
                                        .width(gridLineWidth)
                                        .align(Alignment.CenterStart)
                                        .offset(x = (LocalDensity.current.density * 33).dp)
                                        .background(gridLineColor)
                )

                Box(
                        modifier =
                                Modifier.fillMaxHeight()
                                        .width(gridLineWidth)
                                        .align(Alignment.CenterEnd)
                                        .offset(x = -(LocalDensity.current.density * 33).dp)
                                        .background(gridLineColor)
                )

                // Horizontal inner lines (2)
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(gridLineWidth)
                                        .align(Alignment.TopCenter)
                                        .offset(y = (LocalDensity.current.density * 33).dp)
                                        .background(gridLineColor)
                )

                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(gridLineWidth)
                                        .align(Alignment.BottomCenter)
                                        .offset(y = -(LocalDensity.current.density * 33).dp)
                                        .background(gridLineColor)
                )
            }

            // Cells and pieces
            Column(modifier = Modifier.fillMaxSize()) {
                // 3 rows
                for (row in 0 until 3) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // 3 columns
                        for (col in 0 until 3) {
                            TicTacToeTile(
                                    state = gameState.board[row][col],
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (!isGameFrozen) {
                                            onMoveSelected(row, col)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }

    /** Renders a single Tic-Tac-Toe tile (X or O). */
    @Composable
    private fun TicTacToeTile(state: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
        // Get color scheme values directly from MaterialTheme
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary

        Box(
                modifier = modifier.aspectRatio(1f).clickable(onClick = onClick),
                contentAlignment = Alignment.Center
        ) {
            when (state) {
                GameState.PLAYER_ONE -> {
                    // Draw X with fixed, highly visible colors
                    Canvas(modifier = Modifier.size(60.dp).padding(6.dp)) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val strokeWidth = 14f

                        // Use the primary color from MaterialTheme directly
                        drawLine(
                                color = primaryColor,
                                start = Offset(0f, 0f),
                                end = Offset(canvasWidth, canvasHeight),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                        )

                        drawLine(
                                color = primaryColor,
                                start = Offset(canvasWidth, 0f),
                                end = Offset(0f, canvasHeight),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                        )
                    }
                }
                GameState.PLAYER_TWO -> {
                    // Draw O with fixed, highly visible colors
                    Canvas(modifier = Modifier.size(60.dp).padding(6.dp)) {
                        val canvasWidth = size.width
                        val strokeWidth = 10f

                        // Use the secondary color from MaterialTheme directly
                        drawCircle(
                                color = secondaryColor,
                                radius = (canvasWidth / 2) - (strokeWidth / 2),
                                style =
                                        androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = strokeWidth
                                        )
                        )
                    }
                }
            // GameState.EMPTY is handled by having no content
            }
        }
    }
}
