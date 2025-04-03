package com.iacobo.wuziqi.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.R
import com.iacobo.wuziqi.data.GameState
import com.iacobo.wuziqi.data.GameType
import com.iacobo.wuziqi.ui.theme.Connect4PieceRed
import com.iacobo.wuziqi.ui.theme.Connect4PieceYellow
import com.iacobo.wuziqi.ui.theme.HexPieceBlue
import com.iacobo.wuziqi.ui.theme.HexPieceRed
import com.iacobo.wuziqi.viewmodel.DRAW

/** Displays the current game status (whose turn, winner, etc.) */
@Composable
fun GameStatusBar(gameState: GameState, gameType: GameType, winner: Int?, isLoading: Boolean) {
        if (isLoading) {
                LoadingIndicator()
        } else {
                GameStatusText(gameState = gameState, gameType = gameType, winner = winner)
        }
}

/** Shows a loading indicator when the AI is thinking. */
@Composable
fun LoadingIndicator() {
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

/** Displays game status text based on the current game state. */
@Composable
fun GameStatusText(gameState: GameState, gameType: GameType, winner: Int?) {
        val playerColor = getPlayerColor(gameState, gameType, winner)
        val statusText = getStatusText(gameState, gameType, winner)

        Text(
                text = statusText,
                style =
                        if (winner != null)
                                MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                )
                        else MaterialTheme.typography.titleMedium,
                color = playerColor,
                textAlign = TextAlign.Center
        )
}

/** Determines the appropriate color for the current player/winner. */
@Composable
fun getPlayerColor(gameState: GameState, gameType: GameType, winner: Int?): Color {
        val currentPlayer = winner ?: gameState.currentPlayer

        return when (gameType) {
                GameType.Connect4 -> {
                        if (currentPlayer == GameState.PLAYER_ONE) Connect4PieceRed
                        else Connect4PieceYellow
                }
                GameType.Hex, GameType.Havannah, GameType.HavannahSmall -> {
                        if (currentPlayer == GameState.PLAYER_ONE) HexPieceRed else HexPieceBlue
                }
                else -> {
                        if (currentPlayer == GameState.PLAYER_ONE) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                }
        }
}

/** Creates the status text based on the game state. */
@Composable
fun getStatusText(gameState: GameState, gameType: GameType, winner: Int?): String {
        // Handle draw case first
        if (winner == DRAW) {
                return stringResource(R.string.draw)
        }

        // Handle winner case
        if (winner != null) {
                return when (gameType) {
                        GameType.TicTacToe ->
                                stringResource(
                                        R.string.winner_format,
                                        if (winner == GameState.PLAYER_ONE)
                                                stringResource(R.string.player_x)
                                        else stringResource(R.string.player_o)
                                )
                        GameType.Connect4 ->
                                stringResource(
                                        R.string.winner_format,
                                        if (winner == GameState.PLAYER_ONE)
                                                stringResource(R.string.player_red)
                                        else stringResource(R.string.player_yellow)
                                )
                        GameType.Hex, GameType.Havannah, GameType.HavannahSmall ->
                                stringResource(
                                        R.string.winner_format,
                                        if (winner == GameState.PLAYER_ONE)
                                                stringResource(R.string.player_red)
                                        else stringResource(R.string.player_blue)
                                )
                        GameType.Standard -> {
                                if (gameState.againstComputer) {
                                        if (winner == GameState.PLAYER_ONE)
                                                stringResource(R.string.you_won)
                                        else stringResource(R.string.computer_won)
                                } else {
                                        stringResource(
                                                R.string.winner_format,
                                                if (winner == GameState.PLAYER_ONE)
                                                        stringResource(R.string.player_black)
                                                else stringResource(R.string.player_white)
                                        )
                                }
                        }
                }
        }

        // Handle current player turn case (only reached when winner is null)
        return when (gameType) {
                GameType.TicTacToe ->
                        stringResource(
                                R.string.player_turn_format,
                                if (gameState.currentPlayer == GameState.PLAYER_ONE)
                                        stringResource(R.string.player_x)
                                else stringResource(R.string.player_o)
                        )
                GameType.Connect4 ->
                        stringResource(
                                R.string.player_turn_format,
                                if (gameState.currentPlayer == GameState.PLAYER_ONE)
                                        stringResource(R.string.player_red)
                                else stringResource(R.string.player_yellow)
                        )
                GameType.Hex, GameType.Havannah, GameType.HavannahSmall ->
                        stringResource(
                                R.string.player_turn_format,
                                if (gameState.currentPlayer == GameState.PLAYER_ONE)
                                        stringResource(R.string.player_red)
                                else stringResource(R.string.player_blue)
                        )
                GameType.Standard -> {
                        if (gameState.againstComputer) {
                                if (gameState.currentPlayer == GameState.PLAYER_ONE)
                                        stringResource(R.string.your_turn)
                                else stringResource(R.string.computer_thinking)
                        } else {
                                stringResource(
                                        R.string.player_turn_format,
                                        if (gameState.currentPlayer == GameState.PLAYER_ONE)
                                                stringResource(R.string.player_black)
                                        else stringResource(R.string.player_white)
                                )
                        }
                }
        }
}
