package com.mss.thebigcalendar.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.mss.thebigcalendar.service.GoogleCalendarService
import com.mss.thebigcalendar.service.ProgressiveSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleCalendarSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "GoogleCalendarSyncWorker"
        const val WORK_NAME = "google_calendar_sync"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            
            // Verificar se há conta Google conectada
            val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
            if (account == null) {
                return@withContext Result.success()
            }
            
            // Realizar sincronização progressiva
            val googleCalendarService = GoogleCalendarService(applicationContext)
            val progressiveSyncService = ProgressiveSyncService(applicationContext, googleCalendarService)
            
            val result = progressiveSyncService.syncProgressively(account) { progress ->
                // Progresso da sincronização
            }
            
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
            
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
