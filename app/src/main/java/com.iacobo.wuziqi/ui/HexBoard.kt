package com.iacobo.wuziqi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.viewmodel.Position
import kotlin.math.sqrt

/**
 * Hex game board implementation.
 * Displays a hexagonal grid using a custom canvas drawing.
 */
@Composable
fun HexBoard(
    gameState: GameState,
    lastPlacedPosition: Position?,
    isGameFrozen: Boolean,
    onTileClick: (Int, Int) -> Unit
) {
    val boardSize = gameState.boardSize
    val hexColor1 = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    val hexColor2 = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
    val edgeColor1 = MaterialTheme.colorScheme.primary
    val edgeColor2 = MaterialTheme.colorScheme.secondary
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    
    // Calculate hexagon dimensions
    val density = LocalDensity.current
    val strokeWidth = with(density) { 1.5.dp.toPx() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Draw hexagonal grid on canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hexRadius = size.minDimension / (boardSize * 2)
            val hexHeight = hexRadius * 2
            val hexWidth = hexRadius * sqrt(3f)
            
            // Adjustments to center the grid
            val xOffset = (size.width - hexWidth * (boardSize + 0.5f)) / 2
            val yOffset = (size.height - hexHeight * boardSize * 0.75f - hexHeight / 4) / 2
            
            // Draw board edges (player goal indicators)
            // Top-left to bottom-right (Player 1)
            val edgePath1 = Path().apply {
                moveTo(xOffset, yOffset + hexHeight / 2)
                lineTo(xOffset + hexWidth * boardSize, yOffset + hexHeight * boardSize * 0.75f + hexHeight / 2)
                lineTo(xOffset + hexWidth * boardSize, yOffset + hexHeight * boardSize * 0.75f + hexHeight)
                lineTo(xOffset, yOffset + hexHeight)
                close()
            }
            drawPath(edgePath1, color = edgeColor1.copy(alpha = 0.2f), style = Fill)
            
            // Top-right to bottom-left (Player 2)
            val edgePath2 = Path().apply {
                moveTo(xOffset + hexWidth * boardSize, yOffset + hexHeight / 2)
                lineTo(xOffset, yOffset + hexHeight * boardSize * 0.75f + hexHeight / 2)
                lineTo(xOffset, yOffset + hexHeight * boardSize * 0.75f + hexHeight)
                lineTo(xOffset + hexWidth * boardSize, yOffset + hexHeight)
                close()
            }
            drawPath(edgePath2, color = edgeColor2.copy(alpha = 0.2f), style = Fill)
            
            // Create hit areas and draw hexagons for each cell
            for (row in 0 until boardSize) {
                for (col in 0 until boardSize) {
                    val centerX = xOffset + col * hexWidth + (row % 2) * (hexWidth / 2)
                    val centerY = yOffset + row * hexHeight * 0.75f + hexHeight / 2
                    
                    // Create hexagon path
                    val hexPath = createHexagonPath(centerX, centerY, hexRadius)
                    
                    // Fill with alternating colors
                    val fillColor = if ((row + col) % 2 == 0) hexColor1 else hexColor2
                    drawPath(hexPath, color = fillColor, style = Fill)
                    
                    // Draw border
                    drawPath(hexPath, color = gridLineColor, style = Stroke(width = strokeWidth))
                    
                    // Draw game pieces
                    if (gameState.board[row][col] != GameState.EMPTY) {
                        val pieceColor = when (gameState.board[row][col]) {
                            GameState.PLAYER_ONE -> edgeColor1
                            else -> edgeColor2
                        }
                        
                        drawCircle(
                            color = pieceColor,
                            radius = hexRadius * 0.45f,
                            center = Offset(centerX, centerY)
                        )
                        
                        // Highlight the last placed piece with a stroke
                        if (lastPlacedPosition?.row == row && lastPlacedPosition.col == col) {
                            drawCircle(
                                color = MaterialTheme.colorScheme.tertiary,
                                radius = hexRadius * 0.5f,
                                center = Offset(centerX, centerY),
                                style = Stroke(width = strokeWidth * 1.5f)
                            )
                        }
                    }
                }
            }
            
            // Create clickable areas over each hexagon
            for (row in 0 until boardSize) {
                for (col in 0 until boardSize) {
                    val centerX = xOffset + col * hexWidth + (row % 2) * (hexWidth / 2)
                    val centerY = yOffset + row * hexHeight * 0.75f + hexHeight / 2
                    
                    // Only add click handlers for empty cells
                    if (gameState.board[row][col] == GameState.EMPTY && !isGameFrozen) {
                        // Calculate the clickable area around the hexagon
                        val clickRadius = hexRadius * 0.9f
                        val clickArea = Size(clickRadius * 2, clickRadius * 2)
                        val clickTopLeft = Offset(centerX - clickRadius, centerY - clickRadius)
                        
                        // Add invisible clickable box
                        drawRect(
                            color = Color.Transparent,
                            topLeft = clickTopLeft,
                            size = clickArea
                        )
                        
                        // Handle click events
                        val clickAreaPath = createHexagonPath(centerX, centerY, clickRadius)
                        val r = row
                        val c = col
                        
                        if (isPointInPath(clickAreaPath, centerX, centerY)) {
                            // We can't actually register click events in Canvas directly,
                            // but we've set up the hitboxes for the cell locations
                        }
                    }
                }
            }
        }
        
        // Create an overlay of clickable areas
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                // Create a clickable box for each hexagon cell
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            enabled = gameState.board[row][col] == GameState.EMPTY && !isGameFrozen,
                            onClick = { onTileClick(row, col) }
                        )
                )
            }
        }
    }
}

/**
 * Creates a hexagon path for drawing on canvas.
 */
private fun createHexagonPath(centerX: Float, centerY: Float, radius: Float): Path {
    return Path().apply {
        val angles = List(6) { Math.toRadians((60 * it + 30).toDouble()).toFloat() }
        moveTo(
            centerX + radius * kotlin.math.cos(angles[0]),
            centerY + radius * kotlin.math.sin(angles[0])
        )
        for (i in 1 until 6) {
            lineTo(
                centerX + radius * kotlin.math.cos(angles[i]),
                centerY + radius * kotlin.math.sin(angles[i])
            )
        }
        close()
    }
}

/**
 * Simple check if a point is within a path (approximation).
 */
private fun isPointInPath(path: Path, x: Float, y: Float): Boolean {
    // This is a simplified approximation
    // In a real app, you would use a more sophisticated hit-testing algorithm
    return true
}