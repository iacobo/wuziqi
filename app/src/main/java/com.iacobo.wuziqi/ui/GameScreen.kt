package com.iacobo.wuziqi.ui

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.R
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.data.ThemeMode
import com.iacobo.wuziqi.ui.theme.Connect4PieceRed
import com.iacobo.wuziqi.ui.theme.Connect4PieceYellow
import com.iacobo.wuziqi.ui.theme.HexPieceBlue
import com.iacobo.wuziqi.ui.theme.HexPieceRed
import com.iacobo.wuziqi.viewmodel.DRAW
import com.iacobo.wuziqi.viewmodel.GameViewModel

/**
 * Main game screen composable. Displays the game board, status, and controls. Supports standard
 * game and easter eggs.
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
        val isHex = boardSize == 11 && winLength == 8 // Hex game detection

        // Determine app title based on easter egg mode
        val appTitle =
                when {
                        isXandO -> "X's & O's"
                        isConnect4 -> "Connect 4"
                        isHex -> "Hex"
                        else -> stringResource(R.string.app_name)
                }

        // Determine if we're in dark theme based on the theme mode
        val isDarkTheme =
                when (themeMode) {
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK -> true
                        ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }

        // Detect current orientation
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Create player status string
        val playerStatusText =
                if (isLoading) {
                        stringResource(R.string.computer_thinking)
                } else if (winner == DRAW) {
                        stringResource(R.string.draw)
                } else if (winner != null) {
                        // Show winner with fireworks emoji
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
                                        // Handle computer opponent case
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
                        // Show current player
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
                                        // Handle computer opponent case
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

        // Get player color
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

        // Loading indicator
        val loadingIndicator =
                @Composable
                {
                        if (isLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        }
                }

        // Game toolbar actions (for reuse in both orientations)
        val toolbarActions =
                @Composable
                {
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
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = stringResource(R.string.undo)
                                )
                        }

                        // Reset button
                        IconButton(onClick = { viewModel.resetGame() }) {
                                Icon(
                                        imageVector = Icons.Default.Replay,
                                        contentDescription = stringResource(R.string.reset)
                                )
                        }

                        // Settings button
                        IconButton(onClick = onNavigateToSettings) {
                                Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = stringResource(R.string.settings)
                                )
                        }
                }

        Scaffold(
                topBar = {
                        if (!isLandscape) {
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
                        }
                },
                bottomBar = {
                        if (!isLandscape) {
                                BottomAppBar(actions = { toolbarActions() })
                        }
                }
        ) { innerPadding ->
                if (isLandscape) {
                        // Landscape layout
                        Row(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // Left side - Title and Game Information
                                Column(
                                        modifier =
                                                Modifier.weight(0.2f)
                                                        .fillMaxHeight()
                                                        .padding(horizontal = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                ) {
                                        // Game title
                                        Text(
                                                text = appTitle,
                                                style = MaterialTheme.typography.titleLarge,
                                                textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(24.dp))

                                        // Player status
                                        Text(
                                                text = playerStatusText,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = playerColor,
                                                textAlign = TextAlign.Center
                                        )

                                        // Loading indicator
                                        loadingIndicator()

                                        Spacer(modifier = Modifier.height(24.dp))

                                        // Action buttons in a vertical arrangement
                                        Row { toolbarActions() }
                                }

                                // Center - Game Board
                                Box(
                                        modifier = Modifier.weight(0.8f).fillMaxHeight(),
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
                } else {
                        // Portrait layout (original)
                        Column(
                                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                // Fixed height container for player indicator to prevent layout
                                // shifts
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .height(56.dp)
                                                        .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                        if (isLoading) {
                                                loadingIndicator()
                                        } else {
                                                // Player status text
                                                Text(
                                                        text = playerStatusText,
                                                        style =
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
        }
}
