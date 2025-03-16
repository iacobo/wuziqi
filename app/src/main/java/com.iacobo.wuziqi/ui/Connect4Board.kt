package com.iacobo.wuziqi.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.viewmodel.Position
import kotlinx.coroutines.launch

/**
 * Connect 4 board implementation (7x6 Easter Egg).
 * This is a separate file to fix compilation issues.
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
    val emptySlotColor = MaterialTheme.colorScheme.background
    val pieceSize = 36.dp
    val boardWidth = gameState.boardSize
    val boardHeight = 6 // 6 rows for Connect4 (7x6 grid)
    
    // Use a coroutine scope
    val coroutineScope = rememberCoroutineScope()
    
    // Track animation states for "dropping" pieces
    val droppingAnimations = remember { mutableStateMapOf<Pair<Int, Int>, Animatable<Float, AnimationVector1D>>() }
    
    Box(
        modifier = Modifier
            .aspectRatio(7f/6f) // Aspect ratio for 7x6 board
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(boardColor)
    ) {
        // Main grid with pieces BEHIND the holes
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Rows (only use 6 rows for Connect4)
            for (row in 0 until boardHeight) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Columns
                    for (col in 0 until boardWidth) {
                        val cellPosition = row to col
                        
                        // Create animation if it's a newly placed piece
                        LaunchedEffect(key1 = lastPlacedPosition, key2 = gameState.board[row][col]) {
                            if (gameState.board[row][col] != GameState.EMPTY && 
                                !droppingAnimations.containsKey(cellPosition) &&
                                lastPlacedPosition?.col == col) {
                                droppingAnimations[cellPosition] = Animatable(0f)
                                
                                // Animate directly in a LaunchedEffect
                                droppingAnimations[cellPosition]?.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = 500,
                                        easing = BounceEasing
                                    )
                                )
                            }
                        }
                        
                        // Calculate offset for animation
                        val yOffset = droppingAnimations[cellPosition]?.value ?: 1f
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Game piece (if not empty) - this appears BEHIND the holes
                            if (gameState.board[row][col] != GameState.EMPTY) {
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
                        }
                    }
                }
            }
        }
        
        // Overlay with "holes" (using transparent circles with borders)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Rows of holes
            for (row in 0 until boardHeight) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Columns of holes
                    for (col in 0 until boardWidth) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Cut-out hole (transparent circle with border)
                            Box(
                                modifier = Modifier
                                    .size(pieceSize)
                                    .border(
                                        width = 2.dp,
                                        color = boardColor.copy(alpha = 0.7f),
                                        shape = CircleShape
                                    )
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            }
        }
        
        // Clickable columns (on top)
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            for (col in 0 until boardWidth) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(enabled = !isGameFrozen) {
                            onColumnClick(col)
                        }
                )
            }
        }
    }
}

// Custom bounce easing for Connect 4 pieces
private val BounceEasing = CubicBezierEasing(0.17f, 0.67f, 0.83f, 1.2f)