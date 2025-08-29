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
            Log.d(TAG, "🔄 Iniciando sincronização em background")
            
            // Verificar se há conta Google conectada
            val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
            if (account == null) {
                Log.d(TAG, "⚠️ Nenhuma conta Google conectada")
                return@withContext Result.success()
            }
            
            // Realizar sincronização progressiva
            val googleCalendarService = GoogleCalendarService(applicationContext)
            val progressiveSyncService = ProgressiveSyncService(applicationContext, googleCalendarService)
            
            val result = progressiveSyncService.syncProgressively(account) { progress ->
                Log.d(TAG, "📊 Progresso: ${progress.progress}% - ${progress.currentStep}")
            }
            
            if (result.isSuccess) {
                val totalEvents = result.getOrNull() ?: 0
                Log.d(TAG, "✅ Sincronização em background concluída: $totalEvents eventos")
                Result.success()
            } else {
                val exception = result.exceptionOrNull()
                Log.e(TAG, "❌ Erro na sincronização em background", exception)
                Result.retry()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro inesperado na sincronização em background", e)
            Result.failure()
        }
    }
}
