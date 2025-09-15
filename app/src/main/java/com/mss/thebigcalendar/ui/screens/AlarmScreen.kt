package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.clickable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import android.util.Log
import android.app.TimePickerDialog
import android.widget.TimePicker
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.AlarmSettings
import com.mss.thebigcalendar.data.repository.AlarmRepository
import com.mss.thebigcalendar.service.AlarmService
import com.mss.thebigcalendar.service.NotificationService
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    onBackClick: () -> Unit,
    onBackPressedDispatcher: OnBackPressedDispatcher? = null,
    activityToEdit: Activity? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    
    // Tratar o botão de voltar do sistema
    onBackPressedDispatcher?.let { dispatcher ->
        DisposableEffect(dispatcher) {
            val callback = object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBackClick()
                }
            }
            dispatcher.addCallback(callback)
            onDispose {
                callback.remove()
            }
        }
    }
    
    // ID único para o alarme (usar ID da atividade se estiver editando, senão criar novo)
    val alarmId = remember(activityToEdit?.id) { 
        activityToEdit?.id ?: "alarm_${System.currentTimeMillis()}"
    }
    
    // Estados do alarme (inicializados com valores padrão)
    var selectedTime by remember { mutableStateOf(LocalTime.of(8, 0)) }
    var isAlarmEnabled by remember { mutableStateOf(false) }
    var alarmLabel by remember { mutableStateOf("") }
    var repeatDays by remember { mutableStateOf(setOf<String>()) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var snoozeMinutes by remember { mutableStateOf(5) }
    
    // Estados de UI
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoaded by remember { mutableStateOf(false) }
    
    // Serviços
    val alarmRepository = remember { AlarmRepository(context) }
    val notificationService = remember { NotificationService(context) }
    val alarmService = remember { AlarmService(context, alarmRepository, notificationService) }
    
    // Função para mostrar o seletor de horário nativo do Android
    fun showNativeTimePicker() {
        val timePickerDialog = TimePickerDialog(
            context,
            { _: TimePicker, hourOfDay: Int, minute: Int ->
                selectedTime = LocalTime.of(hourOfDay, minute)
            },
            selectedTime.hour,
            selectedTime.minute,
            true // 24 horas
        )
        timePickerDialog.setTitle("Selecionar Horário")
        timePickerDialog.show()
    }
    
    // Carregar configurações salvas do alarme
    LaunchedEffect(alarmId) {
        try {
            val savedAlarm = alarmRepository.getAlarmById(alarmId)
            if (savedAlarm != null) {
                // Carregar configurações salvas do alarme
                selectedTime = savedAlarm.time
                isAlarmEnabled = savedAlarm.isEnabled
                alarmLabel = savedAlarm.label
                repeatDays = savedAlarm.repeatDays
                soundEnabled = savedAlarm.soundEnabled
                vibrationEnabled = savedAlarm.vibrationEnabled
                snoozeMinutes = savedAlarm.snoozeMinutes
                Log.d("AlarmScreen", "📱 Configurações do alarme carregadas: ${savedAlarm.label}")
            } else if (activityToEdit != null) {
                // Se não há alarme salvo mas há uma atividade para editar, usar dados da atividade
                selectedTime = activityToEdit.startTime ?: LocalTime.of(8, 0)
                isAlarmEnabled = true
                alarmLabel = activityToEdit.title
                repeatDays = emptySet() // Atividades não têm dias de repetição por padrão
                soundEnabled = true
                vibrationEnabled = true
                snoozeMinutes = 5
                Log.d("AlarmScreen", "📱 Usando dados da atividade para editar: ${activityToEdit.title}")
            } else {
                Log.d("AlarmScreen", "📱 Nenhuma configuração salva encontrada, usando padrões")
            }
        } catch (e: Exception) {
            Log.e("AlarmScreen", "❌ Erro ao carregar configurações do alarme", e)
        } finally {
            isLoaded = true
        }
    }
    
    // Salvar configurações automaticamente quando mudarem
    LaunchedEffect(selectedTime, isAlarmEnabled, alarmLabel, repeatDays, soundEnabled, vibrationEnabled, snoozeMinutes) {
        if (isLoaded) {
            try {
                val currentAlarm = AlarmSettings.createDefault().copy(
                    id = alarmId,
                    label = alarmLabel,
                    time = selectedTime,
                    isEnabled = isAlarmEnabled,
                    repeatDays = repeatDays,
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                    snoozeMinutes = snoozeMinutes,
                    createdAt = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis()
                )
                alarmRepository.saveAlarm(currentAlarm)
                Log.d("AlarmScreen", "💾 Configurações do alarme salvas automaticamente")
            } catch (e: Exception) {
                Log.e("AlarmScreen", "❌ Erro ao salvar configurações do alarme", e)
            }
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (activityToEdit != null) "Editar Despertador" else "Despertador",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (!isLoaded) {
            // Mostrar loading enquanto carrega as configurações
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Carregando configurações...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Mensagem informativa se estiver editando
            if (activityToEdit != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Editando despertador para: ${activityToEdit.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Área central principal - Hora, ícone e status
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Ícone do relógio centralizado
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    // Horário do despertador em destaque (clicável)
                    Text(
                        text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { 
                            showNativeTimePicker()
                        }
                    )
                    
                    // Status do despertador
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isAlarmEnabled) Icons.Default.AlarmOn else Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isAlarmEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isAlarmEnabled) "Despertador ativado" else "Despertador desativado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isAlarmEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Configurações abaixo da área central
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            
            // Seletor de horário nativo do Android
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Selecionar Horário",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Toque no horário acima para abrir o seletor nativo do Android",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            
            // Seletor customizado comentado - usando seletor nativo do Android
            /*
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Horário do Despertador",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // TimePicker real
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botão para diminuir hora
                        IconButton(
                            onClick = { 
                                selectedTime = selectedTime.minusHours(1)
                            }
                        ) {
                            Text(
                                text = "−",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Hora atual (clicável para abrir TimePicker)
                        Text(
                            text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showTimePicker = true }
                        )
                        
                        // Botão para aumentar hora
                        IconButton(
                            onClick = { 
                                selectedTime = selectedTime.plusHours(1)
                            }
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Controles de minutos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Minutos:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        IconButton(
                            onClick = { 
                                selectedTime = selectedTime.minusMinutes(15)
                            }
                        ) {
                            Text(
                                text = "−15",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        IconButton(
                            onClick = { 
                                selectedTime = selectedTime.minusMinutes(5)
                            }
                        ) {
                            Text(
                                text = "−5",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        IconButton(
                            onClick = { 
                                selectedTime = selectedTime.plusMinutes(5)
                            }
                        ) {
                            Text(
                                text = "+5",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        IconButton(
                            onClick = { 
                                selectedTime = selectedTime.plusMinutes(15)
                            }
                        ) {
                            Text(
                                text = "+15",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            */
            
            // Configurações adicionais
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Configurações",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Switch para ativar/desativar despertador
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ativar Despertador",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = isAlarmEnabled,
                            onCheckedChange = { isAlarmEnabled = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Campo para rótulo do despertador
                    OutlinedTextField(
                        value = alarmLabel,
                        onValueChange = { alarmLabel = it },
                        label = { Text("Rótulo do despertador") },
                        placeholder = { Text("Ex: Acordar para trabalho") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Configurações de som e vibração
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Som",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { soundEnabled = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vibração",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = { vibrationEnabled = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Configuração de snooze
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Snooze (minutos)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { 
                                    if (snoozeMinutes > 1) snoozeMinutes--
                                }
                            ) {
                                Text("-", style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text = "$snoozeMinutes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                                onClick = { 
                                    if (snoozeMinutes < 60) snoozeMinutes++
                                }
                            ) {
                                Text("+", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
            
            // Dias da semana para repetição
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Repetir",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val daysOfWeek = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Primeira linha - Domingo a Quarta-feira
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val firstRowDays = listOf("Dom", "Seg", "Ter", "Qua")
                            firstRowDays.forEach { day ->
                                FilterChip(
                                    onClick = {
                                        repeatDays = if (repeatDays.contains(day)) {
                                            repeatDays - day
                                        } else {
                                            repeatDays + day
                                        }
                                    },
                                    label = { 
                                        Text(
                                            text = day,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        ) 
                                    },
                                    selected = repeatDays.contains(day)
                                )
                            }
                        }
                        
                        // Segunda linha - Quinta a Sábado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val secondRowDays = listOf("Qui", "Sex", "Sáb")
                            secondRowDays.forEach { day ->
                                FilterChip(
                                    onClick = {
                                        repeatDays = if (repeatDays.contains(day)) {
                                            repeatDays - day
                                        } else {
                                            repeatDays + day
                                        }
                                    },
                                    label = { 
                                        Text(
                                            text = day,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        ) 
                                    },
                                    selected = repeatDays.contains(day)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mensagens de feedback
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            successMessage?.let { success ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = success,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Botões de ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("Cancelar")
                }
                
                Button(
                    onClick = {
                        keyboardController?.hide()
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                saveAlarm(
                                    alarmId = alarmId,
                                    selectedTime = selectedTime,
                                    isAlarmEnabled = isAlarmEnabled,
                                    alarmLabel = alarmLabel,
                                    repeatDays = repeatDays,
                                    soundEnabled = soundEnabled,
                                    vibrationEnabled = vibrationEnabled,
                                    snoozeMinutes = snoozeMinutes,
                                    alarmRepository = alarmRepository,
                                    alarmService = alarmService,
                                    onSuccess = { 
                                        successMessage = "Despertador salvo com sucesso!"
                                        errorMessage = null
                                        isLoading = false
                                    },
                                    onError = { error ->
                                        errorMessage = error
                                        successMessage = null
                                        isLoading = false
                                    },
                                    onLoading = { 
                                        isLoading = true
                                    }
                                )
                            } catch (e: Exception) {
                                errorMessage = "Erro inesperado: ${e.message}"
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Salvar Despertador")
                    }
                }
            }
            }
        }
        }
    }
}

/**
 * Função para salvar o alarme
 */
private suspend fun saveAlarm(
    alarmId: String,
    selectedTime: LocalTime,
    isAlarmEnabled: Boolean,
    alarmLabel: String,
    repeatDays: Set<String>,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    snoozeMinutes: Int,
    alarmRepository: AlarmRepository,
    alarmService: AlarmService,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onLoading: () -> Unit
) {
    onLoading()
    
    // Validar dados
    val label = alarmLabel.trim()
    if (label.isEmpty()) {
        onError("Rótulo do despertador é obrigatório")
        return
    }
    
    if (label.length > 50) {
        onError("Rótulo muito longo (máximo 50 caracteres)")
        return
    }
    
    // Criar configurações do alarme com ID persistente
    val alarmSettings = AlarmSettings(
        id = alarmId,
        label = label,
        time = selectedTime,
        isEnabled = isAlarmEnabled,
        repeatDays = repeatDays,
        soundEnabled = soundEnabled,
        vibrationEnabled = vibrationEnabled,
        snoozeMinutes = snoozeMinutes,
        createdAt = System.currentTimeMillis(),
        lastModified = System.currentTimeMillis()
    )
    
    // Validar configurações
    val validation = AlarmSettings.validate(alarmSettings)
    if (validation is AlarmSettings.ValidationResult.Error) {
        onError(validation.message)
        return
    }
    
    try {
        // Salvar no repositório
        val result = alarmRepository.saveAlarm(alarmSettings)
        result.fold(
            onSuccess = { savedAlarm ->
                // Agendar no sistema
                val scheduleResult = alarmService.scheduleAlarm(savedAlarm)
                scheduleResult.fold(
                    onSuccess = {
                        onSuccess()
                    },
                    onFailure = { error ->
                        onError("Erro ao agendar alarme: ${error.message}")
                    }
                )
            },
            onFailure = { error ->
                onError("Erro ao salvar alarme: ${error.message}")
            }
        )
    } catch (e: Exception) {
        onError("Erro inesperado: ${e.message}")
    }
}
