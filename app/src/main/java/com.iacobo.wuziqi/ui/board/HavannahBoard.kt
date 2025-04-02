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

/**
 * Complete rewrite of the Havannah game board.
 * 
 * A proper Havannah board is a hexagon with side length n (in this case 10).
 * It has 6 corners and 6 edges of length n (excluding corners).
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
        // The side length of the hexagonal board (not the full diameter)
        val sideLength = 10
        
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
            modifier = Modifier
                .aspectRatio(1f)
                .padding(4.dp) // Minimal padding to maximize board space
                .background(backgroundColor)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isGameFrozen) {
                        if (!isGameFrozen) {
                            detectTapGestures { tap ->
                                // Will be populated with hex centers and their array positions
                                if (hexCenters.isNotEmpty()) {
                                    // Find closest hex
                                    var bestDist = Float.MAX_VALUE
                                    var bestPos: Pair<Int, Int>? = null
                                    
                                    hexCenters.forEach { (pos, center) ->
                                        val dist = sqrt(
                                            (tap.x - center.x) * (tap.x - center.x) + 
                                            (tap.y - center.y) * (tap.y - center.y)
                                        )
                                        if (dist < bestDist) {
                                            val (row, col) = pos
                                            if (gameState.board[row][col] == GameState.EMPTY) {
                                                bestDist = dist
                                                bestPos = pos
                                            }
                                        }
                                    }
                                    
                                    // If we found a close empty cell, select it
                                    bestPos?.let { (row, col) ->
                                        // Check if the tap is close enough (within cell radius)
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
                
                // Calculate size based on canvas dimensions and side length
                // Use full canvas with small fixed margin
                val margin = size.minDimension * 0.05f
                val usableWidth = size.width - (2 * margin)
                val usableHeight = size.height - (2 * margin)
                
                // The hexagonal board with side length n requires width = 2n-1 cells
                // The height is also 2n-1 cells
                val boardWidth = 2 * sideLength - 1
                val boardHeight = 2 * sideLength - 1
                
                // Calculate hex size to fit the entire board
                // The flat-topped orientation width/height ratio is sqrt(3)/2
                val hexWidth = usableWidth / boardWidth
                val hexHeight = usableHeight / boardHeight
                hexSize = minOf(hexWidth, hexHeight / sqrt(3f) * 2f) * 0.95f
                
                // Center of the canvas
                val centerX = size.width / 2
                val centerY = size.height / 2
                
                // Draw the board using cube coordinates (q,r,s where q+r+s=0)
                // For a size n board, the range for each coordinate is -n+1 to n-1
                val range = sideLength - 1
                
                for (q in -range..range) {
                    // r range depends on q to form a hexagon
                    val rMin = maxOf(-range, -q - range)
                    val rMax = minOf(range, -q + range)
                    
                    for (r in rMin..rMax) {
                        val s = -q - r
                        
                        // Convert cube coordinates to array indices for storage
                        // Center the hexagon in the array
                        val row = r + range
                        val col = q + range
                        
                        // Calculate pixel position for hex center
                        // Using flat-topped hex layout
                        val x = centerX + hexSize * 1.5f * q
                        val y = centerY + hexSize * sqrt(3f) * (r + q/2f)
                        
                        // Store center position for hit testing
                        hexCenters[Pair(row, col)] = Offset(x, y)
                        
                        // Create hexagon path
                        val hexPath = createHexagonPath(x, y, hexSize)
                        
                        // Determine if this is a corner, edge, or inner cell
                        // For a hexagon, corners have exactly two coordinates at ±range
                        val isCorner = (
                            // The six corners of the hexagon
                            (q == -range && r == range) || // top-left
                            (q == 0 && r == range) || // top
                            (q == range && r == 0) || // top-right
                            (q == range && r == -range) || // bottom-right
                            (q == 0 && r == -range) || // bottom
                            (q == -range && r == 0) // bottom-left
                        )
                        
                        // Edges have exactly one coordinate at ±range (excluding corners)
                        val isEdge = !isCorner && (
                            kotlin.math.abs(q) == range || 
                            kotlin.math.abs(r) == range || 
                            kotlin.math.abs(s) == range
                        )
                        
                        // Select fill color based on position type
                        val fillColor = when {
                            isCorner -> cornerColor
                            isEdge -> edgeColor
                            else -> centerColor
                        }
                        
                        // Draw hexagon fill
                        drawPath(hexPath, color = fillColor, style = Fill)
                        
                        // Draw hexagon border
                        drawPath(hexPath, color = gridLineColor, style = Stroke(width = strokeWidth))
                        
                        // Draw game pieces if present
                        if (row in 0 until gameState.boardSize && 
                            col in 0 until gameState.boardSize && 
                            gameState.board[row][col] != GameState.EMPTY) {
                            
                            val pieceColor = if (gameState.board[row][col] == GameState.PLAYER_ONE) 
                                playerOneColor else playerTwoColor
                            
                            // Draw the piece
                            drawCircle(
                                color = pieceColor,
                                radius = hexSize * 0.7f,
                                center = Offset(x, y)
                            )
                            
                            // Highlight for last placed piece
                            if (lastPlacedPosition?.row == row && 
                                lastPlacedPosition.col == col && 
                                winningPath.isEmpty()) {
                                    
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
                    val winColor = when (winType) {
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
                        val firstCenter = hexCenters[pathList.first()] ?: return@Canvas
                        val lastCenter = hexCenters[pathList.last()] ?: return@Canvas
                        
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
    
    // Cache hex cell centers for hit testing
    private val hexCenters = mutableMapOf<Pair<Int, Int>, Offset>()
    
    // Hex cell size, stored for hit testing
    private var hexSize = 0f
    
    /**
     * Creates a hexagon path centered at (x,y) with the given size.
     */
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
