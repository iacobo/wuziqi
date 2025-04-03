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
import com.iacobo.wuziqi.ui.theme.BridgeColor
import com.iacobo.wuziqi.ui.theme.ForkColor
import com.iacobo.wuziqi.ui.theme.HexPieceBlue
import com.iacobo.wuziqi.ui.theme.HexPieceRed
import com.iacobo.wuziqi.ui.theme.RingColor
import com.iacobo.wuziqi.viewmodel.Position
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Implementation of the Havannah game board with proper hexagonal grid display and interaction.
 * This uses an explicit, direct hex construction approach to ensure all cells are visible and
 * clickable.
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

        // Map to store all hex centers for hit detection
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

                                        // More generous click threshold - 20% of the smaller
                                        // dimension
                                        val clickThreshold = min(size.width, size.height) * 0.2f

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

                // Calculate hex size based on available space
                val padding = size.width * 0.08f // 8% padding
                val availableWidth = size.width - 2 * padding
                val availableHeight = size.height - 2 * padding

                // Compute optimal hex size for the entire board
                // A Havannah board with size N has 3N-2 hexes across its widest row
                val hexSize =
                        min(
                                availableWidth / ((3 * boardSize - 2) * 0.9f),
                                availableHeight / ((2 * boardSize) * 0.8f)
                        )

                // Center position
                val centerX = size.width / 2
                val centerY = size.height / 2

                // For a Havannah board of size N, we have:
                // - N hexes on each of the 6 edges
                // - (N-1) hexes from center to each corner
                // - Total of 3N(N-1)+1 hexes

                // Define rows for the hexagonal board
                // The size of rows grows from N to 2N-1, then shrinks back to N

                // For a specific visualization:
                //     _ _         Rows 1 to (boardSize-1) have increasing length
                //   _/   \_       Row boardSize has length (2*boardSize-1)
                //  /       \      Rows (boardSize+1) to (2*boardSize-1) have decreasing length
                // |         |
                //  \       /
                //   \_   _/
                //     - -

                // Pre-calculate all hex positions with explicit row/column calculations
                val hexPositions =
                        mutableListOf<Triple<Int, Int, Boolean>>() // (row, col, isCornerOrEdge)

                // 1. Top half of the board (including middle row)
                for (row in 0 until boardSize) {
                    // Row width grows from 'boardSize' to '2*boardSize-1'
                    val rowLength = boardSize + row

                    // For each row, determine starting column
                    val startCol = boardSize - 1 - row / 2

                    for (i in 0 until rowLength) {
                        val col = startCol + i

                        // Determine if this is a corner or edge hex
                        val isCornerOrEdge =
                                row == 0 || // Top edge
                                i == 0 || // Left edge of this row
                                        i == rowLength - 1 // Right edge of this row

                        hexPositions.add(Triple(row, col, isCornerOrEdge))
                    }
                }

                // 2. Bottom half of the board (excluding middle row)
                for (rowOffset in 1 until boardSize) {
                    val row = boardSize + rowOffset - 1

                    // Row width shrinks from '2*boardSize-2' to 'boardSize'
                    val rowLength = 2 * boardSize - 1 - rowOffset

                    // Starting column shifts right
                    val startCol = (boardSize - 1) + rowOffset / 2

                    for (i in 0 until rowLength) {
                        val col = startCol + i

                        // Determine if this is an edge hex
                        val isCornerOrEdge =
                                rowOffset == boardSize - 1 || // Bottom edge
                                i == 0 || // Left edge of this row
                                        i == rowLength - 1 // Right edge of this row

                        hexPositions.add(Triple(row, col, isCornerOrEdge))
                    }
                }

                // Define corners explicitly for a board of any size
                val corners =
                        listOf(
                                Pair(0, boardSize - 1), // Top
                                Pair(0, 2 * boardSize - 2), // Top-right
                                Pair(boardSize - 1, 3 * boardSize - 3), // Right
                                Pair(2 * boardSize - 2, 2 * boardSize - 2), // Bottom-right
                                Pair(2 * boardSize - 2, boardSize - 1), // Bottom-left
                                Pair(boardSize - 1, 0) // Left
                        )

                // Now draw all hexagons at their computed positions
                for ((row, col, isEdge) in hexPositions) {
                    // Check if position is within array bounds
                    if (row >= boardSize || col >= boardSize) continue

                    // Calculate visual position for this hex
                    // For a flat-topped hexagonal grid:
                    val x = centerX + (col - (3 * boardSize - 3) / 2) * hexSize * 0.9f
                    val y = centerY + (row - (2 * boardSize - 2) / 2) * hexSize * 0.8f

                    // Store center for hit testing
                    hexCenters[Pair(row, col)] = Offset(x, y)

                    // Create hexagon path
                    val hexPath =
                            Path().apply {
                                for (i in 0 until 6) {
                                    val angle = Math.PI / 3 * i
                                    val hx = x + hexSize * 0.5f * cos(angle.toFloat())
                                    val hy = y + hexSize * 0.5f * sin(angle.toFloat())

                                    if (i == 0) moveTo(hx, hy) else lineTo(hx, hy)
                                }
                                close()
                            }

                    // Check if this is a corner
                    val isCorner = corners.contains(Pair(row, col))

                    // Fill color based on cell type
                    val fillColor =
                            when {
                                isCorner -> cornerColor
                                isEdge -> edgeColor
                                else -> centerColor
                            }

                    // Draw hex cell
                    drawPath(hexPath, color = fillColor, style = Fill)
                    drawPath(hexPath, color = gridLineColor, style = Stroke(width = strokeWidth))

                    // Draw game pieces if present
                    if (row < boardSize &&
                                    col < boardSize &&
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
