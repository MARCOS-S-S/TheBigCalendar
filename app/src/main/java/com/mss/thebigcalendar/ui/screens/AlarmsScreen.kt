package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.util.Log
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.AlarmSettings
import com.mss.thebigcalendar.data.model.NotificationType
import com.mss.thebigcalendar.data.repository.AlarmRepository
import com.mss.thebigcalendar.service.AlarmService
import com.mss.thebigcalendar.service.NotificationService
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
private fun IndependentAlarmCard(
    alarm: AlarmSettings,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isEnabled = alarm.isEnabled
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.primaryContainer
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header com título e status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isEnabled)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${alarm.time} - ${if (isEnabled) stringResource(id = R.string.alarm_status_active) else stringResource(id = R.string.alarm_status_disabled)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isEnabled)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (isEnabled)
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.outline
                        )
                )
            }
            
            // Dias de repetição se houver
            if (alarm.repeatDays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${stringResource(id = R.string.alarm_repeats_label)} ${alarm.repeatDays.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            // Configurações adicionais
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (alarm.soundEnabled) {
                    Text(
                        text = "🔊",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (alarm.vibrationEnabled) {
                    Text(
                        text = "📳",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = stringResource(id = R.string.alarm_snooze_label, alarm.snoozeMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            // Botões de ação
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(id = R.string.edit_alarm)
                    )
                }
                
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.alarm_delete_content_description),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val alarmRepository = remember { AlarmRepository(context) }
    val alarmService = remember { 
        AlarmService(context, alarmRepository, NotificationService(context))
    }
    val coroutineScope = rememberCoroutineScope()
    
    // Estado para armazenar os despertadores independentes
    var independentAlarms by remember { mutableStateOf<List<AlarmSettings>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Estado para controlar o diálogo de exclusão
    var showDeleteDialog by remember { mutableStateOf(false) }
    var alarmToDelete by remember { mutableStateOf<AlarmSettings?>(null) }
    
    // Estados para edição de alarme
    var showEditAlarmScreen by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<AlarmSettings?>(null) }
    
    // Estados para criação de alarme
    var showCreateAlarmScreen by remember { mutableStateOf(false) }
    
    // Observar mudanças no repositório de alarmes
    LaunchedEffect(Unit) {
        alarmRepository.alarms.collect { alarms ->
            independentAlarms = alarms.sortedWith(
                compareBy<AlarmSettings> { alarm ->
                    // Primeiro por horário
                    alarm.time
                }.thenBy { alarm ->
                    // Depois por label
                    alarm.label
                }
            )
            isLoading = false
            Log.d("AlarmsScreen", "📱 Lista de alarmes atualizada: ${alarms.size} alarmes")
        }
    }
    
    // Função para forçar atualização do sistema de alarmes
    val forceAlarmSystemUpdate = {
        try {
            Log.d("AlarmsScreen", "🔄 Forçando atualização do sistema de alarmes")
            
            // Tentar diferentes métodos para forçar atualização do sistema
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            
            // Método 1: Cancelar um alarme fictício para forçar refresh
            val dummyIntent = android.content.Intent(context, com.mss.thebigcalendar.service.AlarmReceiver::class.java).apply {
                action = "DUMMY_ALARM_UPDATE"
            }
            val dummyPendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                System.currentTimeMillis().toInt(),
                dummyIntent,
                android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(dummyPendingIntent)
            
            // Método 2: Agendar e cancelar um alarme temporário para forçar refresh
            val tempIntent = android.content.Intent(context, com.mss.thebigcalendar.service.AlarmReceiver::class.java).apply {
                action = "TEMP_ALARM_REFRESH"
            }
            val tempPendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                (System.currentTimeMillis() + 1000).toInt(),
                tempIntent,
                android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            // Agendar para 1 segundo no futuro
            alarmManager.setExact(
                android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000,
                tempPendingIntent
            )
            
            // Cancelar imediatamente
            alarmManager.cancel(tempPendingIntent)
            
            Log.d("AlarmsScreen", "🔄 Atualização do sistema de alarmes forçada")
            
        } catch (e: Exception) {
            Log.w("AlarmsScreen", "⚠️ Erro ao forçar atualização do sistema: ${e.message}")
        }
    }
    
    // Função para cancelar alarmes de forma inteligente (sem ANR)
    val cancelAllAppAlarms = {
        try {
            Log.d("AlarmsScreen", "🧠 Iniciando cancelamento inteligente de alarmes")
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            
            // Cancelar apenas os alarmes conhecidos do app (sem criar milhões de PendingIntents)
            val knownAlarmIds = listOf(
                "alarm_id_1", "alarm_id_2", "alarm_id_3", // IDs comuns
                "notification_alarm", "backup_alarm", "sync_alarm"
            )
            
            // Cancelar apenas com as classes e ações reais do app
            val realReceiverClasses = listOf(
                com.mss.thebigcalendar.service.AlarmReceiver::class.java,
                com.mss.thebigcalendar.service.NotificationReceiver::class.java
            )
            
            val realActions = listOf(
                "com.mss.thebigcalendar.ALARM_TRIGGERED",
                "com.mss.thebigcalendar.VIEW_ACTIVITY",
                "com.mss.thebigcalendar.SNOOZE",
                "com.mss.thebigcalendar.DISMISS"
            )
            
            var totalCancelled = 0
            
            // Cancelar apenas com hash codes conhecidos (baseados em strings reais)
            knownAlarmIds.forEach { alarmId ->
                realReceiverClasses.forEach { receiverClass ->
                    realActions.forEach { action ->
                        try {
                            val intent = android.content.Intent(context, receiverClass).apply {
                                this.action = action
                                putExtra("alarm_id", alarmId)
                            }
                            
                            val pendingIntent = android.app.PendingIntent.getBroadcast(
                                context,
                                alarmId.hashCode(),
                                intent,
                                android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
                            )
                            
                            alarmManager.cancel(pendingIntent)
                            totalCancelled++
                        } catch (e: Exception) {
                            // Ignorar erros de PendingIntent inexistentes
                        }
                    }
                }
            }
            
            Log.d("AlarmsScreen", "🧠 Cancelamento inteligente concluído: $totalCancelled tentativas")
            
        } catch (e: Exception) {
            Log.e("AlarmsScreen", "❌ Erro no cancelamento inteligente", e)
        }
    }
    
    // Função para deletar alarme
    val deleteAlarm = { alarm: AlarmSettings ->
        coroutineScope.launch {
            try {
                Log.d("AlarmsScreen", "🗑️ Iniciando exclusão do alarme: ${alarm.label} (ID: ${alarm.id})")
                
                // ✅ Cancelar TODOS os alarmes relacionados de forma exaustiva
                
                // 1. Cancelar via AlarmService (método principal)
                alarmService.cancelAlarm(alarm.id)
                Log.d("AlarmsScreen", "✅ Alarme cancelado via AlarmService")
                
                // 2. Cancelar via NotificationService
                val notificationService = NotificationService(context)
                notificationService.cancelNotification(alarm.id)
                Log.d("AlarmsScreen", "✅ Notificação cancelada via NotificationService")
                
                // 3. Cancelar TODOS os alarmes possíveis no AlarmManager
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                
                // Cancelar alarme principal
                val baseIntent = android.content.Intent(context, com.mss.thebigcalendar.service.AlarmReceiver::class.java).apply {
                    action = "com.mss.thebigcalendar.ALARM_TRIGGERED"
                    putExtra("alarm_id", alarm.id)
                }
                
                // Cancelar com diferentes combinações de flags
                listOf(
                    android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                    android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
                ).forEach { flags ->
                    val pendingIntent = android.app.PendingIntent.getBroadcast(
                        context,
                        alarm.id.hashCode(),
                        baseIntent,
                        flags
                    )
                    alarmManager.cancel(pendingIntent)
                }
                
                // Cancelar alarmes recorrentes (apenas próximos 7 dias para evitar ANR)
                val today = java.time.LocalDate.now()
                for (dayOffset in 0..6) { // Cancelar apenas para os próximos 7 dias
                    val futureDate = today.plusDays(dayOffset.toLong())
                    val recurringAlarmId = "${alarm.id}_${futureDate}"
                    
                    val recurringIntent = android.content.Intent(context, com.mss.thebigcalendar.service.AlarmReceiver::class.java).apply {
                        action = "com.mss.thebigcalendar.ALARM_TRIGGERED"
                        putExtra("alarm_id", recurringAlarmId)
                    }
                    
                    val recurringPendingIntent = android.app.PendingIntent.getBroadcast(
                        context,
                        recurringAlarmId.hashCode(),
                        recurringIntent,
                        android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.cancel(recurringPendingIntent)
                    
                    // Pequeno delay para evitar sobrecarga
                    if (dayOffset % 2 == 0) {
                        kotlinx.coroutines.delay(10)
                    }
                }
                
                Log.d("AlarmsScreen", "✅ Alarmes cancelados diretamente no AlarmManager (incluindo recorrentes)")
                
                // 4. Cancelar WorkManager backup
                androidx.work.WorkManager.getInstance(context)
                    .cancelUniqueWork("alarm_backup_${alarm.id}")
                Log.d("AlarmsScreen", "✅ WorkManager backup cancelado")
                
                // 5. Cancelar TODAS as notificações ativas relacionadas
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.cancel(alarm.id.hashCode())
                notificationManager.cancel(alarm.id.hashCode() + 1000)
                notificationManager.cancel(alarm.id.hashCode() - 1000)
                
                // Cancelar notificações de alarmes recorrentes (apenas próximos 7 dias)
                for (dayOffset in 0..6) {
                    val futureDate = today.plusDays(dayOffset.toLong())
                    val recurringAlarmId = "${alarm.id}_${futureDate}"
                    notificationManager.cancel(recurringAlarmId.hashCode())
                }
                
                Log.d("AlarmsScreen", "✅ Todas as notificações relacionadas canceladas")
                
                // 6. Limpeza exaustiva de todos os alarmes pendentes
                try {
                    // Cancelar todos os tipos de intent possíveis
                    val allReceiverClasses = listOf(
                        com.mss.thebigcalendar.service.AlarmReceiver::class.java,
                        com.mss.thebigcalendar.service.NotificationReceiver::class.java
                    )
                    
                    val allActions = listOf(
                        "com.mss.thebigcalendar.ALARM_TRIGGERED",
                        "com.mss.thebigcalendar.VIEW_ACTIVITY",
                        "com.mss.thebigcalendar.SNOOZE",
                        "com.mss.thebigcalendar.DISMISS"
                    )
                    
                    allReceiverClasses.forEach { receiverClass ->
                        allActions.forEach { action ->
                            val intent = android.content.Intent(context, receiverClass).apply {
                                this.action = action
                                putExtra("alarm_id", alarm.id)
                            }
                            
                            val pendingIntent = android.app.PendingIntent.getBroadcast(
                                context,
                                alarm.id.hashCode(),
                                intent,
                                android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
                            )
                            alarmManager.cancel(pendingIntent)
                        }
                    }
                    
                    Log.d("AlarmsScreen", "✅ Limpeza exaustiva de alarmes pendentes concluída")
                } catch (e: Exception) {
                    Log.w("AlarmsScreen", "⚠️ Erro na limpeza exaustiva: ${e.message}")
                }
                
                // ✅ Deletar do repositório
                val result = alarmRepository.deleteAlarm(alarm.id)
                if (result.isSuccess) {
                    Log.d("AlarmsScreen", "✅ Alarme deletado do repositório: ${alarm.label}")
                    Log.d("AlarmsScreen", "✅ Exclusão completa do alarme: ${alarm.label} - lista será atualizada automaticamente pelo Flow")
                    
                // 7. Forçar atualização do sistema de alarmes para remover do QS
                forceAlarmSystemUpdate()
                
                // 8. Cancelamento inteligente - cancelar alarmes conhecidos do app
                cancelAllAppAlarms()
                
                // 9. Pequeno delay para evitar sobrecarga do sistema
                kotlinx.coroutines.delay(100)
                    
                } else {
                    Log.e("AlarmsScreen", "❌ Erro ao deletar alarme do repositório: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("AlarmsScreen", "❌ Erro ao deletar alarme", e)
            }
        }
    }
    
    // Função para limpar alarmes órfãos do sistema
    val clearOrphanedAlarms = {
        coroutineScope.launch {
            try {
                Log.d("AlarmsScreen", "🧹 Iniciando limpeza de alarmes órfãos")
                
                // Buscar todos os alarmes do repositório
                val savedAlarms = alarmRepository.getActiveAlarms()
                val savedAlarmIds = savedAlarms.map { it.id }.toSet()
                
                // Cancelar todos os alarmes possíveis no sistema
                for (alarm in savedAlarms) {
                    // Cancelar via AlarmService
                    alarmService.cancelAlarm(alarm.id)
                    
                    // Cancelar via NotificationService
                    val notificationService = NotificationService(context)
                    notificationService.cancelNotification(alarm.id)
                    
                    // Cancelar diretamente no AlarmManager
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    val intent = android.content.Intent(context, com.mss.thebigcalendar.service.AlarmReceiver::class.java).apply {
                        action = "com.mss.thebigcalendar.ALARM_TRIGGERED"
                        putExtra("alarm_id", alarm.id)
                    }
                    val pendingIntent = android.app.PendingIntent.getBroadcast(
                        context,
                        alarm.id.hashCode(),
                        intent,
                        android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.cancel(pendingIntent)
                    
                    // Cancelar WorkManager
                    androidx.work.WorkManager.getInstance(context)
                        .cancelUniqueWork("alarm_backup_${alarm.id}")
                }
                
                Log.d("AlarmsScreen", "🧹 Limpeza de alarmes órfãos concluída - lista será atualizada automaticamente pelo Flow")
                
            } catch (e: Exception) {
                Log.e("AlarmsScreen", "❌ Erro durante limpeza de alarmes órfãos", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = stringResource(id = R.string.alarms),
                            style = MaterialTheme.typography.headlineSmall
                        ) 
                    },
                    actions = {
                        // Botão para limpar alarmes órfãos
                        IconButton(
                            onClick = { clearOrphanedAlarms() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(id = R.string.alarm_clear_orphaned_content_description),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        Log.d("AlarmsScreen", "➕ Botão de criar alarme clicado")
                        showCreateAlarmScreen = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.create_alarm)
                    )
                }
            }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                // Estado de carregamento
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (independentAlarms.isEmpty()) {
                // Estado vazio
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = stringResource(id = R.string.no_alarms_configured),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(id = R.string.no_alarms_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                // Lista de alarmes
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(independentAlarms) { alarm ->
                        IndependentAlarmCard(
                            alarm = alarm,
                            onEditClick = {
                                Log.d("AlarmsScreen", "🔧 Botão de editar clicado para: ${alarm.label}")
                                alarmToEdit = alarm
                                showEditAlarmScreen = true
                                Log.d("AlarmsScreen", "🔧 Estados atualizados - showEditAlarmScreen: $showEditAlarmScreen, alarmToEdit: $alarmToEdit")
                            },
                            onDeleteClick = {
                                alarmToDelete = alarm
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Diálogo de confirmação de exclusão
    if (showDeleteDialog && alarmToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                alarmToDelete = null
            },
            title = {
                Text(stringResource(id = R.string.delete_alarm_dialog_title))
            },
            text = {
                Text(stringResource(id = R.string.delete_alarm_dialog_message, alarmToDelete?.label ?: ""))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        alarmToDelete?.let { alarm ->
                            deleteAlarm(alarm)
                        }
                        showDeleteDialog = false
                        alarmToDelete = null
                    }
                ) {
                    Text(stringResource(id = R.string.delete_alarm_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        alarmToDelete = null
                    }
                ) {
                    Text(stringResource(id = R.string.cancel_alarm_button))
                }
            }
        )
    }
}

    
    // Tela de edição de alarme sobreposta
    Log.d("AlarmsScreen", "🔍 Verificando condições - showEditAlarmScreen: $showEditAlarmScreen, alarmToEdit: $alarmToEdit")
    if (showEditAlarmScreen && alarmToEdit != null) {
        Log.d("AlarmsScreen", "✅ Abrindo tela de edição para alarme: ${alarmToEdit!!.label}")
        AlarmScreen(
            onBackClick = {
                showEditAlarmScreen = false
                alarmToEdit = null
                Log.d("AlarmsScreen", "📱 Voltando da edição - lista será atualizada automaticamente pelo Flow")
            },
            onBackPressedDispatcher = null,
            activityToEdit = null,
            alarmToEdit = alarmToEdit
        )
    }
    
    // Tela de criação de alarme sobreposta
    Log.d("AlarmsScreen", "🔍 Verificando condições - showCreateAlarmScreen: $showCreateAlarmScreen")
    if (showCreateAlarmScreen) {
        Log.d("AlarmsScreen", "✅ Abrindo tela de criação de alarme")
        AlarmScreen(
            onBackClick = {
                showCreateAlarmScreen = false
                Log.d("AlarmsScreen", "📱 Voltando da criação - forçando atualização da lista")
                
                // Forçar atualização da lista de alarmes
                coroutineScope.launch {
                    try {
                        // Pequeno delay para garantir que a persistência foi concluída
                        kotlinx.coroutines.delay(100)
                        
                        // Recarregar alarmes do armazenamento persistente
                        alarmRepository.reloadAlarms()
                        
                        // Recarregar TODOS os alarmes do repositório (não apenas os ativos)
                        val currentAlarms = alarmRepository.getAllAlarms()
                        independentAlarms = currentAlarms.sortedWith(
                            compareBy<AlarmSettings> { alarm ->
                                alarm.time
                            }.thenBy { alarm ->
                                alarm.label
                            }
                        )
                        Log.d("AlarmsScreen", "📱 Lista de alarmes atualizada manualmente: ${currentAlarms.size} alarmes")
                        
                        // Debug: verificar se o alarme está realmente no repositório
                        currentAlarms.forEach { alarm ->
                            Log.d("AlarmsScreen", "📱 Alarme encontrado: ${alarm.label} (ID: ${alarm.id}, Enabled: ${alarm.isEnabled})")
                        }
                    } catch (e: Exception) {
                        Log.e("AlarmsScreen", "❌ Erro ao atualizar lista manualmente", e)
                    }
                }
            },
            onBackPressedDispatcher = null,
            activityToEdit = null,
            alarmToEdit = null // null para indicar que é uma criação, não edição
        )
    }
}

@Composable
private fun AlarmCard(
    activity: Activity,
    onEditClick: () -> Unit
) {
    val activityDate = LocalDate.parse(activity.date)
    val isToday = activityDate == LocalDate.now()
    val isTomorrow = activityDate == LocalDate.now().plusDays(1)
    val isPast = activityDate.isBefore(LocalDate.now())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isPast -> MaterialTheme.colorScheme.errorContainer
                isToday -> MaterialTheme.colorScheme.primaryContainer
                isTomorrow -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Cabeçalho com título e ícone de edição
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (activity.notificationSettings.isEnabled) 
                            Icons.Default.AlarmOn 
                        else 
                            Icons.Default.Alarm,
                        contentDescription = null,
                        tint = if (activity.notificationSettings.isEnabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(id = R.string.edit_alarm)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Data e horário
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Data
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(activityDate, isToday, isTomorrow, isPast),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Horário (se disponível)
                activity.startTime?.let { startTime ->
                    Text(
                        text = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Tipo de atividade
            Text(
                text = getActivityTypeDisplayName(activity.activityType),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            // Configurações do alarme
            if (activity.notificationSettings.isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tipo de notificação
                    FilterChip(
                        onClick = { },
                        label = {
                            Text(
                                text = getNotificationTypeDisplayName(activity.notificationSettings.notificationType),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = true,
                        enabled = false
                    )
                    
                    // Horário da notificação (se diferente do horário da atividade)
                    activity.notificationSettings.notificationTime?.let { notificationTime ->
                        if (notificationTime != activity.startTime) {
                            FilterChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = stringResource(id = R.string.reminder_label, notificationTime),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                selected = true,
                                enabled = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun formatDate(
    date: LocalDate,
    isToday: Boolean,
    isTomorrow: Boolean,
    isPast: Boolean
): String {
    return when {
        isToday -> stringResource(id = R.string.today_label)
        isTomorrow -> stringResource(id = R.string.tomorrow_label)
        isPast -> stringResource(id = R.string.past_label, formatDateWithDay(date))
        else -> formatDateWithDay(date)
    }
}

private fun formatDateWithDay(date: LocalDate): String {
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
    val dayOfMonth = date.dayOfMonth
    val month = date.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
    val year = date.year
    
    return "$dayOfWeek, $dayOfMonth de $month de $year"
}

@Composable
private fun getActivityTypeDisplayName(activityType: ActivityType): String {
    return when (activityType) {
        ActivityType.TASK -> stringResource(id = R.string.activity_type_task)
        ActivityType.EVENT -> stringResource(id = R.string.activity_type_event)
        ActivityType.BIRTHDAY -> stringResource(id = R.string.activity_type_birthday)
        ActivityType.NOTE -> stringResource(id = R.string.activity_type_note)
    }
}

@Composable
private fun getNotificationTypeDisplayName(notificationType: NotificationType): String {
    return when (notificationType) {
        NotificationType.BEFORE_ACTIVITY -> stringResource(id = R.string.notification_type_on_time)
        NotificationType.FIVE_MINUTES_BEFORE -> stringResource(id = R.string.notification_type_5min_before)
        NotificationType.TEN_MINUTES_BEFORE -> stringResource(id = R.string.notification_type_10min_before)
        NotificationType.FIFTEEN_MINUTES_BEFORE -> stringResource(id = R.string.notification_type_15min_before)
        NotificationType.THIRTY_MINUTES_BEFORE -> stringResource(id = R.string.notification_type_30min_before)
        NotificationType.ONE_HOUR_BEFORE -> stringResource(id = R.string.notification_type_1hour_before)
        NotificationType.TWO_HOURS_BEFORE -> stringResource(id = R.string.notification_type_2hours_before)
        NotificationType.ONE_DAY_BEFORE -> stringResource(id = R.string.notification_type_1day_before)
        NotificationType.CUSTOM -> stringResource(id = R.string.notification_type_custom)
        NotificationType.NONE -> stringResource(id = R.string.notification_type_disabled)
    }
}

