package com.iacobo.wuziqi.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.ui.theme.Connect4BoardBlue
import com.iacobo.wuziqi.ui.theme.Connect4PieceRed
import com.iacobo.wuziqi.ui.theme.Connect4PieceRedBright
import com.iacobo.wuziqi.ui.theme.Connect4PieceYellow
import com.iacobo.wuziqi.ui.theme.Connect4PieceYellowBright
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
    val boardSize = gameState.boardSize
    val boardHeight = 6 // 6 rows for Connect 4 (7x6 grid)

    // Track animation states for "dropping" pieces
    val droppingAnimations = remember { mutableStateMapOf<Pair<Int, Int>, Animatable<Float, AnimationVector1D>>() }

    // Track if an animation is currently in progress
    val isAnimating = remember { mutableStateOf(false) }

    // Define the padding percentage for both holes and pieces
    val paddingPercent = 0.05f

    // Track board dimensions for calculating the padding in dp
    var boardWidthPx by remember { mutableIntStateOf(0) }
    var boardHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .aspectRatio(7f/6f) // Aspect ratio for 7x6 board
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .onGloballyPositioned { coordinates ->
                boardWidthPx = coordinates.size.width
                boardHeightPx = coordinates.size.height
            }
    ) {
        // Calculate padding in dp based on board size
        val horizontalPaddingDp = with(density) { (boardWidthPx * paddingPercent).toDp() }
        val verticalPaddingDp = with(density) { (boardHeightPx * paddingPercent).toDp() }

        // Layer 1: Background and pieces layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Game pieces are positioned with Row/Column layout with proper padding
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = horizontalPaddingDp,
                        end = horizontalPaddingDp,
                        top = verticalPaddingDp,
                        bottom = verticalPaddingDp
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    for (row in 0 until boardHeight) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until boardSize) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Only render pieces that exist in the game state
                                    if (gameState.board[row][col] != GameState.EMPTY) {
                                        val cellPosition = row to col

                                        // Set up animation for newly placed pieces
                                        LaunchedEffect(lastPlacedPosition, gameState.board[row][col]) {
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

                                        // Determine piece color using resources instead of hardcoded values
                                        val pieceColor = when (gameState.board[row][col]) {
                                            GameState.PLAYER_ONE -> Connect4PieceRed
                                            else -> Connect4PieceYellow
                                        }

                                        // Inner color slightly more saturated for contrast
                                        val innerPieceColor = when (gameState.board[row][col]) {
                                            GameState.PLAYER_ONE -> Connect4PieceRedBright
                                            else -> Connect4PieceYellowBright
                                        }

                                        // The actual game piece with animation
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .offset(
                                                    y = if (yOffset < 1f) {
                                                        (-300 * (1f - yOffset)).dp
                                                    } else {
                                                        0.dp
                                                    }
                                                )
                                                .clip(CircleShape)
                                                .background(
                                                    color = pieceColor.copy(alpha = 0.85f)
                                                )
                                        ) {
                                            // Inner part of piece (creates a ring effect)
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .align(Alignment.Center)
                                                    .clip(CircleShape)
                                                    .background(innerPieceColor)
                                            )
                                        }
                                    }
                                }
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
                .background(Connect4BoardBlue)
                .drawWithContent {
                    drawContent()

                    // Define hole dimensions
                    val columns = boardSize
                    val rows = boardHeight
                    val boardWidth = size.width
                    val boardHeight = size.height

                    // Use the same padding percentage as pieces
                    val hPadding = boardWidth * paddingPercent
                    val vPadding = boardHeight * paddingPercent

                    val cellWidth = (boardWidth - (2 * hPadding)) / columns
                    val cellHeight = (boardHeight - (2 * vPadding)) / rows

                    // 40% of cell size for hole radius to leave good borders
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