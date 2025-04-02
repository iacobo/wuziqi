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
 * Simple Havannah game board implementation.
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
        // CONSTANTS
        val BOARD_SIZE = 10 // Game is using a 10x10 array

        // COLORS
        val backgroundColor = MaterialTheme.colorScheme.surface
        val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        val cornerColor = MaterialTheme.colorScheme.primaryContainer
        val edgeColor = MaterialTheme.colorScheme.secondaryContainer
        val centerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        val playerOneColor = HexPieceRed
        val playerTwoColor = HexPieceBlue
        val highlightColor = MaterialTheme.colorScheme.tertiary

        // Store all valid positions
        val cellPositions = remember { mutableMapOf<Pair<Int, Int>, Offset>() }
        
        // Get winning path if any
        val winningPath = if (isGameFrozen) gameState.getWinningPath() else emptySet()
        
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(8.dp)
                .background(backgroundColor)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isGameFrozen) {
                        if (!isGameFrozen) {
                            detectTapGestures { tap ->
                                // Find the closest hex cell
                                var bestDist = Float.MAX_VALUE
                                var bestPos: Pair<Int, Int>? = null
                                
                                cellPositions.forEach { (pos, center) ->
                                    val dist = sqrt(
                                        (tap.x - center.x) * (tap.x - center.x) + 
                                        (tap.y - center.y) * (tap.y - center.y)
                                    )
                                    
                                    if (dist < bestDist) {
                                        val (row, col) = pos
                                        if (row in 0 until BOARD_SIZE && 
                                            col in 0 until BOARD_SIZE &&
                                            gameState.board[row][col] == GameState.EMPTY) {
                                            bestDist = dist
                                            bestPos = pos
                                        }
                                    }
                                }
                                
                                bestPos?.let { (row, col) ->
                                    if (bestDist < cellRadius * 0.9f) {
                                        onMoveSelected(row, col)
                                    }
                                }
                            }
                        }
                    }
            ) {
                // SETUP
                cellPositions.clear()
                
                // Calculate size to fit screen
                val canvasWidth = size.width
                val canvasHeight = size.height
                val margin = size.minDimension * 0.1f
                
                // Havannah board coordinates
                val HEX_RADIUS = 5 // For a size-10 Havannah board
                
                // Calculate cell size
                val availableWidth = canvasWidth - (2 * margin)
                val availableHeight = canvasHeight - (2 * margin)
                val cellWidth = availableWidth / (2 * HEX_RADIUS)
                val cellHeight = availableHeight / (2 * HEX_RADIUS * sqrt(3f)) * 2f
                cellRadius = minOf(cellWidth, cellHeight) * 0.45f
                
                // Center of the canvas
                val centerX = canvasWidth / 2
                val centerY = canvasHeight / 2
                
                // RENDER THE BOARD
                // In axial coordinates, a size-10 hexagon spans from -9 to +9 on each axis
                
                // Map from axial (x,z) to cubic (x,y,z) where y = -x-z
                // Map from cubic to array indices [row,col]
                
                // Loop through the cubic coordinates to create a hexagon with side length 10
                for (q in -HEX_RADIUS..HEX_RADIUS) {
                    // In a hexagon, the valid range for r depends on q
                    val rMin = maxOf(-HEX_RADIUS, -q - HEX_RADIUS)
                    val rMax = minOf(HEX_RADIUS, -q + HEX_RADIUS)
                    
                    for (r in rMin..rMax) {
                        val s = -q - r // Maintain q+r+s=0 in cubic coordinates
                        
                        // Convert cubic to array indices by offsetting
                        val row = HEX_RADIUS + r
                        val col = HEX_RADIUS + q
                        
                        // Skip positions outside game array bounds
                        if (row !in 0 until BOARD_SIZE || col !in 0 until BOARD_SIZE) {
                            continue
                        }
                        
                        // Convert to screen position with flat-top orientation
                        val xPos = centerX + (cellRadius * 1.75f) * q
                        val yPos = centerY + (cellRadius * sqrt(3f)) * (r + q/2f)
                        val center = Offset(xPos, yPos)
                        
                        // Store position for hit testing
                        cellPositions[Pair(row, col)] = center
                        
                        // Create hex path
                        val hexPath = Path().apply {
                            for (i in 0 until 6) {
                                val angle = Math.toRadians((60 * i).toDouble()).toFloat()
                                val hx = xPos + cellRadius * cos(angle)
                                val hy = yPos + cellRadius * sin(angle)
                                
                                if (i == 0) {
                                    moveTo(hx, hy)
                                } else {
                                    lineTo(hx, hy)
                                }
                            }
                            close()
                        }
                        
                        // Determine cell type
                        val isCorner = 
                            (q == -HEX_RADIUS && r == HEX_RADIUS) || // top-left
                            (q == 0 && r == HEX_RADIUS) || // top
                            (q == HEX_RADIUS && r == 0) || // top-right
                            (q == HEX_RADIUS && r == -HEX_RADIUS) || // bottom-right
                            (q == 0 && r == -HEX_RADIUS) || // bottom
                            (q == -HEX_RADIUS && r == 0) // bottom-left
                        
                        val isEdge = !isCorner && (
                            kotlin.math.abs(q) == HEX_RADIUS || 
                            kotlin.math.abs(r) == HEX_RADIUS || 
                            kotlin.math.abs(s) == HEX_RADIUS
                        )
                        
                        // Fill color
                        val fillColor = when {
                            isCorner -> cornerColor
                            isEdge -> edgeColor
                            else -> centerColor
                        }
                        
                        // Draw hex cell
                        drawPath(hexPath, color = fillColor, style = Fill)
                        drawPath(
                            hexPath, 
                            color = gridLineColor, 
                            style = Stroke(width = with(LocalDensity.current) { 1.dp.toPx() })
                        )
                        
                        // Draw stone if exists
                        if (gameState.board[row][col] != GameState.EMPTY) {
                            val stoneColor = if (gameState.board[row][col] == GameState.PLAYER_ONE)
                                playerOneColor else playerTwoColor
                            
                            drawCircle(
                                color = stoneColor,
                                radius = cellRadius * 0.75f,
                                center = center
                            )
                            
                            // Highlight last placed stone
                            if (lastPlacedPosition?.row == row && 
                                lastPlacedPosition.col == col &&
                                winningPath.isEmpty()) {
                                drawCircle(
                                    color = highlightColor,
                                    radius = cellRadius * 0.85f,
                                    center = center,
                                    style = Stroke(width = with(LocalDensity.current) { 2.dp.toPx() })
                                )
                            }
                        }
                    }
                }
                
                // HIGHLIGHT WINNING PATH
                if (winningPath.isNotEmpty()) {
                    val winType = gameState.getWinningType()
                    val winColor = when (winType) {
                        1 -> Color(0xFF9C27B0) // Purple for ring
                        2 -> Color(0xFF2196F3) // Blue for bridge
                        3 -> Color(0xFF4CAF50) // Green for fork
                        else -> highlightColor
                    }
                    
                    // Highlight cells
                    winningPath.forEach { pos ->
                        cellPositions[pos]?.let { center ->
                            drawCircle(
                                color = winColor,
                                radius = cellRadius * 0.9f,
                                center = center,
                                alpha = 0.4f
                            )
                        }
                    }
                    
                    // Connect cells
                    val pathList = winningPath.toList()
                    for (i in 0 until pathList.size - 1) {
                        val start = cellPositions[pathList[i]]
                        val end = cellPositions[pathList[i+1]]
                        
                        if (start != null && end != null) {
                            drawLine(
                                color = winColor,
                                start = start,
                                end = end,
                                strokeWidth = with(LocalDensity.current) { 2.5.dp.toPx() }
                            )
                        }
                    }
                    
                    // Connect first and last for rings
                    if (winType == 1 && pathList.size > 2) {
                        val first = cellPositions[pathList.first()]
                        val last = cellPositions[pathList.last()]
                        
                        if (first != null && last != null) {
                            drawLine(
                                color = winColor,
                                start = first,
                                end = last,
                                strokeWidth = with(LocalDensity.current) { 2.5.dp.toPx() }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Cell radius for hit testing
    private var cellRadius = 0f
}
