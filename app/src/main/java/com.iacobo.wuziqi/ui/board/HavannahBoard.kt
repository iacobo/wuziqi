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
        
        // Convert dp to px once here in the Composable context
        val density = LocalDensity.current
        val lineWidthPx = with(density) { 1.dp.toPx() }
        val thickLineWidthPx = with(density) { 2.5.dp.toPx() }
        val outlineWidthPx = with(density) { 2.dp.toPx() }
        
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
                
                // For a hexagon with side length 5, the radius in cubic coordinates is 5
                val HEX_SIDE = 5
                
                // Calculate cell size
                val availableWidth = canvasWidth - (2 * margin)
                val availableHeight = canvasHeight - (2 * margin)
                val cellWidth = availableWidth / (2 * HEX_SIDE + 1) // Add 1 for better fit
                val cellHeight = availableHeight / (2 * HEX_SIDE + 1) * sqrt(3f) 
                cellRadius = minOf(cellWidth, cellHeight) * 0.65f
                
                // Center of the canvas
                val centerX = canvasWidth / 2
                val centerY = canvasHeight / 2
                
                // Define all six corners of the hexagon in cubic coordinates
                val corners = listOf(
                    Triple(-HEX_SIDE, 0, HEX_SIDE),       // left
                    Triple(-HEX_SIDE, HEX_SIDE, 0),       // top-left
                    Triple(0, HEX_SIDE, -HEX_SIDE),       // top-right
                    Triple(HEX_SIDE, 0, -HEX_SIDE),       // right
                    Triple(HEX_SIDE, -HEX_SIDE, 0),       // bottom-right
                    Triple(0, -HEX_SIDE, HEX_SIDE)        // bottom-left
                )
                
                // Loop through all possible cubic coordinates within the hexagon
                for (q in -HEX_SIDE..HEX_SIDE) {
                    for (r in -HEX_SIDE..HEX_SIDE) {
                        val s = -q - r
                        
                        // Check if this point is inside the hexagon
                        // For a hexagon with side length n, the constraint is |q|, |r|, |s| <= n
                        if (maxOf(kotlin.math.abs(q), kotlin.math.abs(r), kotlin.math.abs(s)) > HEX_SIDE) {
                            continue
                        }
                        
                        // Convert to array indices (need to offset to positive values)
                        val row = r + HEX_SIDE
                        val col = q + HEX_SIDE
                        
                        // Calculate pixel position
                        // For flat-topped hexes, the standard formula is:
                        val x = centerX + cellRadius * 3/2 * q
                        val y = centerY + cellRadius * sqrt(3f) * (r + q/2f)
                        
                        // Store for hit testing
                        val position = Pair(row, col)
                        val center = Offset(x, y)
                        cellPositions[position] = center
                        
                        // Create the hexagon path
                        val hexPath = Path().apply {
                            for (i in 0 until 6) {
                                val angle = Math.toRadians((60 * i).toDouble()).toFloat()
                                val hx = x + cellRadius * cos(angle)
                                val hy = y + cellRadius * sin(angle)
                                
                                if (i == 0) {
                                    moveTo(hx, hy)
                                } else {
                                    lineTo(hx, hy)
                                }
                            }
                            close()
                        }
                        
                        // Determine if this is a corner, edge, or interior cell
                        val cubicCoord = Triple(q, r, s)
                        val isCorner = corners.contains(cubicCoord)
                        
                        // Edge cells have at least one coordinate at exactly ±side length
                        // but are not corners
                        val isEdge = !isCorner && (
                            kotlin.math.abs(q) == HEX_SIDE || 
                            kotlin.math.abs(r) == HEX_SIDE || 
                            kotlin.math.abs(s) == HEX_SIDE
                        )
                        
                        // Choose fill color
                        val fillColor = when {
                            isCorner -> cornerColor
                            isEdge -> edgeColor
                            else -> centerColor
                        }
                        
                        // Draw hex cell
                        drawPath(hexPath, color = fillColor, style = Fill)
                        drawPath(hexPath, color = gridLineColor, style = Stroke(width = lineWidthPx))
                        
                        // Draw game pieces if this position is within the game array
                        if (row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE) {
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
                                        style = Stroke(width = outlineWidthPx)
                                    )
                                }
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
                                strokeWidth = thickLineWidthPx
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
                                strokeWidth = thickLineWidthPx
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
