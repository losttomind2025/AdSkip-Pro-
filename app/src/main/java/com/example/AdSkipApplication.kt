package com.example

import android.app.Application
import com.example.data.AdSkipPreferences

class AdSkipApplication : Application() {
    lateinit var preferences: AdSkipPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = AdSkipPreferences(this)
    }

    companion object {
        lateinit var instance: AdSkipApplication
            private set
    }
}
