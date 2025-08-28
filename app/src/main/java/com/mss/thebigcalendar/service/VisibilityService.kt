package com.mss.thebigcalendar.service

import android.app.NotificationManager
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
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.VisibilityLevel
import java.time.LocalDateTime
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Para versões anteriores ao Android 6.0
        }
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
        when (activity.visibility) {
            VisibilityLevel.LOW -> {
                // Apenas notificação padrão (já gerenciada pelo NotificationService)
                Log.d(TAG, "Visibilidade baixa: apenas notificação padrão")
            }
            VisibilityLevel.MEDIUM -> {
                if (hasOverlayPermission()) {
                    showMediumVisibilityAlert(activity)
                } else {
                    // Fallback para notificação se não tiver permissão
                    showFallbackNotification(activity, "Alerta Médio")
                }
            }
            VisibilityLevel.HIGH -> {
                if (hasOverlayPermission()) {
                    showHighVisibilityAlert(activity)
                } else {
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

            Log.d(TAG, "Banner de visibilidade média exibido para: ${activity.title}")

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
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                gravity = Gravity.CENTER
            }

            // Adicionar alerta à tela
            windowManager.addView(fullScreenView, layoutParams)

            // Configurar botão de fechar
            val closeButton = fullScreenView.findViewById<View>(R.id.alert_close_button)
            closeButton.setOnClickListener {
                try {
                    windowManager.removeView(fullScreenView)
                } catch (e: Exception) {
                    Log.w(TAG, "Erro ao remover alerta de tela inteira: ${e.message}")
                }
            }

            // Remover alerta automaticamente após 10 segundos
            fullScreenView.postDelayed({
                try {
                    windowManager.removeView(fullScreenView)
                } catch (e: Exception) {
                    Log.w(TAG, "Erro ao remover alerta de tela inteira: ${e.message}")
                }
            }, 10000)

            Log.d(TAG, "Alerta de visibilidade alta exibido para: ${activity.title}")

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao exibir alerta de visibilidade alta", e)
            // Fallback para notificação
            showFallbackNotification(activity, "Alerta Alto")
        }
    }

    /**
     * Notificação de fallback quando não há permissão de sobreposição
     */
    private fun showFallbackNotification(activity: Activity, alertType: String) {
        val notification = NotificationCompat.Builder(context, VISIBILITY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("$alertType: ${activity.title}")
            .setContentText("${activity.description ?: "Sem descrição"} - ${formatActivityTime(activity)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(activity.id.hashCode(), notification)
        Log.d(TAG, "Notificação de fallback exibida para: ${activity.title}")
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
}
