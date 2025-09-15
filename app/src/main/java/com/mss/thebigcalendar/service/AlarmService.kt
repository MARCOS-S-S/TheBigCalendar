package com.mss.thebigcalendar.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mss.thebigcalendar.data.model.AlarmSettings
import com.mss.thebigcalendar.data.repository.AlarmRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Serviço para gerenciar alarmes do sistema
 * Integra com AlarmManager do Android e NotificationService
 */
class AlarmService(
    private val context: Context,
    private val alarmRepository: AlarmRepository,
    private val notificationService: NotificationService
) {
    
    companion object {
        private const val TAG = "AlarmService"
        const val ACTION_ALARM_TRIGGERED = "com.mss.thebigcalendar.ALARM_TRIGGERED"
        const val EXTRA_ALARM_ID = "alarm_id"
    }
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    /**
     * Agenda um alarme no sistema
     */
    suspend fun scheduleAlarm(alarmSettings: AlarmSettings): Result<Unit> {
        return try {
            Log.d(TAG, "⏰ Agendando alarme: ${alarmSettings.label} às ${alarmSettings.time}")
            
            if (!alarmSettings.isEnabled) {
                Log.d(TAG, "⏰ Alarme desabilitado, cancelando agendamento")
                cancelAlarm(alarmSettings.id)
                hideAlarmStatusNotification()
                return Result.success(Unit)
            }
            
            if (alarmSettings.repeatDays.isEmpty()) {
                // Alarme único
                scheduleSingleAlarm(alarmSettings)
            } else {
                // Alarme recorrente
                scheduleRepeatingAlarm(alarmSettings)
            }
            
            // O ícone de alarme será gerenciado automaticamente pelo sistema Android
            // quando usamos setAlarmClock()
            Log.d(TAG, "⏰ Alarme configurado - sistema Android gerenciará o ícone na barra de status")
            
            Log.d(TAG, "⏰ Alarme agendado com sucesso")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "⏰ Erro ao agendar alarme", e)
            Result.failure(e)
        }
    }
    
    /**
     * Cancela um alarme agendado
     */
    suspend fun cancelAlarm(alarmId: String) {
        try {
            Log.d(TAG, "❌ Cancelando alarme: $alarmId")
            
            val intent = createAlarmIntent(alarmId)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.hashCode(),
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
            
            // Verificar se ainda há alarmes ativos
            checkAndUpdateAlarmStatus()
            
            Log.d(TAG, "❌ Alarme cancelado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao cancelar alarme", e)
        }
    }
    
    /**
     * Agenda um alarme único (não recorrente)
     */
    private fun scheduleSingleAlarm(alarmSettings: AlarmSettings) {
        val triggerTime = calculateNextTriggerTime(alarmSettings.time)
        
        if (triggerTime <= System.currentTimeMillis()) {
            Log.w(TAG, "⏰ Horário do alarme já passou hoje, agendando para amanhã")
            val tomorrowTime = calculateNextTriggerTime(alarmSettings.time, LocalDate.now().plusDays(1))
            scheduleAlarmAtTime(alarmSettings.id, tomorrowTime)
        } else {
            scheduleAlarmAtTime(alarmSettings.id, triggerTime)
        }
    }
    
    /**
     * Agenda um alarme recorrente
     */
    private fun scheduleRepeatingAlarm(alarmSettings: AlarmSettings) {
        val today = LocalDate.now()
        val todayDayOfWeek = getDayOfWeekString(today)
        
        if (alarmSettings.repeatDays.contains(todayDayOfWeek)) {
            // Se hoje está nos dias de repetição, agendar para hoje
            val triggerTime = calculateNextTriggerTime(alarmSettings.time)
            if (triggerTime > System.currentTimeMillis()) {
                scheduleAlarmAtTime(alarmSettings.id, triggerTime)
            }
        }
        
        // Agendar para os próximos dias da semana
        for (dayOffset in 1..7) {
            val futureDate = today.plusDays(dayOffset.toLong())
            val futureDayOfWeek = getDayOfWeekString(futureDate)
            
            if (alarmSettings.repeatDays.contains(futureDayOfWeek)) {
                val triggerTime = calculateNextTriggerTime(alarmSettings.time, futureDate)
                scheduleAlarmAtTime("${alarmSettings.id}_${futureDate}", triggerTime)
            }
        }
    }
    
    /**
     * Agenda um alarme em um horário específico
     */
    private fun scheduleAlarmAtTime(alarmId: String, triggerTime: Long) {
        val intent = createAlarmIntent(alarmId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            // Usar setAlarmClock para que o sistema Android reconheça como alarme real
            // Isso fará o ícone de alarme aparecer na barra de status
            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                triggerTime,
                pendingIntent
            )
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "⏰ Alarme configurado com setAlarmClock para: ${java.util.Date(triggerTime)}")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Não foi possível usar setAlarmClock, usando fallback: ${e.message}")
            // Fallback para setExactAndAllowWhileIdle
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.d(TAG, "⏰ Alarme agendado com fallback para: ${java.util.Date(triggerTime)}")
        }
    }
    
    /**
     * Cria Intent para o alarme
     */
    private fun createAlarmIntent(alarmId: String): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGERED
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
    }
    
    /**
     * Calcula o próximo horário de disparo
     */
    private fun calculateNextTriggerTime(time: LocalTime, date: LocalDate = LocalDate.now()): Long {
        val dateTime = LocalDateTime.of(date, time)
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    
    /**
     * Converte LocalDate para string do dia da semana
     */
    private fun getDayOfWeekString(date: LocalDate): String {
        return when (date.dayOfWeek) {
            java.time.DayOfWeek.SUNDAY -> "Dom"
            java.time.DayOfWeek.MONDAY -> "Seg"
            java.time.DayOfWeek.TUESDAY -> "Ter"
            java.time.DayOfWeek.WEDNESDAY -> "Qua"
            java.time.DayOfWeek.THURSDAY -> "Qui"
            java.time.DayOfWeek.FRIDAY -> "Sex"
            java.time.DayOfWeek.SATURDAY -> "Sáb"
        }
    }
    
    /**
     * Processa quando um alarme é disparado
     */
    suspend fun handleAlarmTriggered(alarmId: String) {
        try {
            Log.d(TAG, "🔔 Alarme disparado: $alarmId")
            
            val alarmSettings = alarmRepository.getAlarmById(alarmId)
            if (alarmSettings != null) {
                // Abrir AlarmActivity para exibir o alarme completo
                openAlarmActivity(alarmSettings)
                
                // Se for recorrente, reagendar para o próximo dia
                if (alarmSettings.repeatDays.isNotEmpty()) {
                    scheduleRepeatingAlarm(alarmSettings)
                }
                
                Log.d(TAG, "🔔 AlarmActivity aberta")
            } else {
                Log.w(TAG, "🔔 Configurações de alarme não encontradas: $alarmId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔔 Erro ao processar alarme disparado", e)
        }
    }
    
    /**
     * Abre a AlarmActivity para exibir o alarme
     */
    private fun openAlarmActivity(alarmSettings: AlarmSettings) {
        val intent = Intent(context, com.mss.thebigcalendar.ui.screens.AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(com.mss.thebigcalendar.ui.screens.AlarmActivity.EXTRA_ALARM_ID, alarmSettings.id)
        }
        context.startActivity(intent)
    }
    
    /**
     * Mostra notificação de status do alarme na barra de status
     */
    private fun showAlarmStatusNotification(alarmSettings: AlarmSettings) {
        try {
            Log.d(TAG, "🔔 Iniciando showAlarmStatusNotification")
            
            // Verificar permissões de notificação
            val permissionChecker = com.mss.thebigcalendar.service.NotificationPermissionChecker(context)
            if (!permissionChecker.canShowNotifications()) {
                Log.e(TAG, "🔔 Permissões de notificação não concedidas")
                return
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            Log.d(TAG, "🔔 NotificationManager obtido: $notificationManager")
            
            // Criar canal de notificação para alarme
            val channelId = "alarm_status_channel"
            val channel = android.app.NotificationChannel(
                channelId,
                "Status do Alarme",
                android.app.NotificationManager.IMPORTANCE_HIGH // Prioridade alta para aparecer na barra
            ).apply {
                description = "Mostra quando há alarmes ativos"
                setShowBadge(true) // Mostrar badge
                enableLights(true) // Habilitar luz do LED
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true) // Contornar "Não perturbe"
            }
            notificationManager.createNotificationChannel(channel)
            
            // Intent para abrir o app quando tocar na notificação
            val intent = android.content.Intent(context, com.mss.thebigcalendar.MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setContentTitle("⏰ Alarme ativo")
                .setContentText("${alarmSettings.label} às ${alarmSettings.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}")
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // Ícone de alerta mais visível
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Notificação persistente
                .setAutoCancel(false) // Não remove ao tocar
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH) // Prioridade alta para aparecer na barra
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(false) // Não mostrar timestamp
                .setLocalOnly(true) // Apenas local
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL) // Usar padrões do sistema
                .build()
            
            Log.d(TAG, "🔔 Enviando notificação com ID: 9999")
            notificationManager.notify(9999, notification) // ID fixo para status do alarme
            
            Log.d(TAG, "🔔 Notificação de status do alarme exibida - Canal: $channelId, Importância: ${channel.importance}")
            Log.d(TAG, "🔔 Título: ${notification.extras.getString(android.app.Notification.EXTRA_TITLE)}")
            Log.d(TAG, "🔔 Texto: ${notification.extras.getString(android.app.Notification.EXTRA_TEXT)}")
            Log.d(TAG, "🔔 Ícone: ${notification.extras.getInt(android.app.Notification.EXTRA_SMALL_ICON)}")
        } catch (e: Exception) {
            Log.e(TAG, "🔔 Erro ao mostrar notificação de status", e)
        }
    }
    
    /**
     * Esconde a notificação de status do alarme
     */
    private fun hideAlarmStatusNotification() {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(9999) // ID fixo para status do alarme
            Log.d(TAG, "🔔 Notificação de status do alarme removida")
        } catch (e: Exception) {
            Log.e(TAG, "🔔 Erro ao esconder notificação de status", e)
        }
    }
    
    /**
     * Método de teste para verificar se a notificação está funcionando
     */
    fun testAlarmNotification() {
        Log.d(TAG, "🧪 Testando notificação de alarme")
        val testAlarm = AlarmSettings.createDefault("Teste", LocalTime.now().plusMinutes(1))
        showAlarmStatusNotification(testAlarm)
    }
    
    /**
     * Verifica se ainda há alarmes ativos e atualiza o status
     */
    private suspend fun checkAndUpdateAlarmStatus() {
        try {
            val activeAlarms = alarmRepository.getActiveAlarms()
            if (activeAlarms.isEmpty()) {
                hideAlarmStatusNotification()
            } else {
                // Mostrar notificação com o próximo alarme
                val nextAlarm = activeAlarms.minByOrNull { it.time }
                if (nextAlarm != null) {
                    showAlarmStatusNotification(nextAlarm)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔔 Erro ao verificar status dos alarmes", e)
        }
    }
    
    /**
     * Cria uma Activity a partir das configurações do alarme
     */
    private fun createActivityFromAlarm(alarmSettings: AlarmSettings): com.mss.thebigcalendar.data.model.Activity {
        return com.mss.thebigcalendar.data.model.Activity(
            id = alarmSettings.id,
            title = alarmSettings.label,
            description = "Despertador agendado",
            date = LocalDate.now().toString(),
            startTime = alarmSettings.time,
            endTime = alarmSettings.time.plusMinutes(1),
            activityType = com.mss.thebigcalendar.data.model.ActivityType.NOTE, // Usando NOTE já que ALARM não existe
            isAllDay = false,
            location = null,
            categoryColor = "#FF9800", // Cor laranja para alarmes
            recurrenceRule = if (alarmSettings.repeatDays.isNotEmpty()) {
                "FREQ=WEEKLY;BYDAY=${alarmSettings.repeatDays.joinToString(",") { 
                    when (it) {
                        "Dom" -> "SU"
                        "Seg" -> "MO"
                        "Ter" -> "TU"
                        "Qua" -> "WE"
                        "Qui" -> "TH"
                        "Sex" -> "FR"
                        "Sáb" -> "SA"
                        else -> "MO"
                    }
                }}"
            } else null,
            visibility = com.mss.thebigcalendar.data.model.VisibilityLevel.HIGH,
            notificationSettings = com.mss.thebigcalendar.data.model.NotificationSettings(
                notificationType = com.mss.thebigcalendar.data.model.NotificationType.BEFORE_ACTIVITY,
                isEnabled = true
            )
        )
    }
    
}
