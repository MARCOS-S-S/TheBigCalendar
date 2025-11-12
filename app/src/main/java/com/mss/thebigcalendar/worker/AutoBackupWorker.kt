package com.mss.thebigcalendar.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mss.thebigcalendar.data.repository.SettingsRepository
import com.mss.thebigcalendar.data.repository.AutoBackupSettings
import com.mss.thebigcalendar.data.service.BackupService
import com.mss.thebigcalendar.service.NotificationService
import kotlinx.coroutines.flow.first
import com.mss.thebigcalendar.data.repository.BackupType
import com.mss.thebigcalendar.service.GoogleAuthService
import com.mss.thebigcalendar.data.repository.ActivityRepository
import com.mss.thebigcalendar.data.repository.DeletedActivityRepository
import com.mss.thebigcalendar.data.repository.CompletedActivityRepository

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settingsRepository = SettingsRepository(applicationContext)
        val notificationService = NotificationService(applicationContext)
        val googleAuthService = GoogleAuthService(applicationContext)

        val settings = settingsRepository.autoBackupSettings.first()

        if (!settings.enabled) {
            return Result.success()
        }

        val activityRepository = ActivityRepository(applicationContext)
        val deletedActivityRepository = DeletedActivityRepository(applicationContext)
        val completedActivityRepository = CompletedActivityRepository(applicationContext)

        val backupService = BackupService(
            applicationContext,
            activityRepository,
            deletedActivityRepository,
            completedActivityRepository
        )

        val result = when (settings.backupType) {
            BackupType.LOCAL -> {
                backupService.createBackup()
            }
            BackupType.CLOUD -> {
                val account = googleAuthService.getLastSignedInAccount()
                if (account != null) {
                    backupService.createCloudBackup(account)
                } else {
                    // If no account is signed in, we can't do a cloud backup.
                    // We could potentially fall back to a local backup or just fail.
                    // For now, we'll just fail.
                    return Result.failure()
                }
            }
        }

        return if (result.isSuccess) {
            notificationService.showAutoBackupSuccessNotification(
                settings.backupType,
                result.getOrNull() ?: ""
            )
            Result.success()
        } else {
            notificationService.showAutoBackupFailedNotification(
                settings.backupType,
                result.exceptionOrNull()?.message ?: "Unknown error"
            )
            Result.failure()
        }
    }
}
