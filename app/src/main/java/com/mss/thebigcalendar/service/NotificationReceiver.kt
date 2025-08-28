package com.mss.thebigcalendar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mss.thebigcalendar.MainActivity
import com.mss.thebigcalendar.data.repository.ActivityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.LocalDateTime
import com.mss.thebigcalendar.data.model.VisibilityLevel

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "NotificationReceiver acionado com ação: ${intent.action}")
        
        when (intent.action) {
            NotificationService.ACTION_VIEW_ACTIVITY -> {
                // ✅ Exibir a notificação visual quando o alarme for acionado
                Log.d(TAG, "Exibindo notificação visual para atividade agendada")
                handleViewActivity(context, intent)
            }
            NotificationService.ACTION_SNOOZE -> {
                handleSnooze(context, intent)
            }
            NotificationService.ACTION_DISMISS -> {
                handleDismiss(context, intent)
            }
        }
    }

    /**
     * Exibe a notificação visual quando o alarme for acionado
     */
    private fun handleViewActivity(context: Context, intent: Intent) {
        val activityId = intent.getStringExtra(NotificationService.EXTRA_ACTIVITY_ID)
        val activityTitle = intent.getStringExtra(NotificationService.EXTRA_ACTIVITY_TITLE)
        val activityDate = intent.getStringExtra(NotificationService.EXTRA_ACTIVITY_DATE)
        val activityTime = intent.getStringExtra(NotificationService.EXTRA_ACTIVITY_TIME)
        
        Log.d(TAG, "Exibindo notificação visual para: $activityTitle (ID: $activityId)")
        
        // ✅ Buscar a atividade REAL do repositório para obter a visibilidade configurada
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        
        scope.launch {
            try {
                val repository = ActivityRepository(context)
                val notificationService = NotificationService(context)
                
                // ✅ Usar first() em vez de collect() para obter apenas o primeiro valor
                val activities = repository.activities.first()
                val realActivity = activities.find { it.id == activityId }
                
                if (realActivity != null) {
                    Log.d(TAG, "✅ Atividade encontrada no repositório: ${realActivity.title}")
                    Log.d(TAG, "🔍 Visibilidade configurada: ${realActivity.visibility}")
                    Log.d(TAG, "🔔 Configurações de notificação: ${realActivity.notificationSettings}")
                    
                    // ✅ Mudar para Main thread para exibir overlay
                    withContext(Dispatchers.Main) {
                        notificationService.showNotification(realActivity)
                    }
                    
                    Log.d(TAG, "🎉 Notificação visual exibida com sucesso para: ${realActivity.title}")
                } else {
                    Log.w(TAG, "⚠️ Atividade não encontrada no repositório, usando fallback")
                    
                    // Fallback: criar uma atividade temporária se não encontrar a real
                    val tempActivity = com.mss.thebigcalendar.data.model.Activity(
                        id = activityId ?: "unknown",
                        title = activityTitle ?: "Atividade",
                        description = null,
                        date = activityDate ?: "",
                        startTime = activityTime?.let { java.time.LocalTime.parse(it) },
                        endTime = null,
                        isAllDay = false,
                        location = null,
                        categoryColor = "#FF0000",
                        activityType = com.mss.thebigcalendar.data.model.ActivityType.TASK,
                        recurrenceRule = null,
                        notificationSettings = com.mss.thebigcalendar.data.model.NotificationSettings(
                            isEnabled = true,
                            notificationType = com.mss.thebigcalendar.data.model.NotificationType.BEFORE_ACTIVITY
                        ),
                        visibility = com.mss.thebigcalendar.data.model.VisibilityLevel.LOW
                    )
                    
                    // ✅ Mudar para Main thread para exibir overlay
                    withContext(Dispatchers.Main) {
                        notificationService.showNotification(tempActivity)
                    }
                    
                    Log.d(TAG, "📱 Notificação de fallback exibida para: $activityTitle")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao buscar atividade no repositório", e)
                
                // Fallback em caso de erro
                val notificationService = NotificationService(context)
                val tempActivity = com.mss.thebigcalendar.data.model.Activity(
                    id = activityId ?: "unknown",
                    title = activityTitle ?: "Atividade",
                    description = null,
                    date = activityDate ?: "",
                    startTime = activityTime?.let { java.time.LocalTime.parse(it) },
                    endTime = null,
                    isAllDay = false,
                    location = null,
                    categoryColor = "#FF0000",
                    activityType = com.mss.thebigcalendar.data.model.ActivityType.TASK,
                    recurrenceRule = null,
                    notificationSettings = com.mss.thebigcalendar.data.model.NotificationSettings(
                        isEnabled = true,
                        notificationType = com.mss.thebigcalendar.data.model.NotificationType.BEFORE_ACTIVITY
                    ),
                    visibility = com.mss.thebigcalendar.data.model.VisibilityLevel.LOW
                )
                
                // ✅ Mudar para Main thread para exibir overlay
                withContext(Dispatchers.Main) {
                    notificationService.showNotification(tempActivity)
                }
                
                Log.d(TAG, "📱 Notificação de fallback exibida após erro para: $activityTitle")
            }
        }
    }

    /**
     * Adia a notificação por alguns minutos
     */
    private fun handleSnooze(context: Context, intent: Intent) {
        val activityId = intent.getStringExtra(NotificationService.EXTRA_ACTIVITY_ID)
        val snoozeMinutes = intent.getIntExtra("snooze_minutes", 5)
        
        Log.d(TAG, "Adiando notificação para atividade $activityId por $snoozeMinutes minutos")
        
        // Usar CoroutineScope com SupervisorJob para evitar cancelamento
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        
        scope.launch {
            try {
                val repository = ActivityRepository(context)
                
                // Coletar o Flow para obter a lista de atividades
                repository.activities.collect { activities ->
                    val activity = activities.find { it.id == activityId }
                    
                    if (activity != null) {
                        // Reagendar notificação para alguns minutos depois
                        val notificationService = NotificationService(context)
                        
                        // Criar uma atividade temporária com horário ajustado
                        val currentTime = LocalDateTime.now()
                        val snoozedTime = currentTime.plusMinutes(snoozeMinutes.toLong())
                        
                        val snoozedActivity = activity.copy(
                            startTime = snoozedTime.toLocalTime(),
                            date = snoozedTime.toLocalDate().toString()
                        )
                        
                        notificationService.scheduleNotification(snoozedActivity)
                        
                        // Mostrar notificação de confirmação
                        notificationService.showNotification(snoozedActivity)
                        
                        // Parar de coletar após encontrar a atividade
                        return@collect
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao adiar notificação", e)
            }
        }
    }

    /**
     * Cancela a notificação
     */
    private fun handleDismiss(context: Context, intent: Intent) {
        val activityId = intent.getStringExtra(NotificationService.EXTRA_ACTIVITY_ID)
        
        Log.d(TAG, "Cancelando notificação para atividade $activityId")
        
        // Cancelar a notificação agendada
        val notificationService = NotificationService(context)
        if (activityId != null) {
            notificationService.cancelNotification(activityId)
        }
    }
}
