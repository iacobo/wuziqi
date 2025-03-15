package com.iacobo.wuziqi.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iacobo.wuziqi.data.ThemeMode
import com.iacobo.wuziqi.data.UserPreferences
import com.iacobo.wuziqi.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing settings and user preferences.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = UserPreferencesRepository(application)
    
    // Expose user preferences as a state flow for the UI
    val userPreferences: StateFlow<UserPreferences> = repository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )
    
    /**
     * Update sound enabled preference.
     */
    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSoundEnabled(enabled)
        }
    }
    
    /**
     * Update theme mode preference.
     */
    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            repository.updateThemeMode(themeMode)
        }
    }
    
    /**
     * Update language preference.
     */
    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            repository.updateLanguage(languageCode)
        }
    }
}