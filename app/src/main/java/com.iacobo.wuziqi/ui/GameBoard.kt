package com.iacobo.wuziqi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.ui.theme.BoardDarkColor
import com.iacobo.wuziqi.ui.theme.BoardLightColor
import com.iacobo.wuziqi.ui.theme.GridDarkColor
import com.iacobo.wuziqi.ui.theme.GridLightColor
import com.iacobo.wuziqi.viewmodel.Position

/**
 * The standard Wuziqi game board composable. Displays the grid and pieces. FIXED: Properly aligns
 * gridlines with tile centers by using proper padding.
 */
@Composable
fun GameBoard(
        gameState: GameState,
        lastPlacedPosition: Position?,
        isDarkTheme: Boolean,
        isGameFrozen: Boolean,
        onTileClick: (Int, Int) -> Unit
) {
    // Use theme-appropriate colors for grid lines and board background
    val gridLineColor = if (isDarkTheme) GridDarkColor else GridLightColor
    val boardColor = if (isDarkTheme) BoardDarkColor else BoardLightColor
    val gridLineWidth = 1.dp
    val boardSize = gameState.boardSize

    // Adjust piece size based on board size
    val pieceSize =
            when {
                boardSize <= 10 -> 32.dp
                boardSize <= 13 -> 28.dp
                boardSize <= 15 -> 24.dp
                boardSize <= 17 -> 20.dp
                else -> 18.dp
            }

    Box(modifier = Modifier.aspectRatio(1f).padding(8.dp).background(boardColor)) {
        /*
         * IMPORTANT FIX: We need to center the grid lines on the intersections
         * where the pieces will be placed. For proper alignment, we need:
         * 1. A padding of exactly half a cell size on each edge
         * 2. A total of (boardSize - 1) grid lines that create boardSize intersections
         */

        // Draw grid lines on canvas
        val density = LocalDensity.current
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate cell size - the distance between grid lines
            val cellWidth = canvasWidth / boardSize
            val cellHeight = canvasHeight / boardSize

            // Start and end positions with half-cell padding
            val startX = cellWidth / 2
            val startY = cellHeight / 2
            val endX = canvasWidth - (cellWidth / 2)
            val endY = canvasHeight - (cellHeight / 2)

            // Convert DP to pixels for line width
            val strokeWidth = with(density) { gridLineWidth.toPx() }

            // Draw horizontal lines
            for (i in 0 until boardSize) {
                val y = startY + (i * cellHeight)
                drawLine(
                        color = gridLineColor,
                        start = Offset(startX, y),
                        end = Offset(endX, y),
                        strokeWidth = strokeWidth
                )
            }

            // Draw vertical lines
            for (i in 0 until boardSize) {
                val x = startX + (i * cellWidth)
                drawLine(
                        color = gridLineColor,
                        start = Offset(x, startY),
                        end = Offset(x, endY),
                        strokeWidth = strokeWidth
                )
            }
        }

        // Tiles and pieces
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0 until boardSize) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0 until boardSize) {
                        Tile(
                                state = gameState.board[row][col],
                                isLastPlaced =
                                        lastPlacedPosition?.row == row &&
                                                lastPlacedPosition.col == col,
                                pieceSize = pieceSize,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (!isGameFrozen) {
                                        onTileClick(row, col)
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

/** Standard tile representing a game piece. */
@Composable
fun Tile(
        state: Int,
        isLastPlaced: Boolean,
        pieceSize: androidx.compose.ui.unit.Dp = 24.dp,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
) {
    Box(
            modifier =
                    modifier.aspectRatio(1f)
                            .padding(1.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
    ) {
        if (state != GameState.EMPTY) {
            Box(
                    modifier =
                            Modifier.size(pieceSize)
                                    .clip(CircleShape)
                                    .background(
                                            when (state) {
                                                GameState.PLAYER_ONE -> Color.Black
                                                else -> Color.White
                                            }
                                    )
                                    .border(
                                            width = if (isLastPlaced) 2.dp else 0.dp,
                                            color =
                                                    if (isLastPlaced)
                                                            MaterialTheme.colorScheme.tertiary
                                                    else Color.Transparent,
                                            shape = CircleShape
                                    )
            )
        }
    }
}

/**
 * Tic-Tac-Toe tile composable that draws either X or O based on state. Extracted to a separate
 * function for better reusability and debugging.
 */
@Composable
fun TicTacToeTile(state: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
                            style = Stroke(width = strokeWidth)
                    )
                }
            }
        // GameState.EMPTY is handled by having no content
        }
    }
}

/**
 * Tic-Tac-Toe board implementation (3x3 Easter Egg). Now with transparent background and no edge
 * lines. MODIFIED: Using the extracted TicTacToeTile composable for better traceability
 */
@Composable
fun TicTacToeBoard(
        gameState: GameState,
        lastPlacedPosition: Position?,
        isDarkTheme: Boolean,
        isGameFrozen: Boolean,
        onTileClick: (Int, Int) -> Unit
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
                        // Use the extracted TicTacToeTile composable
                        TicTacToeTile(
                                state = gameState.board[row][col],
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (!isGameFrozen) {
                                        onTileClick(row, col)
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}
