package com.iacobo.wuziqi.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.iacobo.wuziqi.ui.theme.HexPieceBlue
import com.iacobo.wuziqi.ui.theme.HexPieceRed
import com.iacobo.wuziqi.viewmodel.Position
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class HavannahBoard : GameBoard {

    @Composable
    override fun Render(
            gameState: GameState,
            lastPlacedPosition: Position?,
            isDarkTheme: Boolean,
            isGameFrozen: Boolean,
            onMoveSelected: (Int, Int) -> Unit
    ) {
        val edgeLength = gameState.boardSize

        // Colors
        val backgroundColor = MaterialTheme.colorScheme.surface
        val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        val cornerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        val edgeColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        val centerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        val playerOneColor = HexPieceRed
        val playerTwoColor = HexPieceBlue
        val highlightColor = MaterialTheme.colorScheme.tertiary

        val density = LocalDensity.current
        val strokeWidth = with(density) { 1.5.dp.toPx() }

        val winningPath = if (isGameFrozen) gameState.getWinningPath() else emptySet()

        // Store hexagon centers for hit testing
        val hexCenters = remember { mutableMapOf<Pair<Int, Int>, Offset>() }

        Box(modifier = Modifier.aspectRatio(1f).padding(4.dp).background(backgroundColor)) {
            Canvas(
                    modifier =
                            Modifier.fillMaxSize().pointerInput(isGameFrozen) {
                                if (!isGameFrozen) {
                                    detectTapGestures { tap ->
                                        var bestDist = Float.MAX_VALUE
                                        var bestPos: Pair<Int, Int>? = null

                                        for ((pos, center) in hexCenters) {
                                            val (row, col) = pos
                                            val dist =
                                                    sqrt(
                                                            (tap.x - center.x) *
                                                                    (tap.x - center.x) +
                                                                    (tap.y - center.y) *
                                                                            (tap.y - center.y)
                                                    )

                                            if (row >= 0 &&
                                                            row < gameState.boardSize &&
                                                            col >= 0 &&
                                                            col < gameState.boardSize &&
                                                            gameState.board[row][col] ==
                                                                    GameState.EMPTY
                                            ) {

                                                if (dist < bestDist) {
                                                    bestDist = dist
                                                    bestPos = pos
                                                }
                                            }
                                        }

                                        bestPos?.let { (row, col) ->
                                            if (bestDist < min(size.width, size.height) * 0.05f) {
                                                onMoveSelected(row, col)
                                            }
                                        }
                                    }
                                }
                            }
            ) {
                hexCenters.clear()

                // Calculate board dimensions and scaling
                val boardDiameter = 2 * edgeLength - 1
                val padding = 20f
                val availableWidth = size.width - 2 * padding
                val availableHeight = size.height - 2 * padding

                // Calculate the size of each hexagon to fit within canvas
                // Key fix: Use correct scaling factors for flat-topped hexagons
                // Width factor is 0.75 * diameter (due to horizontal overlap)
                // Height factor is 0.866 * diameter (sqrt(3)/2 for vertical spacing)
                val hexSize =
                        min(
                                availableWidth / (boardDiameter * 0.75f),
                                availableHeight / (boardDiameter * 0.866f)
                        ) * 0.9f // Add 10% margin for safety

                // Center position
                val centerX = size.width / 2
                val centerY = size.height / 2

                // Coordinate system for hexagonal grid
                val range = edgeLength - 1

                // Define corners
                val corners =
                        setOf(
                                Pair(-range, range), // top-left
                                Pair(0, range), // top
                                Pair(range, 0), // top-right
                                Pair(range, -range), // bottom-right
                                Pair(0, -range), // bottom
                                Pair(-range, 0) // bottom-left
                        )

                // Draw hexagonal grid
                for (q in -range..range) {
                    val rMin = maxOf(-range, -q - range)
                    val rMax = minOf(range, -q + range)

                    for (r in rMin..rMax) {
                        val s = -q - r

                        // Convert cube coordinates to array indices
                        val row = r + range
                        val col = q + range

                        // Calculate center position in pixels
                        val x = centerX + hexSize * 1.5f * q
                        val y = centerY + hexSize * sqrt(3f) * (r + q / 2f)

                        // Store center for hit testing
                        hexCenters[Pair(row, col)] = Offset(x, y)

                        // Draw hexagon
                        val hexPath =
                                Path().apply {
                                    for (i in 0 until 6) {
                                        val angle = Math.PI / 3 * i
                                        val hx = x + hexSize * cos(angle.toFloat())
                                        val hy = y + hexSize * sin(angle.toFloat())

                                        if (i == 0) moveTo(hx, hy) else lineTo(hx, hy)
                                    }
                                    close()
                                }

                        // Determine cell type
                        val isCorner = Pair(q, r) in corners
                        val isEdge =
                                (abs(q) == range || abs(r) == range || abs(s) == range) && !isCorner

                        // Apply fill color
                        val fillColor =
                                when {
                                    isCorner -> cornerColor
                                    isEdge -> edgeColor
                                    else -> centerColor
                                }

                        drawPath(hexPath, color = fillColor, style = Fill)
                        drawPath(
                                hexPath,
                                color = gridLineColor,
                                style = Stroke(width = strokeWidth)
                        )

                        // Draw game pieces
                        if (row in 0 until gameState.boardSize &&
                                        col in 0 until gameState.boardSize &&
                                        gameState.board[row][col] != GameState.EMPTY
                        ) {

                            val pieceColor =
                                    if (gameState.board[row][col] == GameState.PLAYER_ONE)
                                            playerOneColor
                                    else playerTwoColor

                            // Draw piece
                            drawCircle(
                                    color = pieceColor,
                                    radius = hexSize * 0.4f,
                                    center = Offset(x, y)
                            )

                            // Draw highlight
                            drawCircle(
                                    color = pieceColor.copy(alpha = 0.7f),
                                    radius = hexSize * 0.3f,
                                    center = Offset(x - hexSize * 0.08f, y - hexSize * 0.08f)
                            )

                            // Highlight last placed piece
                            if (lastPlacedPosition?.row == row &&
                                            lastPlacedPosition.col == col &&
                                            winningPath.isEmpty()
                            ) {

                                drawCircle(
                                        color = highlightColor,
                                        radius = hexSize * 0.5f,
                                        center = Offset(x, y),
                                        style = Stroke(width = strokeWidth * 1.5f)
                                )
                            }
                        }
                    }
                }

                // Draw winning path
                if (winningPath.isNotEmpty()) {
                    val winType = gameState.getWinningType()
                    val pathList = winningPath.toList()

                    val winColor =
                            when (winType) {
                                1 -> Color(0xFF9C27B0) // Purple for ring
                                2 -> Color(0xFF2196F3) // Blue for bridge
                                3 -> Color(0xFF4CAF50) // Green for fork
                                else -> highlightColor
                            }

                    // Highlight cells
                    for (pos in winningPath) {
                        val center = hexCenters[pos] ?: continue
                        drawCircle(
                                color = winColor,
                                radius = hexSize * 0.6f,
                                center = center,
                                alpha = 0.4f
                        )
                    }

                    // Connect path
                    for (i in 0 until pathList.size - 1) {
                        val center1 = hexCenters[pathList[i]] ?: continue
                        val center2 = hexCenters[pathList[i + 1]] ?: continue

                        drawLine(
                                color = winColor,
                                start = center1,
                                end = center2,
                                strokeWidth = strokeWidth * 2.5f
                        )
                    }

                    // Connect first and last for rings
                    if (winType == 1 && pathList.size > 2) {
                        val firstCenter = hexCenters[pathList.first()]
                        val lastCenter = hexCenters[pathList.last()]

                        if (firstCenter != null && lastCenter != null) {
                            drawLine(
                                    color = winColor,
                                    start = firstCenter,
                                    end = lastCenter,
                                    strokeWidth = strokeWidth * 2.5f
                            )
                        }
                    }
                }
            }
        }
    }
}
