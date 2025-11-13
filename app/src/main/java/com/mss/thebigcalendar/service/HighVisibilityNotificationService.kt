package com.mss.thebigcalendar.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mss.thebigcalendar.MainActivity
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.VisibilityLevel
import com.mss.thebigcalendar.ui.screens.HighVisibilityNotificationActivity

/**
 * Serviço para gerenciar notificações de alta visibilidade
 * Mantém o app ativo mesmo com a tela desligada até que o usuário veja a notificação
 */
class HighVisibilityNotificationService : Service() {

    companion object {
        private const val TAG = "HighVisibilityNotificationService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "high_visibility_notifications"
        private const val CHANNEL_NAME = "Notificações de Alta Visibilidade"
        private const val CHANNEL_DESCRIPTION = "Notificações importantes que mantêm o app ativo"
        
        // Ações
        const val ACTION_SHOW_NOTIFICATION = "com.mss.thebigcalendar.SHOW_HIGH_VISIBILITY_NOTIFICATION"
        const val ACTION_DISMISS_NOTIFICATION = "com.mss.thebigcalendar.DISMISS_HIGH_VISIBILITY_NOTIFICATION"
        
        // Extras
        const val EXTRA_ACTIVITY_ID = "activity_id"
        const val EXTRA_ACTIVITY_TITLE = "activity_title"
        const val EXTRA_ACTIVITY_DESCRIPTION = "activity_description"
        const val EXTRA_ACTIVITY_DATE = "activity_date"
        const val EXTRA_ACTIVITY_TIME = "activity_time"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var isNotificationActive = false
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        
        when (intent?.action) {
            ACTION_SHOW_NOTIFICATION -> {
                val activityId = intent.getStringExtra(EXTRA_ACTIVITY_ID)
                val activityTitle = intent.getStringExtra(EXTRA_ACTIVITY_TITLE)
                val activityDescription = intent.getStringExtra(EXTRA_ACTIVITY_DESCRIPTION)
                val activityDate = intent.getStringExtra(EXTRA_ACTIVITY_DATE)
                val activityTime = intent.getStringExtra(EXTRA_ACTIVITY_TIME)
                
                if (activityId != null && activityTitle != null) {
                    Log.d(TAG, "🔔 Criando atividade para notificação de alta visibilidade - ID: $activityId")
                    
                    val activity = Activity(
                        id = activityId, // ✅ Manter o ID original (que pode incluir data e horário para instâncias recorrentes)
                        title = activityTitle,
                        description = activityDescription,
                        date = activityDate ?: "",
                        startTime = activityTime?.let { java.time.LocalTime.parse(it) },
                        endTime = null,
                        isAllDay = false,
                        location = null,
                        categoryColor = "#FF0000",
                        activityType = com.mss.thebigcalendar.data.model.ActivityType.TASK,
                        recurrenceRule = null, // ✅ Será identificado pelo NotificationReceiver baseado no ID
                        notificationSettings = com.mss.thebigcalendar.data.model.NotificationSettings(
                            isEnabled = true,
                            notificationType = com.mss.thebigcalendar.data.model.NotificationType.BEFORE_ACTIVITY
                        ),
                        visibility = VisibilityLevel.HIGH,
                        showInCalendar = true,
                        isCompleted = false,
                        isFromGoogle = false
                    )
                    
                    Log.d(TAG, "🔔 Atividade criada com ID: ${activity.id}")
                    
                    showHighVisibilityNotification(activity)
                }
            }
            ACTION_DISMISS_NOTIFICATION -> {
                dismissNotification()
            }
        }
        
        return START_STICKY // Serviço deve ser reiniciado se for morto
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Exibe notificação de alta visibilidade e mantém o app ativo
     */
    private fun showHighVisibilityNotification(activity: Activity) {
        
        currentActivity = activity
        isNotificationActive = true
        
        // Criar WakeLock para manter o dispositivo ativo
        acquireWakeLock()
        
        // Iniciar a Activity de notificação de alta visibilidade
        startHighVisibilityActivity(activity)
        
        // Criar notificação persistente para manter o serviço ativo
        createPersistentNotification(activity)
        
    }

    /**
     * Inicia a Activity de notificação de alta visibilidade
     */
    private fun startHighVisibilityActivity(activity: Activity) {
        val intent = Intent(this, HighVisibilityNotificationActivity::class.java).apply {
            putExtra(EXTRA_ACTIVITY_ID, activity.id)
            putExtra(EXTRA_ACTIVITY_TITLE, activity.title)
            putExtra(EXTRA_ACTIVITY_DESCRIPTION, activity.description)
            putExtra(EXTRA_ACTIVITY_DATE, activity.date)
            putExtra(EXTRA_ACTIVITY_TIME, activity.startTime?.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        
        startActivity(intent)
    }

    /**
     * Cria notificação persistente para manter o serviço ativo
     */
    private fun createPersistentNotification(activity: Activity) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Intent para abrir o app principal
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent para dismissar a notificação
        val dismissIntent = Intent(this, HighVisibilityNotificationService::class.java).apply {
            action = ACTION_DISMISS_NOTIFICATION
        }
        
        val dismissPendingIntent = PendingIntent.getService(
            this,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.mss.thebigcalendar.R.mipmap.ic_launcher)
            .setContentTitle("🔔 ${activity.title}")
            .setContentText(applicationContext.getString(R.string.high_visibility_notification_active))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true) // Notificação persistente
            .setAutoCancel(false)
            .setContentIntent(mainPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Finalizar",
                dismissPendingIntent
            )
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setLights(0xFF0000FF.toInt(), 1000, 1000)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Adquire WakeLock suave para manter o app ativo sem forçar tela a ligar
     */
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        
        // Usar PARTIAL_WAKE_LOCK sem ACQUIRE_CAUSES_WAKEUP para não forçar tela a ligar
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TheBigCalendar:HighVisibilityNotification"
        )
        
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutos - tempo menor
    }

    /**
     * Libera WakeLock
     */
    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
        wakeLock = null
    }

    /**
     * Dismissa a notificação e para o serviço
     */
    private fun dismissNotification() {
        
        isNotificationActive = false
        currentActivity = null
        
        // Cancelar notificação persistente
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        
        // Liberar WakeLock
        releaseWakeLock()
        
        // Parar o serviço
        stopSelf()
        
        Log.d(TAG, "🔔 Notificação de alta visibilidade dismissada")
    }

    /**
     * Cria o canal de notificação
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🔔 HighVisibilityNotificationService destruído")
        
        // Liberar WakeLock se ainda estiver ativo
        releaseWakeLock()
        
        // Cancelar notificação persistente
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
