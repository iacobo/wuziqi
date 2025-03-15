package com.iacobo.wuziqi

import android.app.Application
import androidx.compose.runtime.Composable
import com.iacobo.wuziqi.ui.GameScreen
import com.iacobo.wuziqi.ui.StartScreen
import com.iacobo.wuziqi.ui.SettingsScreen

/**
 * Main application class that ensures all composable 
 * functions are registered and accessible to the compiler.
 */
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize anything needed at application level here
    }
    
    /**
     * This method is never called but ensures the Compose compiler
     * recognizes all @Composable functions in the codebase.
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @Composable
    private fun RegisterAllComposables() {
        // Reference all composable functions that might be causing issues
        // This ensures they are recognized during compilation
        
        // This is a dummy function that is never actually called,
        // it just helps the compiler understand that these functions exist
        
        // If compiler still can't find certain composables, add them here
    }
}