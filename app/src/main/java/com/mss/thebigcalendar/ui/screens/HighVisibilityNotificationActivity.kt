package com.mss.thebigcalendar.ui.screens

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
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
import com.mss.thebigcalendar.MainActivity
import com.mss.thebigcalendar.service.HighVisibilityNotificationService
import com.mss.thebigcalendar.service.NotificationService
import com.mss.thebigcalendar.ui.theme.TheBigCalendarTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Activity para exibir notificações de alta visibilidade
 * Mantém a tela ligada e o app ativo até que o usuário interaja
 */
class HighVisibilityNotificationActivity : ComponentActivity() {
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isActivityActive = true
    
    private val stopSoundsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.mss.thebigcalendar.STOP_ALL_SOUNDS") {
                Log.d(TAG, "🔇 Recebido broadcast para parar sons")
                stopNotificationSound()
            }
        }
    }
    
    companion object {
        private const val TAG = "HighVisibilityNotificationActivity"
    }
    
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "🔔 HighVisibilityNotificationActivity criada")
        
        // Registrar BroadcastReceiver para parar sons
        val filter = IntentFilter("com.mss.thebigcalendar.STOP_ALL_SOUNDS")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopSoundsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopSoundsReceiver, filter)
        }
        
        // Configurar para acender a tela e forçar abertura
        setupWakeLock()
        setupScreen()
        forceBringToFront()
        
        // Iniciar som de notificação
        startNotificationSound()
        
        // Configurar para manter a tela ligada
        keepScreenOn()
        
        // Exibir UI
        setContent {
            TheBigCalendarTheme {
                HighVisibilityNotificationScreen(
                    activityTitle = intent.getStringExtra(HighVisibilityNotificationService.EXTRA_ACTIVITY_TITLE) ?: "Notificação",
                    activityDescription = intent.getStringExtra(HighVisibilityNotificationService.EXTRA_ACTIVITY_DESCRIPTION),
                    activityDate = intent.getStringExtra(HighVisibilityNotificationService.EXTRA_ACTIVITY_DATE),
                    activityTime = intent.getStringExtra(HighVisibilityNotificationService.EXTRA_ACTIVITY_TIME),
                    onDismiss = { dismissNotification() },
                    onSnooze = { snoozeNotification() },
                    onOpenApp = { openMainApp() }
                )
            }
        }
    }
    
    /**
     * Configura WakeLock suave para manter o app ativo sem forçar tela a ligar
     */
    private fun setupWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        // Usar PARTIAL_WAKE_LOCK sem ACQUIRE_CAUSES_WAKEUP para não forçar tela a ligar
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TheBigCalendar:HighVisibilityWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutos - tempo menor
        Log.d(TAG, "🔔 WakeLock suave adquirido por 10 minutos")
    }
    
    /**
     * Configura a tela para aparecer quando desbloqueada (sem forçar a ligar)
     */
    private fun setupScreen() {
        // Configurar para aparecer quando desbloqueada, mas não forçar tela a ligar
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        
        // Não forçar desbloqueio - apenas mostrar quando desbloqueado
        Log.d(TAG, "🔔 Tela configurada para aparecer quando desbloqueada")
    }
    
    /**
     * Configura a activity para aparecer quando o usuário desbloquear
     */
    private fun forceBringToFront() {
        try {
            // Configurar para aparecer quando desbloqueada, mas não forçar tela a ligar
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            
            // Configurar tela cheia apenas se não estiver finalizando
            if (!isFinishing) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            }
            
            Log.d(TAG, "🔔 Activity configurada para aparecer quando desbloqueada")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao configurar activity", e)
        }
    }
    
    /**
     * Mantém a tela ligada
     */
    private fun keepScreenOn() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.d(TAG, "🔔 Tela configurada para permanecer ligada")
    }
    
    /**
     * Inicia som de notificação suave (apenas uma vez, sem repetir)
     */
    private fun startNotificationSound() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            // Verificar se o som está habilitado
            if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) {
                Log.d(TAG, "🔇 Som silenciado - não tocando notificação")
                return
            }
            
            val notificationUri = Settings.System.DEFAULT_NOTIFICATION_URI
            Log.d(TAG, "🔊 Iniciando som de notificação suave...")
            
            // Configurar atributos de áudio para notificação
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            // Criar MediaPlayer com configuração assíncrona
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(this@HighVisibilityNotificationActivity, notificationUri)
                setVolume(0.7f, 0.7f) // Volume moderado
                isLooping = false // Não repetir - apenas uma vez
                
                // Configurar listeners antes de preparar
                setOnPreparedListener { mp ->
                    Log.d(TAG, "🎵 Som preparado, tocando...")
                    try {
                        mp.start()
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Erro ao iniciar MediaPlayer", e)
                        mp.release()
                    }
                }
                
                setOnCompletionListener { mp ->
                    Log.d(TAG, "✅ Som de notificação finalizado")
                    mp.release()
                }
                
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "❌ Erro no MediaPlayer: what=$what, extra=$extra")
                    mp.release()
                    true
                }
                
                // Preparar de forma assíncrona
                prepareAsync()
            }
            
            // Adicionar vibração suave
            playVibration()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao configurar som de notificação", e)
            // Pelo menos tentar vibrar
            playVibration()
        }
    }
    
    /**
     * Toca vibração suave para notificações de alta visibilidade
     */
    private fun playVibration() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Padrão de vibração suave para notificações
                val vibrationPattern = longArrayOf(0, 300, 100, 300)
                val vibrationEffect = android.os.VibrationEffect.createWaveform(
                    vibrationPattern, 
                    -1 // Não repetir
                )
                vibrator.vibrate(vibrationEffect)
            } else {
                // Para versões anteriores ao Android 8.0
                vibrator.vibrate(longArrayOf(0, 300, 100, 300), -1)
            }
            
            Log.d(TAG, "📳 Vibração suave ativada")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao ativar vibração", e)
        }
    }
    
    /**
     * Para o som de notificação
     */
    private fun stopNotificationSound() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                    Log.d(TAG, "🔇 MediaPlayer parado")
                }
                player.reset()
                Log.d(TAG, "🔇 MediaPlayer resetado")
                player.release()
                Log.d(TAG, "🔇 MediaPlayer liberado")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Erro ao parar MediaPlayer: ${e.message}")
            }
        }
        mediaPlayer = null
        Log.d(TAG, "🔇 Som de notificação parado")
    }
    
    /**
     * Dismissa a notificação
     */
    private fun dismissNotification() {
        Log.d(TAG, "🔔 Dismissando notificação de alta visibilidade")
        
        isActivityActive = false
        stopNotificationSound()
        releaseWakeLock()
        
        // Enviar broadcast para o serviço dismissar a notificação
        val dismissIntent = Intent(this, HighVisibilityNotificationService::class.java).apply {
            action = HighVisibilityNotificationService.ACTION_DISMISS_NOTIFICATION
        }
        startService(dismissIntent)
        
        // Enviar broadcast para marcar atividade como concluída
        val activityId = intent.getStringExtra(HighVisibilityNotificationService.EXTRA_ACTIVITY_ID)
        Log.d(TAG, "🔔 Activity ID recebido para dismiss: $activityId")
        if (activityId != null) {
            val completeIntent = Intent(this, com.mss.thebigcalendar.service.NotificationReceiver::class.java).apply {
                action = NotificationService.ACTION_DISMISS
                putExtra(NotificationService.EXTRA_ACTIVITY_ID, activityId)
            }
            Log.d(TAG, "🔔 Enviando ACTION_DISMISS com ID: $activityId")
            sendBroadcast(completeIntent)
        }
        
        finish()
    }
    
    /**
     * Adia a notificação (snooze)
     */
    private fun snoozeNotification() {
        Log.d(TAG, "🔔 Adiando notificação de alta visibilidade")
        
        isActivityActive = false
        stopNotificationSound()
        releaseWakeLock()
        
        // Enviar broadcast para o serviço dismissar a notificação
        val dismissIntent = Intent(this, HighVisibilityNotificationService::class.java).apply {
            action = HighVisibilityNotificationService.ACTION_DISMISS_NOTIFICATION
        }
        startService(dismissIntent)
        
        // Enviar broadcast para adiar a notificação
        val activityId = intent.getStringExtra(HighVisibilityNotificationService.EXTRA_ACTIVITY_ID)
        if (activityId != null) {
            val snoozeIntent = Intent(this, com.mss.thebigcalendar.service.NotificationReceiver::class.java).apply {
                action = NotificationService.ACTION_SNOOZE
                putExtra(NotificationService.EXTRA_ACTIVITY_ID, activityId)
                putExtra("snooze_minutes", 5)
            }
            sendBroadcast(snoozeIntent)
        }
        
        finish()
    }
    
    /**
     * Abre o app principal
     */
    private fun openMainApp() {
        Log.d(TAG, "🔔 Abrindo app principal")
        
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        startActivity(mainIntent)
        
        // Dismissar a notificação após abrir o app
        dismissNotification()
    }
    
    /**
     * Libera WakeLock
     */
    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
                Log.d(TAG, "🔔 WakeLock liberado")
            }
        }
        wakeLock = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🔔 HighVisibilityNotificationActivity destruída")
        
        // Desregistrar BroadcastReceiver
        try {
            unregisterReceiver(stopSoundsReceiver)
            Log.d(TAG, "🔇 BroadcastReceiver desregistrado")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro ao desregistrar BroadcastReceiver: ${e.message}")
        }
        
        stopNotificationSound()
        releaseWakeLock()
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "🔔 Activity pausada - mantendo WakeLock ativo")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔔 Activity resumida")
    }
}

@Composable
private fun HighVisibilityNotificationScreen(
    activityTitle: String,
    activityDescription: String?,
    activityDate: String?,
    activityTime: String?,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onOpenApp: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Título da notificação
            Text(
                text = "🔔 NOTIFICAÇÃO IMPORTANTE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
            
            // Título da atividade
            Text(
                text = activityTitle,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            // Descrição da atividade
            activityDescription?.let { description ->
                Text(
                    text = description,
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            // Horário atual
            Text(
                text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // Data e horário da atividade
            if (activityDate != null || activityTime != null) {
                val dateTimeText = buildString {
                    activityDate?.let { append("Data: $it") }
                    if (activityDate != null && activityTime != null) append(" | ")
                    activityTime?.let { append("Horário: $it") }
                }
                
                Text(
                    text = dateTimeText,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Botões de ação
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Botão para abrir o app
                Button(
                    onClick = onOpenApp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Blue
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Text(
                        text = "ABRIR APP",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Botões de ação
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Botão Snooze
                    Button(
                        onClick = onSnooze,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Magenta
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(70.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Adiar 5min",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Botão Dismiss
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(70.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AlarmOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Finalizar",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
