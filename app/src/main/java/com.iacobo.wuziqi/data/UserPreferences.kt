package com.iacobo.wuziqi.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

/**
 * Enum representing theme modes.
 */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

/**
 * Data class holding user preferences for the app.
 */
data class UserPreferences(
    val soundEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageCode: String = Locale.getDefault().language.let { if (it == "zh") "zh" else "en" }
)

/**
 * Repository for accessing and modifying user preferences.
 */
class UserPreferencesRepository(private val context: Context) {
    
    companion object {
        private val Context.dataStore by preferencesDataStore(name = "user_preferences")
        
        // Preference keys
        private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val LANGUAGE_CODE = stringPreferencesKey("language_code")
    }
    
    /**
     * Get user preferences as a Flow.
     */
    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val soundEnabled = preferences[SOUND_ENABLED] == true
        val themeMode = preferences[THEME_MODE]?.let { 
            try {
                ThemeMode.valueOf(it)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        } ?: ThemeMode.SYSTEM
        val languageCode = preferences[LANGUAGE_CODE] 
            ?: Locale.getDefault().language.let { if (it == "zh") "zh" else "en" }
        
        UserPreferences(soundEnabled, themeMode, languageCode)
    }
    
    /**
     * Update sound enabled preference.
     */
    suspend fun updateSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SOUND_ENABLED] = enabled
        }
    }
    
    /**
     * Update theme mode preference.
     */
    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode.name
        }
    }
    
    /**
     * Update language preference.
     */
    suspend fun updateLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_CODE] = languageCode
        }
    }
}
