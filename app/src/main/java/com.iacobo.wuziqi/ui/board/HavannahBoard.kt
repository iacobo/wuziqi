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
 * Implementation of the Havannah game board with proper hexagonal grid and proper clickable areas.
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
        // Get the game type
        val gameType = GameType.fromGameState(gameState)

        // Get the edge length for the hexagonal board
        val edgeLength = gameType.getEdgeLength()

        // Properly calculate aspect ratio for hexagonal board
        // For a hexagonal board, width:height ratio is approximately 2:√3
        val hexAspectRatio = 2f / sqrt(3f)

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

        // Store hexagon centers for hit testing - using remember to preserve across recompositions
        val hexCenters = remember { mutableMapOf<Pair<Int, Int>, Offset>() }

        Box(
                modifier =
                        Modifier.aspectRatio(hexAspectRatio)
                                .padding(8.dp)
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

                                            // Calculate Euclidean distance from tap to center
                                            val dist =
                                                    sqrt(
                                                            (tap.x - center.x) *
                                                                    (tap.x - center.x) +
                                                                    (tap.y - center.y) *
                                                                            (tap.y - center.y)
                                                    )

                                            // Check if this cell is valid and empty
                                            if (gameType.isValidHexPosition(row, col) &&
                                                            gameState.board[row][col] ==
                                                                    GameState.EMPTY
                                            ) {
                                                if (dist < bestDistance) {
                                                    bestDistance = dist
                                                    bestMatch = pos
                                                }
                                            }
                                        }

                                        // Using a more generous click threshold - 10% of the
                                        // smaller dimension
                                        val clickThreshold = min(size.width, size.height) * 0.1f

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

                // Calculate board dimensions for drawing
                val boardDiameter = 2 * edgeLength - 1
                val padding = size.width * 0.05f // 5% padding
                val availableWidth = size.width - 2 * padding
                val availableHeight = size.height - 2 * padding

                // Calculate hex size to fit within available space
                val hexSize =
                        min(
                                availableWidth / (boardDiameter * 1.5f),
                                availableHeight / (boardDiameter * sqrt(3f) / 2f)
                        )

                // Center position
                val centerX = size.width / 2
                val centerY = size.height / 2

                // Center of the board in array coordinates
                val boardCenter = gameState.boardSize / 2

                // Range of coordinates (edge length - 1 for the hexagon)
                val range = edgeLength - 1

                // Define corners for special highlighting
                val corners =
                        setOf(
                                Triple(-range, range, 0), // top-left
                                Triple(0, range, -range), // top
                                Triple(range, 0, -range), // top-right
                                Triple(range, -range, 0), // bottom-right
                                Triple(0, -range, range), // bottom
                                Triple(-range, 0, range) // bottom-left
                        )

                // Draw hexagonal grid using axial coordinates
                for (q in -range..range) {
                    val rMin = maxOf(-range, -q - range)
                    val rMax = minOf(range, -q + range)

                    for (r in rMin..rMax) {
                        val s = -q - r // Third coordinate for cube representation

                        // Skip hexes that are outside the valid hexagonal board
                        if (abs(q) + abs(r) + abs(s) > range * 2) continue

                        // Convert to array indices (for game state access)
                        val row = r + boardCenter
                        val col = q + boardCenter

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
                                (abs(q) == range || abs(r) == range || abs(s) == range) && !isCorner

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
                        if (row in 0 until gameState.boardSize &&
                                        col in 0 until gameState.boardSize &&
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
                    // Rest of winning path drawing logic remains the same
                    // ...
                }
            }
        }
    }
}
