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
            
            // 1. Cancelar via AlarmManager com múltiplas estratégias
            val intent = createAlarmIntent(alarmId)
            
            // Tentar cancelar com diferentes flags para garantir que funcione
            val flags = listOf(
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            
            var cancelled = false
            for (flag in flags) {
                try {
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        alarmId.hashCode(),
                        intent,
                        flag
                    )
                    alarmManager.cancel(pendingIntent)
                    cancelled = true
                    Log.d(TAG, "❌ Alarme cancelado com flag: $flag")
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Falha ao cancelar com flag $flag: ${e.message}")
                }
            }
            
            // 2. Cancelar alarmes recorrentes (próximos 7 dias)
            val today = LocalDate.now()
            for (dayOffset in 0..6) {
                val futureDate = today.plusDays(dayOffset.toLong())
                val futureAlarmId = "${alarmId}_${futureDate}"
                
                for (flag in flags) {
                    try {
                        val futureIntent = createAlarmIntent(futureAlarmId)
                        val futurePendingIntent = PendingIntent.getBroadcast(
                            context,
                            futureAlarmId.hashCode(),
                            futureIntent,
                            flag
                        )
                        alarmManager.cancel(futurePendingIntent)
                        Log.d(TAG, "❌ Alarme futuro cancelado: $futureAlarmId")
                    } catch (e: Exception) {
                        // Ignorar erros para alarmes futuros que podem não existir
                    }
                }
            }
            
            // 3. Cancelar via NotificationService
            try {
                notificationService.cancelNotification(alarmId)
                Log.d(TAG, "❌ Notificação cancelada via NotificationService")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Erro ao cancelar notificação: ${e.message}")
            }
            
            // 4. Cancelar todas as notificações relacionadas
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(alarmId.hashCode())
            notificationManager.cancel(alarmId.hashCode() + 1000)
            notificationManager.cancel(alarmId.hashCode() - 1000)
            
            // Cancelar notificações de alarmes recorrentes
            for (dayOffset in 0..6) {
                val futureDate = today.plusDays(dayOffset.toLong())
                val recurringAlarmId = "${alarmId}_${futureDate}"
                notificationManager.cancel(recurringAlarmId.hashCode())
            }
            
            Log.d(TAG, "❌ Todas as notificações relacionadas canceladas")
            
            // 5. Cancelar backup do WorkManager
            cancelWorkManagerBackup(alarmId)
            
            // 6. Limpeza exaustiva de todos os alarmes pendentes
            try {
                val allReceiverClasses = listOf(
                    com.mss.thebigcalendar.service.AlarmReceiver::class.java,
                    com.mss.thebigcalendar.service.NotificationReceiver::class.java
                )
                
                val allActions = listOf(
                    "com.mss.thebigcalendar.ALARM_TRIGGERED",
                    "com.mss.thebigcalendar.VIEW_ACTIVITY",
                    "com.mss.thebigcalendar.SNOOZE",
                    "com.mss.thebigcalendar.DISMISS"
                )
                
                allReceiverClasses.forEach { receiverClass ->
                    allActions.forEach { action ->
                        val cleanupIntent = Intent(context, receiverClass).apply {
                            this.action = action
                            putExtra("alarm_id", alarmId)
                        }
                        
                        val cleanupPendingIntent = PendingIntent.getBroadcast(
                            context,
                            alarmId.hashCode(),
                            cleanupIntent,
                            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                        )
                        alarmManager.cancel(cleanupPendingIntent)
                    }
                }
                
                Log.d(TAG, "❌ Limpeza exaustiva de alarmes pendentes concluída")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Erro na limpeza exaustiva: ${e.message}")
            }
            
            // 7. Verificar se ainda há alarmes ativos
            checkAndUpdateAlarmStatus()
            
            // 8. Forçar atualização do sistema de alarmes para remover do QS
            forceAlarmSystemUpdate()
            
            if (!cancelled) {
                Log.w(TAG, "⚠️ Não foi possível cancelar o alarme com nenhuma flag")
            }
            
            Log.d(TAG, "❌ Alarme cancelado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao cancelar alarme", e)
        }
    }
    
    /**
     * Força atualização do sistema de alarmes para remover do QS
     */
    private fun forceAlarmSystemUpdate() {
        try {
            Log.d(TAG, "🔄 Forçando atualização do sistema de alarmes")
            
            // Método 1: Cancelar um alarme fictício para forçar refresh
            val dummyIntent = Intent(context, com.mss.thebigcalendar.service.AlarmReceiver::class.java).apply {
                action = "DUMMY_ALARM_UPDATE"
            }
            val dummyPendingIntent = PendingIntent.getBroadcast(
                context,
                System.currentTimeMillis().toInt(),
                dummyIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(dummyPendingIntent)
            
            // Método 2: Agendar e cancelar um alarme temporário para forçar refresh
            val tempIntent = Intent(context, com.mss.thebigcalendar.service.AlarmReceiver::class.java).apply {
                action = "TEMP_ALARM_REFRESH"
            }
            val tempPendingIntent = PendingIntent.getBroadcast(
                context,
                (System.currentTimeMillis() + 1000).toInt(),
                tempIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Agendar para 1 segundo no futuro
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000,
                tempPendingIntent
            )
            
            // Cancelar imediatamente
            alarmManager.cancel(tempPendingIntent)
            
            Log.d(TAG, "🔄 Atualização do sistema de alarmes forçada")
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro ao forçar atualização do sistema: ${e.message}")
        }
    }
    
    /**
     * Cancela backup do WorkManager para um alarme
     */
    private fun cancelWorkManagerBackup(alarmId: String) {
        try {
            androidx.work.WorkManager.getInstance(context)
                .cancelUniqueWork("alarm_backup_$alarmId")
            Log.d(TAG, "❌ Backup WorkManager cancelado para alarme: $alarmId")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro ao cancelar backup WorkManager: ${e.message}")
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
     * Agenda um alarme em um horário específico com múltiplas estratégias de backup
     */
    private fun scheduleAlarmAtTime(alarmId: String, triggerTime: Long) {
        val intent = createAlarmIntent(alarmId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        var primaryMethodSuccess = false
        
        try {
            // ESTRATÉGIA 1: setAlarmClock (mais confiável, reconhecido pelo sistema)
            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                triggerTime,
                pendingIntent
            )
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            primaryMethodSuccess = true
            Log.d(TAG, "⏰ Alarme configurado com setAlarmClock para: ${java.util.Date(triggerTime)}")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ setAlarmClock falhou: ${e.message}")
        }
        
        if (!primaryMethodSuccess) {
            try {
                // ESTRATÉGIA 2: setExactAndAllowWhileIdle (funciona com otimizações de bateria)
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                primaryMethodSuccess = true
                Log.d(TAG, "⏰ Alarme agendado com setExactAndAllowWhileIdle para: ${java.util.Date(triggerTime)}")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ setExactAndAllowWhileIdle falhou: ${e.message}")
            }
        }
        
        if (!primaryMethodSuccess) {
            try {
                // ESTRATÉGIA 3: setExact (fallback básico)
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                primaryMethodSuccess = true
                Log.d(TAG, "⏰ Alarme agendado com setExact para: ${java.util.Date(triggerTime)}")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ setExact falhou: ${e.message}")
            }
        }
        
        if (!primaryMethodSuccess) {
            try {
                // ESTRATÉGIA 4: set (último recurso)
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "⏰ Alarme agendado com set (último recurso) para: ${java.util.Date(triggerTime)}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Todas as estratégias de agendamento falharam", e)
            }
        }
        
        // BACKUP: Agendar WorkManager como segurança adicional
        scheduleWorkManagerBackup(alarmId, triggerTime)
    }
    
    /**
     * Agenda backup com WorkManager para garantir que o alarme toque
     */
    private fun scheduleWorkManagerBackup(alarmId: String, triggerTime: Long) {
        try {
            // Usar WorkManager como backup para garantir confiabilidade
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<AlarmBackupWorker>()
                .setInitialDelay(java.time.Duration.ofMillis(triggerTime - System.currentTimeMillis()))
                .setInputData(androidx.work.Data.Builder()
                    .putString("alarm_id", alarmId)
                    .putLong("trigger_time", triggerTime)
                    .build())
                .setConstraints(androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false)
                    .setRequiresStorageNotLow(false)
                    .build())
                .build()
            
            androidx.work.WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "alarm_backup_$alarmId",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            
            Log.d(TAG, "⏰ Backup WorkManager agendado para alarme: $alarmId")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Falha ao agendar backup WorkManager: ${e.message}")
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
                // Forçar abertura da tela do alarme
                forceOpenAlarmScreen(alarmSettings)
                
                // Reagendar o alarme baseado no tipo:
                if (alarmSettings.repeatDays.isNotEmpty()) {
                    // Alarme recorrente - reagendar para os próximos dias
                    Log.d(TAG, "🔔 Reagendando alarme recorrente: ${alarmSettings.label}")
                    scheduleRepeatingAlarm(alarmSettings)
                } else {
                    // Alarme único - desativar após tocar
                    Log.d(TAG, "🔔 Desativando alarme único após tocar: ${alarmSettings.label}")
                    val updatedAlarm = alarmSettings.copy(
                        isEnabled = false,
                        lastModified = System.currentTimeMillis()
                    )
                    alarmRepository.saveAlarm(updatedAlarm)
                    Log.d(TAG, "🔔 Alarme único desativado: ${alarmSettings.label}")
                }
            } else {
                Log.w(TAG, "🔔 Configurações de alarme não encontradas: $alarmId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔔 Erro ao processar alarme disparado", e)
        }
    }
    
    /**
     * Força a abertura da tela do alarme
     */
    private fun forceOpenAlarmScreen(alarmSettings: AlarmSettings) {
        try {
            Log.d(TAG, "🔔 Forçando abertura da tela do alarme: ${alarmSettings.label}")
            
            // Criar notificação com full screen intent
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            // Canal de notificação para alarmes
            val channelId = "alarm_fullscreen_channel"
            val channel = android.app.NotificationChannel(
                channelId,
                "Alarmes Tela Cheia",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de alarme em tela cheia"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
            
            // Intent para abrir AlarmActivity
            val intent = Intent(context, com.mss.thebigcalendar.ui.screens.AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                putExtra(com.mss.thebigcalendar.ui.screens.AlarmActivity.EXTRA_ALARM_ID, alarmSettings.id)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                alarmSettings.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Notificação com full screen intent
            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setContentTitle("⏰ Despertador")
                .setContentText("${alarmSettings.label} - ${alarmSettings.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true) // Força tela cheia
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setOngoing(true)
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
                .build()
            
            // Mostrar notificação
            notificationManager.notify(alarmSettings.id.hashCode(), notification)
            
            // Tentar abrir a activity diretamente também
            try {
                context.startActivity(intent)
                Log.d(TAG, "🔔 AlarmActivity iniciada diretamente")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Falha ao abrir AlarmActivity diretamente: ${e.message}")
            }
            
            // Tocar som de alarme
            playAlarmSound(alarmSettings)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao forçar abertura da tela do alarme", e)
        }
    }
    
    /**
     * Mostra notificação de alarme com som e vibração
     */
    private fun showAlarmNotification(alarmSettings: AlarmSettings) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            // Canal de notificação para alarmes
            val channelId = "alarm_notification_channel"
            val channel = android.app.NotificationChannel(
                channelId,
                "Alarmes",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de alarme"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true) // Contornar "Não perturbe"
                // Configurar som de alarme
                setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
            
            // Intent para abrir o app quando tocar na notificação
            val intent = android.content.Intent(context, com.mss.thebigcalendar.MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                alarmSettings.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Intent para dismiss do alarme
            val dismissIntent = android.content.Intent(context, AlarmReceiver::class.java).apply {
                action = "DISMISS_ALARM"
                putExtra(EXTRA_ALARM_ID, alarmSettings.id)
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                alarmSettings.id.hashCode() + 1000,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Notificação de alarme com ações
            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setContentTitle("⏰ Despertador")
                .setContentText("${alarmSettings.label} - ${alarmSettings.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(pendingIntent)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false) // Não remove automaticamente
                .setOngoing(true) // Persistente
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL) // Som e vibração
                .setFullScreenIntent(pendingIntent, true) // Tela cheia se possível
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Desligar",
                    dismissPendingIntent
                )
                .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000)) // Padrão de vibração de alarme
                .build()
            
            notificationManager.notify(alarmSettings.id.hashCode(), notification)
            Log.d(TAG, "🔔 Notificação de alarme exibida: ${alarmSettings.label}")
            
            // Tocar som de alarme adicional
            playAlarmSound(alarmSettings)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao mostrar notificação de alarme", e)
        }
    }
    
    /**
     * Toca som de alarme diretamente
     */
    private fun playAlarmSound(alarmSettings: AlarmSettings) {
        try {
            val mediaPlayer = android.media.MediaPlayer()
            val alarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            
            if (alarmUri != null) {
                // Obter o volume de alarme do sistema
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
                val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM)
                val volumeLevel = currentVolume.toFloat() / maxVolume.toFloat()
                
                Log.d(TAG, "🔔 Volume do alarme (Service): $currentVolume/$maxVolume (${(volumeLevel * 100).toInt()}%)")
                
                mediaPlayer.setDataSource(context, alarmUri)
                mediaPlayer.setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                mediaPlayer.isLooping = true
                mediaPlayer.setVolume(volumeLevel, volumeLevel)
                mediaPlayer.prepare()
                mediaPlayer.start()
                
                // Parar após 30 segundos se não for interrompido
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.stop()
                        mediaPlayer.release()
                    }
                }, 30000)
                
                Log.d(TAG, "🔊 Som de alarme tocando: ${alarmSettings.label}")
            } else {
                Log.w(TAG, "⚠️ URI de alarme não encontrado, usando som padrão")
                // Fallback para som padrão
                val ringtone = android.media.RingtoneManager.getRingtone(context, android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE))
                ringtone?.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao tocar som de alarme", e)
        }
    }
    
    /**
     * Cancela a notificação de alarme
     */
    fun cancelAlarmNotification(alarmId: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(alarmId.hashCode())
            Log.d(TAG, "🔔 Notificação de alarme cancelada: $alarmId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao cancelar notificação de alarme", e)
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
