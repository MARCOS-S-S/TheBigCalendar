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
        
        // Verificar permissão POST_NOTIFICATIONS (Android 13+)
        val hasPostNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            permission == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        // Verificar permissão SCHEDULE_EXACT_ALARM
        val hasScheduleExactAlarmPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SCHEDULE_EXACT_ALARM
        ) == PackageManager.PERMISSION_GRANTED
        

        // Verificar permissão USE_EXACT_ALARM
        val hasUseExactAlarmPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.USE_EXACT_ALARM
        ) == PackageManager.PERMISSION_GRANTED
        

        // Verificar se o app está ignorando otimizações de bateria
        val isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
        
        
        val allPermissionsGranted = hasPostNotificationPermission && 
                                   (hasScheduleExactAlarmPermission || hasUseExactAlarmPermission)
        
        
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
        
        
        return areNotificationsEnabled
    }
}
