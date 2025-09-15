package com.mss.thebigcalendar.ui.screens

import android.app.KeyguardManager
import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.PowerManager
import android.provider.CalendarContract
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.mss.thebigcalendar.data.model.AlarmSettings
import com.mss.thebigcalendar.data.repository.AlarmRepository
import com.mss.thebigcalendar.service.AlarmService
import com.mss.thebigcalendar.service.NotificationService
import com.mss.thebigcalendar.ui.theme.TheBigCalendarTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Activity dedicada para exibir quando o alarme disparar
 * Esta activity acende a tela, toca o alarme e permite dismiss/snooze
 */
class AlarmActivity : ComponentActivity() {
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private var alarmSettings: AlarmSettings? = null
    
    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        private const val TAG = "AlarmActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Obter ID do alarme
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        if (alarmId == null) {
            finish()
            return
        }
        
        // Configurar para acender a tela e forçar abertura
        setupWakeLock()
        setupScreen()
        forceBringToFront()
        
        // Carregar configurações do alarme
        val alarmRepository = AlarmRepository(this)
        val notificationService = NotificationService(this)
        val alarmService = AlarmService(this, alarmRepository, notificationService)
        
        lifecycleScope.launch {
            // Cancelar notificação de alarme quando a activity abrir
            alarmService.cancelAlarmNotification(alarmId)
            
            alarmSettings = alarmRepository.getAlarmById(alarmId)
            if (alarmSettings == null) {
                finish()
                return@launch
            }
            
            // Iniciar som do alarme
            startAlarmSound()
            
            // Exibir UI
            setContent {
                TheBigCalendarTheme {
                    AlarmScreen(
                        alarmSettings = alarmSettings!!,
                        onDismiss = { dismissAlarm() },
                        onSnooze = { snoozeAlarm() }
                    )
                }
            }
        }
    }
    
    /**
     * Configura WakeLock para manter a tela ligada
     */
    private fun setupWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "TheBigCalendar:AlarmWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutos
    }
    
    /**
     * Configura a tela para acender e desbloquear
     */
    private fun setupScreen() {
        // Acender a tela
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        
        // Desbloquear se necessário
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguardManager.isKeyguardLocked) {
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }
    
    /**
     * Força a activity a aparecer na frente
     */
    private fun forceBringToFront() {
        try {
            // Trazer para frente usando flags de window
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            
            // Forçar foco e tela cheia
            if (!isFinishing) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            }
            
            // Tentar trazer para frente usando ActivityManager (API 21+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                try {
                    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    val tasks = activityManager.getRunningTasks(1)
                    if (tasks.isNotEmpty()) {
                        val topTask = tasks[0]
                        if (topTask.topActivity?.packageName == packageName) {
                            // Já está na frente
                            Log.d(TAG, "🔔 Activity já está na frente")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Não foi possível verificar posição da activity: ${e.message}")
                }
            }
            
            Log.d(TAG, "🔔 Activity configurada para aparecer na frente")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao forçar activity para frente", e)
        }
    }
    
    /**
     * Inicia o som do alarme
     */
    private fun startAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri != null) {
                // Obter o volume de alarme do sistema
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
                val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM)
                val volumeLevel = currentVolume.toFloat() / maxVolume.toFloat()
                
                Log.d(TAG, "🔔 Volume do alarme: $currentVolume/$maxVolume (${(volumeLevel * 100).toInt()}%)")
                
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@AlarmActivity, alarmUri)
                    isLooping = true
                    setVolume(volumeLevel, volumeLevel)
                    setAudioStreamType(android.media.AudioManager.STREAM_ALARM)
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao iniciar som do alarme", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Para o som do alarme
     */
    private fun stopAlarmSound() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        mediaPlayer = null
    }
    
    /**
     * Desativa o alarme
     */
    private fun dismissAlarm() {
        stopAlarmSound()
        wakeLock?.release()
        
        // Remover notificação de status do alarme
        lifecycleScope.launch {
            val alarmRepository = AlarmRepository(this@AlarmActivity)
            val notificationService = NotificationService(this@AlarmActivity)
            val alarmService = AlarmService(this@AlarmActivity, alarmRepository, notificationService)
            
            alarmSettings?.let { settings ->
                // Cancelar alarme no sistema
                alarmService.cancelAlarm(settings.id)
                
                // Cancelar notificação de status do alarme
                alarmService.cancelAlarmNotification(settings.id)
            }
        }
        
        finish()
    }
    
    /**
     * Adia o alarme (snooze)
     */
    private fun snoozeAlarm() {
        stopAlarmSound()
        wakeLock?.release()
        
        // Reagendar para snooze
        alarmSettings?.let { settings ->
            val snoozeTime = LocalTime.now().plusMinutes(settings.snoozeMinutes.toLong())
            val snoozeSettings = settings.copy(
                id = "${settings.id}_snooze_${System.currentTimeMillis()}",
                time = snoozeTime,
                repeatDays = emptySet() // Snooze é sempre único
            )
            
            lifecycleScope.launch {
                val alarmRepository = AlarmRepository(this@AlarmActivity)
                val notificationService = NotificationService(this@AlarmActivity)
                val alarmService = AlarmService(this@AlarmActivity, alarmRepository, notificationService)
                
                alarmRepository.saveAlarm(snoozeSettings)
                alarmService.scheduleAlarm(snoozeSettings)
            }
        }
        
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
        wakeLock?.release()
    }
}

@Composable
private fun AlarmScreen(
    alarmSettings: AlarmSettings,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    
    // Atualizar tempo a cada segundo
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Título do alarme
            Text(
                text = alarmSettings.label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // Horário atual
            Text(
                text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // Horário do alarme
            Text(
                text = "Alarme: ${alarmSettings.time.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                fontSize = 18.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Botões de ação
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Botão Snooze
                Button(
                    onClick = onSnooze,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Magenta
                    ),
                    modifier = Modifier.size(120.dp, 60.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = "Snooze\n${alarmSettings.snoozeMinutes}min",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
                
                // Botão Dismiss
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    modifier = Modifier.size(120.dp, 60.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlarmOff,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = "Desligar",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
