package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.* // Importa tudo de layout
import androidx.compose.foundation.lazy.LazyRow // Para a lista de cores horizontal
import androidx.compose.foundation.lazy.items // Para a lista de cores horizontal
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.* // Importa tudo de runtime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonDefaults
import com.mss.thebigcalendar.R
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.NotificationSettings
import com.mss.thebigcalendar.data.model.NotificationType
import com.mss.thebigcalendar.data.model.VisibilityLevel
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateActivityModal(
    activityToEdit: Activity?,
    onDismissRequest: () -> Unit,
    onSaveActivity: (Activity) -> Unit
) {
    val currentActivity = activityToEdit ?: return

    var title by remember(currentActivity.id) { mutableStateOf(currentActivity.title) }
    var selectedPriority by remember(currentActivity.id) { mutableStateOf(currentActivity.categoryColor) }
    var selectedActivityType by remember(currentActivity.id) { mutableStateOf(currentActivity.activityType) }
    var selectedVisibility by remember(currentActivity.id) { 
        mutableStateOf(
            if (currentActivity.id == "new" || currentActivity.id.isBlank()) {
                // Para novas atividades, usar visibilidade baixa por padrão
                com.mss.thebigcalendar.data.model.VisibilityLevel.LOW
            } else {
                // Para atividades existentes, usar a visibilidade salva
                currentActivity.visibility
            }
        ) 
    }

    val focusRequester = remember { FocusRequester() }
    var startTime by remember(currentActivity.id) { mutableStateOf(currentActivity.startTime) }
    var endTime by remember(currentActivity.id) { mutableStateOf(currentActivity.endTime) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isPickingStartTime by remember { mutableStateOf(true) }
    var hasScheduledTime by remember(currentActivity.id) { mutableStateOf(currentActivity.startTime != null) }
    
    // Estado para configurações de notificação
    var notificationSettings by remember(currentActivity.id) { 
        mutableStateOf(
            if (currentActivity.id == "new" || currentActivity.id.isBlank()) {
                // ✅ Para novas atividades, usar configurações padrão
                currentActivity.notificationSettings.copy(
                    isEnabled = true,
                    notificationType = com.mss.thebigcalendar.data.model.NotificationType.FIFTEEN_MINUTES_BEFORE
                )
            } else {
                // ✅ Para atividades existentes, usar as configurações salvas
                currentActivity.notificationSettings
            }
        ) 
    }
    
    // Estado para repetições
    var isRepetitionMenuExpanded by remember { mutableStateOf(false) }
    val repetitionOptions = listOf(
        stringResource(id = R.string.repetition_dont_repeat),
        stringResource(id = R.string.repetition_every_day),
        stringResource(id = R.string.repetition_every_week),
        stringResource(id = R.string.repetition_every_month),
        stringResource(id = R.string.repetition_every_year),
        stringResource(id = R.string.repetition_custom)
    )
    var selectedRepetition by remember { mutableStateOf(repetitionOptions.first()) }

    val date = LocalDate.parse(currentActivity.date)
    val formatter = DateTimeFormatter.ofPattern(stringResource(id = R.string.date_format_day_month), java.util.Locale("pt", "BR"))
    val formattedDate = date.format(formatter)

    LaunchedEffect(currentActivity.id) {
        if (currentActivity.id == "new" || currentActivity.id.isBlank()) {
            focusRequester.requestFocus()

            // Configurar prioridade padrão
            val validPriorities = listOf("1", "2", "3", "4")
            if (selectedPriority !in validPriorities) {
                selectedPriority = validPriorities.first()
            }
            
            // Configurar visibilidade padrão
            selectedVisibility = com.mss.thebigcalendar.data.model.VisibilityLevel.LOW
            
            // Configurar notificações padrão
            notificationSettings = notificationSettings.copy(
                isEnabled = true,
                notificationType = com.mss.thebigcalendar.data.model.NotificationType.FIFTEEN_MINUTES_BEFORE
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            val titleText = when {
                currentActivity.id == "new" || currentActivity.id.isBlank() -> {
                    when (selectedActivityType) {
                        ActivityType.TASK -> stringResource(id = R.string.create_activity_modal_new_task)
                        ActivityType.EVENT -> stringResource(id = R.string.create_activity_modal_new_event)
                        ActivityType.NOTE -> stringResource(id = R.string.create_activity_modal_new_note)
                        ActivityType.BIRTHDAY -> stringResource(id = R.string.create_activity_modal_new_birthday)
                    }
                }
                else -> {
                    when (selectedActivityType) {
                        ActivityType.TASK -> stringResource(id = R.string.create_activity_modal_edit_task)
                        ActivityType.EVENT -> stringResource(id = R.string.create_activity_modal_edit_event)
                        ActivityType.NOTE -> stringResource(id = R.string.create_activity_modal_edit_note)
                        ActivityType.BIRTHDAY -> stringResource(id = R.string.create_activity_modal_edit_birthday)
                    }
                }
            }
            Text(text = "$titleText $formattedDate")
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(id = R.string.create_activity_modal_title)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(ActivityType.TASK, ActivityType.EVENT, ActivityType.NOTE, ActivityType.BIRTHDAY).forEach { type ->
                        val isSelected = selectedActivityType == type
                        TextButton(
                            onClick = { selectedActivityType = type },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            val text = when (type) {
                                ActivityType.TASK -> stringResource(id = R.string.task)
                                ActivityType.EVENT -> stringResource(id = R.string.event)
                                ActivityType.NOTE -> stringResource(id = R.string.note)
                                ActivityType.BIRTHDAY -> stringResource(id = R.string.birthday)
                            }
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                PrioritySelector(
                    selectedPriority = selectedPriority,
                    onPrioritySelected = { selectedPriority = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                VisibilitySelector(
                    selectedVisibility = selectedVisibility,
                    onVisibilitySelected = { selectedVisibility = it },
                    isCustomized = selectedVisibility != com.mss.thebigcalendar.data.model.VisibilityLevel.LOW
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimeSelector(
                    hasScheduledTime = hasScheduledTime,
                    startTime = startTime,
                    endTime = endTime,
                    onScheduleToggle = { hasScheduledTime = it },
                    onTimeClick = { isStart ->
                        isPickingStartTime = isStart
                        showTimePicker = true
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Seletor de notificações (visível apenas quando há horário agendado)
                if (hasScheduledTime) {
                    NotificationSelector(
                        notificationSettings = notificationSettings,
                        onNotificationSettingsChanged = { notificationSettings = it },
                        isCustomized = notificationSettings.notificationType != com.mss.thebigcalendar.data.model.NotificationType.FIFTEEN_MINUTES_BEFORE
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRepetitionMenuExpanded = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(id = R.string.repeat),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedRepetition,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedRepetition != stringResource(id = R.string.repetition_dont_repeat)) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            }
                        )
                    }
                    DropdownMenu(
                        expanded = isRepetitionMenuExpanded,
                        onDismissRequest = { isRepetitionMenuExpanded = false }
                    ) {
                        repetitionOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = option, 
                                        color = if (option == selectedRepetition) MaterialTheme.colorScheme.primary else Color.Unspecified
                                    )
                                },
                                onClick = {
                                    selectedRepetition = option
                                    isRepetitionMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // TimePicker Modal
            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(
                    initialHour = if (isPickingStartTime) startTime?.hour ?: 9 else endTime?.hour ?: 10,
                    initialMinute = if (isPickingStartTime) startTime?.minute ?: 0 else endTime?.minute ?: 0,
                    is24Hour = true
                )
                
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    title = {
                        Text(text = if (isPickingStartTime) stringResource(id = R.string.start_time) else stringResource(id = R.string.end_time))
                    },
                    text = {
                        TimePicker(state = timePickerState)
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val selectedTime = java.time.LocalTime.of(timePickerState.hour, timePickerState.minute)
                                if (isPickingStartTime) {
                                    startTime = selectedTime
                                } else {
                                    endTime = selectedTime
                                }
                                showTimePicker = false
                            }
                        ) {
                            Text(stringResource(id = R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(id = R.string.create_activity_modal_cancel))
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedActivity = currentActivity.copy(
                        title = title.trim(),
                        categoryColor = selectedPriority,
                        activityType = selectedActivityType,
                        startTime = if (hasScheduledTime) startTime else null,
                        endTime = if (hasScheduledTime) endTime else null,
                        isAllDay = !hasScheduledTime,
                        notificationSettings = notificationSettings, // ✅ Adicionar configurações de notificação
                        visibility = selectedVisibility, // ✅ Adicionar visibilidade
                        recurrenceRule = convertRepetitionOptionToRule(selectedRepetition, repetitionOptions) // ✅ Adicionar regra de repetição
                    )
                    if (updatedActivity.title.isNotBlank()) {
                        onSaveActivity(updatedActivity)
                        onDismissRequest()
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(stringResource(id = R.string.create_activity_modal_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.create_activity_modal_cancel))
            }
        }
    )
}

@Composable
fun PrioritySelector(
    selectedPriority: String,
    onPrioritySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val priorities = mapOf(
        "1" to Color.White,
        "2" to Color.Blue,
        "3" to Color.Yellow,
        "4" to Color.Red
    )

    Column(modifier = modifier) {
        Text(stringResource(id = R.string.priority), style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            priorities.forEach { (priority, color) ->
                val isSelected = selectedPriority == priority
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onPrioritySelected(priority) },
                    shape = CircleShape,
                    color = color,
                    border = if (isSelected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                    } else if (color == Color.White) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    } else {
                        null
                    }
                ) {}
            }
        }
    }
}

@Composable
fun TimeSelector(
    hasScheduledTime: Boolean,
    startTime: java.time.LocalTime?,
    endTime: java.time.LocalTime?,
    onScheduleToggle: (Boolean) -> Unit,
    onTimeClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.time),
                style = MaterialTheme.typography.labelMedium
            )
            androidx.compose.material3.Switch(
                checked = hasScheduledTime,
                onCheckedChange = onScheduleToggle
            )
        }
        
        if (hasScheduledTime) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botão de horário de início
                Button(
                    onClick = { onTimeClick(true) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = stringResource(id = R.string.start_time),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (startTime != null) {
                            String.format("%02d:%02d", startTime.hour, startTime.minute)
                        } else {
                            stringResource(id = R.string.start)
                        }
                    )
                }
                
                // Botão de horário de fim
                Button(
                    onClick = { onTimeClick(false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = stringResource(id = R.string.end_time),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (endTime != null) {
                            String.format("%02d:%02d", endTime.hour, endTime.minute)
                        } else {
                            stringResource(id = R.string.end)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VisibilitySelector(
    selectedVisibility: com.mss.thebigcalendar.data.model.VisibilityLevel,
    onVisibilitySelected: (com.mss.thebigcalendar.data.model.VisibilityLevel) -> Unit,
    isCustomized: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isVisibilityMenuExpanded by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingVisibilitySelection by remember { mutableStateOf<com.mss.thebigcalendar.data.model.VisibilityLevel?>(null) }
    
    // Verificar permissão quando o componente é composto
    val viewModel = LocalViewModelStoreOwner.current?.let { 
        ViewModelProvider(it)[CalendarViewModel::class.java] 
    }
    val hasOverlayPermission = viewModel?.hasOverlayPermission() ?: false
    
    val visibilityOptions = listOf(
        com.mss.thebigcalendar.data.model.VisibilityLevel.LOW,
        com.mss.thebigcalendar.data.model.VisibilityLevel.MEDIUM,
        com.mss.thebigcalendar.data.model.VisibilityLevel.HIGH
    )
    
    val getVisibilityDescription = @androidx.compose.runtime.Composable { visibility: com.mss.thebigcalendar.data.model.VisibilityLevel ->
        when (visibility) {
            com.mss.thebigcalendar.data.model.VisibilityLevel.LOW -> stringResource(id = R.string.visibility_low_description)
            com.mss.thebigcalendar.data.model.VisibilityLevel.MEDIUM -> stringResource(id = R.string.visibility_medium_description)
            com.mss.thebigcalendar.data.model.VisibilityLevel.HIGH -> stringResource(id = R.string.visibility_high_description)
        }
    }
    
    val getVisibilityLabel = @androidx.compose.runtime.Composable { visibility: com.mss.thebigcalendar.data.model.VisibilityLevel ->
        when (visibility) {
            com.mss.thebigcalendar.data.model.VisibilityLevel.LOW -> stringResource(id = R.string.visibility_low)
            com.mss.thebigcalendar.data.model.VisibilityLevel.MEDIUM -> stringResource(id = R.string.visibility_medium)
            com.mss.thebigcalendar.data.model.VisibilityLevel.HIGH -> stringResource(id = R.string.visibility_high)
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(id = R.string.visibility), style = MaterialTheme.typography.labelMedium)
            if (isCustomized) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Configuração personalizada",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isVisibilityMenuExpanded = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = getVisibilityLabel(selectedVisibility),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = getVisibilityDescription(selectedVisibility),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            DropdownMenu(
                expanded = isVisibilityMenuExpanded,
                onDismissRequest = { isVisibilityMenuExpanded = false }
            ) {
                visibilityOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = getVisibilityLabel(option), 
                                color = if (option == selectedVisibility) MaterialTheme.colorScheme.primary else Color.Unspecified
                            )
                        },
                        onClick = {
                            if (option == com.mss.thebigcalendar.data.model.VisibilityLevel.LOW) {
                                // Visibilidade baixa não precisa de permissão
                                onVisibilitySelected(option)
                                isVisibilityMenuExpanded = false
                            } else {
                                // Verificar se já tem permissão para visibilidade média/alta
                                if (hasOverlayPermission) {
                                    // Já tem permissão, pode selecionar diretamente
                                    onVisibilitySelected(option)
                                    isVisibilityMenuExpanded = false
                                } else {
                                    // Precisa de permissão, mostrar diálogo
                                    pendingVisibilitySelection = option
                                    showPermissionDialog = true
                                    isVisibilityMenuExpanded = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Diálogo de solicitação de permissão
    if (showPermissionDialog) {
        OverlayPermissionDialog(
            onConfirm = {
                showPermissionDialog = false
                pendingVisibilitySelection?.let { visibility ->
                    onVisibilitySelected(visibility)
                    pendingVisibilitySelection = null
                }
            },
            onDismiss = {
                showPermissionDialog = false
                pendingVisibilitySelection = null
            }
        )
    }
}

@Composable
fun OverlayPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = LocalViewModelStoreOwner.current?.let { 
        ViewModelProvider(it)[CalendarViewModel::class.java] 
    }
    
    // Estado para controlar se deve verificar a permissão
    var shouldCheckPermission by remember { mutableStateOf(false) }
    
    // Verificar permissão quando o usuário retornar das configurações
    LaunchedEffect(shouldCheckPermission) {
        if (shouldCheckPermission) {
            // Aguardar um pouco para o usuário voltar das configurações
            kotlinx.coroutines.delay(500)
            
            // Verificar se a permissão foi concedida
            if (viewModel?.hasOverlayPermission() == true) {
                // Permissão concedida, fechar diálogo e aplicar seleção
                onConfirm()
            }
            shouldCheckPermission = false
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.overlay_permission_title)) },
        text = { 
            Text(
                stringResource(R.string.overlay_permission_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Solicitar permissão de sobreposição
                    viewModel?.let { vm ->
                        val intent = vm.requestOverlayPermission()
                        context.startActivity(intent)
                        shouldCheckPermission = true
                    }
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.overlay_permission_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

//@Composable
//private fun ColorSwatch(
//    color: Color,
//    isSelected: Boolean,
//    onClick: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Surface( // Usando Surface para aplicar borda condicionalmente
//        modifier = modifier
//            .size(32.dp)
//            .clip(CircleShape)
//            .clickable(onClick = onClick),
//        shape = CircleShape,
//        color = color,
//        border = if (isSelected) {
//            BorderStroke(2.dp, MaterialTheme.colorScheme.outline) // Destaque para cor selecionada
//        } else {
//            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
//        }
//    ) {
//        // O conteúdo do Box é opcional, o Surface já tem a cor.
//        // Box(modifier = Modifier.fillMaxSize())
//    }
//}

/**
 * Função auxiliar para converter opções de repetição em regras de repetição
 */
private fun convertRepetitionOptionToRule(selectedOption: String, allOptions: List<String>): String {
    return when (selectedOption) {
        allOptions[0] -> "" // "Não repetir"
        allOptions[1] -> "DAILY" // "Todos os dias"
        allOptions[2] -> "WEEKLY" // "Todas as semanas"
        allOptions[3] -> "MONTHLY" // "Todos os meses"
        allOptions[4] -> "YEARLY" // "Todos os anos"
        allOptions[5] -> "CUSTOM" // "Personalizado"
        else -> ""
    }
}