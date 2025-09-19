package com.mss.thebigcalendar.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.mss.thebigcalendar.data.repository.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Serviço para verificação periódica de alarmes
 * Garante que alarmes não sejam perdidos devido a otimizações do sistema
 */
class AlarmVerificationService : Service() {

    companion object {
        private const val TAG = "AlarmVerificationService"
        private const val VERIFICATION_INTERVAL = 30 * 60 * 1000L // 30 minutos
        private const val NOTIFICATION_ID = 1002
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔍 AlarmVerificationService criado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🔍 AlarmVerificationService iniciado")
        
        if (!isRunning) {
            isRunning = true
            startVerificationLoop()
        }
        
        return START_STICKY // Serviço deve ser reiniciado se for morto
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Inicia o loop de verificação de alarmes
     */
    private fun startVerificationLoop() {
        coroutineScope.launch {
            while (isRunning) {
                try {
                    Log.d(TAG, "🔍 Verificando alarmes ativos...")
                    verifyActiveAlarms()
                    
                    // Aguardar antes da próxima verificação
                    delay(VERIFICATION_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro no loop de verificação", e)
                    delay(60 * 1000L) // Aguardar 1 minuto em caso de erro
                }
            }
        }
    }

    /**
     * Verifica se todos os alarmes ativos estão devidamente agendados
     */
    private suspend fun verifyActiveAlarms() {
        try {
            val alarmRepository = AlarmRepository(this)
            val notificationService = NotificationService(this)
            val alarmService = AlarmService(this, alarmRepository, notificationService)
            
            val activeAlarms = alarmRepository.getActiveAlarms()
            Log.d(TAG, "🔍 Verificando ${activeAlarms.size} alarmes ativos")
            
            activeAlarms.forEach { alarmSettings ->
                try {
                    // Verificar se o alarme está agendado no sistema
                    val isScheduled = isAlarmScheduled(alarmSettings.id)
                    val isScheduledForToday = isAlarmScheduledForToday(alarmSettings)
                    
                    if (!isScheduled || !isScheduledForToday) {
                        Log.w(TAG, "⚠️ Alarme ${alarmSettings.label} não está agendado corretamente, reagendando...")
                        alarmService.scheduleAlarm(alarmSettings)
                        
                        // Garantir que as configurações sejam persistidas
                        val saveResult = alarmRepository.saveAlarm(alarmSettings)
                        if (saveResult.isSuccess) {
                            Log.d(TAG, "✅ Alarme ${alarmSettings.label} reagendado e persistido com sucesso")
                        } else {
                            Log.w(TAG, "⚠️ Alarme ${alarmSettings.label} reagendado mas falha ao persistir")
                        }
                    } else {
                        Log.d(TAG, "✅ Alarme ${alarmSettings.label} está agendado corretamente")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao verificar alarme ${alarmSettings.label}", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao verificar alarmes ativos", e)
        }
    }

    /**
     * Verifica se um alarme específico está agendado no sistema
     */
    private fun isAlarmScheduled(alarmId: String): Boolean {
        return try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, AlarmReceiver::class.java).apply {
                action = AlarmService.ACTION_ALARM_TRIGGERED
                putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                alarmId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            
            pendingIntent != null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao verificar se alarme está agendado", e)
            false
        }
    }
    
    /**
     * Verifica se um alarme está agendado para hoje (considerando alarmes recorrentes)
     */
    private fun isAlarmScheduledForToday(alarmSettings: com.mss.thebigcalendar.data.model.AlarmSettings): Boolean {
        return try {
            val today = LocalDate.now()
            val todayDayOfWeek = when (today.dayOfWeek) {
                java.time.DayOfWeek.SUNDAY -> "Dom"
                java.time.DayOfWeek.MONDAY -> "Seg"
                java.time.DayOfWeek.TUESDAY -> "Ter"
                java.time.DayOfWeek.WEDNESDAY -> "Qua"
                java.time.DayOfWeek.THURSDAY -> "Qui"
                java.time.DayOfWeek.FRIDAY -> "Sex"
                java.time.DayOfWeek.SATURDAY -> "Sáb"
            }
            
            // Para alarmes recorrentes, verificar se hoje está nos dias de repetição
            if (alarmSettings.repeatDays.isNotEmpty()) {
                return alarmSettings.repeatDays.contains(todayDayOfWeek)
            }
            
            // Para alarmes únicos, sempre considerar que devem estar agendados
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao verificar se alarme está agendado para hoje", e)
            true // Em caso de erro, assumir que está agendado
        }
    }

    /**
     * Agenda verificação periódica usando AlarmManager
     */
    fun schedulePeriodicVerification() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, AlarmVerificationService::class.java)
            val pendingIntent = PendingIntent.getService(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Agendar verificação a cada 30 minutos
            val triggerTime = System.currentTimeMillis() + VERIFICATION_INTERVAL
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            
            Log.d(TAG, "🔍 Verificação periódica agendada")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao agendar verificação periódica", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🔍 AlarmVerificationService destruído")
        isRunning = false
    }
}
