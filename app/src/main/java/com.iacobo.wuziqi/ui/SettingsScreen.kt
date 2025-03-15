package com.iacobo.wuziqi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iacobo.wuziqi.BuildConfig
import com.iacobo.wuziqi.R
import com.iacobo.wuziqi.data.ThemeMode
import com.iacobo.wuziqi.data.UserPreferences
import com.iacobo.wuziqi.viewmodel.SettingsViewModel

/**
 * Settings screen that displays user-configurable options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val userPreferences = viewModel.userPreferences.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Game Settings Section
            SettingsSection(title = stringResource(R.string.game_settings)) {
                // Sound toggle with icon
                SwitchPreference(
                    title = stringResource(R.string.sounds),
                    icon = Icons.Default.VolumeUp,
                    isChecked = userPreferences.soundEnabled,
                    onCheckedChange = { viewModel.updateSoundEnabled(it) }
                )

                // Theme selector with icon
                DropdownPreference(
                    title = stringResource(R.string.theme),
                    icon = Icons.Default.DarkMode,
                    selectedValue = userPreferences.themeMode.name,
                    options = ThemeMode.values().map { it.name },
                    onOptionSelected = { viewModel.updateThemeMode(ThemeMode.valueOf(it)) },
                    getOptionLabel = {
                        when (it) {
                            "SYSTEM" -> "System Default"
                            "LIGHT" -> "Light"
                            "DARK" -> "Dark"
                            else -> it
                        }
                    }
                )

                // Language selector with icon
                DropdownPreference(
                    title = stringResource(R.string.language),
                    icon = Icons.Default.Language,
                    selectedValue = userPreferences.languageCode,
                    options = listOf("en", "zh"),
                    onOptionSelected = { viewModel.updateLanguage(it) },
                    getOptionLabel = {
                        when (it) {
                            "en" -> "English"
                            "zh" -> "简体中文"
                            else -> it
                        }
                    }
                )
            }

            // About Section
            SettingsSection(title = stringResource(R.string.about)) {
                // Source code link
                LinkPreference(
                    title = stringResource(R.string.source_code),
                    icon = Icons.Default.Code,
                    onClick = {
                        uriHandler.openUri("https://github.com/iacobo/wuziqi")
                    }
                )

                // Bug report link
                LinkPreference(
                    title = stringResource(R.string.report_bug),
                    icon = Icons.Default.BugReport,
                    onClick = {
                        uriHandler.openUri("https://github.com/iacobo/wuziqi/issues")
                    }
                )
            }

            // Version and tagline
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.version_format, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Section header with title and content.
 */
@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

/**
 * Switch preference item with icon.
 */
@Composable
fun SwitchPreference(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Dropdown preference item with icon.
 */
@Composable
fun DropdownPreference(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    getOptionLabel: (String) -> String = { it }
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        Box {
            Text(
                text = getOptionLabel(selectedValue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(getOptionLabel(option)) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Link preference item.
 */
@Composable
fun LinkPreference(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}