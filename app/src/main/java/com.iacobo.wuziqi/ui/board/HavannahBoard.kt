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

/** Emergency fix for Havannah board. */
class HavannahBoard : GameBoard {

    @Composable
    override fun Render(
            gameState: GameState,
            lastPlacedPosition: Position?,
            isDarkTheme: Boolean,
            isGameFrozen: Boolean,
            onMoveSelected: (Int, Int) -> Unit
    ) {
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

        // Winning path highlight
        val winningPath = if (isGameFrozen) gameState.getWinningPath() else emptySet()

        Box(
                modifier =
                        Modifier.aspectRatio(1f)
                                .padding(8.dp) // Increase padding to ensure board fits
                                .background(backgroundColor)
        ) {
            Canvas(
                    modifier =
                            Modifier.fillMaxSize().pointerInput(isGameFrozen) {
                                if (!isGameFrozen) {
                                    detectTapGestures { tap ->
                                        // Will be populated with hex centers and their array
                                        // positions
                                        if (hexCenters.isNotEmpty()) {
                                            // Find closest hex
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
                                                if (dist < bestDist) {
                                                    // Make sure the position is valid within the
                                                    // game array bounds
                                                    val (row, col) = pos
                                                    if (row >= 0 &&
                                                                    row < gameState.boardSize &&
                                                                    col >= 0 &&
                                                                    col < gameState.boardSize &&
                                                                    gameState.board[row][col] ==
                                                                            GameState.EMPTY
                                                    ) {
                                                        bestDist = dist
                                                        bestPos = pos
                                                    }
                                                }
                                            }

                                            // If we found a close empty cell, select it
                                            bestPos?.let { (row, col) ->
                                                // Check if the tap is close enough (within cell
                                                // radius)
                                                if (bestDist < hexSize * 0.8f) {
                                                    onMoveSelected(row, col)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
            ) {
                // Clear previous data
                hexCenters.clear()

                // Calculate size based on canvas dimensions
                // Add larger margin to ensure it fits on phone screens
                val margin = size.minDimension * 0.12f
                val usableWidth = size.width - (2 * margin)
                val usableHeight = size.height - (2 * margin)

                // The actual board size used by the game logic
                val actualBoardSize = gameState.boardSize

                // For a hexagonal board, we need to fit a width of about 2 * actualBoardSize
                val hexWidth = usableWidth / (actualBoardSize * 1.7f)
                val hexHeight = usableHeight / (actualBoardSize * 1.7f)
                hexSize = minOf(hexWidth, hexHeight / sqrt(3f) * 2f) * 0.9f

                // Center of the canvas
                val centerX = size.width / 2
                val centerY = size.height / 2

                // Map from cubic coordinates to array indices
                // In a side-10 hexagon, the board array is 19x19 (but actual game array is only
                // 10x10)
                // We need to map the hexagon to the center of the 10x10 array

                // Draw the board using axial coordinates (q,r)
                // q is along the horizontal axis, r is along the diagonal axis
                val range = 4 // For a 10x10 board, the range is 4 in cubic coordinates

                for (q in -range..range) {
                    // r range depends on q to form a hexagon
                    val rMin = maxOf(-range, -q - range)
                    val rMax = minOf(range, -q + range)

                    for (r in rMin..rMax) {
                        val s = -q - r // Maintain q+r+s = 0 for cubic coordinates

                        // Convert to array coordinates for the game state
                        // Map to the center of the 10x10 board
                        val row = r + range
                        val col = q + range

                        // Skip if outside the valid game state array bounds
                        if (row < 0 || row >= actualBoardSize || col < 0 || col >= actualBoardSize
                        ) {
                            continue
                        }

                        // Calculate pixel position for hex center using axial coordinates
                        // For flat-topped hexagons
                        val x = centerX + hexSize * 1.5f * q
                        val y = centerY + hexSize * sqrt(3f) * (r + q / 2f)

                        // Store center position for hit testing
                        hexCenters[Pair(row, col)] = Offset(x, y)

                        // Create hexagon path
                        val hexPath = createHexagonPath(x, y, hexSize)

                        // Determine if this is a corner, edge, or inner cell
                        val isCorner =
                                ((q == -range && r == range) || // top-left
                                (q == 0 && r == range) || // top
                                        (q == range && r == 0) || // top-right
                                        (q == range && r == -range) || // bottom-right
                                        (q == 0 && r == -range) || // bottom
                                        (q == -range && r == 0) // bottom-left
                                )

                        // Edges have exactly one coordinate at ±range (excluding corners)
                        val isEdge =
                                !isCorner &&
                                        (kotlin.math.abs(q) == range ||
                                                kotlin.math.abs(r) == range ||
                                                kotlin.math.abs(s) == range)

                        // Select fill color based on position type
                        val fillColor =
                                when {
                                    isCorner -> cornerColor
                                    isEdge -> edgeColor
                                    else -> centerColor
                                }

                        // Draw hexagon fill
                        drawPath(hexPath, color = fillColor, style = Fill)

                        // Draw hexagon border
                        drawPath(
                                hexPath,
                                color = gridLineColor,
                                style = Stroke(width = strokeWidth)
                        )

                        // Draw game pieces if present
                        if (gameState.board[row][col] != GameState.EMPTY) {
                            val pieceColor =
                                    if (gameState.board[row][col] == GameState.PLAYER_ONE)
                                            playerOneColor
                                    else playerTwoColor

                            // Draw the piece
                            drawCircle(
                                    color = pieceColor,
                                    radius = hexSize * 0.7f,
                                    center = Offset(x, y)
                            )

                            // Highlight for last placed piece
                            if (lastPlacedPosition?.row == row &&
                                            lastPlacedPosition.col == col &&
                                            winningPath.isEmpty()
                            ) {

                                drawCircle(
                                        color = highlightColor,
                                        radius = hexSize * 0.85f,
                                        center = Offset(x, y),
                                        style = Stroke(width = strokeWidth * 1.5f)
                                )
                            }
                        }
                    }
                }

                // Highlight winning path if game is over
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
                                radius = hexSize * 0.9f,
                                center = center,
                                alpha = 0.4f
                        )
                    }

                    // Connect winning cells
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

                    // For rings, connect the first and last piece to complete the loop
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

    // Cache hex cell centers for hit testing
    private val hexCenters = mutableMapOf<Pair<Int, Int>, Offset>()

    // Hex cell size, stored for hit testing
    private var hexSize = 0f

    /** Creates a hexagon path centered at (x,y) with the given size. */
    private fun createHexagonPath(x: Float, y: Float, size: Float): Path {
        return Path().apply {
            // Flat-topped hexagon
            for (i in 0 until 6) {
                val angle = Math.toRadians((60 * i).toDouble()).toFloat()
                val hx = x + size * cos(angle)
                val hy = y + size * sin(angle)

                if (i == 0) {
                    moveTo(hx, hy)
                } else {
                    lineTo(hx, hy)
                }
            }
            close()
        }
    }
}
