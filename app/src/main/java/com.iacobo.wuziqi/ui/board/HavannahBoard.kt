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
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Implementation of a Havannah game board.
 * Havannah uses a hexagonal grid with a hexagonal shape (vs. the parallelogram shape of Hex).
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
        // Calculate actual board size (Havannah uses a hexagonal board of size N)
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

        // New colors for highlighting the win structures
        val ringColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
        val bridgeColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
        val forkColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)

        val density = LocalDensity.current
        val strokeWidth = with(density) { 1.5.dp.toPx() }

        // Get the winning path if the game is over (there's a winner)
        val winningPath = if (isGameFrozen) gameState.getWinningPath() else emptySet()
        
        // Track board dimensions for hexagons
        var boardWidthPx by remember { mutableStateOf(0f) }
        var boardHeightPx by remember { mutableStateOf(0f) }

        Box(
                modifier = Modifier.aspectRatio(1f).padding(16.dp).background(backgroundColor)
        ) {
            Canvas(
                    modifier = 
                            Modifier.fillMaxSize().pointerInput(isGameFrozen) {
                                if (!isGameFrozen) {
                                    detectTapGestures { tapOffset ->
                                        // Update board dimensions
                                        boardWidthPx = size.width
                                        boardHeightPx = size.height
                                        
                                        // Havannah uses a hexagonal board layout
                                        // Calculate the grid dimensions and spacing
                                        val radius = minOf(boardWidthPx, boardHeightPx) / (boardSize * 2.5f)
                                        
                                        // Find the closest hexagon to the tap
                                        var bestRow = -1
                                        var bestCol = -1
                                        var bestDistance = Float.MAX_VALUE
                                        
                                        // The center of the board is the origin point
                                        val centerX = boardWidthPx / 2
                                        val centerY = boardHeightPx / 2
                                        
                                        // This transforms coordinates from Havannah's hexagonal grid to our internal storage grid
                                        for (q in -boardSize+1 until boardSize) {
                                            for (r in -boardSize+1 until boardSize) {
                                                // Skip invalid coordinates in hexagonal grid
                                                val s = -q - r
                                                if (kotlin.math.abs(q) + kotlin.math.abs(r) + kotlin.math.abs(s) > 2 * (boardSize - 1)) {
                                                    continue
                                                }
                                                
                                                // Convert axial coordinates to pixel positions
                                                val x = centerX + radius * 1.5f * q
                                                val y = centerY + radius * sqrt(3f) * (r + q/2f)
                                                
                                                // Calculate distance to tap point
                                                val distance = sqrt((tapOffset.x - x).pow(2) + (tapOffset.y - y).pow(2))
                                                
                                                // If this is closer than the previous best, track it
                                                if (distance < bestDistance) {
                                                    bestDistance = distance
                                                    // Transform from axial coordinates to array indices
                                                    bestRow = r + boardSize - 1
                                                    bestCol = q + boardSize - 1
                                                }
                                            }
                                        }
                                        
                                        // Only place a tile if we found a valid position and it's close enough
                                        if (bestRow >= 0 && bestRow < gameState.boardSize && 
                                            bestCol >= 0 && bestCol < gameState.boardSize &&
                                            bestDistance < radius * 0.8f &&
                                            gameState.board[bestRow][bestCol] == GameState.EMPTY) {
                                            onMoveSelected(bestRow, bestCol)
                                        }
                                    }
                                }
                            }
            ) {
                // Update board dimensions
                boardWidthPx = size.width
                boardHeightPx = size.height
                
                // The hexagon side length
                val radius = minOf(boardWidthPx, boardHeightPx) / (boardSize * 2.5f)
                
                // The center of the board is the origin point
                val centerX = boardWidthPx / 2
                val centerY = boardHeightPx / 2
                
                // Map of positions to center coordinates for highlighting winning structures
                val hexCenters = mutableMapOf<Pair<Int, Int>, Offset>()
                
                // Draw the hexagonal grid
                for (q in -boardSize+1 until boardSize) {
                    for (r in -boardSize+1 until boardSize) {
                        // Skip invalid coordinates in hexagonal grid
                        val s = -q - r
                        if (kotlin.math.abs(q) + kotlin.math.abs(r) + kotlin.math.abs(s) > 2 * (boardSize - 1)) {
                            continue
                        }
                        
                        // Convert axial coordinates to pixel positions
                        val x = centerX + radius * 1.5f * q
                        val y = centerY + radius * sqrt(3f) * (r + q/2f)
                        
                        // Transform from axial coordinates to array indices
                        val row = r + boardSize - 1
                        val col = q + boardSize - 1
                        
                        // Store the center for later use
                        hexCenters[Pair(row, col)] = Offset(x, y)
                        
                        // Draw the hexagon
                        val hexPath = Path().apply {
                            for (i in 0 until 6) {
                                val angle = Math.toRadians((60 * i).toDouble()).toFloat()
                                val hx = x + radius * cos(angle)
                                val hy = y + radius * sin(angle)
                                
                                if (i == 0) {
                                    moveTo(hx, hy)
                                } else {
                                    lineTo(hx, hy)
                                }
                            }
                            close()
                        }
                        
                        // Alternate fill colors for adjacent hexagons
                        val fillColor = if ((q + r) % 2 == 0) hexColor1 else hexColor2
                        drawPath(hexPath, color = fillColor, style = Fill)
                        
                        // Draw grid lines
                        drawPath(hexPath, color = gridLineColor, style = Stroke(width = strokeWidth))
                        
                        // Check if this cell has a game piece
                        if (row >= 0 && row < gameState.boardSize && col >= 0 && col < gameState.boardSize) {
                            if (gameState.board[row][col] != GameState.EMPTY) {
                                val pieceColor = if (gameState.board[row][col] == GameState.PLAYER_ONE) 
                                                     playerOneColor else playerTwoColor
                                
                                // Draw the game piece
                                drawCircle(
                                    color = pieceColor,
                                    radius = radius * 0.7f,
                                    center = Offset(x, y)
                                )
                                
                                // Add a highlight for the last placed piece
                                if (lastPlacedPosition?.row == row && lastPlacedPosition.col == col && winningPath.isEmpty()) {
                                    drawCircle(
                                        color = tertiaryColor,
                                        radius = radius * 0.8f,
                                        center = Offset(x, y),
                                        style = Stroke(width = strokeWidth * 1.5f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Highlight the winning structure if there is a winner
                if (winningPath.isNotEmpty()) {
                    // The winning path is already collected by the game logic
                    // Here we just need to highlight it
                    
                    // Draw glows under the winning pieces
                    for (pos in winningPath) {
                        val center = hexCenters[pos] ?: continue
                        
                        // Use different colors based on win type (stored in extra data)
                        val winType = if (winningPath.size <= 1) 0 
                                     else gameState.getWinningType()
                        
                        val highlightColor = when (winType) {
                            1 -> ringColor    // Ring win
                            2 -> bridgeColor  // Bridge win
                            3 -> forkColor    // Fork win
                            else -> tertiaryColor
                        }
                        
                        drawCircle(
                            color = highlightColor,
                            radius = radius * 0.9f,
                            center = center,
                            alpha = 0.4f
                        )
                    }
                    
                    // Draw connectors between pieces
                    for (i in winningPath.toList().indices) {
                        val pos = winningPath.toList()[i]
                        val center = hexCenters[pos] ?: continue
                        
                        if (i < winningPath.size - 1) {
                            val nextPos = winningPath.toList()[i + 1]
                            val nextCenter = hexCenters[nextPos] ?: continue
                            
                            // Use different colors based on win type (stored in extra data)
                            val winType = gameState.getWinningType()
                            
                            val lineColor = when (winType) {
                                1 -> ringColor    // Ring win
                                2 -> bridgeColor  // Bridge win
                                3 -> forkColor    // Fork win
                                else -> tertiaryColor
                            }
                            
                            // Draw connecting line
                            drawLine(
                                color = lineColor,
                                start = center,
                                end = nextCenter,
                                strokeWidth = strokeWidth * 2f
                            )
                        }
                    }
                }
            }
        }
    }
}
