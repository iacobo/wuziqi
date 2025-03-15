package com.iacobo.wuziqi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.R

/**
 * Start screen that allows users to select game modes and options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToStandardGame: (opponent: Opponent) -> Unit,
    onNavigateToCustomGame: (boardSize: Int, winLength: Int) -> Unit
) {
    var showOpponentDialog by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo or icon (optional)
            // Logo()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = stringResource(R.string.welcome_wuziqi),
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
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Game mode buttons
            GameModeButton(
                title = stringResource(R.string.standard_wuziqi),
                description = stringResource(R.string.standard_wuziqi_desc),
                onClick = { showOpponentDialog = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            GameModeButton(
                title = stringResource(R.string.custom_game),
                description = stringResource(R.string.custom_game_desc),
                onClick = { showCustomDialog = true }
            )
        }
    }
    
    // Opponent selection dialog
    if (showOpponentDialog) {
        OpponentSelectionDialog(
            onDismiss = { showOpponentDialog = false },
            onSelectOpponent = { opponent ->
                showOpponentDialog = false
                onNavigateToStandardGame(opponent)
            }
        )
    }
    
    // Custom game settings dialog
    if (showCustomDialog) {
        CustomGameDialog(
            onDismiss = { showCustomDialog = false },
            onStartGame = { boardSize, winLength ->
                showCustomDialog = false
                onNavigateToCustomGame(boardSize, winLength)
            }
        )
    }
}

/**
 * Button representing a game mode option.
 */
@Composable
fun GameModeButton(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Dialog for selecting opponent type.
 */
@Composable
fun OpponentSelectionDialog(
    onDismiss: () -> Unit,
    onSelectOpponent: (Opponent) -> Unit
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
                Button(
                    onClick = { onSelectOpponent(Opponent.HUMAN) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.play_against_human))
                }
                
                Button(
                    onClick = { onSelectOpponent(Opponent.COMPUTER) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.play_against_computer))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Dialog for custom game settings.
 */
@Composable
fun CustomGameDialog(
    onDismiss: () -> Unit,
    onStartGame: (boardSize: Int, winLength: Int) -> Unit
) {
    var boardSize by remember { mutableStateOf(15) }
    var winLength by remember { mutableStateOf(5) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_game_settings)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.board_size, boardSize, boardSize),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Slider(
                    value = boardSize.toFloat(),
                    onValueChange = { boardSize = it.toInt() },
                    valueRange = 5f..19f,
                    steps = 7,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.win_length, winLength),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Slider(
                    value = winLength.toFloat(),
                    onValueChange = { winLength = it.toInt() },
                    valueRange = 3f..minOf(boardSize.toFloat(), 8f),
                    steps = minOf(boardSize, 8) - 3,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                // Show win length warning if needed
                if (winLength > boardSize / 2 + 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.win_length_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStartGame(boardSize, winLength) }
            ) {
                Text(stringResource(R.string.start_game))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Represents opponent types in the game.
 */
enum class Opponent {
    HUMAN, COMPUTER
}
