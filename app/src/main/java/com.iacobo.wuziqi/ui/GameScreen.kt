package com.iacobo.wuziqi.ui

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.iacobo.wuziqi.R
import com.iacobo.wuziqi.data.ThemeMode
import com.iacobo.wuziqi.ui.theme.Connect4PieceRed
import com.iacobo.wuziqi.ui.theme.Connect4PieceYellow
import com.iacobo.wuziqi.ui.theme.HexPieceBlue
import com.iacobo.wuziqi.ui.theme.HexPieceRed
import com.iacobo.wuziqi.viewmodel.DRAW
import com.iacobo.wuziqi.viewmodel.GameViewModel

/**
 * Main game screen composable. Displays the game board, status, and controls. Supports standard
 * game and easter eggs. Now with orientation-aware side bars in landscape mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
        viewModel: GameViewModel,
        onNavigateToSettings: () -> Unit,
        onNavigateToHome: () -> Unit,
        themeMode: ThemeMode = ThemeMode.SYSTEM
) {
        var showResetConfirmation by remember { mutableStateOf(false) }

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
        val isHex = boardSize == 11 && winLength == 8

        // Determine app title based on easter egg mode
        val appTitle =
                when {
                        isXandO -> stringResource(R.string.tictactoe_title)
                        isConnect4 -> stringResource(R.string.connect4_title)
                        isHex -> stringResource(R.string.hex_title)
                        else -> stringResource(R.string.app_name)
                }

        // Determine if we're in dark theme based on the theme mode
        val isDarkTheme =
                when (themeMode) {
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK -> true
                        ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }

        // Detect current orientation and rotation
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Get the device rotation from the window manager
        val context = LocalContext.current
        val windowManager = context.getSystemService(android.view.WindowManager::class.java)
        val rotation = windowManager.defaultDisplay.rotation

        // Determine if the app bar should be on the left or right side
        // Surface.ROTATION_90: Bottom on right side
        // Surface.ROTATION_270: Bottom on left side
        val isAppBarOnLeft = rotation == Surface.ROTATION_270

        // Calculate the player status text for display
        val playerStatusText =
                if (isLoading) {
                        stringResource(R.string.computer_thinking)
                } else if (winner == DRAW) {
                        stringResource(R.string.draw)
                } else if (winner != null) {
                        when {
                                isXandO ->
                                        stringResource(
                                                R.string.winner_format,
                                                if (winner == GameState.PLAYER_ONE)
                                                        stringResource(R.string.player_x)
                                                else stringResource(R.string.player_o)
                                        )
                                isConnect4 ->
                                        stringResource(
                                                R.string.winner_format,
                                                if (winner == GameState.PLAYER_ONE)
                                                        stringResource(R.string.player_red)
                                                else stringResource(R.string.player_yellow)
                                        )
                                isHex ->
                                        stringResource(
                                                R.string.winner_format,
                                                if (winner == GameState.PLAYER_ONE)
                                                        stringResource(R.string.player_red)
                                                else stringResource(R.string.player_blue)
                                        )
                                else -> {
                                        if (gameState.againstComputer) {
                                                if (winner == GameState.PLAYER_ONE)
                                                        stringResource(R.string.you_won)
                                                else stringResource(R.string.computer_won)
                                        } else {
                                                stringResource(
                                                        R.string.winner_format,
                                                        if (winner == GameState.PLAYER_ONE)
                                                                stringResource(
                                                                        R.string.player_black
                                                                )
                                                        else stringResource(R.string.player_white)
                                                )
                                        }
                                }
                        }
                } else {
                        when {
                                isXandO ->
                                        stringResource(
                                                R.string.player_turn_format,
                                                if (gameState.currentPlayer == GameState.PLAYER_ONE)
                                                        stringResource(R.string.player_x)
                                                else stringResource(R.string.player_o)
                                        )
                                isConnect4 ->
                                        stringResource(
                                                R.string.player_turn_format,
                                                if (gameState.currentPlayer == GameState.PLAYER_ONE)
                                                        stringResource(R.string.player_red)
                                                else stringResource(R.string.player_yellow)
                                        )
                                isHex ->
                                        stringResource(
                                                R.string.player_turn_format,
                                                if (gameState.currentPlayer == GameState.PLAYER_ONE)
                                                        stringResource(R.string.player_red)
                                                else stringResource(R.string.player_blue)
                                        )
                                else -> {
                                        if (gameState.againstComputer) {
                                                if (gameState.currentPlayer == GameState.PLAYER_ONE)
                                                        stringResource(R.string.your_turn)
                                                else stringResource(R.string.computer_thinking)
                                        } else {
                                                stringResource(
                                                        R.string.player_turn_format,
                                                        if (gameState.currentPlayer ==
                                                                        GameState.PLAYER_ONE
                                                        )
                                                                stringResource(
                                                                        R.string.player_black
                                                                )
                                                        else stringResource(R.string.player_white)
                                                )
                                        }
                                }
                        }
                }

        // Calculate the player color
        val playerColor =
                when {
                        isConnect4 ->
                                if ((winner ?: gameState.currentPlayer) == GameState.PLAYER_ONE)
                                        Connect4PieceRed
                                else Connect4PieceYellow
                        isHex ->
                                if ((winner ?: gameState.currentPlayer) == GameState.PLAYER_ONE)
                                        HexPieceRed
                                else HexPieceBlue
                        else ->
                                if ((winner ?: gameState.currentPlayer) == GameState.PLAYER_ONE)
                                        MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary
                }

        // Conditional scaffold based on orientation
        if (!isLandscape) {
                // PORTRAIT MODE - Use normal Scaffold with top and bottom bars
                Scaffold(
                        topBar = {
                                CenterAlignedTopAppBar(
                                        title = {
                                                Text(
                                                        text = appTitle,
                                                        style = MaterialTheme.typography.titleLarge
                                                )
                                        },
                                        colors =
                                                TopAppBarDefaults.topAppBarColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .background,
                                                        titleContentColor =
                                                                MaterialTheme.colorScheme
                                                                        .onBackground
                                                )
                                )
                        },
                        bottomBar = {
                                BottomAppBar(
                                        containerColor = MaterialTheme.colorScheme.background
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                // Home button
                                                IconButton(onClick = onNavigateToHome) {
                                                        Icon(
                                                                imageVector = Icons.Default.Home,
                                                                contentDescription =
                                                                        stringResource(
                                                                                R.string.home
                                                                        )
                                                        )
                                                }

                                                // Undo button (disabled if no moves or game frozen)
                                                IconButton(
                                                        onClick = { viewModel.undoMove() },
                                                        enabled =
                                                                moveHistory.isNotEmpty() &&
                                                                        !isLoading
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.AutoMirrored.Filled
                                                                                .Undo,
                                                                contentDescription =
                                                                        stringResource(
                                                                                R.string.undo
                                                                        )
                                                        )
                                                }

                                                // Reset button
                                                IconButton(
                                                        onClick = {
                                                                if (winner == null &&
                                                                                moveHistory
                                                                                        .isNotEmpty()
                                                                ) {
                                                                        showResetConfirmation = true
                                                                } else {
                                                                        viewModel.resetGame()
                                                                }
                                                        }
                                                ) {
                                                        Icon(
                                                                imageVector = Icons.Default.Replay,
                                                                contentDescription =
                                                                        stringResource(
                                                                                R.string.reset
                                                                        )
                                                        )
                                                }

                                                // Settings button
                                                IconButton(onClick = onNavigateToSettings) {
                                                        Icon(
                                                                imageVector = Icons.Default.Menu,
                                                                contentDescription =
                                                                        stringResource(
                                                                                R.string.settings
                                                                        )
                                                        )
                                                }
                                        }
                                }
                        }
                ) { innerPadding ->
                        // Portrait Content
                        Column(
                                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                // Status display
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .height(56.dp)
                                                        .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                        if (isLoading) {
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        CircularProgressIndicator(
                                                                modifier = Modifier.size(24.dp),
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .secondary,
                                                                strokeWidth = 2.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                                text =
                                                                        stringResource(
                                                                                R.string
                                                                                        .computer_thinking
                                                                        ),
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleMedium,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .secondary
                                                        )
                                                }
                                        } else {
                                                Text(
                                                        text = playerStatusText,
                                                        style =
                                                                if (winner != null)
                                                                        MaterialTheme.typography
                                                                                .titleMedium.copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        )
                                                                else
                                                                        MaterialTheme.typography
                                                                                .titleMedium,
                                                        color = playerColor
                                                )
                                        }
                                }

                                // Game Board - fills remaining space
                                Box(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        contentAlignment = Alignment.Center
                                ) {
                                        when {
                                                isXandO ->
                                                        TicTacToeBoard(
                                                                gameState = gameState,
                                                                lastPlacedPosition =
                                                                        lastPlacedPosition,
                                                                isDarkTheme = isDarkTheme,
                                                                isGameFrozen =
                                                                        winner != null || isLoading,
                                                                onTileClick = { row, col ->
                                                                        viewModel.placeTile(
                                                                                row,
                                                                                col
                                                                        )
                                                                }
                                                        )
                                                isConnect4 ->
                                                        Connect4Board(
                                                                gameState = gameState,
                                                                lastPlacedPosition =
                                                                        lastPlacedPosition,
                                                                isGameFrozen =
                                                                        winner != null || isLoading,
                                                                onColumnClick = { col ->
                                                                        viewModel.placeConnect4Tile(
                                                                                col
                                                                        )
                                                                }
                                                        )
                                                isHex ->
                                                        HexBoard(
                                                                gameState = gameState,
                                                                lastPlacedPosition =
                                                                        lastPlacedPosition,
                                                                isGameFrozen =
                                                                        winner != null || isLoading,
                                                                onTileClick = { row, col ->
                                                                        viewModel.placeTile(
                                                                                row,
                                                                                col
                                                                        )
                                                                }
                                                        )
                                                else ->
                                                        GameBoard(
                                                                gameState = gameState,
                                                                lastPlacedPosition =
                                                                        lastPlacedPosition,
                                                                isDarkTheme = isDarkTheme,
                                                                isGameFrozen =
                                                                        winner != null || isLoading,
                                                                onTileClick = { row, col ->
                                                                        viewModel.placeTile(
                                                                                row,
                                                                                col
                                                                        )
                                                                }
                                                        )
                                        }
                                }
                        }
                }
        } else {
                // LANDSCAPE MODE - Use Box layout without Scaffold to control exact positioning
                Box(modifier = Modifier.fillMaxSize()) {
                        // Conditional layout based on rotation - modify the design to place sidebar
                        // on correct
                        // side
                        if (isAppBarOnLeft) {
                                // Layout with app bar on LEFT side when rotated clockwise (bottom
                                // on left)
                                Row(modifier = Modifier.fillMaxSize()) {
                                        // VERTICAL SIDE ACTION BAR - left side
                                        Column(
                                                modifier =
                                                        Modifier.fillMaxHeight()
                                                                .width(64.dp)
                                                                .background(
                                                                        MaterialTheme.colorScheme
                                                                                .background
                                                                ),
                                                verticalArrangement = Arrangement.SpaceEvenly,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                                // Home button
                                                IconButton(onClick = onNavigateToHome) {
                                                        Icon(
                                                                imageVector = Icons.Default.Home,
                                                                contentDescription =
                                                                        stringResource(
                                                                                R.string.home
                                                                        )
                                                        )
                                                }

                                                // Undo button
                                                IconButton(
                                                        onClick = { viewModel.undoMove() },
                                                        enabled =
                                                                moveHistory.isNotEmpty() &&
                                                                        !isLoading
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.AutoMirrored.Filled
                                                                                .Undo,
                                                                contentDescription =
                                                                        stringResource(
                                                                                R.string.undo
                                                                        )
                                                        )
                                                }

                                                // Reset button
                                                IconButton(
                                                        onClick = {
                                                                if (winner == null &&
                                                                                moveHistory
                                                                                        .isNotEmpty()
                                                                ) {
                                                                        showResetConfirmation = true
                                                                } else {
                                                                        viewModel.resetGame()
                                                                }
                                                        }
                                                ) {
                                                        Icon(
                                                                imageVector = Icons.Default.Replay,
                                                                contentDescription =
                                                                        stringResource(
                                                                                R.string.reset
                                                                        )
                                                        )
                                                }

                                                // Settings button
                                                IconButton(onClick = onNavigateToSettings) {
                                                        Icon(
                                                                imageVector = Icons.Default.Menu,
                                                                contentDescription =
                                                                        stringResource(
                                                                                R.string.settings
                                                                        )
                                                        )
                                                }
                                        }

                                        // Content area layout
                                        Row(modifier = Modifier.fillMaxSize().padding(8.dp)) {

                                                // Center - Game Board
                                                Box(
                                                        modifier =
                                                                Modifier.weight(0.75f)
                                                                        .fillMaxHeight(),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        when {
                                                                isXandO ->
                                                                        TicTacToeBoard(
                                                                                gameState =
                                                                                        gameState,
                                                                                lastPlacedPosition =
                                                                                        lastPlacedPosition,
                                                                                isDarkTheme =
                                                                                        isDarkTheme,
                                                                                isGameFrozen =
                                                                                        winner !=
                                                                                                null ||
                                                                                                isLoading,
                                                                                onTileClick = {
                                                                                        row,
                                                                                        col ->
                                                                                        viewModel
                                                                                                .placeTile(
                                                                                                        row,
                                                                                                        col
                                                                                                )
                                                                                }
                                                                        )
                                                                isConnect4 ->
                                                                        Connect4Board(
                                                                                gameState =
                                                                                        gameState,
                                                                                lastPlacedPosition =
                                                                                        lastPlacedPosition,
                                                                                isGameFrozen =
                                                                                        winner !=
                                                                                                null ||
                                                                                                isLoading,
                                                                                onColumnClick = {
                                                                                        col ->
                                                                                        viewModel
                                                                                                .placeConnect4Tile(
                                                                                                        col
                                                                                                )
                                                                                }
                                                                        )
                                                                isHex ->
                                                                        HexBoard(
                                                                                gameState =
                                                                                        gameState,
                                                                                lastPlacedPosition =
                                                                                        lastPlacedPosition,
                                                                                isGameFrozen =
                                                                                        winner !=
                                                                                                null ||
                                                                                                isLoading,
                                                                                onTileClick = {
                                                                                        row,
                                                                                        col ->
                                                                                        viewModel
                                                                                                .placeTile(
                                                                                                        row,
                                                                                                        col
                                                                                                )
                                                                                }
                                                                        )
                                                                else ->
                                                                        GameBoard(
                                                                                gameState =
                                                                                        gameState,
                                                                                lastPlacedPosition =
                                                                                        lastPlacedPosition,
                                                                                isDarkTheme =
                                                                                        isDarkTheme,
                                                                                isGameFrozen =
                                                                                        winner !=
                                                                                                null ||
                                                                                                isLoading,
                                                                                onTileClick = {
                                                                                        row,
                                                                                        col ->
                                                                                        viewModel
                                                                                                .placeTile(
                                                                                                        row,
                                                                                                        col
                                                                                                )
                                                                                }
                                                                        )
                                                        }
                                                }

                                                // Right side - Game info column with status
                                                Box(
                                                        modifier =
                                                                Modifier.weight(0.25f)
                                                                        .fillMaxHeight()
                                                                        .padding(horizontal = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Column(
                                                                horizontalAlignment =
                                                                        Alignment
                                                                                .CenterHorizontally,
                                                                verticalArrangement =
                                                                        Arrangement.Center
                                                        ) {
                                                                // Title
                                                                Text(
                                                                        text = appTitle,
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .titleLarge,
                                                                        textAlign = TextAlign.Center
                                                                )

                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        32.dp
                                                                                )
                                                                )

                                                                // Player status
                                                                if (isLoading) {
                                                                        Row(
                                                                                verticalAlignment =
                                                                                        Alignment
                                                                                                .CenterVertically
                                                                        ) {
                                                                                CircularProgressIndicator(
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        24.dp
                                                                                                ),
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .secondary,
                                                                                        strokeWidth =
                                                                                                2.dp
                                                                                )
                                                                                Spacer(
                                                                                        modifier =
                                                                                                Modifier.width(
                                                                                                        8.dp
                                                                                                )
                                                                                )
                                                                                Text(
                                                                                        text =
                                                                                                stringResource(
                                                                                                        R.string
                                                                                                                .computer_thinking
                                                                                                ),
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .titleMedium,
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .secondary
                                                                                )
                                                                        }
                                                                } else {
                                                                        Text(
                                                                                text =
                                                                                        playerStatusText,
                                                                                style =
                                                                                        if (winner !=
                                                                                                        null
                                                                                        )
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .titleMedium
                                                                                                        .copy(
                                                                                                                fontWeight =
                                                                                                                        FontWeight
                                                                                                                                .Bold
                                                                                                        )
                                                                                        else
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .titleMedium,
                                                                                color = playerColor,
                                                                                textAlign =
                                                                                        TextAlign
                                                                                                .Center
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        } else {
                                // Layout with app bar on RIGHT side when rotated counter-clockwise
                                // (bottom on
                                // right)
                                Row(modifier = Modifier.fillMaxSize()) {

                                        // Left side - Game info column
                                        Box(
                                                modifier =
                                                        Modifier.weight(0.25f)
                                                                .fillMaxHeight()
                                                                .padding(start = 8.dp, end = 16.dp),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Column(
                                                        horizontalAlignment =
                                                                Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                ) {
                                                        // Title - now here instead of in top bar
                                                        Text(
                                                                text = appTitle,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleLarge,
                                                                textAlign = TextAlign.Center
                                                        )

                                                        Spacer(modifier = Modifier.height(32.dp))

                                                        // Player status
                                                        if (isLoading) {
                                                                Row(
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        CircularProgressIndicator(
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                24.dp
                                                                                        ),
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .secondary,
                                                                                strokeWidth = 2.dp
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.width(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        Text(
                                                                                text =
                                                                                        stringResource(
                                                                                                R.string
                                                                                                        .computer_thinking
                                                                                        ),
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .titleMedium,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .secondary
                                                                        )
                                                                }
                                                        } else {
                                                                Text(
                                                                        text = playerStatusText,
                                                                        style =
                                                                                if (winner != null)
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .titleMedium
                                                                                                .copy(
                                                                                                        fontWeight =
                                                                                                                FontWeight
                                                                                                                        .Bold
                                                                                                )
                                                                                else
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .titleMedium,
                                                                        color = playerColor,
                                                                        textAlign = TextAlign.Center
                                                                )
                                                        }
                                                }
                                        }

                                        // Content area layout
                                        Row(
                                                modifier =
                                                        Modifier.weight(1f)
                                                                .fillMaxSize()
                                                                .padding(8.dp)
                                        ) {
                                                // Center - Game Board
                                                Box(
                                                        modifier =
                                                                Modifier.weight(0.75f)
                                                                        .fillMaxHeight(),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        when {
                                                                isXandO ->
                                                                        TicTacToeBoard(
                                                                                gameState =
                                                                                        gameState,
                                                                                lastPlacedPosition =
                                                                                        lastPlacedPosition,
                                                                                isDarkTheme =
                                                                                        isDarkTheme,
                                                                                isGameFrozen =
                                                                                        winner !=
                                                                                                null ||
                                                                                                isLoading,
                                                                                onTileClick = {
                                                                                        row,
                                                                                        col ->
                                                                                        viewModel
                                                                                                .placeTile(
                                                                                                        row,
                                                                                                        col
                                                                                                )
                                                                                }
                                                                        )
                                                                isConnect4 ->
                                                                        Connect4Board(
                                                                                gameState =
                                                                                        gameState,
                                                                                lastPlacedPosition =
                                                                                        lastPlacedPosition,
                                                                                isGameFrozen =
                                                                                        winner !=
                                                                                                null ||
                                                                                                isLoading,
                                                                                onColumnClick = {
                                                                                        col ->
                                                                                        viewModel
                                                                                                .placeConnect4Tile(
                                                                                                        col
                                                                                                )
                                                                                }
                                                                        )
                                                                isHex ->
                                                                        HexBoard(
                                                                                gameState =
                                                                                        gameState,
                                                                                lastPlacedPosition =
                                                                                        lastPlacedPosition,
                                                                                isGameFrozen =
                                                                                        winner !=
                                                                                                null ||
                                                                                                isLoading,
                                                                                onTileClick = {
                                                                                        row,
                                                                                        col ->
                                                                                        viewModel
                                                                                                .placeTile(
                                                                                                        row,
                                                                                                        col
                                                                                                )
                                                                                }
                                                                        )
                                                                else ->
                                                                        GameBoard(
                                                                                gameState =
                                                                                        gameState,
                                                                                lastPlacedPosition =
                                                                                        lastPlacedPosition,
                                                                                isDarkTheme =
                                                                                        isDarkTheme,
                                                                                isGameFrozen =
                                                                                        winner !=
                                                                                                null ||
                                                                                                isLoading,
                                                                                onTileClick = {
                                                                                        row,
                                                                                        col ->
                                                                                        viewModel
                                                                                                .placeTile(
                                                                                                        row,
                                                                                                        col
                                                                                                )
                                                                                }
                                                                        )
                                                        }
                                                }

                                                // VERTICAL SIDE ACTION BAR for landscape mode -
                                                // right side (when bottom is
                                                // on
                                                // right)
                                                Column(
                                                        modifier =
                                                                Modifier.width(64.dp)
                                                                        .fillMaxHeight()
                                                                        .background(
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .background
                                                                        ),
                                                        verticalArrangement =
                                                                Arrangement.SpaceEvenly,
                                                        horizontalAlignment =
                                                                Alignment.CenterHorizontally
                                                ) {

                                                        // Settings button
                                                        IconButton(onClick = onNavigateToSettings) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Default.Menu,
                                                                        contentDescription =
                                                                                stringResource(
                                                                                        R.string
                                                                                                .settings
                                                                                )
                                                                )
                                                        }

                                                        // Reset button
                                                        IconButton(
                                                                onClick = {
                                                                        if (winner == null &&
                                                                                        moveHistory
                                                                                                .isNotEmpty()
                                                                        ) {
                                                                                showResetConfirmation =
                                                                                        true
                                                                        } else {
                                                                                viewModel
                                                                                        .resetGame()
                                                                        }
                                                                }
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Default
                                                                                        .Replay,
                                                                        contentDescription =
                                                                                stringResource(
                                                                                        R.string
                                                                                                .reset
                                                                                )
                                                                )
                                                        }

                                                        // Undo button (disabled if no moves or game
                                                        // frozen)
                                                        IconButton(
                                                                onClick = { viewModel.undoMove() },
                                                                enabled =
                                                                        moveHistory.isNotEmpty() &&
                                                                                !isLoading
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.AutoMirrored
                                                                                        .Filled
                                                                                        .Undo,
                                                                        contentDescription =
                                                                                stringResource(
                                                                                        R.string
                                                                                                .undo
                                                                                )
                                                                )
                                                        }

                                                        // Home button
                                                        IconButton(onClick = onNavigateToHome) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Default.Home,
                                                                        contentDescription =
                                                                                stringResource(
                                                                                        R.string
                                                                                                .home
                                                                                )
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }

        if (showResetConfirmation) {
                ResetConfirmationDialog(
                        onConfirm = {
                                viewModel.resetGame()
                                showResetConfirmation = false
                        },
                        onDismiss = { showResetConfirmation = false }
                )
        }
}

/** Dialog to confirm game reset when the game is still in progress. */
@Composable
fun ResetConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.confirm_reset_title)) },
                text = { Text(stringResource(R.string.confirm_reset_message)) },
                confirmButton = {
                        Button(onClick = onConfirm) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
        )
}
