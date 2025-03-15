package com.iacobo.wuziqi.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iacobo.wuziqi.R
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.data.ThemeMode
import com.iacobo.wuziqi.ui.theme.BoardDarkColor
import com.iacobo.wuziqi.ui.theme.BoardLightColor
import com.iacobo.wuziqi.ui.theme.GridDarkColor
import com.iacobo.wuziqi.ui.theme.GridLightColor
import com.iacobo.wuziqi.viewmodel.DRAW
import com.iacobo.wuziqi.viewmodel.GameViewModel
import com.iacobo.wuziqi.viewmodel.Position
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main game screen composable.
 * Displays the game board, status, and controls.
 * Supports standard game and easter eggs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToHome: () -> Unit,
    themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val gameState = viewModel.gameState
    val winner = viewModel.winner
    val lastPlacedPosition = viewModel.lastPlacedPosition
    val moveHistory = viewModel.moveHistory
    val isLoading = viewModel.isLoading
    val boardSize = gameState.boardSize
    val winLength = gameState.winCondition
    
    // Determine game type for easter eggs
    val isXandO = boardSize == 3 && winLength == 3
    val isConnect4 = boardSize == 7 && winLength == 4

    // Determine app title based on easter egg mode
    val appTitle = when {
        isXandO -> "X's & O's"
        isConnect4 -> "Connect 4"
        else -> stringResource(R.string.app_name)
    }

    // Determine if we're in dark theme based on the theme mode
    val isDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = appTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    // Home button
                    IconButton(onClick = onNavigateToHome) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = stringResource(R.string.home)
                        )
                    }
                    
                    // Undo button (disabled if no moves or game frozen)
                    IconButton(
                        onClick = { viewModel.undoMove() },
                        enabled = moveHistory.isNotEmpty() && !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.undo)
                        )
                    }
                    
                    // Reset button
                    IconButton(onClick = { viewModel.resetGame() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.reset)
                        )
                    }
                    
                    Spacer(Modifier.weight(1f))
                    
                    // Settings button (right aligned)
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fixed height container for player indicator to prevent layout shifts
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.computer_thinking),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else if (winner == DRAW) {
                    // Show draw message
                    Text(
                        text = stringResource(R.string.draw),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (winner != null) {
                    // Show winner
                    val winnerText = when {
                        isXandO -> if (winner == GameState.PLAYER_ONE) "X Wins!" else "O Wins!"
                        isConnect4 -> if (winner == GameState.PLAYER_ONE) "Red Wins!" else "Yellow Wins!"
                        else -> stringResource(
                            R.string.winner_format,
                            if (winner == GameState.PLAYER_ONE) 
                                stringResource(R.string.player_black)
                            else 
                                stringResource(R.string.player_white)
                        )
                    }
                    
                    val winnerColor = when {
                        isXandO -> if (winner == GameState.PLAYER_ONE) Color.Black else Color.White
                        isConnect4 -> if (winner == GameState.PLAYER_ONE) Color.Red else Color(0xFFFFD700) // Gold
                        else -> if (winner == GameState.PLAYER_ONE) 
                            Color.Black
                        else 
                            Color.White
                    }
                    
                    Text(
                        text = winnerText,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (winner == GameState.PLAYER_TWO && !isConnect4) 
                            MaterialTheme.colorScheme.onBackground // Make white text visible
                        else 
                            winnerColor
                    )
                } else {
                    // Show current player
                    val playerText = when {
                        isXandO -> if (gameState.currentPlayer == GameState.PLAYER_ONE) "X's Turn" else "O's Turn"
                        isConnect4 -> if (gameState.currentPlayer == GameState.PLAYER_ONE) "Red's Turn" else "Yellow's Turn"
                        else -> stringResource(
                            R.string.player_turn_format,
                            if (gameState.currentPlayer == GameState.PLAYER_ONE) 
                                stringResource(R.string.player_black)
                            else 
                                stringResource(R.string.player_white)
                        )
                    }
                    
                    val playerColor = when {
                        isXandO -> if (gameState.currentPlayer == GameState.PLAYER_ONE) Color.Black else Color.White
                        isConnect4 -> if (gameState.currentPlayer == GameState.PLAYER_ONE) Color.Red else Color(0xFFFFD700) // Gold
                        else -> if (gameState.currentPlayer == GameState.PLAYER_ONE) 
                            Color.Black
                        else 
                            Color.White
                    }
                    
                    Text(
                        text = playerText,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (gameState.currentPlayer == GameState.PLAYER_TWO && !isConnect4) 
                            MaterialTheme.colorScheme.onBackground // Make white text visible
                        else 
                            playerColor
                    )
                }
            }

            // Game Board - fills remaining space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isXandO -> TicTacToeBoard(
                        gameState = gameState,
                        lastPlacedPosition = lastPlacedPosition,
                        isDarkTheme = isDarkTheme,
                        isGameFrozen = winner != null || isLoading,
                        onTileClick = { row, col ->
                            viewModel.placeTile(row, col)
                        }
                    )
                    isConnect4 -> Connect4Board(
                        gameState = gameState,
                        lastPlacedPosition = lastPlacedPosition,
                        isDarkTheme = isDarkTheme,
                        isGameFrozen = winner != null || isLoading,
                        onColumnClick = { col ->
                            viewModel.placeConnect4Tile(col)
                        }
                    )
                    else -> GameBoard(
                        gameState = gameState,
                        lastPlacedPosition = lastPlacedPosition,
                        isDarkTheme = isDarkTheme,
                        isGameFrozen = winner != null || isLoading,
                        onTileClick = { row, col ->
                            viewModel.placeTile(row, col)
                        }
                    )
                }
            }
        }
    }
}

/**
 * The standard Wuziqi game board composable.
 * Displays the grid and pieces.
 * Supports variable board sizes with dynamic gridline padding.
 */
@Composable
fun GameBoard(
    gameState: GameState,
    lastPlacedPosition: Position?,
    isDarkTheme: Boolean,
    isGameFrozen: Boolean,
    onTileClick: (Int, Int) -> Unit
) {
    // Use theme-appropriate colors for grid lines and board background
    val gridLineColor = if (isDarkTheme) GridDarkColor else GridLightColor
    val boardColor = if (isDarkTheme) BoardDarkColor else BoardLightColor
    val gridLineWidth = 1.dp
    val boardSize = gameState.boardSize
    
    // Adjust piece size based on board size
    val pieceSize = when {
        boardSize <= 10 -> 32.dp
        boardSize <= 13 -> 28.dp
        boardSize <= 15 -> 24.dp
        boardSize <= 17 -> 20.dp
        else -> 18.dp
    }
    
    // Calculate tile width for proper padding
    val tileWidth = (100f / boardSize).dp
    val dynamicPadding = (tileWidth / 2)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(8.dp)
            .background(boardColor)
    ) {
        // Draw gridlines with dynamic padding
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(dynamicPadding)
        ) {
            // Horizontal grid lines
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until boardSize) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridLineWidth)
                            .background(gridLineColor)
                    )

                    if (i < boardSize - 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Vertical grid lines
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until boardSize) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(gridLineWidth)
                            .background(gridLineColor)
                    )

                    if (i < boardSize - 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Tiles and pieces
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0 until boardSize) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    for (col in 0 until boardSize) {
                        Tile(
                            state = gameState.board[row][col],
                            isLastPlaced = lastPlacedPosition?.row == row && lastPlacedPosition?.col == col,
                            pieceSize = pieceSize,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (!isGameFrozen) {
                                    onTileClick(row, col)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Standard tile representing a game piece.
 */
@Composable
fun Tile(
    state: Int,
    isLastPlaced: Boolean,
    pieceSize: androidx.compose.ui.unit.Dp = 24.dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (state != GameState.EMPTY) {
            Box(
                modifier = Modifier
                    .size(pieceSize)
                    .clip(CircleShape)
                    .background(
                        when (state) {
                            GameState.PLAYER_ONE -> Color.Black
                            else -> Color.White
                        }
                    )
                    .border(
                        width = if (isLastPlaced) 2.dp else 0.dp,
                        color = if (isLastPlaced) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Tic-Tac-Toe board implementation (3x3 Easter Egg).
 * Now with transparent background and no edge lines.
 */
@Composable
fun TicTacToeBoard(
    gameState: GameState,
    lastPlacedPosition: Position?,
    isDarkTheme: Boolean,
    isGameFrozen: Boolean,
    onTileClick: (Int, Int) -> Unit
) {
    val gridLineColor = if (isDarkTheme) GridDarkColor else GridLightColor
    val gridLineWidth = 4.dp // Thicker grid lines for tic-tac-toe
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(16.dp)
            // Transparent background
            .background(Color.Transparent)
    ) {
        // Draw just the internal grid lines, not the outer edges
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Vertical inner lines (2)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(gridLineWidth)
                    .align(Alignment.CenterStart)
                    .offset(x = (LocalDensity.current.density * 33).dp)
                    .background(gridLineColor)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(gridLineWidth)
                    .align(Alignment.CenterEnd)
                    .offset(x = -(LocalDensity.current.density * 33).dp)
                    .background(gridLineColor)
            )
            
            // Horizontal inner lines (2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridLineWidth)
                    .align(Alignment.TopCenter)
                    .offset(y = (LocalDensity.current.density * 33).dp)
                    .background(gridLineColor)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridLineWidth)
                    .align(Alignment.BottomCenter)
                    .offset(y = -(LocalDensity.current.density * 33).dp)
                    .background(gridLineColor)
            )
        }
        
        // Cells and pieces
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 3 rows
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // 3 columns
                    for (col in 0 until 3) {
                        // Cell without border
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable(enabled = !isGameFrozen) {
                                    onTileClick(row, col)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (gameState.board[row][col]) {
                                GameState.PLAYER_ONE -> {
                                    // Draw X with lines - using BLACK to match standard pieces
                                    Canvas(
                                        modifier = Modifier
                                            .size(60.dp) // Larger size
                                            .padding(6.dp)
                                    ) {
                                        val canvasWidth = size.width
                                        val canvasHeight = size.height
                                        val strokeWidth = 10f // Thicker lines
                                        
                                        // Draw X using two lines
                                        drawLine(
                                            color = Color.Black, // Pure black like standard pieces
                                            start = Offset(0f, 0f),
                                            end = Offset(canvasWidth, canvasHeight),
                                            strokeWidth = strokeWidth,
                                            cap = StrokeCap.Round
                                        )
                                        
                                        drawLine(
                                            color = Color.Black,
                                            start = Offset(canvasWidth, 0f),
                                            end = Offset(0f, canvasHeight),
                                            strokeWidth = strokeWidth,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                                GameState.PLAYER_TWO -> {
                                    // Draw O as a circle - using WHITE to match standard pieces
                                    Canvas(
                                        modifier = Modifier
                                            .size(60.dp) // Larger size
                                            .padding(6.dp)
                                    ) {
                                        val canvasWidth = size.width
                                        val canvasHeight = size.height
                                        val strokeWidth = 10f // Thicker stroke
                                        
                                        // Draw O as a circle with stroke
                                        drawCircle(
                                            color = Color.White, // Pure white like standard pieces
                                            radius = (canvasWidth / 2) - (strokeWidth / 2),
                                            style = Stroke(width = strokeWidth)
                                        )
                                    }
                                }
                                else -> {
                                    // Empty cell
                                }
                            }
                            
                            // Highlight last placed position
                            if (lastPlacedPosition?.row == row && lastPlacedPosition.col == col) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            shape = RectangleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Connect 4 board implementation (7x7 Easter Egg).
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
    val boardSize = gameState.boardSize
    val scope = rememberCoroutineScope()
    
    // Track animation states for "dropping" pieces
    val droppingAnimations = remember { mutableStateMapOf<Pair<Int, Int>, Animatable<Float, AnimationVector1D>>() }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(boardColor)
    ) {
        // Main grid with pieces
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Rows
            for (row in 0 until boardSize) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Columns
                    for (col in 0 until boardSize) {
                        val cellPosition = row to col
                        
                        // Create animation if it's a newly placed piece
                        if (gameState.board[row][col] != GameState.EMPTY && 
                            !droppingAnimations.containsKey(cellPosition) &&
                            lastPlacedPosition?.col == col) {
                            droppingAnimations[cellPosition] = Animatable(0f)
                            
                            // Start animation
                            scope.launch {
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
                            // Empty slot (always visible)
                            Box(
                                modifier = Modifier
                                    .size(pieceSize)
                                    .clip(CircleShape)
                                    .background(emptySlotColor)
                            )
                            
                            // Game piece (if not empty)
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
                                                (-pieceSize.value * (1f - yOffset)).dp
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
        
        // Clickable columns
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            for (col in 0 until boardSize) {
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