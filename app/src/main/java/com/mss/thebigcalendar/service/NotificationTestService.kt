package com.mss.thebigcalendar.service

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.NotificationSettings
import com.mss.thebigcalendar.data.model.NotificationType
import java.time.LocalDate
import java.time.LocalTime

/**
 * Serviço de teste para verificar se as notificações estão funcionando
 */
class NotificationTestService(private val context: Context) {

    companion object {
        private const val TAG = "NotificationTestService"
        const val TEST_CHANNEL_ID = "test_notifications"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createTestNotificationChannel()
    }

    /**
     * Cria canal de notificação para testes
     */
    private fun createTestNotificationChannel() {
        val channel = android.app.NotificationChannel(
            TEST_CHANNEL_ID,
            "Testes de Notificação",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Canal para testes de notificação"
            enableVibration(true)
            enableLights(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Mostra uma notificação de teste imediatamente
     */
    fun showTestNotification() {
        Log.d(TAG, "Mostrando notificação de teste")
        
        val notification = NotificationCompat.Builder(context, TEST_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🧪 Teste de Notificação")
            .setContentText("Se você vê esta notificação, o sistema está funcionando!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(999, notification)
        Log.d(TAG, "Notificação de teste exibida")
    }

    /**
     * Testa o agendamento de uma notificação
     */
    fun testScheduleNotification() {
        Log.d(TAG, "Testando agendamento de notificação")
        
        // Criar uma atividade de teste para daqui a 1 minuto
        val testActivity = Activity(
            id = "test_${System.currentTimeMillis()}",
            title = "🧪 Atividade de Teste",
            description = "Esta é uma atividade de teste para verificar notificações",
            date = LocalDate.now().toString(),
            startTime = LocalTime.now().plusMinutes(1),
            endTime = LocalTime.now().plusMinutes(2),
            isAllDay = false,
            location = null,
            categoryColor = "#FF0000",
            activityType = com.mss.thebigcalendar.data.model.ActivityType.TASK,
            recurrenceRule = null,
            notificationSettings = NotificationSettings(
                isEnabled = true,
                notificationType = NotificationType.FIVE_MINUTES_BEFORE
            ),
            showInCalendar = true
        )

        // Usar o NotificationService real para agendar
        val notificationService = NotificationService(context)
        notificationService.scheduleNotification(testActivity)
        
        Log.d(TAG, "Notificação de teste agendada para: ${testActivity.startTime}")
    }

    /**
     * Verifica se as permissões estão funcionando
     */
    fun checkPermissions(): Boolean {
        val hasNotificationPermission = context.checkSelfPermission(
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        Log.d(TAG, "Permissão POST_NOTIFICATIONS: $hasNotificationPermission")
        return hasNotificationPermission
    }
}
