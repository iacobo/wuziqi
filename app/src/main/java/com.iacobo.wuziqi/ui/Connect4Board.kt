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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.viewmodel.Position

/**
 * Connect 4 board implementation with proper cutout holes.
 * Uses Porter/Duff blend modes to create transparent holes in the board
 * so pieces are visible through them.
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
    ) {
        // Layer 1: Background and pieces layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Game pieces are positioned here using absolute positioning to match hole coordinates exactly
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                // Access the constraints to get the actual size of the container
                val widthPx = constraints.maxWidth.toFloat()
                val heightPx = constraints.maxHeight.toFloat()
                val hPaddingPx = widthPx * 0.1f
                val vPaddingPx = heightPx * 0.1f
                val cellWidthPx = (widthPx - (2 * hPaddingPx)) / boardSize
                val cellHeightPx = (heightPx - (2 * vPaddingPx)) / boardHeight
                val pieceDiameterPx = minOf(cellWidthPx, cellHeightPx) * 0.8f
                val pieceDiameterDp = with(LocalDensity.current) { pieceDiameterPx.toDp() }

                for (row in 0 until boardHeight) {
                    for (col in 0 until boardSize) {
                        // Only render pieces that exist in the game state
                        if (gameState.board[row][col] != GameState.EMPTY) {
                            val cellPosition = row to col
                            
                            // Calculate exact center position matching the hole cutouts
                            val centerX = with(LocalDensity.current) { 
                                (hPaddingPx + (col * cellWidthPx) + (cellWidthPx / 2)).toDp() 
                            }
                            val centerY = with(LocalDensity.current) { 
                                (vPaddingPx + (row * cellHeightPx) + (cellHeightPx / 2)).toDp() 
                            }
                            
                            // Set up animation for newly placed pieces
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
                            
                            // Calculate animation offset
                            val yOffset = droppingAnimations[cellPosition]?.value ?: 1f
                            
                            // Determine piece color
                            val pieceColor = when (gameState.board[row][col]) {
                                GameState.PLAYER_ONE -> Color.Red
                                else -> Color(0xFFFFD700) // Gold/Yellow
                            }
                            
                            // Position piece exactly at the hole center using absoluteOffset
                            Box(
                                modifier = Modifier
                                    .size(pieceDiameterDp)
                                    .absoluteOffset(
                                        x = centerX - (pieceDiameterDp / 2),
                                        y = if (yOffset < 1f) {
                                            centerY - (pieceDiameterDp / 2) - 
                                            with(LocalDensity.current) { (300f * (1f - yOffset)).toDp() }
                                        } else {
                                            centerY - (pieceDiameterDp / 2)
                                        }
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        // Add a slightly darker ring effect with a gradient 
                                        color = when (gameState.board[row][col]) {
                                            GameState.PLAYER_ONE -> Color.Red.copy(alpha = 0.85f) // Lighter red for main piece
                                            else -> Color(0xFFFFD700).copy(alpha = 0.85f) // Lighter gold for main piece
                                        }
                                    )
                            ) {
                                // Inner part of piece (creates a ring effect)
                                Box(
                                    modifier = Modifier
                                        .size(pieceDiameterDp * 0.9f)
                                        .align(Alignment.Center)
                                        .clip(CircleShape)
                                        .background(pieceColor)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Layer 2: Board with cutout holes
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .background(boardColor)
                .drawWithContent {
                    drawContent()
                    
                    // Define hole dimensions
                    val columns = boardSize
                    val rows = boardHeight
                    val boardWidth = size.width
                    val boardHeight = size.height
                    
                    // FIX: Increase padding to better align pieces with holes
                    val hPadding = boardWidth * 0.1f // Increased from 0.05f to 0.1f (10% padding)
                    val vPadding = boardHeight * 0.1f // Increased from 0.05f to 0.1f (10% padding)
                    
                    val cellWidth = (boardWidth - (2 * hPadding)) / columns
                    val cellHeight = (boardHeight - (2 * vPadding)) / rows
                    
                    val holeRadius = minOf(cellWidth, cellHeight) * 0.4f
                    
                    // Cut out holes from the board
                    for (row in 0 until rows) {
                        for (col in 0 until columns) {
                            val centerX = hPadding + (col * cellWidth) + (cellWidth / 2)
                            val centerY = vPadding + (row * cellHeight) + (cellHeight / 2)
                            
                            // Draw a circle that will be cut out with BlendMode.DstOut
                            drawCircle(
                                color = Color.White, // Color doesn't matter for cutout
                                radius = holeRadius,
                                center = Offset(centerX, centerY),
                                blendMode = BlendMode.DstOut // This creates the cutout effect
                            )
                        }
                    }
                }
        )
        
        // Layer 3: Clickable column overlays (invisible)
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
    }
}

// Custom bounce easing for Connect 4 pieces
private val BounceEasing = CubicBezierEasing(0.17f, 0.67f, 0.83f, 1.2f)