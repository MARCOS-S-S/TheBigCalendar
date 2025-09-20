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
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
    alarmToEdit: AlarmSettings? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    
    // Estado para controlar o diálogo de permissão
    var showPermissionDialog by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        )
    }
    var showThankYouMessage by remember { mutableStateOf(false) }
    
    // Verificar permissões quando a tela abrir
    LaunchedEffect(Unit) {
        // Só mostrar diálogo se realmente não tiver permissão
        if (!hasOverlayPermission) {
            showPermissionDialog = true
        }
    }
    
    // Estados do alarme
    var selectedTime by remember { mutableStateOf(LocalTime.of(8, 0)) }
    var isAlarmEnabled by remember { mutableStateOf(true) }
    var alarmLabel by remember { mutableStateOf("") }
    var repeatDays by remember { mutableStateOf(setOf<String>()) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var snoozeMinutes by remember { mutableStateOf(5) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isLoaded by remember { mutableStateOf(false) }
    
    // Strings para usar nas funções não-Composable
    val defaultAlarmLabel = stringResource(id = R.string.default_alarm_label)
    val alarmSavedSuccess = stringResource(id = R.string.alarm_saved_success)
    val errorSavingAlarm = stringResource(id = R.string.error_saving_alarm)
    val unexpectedError = stringResource(id = R.string.unexpected_error)
    val alarmEnabled = stringResource(id = R.string.alarm_enabled)
    val alarmDisabled = stringResource(id = R.string.alarm_disabled)
    val selectTime = stringResource(id = R.string.select_time)
    val errorLoadingConfig = stringResource(id = R.string.error_loading_config)
    val enableAlarmTitle = stringResource(id = R.string.enable_alarm_title)
    val repeatTitle = stringResource(id = R.string.repeat_title)
    val settingsTitle = stringResource(id = R.string.settings_title)
    val soundTitle = stringResource(id = R.string.sound_title)
    val vibrationTitle = stringResource(id = R.string.vibration_title)
    val snoozeMinutesTitle = stringResource(id = R.string.snooze_minutes_title)
    val minusButton = stringResource(id = R.string.minus_button)
    val plusButton = stringResource(id = R.string.plus_button)
    val thankYouMessage = stringResource(id = R.string.thank_you_message)
    val permissionGrantedMessage = stringResource(id = R.string.permission_granted_message)
    
    // ID do alarme baseado no alarme a editar, atividade ou gerado
    val alarmId = remember(alarmToEdit?.id, activityToEdit?.id) { 
        alarmToEdit?.id ?: activityToEdit?.id ?: "alarm_${System.currentTimeMillis()}"
    }
    
    // Repositórios e serviços
    val alarmRepository = remember { AlarmRepository(context) }
    val notificationService = remember { NotificationService(context) }
    val alarmService = remember { AlarmService(context, alarmRepository, notificationService) }
    
    // Carregar configurações existentes
    LaunchedEffect(alarmId, alarmToEdit) {
        try {
            if (alarmToEdit != null) {
                // Priorizar configurações do alarme a editar
                selectedTime = alarmToEdit.time
                isAlarmEnabled = alarmToEdit.isEnabled
                alarmLabel = alarmToEdit.label
                repeatDays = alarmToEdit.repeatDays
                soundEnabled = alarmToEdit.soundEnabled
                vibrationEnabled = alarmToEdit.vibrationEnabled
                snoozeMinutes = alarmToEdit.snoozeMinutes
                Log.d("AlarmScreen", "✅ Configurações do alarme a editar carregadas: $alarmToEdit")
            } else {
                val savedAlarm = alarmRepository.getAlarmById(alarmId)
                if (savedAlarm != null) {
                    // Carregar configurações salvas
                    selectedTime = savedAlarm.time
                    isAlarmEnabled = savedAlarm.isEnabled
                    alarmLabel = savedAlarm.label
                    repeatDays = savedAlarm.repeatDays
                    soundEnabled = savedAlarm.soundEnabled
                    vibrationEnabled = savedAlarm.vibrationEnabled
                    snoozeMinutes = savedAlarm.snoozeMinutes
                    Log.d("AlarmScreen", "✅ Configurações de alarme carregadas: $savedAlarm")
                } else if (activityToEdit != null) {
                    // Se não há alarme salvo, usar dados da atividade
                    selectedTime = activityToEdit.startTime ?: LocalTime.of(8, 0)
                    isAlarmEnabled = true
                    alarmLabel = activityToEdit.title
                    repeatDays = setOf()
                    soundEnabled = true
                    vibrationEnabled = true
                    snoozeMinutes = 5
                    Log.d("AlarmScreen", "📝 Usando dados da atividade: ${activityToEdit.title}")
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmScreen", "❌ Erro ao carregar configurações", e)
            errorMessage = "$errorLoadingConfig ${e.message ?: ""}"
        } finally {
            isLoaded = true
        }
    }
    
    // Função para mostrar seletor de horário nativo
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
        timePickerDialog.setTitle(selectTime)
        timePickerDialog.show()
    }
    
    // Função para salvar alarme
    suspend fun saveAlarm() {
        try {
            isLoading = true
            errorMessage = ""
            successMessage = ""
            
            val alarmSettings = AlarmSettings(
                id = alarmId,
                label = alarmLabel.ifBlank { defaultAlarmLabel },
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
                errorMessage = validation.message
                return
            }
            
            // Salvar no repositório
            val result = alarmRepository.saveAlarm(alarmSettings)
            if (result.isSuccess) {
                // Agendar ou cancelar alarme baseado no status
                if (isAlarmEnabled) {
                    Log.d("AlarmScreen", "🔔 Alarme habilitado - agendando no sistema")
                    alarmService.scheduleAlarm(alarmSettings)
                } else {
                    Log.d("AlarmScreen", "🔕 Alarme desabilitado - cancelando no sistema")
                    alarmService.cancelAlarm(alarmSettings.id)
                }
                successMessage = alarmSavedSuccess
                Log.d("AlarmScreen", "✅ Despertador salvo: $alarmSettings")
            } else {
                errorMessage = "$errorSavingAlarm ${result.exceptionOrNull()?.message ?: ""}"
            }
        } catch (e: Exception) {
            errorMessage = "$unexpectedError ${e.message ?: ""}"
            Log.e("AlarmScreen", "❌ Erro ao salvar despertador", e)
        } finally {
            isLoading = false
        }
    }
    
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when {
                            alarmToEdit != null -> stringResource(id = R.string.edit_alarm)
                            activityToEdit != null -> stringResource(id = R.string.edit_alarm)
                            else -> stringResource(id = R.string.configure_alarm)
                        }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_button))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Centralizar horário, ícone e status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Ícone do alarme
                    Icon(
                        imageVector = if (isAlarmEnabled) Icons.Default.AlarmOn else Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = if (isAlarmEnabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.outline
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Horário (clicável)
                    Text(
                        text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { showNativeTimePicker() }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Status
                    Text(
                        text = if (isAlarmEnabled) alarmEnabled else alarmDisabled,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isAlarmEnabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            
            // Card de permissão necessária
            if (!hasOverlayPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.permission_required_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.permission_required_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { 
                                    showPermissionDialog = true
                                }
                            ) {
                                Text(stringResource(id = R.string.configure_button_text))
                            }
                            TextButton(
                                onClick = { 
                                    hasOverlayPermission = true
                                    showThankYouMessage = true
                                }
                            ) {
                                Text(stringResource(id = R.string.done_button_text))
                            }
                        }
                    }
                }
            } else if (showThankYouMessage) {
                // Mensagem de agradecimento
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.thank_you_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(id = R.string.permission_granted_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Exibir mensagens de erro/sucesso
            if (errorMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            if (successMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = successMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Toggle para ativar/desativar
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = enableAlarmTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Switch(
                        checked = isAlarmEnabled,
                        onCheckedChange = { isAlarmEnabled = it }
                    )
                }
            }
            
            // Campo de texto para rótulo
            OutlinedTextField(
                value = alarmLabel,
                onValueChange = { alarmLabel = it },
                label = { Text(stringResource(id = R.string.alarm_label_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Dias da semana
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = repeatTitle,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Primeira linha: Dom, Seg, Ter, Qua
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val firstRowDays = listOf(
                            stringResource(id = R.string.day_sun),
                            stringResource(id = R.string.day_mon),
                            stringResource(id = R.string.day_tue),
                            stringResource(id = R.string.day_wed)
                        )
                        firstRowDays.forEach { day ->
                            FilterChip(
                                onClick = {
                                    repeatDays = if (day in repeatDays) {
                                        repeatDays - day
                                    } else {
                                        repeatDays + day
                                    }
                                },
                                label = { Text(day) },
                                selected = day in repeatDays,
                                modifier = Modifier.widthIn(min = 48.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Segunda linha: Qui, Sex, Sáb
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val secondRowDays = listOf(
                            stringResource(id = R.string.day_thu),
                            stringResource(id = R.string.day_fri),
                            stringResource(id = R.string.day_sat)
                        )
                        secondRowDays.forEach { day ->
                            FilterChip(
                                onClick = {
                                    repeatDays = if (day in repeatDays) {
                                        repeatDays - day
                                    } else {
                                        repeatDays + day
                                    }
                                },
                                label = { Text(day) },
                                selected = day in repeatDays,
                                modifier = Modifier.widthIn(min = 48.dp)
                            )
                        }
                    }
                }
            }
            
            // Configurações adicionais
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = settingsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Som
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(soundTitle)
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { soundEnabled = it }
                        )
                    }
                    
                    // Vibração
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(vibrationTitle)
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = { vibrationEnabled = it }
                        )
                    }
                    
                    // Soneca
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(snoozeMinutesTitle)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { if (snoozeMinutes > 1) snoozeMinutes-- }) {
                                Text(minusButton, fontSize = 20.sp)
                            }
                            Text(
                                text = snoozeMinutes.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.width(32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(onClick = { if (snoozeMinutes < 30) snoozeMinutes++ }) {
                                Text(plusButton, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
            
            // Botão salvar
            Button(
                onClick = {
                    coroutineScope.launch {
                        saveAlarm()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                        Text(if (isLoading) stringResource(id = R.string.saving_alarm) else stringResource(id = R.string.save_alarm))
            }
        }
    }
    
    // Diálogo de permissão
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(stringResource(id = R.string.permission_dialog_title))
            },
            text = {
                Text(stringResource(id = R.string.permission_dialog_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        // Abrir configurações de permissão
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("AlarmScreen", "Erro ao abrir configurações de permissão", e)
                            }
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.grant_permission_button_text))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text(stringResource(id = R.string.cancel_button_text))
                }
            }
        )
    }
}