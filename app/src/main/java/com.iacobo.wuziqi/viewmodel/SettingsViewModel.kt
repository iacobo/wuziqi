package com.iacobo.wuziqi.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iacobo.wuziqi.data.ThemeMode
import com.iacobo.wuziqi.data.UserPreferences
import com.iacobo.wuziqi.data.UserPreferencesRepository
import kotlinx.coroutines.launch

/** ViewModel for managing settings and user preferences. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserPreferencesRepository(application)

    // Internal mutable state
    private val _userPreferences = mutableStateOf(UserPreferences())

    // Exposed as immutable state for the UI
    val userPreferences: State<UserPreferences> = _userPreferences

    init {
        // Collect preferences from repository and update the state
        viewModelScope.launch {
            repository.userPreferencesFlow.collect { prefs -> _userPreferences.value = prefs }
        }
    }

    /** Update sound enabled preference. */
    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateSoundEnabled(enabled) }
    }

    /** Update theme mode preference. */
    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { repository.updateThemeMode(themeMode) }
    }

    /** Update language preference. */
    fun updateLanguage(languageCode: String) {
        viewModelScope.launch { repository.updateLanguage(languageCode) }
    }
}
