package com.iacobo.wuziqi

import android.app.Application

/**
 * Main application class that ensures all composable 
 * functions are registered and accessible to the compiler.
 */
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize anything needed at application level here
    }
}