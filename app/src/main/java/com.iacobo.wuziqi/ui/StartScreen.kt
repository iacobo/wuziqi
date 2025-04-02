package com.iacobo.wuziqi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Hive
import androidx.compose.material.icons.filled.Margin
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.R
import com.iacobo.wuziqi.viewmodel.GameViewModel

/**
 * Start screen that allows users to select game modes and options. Reorganized with category
 * headings and two games per row.
 */
@Composable
fun StartScreen(
        viewModel: GameViewModel,
        onNavigateToSettings: () -> Unit,
        onNavigateToStandardGame: (opponent: Opponent) -> Unit,
        onNavigateToCustomGame: (boardSize: Int, winLength: Int, opponent: Opponent) -> Unit
) {
        val discoveredEasterEggs = viewModel.discoveredEasterEggs

        var showOpponentDialog by remember { mutableStateOf(false) }
        var showCustomDialog by remember { mutableStateOf(false) }
        var currentMode by remember { mutableStateOf("standard") }
        var boardSize by remember { mutableIntStateOf(15) }
        var winLength by remember { mutableIntStateOf(5) }

        // Determine AI support for different game modes
        val aiSupportedModes = remember {
                setOf("standard", "tictactoe", "connect4", "hex", "havannah")
        }

        Scaffold(
                bottomBar = {
                        BottomAppBar(
                                actions = {
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = onNavigateToSettings) {
                                                Icon(
                                                        imageVector = Icons.Default.Menu,
                                                        contentDescription =
                                                                stringResource(R.string.settings)
                                                )
                                        }
                                }
                        )
                }
        ) { innerPadding ->
                Column(
                        modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Title
                        Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle
                        Text(
                                text = stringResource(R.string.choose_game_mode),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // N-in-a-row games category
                        CategoryHeader(title = "N-in-a-row Games")

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 1: Wuziqi and Connect4
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                // Wuziqi (Standard)
                                GameModeButtonSmall(
                                        title = stringResource(R.string.standard_wuziqi),
                                        description = stringResource(R.string.standard_wuziqi_desc),
                                        icon = Icons.Default.Apps,
                                        onClick = {
                                                currentMode = "standard"
                                                showOpponentDialog = true
                                        },
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )

                                // Connect4
                                GameModeButtonSmall(
                                        title = stringResource(R.string.connect4_title),
                                        description = stringResource(R.string.connect4_description),
                                        icon = Icons.Default.Margin,
                                        onClick = {
                                                currentMode = "connect4"
                                                showOpponentDialog = true
                                        },
                                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                                )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Row 2: TicTacToe and Custom
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                // TicTacToe
                                GameModeButtonSmall(
                                        title = stringResource(R.string.tictactoe_title),
                                        description =
                                                stringResource(R.string.tictactoe_description),
                                        icon = Icons.Default.Grid3x3,
                                        onClick = {
                                                currentMode = "tictactoe"
                                                showOpponentDialog = true
                                        },
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )

                                // Custom Game
                                GameModeButtonSmall(
                                        title = stringResource(R.string.custom_game),
                                        description = stringResource(R.string.custom_game_desc),
                                        icon = Icons.Default.AppRegistration,
                                        onClick = { showCustomDialog = true },
                                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                                )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Connection games category
                        CategoryHeader(title = "Connection Games")

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 3: Hex and Havannah
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                // Hex
                                GameModeButtonSmall(
                                        title = stringResource(R.string.hex_title),
                                        description = stringResource(R.string.hex_description),
                                        icon = Icons.Default.Hexagon,
                                        onClick = {
                                                currentMode = "hex"
                                                showOpponentDialog = true
                                        },
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )

                                // Havannah
                                GameModeButtonSmall(
                                        title = stringResource(R.string.havannah_title),
                                        description = stringResource(R.string.havannah_description),
                                        icon = Icons.Default.Hive,
                                        onClick = {
                                                currentMode = "havannah"
                                                showOpponentDialog = true
                                        },
                                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                                )
                        }
                }
        }

        // Opponent selection dialog
        if (showOpponentDialog) {
                OpponentSelectionDialog(
                        onDismiss = { showOpponentDialog = false },
                        onSelectOpponent = { opponent ->
                                showOpponentDialog = false

                                when (currentMode) {
                                        "standard" -> onNavigateToStandardGame(opponent)
                                        "tictactoe" -> onNavigateToCustomGame(3, 3, opponent)
                                        "connect4" -> onNavigateToCustomGame(7, 4, opponent)
                                        "hex" -> onNavigateToCustomGame(11, 8, opponent)
                                        "havannah" -> onNavigateToCustomGame(10, 9, opponent)
                                        "custom" ->
                                                onNavigateToCustomGame(
                                                        boardSize,
                                                        winLength,
                                                        opponent
                                                )
                                }
                        },
                        isAISupported = aiSupportedModes.contains(currentMode)
                )
        }

        // Custom game settings dialog
        if (showCustomDialog) {
                CustomGameDialog(
                        onDismiss = { showCustomDialog = false },
                        onStartGame = { newBoardSize, newWinLength ->
                                // Update our variables
                                boardSize = newBoardSize
                                winLength = newWinLength
                                showCustomDialog = false

                                // Determine if this is a special mode
                                when {
                                        // TicTacToe
                                        boardSize == 3 && winLength == 3 -> {
                                                currentMode = "tictactoe"
                                                showOpponentDialog = true
                                        }
                                        // Connect4
                                        boardSize == 7 && winLength == 4 -> {
                                                currentMode = "connect4"
                                                showOpponentDialog = true
                                        }
                                        // Custom sizes
                                        else -> {
                                                currentMode = "custom"
                                                showOpponentDialog = true
                                        }
                                }
                        }
                )
        }
}

/** Header for game categories. */
@Composable
fun CategoryHeader(title: String) {
        Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
        )
}

/** Smaller button for game modes (fits two per row). */
@Composable
fun GameModeButtonSmall(
        title: String,
        description: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        Button(
                onClick = onClick,
                modifier = modifier.height(100.dp).clip(RoundedCornerShape(12.dp)),
                colors =
                        ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                        )
                }
        }
}

/** Dialog for selecting opponent type. */
@Composable
fun OpponentSelectionDialog(
        onDismiss: () -> Unit,
        onSelectOpponent: (Opponent) -> Unit,
        isAISupported: Boolean = true
) {
        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.choose_opponent)) },
                text = {
                        Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                OpponentButton(
                                        title = stringResource(R.string.play_against_human),
                                        icon = Icons.Default.Person,
                                        onClick = { onSelectOpponent(Opponent.HUMAN) }
                                )

                                OpponentButton(
                                        title = stringResource(R.string.play_against_computer),
                                        icon = Icons.Default.Computer,
                                        onClick = { onSelectOpponent(Opponent.COMPUTER) },
                                        enabled = isAISupported
                                )

                                if (!isAISupported) {
                                        Text(
                                                text = stringResource(R.string.ai_not_supported),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                        )
                                }
                        }
                },
                confirmButton = {},
                dismissButton = {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
        )
}

/** Button for selecting an opponent type. */
@Composable
fun OpponentButton(
        title: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit,
        enabled: Boolean = true
) {
        Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors =
                        ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.38f
                                        )
                        ),
                enabled = enabled
        ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(text = title, style = MaterialTheme.typography.bodyLarge)
                }
        }
}

/** Dialog for custom game settings. */
@Composable
fun CustomGameDialog(onDismiss: () -> Unit, onStartGame: (boardSize: Int, winLength: Int) -> Unit) {
        var boardSize by remember { mutableIntStateOf(15) }
        var winLength by remember { mutableIntStateOf(5) }

        // Board size options: 3, 5, 7, 9, 11, 13, 15, 17, 19
        val boardSizeOptions = listOf(3, 5, 7, 9, 11, 13, 15, 17, 19)
        val boardSizeSteps = boardSizeOptions.size - 1

        // Find the closest option in boardSizeOptions to the current boardSize
        val closestBoardSizeIndex =
                boardSizeOptions.indexOfFirst { it == boardSize }.takeIf { it != -1 }
                        ?: (boardSizeOptions.indexOfFirst { it > boardSize } - 1)

        // Dynamically calculate win length options based on board size
        val minWinLength = 3
        val maxWinLength = minOf(boardSize, 8)
        val winLengthOptions = (minWinLength..maxWinLength).toList()
        val winLengthSteps = winLengthOptions.size - 1

        // Check if this is a special game
        val isTicTacToe = boardSize == 3 && winLength == 3
        val isConnect4 = boardSize == 7 && winLength == 4
        val isHex = boardSize == 11 && winLength == 8

        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.custom_game_settings)) },
                text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                        text =
                                                stringResource(
                                                        R.string.board_size,
                                                        boardSize,
                                                        boardSize
                                                ),
                                        style = MaterialTheme.typography.bodyMedium
                                )

                                Slider(
                                        value = closestBoardSizeIndex.toFloat(),
                                        onValueChange = {
                                                val index =
                                                        it.toInt()
                                                                .coerceIn(
                                                                        0,
                                                                        boardSizeOptions.size - 1
                                                                )
                                                boardSize = boardSizeOptions[index]
                                                // Adjust win length if needed
                                                if (winLength > minOf(boardSize, 8)) {
                                                        winLength = minOf(boardSize, 8)
                                                }
                                        },
                                        valueRange = 0f..boardSizeSteps.toFloat(),
                                        steps = boardSizeSteps - 1, // Correct number of steps
                                        modifier = Modifier.padding(vertical = 8.dp)
                                )

                                // Display board size options
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        boardSizeOptions.forEach { size ->
                                                Text(
                                                        text = "$size",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color =
                                                                if (size == boardSize)
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                                else
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                                                .copy(alpha = 0.6f)
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                        text = stringResource(R.string.win_length, winLength),
                                        style = MaterialTheme.typography.bodyMedium
                                )

                                Slider(
                                        value = (winLength - minWinLength).toFloat(),
                                        onValueChange = { winLength = it.toInt() + minWinLength },
                                        valueRange = 0f..(maxWinLength - minWinLength).toFloat(),
                                        steps =
                                                if (winLengthSteps > 0) winLengthSteps - 1
                                                else 0, // Correct number of steps
                                        modifier = Modifier.padding(vertical = 8.dp)
                                )

                                // Display win length options
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        winLengthOptions.forEach { length ->
                                                Text(
                                                        text = "$length",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color =
                                                                if (length == winLength)
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                                else
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                                                .copy(alpha = 0.6f)
                                                )
                                        }
                                }

                                // Only show win length warning if not a special game and if it's a
                                // large win
                                // length
                                if (!isTicTacToe &&
                                                !isConnect4 &&
                                                !isHex &&
                                                winLength > boardSize / 2 + 1
                                ) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                text = stringResource(R.string.win_length_warning),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                        )
                                }

                                // Easter egg hint for special board configurations
                                if (isTicTacToe || isConnect4 || isHex) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                text = stringResource(R.string.special_game_hint),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                        )
                                }
                        }
                },
                confirmButton = {
                        Button(onClick = { onStartGame(boardSize, winLength) }) {
                                Text(stringResource(R.string.start_game))
                        }
                },
                dismissButton = {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
        )
}

/** Represents opponent types in the game. */
enum class Opponent {
        HUMAN,
        COMPUTER
}
