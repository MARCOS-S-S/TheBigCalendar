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

        // Agendar o RolloverWorker
        scheduleRolloverWorker()
    }

    private fun scheduleRolloverWorker() {
        val workManager = WorkManager.getInstance(this)
        val rolloverRequest =
            androidx.work.PeriodicWorkRequestBuilder<com.mss.thebigcalendar.worker.RolloverWorker>(
                24, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

        workManager.enqueueUniquePeriodicWork(
            "rollover_worker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            rolloverRequest
        )
    }
}
