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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.viewmodel.Position
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Hex game board implementation.
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
            .aspectRatio(1f)
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isGameFrozen) {
                    if (!isGameFrozen) {
                        detectTapGestures { tapOffset ->
                            // Calculate which hex was tapped
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val hexRadius = minOf(canvasWidth, canvasHeight) / (boardSize * 2)
                            val hexHeight = hexRadius * 2
                            val hexWidth = hexRadius * sqrt(3f)
                            
                            // Center the grid
                            val xOffset = (canvasWidth - hexWidth * (boardSize + 0.5f)) / 2
                            val yOffset = (canvasHeight - hexHeight * boardSize * 0.75f - hexHeight / 4) / 2
                            
                            // Find which hex was clicked
                            for (row in 0 until boardSize) {
                                for (col in 0 until boardSize) {
                                    val centerX = xOffset + col * hexWidth + (row % 2) * (hexWidth / 2)
                                    val centerY = yOffset + row * hexHeight * 0.75f + hexHeight / 2
                                    
                                    // Simple distance check
                                    val distance = sqrt(
                                        (tapOffset.x - centerX).pow(2) + 
                                        (tapOffset.y - centerY).pow(2)
                                    )
                                    
                                    if (distance < hexRadius * 0.9f && 
                                        gameState.board[row][col] == GameState.EMPTY) {
                                        onTileClick(row, col)
                                        return@detectTapGestures
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            // Get canvas dimensions
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Calculate hex dimensions based on canvas size
            val hexRadius = minOf(canvasWidth, canvasHeight) / (boardSize * 2)
            val hexHeight = hexRadius * 2
            val hexWidth = hexRadius * sqrt(3f)
            
            // Center the grid within the canvas
            val xOffset = (canvasWidth - hexWidth * (boardSize + 0.5f)) / 2
            val yOffset = (canvasHeight - hexHeight * boardSize * 0.75f - hexHeight / 4) / 2
            
            // Draw hexagons for each board position
            for (row in 0 until boardSize) {
                for (col in 0 until boardSize) {
                    val centerX = xOffset + col * hexWidth + (row % 2) * (hexWidth / 2)
                    val centerY = yOffset + row * hexHeight * 0.75f + hexHeight / 2
                    
                    // Draw the hexagon
                    val hexPath = Path().apply {
                        val angles = List(6) { Math.toRadians((60 * it + 30).toDouble()).toFloat() }
                        moveTo(
                            centerX + hexRadius * kotlin.math.cos(angles[0]),
                            centerY + hexRadius * kotlin.math.sin(angles[0])
                        )
                        for (i in 1 until 6) {
                            lineTo(
                                centerX + hexRadius * kotlin.math.cos(angles[i]),
                                centerY + hexRadius * kotlin.math.sin(angles[i])
                            )
                        }
                        close()
                    }
                    
                    // Fill with alternating colors
                    val fillColor = if ((row + col) % 2 == 0) hexColor1 else hexColor2
                    drawPath(hexPath, color = fillColor, style = Fill)
                    
                    // Draw hexagon border
                    drawPath(hexPath, color = gridLineColor, style = Stroke(width = strokeWidth))
                    
                    // Draw game pieces if there are any
                    if (gameState.board[row][col] != GameState.EMPTY) {
                        val pieceColor = when (gameState.board[row][col]) {
                            GameState.PLAYER_ONE -> edgeColor1
                            else -> edgeColor2
                        }
                        
                        // Draw the game piece
                        drawCircle(
                            color = pieceColor,
                            radius = hexRadius * 0.45f,
                            center = Offset(centerX, centerY)
                        )
                        
                        // Highlight the last placed piece
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
        }
    }
}