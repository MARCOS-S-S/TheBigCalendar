package com.mss.thebigcalendar.service

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mss.thebigcalendar.data.repository.AutoBackupSettings
import com.mss.thebigcalendar.data.repository.BackupFrequency
import com.mss.thebigcalendar.worker.AutoBackupWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BackupScheduler(private val context: Context) {

    companion object {
        private const val AUTO_BACKUP_WORK_NAME = "auto_backup_work"
    }

    fun schedule(settings: AutoBackupSettings) {
        if (settings.enabled) {
            val repeatInterval = when (settings.frequency) {
                BackupFrequency.DAILY -> 1
                BackupFrequency.TWO_DAYS -> 2
                BackupFrequency.WEEKLY -> 7
                BackupFrequency.MONTHLY -> 30
            }

            val initialDelay = calculateInitialDelay(settings.hour, settings.minute)

            val backupWorkRequest =
                PeriodicWorkRequestBuilder<AutoBackupWorker>(repeatInterval.toLong(), TimeUnit.DAYS)
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                AUTO_BACKUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                backupWorkRequest
            )
        } else {
            cancel()
        }
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(AUTO_BACKUP_WORK_NAME)
    }

    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (nextRun.before(now)) {
            nextRun.add(Calendar.DAY_OF_MONTH, 1)
        }

        return nextRun.timeInMillis - now.timeInMillis
    }
}
