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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.ui.theme.HexPieceBlue
import com.iacobo.wuziqi.ui.theme.HexPieceRed
import com.iacobo.wuziqi.viewmodel.Position
import kotlin.math.cos
import kotlin.math.max
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
        val hexAspectRatio =
                if (isLandscape) {
                    0.866f // sqrt(3)/2
                } else {
                    1.155f // 2/sqrt(3)
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

        // Store hexagon centers for click detection
        val hexCenters = remember { mutableMapOf<Pair<Int, Int>, Offset>() }

        Box(
                modifier =
                        Modifier.aspectRatio(hexAspectRatio)
                                .padding(12.dp)
                                .background(backgroundColor)
        ) {
            Canvas(
                    modifier =
                            Modifier.fillMaxSize().pointerInput(isGameFrozen) {
                                if (!isGameFrozen) {
                                    detectTapGestures { tap ->
                                        // Find the closest hexagon to the tap point
                                        var bestMatch: Pair<Int, Int>? = null
                                        var bestDistance = Float.MAX_VALUE

                                        for ((pos, center) in hexCenters) {
                                            val (row, col) = pos

                                            // Calculate distance from tap to center
                                            val dist =
                                                    sqrt(
                                                            (tap.x - center.x) *
                                                                    (tap.x - center.x) +
                                                                    (tap.y - center.y) *
                                                                            (tap.y - center.y)
                                                    )

                                            // Check if this cell is valid and empty
                                            if (row in 0 until boardSize &&
                                                            col in 0 until boardSize &&
                                                            gameState.board[row][col] ==
                                                                    GameState.EMPTY
                                            ) {

                                                if (dist < bestDistance) {
                                                    bestDistance = dist
                                                    bestMatch = pos
                                                }
                                            }
                                        }

                                        // More generous click threshold - 15% of the smaller
                                        // dimension
                                        val clickThreshold = min(size.width, size.height) * 0.15f

                                        // If we found a valid match within reasonable distance
                                        bestMatch?.let { (row, col) ->
                                            if (bestDistance < clickThreshold) {
                                                onMoveSelected(row, col)
                                            }
                                        }
                                    }
                                }
                            }
            ) {
                // Clear the centers map at the start of each drawing
                hexCenters.clear()

                // Calculate board dimensions
                val padding = size.width * 0.05f // 5% padding
                val availableWidth = size.width - 2 * padding
                val availableHeight = size.height - 2 * padding

                // Calculate hex size to fit within available space
                val hexSize =
                        min(availableWidth / (boardSize * 2f), availableHeight / (boardSize * 2f))

                // Center position
                val centerX = size.width / 2
                val centerY = size.height / 2

                // Maximum distance from center to corner (= boardSize - 1)
                val range = boardSize - 1

                // Define corners
                val corners =
                        setOf(
                                Pair(-range, 0), // west
                                Pair(-range / 2, -range), // northwest
                                Pair(range / 2, -range), // northeast
                                Pair(range, 0), // east
                                Pair(range / 2, range), // southeast
                                Pair(-range / 2, range) // southwest
                        )

                // THIS IS THE KEY: Use the EXACT same hexagon constraint as the original working
                // code
                // Iterate through q and compute valid r range for each q
                for (q in -range..range) {
                    // For each q, compute the valid range of r values that creates a perfect
                    // hexagon
                    val rMin = max(-range, -q - range)
                    val rMax = min(range, -q + range)

                    for (r in rMin..rMax) {
                        val s = -q - r

                        // Convert to array indices for game state access
                        val row = r + range
                        val col = q + range

                        // Skip positions outside array bounds
                        if (row !in 0 until boardSize || col !in 0 until boardSize) continue

                        // Calculate pixel position for this hexagon
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

                        // Determine if this is a corner or edge
                        val isCorner = Pair(q, r) in corners

                        // Is it an edge but not a corner?
                        val isEdge =
                                (q == -range ||
                                        q == range ||
                                        r == -range ||
                                        r == range ||
                                        s == -range ||
                                        s == range) && !isCorner

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
                        if (gameState.board[row][col] != GameState.EMPTY) {
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
