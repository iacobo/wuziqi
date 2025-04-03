package com.iacobo.wuziqi.ui.board

import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.ui.theme.HexPieceBlue
import com.iacobo.wuziqi.ui.theme.HexPieceRed
import com.iacobo.wuziqi.viewmodel.Position
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
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
        val aspectRatio = if (isLandscape) 0.866f else 1.155f

        // Store hexagon centers for hit testing
        val hexCenters = remember { mutableMapOf<Pair<Int, Int>, Offset>() }

        Box(
                modifier =
                        Modifier.aspectRatio(aspectRatio).padding(16.dp).background(backgroundColor)
        ) {
            Canvas(
                    modifier =
                            Modifier.fillMaxSize().pointerInput(isGameFrozen) {
                                if (!isGameFrozen) {
                                    detectTapGestures { tapOffset ->
                                        // Find the closest hexagon to the tap
                                        var bestRow = -1
                                        var bestCol = -1
                                        var bestDistance = Float.MAX_VALUE

                                        // Check all stored hexes for the closest match
                                        for ((pos, center) in hexCenters) {
                                            val (row, col) = pos
                                            
                                            // Calculate distance to center
                                            val distance = sqrt(
                                                (tapOffset.x - center.x).pow(2) +
                                                (tapOffset.y - center.y).pow(2)
                                            )
                                            
                                            // Check if this is closer than the previous best
                                            // and if the position is empty
                                            if (distance < bestDistance && 
                                                row < boardSize && col < boardSize && 
                                                gameState.board[row][col] == GameState.EMPTY) {
                                                bestDistance = distance
                                                bestRow = row
                                                bestCol = col
                                            }
                                        }
                                        
                                        // Use a generous threshold - 15% of the smaller dimension
                                        val threshold = min(size.width, size.height) * 0.15f
                                        
                                        // If we found a valid position and it's close enough
                                        if (bestRow >= 0 && bestDistance < threshold) {
                                            onMoveSelected(bestRow, bestCol)
                                        }
                                    }
                                }
                            }
            ) {
                // Clear the centers map at the start of each drawing
                hexCenters.clear()
                
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Calculate the maximum row offset for the board
                val maxRowOffset = (boardSize - 1) * 0.5f

                // Size the hexagons to fit the total width (including offset)
                val hexRadius =
                        minOf(
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

                // Use the original working approach for drawing the hexagonal grid
                val range = boardSize - 1
                
                for (q in -range..range) {
                    val rMin = max(-range, -q - range)
                    val rMax = min(range, -q + range)
                    
                    for (r in rMin..rMax) {
                        val s = -q - r
                        
                        // Convert to array indices
                        val row = r + range
                        val col = q + range
                        
                        // Skip positions outside array bounds
                        if (row >= boardSize || col >= boardSize) continue
                        
                        // Calculate hexagon center position
                        val rowShift = row * (hexWidth / 2)
                        val centerX = xOffset + col * hexWidth + rowShift
                        val centerY = yOffset + row * hexHeight * 0.75f + hexHeight / 2
                        
                        // Store the center for later use in hit testing and path highlighting
                        hexCenters[Pair(row, col)] = Offset(centerX, centerY)

                        // Store vertices for border edge coloring
                        val hexVertices =
                                List(6) { idx ->
                                    val angle = Math.toRadians((60 * idx + 30).toDouble()).toFloat()
                                    Pair(
                                            centerX + hexRadius * cos(angle),
                                            centerY + hexRadius * sin(angle)
                                    )
                                }

                        val hexPath =
                                Path().apply {
                                    moveTo(hexVertices[0].first, hexVertices[0].second)
                                    for (i in 1 until 6) {
                                        lineTo(hexVertices[i].first, hexVertices[i].second)
                                    }
                                    close()
                                }

                        // Alternate fill colors
                        val fillColor = if ((row + col) % 2 == 0) hexColor1 else hexColor2
                        drawPath(hexPath, color = fillColor, style = Fill)

                        // Draw grid lines
                        drawPath(
                                hexPath,
                                color = gridLineColor,
                                style = Stroke(width = strokeWidth)
                        )
                        
                        // Draw game pieces if present
                        if (gameState.board[row][col] != GameState.EMPTY) {
                            val pieceColor = if (gameState.board[row][col] == GameState.PLAYER_ONE)
                                playerOneColor else playerTwoColor
                            
                            // Draw piece with highlight
                            drawCircle(
                                    color = pieceColor,
                                    radius = hexRadius * 0.42f,
                                    center = Offset(centerX, centerY)
                            )

                            // Add a subtle highlight to the top-left of the piece for 3D effect
                            drawCircle(
                                    color = pieceColor.copy(alpha = 0.7f),
                                    radius = hexRadius * 0.32f,
                                    center =
                                            Offset(
                                                    centerX - hexRadius * 0.1f,
                                                    centerY - hexRadius * 0.1f
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
                                        center = Offset(centerX, centerY),
                                        style = Stroke(width = strokeWidth * 1.5f)
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
                    for (i in 0 until pathList.size - 1) {
                        val center1 = hexCenters[pathList[i]] ?: continue
                        val center2 = hexCenters[pathList[i+1]] ?: continue
                        
                        drawLine(
                            color = winningPathColor,
                            start = center1,
                            end = center2,
                            strokeWidth = strokeWidth * 3f
                        )
                    }
                    
                    // For ring wins, connect first and last piece
                    if (gameState.getWinningType() == 1 && pathList.size > 2) {
                        val firstCenter = hexCenters[pathList.first()]
                        val lastCenter = hexCenters[pathList.last()]
                        
                        if (firstCenter != null && lastCenter != null) {
                            drawLine(
                                color = winningPathColor,
                                start = firstCenter,
                                end = lastCenter,
                                strokeWidth = strokeWidth * 3f
                            )
                        }
                    }
                }
            }
        }
    }
}
