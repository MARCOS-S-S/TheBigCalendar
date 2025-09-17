package com.mss.thebigcalendar.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mss.thebigcalendar.data.repository.ActivityRepository
import com.mss.thebigcalendar.service.NotificationService
import kotlinx.coroutines.flow.first

/**
 * Worker para verificar e enviar notificações pendentes
 * Funciona como backup para o AlarmManager
 */
class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "NotificationWorker"
        const val WORK_NAME = "notification_worker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "🔔 NotificationWorker executando")
            
            val activityRepository = ActivityRepository(applicationContext)
            val notificationService = NotificationService(applicationContext)
            
            // Buscar atividades com notificações habilitadas
            val activities = activityRepository.activities.first()
            val currentTime = System.currentTimeMillis()
            
            var notificationsSent = 0
            
            activities.forEach { activity ->
                if (activity.notificationSettings.isEnabled && 
                    activity.notificationSettings.notificationType != com.mss.thebigcalendar.data.model.NotificationType.NONE) {
                    
                    // Calcular quando a notificação deveria ser enviada
                    val triggerTime = getTriggerTimeWithNotificationType(activity)
                    
                    // Se a notificação deveria ter sido enviada nos últimos 5 minutos
                    val timeDiff = currentTime - triggerTime
                    if (timeDiff >= 0 && timeDiff <= 300000) { // 5 minutos
                        Log.d(TAG, "🔔 Enviando notificação tardia para: ${activity.title}")
                        notificationService.showNotification(activity)
                        notificationsSent++
                    }
                }
            }
            
            Log.d(TAG, "🔔 NotificationWorker concluído. Notificações enviadas: $notificationsSent")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "🔔 Erro no NotificationWorker", e)
            Result.retry()
        }
    }
    
    private fun calculateNotificationTime(activity: com.mss.thebigcalendar.data.model.Activity): java.time.LocalTime {
        val startTime = activity.startTime ?: java.time.LocalTime.of(9, 0)
        
        return when (activity.notificationSettings.notificationType) {
            com.mss.thebigcalendar.data.model.NotificationType.FIFTEEN_MINUTES_BEFORE -> 
                startTime.minusMinutes(15)
            com.mss.thebigcalendar.data.model.NotificationType.THIRTY_MINUTES_BEFORE -> 
                startTime.minusMinutes(30)
            com.mss.thebigcalendar.data.model.NotificationType.ONE_HOUR_BEFORE -> 
                startTime.minusHours(1)
            com.mss.thebigcalendar.data.model.NotificationType.ONE_DAY_BEFORE -> 
                startTime // Para um dia antes, manter o mesmo horário
            else -> startTime
        }
    }
    
    private fun getTriggerTime(dateString: String, notificationTime: java.time.LocalTime): Long {
        val date = java.time.LocalDate.parse(dateString)
        val dateTime = java.time.LocalDateTime.of(date, notificationTime)
        return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    
    private fun getTriggerTimeWithNotificationType(activity: com.mss.thebigcalendar.data.model.Activity): Long {
        val activityDate = java.time.LocalDate.parse(activity.date)
        val startTime = activity.startTime ?: java.time.LocalTime.of(9, 0)
        
        val notificationDateTime = when (activity.notificationSettings.notificationType) {
            com.mss.thebigcalendar.data.model.NotificationType.FIFTEEN_MINUTES_BEFORE -> 
                java.time.LocalDateTime.of(activityDate, startTime.minusMinutes(15))
            com.mss.thebigcalendar.data.model.NotificationType.THIRTY_MINUTES_BEFORE -> 
                java.time.LocalDateTime.of(activityDate, startTime.minusMinutes(30))
            com.mss.thebigcalendar.data.model.NotificationType.ONE_HOUR_BEFORE -> 
                java.time.LocalDateTime.of(activityDate, startTime.minusHours(1))
            com.mss.thebigcalendar.data.model.NotificationType.ONE_DAY_BEFORE -> 
                java.time.LocalDateTime.of(activityDate.minusDays(1), startTime)
            else -> 
                java.time.LocalDateTime.of(activityDate, startTime)
        }
        
        return notificationDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
