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
        
        // New color for highlighting the winning path
        val winningPathColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)

        val density = LocalDensity.current
        val strokeWidth = with(density) { 1.5.dp.toPx() }
        
        // Get the winning path if the game is over (there's a winner)
        val winningPath = if (isGameFrozen) gameState.getWinningPath() else emptySet()
        
        // Detect current orientation
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        
        // Use different aspect ratio for landscape mode
        val aspectRatio = if (isLandscape) 1.5f else 1f

        Box(
            modifier = Modifier
                .aspectRatio(aspectRatio)
                .padding(16.dp)
                .background(backgroundColor)
        ) {
                Canvas(
                        modifier =
                                Modifier.fillMaxSize().pointerInput(isGameFrozen) {
                                        if (!isGameFrozen) {
                                                detectTapGestures { tapOffset ->
                                                        val canvasWidth = size.width
                                                        val canvasHeight = size.height

                                                        // Calculate the maximum row offset for the board
                                                        val maxRowOffset = (boardSize - 1) * 0.5f

                                                        // Size the hexagons to fit the total width (including offset)
                                                        val hexRadius = minOf(
                                                            canvasWidth / ((boardSize + maxRowOffset) * sqrt(3f)),
                                                            canvasHeight / ((boardSize + 0.5f) * 1.5f)
                                                        ) * 0.95f
                                                        
                                                        val hexHeight = hexRadius * 2
                                                        val hexWidth = hexRadius * sqrt(3f)

                                                        // Calculate the total width and height of the board
                                                        val totalWidth = (boardSize + maxRowOffset) * hexWidth
                                                        val totalHeight = hexHeight * boardSize * 0.75f + hexHeight / 4
                                                        
                                                        // Calculate offset to center the board
                                                        val xOffset = (canvasWidth - totalWidth) / 2
                                                        val yOffset = (canvasHeight - totalHeight) / 2

                                                        // Simplified approach: find the closest hexagon to the tap
                                                        var bestRow = -1
                                                        var bestCol = -1
                                                        var bestDistance = Float.MAX_VALUE

                                                        // Check all possible positions
                                                        for (row in 0 until boardSize) {
                                                            for (col in 0 until boardSize) {
                                                                // Calculate the center position of this hexagon
                                                                val rowShift = row * (hexWidth / 2)
                                                                val centerX = xOffset + col * hexWidth + rowShift
                                                                val centerY = yOffset + row * hexHeight * 0.75f + hexHeight / 2

                                                                // Calculate distance to tap point
                                                                val distance = sqrt(
                                                                    (tapOffset.x - centerX).pow(2) +
                                                                    (tapOffset.y - centerY).pow(2)
                                                                )

                                                                // If this is closer than the previous best and is empty, track it
                                                                if (distance < bestDistance) {
                                                                    bestDistance = distance
                                                                    bestRow = row
                                                                    bestCol = col
                                                                }
                                                            }
                                                        }

                                                        // Only place a tile if we found a valid position and it's close enough
                                                        if (bestRow >= 0 && bestDistance < hexRadius * 1.5f &&
                                                            gameState.board[bestRow][bestCol] == GameState.EMPTY) {
                                                            onTileClick(bestRow, bestCol)
                                                        }
                                                }
                                        }
                                }
                ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Calculate the maximum row offset for the board
                        val maxRowOffset = (boardSize - 1) * 0.5f

                        // Size the hexagons to fit the total width (including offset)
                        val hexRadius = minOf(
                            canvasWidth / ((boardSize + maxRowOffset) * sqrt(3f)),
                            canvasHeight / ((boardSize + 0.5f) * 1.5f)
                        ) * 0.95f
                        
                        val hexHeight = hexRadius * 2
                        val hexWidth = hexRadius * sqrt(3f)

                        // Calculate the total width and height of the board
                        val totalWidth = (boardSize + maxRowOffset) * hexWidth
                        val totalHeight = hexHeight * boardSize * 0.75f + hexHeight / 4
                        
                        // Calculate offset to center the board
                        val xOffset = (canvasWidth - totalWidth) / 2
                        val yOffset = (canvasHeight - totalHeight) / 2

                        // Map of positions to center coordinates for highlighting winning path
                        val hexCenters = mutableMapOf<Pair<Int, Int>, Offset>()

                        // Draw all hexagons
                        for (row in 0 until boardSize) {
                                for (col in 0 until boardSize) {
                                        // Apply correct offset for each row to create the parallelogram
                                        val rowShift = row * (hexWidth / 2)
                                        val centerX = xOffset + col * hexWidth + rowShift
                                        val centerY = yOffset + row * hexHeight * 0.75f + hexHeight / 2
                                        
                                        // Store the center for later use in path highlighting
                                        hexCenters[Pair(row, col)] = Offset(centerX, centerY)

                                        // Store vertices for border edge coloring
                                        val hexVertices = List(6) { idx ->
                                                val angle = Math.toRadians((60 * idx + 30).toDouble()).toFloat()
                                                Pair(
                                                        centerX + hexRadius * cos(angle),
                                                        centerY + hexRadius * sin(angle)
                                                )
                                        }

                                        val hexPath = Path().apply {
                                                moveTo(hexVertices[0].first, hexVertices[0].second)
                                                for (i in 1 until 6) {
                                                        lineTo(hexVertices[i].first, hexVertices[i].second)
                                                }
                                                close()
                                        }

                                        // Alternate fill colors
                                        val fillColor = if ((row + col) % 2 == 0) hexColor1 else hexColor2
                                        drawPath(hexPath, color = fillColor, style = Fill)

                                        // Draw standard grid lines for interior hexagons
                                        val isTopRow = row == 0
                                        val isBottomRow = row == boardSize - 1
                                        val isLeftCol = col == 0
                                        val isRightCol = col == boardSize - 1

                                        // For interior hexagons or non-edge sides, use standard grid color
                                        if (!isTopRow && !isBottomRow && !isLeftCol && !isRightCol) {
                                                drawPath(hexPath, color = gridLineColor, style = Stroke(width = strokeWidth))
                                        } else {
                                                // For edge hexagons, color each edge appropriately
                                                for (i in 0 until 6) {
                                                        val nextIdx = (i + 1) % 6
                                                        val startX = hexVertices[i].first
                                                        val startY = hexVertices[i].second
                                                        val endX = hexVertices[nextIdx].first
                                                        val endY = hexVertices[nextIdx].second

                                                        val isRightEdge = isRightCol && (i == 4 || i == 5)
                                                        val isLeftEdge = isLeftCol && (i == 1 || i == 2)
                                                        val isTopEdge = isTopRow && (i == 3 || i == 4)
                                                        val isBottomEdge = isBottomRow && (i == 0 || i == 1)

                                                        val edgeColor = when {
                                                                isTopEdge || isBottomEdge -> playerOneColor
                                                                isLeftEdge || isRightEdge -> playerTwoColor
                                                                else -> gridLineColor
                                                        }

                                                        val edgeWidth = when {
                                                                isTopEdge || isBottomEdge || isLeftEdge || isRightEdge -> strokeWidth * 2.5f
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
                                }
                        }
                        
                        // Highlight the winning path cells if there is a winner
                        if (winningPath.isNotEmpty()) {
                            // Draw a glow under the winning path pieces
                            for (pos in winningPath) {
                                val center = hexCenters[pos] ?: continue
                                drawCircle(
                                    color = winningPathColor,
                                    radius = hexRadius * 0.7f,
                                    center = center,
                                    alpha = 0.5f
                                )
                            }
                            
                            // Draw connectors between adjacent pieces in the winning path
                            val pathList = winningPath.toList()
                            for (posA in pathList) {
                                val centerA = hexCenters[posA] ?: continue
                                
                                // Define the neighbor directions
                                val neighbors = arrayOf(
                                    Pair(-1, 0),  // Top-left
                                    Pair(-1, 1),  // Top-right
                                    Pair(0, -1),  // Left
                                    Pair(0, 1),   // Right
                                    Pair(1, -1),  // Bottom-left
                                    Pair(1, 0)    // Bottom-right
                                )
                                
                                // Check each direction for adjacent pieces in the path
                                for ((dr, dc) in neighbors) {
                                    val neighborPos = Pair(posA.first + dr, posA.second + dc)
                                    
                                    // If the neighbor is in the winning path, draw a connector
                                    if (winningPath.contains(neighborPos)) {
                                        val centerB = hexCenters[neighborPos] ?: continue
                                        
                                        // Only draw the connection once (from lower to higher index)
                                        val idxA = pathList.indexOf(posA)
                                        val idxB = pathList.indexOf(neighborPos)
                                        if (idxA < idxB) {
                                            drawLine(
                                                color = winningPathColor,
                                                start = centerA,
                                                end = centerB,
                                                strokeWidth = strokeWidth * 3f
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Draw game pieces (after winning path so pieces are on top)
                        for (row in 0 until boardSize) {
                                for (col in 0 until boardSize) {
                                        if (gameState.board[row][col] != GameState.EMPTY) {
                                                val pos = Pair(row, col)
                                                val center = hexCenters[pos] ?: continue
                                                
                                                val pieceColor = when (gameState.board[row][col]) {
                                                        GameState.PLAYER_ONE -> playerOneColor
                                                        else -> playerTwoColor
                                                }
                                                
                                                // Draw piece with highlight
                                                drawCircle(
                                                        color = pieceColor,
                                                        radius = hexRadius * 0.42f,
                                                        center = center
                                                )

                                                // Add a subtle highlight to the top-left of the piece for 3D effect
                                                drawCircle(
                                                        color = pieceColor.copy(alpha = 0.7f),
                                                        radius = hexRadius * 0.32f,
                                                        center = Offset(
                                                                center.x - hexRadius * 0.1f,
                                                                center.y - hexRadius * 0.1f
                                                        )
                                                )

                                                // Highlight the last placed piece if not part of the winning path
                                                if (lastPlacedPosition?.row == row && 
                                                    lastPlacedPosition.col == col && 
                                                    winningPath.isEmpty()
                                                ) {
                                                        drawCircle(
                                                                color = tertiaryColor,
                                                                radius = hexRadius * 0.5f,
                                                                center = center,
                                                                style = Stroke(width = strokeWidth * 1.5f)
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}
