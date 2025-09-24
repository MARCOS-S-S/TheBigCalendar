package com.mss.thebigcalendar.service

import android.content.Context
import android.util.Log
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.NotificationSettings
import com.mss.thebigcalendar.data.model.NotificationType
import com.mss.thebigcalendar.data.model.VisibilityLevel
import java.time.LocalDate
import java.time.LocalTime

/**
 * Serviço para testar notificações
 */
class NotificationTestService(private val context: Context) {

    companion object {
        private const val TAG = "NotificationTestService"
    }

    /**
     * Testa notificação imediata
     */
    fun testImmediateNotification() {
        
        val testActivity = Activity(
            id = "test_immediate",
            title = "Teste Imediato",
            description = "Esta é uma notificação de teste imediata",
            date = LocalDate.now().toString(),
            startTime = LocalTime.now().plusMinutes(1),
            endTime = LocalTime.now().plusMinutes(2),
            isAllDay = false,
            location = null,
            categoryColor = "#FF5722",
            activityType = ActivityType.TASK,
            recurrenceRule = null,
            notificationSettings = NotificationSettings(
                isEnabled = true,
                notificationType = NotificationType.BEFORE_ACTIVITY
            ),
            isCompleted = false,
            visibility = VisibilityLevel.LOW,
            showInCalendar = true,
            isFromGoogle = false,
            excludedDates = emptyList()
        )

        val notificationService = NotificationService(context)
        notificationService.showNotification(testActivity)
        
    }

    /**
     * Testa agendamento de notificação
     */
    fun testScheduleNotification() {
        
        // Criar atividade para daqui a 1 minuto
        val tomorrow = LocalDate.now().plusDays(1)
        val testTime = LocalTime.now().plusMinutes(1)
        
        val testActivity = Activity(
            id = "test_scheduled",
            title = "Teste Agendado",
            description = "Esta é uma notificação de teste agendada",
            date = tomorrow.toString(),
            startTime = testTime,
            endTime = testTime.plusMinutes(1),
            isAllDay = false,
            location = null,
            categoryColor = "#FF5722",
            activityType = ActivityType.TASK,
            recurrenceRule = null,
            notificationSettings = NotificationSettings(
                isEnabled = true,
                notificationType = NotificationType.BEFORE_ACTIVITY
            ),
            isCompleted = false,
            visibility = VisibilityLevel.LOW,
            showInCalendar = true,
            isFromGoogle = false,
            excludedDates = emptyList()
        )

        val notificationService = NotificationService(context)
        notificationService.scheduleNotification(testActivity)
        
    }

    /**
     * Testa notificação simples
     */
    fun testSimpleNotification() {
        
        val simpleTest = SimpleNotificationTest(context)
        simpleTest.showSimpleTestNotification()
        
    }
    
    /**
     * Verifica permissões de notificação
     */
    fun checkPermissions() {
        
        val permissionChecker = NotificationPermissionChecker(context)
        val canShow = permissionChecker.canShowNotifications()
        
        
        if (!canShow) {
            Log.w(TAG, "🧪 ATENÇÃO: Permissões de notificação não concedidas!")
            Log.w(TAG, "🧪 Vá em Configurações > Apps > TheBigCalendar > Notificações")
        }
    }
}
