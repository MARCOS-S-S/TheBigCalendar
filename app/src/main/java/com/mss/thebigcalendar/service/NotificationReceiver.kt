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
        Log.d(TAG, "🔔 Intent extras: ${intent.extras?.keySet()?.joinToString()}")
        Log.d(TAG, "🔔 Intent data: ${intent.dataString}")
        Log.d(TAG, "🔔 Intent flags: ${intent.flags}")
        
        try {
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
                    Log.d(TAG, "🔔 CLICOU NO BOTÃO FINALIZADO!")
                    handleDismiss(context, intent)
                }
                Intent.ACTION_BOOT_COMPLETED -> {
                    Log.d(TAG, "🔔 Sistema reiniciado - reagendando notificações")
                    // Reagendar todas as notificações após reinicialização
                    scheduleAllNotificationsAfterBoot(context)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔔 Erro no NotificationReceiver", e)
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
                
                // Cancelar a notificação imediatamente
                if (activityId != null) {
                    notificationService.cancelNotification(activityId)
                    Log.d(TAG, "🔔 Notificação cancelada imediatamente")
                }
                
                // ✅ Usar first() em vez de collect() para obter apenas o primeiro valor
                val activities = repository.activities.first()
                
                Log.d(TAG, "🔔 Buscando atividade com ID: $activityId")
                Log.d(TAG, "🔔 Total de atividades no repositório: ${activities.size}")
                
                // ✅ Verificar se é uma instância recorrente (ID contém data)
                val isRecurringInstance = activityId?.contains("_") == true && activityId.split("_").size == 2
                
                val realActivity = if (isRecurringInstance && activityId != null) {
                    // Para instâncias recorrentes, buscar pela atividade base
                    val parts = activityId.split("_")
                    val baseId = parts.getOrNull(0) ?: ""
                    val instanceDate = parts.getOrNull(1) ?: ""
                    val baseActivity = activities.find { it.id == baseId }
                    
                    Log.d(TAG, "🔔 Instância recorrente - Base ID: $baseId, Data: $instanceDate")
                    Log.d(TAG, "🔔 Atividade base encontrada: ${baseActivity != null}")
                    
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
                    val foundActivity = activities.find { it.id == activityId }
                    Log.d(TAG, "🔔 Atividade única encontrada: ${foundActivity != null}")
                    if (foundActivity != null) {
                        Log.d(TAG, "🔔 Atividade encontrada: ${foundActivity.title} - ID: ${foundActivity.id}")
                    }
                    foundActivity
                }
                
                if (realActivity != null) {
                    Log.d(TAG, "🔔 Atividade encontrada, exibindo notificação: ${realActivity.title}")
                    Log.d(TAG, "🔔 Visibilidade da atividade: ${realActivity.visibility}")
                    // ✅ Mudar para Main thread para exibir overlay
                    withContext(Dispatchers.Main) {
                        notificationService.showNotification(realActivity)
                    }

                    // ✅ Agendar a próxima ocorrência se for uma atividade recorrente
                    val recurrenceService = RecurrenceService()
                    if (recurrenceService.isRecurring(realActivity)) {
                        val nextOccurrenceDate = recurrenceService.getNextOccurrence(realActivity, java.time.LocalDate.parse(realActivity.date))
                        if (nextOccurrenceDate != null) {
                            val baseId = realActivity.id.split("_").firstOrNull() ?: realActivity.id
                            val nextActivity = realActivity.copy(
                                id = "${baseId}_${nextOccurrenceDate}",
                                date = nextOccurrenceDate.toString()
                            )
                            notificationService.scheduleNotification(nextActivity)
                        }
                    }
                    
                } else {
                    Log.w(TAG, "⚠️ Atividade não encontrada no repositório, usando fallback")
                    Log.w(TAG, "⚠️ IDs disponíveis: ${activities.map { it.id }}")
                    
                    // Fallback: criar uma atividade temporária se não encontrar a real
                    val visibilityString = intent.getStringExtra(NotificationService.EXTRA_VISIBILITY) ?: VisibilityLevel.LOW.name
                    val visibility = try {
                        VisibilityLevel.valueOf(visibilityString)
                    } catch (e: Exception) {
                        VisibilityLevel.LOW
                    }

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
                        visibility = visibility,
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
        
        Log.d(TAG, "🔔 Adiando notificação por $snoozeMinutes minutos para atividade: $activityId")
        
                    // Cancelar a notificação imediatamente
            val notificationService = NotificationService(context)
            if (activityId != null) {
                notificationService.cancelNotification(activityId)
                Log.d(TAG, "🔔 Notificação cancelada imediatamente")
            }
        
        // Usar CoroutineScope com SupervisorJob para evitar cancelamento
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        
        scope.launch {
            try {
                val repository = ActivityRepository(context)
                
                // Usar first() em vez de collect() para obter apenas o primeiro valor
                val activities = repository.activities.first()
                
                // Verificar se é uma instância recorrente (ID contém data)
                val isRecurringInstance = activityId?.contains("_") == true && activityId.split("_").size == 2
                
                val activity = if (isRecurringInstance) {
                    // Para instâncias recorrentes, buscar pela atividade base
                    val parts = activityId?.split("_")
                    val baseId = parts?.getOrNull(0) ?: ""
                    activities.find { it.id == baseId }
                } else {
                    // Para atividades únicas, buscar pelo ID completo
                    activities.find { it.id == activityId }
                }
                
                Log.d(TAG, "🔍 Buscando atividade para adiar - ID: $activityId")
                Log.d(TAG, "📋 Total de atividades disponíveis: ${activities.size}")
                Log.d(TAG, "🔍 IDs disponíveis: ${activities.map { it.id }}")
                Log.d(TAG, "🔄 É instância recorrente: $isRecurringInstance")
                
                if (activity != null) {
                    Log.d(TAG, "🔔 Atividade encontrada para adiar: ${activity.title}")
                    
                    // Criar uma atividade temporária com horário ajustado
                    val currentTime = LocalDateTime.now()
                    val snoozedTime = currentTime.plusMinutes(snoozeMinutes.toLong())
                    
                    val snoozedActivity = activity.copy(
                        id = if (isRecurringInstance) {
                            // Para instâncias recorrentes, usar o ID com a nova data
                            "${activity.id}_${snoozedTime.toLocalDate()}"
                        } else {
                            // Para atividades únicas, manter o ID original
                            activity.id
                        },
                        startTime = snoozedTime.toLocalTime(),
                        date = snoozedTime.toLocalDate().toString()
                    )
                    
                    // Agendar nova notificação
                    notificationService.scheduleNotification(snoozedActivity)
                    
                    Log.d(TAG, "🔔 Notificação adiada com sucesso para: ${snoozedTime}")
                    
                } else {
                    Log.w(TAG, "⚠️ Atividade não encontrada para adiar: $activityId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao adiar notificação", e)
            }
        }
    }

    /**
     * Marca a atividade como concluída e cancela a notificação
     */
    private fun handleDismiss(context: Context, intent: Intent) {
        val activityId = intent.getStringExtra(NotificationService.EXTRA_ACTIVITY_ID)
        
        Log.d(TAG, "🔔 Marcando atividade como concluída: $activityId")
        
        if (activityId != null) {
            // Cancelar a notificação imediatamente
            val notificationService = NotificationService(context)
            notificationService.cancelNotification(activityId)
            Log.d(TAG, "🔔 Notificação cancelada imediatamente")
            
            // Usar CoroutineScope com SupervisorJob para evitar cancelamento
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            
            scope.launch {
                try {
                    val repository = ActivityRepository(context)
                    val completedRepository = com.mss.thebigcalendar.data.repository.CompletedActivityRepository(context)
                    val recurrenceService = com.mss.thebigcalendar.service.RecurrenceService()
                    
                    // Verificar se é uma instância recorrente (ID contém data)
                    val isRecurringInstance = activityId.contains("_") && activityId.split("_").size == 2
                    
                    if (isRecurringInstance) {
                        // Tratar instância recorrente específica
                        val parts = activityId.split("_")
                        val baseId = parts.getOrNull(0) ?: ""
                        val instanceDate = parts.getOrNull(1) ?: ""
                        
                        Log.d(TAG, "🔄 Processando instância recorrente via notificação - Base ID: $baseId, Data: $instanceDate")
                        
                        // Buscar a atividade base
                        val activities = repository.activities.first()
                        val baseActivity = activities.find { it.id == baseId }
                        
                        Log.d(TAG, "🔍 Buscando atividade base - ID: $baseId")
                        Log.d(TAG, "📋 Total de atividades disponíveis: ${activities.size}")
                        Log.d(TAG, "🔍 IDs disponíveis: ${activities.map { it.id }}")
                        
                        if (baseActivity != null) {
                            Log.d(TAG, "📋 Atividade base encontrada: ${baseActivity.title}")
                            
                            if (recurrenceService.isRecurring(baseActivity)) {
                                Log.d(TAG, "🔄 Atividade é recorrente, processando instância específica")
                                
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
                                Log.d(TAG, "📝 Atividade não é recorrente, tratando como única")
                                
                                // Tratar como atividade única
                                val completedActivity = baseActivity.copy(
                                    id = activityId,
                                    date = instanceDate,
                                    isCompleted = true,
                                    showInCalendar = false
                                )
                                
                                // Salvar no repositório de atividades finalizadas
                                completedRepository.addCompletedActivity(completedActivity)
                                
                                // Remover da lista principal
                                repository.deleteActivity(baseId)
                                
                                Log.d(TAG, "✅ Atividade única marcada como concluída via notificação: ${completedActivity.title}")
                            }
                        } else {
                            Log.w(TAG, "⚠️ Atividade base não encontrada: $baseId")
                        }
                    } else {
                        // Tratar atividade única ou atividade base
                        val activities = repository.activities.first()
                        val activity = activities.find { it.id == activityId }
                        
                        Log.d(TAG, "🔍 Buscando atividade única para marcar como concluída - ID: $activityId")
                        Log.d(TAG, "📋 Total de atividades disponíveis: ${activities.size}")
                        Log.d(TAG, "🔍 IDs disponíveis: ${activities.map { it.id }}")
                        
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
                    
                    Log.d(TAG, "🔔 Processamento concluído com sucesso")
                    
                    // Enviar broadcast para atualizar a UI
                    if (activityId != null) {
                        val updateIntent = Intent("com.mss.thebigcalendar.ACTIVITY_COMPLETED")
                        updateIntent.putExtra("activity_id", activityId)
                        context.sendBroadcast(updateIntent)
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao marcar atividade como concluída via notificação", e)
                    // Em caso de erro, pelo menos cancelar a notificação
                    if (activityId != null) {
                        val notificationService = NotificationService(context)
                        notificationService.cancelNotification(activityId)
                    }
                }
            }
        }
    }
    
    /**
     * Reagenda todas as notificações após reinicialização do sistema
     */
    private fun scheduleAllNotificationsAfterBoot(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activityRepository = ActivityRepository(context)
                val notificationService = NotificationService(context)
                
                val activities = activityRepository.activities.first()
                
                activities.forEach { activity ->
                    if (activity.notificationSettings.isEnabled && 
                        activity.notificationSettings.notificationType != com.mss.thebigcalendar.data.model.NotificationType.NONE) {
                        
                        // Reagendar notificação
                        notificationService.scheduleNotification(activity)
                        Log.d(TAG, "🔔 Notificação reagendada para: ${activity.title}")
                    }
                }
                
                Log.d(TAG, "🔔 Todas as notificações foram reagendadas após reinicialização")
            } catch (e: Exception) {
                Log.e(TAG, "🔔 Erro ao reagendar notificações após reinicialização", e)
            }
        }
    }
}
