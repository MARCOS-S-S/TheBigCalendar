package com.mss.thebigcalendar

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class TheBigCalendarApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Agendar o RolloverWorker
        scheduleRolloverWorker()
    }

    private fun scheduleRolloverWorker() {
        val workManager = WorkManager.getInstance(this)
        val rolloverRequest =
            androidx.work.OneTimeWorkRequestBuilder<com.mss.thebigcalendar.worker.RolloverWorker>()
                .build()

        workManager.enqueue(rolloverRequest)
    }
}
