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
            "DISMISS_ALARM" -> {
                val alarmId = intent.getStringExtra(AlarmService.EXTRA_ALARM_ID)
                if (alarmId != null) {
                    Log.d(TAG, "🔔 Desligando alarme: $alarmId")
                    dismissAlarm(context, alarmId)
                } else {
                    Log.w(TAG, "🔔 ID do alarme não encontrado para dismiss")
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "🔔 Sistema reiniciado - reagendando alarmes")
                rescheduleAllAlarms(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "🔔 App atualizado - reagendando alarmes")
                rescheduleAllAlarms(context)
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
    
    /**
     * Desliga um alarme
     */
    private fun dismissAlarm(context: Context, alarmId: String) {
        coroutineScope.launch {
            try {
                // Criar instâncias dos serviços
                val alarmRepository = AlarmRepository(context)
                val notificationService = NotificationService(context)
                val alarmService = AlarmService(context, alarmRepository, notificationService)
                
                // Cancelar o alarme
                alarmService.cancelAlarm(alarmId)
                
                // Cancelar notificação
                alarmService.cancelAlarmNotification(alarmId)
                
                Log.d(TAG, "🔔 Alarme desligado com sucesso: $alarmId")
            } catch (e: Exception) {
                Log.e(TAG, "🔔 Erro ao desligar alarme", e)
            }
        }
    }
    
    /**
     * Reagenda todos os alarmes após reinicialização do sistema
     */
    private fun rescheduleAllAlarms(context: Context) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "🔔 Iniciando reagendamento de todos os alarmes")
                
                // Criar instâncias dos serviços
                val alarmRepository = AlarmRepository(context)
                val notificationService = NotificationService(context)
                val alarmService = AlarmService(context, alarmRepository, notificationService)
                
                // Obter todos os alarmes ativos
                val activeAlarms = alarmRepository.getActiveAlarms()
                Log.d(TAG, "🔔 Encontrados ${activeAlarms.size} alarmes ativos para reagendar")
                
                // Reagendar cada alarme
                activeAlarms.forEach { alarmSettings ->
                    try {
                        alarmService.scheduleAlarm(alarmSettings)
                        Log.d(TAG, "🔔 Alarme reagendado: ${alarmSettings.label} às ${alarmSettings.time}")
                    } catch (e: Exception) {
                        Log.e(TAG, "🔔 Erro ao reagendar alarme ${alarmSettings.label}", e)
                    }
                }
                
                Log.d(TAG, "🔔 Reagendamento de alarmes concluído")
            } catch (e: Exception) {
                Log.e(TAG, "🔔 Erro ao reagendar alarmes", e)
            }
        }
    }
}
