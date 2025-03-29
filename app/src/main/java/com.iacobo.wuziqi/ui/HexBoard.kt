package com.iacobo.wuziqi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.viewmodel.Position
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Hex game board implementation. */
@Composable
fun HexBoard(
        gameState: GameState,
        lastPlacedPosition: Position?,
        isGameFrozen: Boolean,
        onTileClick: (Int, Int) -> Unit
) {
    val boardSize = gameState.boardSize

    // Define custom colors for better visibility
    val playerOneColor = Color(0xFFB71C1C) // Muted red
    val playerTwoColor = Color(0xFF1565C0) // Muted blue

    // Extract all MaterialTheme colors here in the composable context
    val hexColor1 = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val hexColor2 = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val backgroundColor = MaterialTheme.colorScheme.surface

    val density = LocalDensity.current
    val strokeWidth = with(density) { 1.5.dp.toPx() }

    Box(modifier = Modifier.aspectRatio(1f).padding(16.dp).background(backgroundColor)) {
        Canvas(
                modifier =
                        Modifier.fillMaxSize().pointerInput(isGameFrozen) {
                            if (!isGameFrozen) {
                                detectTapGestures { tapOffset ->
                                    val canvasWidth = size.width
                                    val canvasHeight = size.height

                                    // Make hexagons larger so they touch
                                    val hexRadius =
                                            (minOf(canvasWidth, canvasHeight) / (boardSize * 1.8f))
                                    val hexHeight = hexRadius * 2
                                    val hexWidth = hexRadius * sqrt(3f)

                                    // Calculate offset to center the parallelogram
                                    val totalWidth = hexWidth * boardSize
                                    val totalHeight = hexHeight * 0.75f * (boardSize + 0.5f)
                                    val xOffset = (canvasWidth - totalWidth) / 2
                                    val yOffset = (canvasHeight - totalHeight) / 2

                                    for (row in 0 until boardSize) {
                                        for (col in 0 until boardSize) {
                                            // Correct offset pattern for each row to form a
                                            // parallelogram
                                            val rowOffset = row * (hexWidth / 2)
                                            val centerX = xOffset + col * hexWidth + rowOffset
                                            val centerY =
                                                    yOffset +
                                                            row * hexHeight * 0.75f +
                                                            hexHeight / 2

                                            val distance =
                                                    sqrt(
                                                            (tapOffset.x - centerX).pow(2) +
                                                                    (tapOffset.y - centerY).pow(2)
                                                    )

                                            if (distance < hexRadius * 0.9f &&
                                                            gameState.board[row][col] ==
                                                                    GameState.EMPTY
                                            ) {
                                                onTileClick(row, col)
                                                return@detectTapGestures
                                            }
                                        }
                                    }
                                }
                            }
                        }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Make hexagons larger so they touch
            val hexRadius = (minOf(canvasWidth, canvasHeight) / (boardSize * 1.8f))
            val hexHeight = hexRadius * 2
            val hexWidth = hexRadius * sqrt(3f)

            // Calculate offset to center the parallelogram
            val totalWidth = hexWidth * boardSize
            val totalHeight = hexHeight * 0.75f * (boardSize + 0.5f)
            val xOffset = (canvasWidth - totalWidth) / 2
            val yOffset = (canvasHeight - totalHeight) / 2

            // Draw player territory indicators
            // Player One (red) - connects top to bottom
            drawRect(
                    color = playerOneColor.copy(alpha = 0.1f),
                    topLeft = Offset(xOffset, yOffset),
                    size = androidx.compose.ui.geometry.Size(totalWidth, hexHeight * 0.75f)
            )

            drawRect(
                    color = playerOneColor.copy(alpha = 0.1f),
                    topLeft =
                            Offset(
                                    xOffset + 0.5f * hexWidth * (boardSize - 1),
                                    yOffset + totalHeight - hexHeight * 0.75f
                            ),
                    size = androidx.compose.ui.geometry.Size(totalWidth, hexHeight * 0.75f)
            )

            // Player Two (blue) - connects left to right
            drawRect(
                    color = playerTwoColor.copy(alpha = 0.1f),
                    topLeft = Offset(xOffset, yOffset),
                    size = androidx.compose.ui.geometry.Size(hexWidth, totalHeight)
            )

            drawRect(
                    color = playerTwoColor.copy(alpha = 0.1f),
                    topLeft = Offset(xOffset + totalWidth - hexWidth, yOffset),
                    size = androidx.compose.ui.geometry.Size(hexWidth, totalHeight)
            )

            // Draw all hexagons
            for (row in 0 until boardSize) {
                for (col in 0 until boardSize) {
                    // Correct offset pattern for each row to form a parallelogram
                    val rowOffset = row * (hexWidth / 2)
                    val centerX = xOffset + col * hexWidth + rowOffset
                    val centerY = yOffset + row * hexHeight * 0.75f + hexHeight / 2

                    val hexPath =
                            Path().apply {
                                val angles =
                                        List(6) {
                                            Math.toRadians((60 * it + 30).toDouble()).toFloat()
                                        }
                                moveTo(
                                        centerX + hexRadius * cos(angles[0]),
                                        centerY + hexRadius * sin(angles[0])
                                )
                                for (i in 1 until 6) {
                                    lineTo(
                                            centerX + hexRadius * cos(angles[i]),
                                            centerY + hexRadius * sin(angles[i])
                                    )
                                }
                                close()
                            }

                    // Alternate fill colors
                    val fillColor = if ((row + col) % 2 == 0) hexColor1 else hexColor2
                    drawPath(hexPath, color = fillColor, style = Fill)
                    drawPath(hexPath, color = gridLineColor, style = Stroke(width = strokeWidth))

                    // Draw game pieces
                    if (gameState.board[row][col] != GameState.EMPTY) {
                        val pieceColor =
                                when (gameState.board[row][col]) {
                                    GameState.PLAYER_ONE -> playerOneColor
                                    else -> playerTwoColor
                                }

                        // Draw piece with highlight
                        drawCircle(
                                color = pieceColor,
                                radius = hexRadius * 0.42f,
                                center = Offset(centerX, centerY)
                        )

                        // Add a subtle highlight to the top-left of the piece for 3D effect
                        drawCircle(
                                color = pieceColor.copy(alpha = 0.7f),
                                radius = hexRadius * 0.32f,
                                center =
                                        Offset(
                                                centerX - hexRadius * 0.1f,
                                                centerY - hexRadius * 0.1f
                                        )
                        )

                        // Highlight the last placed piece
                        if (lastPlacedPosition?.row == row && lastPlacedPosition.col == col) {
                            drawCircle(
                                    color = tertiaryColor,
                                    radius = hexRadius * 0.5f,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = strokeWidth * 1.5f)
                            )
                        }
                    }
                }
            }

            // Draw edge labels to indicate player goals
            val textSize = hexRadius * 0.6f

            // Player One (red) goals - connect top to bottom
            drawLine(
                    color = playerOneColor,
                    strokeWidth = hexRadius * 0.2f,
                    start = Offset(xOffset, yOffset),
                    end = Offset(xOffset + totalWidth, yOffset)
            )

            drawLine(
                    color = playerOneColor,
                    strokeWidth = hexRadius * 0.2f,
                    start =
                            Offset(
                                    xOffset + 0.5f * hexWidth * (boardSize - 1),
                                    yOffset + totalHeight
                            ),
                    end =
                            Offset(
                                    xOffset + 0.5f * hexWidth * (boardSize - 1) + totalWidth,
                                    yOffset + totalHeight
                            )
            )

            // Player Two (blue) goals - connect left to right
            drawLine(
                    color = playerTwoColor,
                    strokeWidth = hexRadius * 0.2f,
                    start = Offset(xOffset, yOffset),
                    end = Offset(xOffset, yOffset + totalHeight)
            )

            drawLine(
                    color = playerTwoColor,
                    strokeWidth = hexRadius * 0.2f,
                    start = Offset(xOffset + totalWidth, yOffset),
                    end = Offset(xOffset + totalWidth, yOffset + totalHeight)
            )
        }
    }
}
