package com.iacobo.wuziqi.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.iacobo.wuziqi.data.ThemeMode
import com.iacobo.wuziqi.data.UserPreferences
import com.iacobo.wuziqi.ui.theme.WuziqiTheme
import com.iacobo.wuziqi.viewmodel.SettingsViewModel
import java.util.Locale

/**
 * Main entry point for the Wuziqi application.
 */
class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val preferences by settingsViewModel.userPreferences.collectAsState()

            // Apply language settings
            ApplyLanguageSettings(preferences)

            // Apply theme settings
            val darkTheme = when (preferences.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            WuziqiTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController = navController)
                }
            }
        }
    }
}

/**
 * Applies language settings based on user preferences.
 */
@Composable
fun ApplyLanguageSettings(preferences: UserPreferences) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    LaunchedEffect(preferences.languageCode) {
        updateLanguage(context, preferences.languageCode)
    }
}

/**
 * Updates the app's language configuration.
 */
fun updateLanguage(context: Context, languageCode: String) {
    val locale = when (languageCode) {
        "zh" -> Locale.SIMPLIFIED_CHINESE
        else -> Locale.ENGLISH
    }

    val config = context.resources.configuration
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}