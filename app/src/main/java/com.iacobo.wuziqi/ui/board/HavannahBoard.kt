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

    /**
     * Represents a cubic hexagonal coordinate system with (q, r, s) where q + r + s = 0
     */
    private data class HexCoord(val q: Int, val r: Int, val s: Int = -q - r) {
        companion object {
            // Convert from offset coordinates (array indices) to cubic coordinates
            fun fromOffset(row: Int, col: Int, boardSize: Int): HexCoord {
                val q = col - boardSize / 2
                val r = row - boardSize / 2
                return HexCoord(q, r)
            }
            
            // Check if a hex coordinate is within the board boundary
            fun isValidHex(q: Int, r: Int, s: Int, boardSize: Int): Boolean {
                return maxOf(kotlin.math.abs(q), kotlin.math.abs(r), kotlin.math.abs(s)) <= boardSize / 2
            }
        }
        
        // Convert to offset coordinates (array indices) for storage
        fun toOffset(boardSize: Int): Pair<Int, Int> {
            val row = r + boardSize / 2
            val col = q + boardSize / 2
            return Pair(row, col)
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
        
        // Colors
        val playerOneColor = HexPieceRed
        val playerTwoColor = HexPieceBlue
        val backgroundColor = MaterialTheme.colorScheme.surface
        val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        val cornerColor = MaterialTheme.colorScheme.primaryContainer
        val edgeColor = MaterialTheme.colorScheme.secondaryContainer
        val centerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        val highlightColor = MaterialTheme.colorScheme.tertiary
        
        // Line widths
        val density = LocalDensity.current
        val strokeWidth = with(density) { 1.5.dp.toPx() }
        
        // Track board dimensions
        var boardWidthPx by remember { mutableStateOf(0f) }
        var boardHeightPx by remember { mutableStateOf(0f) }
        
        // Map to store hex centers and valid coordinates
        val hexCenters = remember { mutableMapOf<Pair<Int, Int>, Offset>() }
        val validHexes = remember { mutableSetOf<HexCoord>() }
        
        // Get winning path
        val winningPath = if (isGameFrozen) gameState.getWinningPath() else emptySet()
        
        // Win highlight colors
        val ringColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
        val bridgeColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
        val forkColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(8.dp) // Slightly reduced padding to give more room
                .background(backgroundColor)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isGameFrozen) {
                        if (!isGameFrozen) {
                            detectTapGestures { tapOffset ->
                                // Find the hex clicked on
                                var bestPosition: Pair<Int, Int>? = null
                                var bestDistance = Float.MAX_VALUE
                                
                                hexCenters.forEach { (position, center) ->
                                    val distance = sqrt((tapOffset.x - center.x).pow(2) + (tapOffset.y - center.y).pow(2))
                                    
                                    if (distance < bestDistance) {
                                        val (row, col) = position
                                        if (row in 0 until boardSize && 
                                            col in 0 until boardSize && 
                                            gameState.board[row][col] == GameState.EMPTY) {
                                            bestDistance = distance
                                            bestPosition = position
                                        }
                                    }
                                }
                                
                                bestPosition?.let { (row, col) ->
                                    val hexSize = minOf(boardWidthPx, boardHeightPx) / (boardSize * 1.8f)
                                    if (bestDistance < hexSize * 0.8f) {
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
                
                // Clear existing mappings
                hexCenters.clear()
                validHexes.clear()
                
                // Calculate hex size to fit the board - using a different formula
                // Dividing by boardSize * 1.8 gives us enough room for the entire board
                val hexRadius = minOf(boardWidthPx, boardHeightPx) / (boardSize * 1.8f)
                
                // Center of the canvas
                val centerX = boardWidthPx / 2
                val centerY = boardHeightPx / 2
                
                // The range needs to accommodate a full hexagonal board
                // A size 10 board has a radius of 5 (counting from 0), so we need to go from -5 to +5
                val hexRange = boardSize / 2
                
                // Generate all valid hex coordinates for a hexagonal board
                for (q in -hexRange..hexRange) {
                    for (r in maxOf(-hexRange, -q - hexRange)..minOf(hexRange, -q + hexRange)) {
                        val s = -q - r
                        
                        // Cubic coordinates must satisfy |q| + |r| + |s| <= boardSize
                        // But for a proper hexagon we use max(|q|, |r|, |s|) <= hexRange
                        if (maxOf(kotlin.math.abs(q), kotlin.math.abs(r), kotlin.math.abs(s)) <= hexRange) {
                            val hexCoord = HexCoord(q, r, s)
                            validHexes.add(hexCoord)
                            
                            // Convert to array indices
                            val arrayCoords = hexCoord.toOffset(boardSize)
                            
                            // Calculate pixel position for hex center
                            // Using proper hexagonal coordinates with a sqrt(3) factor
                            val x = centerX + hexRadius * 3/2 * q
                            val y = centerY + hexRadius * sqrt(3f) * (r + q/2f)
                            
                            // Store center position
                            hexCenters[arrayCoords] = Offset(x, y)
                            
                            // Create hexagon path
                            val hexPath = Path().apply {
                                for (i in 0 until 6) {
                                    val angle = Math.toRadians((60 * i).toDouble()).toFloat()
                                    val hx = x + hexRadius * cos(angle)
                                    val hy = y + hexRadius * sin(angle)
                                    
                                    if (i == 0) {
                                        moveTo(hx, hy)
                                    } else {
                                        lineTo(hx, hy)
                                    }
                                }
                                close()
                            }
                            
                            // Determine if this is a corner, edge, or inner cell
                            // For a hexagon, corners have two coordinates at ±hexRange
                            val isCorner = 
                                (q == -hexRange && r == hexRange) || // top-left
                                (q == 0 && r == hexRange) || // top
                                (q == hexRange && r == 0) || // top-right
                                (q == hexRange && r == -hexRange) || // bottom-right
                                (q == 0 && r == -hexRange) || // bottom
                                (q == -hexRange && r == 0) // bottom-left
                            
                            // An edge hex has exactly one coordinate at ±hexRange
                            val isEdge = !isCorner && (
                                kotlin.math.abs(q) == hexRange || 
                                kotlin.math.abs(r) == hexRange || 
                                kotlin.math.abs(s) == hexRange
                            )
                            
                            // Fill color based on position type
                            val fillColor = when {
                                isCorner -> cornerColor.copy(alpha = 0.7f)
                                isEdge -> edgeColor.copy(alpha = 0.5f)
                                else -> centerColor.copy(alpha = 0.3f)
                            }
                            
                            // Draw hexagon fill
                            drawPath(hexPath, color = fillColor, style = Fill)
                            
                            // Draw hexagon border
                            drawPath(hexPath, color = gridLineColor, style = Stroke(width = strokeWidth))
                            
                            // Draw game pieces if present
                            val (row, col) = arrayCoords
                            if (row in 0 until boardSize && 
                                col in 0 until boardSize && 
                                gameState.board[row][col] != GameState.EMPTY) {
                                
                                val pieceColor = if (gameState.board[row][col] == GameState.PLAYER_ONE) 
                                                    playerOneColor else playerTwoColor
                                
                                // Draw the piece
                                drawCircle(
                                    color = pieceColor,
                                    radius = hexRadius * 0.7f,
                                    center = Offset(x, y)
                                )
                                
                                // Highlight for last placed piece
                                if (lastPlacedPosition?.row == row && 
                                    lastPlacedPosition.col == col && 
                                    winningPath.isEmpty()) {
                                    drawCircle(
                                        color = highlightColor,
                                        radius = hexRadius * 0.85f,
                                        center = Offset(x, y),
                                        style = Stroke(width = strokeWidth * 1.5f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Highlight winning structures
                if (winningPath.isNotEmpty()) {
                    val winType = gameState.getWinningType()
                    val winHighlightColor = when (winType) {
                        1 -> ringColor
                        2 -> bridgeColor
                        3 -> forkColor
                        else -> highlightColor
                    }
                    
                    // Highlight all pieces in the winning path
                    for (pos in winningPath) {
                        val center = hexCenters[pos] ?: continue
                        
                        // Draw a glow under the piece
                        drawCircle(
                            color = winHighlightColor,
                            radius = hexRadius * 0.9f,
                            center = center,
                            alpha = 0.4f
                        )
                    }
                    
                    // Connect winning pieces
                    val pathList = winningPath.toList()
                    for (i in 0 until pathList.size - 1) {
                        val pos1 = pathList[i]
                        val pos2 = pathList[i + 1]
                        
                        val center1 = hexCenters[pos1]
                        val center2 = hexCenters[pos2]
                        
                        if (center1 != null && center2 != null) {
                            drawLine(
                                color = winHighlightColor,
                                start = center1,
                                end = center2,
                                strokeWidth = strokeWidth * 2.5f
                            )
                        }
                    }
                    
                    // Connect the loop for rings
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
}
