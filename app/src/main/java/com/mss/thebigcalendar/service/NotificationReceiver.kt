package com.mss.thebigcalendar.service

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
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
import java.time.LocalDate
import com.mss.thebigcalendar.data.model.VisibilityLevel

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        
        try {
            when (intent.action) {
                NotificationService.ACTION_VIEW_ACTIVITY -> {
                    // ✅ Exibir a notificação visual quando o alarme for acionado
                    handleViewActivity(context, intent)
                }
                NotificationService.ACTION_SNOOZE -> {
                    handleSnooze(context, intent)
                }
                NotificationService.ACTION_DISMISS -> {
                    handleDismiss(context, intent)
                }
                Intent.ACTION_BOOT_COMPLETED -> {
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
        
        Log.d(TAG, "🔔 handleViewActivity chamado para ID: $activityId")
        
        // ✅ Verificação imediata: se a atividade foi deletada, não processar a notificação
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        
        scope.launch {
            try {
                val repository = ActivityRepository(context)
                
                // ✅ Verificar PRIMEIRO se a atividade ainda existe
                val activities = repository.activities.first()
                val activityExists = activities.any { 
                    it.id == activityId || 
                    (activityId?.contains("_") == true && it.id == activityId.split("_")[0])
                }
                
                if (!activityExists) {
                    Log.d(TAG, "🔔 Atividade $activityId foi deletada - cancelando notificação sem exibir")
                    val notificationService = NotificationService(context)
                    notificationService.cancelNotification(activityId ?: "")
                    return@launch
                }
                
                Log.d(TAG, "🔔 Atividade $activityId ainda existe - processando notificação")
                
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
                    
                    // ✅ Verificar se é notificação de alta visibilidade
                    if (realActivity.visibility == com.mss.thebigcalendar.data.model.VisibilityLevel.HIGH) {
                        Log.d(TAG, "🔔 Atividade de alta visibilidade - iniciando serviço especializado")
                        
                        // Iniciar serviço de alta visibilidade
                        val highVisibilityIntent = Intent(context, com.mss.thebigcalendar.service.HighVisibilityNotificationService::class.java).apply {
                            action = com.mss.thebigcalendar.service.HighVisibilityNotificationService.ACTION_SHOW_NOTIFICATION
                            putExtra(com.mss.thebigcalendar.service.HighVisibilityNotificationService.EXTRA_ACTIVITY_ID, realActivity.id)
                            putExtra(com.mss.thebigcalendar.service.HighVisibilityNotificationService.EXTRA_ACTIVITY_TITLE, realActivity.title)
                            putExtra(com.mss.thebigcalendar.service.HighVisibilityNotificationService.EXTRA_ACTIVITY_DESCRIPTION, realActivity.description)
                            putExtra(com.mss.thebigcalendar.service.HighVisibilityNotificationService.EXTRA_ACTIVITY_DATE, realActivity.date)
                            putExtra(com.mss.thebigcalendar.service.HighVisibilityNotificationService.EXTRA_ACTIVITY_TIME, realActivity.startTime?.toString())
                        }
                        
                        context.startService(highVisibilityIntent)
                    } else {
                        // ✅ Mudar para Main thread para exibir overlay normal
                        withContext(Dispatchers.Main) {
                            val notificationService = NotificationService(context)
                            notificationService.showNotification(realActivity)
                        }
                    }

                    // ✅ Agendar a próxima ocorrência APENAS se for uma atividade recorrente base
                    // e não for uma instância específica (que já foi processada)
                    val recurrenceService = RecurrenceService()
                    val isRecurringBase = recurrenceService.isRecurring(realActivity) && !realActivity.id.contains("_")
                    
                    if (isRecurringBase) {
                        Log.d(TAG, "🔔 Agendando próxima ocorrência para atividade recorrente: ${realActivity.title}")
                        val nextOccurrenceDate = recurrenceService.getNextOccurrence(realActivity, java.time.LocalDate.parse(realActivity.date))
                        if (nextOccurrenceDate != null) {
                            val nextActivity = realActivity.copy(
                                id = "${realActivity.id}_${nextOccurrenceDate}",
                                date = nextOccurrenceDate.toString()
                            )
                            val notificationService = NotificationService(context)
                            notificationService.scheduleNotification(nextActivity)
                        }
                    } else {
                        Log.d(TAG, "🔔 Não agendando próxima ocorrência - atividade não é base recorrente ou já é instância específica")
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
                    
                    // ✅ Verificar se é notificação de alta visibilidade
                    if (tempActivity.visibility == com.mss.thebigcalendar.data.model.VisibilityLevel.HIGH) {
                        Log.d(TAG, "🔔 Atividade de alta visibilidade (fallback) - iniciando serviço especializado")
                        
                        // Iniciar serviço de alta visibilidade
                        val highVisibilityIntent = Intent(context, com.mss.thebigcalendar.service.HighVisibilityNotificationService::class.java).apply {
                            action = com.mss.thebigcalendar.service.HighVisibilityNotificationService.ACTION_SHOW_NOTIFICATION
                            putExtra(com.mss.thebigcalendar.service.HighVisibilityNotificationService.EXTRA_ACTIVITY_ID, tempActivity.id)
                            putExtra(com.mss.thebigcalendar.service.HighVisibilityNotificationService.EXTRA_ACTIVITY_TITLE, tempActivity.title)
                            putExtra(com.mss.thebigcalendar.service.HighVisibilityNotificationService.EXTRA_ACTIVITY_DESCRIPTION, tempActivity.description)
                            putExtra(com.mss.thebigcalendar.service.HighVisibilityNotificationService.EXTRA_ACTIVITY_DATE, tempActivity.date)
                            putExtra(com.mss.thebigcalendar.service.HighVisibilityNotificationService.EXTRA_ACTIVITY_TIME, tempActivity.startTime?.toString())
                        }
                        
                        context.startService(highVisibilityIntent)
                    } else {
                        // ✅ Mudar para Main thread para exibir overlay normal
                        withContext(Dispatchers.Main) {
                            val notificationService = NotificationService(context)
                            notificationService.showNotification(tempActivity)
                        }
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
                
                // ✅ Verificar se é notificação de alta visibilidade
                if (tempActivity.visibility == com.mss.thebigcalendar.data.model.VisibilityLevel.HIGH) {
                    Log.d(TAG, "🔔 Atividade de alta visibilidade (fallback 2) - iniciando serviço especializado")
                    
                    // Iniciar serviço de alta visibilidade
                    val highVisibilityIntent = Intent(context, com.mss.thebigcalendar.service.HighVisibilityNotificationService::class.java).apply {
                        action = com.mss.thebigcalendar.service.HighVisibilityNotificationService.ACTION_SHOW_NOTIFICATION
                        putExtra(com.mss.thebigcalendar.service.HighVisibilityNotificationService.EXTRA_ACTIVITY_ID, tempActivity.id)
                        putExtra(com.mss.thebigcalendar.service.HighVisibilityNotificationService.EXTRA_ACTIVITY_TITLE, tempActivity.title)
                        putExtra(com.mss.thebigcalendar.service.HighVisibilityNotificationService.EXTRA_ACTIVITY_DESCRIPTION, tempActivity.description)
                        putExtra(com.mss.thebigcalendar.service.HighVisibilityNotificationService.EXTRA_ACTIVITY_DATE, tempActivity.date)
                        putExtra(com.mss.thebigcalendar.service.HighVisibilityNotificationService.EXTRA_ACTIVITY_TIME, tempActivity.startTime?.toString())
                    }
                    
                    context.startService(highVisibilityIntent)
                } else {
                    // ✅ Mudar para Main thread para exibir overlay normal
                    withContext(Dispatchers.Main) {
                        val notificationService = NotificationService(context)
                        notificationService.showNotification(tempActivity)
                    }
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
                
                // ✅ Verificar PRIMEIRO se a atividade ainda existe
                val activityExists = activities.any { 
                    it.id == activityId || 
                    (activityId?.contains("_") == true && it.id == activityId.split("_")[0])
                }
                
                if (!activityExists) {
                    Log.d(TAG, "🔔 Atividade $activityId foi deletada - cancelando adiamento")
                    return@launch
                }
                
                // Verificar se é uma instância recorrente (ID contém data e horário)
                val isRecurringInstance = activityId?.contains("_") == true && activityId.split("_").size >= 2
                
                val activity = if (isRecurringInstance) {
                    // Para instâncias recorrentes, buscar pela atividade base
                    val parts = activityId?.split("_")
                    val baseId = parts?.getOrNull(0) ?: ""
                    val baseActivity = activities.find { it.id == baseId }
                    
                    Log.d(TAG, "🔍 Verificando se é instância recorrente - Base ID: $baseId")
                    Log.d(TAG, "🔍 Atividade base encontrada: ${baseActivity != null}")
                    Log.d(TAG, "🔍 Atividade base é recorrente: ${baseActivity?.recurrenceRule?.isNotEmpty() == true}")
                    
                    // Se encontrou a atividade base e ela é recorrente, é realmente uma instância
                    if (baseActivity != null && baseActivity.recurrenceRule?.isNotEmpty() == true) {
                        Log.d(TAG, "🔍 Confirmado: É instância recorrente de atividade base")
                        baseActivity
                    } else {
                        // Se a atividade base não é recorrente, tratar como atividade única
                        Log.d(TAG, "🔍 Não é instância recorrente, buscando como atividade única")
                        val foundActivity = activities.find { it.id == activityId }
                        if (foundActivity != null) {
                            Log.d(TAG, "🔍 Atividade encontrada pelo ID completo")
                            foundActivity
                        } else {
                            // Se não encontrou pelo ID completo, tentar buscar pela atividade base
                            Log.d(TAG, "🔍 Atividade não encontrada pelo ID completo, tentando buscar pela base")
                            val baseActivity = activities.find { it.id == baseId }
                            if (baseActivity != null) {
                                Log.d(TAG, "🔍 Atividade base encontrada, usando ela para adiamento")
                                baseActivity
                            } else {
                                Log.d(TAG, "🔍 Atividade base também não encontrada")
                                null
                            }
                        }
                    }
                } else {
                    // Para atividades únicas, buscar pelo ID completo
                    Log.d(TAG, "🔍 Buscando como atividade única (ID sem underscore)")
                    activities.find { it.id == activityId }
                }
                
                Log.d(TAG, "🔍 Buscando atividade para adiar - ID: $activityId")
                Log.d(TAG, "📋 Total de atividades disponíveis: ${activities.size}")
                Log.d(TAG, "🔍 IDs disponíveis: ${activities.map { it.id }}")
                Log.d(TAG, "🔄 É instância recorrente: $isRecurringInstance")
                
                if (activity != null) {
                    Log.d(TAG, "🔔 Atividade encontrada para adiar: ${activity.title}")
                    Log.d(TAG, "🔔 Atividade ID: ${activity.id}")
                    Log.d(TAG, "🔔 Atividade recurrenceRule: ${activity.recurrenceRule}")
                    Log.d(TAG, "🔔 Atividade é recorrente: ${activity.recurrenceRule?.isNotEmpty() == true}")
                    
                    // Criar uma atividade temporária com horário ajustado
                    val currentTime = LocalDateTime.now()
                    val snoozedTime = currentTime.plusMinutes(snoozeMinutes.toLong())
                    
                    if (activity.recurrenceRule?.isNotEmpty() == true) {
                        // Para atividades recorrentes, criar instância específica adiada e salvar no repositório
                        
                        // Extrair informações da instância original para excluí-la
                        val originalInstanceDate = if (isRecurringInstance && activityId != null) {
                            val parts = activityId.split("_")
                            parts.getOrNull(1) ?: activity.date
                        } else {
                            activity.date
                        }
                        
                        val originalInstanceTime = if (isRecurringInstance && activityId != null && activity.recurrenceRule?.startsWith("FREQ=HOURLY") == true) {
                            val parts = activityId.split("_")
                            val timePart = parts.getOrNull(2)
                            if (timePart != null) {
                                try {
                                    java.time.LocalTime.parse(timePart)
                                } catch (e: Exception) {
                                    activity.startTime
                                }
                            } else {
                                activity.startTime
                            }
                        } else {
                            activity.startTime
                        }
                        
                        // Criar ID da instância original para exclusão
                        val originalInstanceId = if (activity.recurrenceRule?.startsWith("FREQ=HOURLY") == true) {
                            val timeString = originalInstanceTime?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) ?: "00:00"
                            "${activity.id}_${originalInstanceDate}_${timeString}"
                        } else {
                            "${activity.id}_${originalInstanceDate}"
                        }
                        
                        // Criar nova instância com horário adiado
                        val snoozedActivity = activity.copy(
                            id = if (activity.recurrenceRule?.startsWith("FREQ=HOURLY") == true) {
                                val timeString = snoozedTime.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                                "${activity.id}_${snoozedTime.toLocalDate()}_${timeString}"
                            } else {
                                "${activity.id}_${snoozedTime.toLocalDate()}"
                            },
                            startTime = snoozedTime.toLocalTime(),
                            date = snoozedTime.toLocalDate().toString(),
                            recurrenceRule = null // Remover recorrência para que seja tratada como instância única
                        )
                        
                        Log.d(TAG, "🔔 Criando instância adiada para atividade recorrente: ${snoozedActivity.title} - ${snoozedActivity.date} ${snoozedActivity.startTime}")
                        Log.d(TAG, "🔔 Excluindo instância original: $originalInstanceId")
                        Log.d(TAG, "🔔 Data original para exclusão: $originalInstanceDate")
                        Log.d(TAG, "🔔 Atividade base ID: ${activity.id}")
                        Log.d(TAG, "🔔 Atividade base recurrenceRule: ${activity.recurrenceRule}")
                        
                        // Adicionar instância original à lista de exclusões da atividade base
                        val updatedBaseActivity = if (activity.recurrenceRule?.startsWith("FREQ=HOURLY") == true) {
                            val updatedExcludedInstances = activity.excludedInstances + originalInstanceId
                            Log.d(TAG, "🔔 Excluindo instância HOURLY: $originalInstanceId")
                            Log.d(TAG, "🔔 Lista de exclusões atualizada: $updatedExcludedInstances")
                            activity.copy(excludedInstances = updatedExcludedInstances)
                        } else {
                            val updatedExcludedDates = activity.excludedDates + originalInstanceDate
                            Log.d(TAG, "🔔 Excluindo data: $originalInstanceDate")
                            Log.d(TAG, "🔔 Lista de datas excluídas atualizada: $updatedExcludedDates")
                            activity.copy(excludedDates = updatedExcludedDates)
                        }
                        
                        // Salvar a atividade base atualizada com a exclusão
                        repository.saveActivity(updatedBaseActivity)
                        Log.d(TAG, "🔔 Atividade base atualizada com exclusão salva no repositório")
                        
                        // Para atividades HOURLY, remover a instância original específica do repositório
                        if (activity.recurrenceRule?.startsWith("FREQ=HOURLY") == true) {
                            try {
                                repository.deleteActivity(originalInstanceId)
                                Log.d(TAG, "🔔 Instância original HOURLY removida do repositório: $originalInstanceId")
                            } catch (e: Exception) {
                                Log.w(TAG, "⚠️ Erro ao remover instância original HOURLY: $originalInstanceId", e)
                            }
                        }
                        
                        // Salvar a instância adiada no repositório para que apareça no calendário
                        repository.saveActivity(snoozedActivity)
                        
                        // Agendar nova notificação
                        val notificationService = NotificationService(context)
                        notificationService.scheduleNotification(snoozedActivity)
                        
                        // ✅ Marcar atividade como adiada para evitar notificações tardias desnecessárias
                        val prefs = context.getSharedPreferences("notification_tracking", Context.MODE_PRIVATE)
                        prefs.edit().putLong("activity_snoozed_${snoozedActivity.id}", System.currentTimeMillis()).apply()
                        // ✅ Também marcar a atividade base como adiada para evitar notificações tardias do Worker
                        prefs.edit().putLong("activity_snoozed_${activity.id}", System.currentTimeMillis()).apply()
                        Log.d(TAG, "🔔 Atividade marcada como adiada: ${snoozedActivity.id} e base: ${activity.id}")
                        
                        Log.d(TAG, "🔔 Instância recorrente adiada e salva no repositório - instância original excluída - nova notificação agendada para: ${snoozedTime}")
                        
                        // Forçar atualização dos widgets para refletir as mudanças
                        try {
                            val widgetIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                            widgetIntent.component = ComponentName(context, "com.mss.thebigcalendar.widget.GreetingWidgetProvider")
                            context.sendBroadcast(widgetIntent)
                            Log.d(TAG, "🔔 Widget atualizado após adiamento")
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Erro ao atualizar widget", e)
                        }
                    } else {
                        // Para atividades não recorrentes, atualizar a atividade original no repositório
                        Log.d(TAG, "🔔 Processando atividade NÃO RECORRENTE: ${activity.title}")
                        Log.d(TAG, "🔔 Horário original: ${activity.startTime}")
                        Log.d(TAG, "🔔 Data original: ${activity.date}")
                        Log.d(TAG, "🔔 Novo horário adiado: ${snoozedTime.toLocalTime()}")
                        Log.d(TAG, "🔔 Nova data adiada: ${snoozedTime.toLocalDate()}")
                        
                        val updatedActivity = activity.copy(
                            startTime = snoozedTime.toLocalTime(),
                            date = snoozedTime.toLocalDate().toString()
                        )
                        
                        Log.d(TAG, "🔔 Atividade atualizada - ID: ${updatedActivity.id}")
                        Log.d(TAG, "🔔 Atividade atualizada - Horário: ${updatedActivity.startTime}")
                        Log.d(TAG, "🔔 Atividade atualizada - Data: ${updatedActivity.date}")
                        
                        // Salvar a atividade atualizada no repositório
                        repository.saveActivity(updatedActivity)
                        Log.d(TAG, "🔔 Atividade salva no repositório com novo horário")
                        
                        // Agendar nova notificação com a atividade atualizada
                        val notificationService = NotificationService(context)
                        notificationService.scheduleNotification(updatedActivity)
                        Log.d(TAG, "🔔 Nova notificação agendada para: ${snoozedTime}")
                        
                        // ✅ Marcar atividade como adiada para evitar notificações tardias desnecessárias
                        val prefs = context.getSharedPreferences("notification_tracking", Context.MODE_PRIVATE)
                        prefs.edit().putLong("activity_snoozed_${updatedActivity.id}", System.currentTimeMillis()).apply()
                        // ✅ Também marcar a atividade original como adiada para evitar notificações tardias do Worker
                        prefs.edit().putLong("activity_snoozed_${activity.id}", System.currentTimeMillis()).apply()
                        Log.d(TAG, "🔔 Atividade marcada como adiada: ${updatedActivity.id} e original: ${activity.id}")
                        
                        Log.d(TAG, "🔔 Atividade não recorrente atualizada no repositório com novo horário")
                    }
                    
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
                    val isRecurringInstance = activityId.contains("_") && activityId.split("_").size >= 2
                    
                    if (isRecurringInstance) {
                        // Tratar instância recorrente específica
                        val parts = activityId.split("_")
                        val baseId = parts.getOrNull(0) ?: ""
                        val instanceDate = parts.getOrNull(1) ?: ""
                        
                        Log.d(TAG, "🔄 Processando instância recorrente via notificação - Base ID: $baseId, Data: $instanceDate")
                        
                        // Buscar a atividade base
                        val activities = repository.activities.first()
                        val baseActivity = activities.find { it.id == baseId }
                        
                        if (baseActivity != null) {
                            
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
                                
                                // Para TODAS as instâncias, apenas adicionar à lista de exclusões (mesma lógica do CalendarViewModel)
                                val updatedBaseActivity = if (baseActivity.recurrenceRule?.startsWith("FREQ=HOURLY") == true) {
                                    // Extrair horário da instância atual
                                    val instanceTime = if (activityId.contains("_")) {
                                        val parts = activityId.split("_")
                                        val timePart = parts.getOrNull(2) // formato: baseId_date_time
                                        if (timePart != null) {
                                            try {
                                                java.time.LocalTime.parse(timePart)
                                            } catch (e: Exception) {
                                                baseActivity.startTime
                                            }
                                        } else {
                                            baseActivity.startTime
                                        }
                                    } else {
                                        baseActivity.startTime
                                    }
                                    
                                    val timeString = instanceTime?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) ?: "00:00"
                                    val instanceId = "${baseActivity.id}_${instanceDate}_${timeString}"
                                    
                                    // Para TODAS as instâncias, apenas adicionar à lista de exclusões
                                    val updatedExcludedInstances = baseActivity.excludedInstances + instanceId
                                    baseActivity.copy(excludedInstances = updatedExcludedInstances)
                                } else {
                                    val updatedExcludedDates = baseActivity.excludedDates + instanceDate
                                    baseActivity.copy(excludedDates = updatedExcludedDates)
                                }
                                
                                // Atualizar a atividade base com a nova lista de exclusões
                                repository.saveActivity(updatedBaseActivity)
                                
                                // ✅ Remover a instância específica do repositório principal se ela existir
                                val specificInstance = activities.find { it.id == activityId }
                                if (specificInstance != null) {
                                    repository.deleteActivity(activityId)
                                    Log.d(TAG, "✅ Instância específica removida do repositório principal: $activityId")
                                }

                                Log.d(TAG, "✅ Instância recorrente marcada como concluída via notificação: ${instanceToComplete.title}")
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
                                
                                // Remover da lista principal (tanto a base quanto a instância específica)
                                repository.deleteActivity(baseId)
                                repository.deleteActivity(activityId)
                                
                                Log.d(TAG, "✅ Atividade única marcada como concluída via notificação: ${completedActivity.title}")
                            }
                        } else {
                            Log.w(TAG, "⚠️ Atividade base não encontrada: $baseId")
                        }
                    } else {
                        // Tratar atividade única ou atividade base
                        val activities = repository.activities.first()
                        
                        Log.d(TAG, "🔍 Buscando atividade única para marcar como concluída - ID: $activityId")
                        Log.d(TAG, "📋 Total de atividades disponíveis: ${activities.size}")
                        Log.d(TAG, "🔍 IDs disponíveis: ${activities.map { it.id }}")
                        
                        // ✅ Verificar se é uma instância específica que foi criada pelo adiamento
                        val isSnoozedInstance = activityId.contains("_") && activityId.split("_").size >= 2
                        
                        if (isSnoozedInstance) {
                            // É uma instância específica criada pelo adiamento - buscar pela instância exata
                            val instanceActivity = activities.find { it.id == activityId }
                            
                            if (instanceActivity != null) {
                                Log.d(TAG, "✅ Marcando instância específica adiada como concluída: ${instanceActivity.title}")
                                
                                // Marcar instância específica como concluída
                                val completedInstance = instanceActivity.copy(
                                    isCompleted = true,
                                    showInCalendar = false
                                )
                                
                                // Salvar no repositório de atividades finalizadas
                                completedRepository.addCompletedActivity(completedInstance)
                                
                                // Remover a instância específica do repositório principal
                                repository.deleteActivity(activityId)
                                
                                Log.d(TAG, "✅ Instância específica adiada marcada como concluída e removida: ${completedInstance.title}")
                            } else {
                                Log.w(TAG, "⚠️ Instância específica não encontrada: $activityId")
                            }
                        } else {
                            // É uma atividade base - buscar normalmente
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
                        val notificationService = NotificationService(context)
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

    /**
     * Calcula a próxima ocorrência para uma atividade recorrente horária
     * (Implementação idêntica à do CalendarViewModel)
     */
    private fun calculateNextHourlyOccurrence(activity: com.mss.thebigcalendar.data.model.Activity, currentDate: LocalDate, currentTime: LocalTime?): Pair<LocalDate, LocalTime?> {
        val recurrenceService = com.mss.thebigcalendar.service.RecurrenceService()
        
        // Gerar instâncias para os próximos 7 dias para encontrar a próxima ocorrência
        val startDate = currentDate.plusDays(1)
        val endDate = currentDate.plusDays(7)
        
        val recurringInstances = recurrenceService.generateRecurringInstances(
            activity, startDate, endDate
        )
        
        // Encontrar a primeira instância que não está excluída
        val nextInstance = recurringInstances.firstOrNull { instance ->
            val instanceDate = LocalDate.parse(instance.date)
            val instanceTime = instance.startTime
            val timeString = instanceTime?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) ?: "00:00"
            val instanceId = "${activity.id}_${instanceDate}_${timeString}"
            
            !activity.excludedInstances.contains(instanceId)
        }
        
        return if (nextInstance != null) {
            Pair(LocalDate.parse(nextInstance.date), nextInstance.startTime)
        } else {
            // Se não encontrar próxima instância, avançar para o próximo dia na mesma hora
            val nextDate = currentDate.plusDays(1)
            Pair(nextDate, currentTime)
        }
    }
}
