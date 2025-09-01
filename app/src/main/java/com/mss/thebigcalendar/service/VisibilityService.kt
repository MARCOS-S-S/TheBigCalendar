package com.mss.thebigcalendar.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.NotificationSettings
import com.mss.thebigcalendar.data.model.VisibilityLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Serviço para gerenciar a visibilidade das atividades baseado no nível configurado
 */
class VisibilityService(private val context: Context) {

    companion object {
        private const val TAG = "VisibilityService"
        private const val VISIBILITY_CHANNEL_ID = "visibility_alerts"
        private const val VISIBILITY_CHANNEL_NAME = "Alertas de Visibilidade"
        private const val VISIBILITY_CHANNEL_DESCRIPTION = "Alertas para atividades com alta visibilidade"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    init {
        createVisibilityNotificationChannel()
    }

    /**
     * Cria o canal de notificação para alertas de visibilidade
     */
    private fun createVisibilityNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                VISIBILITY_CHANNEL_ID,
                VISIBILITY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = VISIBILITY_CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Verifica se o app tem permissão para sobrepor outros apps
     */
    fun hasOverlayPermission(): Boolean {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Para versões anteriores ao Android 6.0
        }
        
        return hasPermission
    }

    /**
     * Solicita permissão para sobrepor outros apps
     */
    fun requestOverlayPermission(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    /**
     * Exibe o alerta baseado no nível de visibilidade da atividade
     */
    fun showVisibilityAlert(activity: Activity) {
        val hasPermission = hasOverlayPermission()
        
        when (activity.visibility) {
            VisibilityLevel.LOW -> {
                // Apenas notificação padrão (já gerenciada pelo NotificationService)
            }
            VisibilityLevel.MEDIUM -> {
                if (hasPermission) {
                    showMediumVisibilityAlert(activity)
                } else {
                    Log.d(TAG, "⚠️ Sem permissão para visibilidade média, usando fallback")
                    // Fallback para notificação se não tiver permissão
                    showFallbackNotification(activity, "Alerta Médio")
                }
            }
            VisibilityLevel.HIGH -> {
                if (hasPermission) {
                    showHighVisibilityAlert(activity)
                } else {
                    Log.d(TAG, "⚠️ Sem permissão para visibilidade alta, usando fallback")
                    // Fallback para notificação se não tiver permissão
                    showFallbackNotification(activity, "Alerta Alto")
                }
            }
        }
    }

    /**
     * Exibe alerta de visibilidade média (banner na parte inferior)
     */
    private fun showMediumVisibilityAlert(activity: Activity) {
        try {
            // ✅ Verificar se estamos na Main thread
            if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
                Log.w(TAG, "⚠️ Não estamos na Main thread, mudando para Main thread")
                // Usar Handler para executar na Main thread
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    showMediumVisibilityAlert(activity)
                }
                return
            }
            
            // Criar layout para o banner
            val layoutInflater = LayoutInflater.from(context)
            val bannerView = layoutInflater.inflate(R.layout.visibility_banner_medium, null)

            // Configurar texto do banner
            val titleText = bannerView.findViewById<TextView>(R.id.banner_title)
            val timeText = bannerView.findViewById<TextView>(R.id.banner_time)
            
            titleText.text = activity.title
            timeText.text = formatActivityTime(activity)

            // Configurar parâmetros da janela
            val layoutParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.BOTTOM
                y = 100 // Margem do fundo
            }

            // Adicionar banner à tela
            windowManager.addView(bannerView, layoutParams)

            // Remover banner após 5 segundos
            bannerView.postDelayed({
                try {
                    windowManager.removeView(bannerView)
                } catch (e: Exception) {
                    Log.w(TAG, "Erro ao remover banner: ${e.message}")
                }
            }, 5000)

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao exibir banner de visibilidade média", e)
            // Fallback para notificação
            showFallbackNotification(activity, "Alerta Médio")
        }
    }

    /**
     * Exibe alerta de visibilidade alta (tela inteira)
     */
    private fun showHighVisibilityAlert(activity: Activity) {
        try {
            // ✅ Verificar se estamos na Main thread
            if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
                Log.w(TAG, "⚠️ Não estamos na Main thread, mudando para Main thread")
                // Usar Handler para executar na Main thread
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    showHighVisibilityAlert(activity)
                }
                return
            }
            
            // Criar layout para o alerta de tela inteira
            val layoutInflater = LayoutInflater.from(context)
            val fullScreenView = layoutInflater.inflate(R.layout.visibility_alert_high, null)

            // Configurar texto do alerta
            val titleText = fullScreenView.findViewById<TextView>(R.id.alert_title)
            val descriptionText = fullScreenView.findViewById<TextView>(R.id.alert_description)
            val timeText = fullScreenView.findViewById<TextView>(R.id.alert_time)
            
            titleText.text = activity.title
            descriptionText.text = activity.description ?: "Sem descrição"
            timeText.text = formatActivityTime(activity)

            // Configurar parâmetros da janela
            val layoutParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                gravity = Gravity.CENTER
                systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                                   View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                   View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }

            // Adicionar alerta à tela
            windowManager.addView(fullScreenView, layoutParams)

            // Configurar botão Adiar
            val snoozeButton = fullScreenView.findViewById<Button>(R.id.alert_snooze_button)
            snoozeButton.setOnClickListener {
                try {
                    showSnoozeOptionsDialog(activity, fullScreenView)
                } catch (e: Exception) {
                    Log.w(TAG, "❌ Erro ao mostrar opções de adiamento: ${e.message}")
                }
            }

            // Configurar botão Concluído
            val completeButton = fullScreenView.findViewById<Button>(R.id.alert_complete_button)
            completeButton.setOnClickListener {
                try {
                    windowManager.removeView(fullScreenView)
                    
                    // Marcar atividade como concluída
                    markActivityAsCompleted(activity)
                    
                } catch (e: Exception) {
                    Log.w(TAG, "❌ Erro ao remover alerta de tela inteira: ${e.message}")
                }
            }


        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao exibir alerta de visibilidade alta", e)
            // Fallback para notificação
            showFallbackNotification(activity, "Alerta Alto")
        }
    }

    /**
     * Notificação de fallback quando não há permissão de sobreposição
     */
    private fun showFallbackNotification(activity: Activity, alertType: String) {
        // Intent para marcar como concluída
        val completeIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationService.ACTION_DISMISS
            putExtra(NotificationService.EXTRA_ACTIVITY_ID, activity.id)
        }
        
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (activity.id + "_complete_fallback").hashCode(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, VISIBILITY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("$alertType: ${activity.title}")
            .setContentText("${activity.description ?: "Sem descrição"} - ${formatActivityTime(activity)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Finalizado",
                completePendingIntent
            )
            .build()

        notificationManager.notify(activity.id.hashCode(), notification)
    }

    /**
     * Formata o horário da atividade para exibição
     */
    private fun formatActivityTime(activity: Activity): String {
        return if (activity.isAllDay) {
            "Dia inteiro"
        } else if (activity.startTime != null) {
            val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR"))
            "Às ${activity.startTime.format(formatter)}"
        } else {
            "Sem horário definido"
        }
    }

    /**
     * Marca uma atividade como concluída
     */
    private fun markActivityAsCompleted(activity: Activity) {
        // Usar CoroutineScope com SupervisorJob para evitar cancelamento
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        
        scope.launch {
            try {
                val repository = com.mss.thebigcalendar.data.repository.ActivityRepository(context)
                val completedRepository = com.mss.thebigcalendar.data.repository.CompletedActivityRepository(context)
                val notificationService = NotificationService(context)
                val recurrenceService = com.mss.thebigcalendar.service.RecurrenceService()
                
                // Verificar se é uma instância recorrente (ID contém data)
                val isRecurringInstance = activity.id.contains("_") && activity.id.split("_").size == 2
                
                if (isRecurringInstance) {
                    // Tratar instância recorrente específica
                    val parts = activity.id.split("_")
                    val baseId = parts[0]
                    val instanceDate = parts[1]
                    
                    Log.d(TAG, "🔄 Processando instância recorrente via overlay - Base ID: $baseId, Data: $instanceDate")
                    
                    // Buscar a atividade base
                    val activities = repository.activities.first()
                    val baseActivity = activities.find { it.id == baseId }
                    
                    if (baseActivity != null && recurrenceService.isRecurring(baseActivity)) {
                        Log.d(TAG, "📋 Atividade base encontrada: ${baseActivity.title}")
                        
                        // Criar instância específica para salvar como concluída
                        val instanceToComplete = baseActivity.copy(
                            id = activity.id,
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
                        
                        Log.d(TAG, "✅ Instância recorrente marcada como concluída via overlay: ${instanceToComplete.title} - Data: $instanceDate")
                        
                    } else {
                        Log.w(TAG, "⚠️ Atividade base não encontrada ou não é recorrente: $baseId")
                    }
                } else {
                    // Tratar atividade única ou atividade base
                    // Verificar se é uma atividade recorrente
                    if (recurrenceService.isRecurring(activity)) {
                        // Para atividades recorrentes (primeira instância), sempre tratar como instância específica
                        val activityDate = activity.date
                        
                        Log.d(TAG, "🔄 Processando primeira instância recorrente via overlay - ID: ${activity.id}, Data: $activityDate")
                        
                        // Criar instância específica para salvar como concluída
                        val instanceToComplete = activity.copy(
                            id = activity.id,
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
                        
                        Log.d(TAG, "✅ Primeira instância recorrente marcada como concluída via overlay: ${instanceToComplete.title} - Data: $activityDate")
                        
                    } else {
                        // Tratar atividade única (não recorrente)
                        Log.d(TAG, "✅ Marcando atividade única como concluída via overlay: ${activity.title}")
                        
                        // Marcar como concluída e salvar no repositório de finalizadas
                        val completedActivity = activity.copy(
                            isCompleted = true,
                            showInCalendar = false
                        )
                        
                        // Salvar no repositório de atividades finalizadas
                        completedRepository.addCompletedActivity(completedActivity)
                        
                        // Remover da lista principal
                        repository.deleteActivity(activity.id)
                        
                        Log.d(TAG, "✅ Atividade única marcada como concluída via overlay: ${completedActivity.title}")
                    }
                }
                
                // Cancelar notificação se existir
                notificationService.cancelNotification(activity.id)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao marcar atividade como concluída via overlay", e)
            }
        }
    }

    /**
     * Exibe o diálogo de opções de adiamento
     */
    private fun showSnoozeOptionsDialog(activity: Activity, currentView: View) {
        try {
            // Inflar o layout do diálogo
            val inflater = LayoutInflater.from(context)
            val dialogView = inflater.inflate(R.layout.snooze_options_dialog, null)
            
            // Configurar parâmetros da janela para o diálogo
            val layoutParams = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                gravity = Gravity.CENTER
            }
            
            // Adicionar diálogo à tela
            windowManager.addView(dialogView, layoutParams)
            
            // Configurar botões de adiamento
            val snooze5minButton = dialogView.findViewById<Button>(R.id.snooze_5min_button)
            val snooze30minButton = dialogView.findViewById<Button>(R.id.snooze_30min_button)
            val snooze1hourButton = dialogView.findViewById<Button>(R.id.snooze_1hour_button)
            val cancelButton = dialogView.findViewById<Button>(R.id.snooze_cancel_button)
            
            // Botão 5 minutos
            snooze5minButton.setOnClickListener {
                snoozeActivity(activity, 5, currentView, dialogView)
            }
            
            // Botão 30 minutos
            snooze30minButton.setOnClickListener {
                snoozeActivity(activity, 30, currentView, dialogView)
            }
            
            // Botão 1 hora
            snooze1hourButton.setOnClickListener {
                snoozeActivity(activity, 60, currentView, dialogView)
            }
            
            // Botão cancelar
            cancelButton.setOnClickListener {
                try {
                    windowManager.removeView(dialogView)
                } catch (e: Exception) {
                    Log.w(TAG, "❌ Erro ao remover diálogo de adiamento: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao exibir diálogo de opções de adiamento", e)
        }
    }

    /**
     * Adia a atividade pelo tempo especificado
     */
    private fun snoozeActivity(activity: Activity, minutes: Int, currentView: View, dialogView: View) {
        try {
            // Remover o diálogo e o alerta atual
            windowManager.removeView(dialogView)
            windowManager.removeView(currentView)
            
            // Agendar nova notificação para o tempo de adiamento
            val notificationService = NotificationService(context)
            notificationService.scheduleSnoozedNotification(activity, minutes)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao adiar atividade", e)
        }
    }
    
    /**
     * Função de teste para verificar se o alerta de tela cheia está funcionando
     */
    fun testHighVisibilityAlert() {
        val testActivity = Activity(
            id = "test",
            title = "TESTE - Alerta de Visibilidade Alta",
            description = "Este é um teste para verificar se o alerta de tela cheia está funcionando",
            date = "2024-01-01",
            startTime = LocalTime.now(),
            endTime = null,
            isAllDay = false,
            location = null,
            categoryColor = "1",
            activityType = ActivityType.TASK,
            recurrenceRule = null,
            notificationSettings = NotificationSettings(),
            isCompleted = false,
            visibility = VisibilityLevel.HIGH,
            showInCalendar = true,
            isFromGoogle = false
        )
        
        showVisibilityAlert(testActivity)
    }
}
