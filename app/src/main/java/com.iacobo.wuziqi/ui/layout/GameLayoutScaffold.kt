package com.iacobo.wuziqi.ui.layout

import android.view.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A scaffold component that adapts to the device orientation. In portrait mode, it uses a
 * traditional top/bottom bar layout. In landscape mode, it uses a side-by-side layout with controls
 * on one side.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLayoutScaffold(
        title: String,
        isLandscape: Boolean,
        rotation: Int = Surface.ROTATION_0,
        statusContent: @Composable () -> Unit,
        boardContent: @Composable () -> Unit,
        controlsContent: @Composable () -> Unit
) {
    // Determine if the app bar should be on the left or right in landscape mode
    // Surface.ROTATION_90: Bottom on right side
    // Surface.ROTATION_270: Bottom on left side
    val isAppBarOnLeft = rotation == Surface.ROTATION_270

    if (!isLandscape) {
        // PORTRAIT MODE - Use normal Scaffold with top and bottom bars
        Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                            title = {
                                Text(text = title, style = MaterialTheme.typography.titleLarge)
                            },
                            colors =
                                    TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.background,
                                            titleContentColor =
                                                    MaterialTheme.colorScheme.onBackground
                                    )
                    )
                },
                bottomBar = {
                    BottomAppBar(containerColor = MaterialTheme.colorScheme.background) {
                        controlsContent()
                    }
                }
        ) { innerPadding ->
            // Portrait content
            Column(
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status display
                Box(
                        modifier =
                                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                ) { statusContent() }

                // Game Board - fills remaining space
                Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                ) { boardContent() }
            }
        }
    } else {
        // LANDSCAPE MODE - Use custom layout without Scaffold
        Box(modifier = Modifier.fillMaxSize()) {
            // Layout based on rotation
            if (isAppBarOnLeft) {
                // Layout with app bar on LEFT side
                Row(modifier = Modifier.fillMaxSize()) {
                    // VERTICAL SIDE ACTION BAR - left side
                    Column(
                            modifier =
                                    Modifier.fillMaxHeight().width(64.dp).padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) { controlsContent() }

                    // Content area layout
                    Row(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        // Center - Game Board
                        Box(
                                modifier = Modifier.weight(0.75f).fillMaxHeight(),
                                contentAlignment = Alignment.Center
                        ) { boardContent() }

                        // Right side - Game info column with status
                        Box(
                                modifier =
                                        Modifier.weight(0.25f)
                                                .fillMaxHeight()
                                                .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Title
                                Text(text = title, style = MaterialTheme.typography.titleLarge)

                                Spacer(modifier = Modifier.height(32.dp))

                                // Player status
                                statusContent()
                            }
                        }
                    }
                }
            } else {
                // Layout with app bar on RIGHT side
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left side - Game info column
                    Box(
                            modifier =
                                    Modifier.weight(0.25f)
                                            .fillMaxHeight()
                                            .padding(start = 8.dp, end = 16.dp),
                            contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Title - now here instead of in top bar
                            Text(text = title, style = MaterialTheme.typography.titleLarge)

                            Spacer(modifier = Modifier.height(32.dp))

                            // Player status
                            statusContent()
                        }
                    }

                    // Content area layout
                    Row(modifier = Modifier.weight(1f).fillMaxSize().padding(8.dp)) {
                        // Center - Game Board
                        Box(
                                modifier = Modifier.weight(0.75f).fillMaxHeight(),
                                contentAlignment = Alignment.Center
                        ) { boardContent() }

                        // VERTICAL SIDE ACTION BAR - right side
                        Column(
                                modifier =
                                        Modifier.width(64.dp)
                                                .fillMaxHeight()
                                                .padding(vertical = 16.dp),
                                verticalArrangement = Arrangement.SpaceEvenly,
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) { controlsContent() }
                    }
                }
            }
        }
    }
}
