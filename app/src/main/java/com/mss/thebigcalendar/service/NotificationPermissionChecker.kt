package com.mss.thebigcalendar.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Verificador de permissões de notificação
 */
class NotificationPermissionChecker(private val context: Context) {

    companion object {
        private const val TAG = "NotificationPermissionChecker"
    }

    /**
     * Verifica se as permissões de notificação estão concedidas
     */
    fun checkNotificationPermissions(): Boolean {
        Log.d(TAG, "🔔 Verificando permissões de notificação")
        
        // Verificar permissão POST_NOTIFICATIONS (Android 13+)
        val hasPostNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            Log.d(TAG, "🔔 Permissão POST_NOTIFICATIONS: ${permission == PackageManager.PERMISSION_GRANTED}")
            permission == PackageManager.PERMISSION_GRANTED
        } else {
            Log.d(TAG, "🔔 Versão Android < 13, permissão POST_NOTIFICATIONS não necessária")
            true
        }

        // Verificar permissão SCHEDULE_EXACT_ALARM
        val hasScheduleExactAlarmPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SCHEDULE_EXACT_ALARM
        ) == PackageManager.PERMISSION_GRANTED
        
        Log.d(TAG, "🔔 Permissão SCHEDULE_EXACT_ALARM: $hasScheduleExactAlarmPermission")

        // Verificar permissão USE_EXACT_ALARM
        val hasUseExactAlarmPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.USE_EXACT_ALARM
        ) == PackageManager.PERMISSION_GRANTED
        
        Log.d(TAG, "🔔 Permissão USE_EXACT_ALARM: $hasUseExactAlarmPermission")

        val allPermissionsGranted = hasPostNotificationPermission && 
                                   (hasScheduleExactAlarmPermission || hasUseExactAlarmPermission)
        
        Log.d(TAG, "🔔 Todas as permissões concedidas: $allPermissionsGranted")
        
        return allPermissionsGranted
    }

    /**
     * Verifica se o app tem permissão para mostrar notificações
     */
    fun canShowNotifications(): Boolean {
        val hasPermission = checkNotificationPermissions()
        
        if (!hasPermission) {
            Log.w(TAG, "🔔 Permissões de notificação não concedidas!")
            return false
        }

        // Verificar se as notificações estão habilitadas no sistema
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        val areNotificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationManager.areNotificationsEnabled()
        } else {
            true // Para versões anteriores, assumir que estão habilitadas
        }
        
        Log.d(TAG, "🔔 Notificações habilitadas no sistema: $areNotificationsEnabled")
        
        return areNotificationsEnabled
    }
}
