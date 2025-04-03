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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Implementation of a Havannah game board.
 *
 * A proper Havannah board is a hexagon with side length n (configurable, typically 10). It has 6
 * corners and 6 edges of length n (excluding corners).
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
        // Get the edge length from the board size
        // For Havannah, boardSize represents the edge length
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

        // Line width for the grid
        val density = LocalDensity.current
        val strokeWidth = with(density) { 1.5.dp.toPx() }

        // Cache for hex coordinates and cell mapping
        val hexCenters = remember { mutableMapOf<Pair<Int, Int>, Offset>() }
        var hexSize by remember { mutableStateOf(0f) }

        // Winning path for highlighting
        val winningPath = if (isGameFrozen) gameState.getWinningPath() else emptySet()

        Box(modifier = Modifier.aspectRatio(1f).padding(4.dp).background(backgroundColor)) {
            Canvas(
                    modifier =
                            Modifier.fillMaxSize().pointerInput(isGameFrozen) {
                                if (!isGameFrozen) {
                                    detectTapGestures { tap ->
                                        var bestDist = Float.MAX_VALUE
                                        var bestPos: Pair<Int, Int>? = null

                                        hexCenters.forEach { (pos, center) ->
                                            val dist =
                                                    sqrt(
                                                            (tap.x - center.x) *
                                                                    (tap.x - center.x) +
                                                                    (tap.y - center.y) *
                                                                            (tap.y - center.y)
                                                    )

                                            // Check if this position is within the array bounds and
                                            // empty
                                            val (row, col) = pos
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

                                        // If we found a close empty cell, select it
                                        bestPos?.let { (row, col) ->
                                            // Use a more generous hit radius
                                            if (bestDist < hexSize * 1.2f) {
                                                onMoveSelected(row, col)
                                            }
                                        }
                                    }
                                }
                            }
            ) {
                // Clear the centers cache on each render
                hexCenters.clear()

                // Calculate size with margin
                val margin = size.minDimension * 0.03f
                val usableWidth = size.width - (2 * margin)
                val usableHeight = size.height - (2 * margin)

                // Calculate board dimensions - a hexagon with edge length 'edgeLength'
                // has width and height of 2*edgeLength-1 in hexagonal coordinates
                val boardDiameter = 2 * edgeLength - 1

                // Calculate hex size to fit the entire board
                val hexWidth = usableWidth / boardDiameter
                val hexHeight = usableHeight / boardDiameter
                hexSize = minOf(hexWidth, hexHeight / sqrt(3f) * 2f) * 0.95f

                // Center of the canvas
                val centerX = size.width / 2
                val centerY = size.height / 2

                // Draw the board using cube coordinates (q,r,s where q+r+s=0)
                // For a hexagon with edge length N, the range is -N+1 to N-1
                val range = edgeLength - 1

                // Track corners and edges for special coloring
                val corners = mutableSetOf<Pair<Int, Int>>()
                val edges = mutableSetOf<Pair<Int, Int>>()

                // Define the 6 corners in cube coordinates
                corners.add(Pair(-range, range)) // top-left
                corners.add(Pair(0, range)) // top
                corners.add(Pair(range, 0)) // top-right
                corners.add(Pair(range, -range)) // bottom-right
                corners.add(Pair(0, -range)) // bottom
                corners.add(Pair(-range, 0)) // bottom-left

                // Pre-calculate edge positions (non-corner cells along the outer boundary)
                for (q in -range..range) {
                    for (r in -range..range) {
                        val s = -q - r
                        if (kotlin.math.abs(q) == range ||
                                        kotlin.math.abs(r) == range ||
                                        kotlin.math.abs(s) == range
                        ) {
                            val pos = Pair(q, r)
                            if (pos !in corners) {
                                edges.add(pos)
                            }
                        }
                    }
                }

                // Draw all hexagons in the grid
                for (q in -range..range) {
                    // r range depends on q to form a hexagon
                    val rMin = maxOf(-range, -q - range)
                    val rMax = minOf(range, -q + range)

                    for (r in rMin..rMax) {
                        val s = -q - r // Cube coordinate constraint: q + r + s = 0

                        // Convert cube coordinates to array indices consistently
                        val arrayRow = r + range
                        val arrayCol = q + range

                        // Calculate pixel position for hex center
                        val x = centerX + (q * 1.5f * hexSize)
                        val y = centerY + (r + q / 2f) * hexSize * sqrt(3f)

                        // Store all valid positions in the hexCenters map
                        // This is critically important for hit testing
                        hexCenters[Pair(arrayRow, arrayCol)] = Offset(x, y)

                        // Create hexagon path
                        val hexPath =
                                Path().apply {
                                    for (i in 0 until 6) {
                                        val angle = Math.toRadians((60 * i).toDouble()).toFloat()
                                        val hx = x + hexSize * cos(angle)
                                        val hy = y + hexSize * sin(angle)

                                        if (i == 0) {
                                            moveTo(hx, hy)
                                        } else {
                                            lineTo(hx, hy)
                                        }
                                    }
                                    close()
                                }

                        // Determine if this is a corner, edge, or inner cell
                        val isCorner = Pair(q, r) in corners
                        val isEdge = Pair(q, r) in edges

                        // Select fill color based on position type
                        val fillColor =
                                when {
                                    isCorner -> cornerColor
                                    isEdge -> edgeColor
                                    else -> centerColor
                                }

                        // Draw hexagon fill and border
                        drawPath(path = hexPath, color = fillColor, style = Fill)
                        drawPath(
                                path = hexPath,
                                color = gridLineColor,
                                style = Stroke(width = strokeWidth)
                        )

                        // Draw game piece if present
                        if (arrayRow in 0 until gameState.boardSize &&
                                        arrayCol in 0 until gameState.boardSize &&
                                        gameState.board[arrayRow][arrayCol] != GameState.EMPTY
                        ) {

                            val pieceColor =
                                    if (gameState.board[arrayRow][arrayCol] == GameState.PLAYER_ONE)
                                            playerOneColor
                                    else playerTwoColor

                            // Draw piece
                            drawCircle(
                                    color = pieceColor,
                                    radius = hexSize * 0.4f,
                                    center = Offset(x, y)
                            )

                            // Add subtle highlight
                            drawCircle(
                                    color = pieceColor.copy(alpha = 0.7f),
                                    radius = hexSize * 0.3f,
                                    center = Offset(x - hexSize * 0.08f, y - hexSize * 0.08f)
                            )

                            // Highlight last placed piece
                            if (lastPlacedPosition?.row == arrayRow &&
                                            lastPlacedPosition.col == arrayCol &&
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

                // Highlight winning path
                if (winningPath.isNotEmpty()) {
                    val pathList = winningPath.toList()
                    val winType = gameState.getWinningType()

                    // Choose color based on win type
                    val winColor =
                            when (winType) {
                                1 -> Color(0xFF9C27B0) // Purple for ring
                                2 -> Color(0xFF2196F3) // Blue for bridge
                                3 -> Color(0xFF4CAF50) // Green for fork
                                else -> highlightColor
                            }

                    // Highlight all winning cells
                    for (pos in winningPath) {
                        val center = hexCenters[pos] ?: continue

                        // Draw a glow
                        drawCircle(
                                color = winColor,
                                radius = hexSize * 0.7f,
                                center = center,
                                alpha = 0.4f
                        )
                    }

                    // Draw connectors between cells in the path
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

                    // For rings, connect the first and last piece
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
