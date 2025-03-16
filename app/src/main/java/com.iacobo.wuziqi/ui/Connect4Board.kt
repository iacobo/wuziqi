package com.iacobo.wuziqi.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.viewmodel.Position
import kotlinx.coroutines.launch

/**
 * Connect 4 board implementation (7x6 Easter Egg).
 * Features a traditional Connect 4 board with actual holes where you can see the 
 * pieces drop and stack behind the board.
 */
@Composable
fun Connect4Board(
    gameState: GameState,
    lastPlacedPosition: Position?,
    isDarkTheme: Boolean,
    isGameFrozen: Boolean,
    onColumnClick: (Int) -> Unit
) {
    val boardColor = Color(0xFF1565C0) // Connect 4 blue board color
    val backgroundColor = MaterialTheme.colorScheme.background
    val boardSize = gameState.boardSize
    val boardHeight = 6 // 6 rows for Connect 4 (7x6 grid)
    
    // Track animation states for "dropping" pieces
    val droppingAnimations = remember { mutableStateMapOf<Pair<Int, Int>, Animatable<Float, AnimationVector1D>>() }
    
    // Track if an animation is currently in progress
    val isAnimating = remember { mutableStateOf(false) }
    
    // Use a coroutine scope for animations
    val coroutineScope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .aspectRatio(7f/6f) // Aspect ratio for 7x6 board
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor) // Use background color behind the board
    ) {
        // First layer: Pieces that will be visible through the holes
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            for (row in 0 until boardHeight) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until boardSize) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Only render pieces that have been placed
                            if (gameState.board[row][col] != GameState.EMPTY) {
                                val cellPosition = row to col
                                
                                // Create animation for newly placed pieces
                                LaunchedEffect(key1 = lastPlacedPosition, key2 = gameState.board[row][col]) {
                                    if (lastPlacedPosition?.col == col && 
                                        !droppingAnimations.containsKey(cellPosition)) {
                                        // Create new animation
                                        droppingAnimations[cellPosition] = Animatable(0f)
                                        isAnimating.value = true
                                        
                                        // Animate directly in a LaunchedEffect
                                        droppingAnimations[cellPosition]?.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(
                                                durationMillis = 500,
                                                easing = BounceEasing
                                            )
                                        )
                                        
                                        // Animation complete
                                        isAnimating.value = false
                                    }
                                }
                                
                                // Calculate offset for animation
                                val yOffset = droppingAnimations[cellPosition]?.value ?: 1f
                                
                                // Game piece with animation
                                val pieceColor = when (gameState.board[row][col]) {
                                    GameState.PLAYER_ONE -> Color.Red
                                    else -> Color(0xFFFFD700) // Gold/Yellow
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(35.dp)
                                        .offset(
                                            y = if (yOffset < 1f) {
                                                (-200 * (1f - yOffset)).dp
                                            } else {
                                                0.dp
                                            }
                                        )
                                        .clip(CircleShape)
                                        .background(pieceColor)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Second layer: Board with circular holes
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBoard(
                boardColor = boardColor,
                backgroundColor = backgroundColor,
                boardWidth = size.width,
                boardHeight = size.height,
                columns = boardSize,
                rows = boardHeight
            )
        }
        
        // Third layer: Clickable column overlays
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (col in 0 until boardSize) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            enabled = !isGameFrozen && !isAnimating.value
                        ) {
                            if (!isAnimating.value) {
                                onColumnClick(col)
                            }
                        }
                )
            }
        }
    }
}

/**
 * Draws the Connect 4 board with circular holes using Canvas.
 */
private fun DrawScope.drawBoard(
    boardColor: Color,
    backgroundColor: Color,
    boardWidth: Float,
    boardHeight: Float,
    columns: Int,
    rows: Int
) {
    // Draw the base board
    drawRect(color = boardColor)
    
    // Calculate circle size and spacing
    val hPadding = boardWidth * 0.05f  // 5% horizontal padding
    val vPadding = boardHeight * 0.05f // 5% vertical padding
    val availableWidth = boardWidth - (2 * hPadding)
    val availableHeight = boardHeight - (2 * vPadding)
    
    val cellWidth = availableWidth / columns
    val cellHeight = availableHeight / rows
    
    val circleRadius = minOf(cellWidth, cellHeight) * 0.4f // 80% of the smaller dimension

    // Draw the circular holes (where background shows through)
    for (row in 0 until rows) {
        for (col in 0 until columns) {
            val centerX = hPadding + (col * cellWidth) + (cellWidth / 2)
            val centerY = vPadding + (row * cellHeight) + (cellHeight / 2)
            
            drawCircle(
                color = backgroundColor,
                radius = circleRadius,
                center = Offset(centerX, centerY)
            )
        }
    }
}

// Custom bounce easing for Connect 4 pieces
private val BounceEasing = CubicBezierEasing(0.17f, 0.67f, 0.83f, 1.2f)