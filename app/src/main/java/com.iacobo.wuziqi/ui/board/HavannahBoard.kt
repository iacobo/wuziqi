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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.ui.theme.BridgeColor
import com.iacobo.wuziqi.ui.theme.ForkColor
import com.iacobo.wuziqi.ui.theme.HexPieceBlue
import com.iacobo.wuziqi.ui.theme.HexPieceRed
import com.iacobo.wuziqi.ui.theme.RingColor
import com.iacobo.wuziqi.viewmodel.Position
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Implementation of the Havannah game board with proper hexagonal grid display and interaction. */
class HavannahBoard : GameBoard {

    @Composable
    override fun Render(
            gameState: GameState,
            lastPlacedPosition: Position?,
            isDarkTheme: Boolean,
            isGameFrozen: Boolean,
            onMoveSelected: (Int, Int) -> Unit
    ) {
        val boardSize = gameState.boardSize

        // Detect orientation
        val configuration = LocalConfiguration.current
        val isLandscape =
                configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        // Adjust aspect ratio based on orientation
        // The hexagonal grid has different width/height ratio than a rectangle
        val hexAspectRatio =
                if (isLandscape) {
                    // In landscape mode, we need to ensure the board fits vertically
                    0.866f // sqrt(3)/2 - makes the hex grid taller than wide
                } else {
                    // In portrait mode, ensure the board fits horizontally
                    1.155f // 2/sqrt(3) - makes the hex grid wider than tall
                }

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

        // Store valid hex positions for rendering and interaction
        val validHexPositions = remember { mutableSetOf<Pair<Int, Int>>() }
        val hexCenters = remember { mutableMapOf<Pair<Int, Int>, Offset>() }

        // Track if we need to redraw the centers
        var shouldUpdateCenters by remember { mutableStateOf(true) }

        Box(
                modifier =
                        Modifier.aspectRatio(hexAspectRatio)
                                .padding(12.dp)
                                .background(backgroundColor)
        ) {
            Canvas(
                    modifier =
                            Modifier.fillMaxSize().pointerInput(isGameFrozen, shouldUpdateCenters) {
                                if (!isGameFrozen) {
                                    detectTapGestures { tap ->
                                        // Find the closest hexagon to the tap point
                                        var bestMatch: Pair<Int, Int>? = null
                                        var bestDistance = Float.MAX_VALUE

                                        for ((pos, center) in hexCenters) {
                                            // Calculate Euclidean distance from tap to center
                                            val dist =
                                                    sqrt(
                                                            (tap.x - center.x) *
                                                                    (tap.x - center.x) +
                                                                    (tap.y - center.y) *
                                                                            (tap.y - center.y)
                                                    )

                                            // Only consider positions that are valid and empty
                                            if (pos in validHexPositions &&
                                                            pos.first in 0 until boardSize &&
                                                            pos.second in 0 until boardSize &&
                                                            gameState.board[pos.first][
                                                                    pos.second] == GameState.EMPTY
                                            ) {
                                                if (dist < bestDistance) {
                                                    bestDistance = dist
                                                    bestMatch = pos
                                                }
                                            }
                                        }

                                        // Using a more generous click threshold - 12% of the
                                        // smaller dimension
                                        val clickThreshold = min(size.width, size.height) * 0.12f

                                        // If we found a valid match within reasonable distance
                                        bestMatch?.let { (row, col) ->
                                            if (bestDistance < clickThreshold) {
                                                onMoveSelected(row, col)
                                                // Force recalculation of centers on next draw
                                                shouldUpdateCenters = true
                                            }
                                        }
                                    }
                                }
                            }
            ) {
                // Need to recalculate centers if:
                // 1. First render
                // 2. After a move is made
                // 3. Size changes
                if (shouldUpdateCenters) {
                    hexCenters.clear()
                    validHexPositions.clear()
                    shouldUpdateCenters = false
                }

                // Calculate board dimensions for drawing
                val edgeLength = boardSize
                val maxRadius = edgeLength - 1
                val padding = size.width * 0.05f // 5% padding
                val availableWidth = size.width - 2 * padding
                val availableHeight = size.height - 2 * padding

                // Calculate hex size to fit within available space
                // The flat-topped hexagonal grid has special scaling requirements
                val hexRadiusX = availableWidth / (maxRadius * 2 + edgeLength) * 0.95f
                val hexRadiusY = availableHeight / (maxRadius * 2 + edgeLength) * 0.95f
                val hexSize = min(hexRadiusX, hexRadiusY)

                // Center position
                val centerX = size.width / 2
                val centerY = size.height / 2

                // Define corners of the hexagonal board
                val corners =
                        listOf(
                                Triple(-maxRadius, maxRadius, 0), // top-left
                                Triple(0, maxRadius, -maxRadius), // top
                                Triple(maxRadius, 0, -maxRadius), // top-right
                                Triple(maxRadius, -maxRadius, 0), // bottom-right
                                Triple(0, -maxRadius, maxRadius), // bottom
                                Triple(-maxRadius, 0, maxRadius) // bottom-left
                        )

                // Draw the hexagonal grid using axial coordinates
                for (q in -maxRadius..maxRadius) {
                    for (r in -maxRadius..maxRadius) {
                        val s = -q - r // Third coordinate for cube representation

                        // Skip hexes that are outside the valid hexagonal board
                        if (abs(q) + abs(r) + abs(s) > maxRadius * 2) continue

                        // Convert to array indices (for game state access)
                        val row = r + maxRadius
                        val col = q + maxRadius

                        // Save the valid position
                        validHexPositions.add(Pair(row, col))

                        // Calculate pixel position
                        val x = centerX + hexSize * 1.5f * q
                        val y = centerY + hexSize * sqrt(3f) * (r + q / 2f)

                        // Store center for hit testing
                        hexCenters[Pair(row, col)] = Offset(x, y)

                        // Create hexagon path
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

                        // Determine cell type (corner, edge, or center)
                        val isCorner = Triple(q, r, s) in corners
                        val isEdge =
                                (abs(q) == maxRadius ||
                                        abs(r) == maxRadius ||
                                        abs(s) == maxRadius) && !isCorner

                        // Fill color based on cell type
                        val fillColor =
                                when {
                                    isCorner -> cornerColor
                                    isEdge -> edgeColor
                                    else -> centerColor
                                }

                        // Draw hex cell
                        drawPath(hexPath, color = fillColor, style = Fill)
                        drawPath(
                                hexPath,
                                color = gridLineColor,
                                style = Stroke(width = strokeWidth)
                        )

                        // Draw game pieces if present
                        if (row in 0 until boardSize &&
                                        col in 0 until boardSize &&
                                        gameState.board[row][col] != GameState.EMPTY
                        ) {
                            val pieceColor =
                                    if (gameState.board[row][col] == GameState.PLAYER_ONE)
                                            playerOneColor
                                    else playerTwoColor

                            // Draw main piece
                            drawCircle(
                                    color = pieceColor,
                                    radius = hexSize * 0.4f,
                                    center = Offset(x, y)
                            )

                            // Draw highlight effect
                            drawCircle(
                                    color = pieceColor.copy(alpha = 0.7f),
                                    radius = hexSize * 0.3f,
                                    center = Offset(x - hexSize * 0.08f, y - hexSize * 0.08f)
                            )

                            // Highlight last placed piece if it's not part of a winning path
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

                // Draw winning path if game is over
                if (winningPath.isNotEmpty()) {
                    val winType = gameState.getWinningType()
                    val pathList = winningPath.toList()

                    // Color based on win type
                    val winColor =
                            when (winType) {
                                1 -> RingColor // Purple for ring
                                2 -> BridgeColor // Blue for bridge
                                3 -> ForkColor // Green for fork
                                else -> highlightColor
                            }

                    // Highlight winning cells
                    for (pos in winningPath) {
                        val center = hexCenters[pos] ?: continue
                        drawCircle(
                                color = winColor,
                                radius = hexSize * 0.6f,
                                center = center,
                                alpha = 0.4f
                        )
                    }

                    // Connect path with lines
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

                    // For ring wins, connect first and last cell
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
