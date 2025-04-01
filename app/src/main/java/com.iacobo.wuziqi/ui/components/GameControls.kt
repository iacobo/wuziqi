package com.iacobo.wuziqi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.R

/**
 * A reusable component for game controls (home, undo, reset, how to play, settings) that can be
 * used in both portrait and landscape orientations.
 */
@Composable
fun GameControls(
        isLandscape: Boolean,
        isAppBarOnLeft: Boolean,
        onHome: () -> Unit,
        onUndo: () -> Unit,
        onReset: () -> Unit,
        onHowToPlay: () -> Unit,
        onSettings: () -> Unit,
        canUndo: Boolean = true,
        isLoading: Boolean = false
) {
    if (isLandscape && isAppBarOnLeft) {
        // In landscape mode, render controls vertically
        GameControlsVertical(
                onHome = onHome,
                onUndo = onUndo,
                onReset = onReset,
                onHowToPlay = onHowToPlay,
                onSettings = onSettings,
                canUndo = canUndo,
                isLoading = isLoading
        )
    } else if (isLandscape) {
        // In landscape mode, render controls vertically
        GameControlsVerticalInverted(
                onHome = onHome,
                onUndo = onUndo,
                onReset = onReset,
                onHowToPlay = onHowToPlay,
                onSettings = onSettings,
                canUndo = canUndo,
                isLoading = isLoading
        )
    } else {
        // In portrait mode, render controls horizontally
        GameControlsHorizontal(
                onHome = onHome,
                onUndo = onUndo,
                onReset = onReset,
                onHowToPlay = onHowToPlay,
                onSettings = onSettings,
                canUndo = canUndo,
                isLoading = isLoading
        )
    }
}

/** Horizontal layout for game controls (used in portrait mode). */
@Composable
fun GameControlsHorizontal(
        onHome: () -> Unit,
        onUndo: () -> Unit,
        onReset: () -> Unit,
        onHowToPlay: () -> Unit,
        onSettings: () -> Unit,
        canUndo: Boolean = true,
        isLoading: Boolean = false
) {
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
    ) {
        // Home button
        IconButton(onClick = onHome) {
            Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = stringResource(R.string.home)
            )
        }

        // Undo button
        IconButton(onClick = onUndo, enabled = canUndo && !isLoading) {
            Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.undo)
            )
        }

        // Reset button
        IconButton(onClick = onReset, enabled = !isLoading) {
            Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = stringResource(R.string.reset)
            )
        }

        // How to Play button
        IconButton(onClick = onHowToPlay) {
            Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = stringResource(R.string.how_to_play)
            )
        }

        // Settings button
        IconButton(onClick = onSettings) {
            Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.settings)
            )
        }
    }
}

/** Vertical layout for game controls (used in landscape mode). */
@Composable
fun GameControlsVertical(
        onHome: () -> Unit,
        onUndo: () -> Unit,
        onReset: () -> Unit,
        onHowToPlay: () -> Unit,
        onSettings: () -> Unit,
        canUndo: Boolean = true,
        isLoading: Boolean = false
) {
    // Home button
    IconButton(onClick = onHome) {
        Icon(imageVector = Icons.Default.Home, contentDescription = stringResource(R.string.home))
    }

    Spacer(modifier = Modifier.width(16.dp))

    // Undo button
    IconButton(onClick = onUndo, enabled = canUndo && !isLoading) {
        Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.undo)
        )
    }

    Spacer(modifier = Modifier.width(16.dp))

    // Reset button
    IconButton(onClick = onReset, enabled = !isLoading) {
        Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = stringResource(R.string.reset)
        )
    }

    Spacer(modifier = Modifier.width(16.dp))

    // How to Play button
    IconButton(onClick = onHowToPlay) {
        Icon(
                imageVector = Icons.Default.Help,
                contentDescription = stringResource(R.string.how_to_play)
        )
    }

    Spacer(modifier = Modifier.width(16.dp))

    // Settings button
    IconButton(onClick = onSettings) {
        Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = stringResource(R.string.settings)
        )
    }
}

/** Vertical layout for game controls (used in landscape mode). */
@Composable
fun GameControlsVerticalInverted(
        onHome: () -> Unit,
        onUndo: () -> Unit,
        onReset: () -> Unit,
        onHowToPlay: () -> Unit,
        onSettings: () -> Unit,
        canUndo: Boolean = true,
        isLoading: Boolean = false
) {
    // Settings button
    IconButton(onClick = onSettings) {
        Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = stringResource(R.string.settings)
        )
    }

    Spacer(modifier = Modifier.width(16.dp))

    // How to Play button
    IconButton(onClick = onHowToPlay) {
        Icon(
                imageVector = Icons.Default.Help,
                contentDescription = stringResource(R.string.how_to_play)
        )
    }

    Spacer(modifier = Modifier.width(16.dp))

    // Reset button
    IconButton(onClick = onReset, enabled = !isLoading) {
        Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = stringResource(R.string.reset)
        )
    }

    Spacer(modifier = Modifier.width(16.dp))

    // Undo button
    IconButton(onClick = onUndo, enabled = canUndo && !isLoading) {
        Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.undo)
        )
    }

    Spacer(modifier = Modifier.width(16.dp))

    // Home button
    IconButton(onClick = onHome) {
        Icon(imageVector = Icons.Default.Home, contentDescription = stringResource(R.string.home))
    }
}
