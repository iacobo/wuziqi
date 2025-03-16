package com.iacobo.wuziqi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import com.iacobo.wuziqi.viewmodel.SettingsViewModel

/**
 * Settings screen that displays user-configurable options.
 * Improved with LazyColumn for better performance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    
    // Get preferences as a direct value
    val preferences = viewModel.userPreferences.value

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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Game Settings Section
            item {
                Text(
                    text = stringResource(R.string.game_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Sound toggle
            item {
                SwitchPreference(
                    title = stringResource(R.string.sounds),
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    isChecked = preferences.soundEnabled,
                    onCheckedChange = { viewModel.updateSoundEnabled(it) }
                )
            }
            
            // Theme selector
            item {
                DropdownPreference(
                    title = stringResource(R.string.theme),
                    icon = Icons.Default.DarkMode,
                    selectedValue = preferences.themeMode.name,
                    options = ThemeMode.entries.map { it.name },
                    onOptionSelected = { viewModel.updateThemeMode(ThemeMode.valueOf(it)) },
                    getOptionLabel = {
                        when (it) {
                            "SYSTEM" -> stringResource(R.string.theme_system)
                            "LIGHT" -> stringResource(R.string.theme_light)
                            "DARK" -> stringResource(R.string.theme_dark)
                            else -> it
                        }
                    }
                )
            }
            
            // Language selector
            item {
                DropdownPreference(
                    title = stringResource(R.string.language),
                    icon = Icons.Default.Language,
                    selectedValue = preferences.languageCode,
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
            
            // Divider
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            
            // About Section
            item {
                Text(
                    text = stringResource(R.string.about),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Links
            item {
                LinkPreference(
                    title = stringResource(R.string.source_code),
                    icon = Icons.Default.Code,
                    onClick = {
                        uriHandler.openUri("https://github.com/iacobo/wuziqi")
                    }
                )
            }
            
            item {
                LinkPreference(
                    title = stringResource(R.string.report_bug),
                    icon = Icons.Default.BugReport,
                    onClick = {
                        uriHandler.openUri("https://github.com/iacobo/wuziqi/issues")
                    }
                )
            }
            
            // Divider
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            
            // Version and tagline
            item {
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
 * Dropdown preference item with icon - optimized version.
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