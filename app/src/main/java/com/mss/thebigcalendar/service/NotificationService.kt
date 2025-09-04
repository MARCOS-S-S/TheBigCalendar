package com.mss.thebigcalendar.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.NotificationType
import com.mss.thebigcalendar.data.model.VisibilityLevel


import java.time.LocalDateTime
import java.time.ZoneId

class NotificationService(
    private val context: Context
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "NotificationService"
        const val CHANNEL_ID = "calendar_notifications"
        const val CHANNEL_NAME = "Lembretes do Calendário"
        const val CHANNEL_DESCRIPTION = "Notificações para atividades e tarefas do calendário"
        
        // Ações para as notificações
        const val ACTION_VIEW_ACTIVITY = "com.mss.thebigcalendar.VIEW_ACTIVITY"
        const val ACTION_SNOOZE = "com.mss.thebigcalendar.SNOOZE"
        const val ACTION_DISMISS = "com.mss.thebigcalendar.DISMISS"
        
        // Extras para as notificações
        const val EXTRA_ACTIVITY_ID = "activity_id"
        const val EXTRA_ACTIVITY_TITLE = "activity_title"
        const val EXTRA_ACTIVITY_DATE = "activity_date"
        const val EXTRA_ACTIVITY_TIME = "activity_time"
    }

    init {
        createNotificationChannel()
    }

    /**
     * Cria o canal de notificação (necessário para Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, 
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                setBypassDnd(true) // Ignorar "Não perturbe"
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "🔔 Canal de notificação criado com som: ${android.provider.Settings.System.DEFAULT_NOTIFICATION_URI}")
        }
    }

    /**
     * Agenda uma notificação para uma atividade
     */
    fun scheduleNotification(activity: Activity) {
        if (!activity.notificationSettings.isEnabled || 
            activity.notificationSettings.notificationType == com.mss.thebigcalendar.data.model.NotificationType.NONE) {
            return
        }
        
        // Se não há horário específico, usar início do dia (00:00)
        if (activity.startTime == null) {
        }

        val notificationTime = calculateNotificationTime(activity)
        val triggerTime = getTriggerTime(activity.date, notificationTime)
        

        
        // Cancelar notificação anterior se existir
        cancelNotification(activity.id)
        
        // ✅ Criar intent para exibir a notificação visual
        // Para atividades recorrentes, usar o ID da instância específica
        val activityIdForNotification = if (activity.id.contains("_")) {
            // Já é uma instância específica
            activity.id
        } else {
            // É uma atividade base, criar ID da instância específica
            "${activity.id}_${activity.date}"
        }
        
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_VIEW_ACTIVITY
            putExtra(EXTRA_ACTIVITY_ID, activityIdForNotification)
            putExtra(EXTRA_ACTIVITY_TITLE, activity.title)
            putExtra(EXTRA_ACTIVITY_DATE, activity.date)
            putExtra(EXTRA_ACTIVITY_TIME, activity.startTime?.toString() ?: "")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            activityIdForNotification.hashCode(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        
        // Verificar se o timestamp é no futuro
        if (triggerTime <= System.currentTimeMillis()) {
            return
        }
        
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )

    }

    /**
     * Cancela uma notificação agendada e a notificação atual
     */
    fun cancelNotification(activityId: String) {
        Log.d(TAG, "🔔 Cancelando notificação para atividade: $activityId")
        Log.d(TAG, "🔔 CANCELAMENTO DE NOTIFICAÇÃO INICIADO!")
        
        // Cancelar o alarme agendado
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            activityId.hashCode(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        
        // Tentar cancelar também o alarme base (para casos de instâncias recorrentes)
        if (activityId.contains("_")) {
            val baseId = activityId.split("_")[0]
            val baseIntent = Intent(context, NotificationReceiver::class.java)
            val basePendingIntent = PendingIntent.getBroadcast(
                context,
                baseId.hashCode(),
                baseIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(basePendingIntent)
            Log.d(TAG, "🔔 Alarmes base e instância cancelados")
        } else {
            Log.d(TAG, "🔔 Alarme cancelado")
        }
        
        // Cancelar a notificação atual (se estiver sendo exibida)
        val notificationId = activityId.hashCode()
        Log.d(TAG, "🔔 Cancelando notificação com ID: $notificationId")
        notificationManager.cancel(notificationId)
        
        // Tentar cancelar também com IDs alternativos (para casos de instâncias recorrentes)
        val baseId = if (activityId.contains("_")) {
            activityId.split("_")[0]
        } else {
            activityId
        }
        val baseNotificationId = baseId.hashCode()
        if (baseNotificationId != notificationId) {
            Log.d(TAG, "🔔 Cancelando também notificação base com ID: $baseNotificationId")
            notificationManager.cancel(baseNotificationId)
        }
        
        Log.d(TAG, "🔔 Notificação e alarme cancelados com sucesso")
    }

    /**
     * Calcula o horário da notificação baseado nas configurações
     */
    private fun calculateNotificationTime(activity: Activity): LocalDateTime {
        val notificationType = activity.notificationSettings.notificationType
        
        // Se há um horário específico de notificação configurado, usar ele
        if (activity.notificationSettings.notificationTime != null) {
            return LocalDateTime.parse("${activity.date}T${activity.notificationSettings.notificationTime}")
        }
        
        // Se não há horário específico de notificação, calcular baseado no tipo
        val activityDateTime = if (activity.startTime != null) {
            LocalDateTime.parse("${activity.date}T${activity.startTime}")
        } else {
            LocalDateTime.parse("${activity.date}T00:00")
        }
        
        return when (notificationType) {
            com.mss.thebigcalendar.data.model.NotificationType.NONE -> activityDateTime
            com.mss.thebigcalendar.data.model.NotificationType.BEFORE_ACTIVITY -> {
                // Para BEFORE_ACTIVITY, usar o horário exato da atividade (0 minutos antes)
                activityDateTime
            }
            com.mss.thebigcalendar.data.model.NotificationType.CUSTOM -> {
                val customMinutes = activity.notificationSettings.customMinutesBefore ?: 15
                activityDateTime.minusMinutes(customMinutes.toLong())
            }
            else -> {
                val minutes = notificationType.minutesBefore ?: 15
                activityDateTime.minusMinutes(minutes.toLong())
            }
        }
    }

    /**
     * Converte LocalDateTime para timestamp do sistema
     */
    private fun getTriggerTime(date: String, notificationTime: LocalDateTime): Long {
        return notificationTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Mostra uma notificação imediatamente (para testes)
     */
    fun showNotification(activity: Activity) {
        Log.d(TAG, "🔔 showNotification chamado para: ${activity.title}")
        
        // Verificar permissões primeiro
        val permissionChecker = NotificationPermissionChecker(context)
        if (!permissionChecker.canShowNotifications()) {
            Log.e(TAG, "🔔 Não é possível mostrar notificações - permissões não concedidas")
            return
        }
        
        // Verificar se precisa exibir alerta de visibilidade
        if (activity.visibility != VisibilityLevel.LOW) {
            Log.d(TAG, "🔔 Usando VisibilityService para visibilidade: ${activity.visibility}")
            // Usar VisibilityService para alertas especiais
            val visibilityService = VisibilityService(context)
            visibilityService.showVisibilityAlert(activity)
            return
        }
        
        // ✅ Intent para abrir a MainActivity quando tocar na notificação
        val mainIntent = Intent(context, com.mss.thebigcalendar.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("selected_activity_id", activity.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            activity.id.hashCode(),
            mainIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = getNotificationSound()
        Log.d(TAG, "🔔 Som da notificação: $soundUri")
        
        val snoozePendingIntent = createSnoozePendingIntent(activity, 5)
        val dismissPendingIntent = createDismissPendingIntent(activity)
        
        Log.d(TAG, "🔔 Criando PendingIntents - Snooze: ${snoozePendingIntent != null}, Dismiss: ${dismissPendingIntent != null}")
        Log.d(TAG, "🔔 Snooze PendingIntent ID: ${(activity.id + "_snooze").hashCode()}")
        Log.d(TAG, "🔔 Dismiss PendingIntent ID: ${(activity.id + "_dismiss").hashCode()}")
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 Lembrete: ${activity.title}")
            .setContentText(getNotificationText(activity))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(soundUri)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Finalizado",
                dismissPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_revert,
                "Adiar 5 min",
                snoozePendingIntent
            )
            .setVibrate(longArrayOf(0, 500, 200, 500)) // ✅ Adicionar vibração
            .setLights(0xFF0000FF.toInt(), 1000, 1000) // ✅ Adicionar luz LED
            .build()

        Log.d(TAG, "🔔 Enviando notificação com ID: ${activity.id.hashCode()}")
        notificationManager.notify(activity.id.hashCode(), notification)
        Log.d(TAG, "🔔 Notificação enviada com sucesso!")
    }

    /**
     * Gera o texto da notificação
     */
    private fun getNotificationText(activity: Activity): String {
        val timeText = if (activity.startTime != null) {
            " às ${String.format("%02d:%02d", activity.startTime.hour, activity.startTime.minute)}"
        } else {
            ""
        }
        
        return "Atividade programada para hoje$timeText"
    }

    /**
     * Obtém o som padrão de notificação do sistema
     */
    private fun getNotificationSound(): android.net.Uri? {
        return android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
    }

    /**
     * Cria intent para adiar notificação
     */
    private fun createSnoozePendingIntent(activity: Activity, minutes: Int): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_ACTIVITY_ID, activity.id)
            putExtra("snooze_minutes", minutes)
        }
        
        Log.d(TAG, "🔔 Criando Snooze PendingIntent para atividade: ${activity.title}, ID: ${activity.id}")
        
        return PendingIntent.getBroadcast(
            context,
            (activity.id + "_snooze").hashCode(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Cria intent para cancelar notificação
     */
    private fun createDismissPendingIntent(activity: Activity): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_ACTIVITY_ID, activity.id)
        }
        
        Log.d(TAG, "🔔 Criando Dismiss PendingIntent para atividade: ${activity.title}, ID: ${activity.id}")
        
        return PendingIntent.getBroadcast(
            context,
            (activity.id + "_dismiss").hashCode(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Agenda uma notificação adiada para a atividade
     */
    fun scheduleSnoozedNotification(activity: Activity, minutes: Int) {
        try {
            // Calcular o horário de execução
            val now = java.time.LocalDateTime.now()
            val executionTime = now.plusMinutes(minutes.toLong())
            
            // Criar intent para a notificação adiada
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_VIEW_ACTIVITY
                putExtra(EXTRA_ACTIVITY_ID, activity.id)
                putExtra(EXTRA_ACTIVITY_TITLE, activity.title)
                putExtra(EXTRA_ACTIVITY_DATE, activity.date)
                putExtra(EXTRA_ACTIVITY_TIME, activity.startTime?.toString() ?: "")
            }
            
            // Criar PendingIntent único para o adiamento
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (activity.id + "_snooze_${minutes}min").hashCode(),
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Agendar o alarme
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    executionTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    executionTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    pendingIntent
                )
            }
            
        } catch (e: Exception) {
            // Erro ao agendar notificação adiada
        }
    }
}
