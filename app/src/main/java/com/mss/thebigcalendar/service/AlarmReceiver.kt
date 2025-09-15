package com.mss.thebigcalendar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mss.thebigcalendar.data.repository.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver para processar alarmes disparados
 */
class AlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmReceiver"
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔔 AlarmReceiver.onReceive chamado")
        Log.d(TAG, "🔔 Action: ${intent.action}")
        Log.d(TAG, "🔔 Extras: ${intent.extras?.keySet()?.joinToString()}")
        
        when (intent.action) {
            AlarmService.ACTION_ALARM_TRIGGERED -> {
                val alarmId = intent.getStringExtra(AlarmService.EXTRA_ALARM_ID)
                if (alarmId != null) {
                    Log.d(TAG, "🔔 Processando alarme disparado: $alarmId")
                    handleAlarmTriggered(context, alarmId)
                } else {
                    Log.w(TAG, "🔔 ID do alarme não encontrado no Intent")
                }
            }
            else -> {
                Log.w(TAG, "🔔 Ação desconhecida: ${intent.action}")
            }
        }
    }
    
    /**
     * Processa um alarme disparado
     */
    private fun handleAlarmTriggered(context: Context, alarmId: String) {
        coroutineScope.launch {
            try {
                // Criar instâncias dos serviços
                val alarmRepository = AlarmRepository(context)
                val notificationService = NotificationService(context)
                val alarmService = AlarmService(context, alarmRepository, notificationService)
                
                // Processar o alarme
                alarmService.handleAlarmTriggered(alarmId)
                
                Log.d(TAG, "🔔 Alarme processado com sucesso: $alarmId")
            } catch (e: Exception) {
                Log.e(TAG, "🔔 Erro ao processar alarme: $alarmId", e)
            }
        }
    }
}
