package com.iacobo.wuziqi.ui

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.viewmodel.Position

/**
 * Connect 4 board implementation (7x6 Easter Egg).
 * Now featuring actual holes in the board and proper animations.
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
    val boardSize = gameState.boardSize
    val boardHeight = 6 // 6 rows for Connect4 (7x6 grid)
    
    // Piece size calculation based on available space
    val density = LocalDensity.current
    val pieceSize = 36.dp
    
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
            .background(boardColor)
    ) {
        // Main board structure with hole cutouts to see background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Board background - the material behind the holes
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.background)
            )
            
            // Game pieces - fall BEHIND the board front panel
            Column(
                modifier = Modifier.fillMaxSize(),
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
                            val cellPosition = row to col
                            
                            // Only create animations for pieces that exist
                            if (gameState.board[row][col] != GameState.EMPTY) {
                                // Create animations for newly placed pieces
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
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Calculate offset for animation
                                    val yOffset = droppingAnimations[cellPosition]?.value ?: 1f
                                    
                                    // Game piece
                                    val pieceColor = when (gameState.board[row][col]) {
                                        GameState.PLAYER_ONE -> Color.Red
                                        else -> Color(0xFFFFD700) // Gold/Yellow
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(pieceSize)
                                            .offset(
                                                y = if (yOffset < 1f) {
                                                    (-pieceSize.value * (1f - yOffset) * boardHeight).dp
                                                } else {
                                                    0.dp
                                                }
                                            )
                                            .clip(CircleShape)
                                            .background(pieceColor)
                                    )
                                }
                            } else {
                                // Empty space for empty cells
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }
            
            // Board front panel with holes cut out
            Column(
                modifier = Modifier.fillMaxSize(),
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
                                    .aspectRatio(1f)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Create a board piece with a circular hole cut out
                                Box(
                                    modifier = Modifier
                                        .size(pieceSize)
                                        .clip(CircleShape)
                                        .background(boardColor)
                                        .padding(1.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Clickable column overlays (on top)
            Row(
                modifier = Modifier.fillMaxSize()
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
            
            // Drop indicator for hover effect could be added here
        }
    }
}

// Custom bounce easing for Connect 4 pieces
private val BounceEasing = CubicBezierEasing(0.17f, 0.67f, 0.83f, 1.2f)