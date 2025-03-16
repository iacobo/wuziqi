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
import androidx.core.view.WindowCompat

/**
 * Main entry point for the Wuziqi application.
 */
class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    
    // Flag to track whether startup sound should be suppressed
    private var isInitialLaunch = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply splash screen
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Make system UI transparent with edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            // Access userPreferences as a State<UserPreferences>
            val preferences = settingsViewModel.userPreferences.value

            // Apply language settings
            ApplyLanguageSettings(preferences)

            // Apply theme settings based on user preferences
            val darkTheme = when (preferences.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                else -> isSystemInDarkTheme() // Fallback
            }

            // Use dynamic theme (Material You) if available, but with our theme colors
            WuziqiTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Pass initial launch flag to suppress startup sound
                    AppNavigation(
                        navController = navController,
                        isInitialLaunch = isInitialLaunch
                    )
                    
                    // Reset the flag after first navigation
                    LaunchedEffect(Unit) {
                        isInitialLaunch = false
                    }
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