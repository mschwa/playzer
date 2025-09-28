package com.thorfio.playzer

import android.app.Application
import android.util.Log
import com.thorfio.playzer.core.ServiceLocator

class PlayzerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        Log.d("PlayzerApp", "PlayzerApplication initialized")
        // Future: initialize DI, logging, crash reporting, media libs, etc.
    }
}
