package com.iacobo.wuziqi.ui.board

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
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

/**
 * Havannah board implementation with correct hexagonal layout and consistent coordinate mapping.
 */
class HavannahBoard : GameBoard {

    @Composable
    override fun Render(
            gameState: GameState,
            lastPlacedPosition: Position?,
            isDarkTheme: Boolean,
            isGameFrozen: Boolean,
            onMoveSelected: (Int, Int) -> Unit
    ) {
        // The edge length of the hexagonal board
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

        // Line width
        val density = LocalDensity.current
        val strokeWidth = with(density) { 1.5.dp.toPx() }

        // Winning path
        val winningPath = if (isGameFrozen) gameState.getWinningPath() else emptySet()

        // For tracking hexagon centers and positions
        val hexCenters = remember { mutableMapOf<Pair<Int, Int>, Offset>() }
        val hexSize = remember { mutableStateOf(0f) }

        Box(modifier = Modifier.aspectRatio(1f).padding(4.dp).background(backgroundColor)) {
            Canvas(
                    modifier =
                            Modifier.fillMaxSize().pointerInput(isGameFrozen) {
                                if (!isGameFrozen) {
                                    detectTapGestures { tap ->
                                        // Find closest hex center
                                        var bestDist = Float.MAX_VALUE
                                        var bestPos: Pair<Int, Int>? = null

                                        for ((pos, center) in hexCenters) {
                                            val (row, col) = pos

                                            // Calculate distance to center
                                            val dist =
                                                    sqrt(
                                                            (tap.x - center.x) *
                                                                    (tap.x - center.x) +
                                                                    (tap.y - center.y) *
                                                                            (tap.y - center.y)
                                                    )

                                            // Check if position is valid and empty
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

                                        // Use the closest valid position if close enough
                                        bestPos?.let { (row, col) ->
                                            // Fixed: use the minimum of width and height instead of
                                            // minDimension
                                            if (bestDist < hexSize.value * 1.2f) {
                                                onMoveSelected(row, col)
                                            }
                                        }
                                    }
                                }
                            }
            ) {
                // Clear previous mapping
                hexCenters.clear()

                // Calculate available drawing area with margin
                val margin = min(size.width, size.height) * 0.03f
                val availableWidth = size.width - 2 * margin
                val availableHeight = size.height - 2 * margin

                // For a hexagonal board with edge length N, the logical width
                // is 2*N-1 hexagons at the widest point
                val boardDiameter = 2 * edgeLength - 1

                // Calculate the base hexagon size to fit the entire board
                val baseSize =
                        min(
                                availableWidth /
                                        (boardDiameter *
                                                0.75f), // Horizontal spacing factor for flat-top
                                availableHeight /
                                        (boardDiameter *
                                                0.866f) // Vertical spacing factor (sqrt(3)/2)
                        ) * 0.95f // Add safety margin

                // Store the hex size for hit testing
                hexSize.value = baseSize

                // Center of the canvas
                val centerX = size.width / 2
                val centerY = size.height / 2

                // Initialize the axial coordinate system
                val range = edgeLength - 1

                // Identify corners for special coloring
                val corners = mutableSetOf<Pair<Int, Int>>()

                // Define the 6 corners in axial coordinates (q,r)
                corners.add(Pair(-range, range)) // top-left
                corners.add(Pair(0, range)) // top
                corners.add(Pair(range, 0)) // top-right
                corners.add(Pair(range, -range)) // bottom-right
                corners.add(Pair(0, -range)) // bottom
                corners.add(Pair(-range, 0)) // bottom-left

                // Debug
                Log.d(
                        "HavannahBoard",
                        "Drawing board with edge length: $edgeLength, hex size: ${hexSize.value}"
                )

                // Render the hexagonal grid
                for (q in -range..range) {
                    // r range depends on q to maintain the hexagonal shape
                    val rMin = maxOf(-range, -q - range)
                    val rMax = minOf(range, -q + range)

                    for (r in rMin..rMax) {
                        // Cube coordinate constraint: q + r + s = 0, so s = -q-r
                        val s = -q - r

                        // Skip if outside hexagonal board (redundant check but safer)
                        if (abs(q) + abs(r) + abs(s) > 2 * range) continue

                        // Convert axial coordinates to array indices
                        val row = r + range
                        val col = q + range

                        // Calculate pixel coordinates for this hexagon's center
                        // For flat-topped hexagons:
                        val x = centerX + hexSize.value * 1.5f * q
                        val y = centerY + hexSize.value * sqrt(3f) * (r + q / 2f)

                        // Store the center position for hit testing
                        hexCenters[Pair(row, col)] = Offset(x, y)

                        // Log key hexagons for debugging
                        if (q == 0 && r == 0) {
                            Log.d(
                                    "HavannahBoard",
                                    "Center hex at ($x, $y), array pos: ($row, $col)"
                            )
                        }

                        // Create the hexagon path
                        val hexPath =
                                Path().apply {
                                    for (i in 0 until 6) {
                                        val angle = Math.PI / 3 * i
                                        val hx = x + hexSize.value * cos(angle.toFloat())
                                        val hy = y + hexSize.value * sin(angle.toFloat())

                                        if (i == 0) {
                                            moveTo(hx, hy)
                                        } else {
                                            lineTo(hx, hy)
                                        }
                                    }
                                    close()
                                }

                        // Determine hexagon type
                        val isCorner = Pair(q, r) in corners
                        val isEdge =
                                (abs(q) == range || abs(r) == range || abs(s) == range) && !isCorner

                        // Fill color based on position type
                        val fillColor =
                                when {
                                    isCorner -> cornerColor
                                    isEdge -> edgeColor
                                    else -> centerColor
                                }

                        // Draw hexagon
                        drawPath(hexPath, color = fillColor, style = Fill)
                        drawPath(
                                hexPath,
                                color = gridLineColor,
                                style = Stroke(width = strokeWidth)
                        )

                        // Draw game piece if present
                        if (row in 0 until gameState.boardSize &&
                                        col in 0 until gameState.boardSize &&
                                        gameState.board[row][col] != GameState.EMPTY
                        ) {

                            val pieceColor =
                                    if (gameState.board[row][col] == GameState.PLAYER_ONE)
                                            playerOneColor
                                    else playerTwoColor

                            // Draw piece with 3D effect
                            drawCircle(
                                    color = pieceColor,
                                    radius = hexSize.value * 0.4f,
                                    center = Offset(x, y)
                            )

                            // Highlight
                            drawCircle(
                                    color = pieceColor.copy(alpha = 0.7f),
                                    radius = hexSize.value * 0.3f,
                                    center =
                                            Offset(
                                                    x - hexSize.value * 0.08f,
                                                    y - hexSize.value * 0.08f
                                            )
                            )

                            // Highlight last placed piece
                            if (lastPlacedPosition?.row == row &&
                                            lastPlacedPosition.col == col &&
                                            winningPath.isEmpty()
                            ) {

                                drawCircle(
                                        color = highlightColor,
                                        radius = hexSize.value * 0.5f,
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

                    // Color based on win type
                    val winColor =
                            when (winType) {
                                1 -> Color(0xFF9C27B0) // Purple for ring
                                2 -> Color(0xFF2196F3) // Blue for bridge
                                3 -> Color(0xFF4CAF50) // Green for fork
                                else -> highlightColor
                            }

                    // Highlight winning cells
                    for (pos in winningPath) {
                        val center = hexCenters[pos] ?: continue

                        drawCircle(
                                color = winColor,
                                radius = hexSize.value * 0.6f,
                                center = center,
                                alpha = 0.4f
                        )
                    }

                    // Connect winning path
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

                    // For rings, connect first and last
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
