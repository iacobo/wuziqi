package com.iacobo.wuziqi.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.viewmodel.Position

/**
 * Connect 4 board implementation with actual holes and visible pieces.
 * Uses a simpler approach with a background and foreground layer
 * to make pieces visible through the holes.
 */
@Composable
fun Connect4Board(
    gameState: GameState,
    lastPlacedPosition: Position?,
    isGameFrozen: Boolean,
    onColumnClick: (Int) -> Unit
) {
    val boardColor = Color(0xFF1565C0) // Connect 4 blue board color
    val boardSize = gameState.boardSize
    val boardHeight = 6 // 6 rows for Connect 4 (7x6 grid)
    
    // Track animation states for "dropping" pieces
    val droppingAnimations = remember { mutableStateMapOf<Pair<Int, Int>, Animatable<Float, AnimationVector1D>>() }
    
    // Track if an animation is currently in progress
    val isAnimating = remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .aspectRatio(7f/6f) // Aspect ratio for 7x6 board
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(boardColor) // Board color as background
    ) {
        // Board content with pieces
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Create 6 rows
            for (row in 0 until boardHeight) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Create 7 columns
                    for (col in 0 until boardSize) {
                        // Create holes with pieces behind them
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Background piece (only visible if there's a piece in this position)
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
                                
                                // Piece that will be visible through the hole
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .offset(
                                            y = if (yOffset < 1f) {
                                                (-300 * (1f - yOffset)).dp
                                            } else {
                                                0.dp
                                            }
                                        )
                                        .clip(CircleShape)
                                        .background(pieceColor)
                                )
                            }
                            
                            // Hole frame (a blue ring)
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(boardColor)
                            ) {
                                // Transparent hole (inner cut-out)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .align(Alignment.Center)
                                        .clip(CircleShape)
                                        .background(Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Clickable column overlays
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

// Custom bounce easing for Connect 4 pieces
private val BounceEasing = CubicBezierEasing(0.17f, 0.67f, 0.83f, 1.2f)