package com.mss.thebigcalendar

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class TheBigCalendarApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar WorkManager
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()
        )
    }
}
