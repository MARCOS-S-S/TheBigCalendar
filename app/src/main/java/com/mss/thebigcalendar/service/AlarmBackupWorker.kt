package com.mss.thebigcalendar.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mss.thebigcalendar.data.repository.AlarmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker para backup de alarmes usando WorkManager
 * Garante que alarmes toquem mesmo se o AlarmManager falhar
 */
class AlarmBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AlarmBackupWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val alarmId = inputData.getString("alarm_id")
            val triggerTime = inputData.getLong("trigger_time", 0L)
            
            if (alarmId == null) {
                Log.e(TAG, "❌ ID do alarme não fornecido")
                return@withContext Result.failure()
            }
            
            Log.d(TAG, "🔔 AlarmBackupWorker executando para alarme: $alarmId")
            
            // Verificar se o alarme ainda é válido (não foi cancelado)
            val alarmRepository = AlarmRepository(applicationContext)
            val alarmSettings = alarmRepository.getAlarmById(alarmId)
            
            if (alarmSettings == null) {
                Log.d(TAG, "🔔 Alarme $alarmId não encontrado ou foi cancelado")
                return@withContext Result.success()
            }
            
            // Verificar se o alarme ainda está ativo
            if (!alarmSettings.isEnabled) {
                Log.d(TAG, "🔔 Alarme $alarmId foi desabilitado")
                return@withContext Result.success()
            }
            
            // Verificar se ainda é o horário correto (com tolerância de 5 minutos)
            val currentTime = System.currentTimeMillis()
            val timeDifference = kotlin.math.abs(currentTime - triggerTime)
            val tolerance = 5 * 60 * 1000L // 5 minutos
            
            if (timeDifference > tolerance) {
                Log.d(TAG, "🔔 Alarme $alarmId fora do horário esperado (diferença: ${timeDifference / 1000}s)")
                return@withContext Result.success()
            }
            
            // Verificar se o alarme já foi processado recentemente usando o AlarmService
            if (AlarmService.isAlarmRecentlyProcessed(alarmId)) {
                Log.d(TAG, "🔔 Alarme $alarmId já foi processado recentemente, pulando backup")
                return@withContext Result.success()
            }
            
            // Disparar o alarme apenas se não foi processado recentemente
            Log.d(TAG, "🔔 Disparando alarme de backup: $alarmId")
            
            val notificationService = NotificationService(applicationContext)
            val alarmService = AlarmService(applicationContext, alarmRepository, notificationService)
            
            // Processar o alarme (isso também reagenda automaticamente)
            alarmService.handleAlarmTriggered(alarmId)
            
            Log.d(TAG, "✅ Alarme de backup processado e reagendado com sucesso: $alarmId")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no AlarmBackupWorker", e)
            Result.failure()
        }
    }
}
