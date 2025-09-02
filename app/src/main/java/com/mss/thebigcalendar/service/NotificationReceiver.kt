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
        Log.d(TAG, "🔔 NotificationReceiver.onReceive chamado")
        Log.d(TAG, "🔔 Action: ${intent.action}")
        Log.d(TAG, "🔔 Timestamp atual: ${System.currentTimeMillis()}")
        
        when (intent.action) {
            NotificationService.ACTION_VIEW_ACTIVITY -> {
                // ✅ Exibir a notificação visual quando o alarme for acionado
                Log.d(TAG, "🔔 Processando ACTION_VIEW_ACTIVITY")
                handleViewActivity(context, intent)
            }
            NotificationService.ACTION_SNOOZE -> {
                Log.d(TAG, "🔔 Processando ACTION_SNOOZE")
                handleSnooze(context, intent)
            }
            NotificationService.ACTION_DISMISS -> {
                Log.d(TAG, "🔔 Processando ACTION_DISMISS")
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
        

        
        // ✅ Buscar a atividade REAL do repositório para obter a visibilidade configurada
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        
        scope.launch {
            try {
                val repository = ActivityRepository(context)
                val notificationService = NotificationService(context)
                
                // ✅ Usar first() em vez de collect() para obter apenas o primeiro valor
                val activities = repository.activities.first()
                
                // ✅ Verificar se é uma instância recorrente (ID contém data)
                val isRecurringInstance = activityId?.contains("_") == true && activityId.split("_").size == 2
                
                val realActivity = if (isRecurringInstance) {
                    // Para instâncias recorrentes, buscar pela atividade base
                    val baseId = activityId.split("_")[0]
                    val instanceDate = activityId.split("_")[1]
                    val baseActivity = activities.find { it.id == baseId }
                    
                    if (baseActivity != null) {
                        // Criar uma instância específica da atividade base
                        baseActivity.copy(
                            id = activityId,
                            date = instanceDate
                        )
                    } else {
                        null
                    }
                } else {
                    // Para atividades únicas, buscar normalmente
                    activities.find { it.id == activityId }
                }
                
                if (realActivity != null) {
                    // ✅ Mudar para Main thread para exibir overlay
                    withContext(Dispatchers.Main) {
                        notificationService.showNotification(realActivity)
                    }
                    
                } else {
                    Log.w(TAG, "⚠️ Atividade não encontrada no repositório, usando fallback")
                    
                    // Fallback: criar uma atividade temporária se não encontrar a real
                    val tempActivity = com.mss.thebigcalendar.data.model.Activity(
                        id = activityId ?: "unknown",
                        title = activityTitle ?: "Atividade",
                        description = null,
                        date = activityDate ?: "",
                        startTime = activityTime?.takeIf { it.isNotEmpty() && it != "null" }?.let { java.time.LocalTime.parse(it) },
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
                        visibility = com.mss.thebigcalendar.data.model.VisibilityLevel.LOW,
                        showInCalendar = true
                    )
                    
                    // ✅ Mudar para Main thread para exibir overlay
                    withContext(Dispatchers.Main) {
                        notificationService.showNotification(tempActivity)
                    }
                    
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
                    startTime = activityTime?.takeIf { it.isNotEmpty() && it != "null" }?.let { java.time.LocalTime.parse(it) },
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
                    visibility = com.mss.thebigcalendar.data.model.VisibilityLevel.LOW,
                    showInCalendar = true
                )
                
                // ✅ Mudar para Main thread para exibir overlay
                withContext(Dispatchers.Main) {
                    notificationService.showNotification(tempActivity)
                }
                
            }
        }
    }

    /**
     * Adia a notificação por alguns minutos
     */
    private fun handleSnooze(context: Context, intent: Intent) {
        val activityId = intent.getStringExtra(NotificationService.EXTRA_ACTIVITY_ID)
        val snoozeMinutes = intent.getIntExtra("snooze_minutes", 5)
        
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
     * Marca a atividade como concluída e cancela a notificação
     */
    private fun handleDismiss(context: Context, intent: Intent) {
        val activityId = intent.getStringExtra(NotificationService.EXTRA_ACTIVITY_ID)
        
        if (activityId != null) {
            // Usar CoroutineScope com SupervisorJob para evitar cancelamento
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            
            scope.launch {
                try {
                    val repository = ActivityRepository(context)
                    val completedRepository = com.mss.thebigcalendar.data.repository.CompletedActivityRepository(context)
                    val notificationService = NotificationService(context)
                    val recurrenceService = com.mss.thebigcalendar.service.RecurrenceService()
                    
                    // Verificar se é uma instância recorrente (ID contém data)
                    val isRecurringInstance = activityId.contains("_") && activityId.split("_").size == 2
                    
                    if (isRecurringInstance) {
                        // Tratar instância recorrente específica
                        val parts = activityId.split("_")
                        val baseId = parts[0]
                        val instanceDate = parts[1]
                        
                        Log.d(TAG, "🔄 Processando instância recorrente via notificação - Base ID: $baseId, Data: $instanceDate")
                        
                        // Buscar a atividade base
                        val activities = repository.activities.first()
                        val baseActivity = activities.find { it.id == baseId }
                        
                        if (baseActivity != null && recurrenceService.isRecurring(baseActivity)) {
                            Log.d(TAG, "📋 Atividade base encontrada: ${baseActivity.title}")
                            
                            // Criar instância específica para salvar como concluída
                            val instanceToComplete = baseActivity.copy(
                                id = activityId,
                                date = instanceDate,
                                isCompleted = true,
                                showInCalendar = false
                            )
                            
                            // Salvar instância específica como concluída
                            completedRepository.addCompletedActivity(instanceToComplete)
                            
                            // Adicionar data à lista de exclusões da atividade base
                            val updatedExcludedDates = baseActivity.excludedDates + instanceDate
                            val updatedBaseActivity = baseActivity.copy(excludedDates = updatedExcludedDates)
                            
                            // Atualizar a atividade base com a nova lista de exclusões
                            repository.saveActivity(updatedBaseActivity)
                            
                            Log.d(TAG, "✅ Instância recorrente marcada como concluída via notificação: ${instanceToComplete.title} - Data: $instanceDate")
                            
                        } else {
                            Log.w(TAG, "⚠️ Atividade base não encontrada ou não é recorrente: $baseId")
                        }
                    } else {
                        // Tratar atividade única ou atividade base
                        val activities = repository.activities.first()
                        val activity = activities.find { it.id == activityId }
                        
                        if (activity != null) {
                            // Verificar se é uma atividade recorrente
                            if (recurrenceService.isRecurring(activity)) {
                                // Para atividades recorrentes (primeira instância), sempre tratar como instância específica
                                val activityDate = activity.date
                                
                                Log.d(TAG, "🔄 Processando primeira instância recorrente via notificação - ID: $activityId, Data: $activityDate")
                                
                                // Criar instância específica para salvar como concluída
                                val instanceToComplete = activity.copy(
                                    id = activityId,
                                    date = activityDate,
                                    isCompleted = true,
                                    showInCalendar = false
                                )
                                
                                // Salvar instância específica como concluída
                                completedRepository.addCompletedActivity(instanceToComplete)
                                
                                // Adicionar data à lista de exclusões da atividade base
                                val updatedExcludedDates = activity.excludedDates + activityDate
                                val updatedBaseActivity = activity.copy(excludedDates = updatedExcludedDates)
                                
                                // Atualizar a atividade base com a nova lista de exclusões
                                repository.saveActivity(updatedBaseActivity)
                                
                                Log.d(TAG, "✅ Primeira instância recorrente marcada como concluída via notificação: ${instanceToComplete.title} - Data: $activityDate")
                                
                            } else {
                                // Tratar atividade única (não recorrente)
                                Log.d(TAG, "✅ Marcando atividade única como concluída via notificação: ${activity.title}")
                                
                                // Marcar como concluída e salvar no repositório de finalizadas
                                val completedActivity = activity.copy(
                                    isCompleted = true,
                                    showInCalendar = false
                                )
                                
                                // Salvar no repositório de atividades finalizadas
                                completedRepository.addCompletedActivity(completedActivity)
                                
                                // Remover da lista principal
                                repository.deleteActivity(activityId)
                                
                                Log.d(TAG, "✅ Atividade única marcada como concluída via notificação: ${completedActivity.title}")
                            }
                        } else {
                            Log.w(TAG, "⚠️ Atividade não encontrada para marcar como concluída: $activityId")
                        }
                    }
                    
                    // Cancelar a notificação agendada
                    notificationService.cancelNotification(activityId)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao marcar atividade como concluída via notificação", e)
                    // Em caso de erro, pelo menos cancelar a notificação
                    val notificationService = NotificationService(context)
                    notificationService.cancelNotification(activityId)
                }
            }
        }
    }
}
