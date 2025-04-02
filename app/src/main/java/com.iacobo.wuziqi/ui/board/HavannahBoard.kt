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
 * 
 * Havannah is played on a hexagonal grid in a hexagonal shape. Players win by creating:
 * 1. A ring (loop around one or more cells)
 * 2. A bridge (connection between any two corners)
 * 3. A fork (connection between any three edges)
 */
class HavannahBoard : GameBoard {

    // Represents a hexagonal coordinate with three axes (q, r, s where q+r+s=0)
    private data class HexCoord(val q: Int, val r: Int, val s: Int) {
        // Convert to array indices used for board storage
        fun toArrayCoords(boardSize: Int): Pair<Int, Int> {
            // Center the hexagon in the array consistently
            return Pair(r + (boardSize - 1) / 2, q + (boardSize - 1) / 2)
        }
        
        companion object {
            // Convert from array indices to hex coordinates
            fun fromArrayCoords(row: Int, col: Int, boardSize: Int): HexCoord {
                val q = col - (boardSize - 1) / 2
                val r = row - (boardSize - 1) / 2
                return HexCoord(q, r, -q-r)
            }
        }
    }

    @Composable
    override fun Render(
            gameState: GameState,
            lastPlacedPosition: Position?,
            isDarkTheme: Boolean,
            isGameFrozen: Boolean,
            onMoveSelected: (Int, Int) -> Unit
    ) {
        // Board configuration
        val boardSize = gameState.boardSize
        val boardRadius = (boardSize - 1) / 2
        
        // Colors
        val playerOneColor = HexPieceRed
        val playerTwoColor = HexPieceBlue
        val backgroundColor = MaterialTheme.colorScheme.surface
        val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        val cornerColor = MaterialTheme.colorScheme.primaryContainer
        val edgeColor = MaterialTheme.colorScheme.secondaryContainer
        val centerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        val highlightColor = MaterialTheme.colorScheme.tertiary
        
        // Win highlight colors based on win type
        val ringColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
        val bridgeColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
        val forkColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)

        // Line widths
        val density = LocalDensity.current
        val strokeWidth = with(density) { 1.5.dp.toPx() }
        
        // Track board dimensions
        var boardWidthPx by remember { mutableStateOf(0f) }
        var boardHeightPx by remember { mutableStateOf(0f) }
        
        // Track hex centers and valid coordinates
        val hexCenters = remember { mutableMapOf<Pair<Int, Int>, Offset>() }
        val validHexCoords = remember { mutableSetOf<HexCoord>() }
        
        // Get winning path
        val winningPath = if (isGameFrozen) gameState.getWinningPath() else emptySet()
        
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(16.dp)
                .background(backgroundColor)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isGameFrozen) {
                        if (!isGameFrozen) {
                            detectTapGestures { tapOffset ->
                                // Find the closest valid hex to the tap point
                                var bestPosition: Pair<Int, Int>? = null
                                var bestDistance = Float.MAX_VALUE
                                
                                // Check all valid hex positions
                                hexCenters.forEach { (position, center) ->
                                    val distance = sqrt((tapOffset.x - center.x).pow(2) + (tapOffset.y - center.y).pow(2))
                                    
                                    // Check if this is a valid and empty position
                                    val (row, col) = position
                                    if (row in 0 until gameState.boardSize && 
                                        col in 0 until gameState.boardSize && 
                                        gameState.board[row][col] == GameState.EMPTY &&
                                        distance < bestDistance) {
                                        
                                        bestDistance = distance
                                        bestPosition = position
                                    }
                                }
                                
                                // If we found a valid position within range, select it
                                bestPosition?.let { (row, col) ->
                                    val hexSize = minOf(boardWidthPx, boardHeightPx) / ((boardSize + 1) * 1.2f)
                                    // Use a more generous tap radius
                                    val tapRadius = hexSize * 1.2f  // Increased for easier selection
                                    if (bestDistance < tapRadius) {
                                        onMoveSelected(row, col)
                                    }
                                }
                            }
                        }
                    }
            ) {
                // Update dimensions
                boardWidthPx = size.width
                boardHeightPx = size.height
                
                // Clear existing data
                hexCenters.clear()
                validHexCoords.clear()
                
                // Calculate hex size to fit the board within the canvas
                // Increased margin for better fit
                val hexSize = minOf(boardWidthPx, boardHeightPx) / ((boardSize + 1) * 1.2f)
                
                // Center of the canvas
                val centerX = boardWidthPx / 2
                val centerY = boardHeightPx / 2
                
                // Generate all valid hex coordinates for a hexagonal board
                for (q in -boardSize..boardSize) {
                    for (r in -boardSize..boardSize) {
                        val s = -q - r
                        
                        // Check if this coordinate is within the hexagonal board
                        // Consistent with isValidHavannahPosition in AI
                        if (kotlin.math.abs(q) + kotlin.math.abs(r) + kotlin.math.abs(s) <= boardSize - 1) {
                            // Add to valid coordinates
                            validHexCoords.add(HexCoord(q, r, s))
                            
                            // Convert to array indices
                            val arrayCoords = HexCoord(q, r, s).toArrayCoords(boardSize)
                            
                            // Calculate pixel position
                            val x = centerX + hexSize * 1.5f * q
                            val y = centerY + hexSize * sqrt(3f) * (r + q/2f)
                            
                            // Store center for later use
                            hexCenters[arrayCoords] = Offset(x, y)
                            
                            // Create hexagon path
                            val hexPath = Path().apply {
                                for (i in 0 until 6) {
                                    val angle = Math.toRadians((60 * i).toDouble()).toFloat()
                                    val hx = x + hexSize * cos(angle)
                                    val hy = y + hexSize * sin(angle)
                                    
                                    if (i == 0) {
                                        moveTo(hx, hy)
                                    } else {
                                        lineTo(hx, hy)
                                    }
                                }
                                close()
                            }
                            
                            // Determine if this is a corner, edge, or inner cell
                            val isCorner = isCornerHex(q, r, s, boardRadius)
                            val isEdge = isEdgeHex(q, r, s, boardRadius) && !isCorner
                            
                            // Fill color based on position type
                            val fillColor = when {
                                isCorner -> cornerColor
                                isEdge -> edgeColor
                                else -> centerColor
                            }
                            
                            // Draw hexagon fill
                            drawPath(hexPath, color = fillColor, style = Fill)
                            
                            // Draw hexagon border
                            drawPath(hexPath, color = gridLineColor, style = Stroke(width = strokeWidth))
                            
                            // Draw piece if one exists at this position
                            val (row, col) = arrayCoords
                            if (row in 0 until gameState.boardSize && 
                                col in 0 until gameState.boardSize && 
                                gameState.board[row][col] != GameState.EMPTY) {
                                
                                val pieceColor = if (gameState.board[row][col] == GameState.PLAYER_ONE) 
                                                    playerOneColor else playerTwoColor
                                
                                // Draw the game piece
                                drawCircle(
                                    color = pieceColor,
                                    radius = hexSize * 0.65f,
                                    center = Offset(x, y)
                                )
                                
                                // Add a highlight for the last placed piece
                                if (lastPlacedPosition?.row == row && 
                                    lastPlacedPosition.col == col && 
                                    winningPath.isEmpty()) {
                                    drawCircle(
                                        color = highlightColor,
                                        radius = hexSize * 0.78f,
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
                    val winType = gameState.getWinningType()
                    val winHighlightColor = when (winType) {
                        1 -> ringColor    // Ring win
                        2 -> bridgeColor  // Bridge win
                        3 -> forkColor    // Fork win
                        else -> highlightColor
                    }
                    
                    // Highlight all pieces in the winning path
                    for (pos in winningPath) {
                        val center = hexCenters[pos] ?: continue
                        
                        // Draw a glow under the piece
                        drawCircle(
                            color = winHighlightColor,
                            radius = hexSize * 0.8f,
                            center = center,
                            alpha = 0.4f
                        )
                    }
                    
                    // Draw connection lines between winning pieces
                    val pathList = winningPath.toList()
                    for (i in 0 until pathList.size - 1) {
                        val pos1 = pathList[i]
                        val pos2 = pathList[i + 1]
                        
                        // Get the centers for these positions
                        val center1 = hexCenters[pos1]
                        val center2 = hexCenters[pos2]
                        
                        if (center1 != null && center2 != null) {
                            // Draw a connection line
                            drawLine(
                                color = winHighlightColor,
                                start = center1,
                                end = center2,
                                strokeWidth = strokeWidth * 2.5f
                            )
                        }
                    }
                    
                    // For rings, connect the last piece to the first
                    if (winType == 1 && pathList.size > 2) {
                        val firstPos = pathList.first()
                        val lastPos = pathList.last()
                        
                        val firstCenter = hexCenters[firstPos]
                        val lastCenter = hexCenters[lastPos]
                        
                        if (firstCenter != null && lastCenter != null) {
                            drawLine(
                                color = winHighlightColor,
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
    
    /**
     * Determines if a hex coordinate represents a corner of the board.
     */
    private fun isCornerHex(q: Int, r: Int, s: Int, boardRadius: Int): Boolean {
        return (q == -boardRadius && r == boardRadius && s == 0) || // Top-left
               (q == 0 && r == boardRadius && s == -boardRadius) || // Top-right
               (q == boardRadius && r == 0 && s == -boardRadius) || // Right
               (q == boardRadius && r == -boardRadius && s == 0) || // Bottom-right
               (q == 0 && r == -boardRadius && s == boardRadius) || // Bottom-left
               (q == -boardRadius && r == 0 && s == boardRadius)    // Left
    }
    
    /**
     * Determines if a hex coordinate is on an edge of the board.
     */
    private fun isEdgeHex(q: Int, r: Int, s: Int, boardRadius: Int): Boolean {
        return q == -boardRadius || r == -boardRadius || s == -boardRadius ||
               q == boardRadius || r == boardRadius || s == boardRadius
    }
}
