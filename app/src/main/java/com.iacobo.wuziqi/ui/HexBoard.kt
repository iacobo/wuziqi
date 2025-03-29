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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.ui.theme.HexPieceBlue
import com.iacobo.wuziqi.ui.theme.HexPieceRed
import com.iacobo.wuziqi.viewmodel.Position
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Hex game board implementation with proper edge coloring and centered layout. */
@Composable
fun HexBoard(
        gameState: GameState,
        lastPlacedPosition: Position?,
        isGameFrozen: Boolean,
        onTileClick: (Int, Int) -> Unit
) {
        // Extract boardSize to make it accessible to lambdas
        val boardSize = gameState.boardSize

        // Define custom colors for better visibility
        val playerOneColor = HexPieceRed
        val playerTwoColor = HexPieceBlue

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

                                                        // FIX 1: Calculate the actual width needed
                                                        // for the parallelogram shape
                                                        // Previous calculation was incorrect,
                                                        // causing extra space on the right
                                                        // We need space for the board + the shift
                                                        // from the lower rows
                                                        val maxShiftWidth = boardSize * 0.5f
                                                        val widthNeeded = boardSize + maxShiftWidth

                                                        // Scale to ensure the board fits within the
                                                        // canvas width
                                                        val hexRadius =
                                                                minOf(
                                                                        canvasWidth /
                                                                                (widthNeeded *
                                                                                        sqrt(3f)),
                                                                        canvasHeight /
                                                                                ((boardSize +
                                                                                        0.5f) *
                                                                                        1.5f)
                                                                ) * 0.98f // Increased from
                                                        // 0.95f to 0.98f to
                                                        // fill more space

                                                        val hexHeight = hexRadius * 2
                                                        val hexWidth = hexRadius * sqrt(3f)

                                                        // Calculate offset to center the
                                                        // parallelogram
                                                        val totalWidth =
                                                                hexWidth * boardSize +
                                                                        (hexWidth * maxShiftWidth)
                                                        val totalHeight =
                                                                hexHeight * boardSize * 0.75f +
                                                                        hexHeight / 4
                                                        val xOffset = (canvasWidth - totalWidth) / 2
                                                        val yOffset =
                                                                (canvasHeight - totalHeight) / 2

                                                        for (row in 0 until boardSize) {
                                                                for (col in 0 until boardSize) {
                                                                        // Correct offset pattern
                                                                        // for each row to form a
                                                                        // parallelogram
                                                                        val rowOffset =
                                                                                row * (hexWidth / 2)
                                                                        val centerX =
                                                                                xOffset +
                                                                                        col *
                                                                                                hexWidth +
                                                                                        rowOffset
                                                                        val centerY =
                                                                                yOffset +
                                                                                        row *
                                                                                                hexHeight *
                                                                                                0.75f +
                                                                                        hexHeight /
                                                                                                2

                                                                        val distance =
                                                                                sqrt(
                                                                                        (tapOffset
                                                                                                        .x -
                                                                                                        centerX)
                                                                                                .pow(
                                                                                                        2
                                                                                                ) +
                                                                                                (tapOffset
                                                                                                                .y -
                                                                                                                centerY)
                                                                                                        .pow(
                                                                                                                2
                                                                                                        )
                                                                                )

                                                                        if (distance <
                                                                                        hexRadius *
                                                                                                0.9f &&
                                                                                        gameState
                                                                                                .board[
                                                                                                row][
                                                                                                col] ==
                                                                                                GameState
                                                                                                        .EMPTY
                                                                        ) {
                                                                                onTileClick(
                                                                                        row,
                                                                                        col
                                                                                )
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

                        // FIX 1: Improved calculation of the actual width needed
                        val maxShiftWidth = (boardSize - 1) * 0.5f
                        val widthNeeded = boardSize + maxShiftWidth

                        // Scale to ensure the board fits within the canvas width
                        val hexRadius =
                                minOf(
                                        canvasWidth / (widthNeeded * sqrt(3f)),
                                        canvasHeight / ((boardSize + 0.5f) * 1.5f)
                                ) * 0.98f // Increased from 0.95f to fill more space

                        val hexHeight = hexRadius * 2
                        val hexWidth = hexRadius * sqrt(3f)

                        // Calculate offset to center the parallelogram
                        val totalWidth = hexWidth * boardSize + (hexWidth * maxShiftWidth)
                        val totalHeight = hexHeight * boardSize * 0.75f + hexHeight / 4
                        val xOffset = (canvasWidth - totalWidth) / 2
                        val yOffset = (canvasHeight - totalHeight) / 2

                        // Draw all hexagons
                        for (row in 0 until boardSize) {
                                for (col in 0 until boardSize) {
                                        // Correct offset pattern for each row to form a
                                        // parallelogram
                                        val rowOffset = row * (hexWidth / 2)
                                        val centerX = xOffset + col * hexWidth + rowOffset
                                        val centerY =
                                                yOffset + row * hexHeight * 0.75f + hexHeight / 2

                                        // Store vertices for border edge coloring
                                        val hexVertices =
                                                List(6) { idx ->
                                                        val angle =
                                                                Math.toRadians(
                                                                                (60 * idx + 30)
                                                                                        .toDouble()
                                                                        )
                                                                        .toFloat()
                                                        Pair(
                                                                centerX + hexRadius * cos(angle),
                                                                centerY + hexRadius * sin(angle)
                                                        )
                                                }

                                        val hexPath =
                                                Path().apply {
                                                        moveTo(
                                                                hexVertices[0].first,
                                                                hexVertices[0].second
                                                        )
                                                        for (i in 1 until 6) {
                                                                lineTo(
                                                                        hexVertices[i].first,
                                                                        hexVertices[i].second
                                                                )
                                                        }
                                                        close()
                                                }

                                        // Alternate fill colors
                                        val fillColor =
                                                if ((row + col) % 2 == 0) hexColor1 else hexColor2
                                        drawPath(hexPath, color = fillColor, style = Fill)

                                        // Draw standard grid lines for interior hexagons
                                        val isTopRow = row == 0
                                        val isBottomRow = row == boardSize - 1
                                        val isLeftCol = col == 0
                                        val isRightCol = col == boardSize - 1

                                        // For interior hexagons or non-edge sides, use standard
                                        // grid color
                                        if (!isTopRow && !isBottomRow && !isLeftCol && !isRightCol
                                        ) {
                                                drawPath(
                                                        hexPath,
                                                        color = gridLineColor,
                                                        style = Stroke(width = strokeWidth)
                                                )
                                        } else {
                                                // For edge hexagons, color each edge appropriately
                                                // FIX 2 & 3: Corrected edge coloring logic
                                                for (i in 0 until 6) {
                                                        val nextIdx = (i + 1) % 6
                                                        val startX = hexVertices[i].first
                                                        val startY = hexVertices[i].second
                                                        val endX = hexVertices[nextIdx].first
                                                        val endY = hexVertices[nextIdx].second

                                                        val isRightEdge =
                                                                isRightCol && (i == 4 || i == 5)
                                                        val isLeftEdge =
                                                                isLeftCol && (i == 1 || i == 2)

                                                        val isTopEdge =
                                                                isTopRow && (i == 3 || i == 4)
                                                        val isBottomEdge =
                                                                isBottomRow && (i == 0 || i == 1)

                                                        val edgeColor =
                                                                when {
                                                                        isTopEdge || isBottomEdge ->
                                                                                playerOneColor
                                                                        isLeftEdge || isRightEdge ->
                                                                                playerTwoColor
                                                                        else -> gridLineColor
                                                                }

                                                        val edgeWidth =
                                                                when {
                                                                        isTopEdge ||
                                                                                isBottomEdge ||
                                                                                isLeftEdge ||
                                                                                isRightEdge ->
                                                                                strokeWidth * 2.5f
                                                                        else -> strokeWidth
                                                                }

                                                        drawLine(
                                                                color = edgeColor,
                                                                start = Offset(startX, startY),
                                                                end = Offset(endX, endY),
                                                                strokeWidth = edgeWidth
                                                        )
                                                }
                                        }

                                        // Draw game pieces
                                        if (gameState.board[row][col] != GameState.EMPTY) {
                                                val pieceColor =
                                                        when (gameState.board[row][col]) {
                                                                GameState.PLAYER_ONE ->
                                                                        playerOneColor
                                                                else -> playerTwoColor
                                                        }

                                                // Draw piece with highlight
                                                drawCircle(
                                                        color = pieceColor,
                                                        radius = hexRadius * 0.42f,
                                                        center = Offset(centerX, centerY)
                                                )

                                                // Add a subtle highlight to the top-left of the
                                                // piece for 3D effect
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
                                                if (lastPlacedPosition?.row == row &&
                                                                lastPlacedPosition.col == col
                                                ) {
                                                        drawCircle(
                                                                color = tertiaryColor,
                                                                radius = hexRadius * 0.5f,
                                                                center = Offset(centerX, centerY),
                                                                style =
                                                                        Stroke(
                                                                                width =
                                                                                        strokeWidth *
                                                                                                1.5f
                                                                        )
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}
