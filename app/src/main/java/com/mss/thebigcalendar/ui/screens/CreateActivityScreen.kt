
package com.mss.thebigcalendar.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.NotificationType
import com.mss.thebigcalendar.data.model.VisibilityLevel
import com.mss.thebigcalendar.ui.components.NotificationSelector
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateActivityScreen(
    activityToEdit: Activity?,
    onDismissRequest: () -> Unit,
    onSaveActivity: (Activity, Boolean) -> Unit,
    isGoogleLoggedIn: Boolean
) {
    val currentActivity = activityToEdit ?: return

    var title by remember(currentActivity.id) { mutableStateOf(currentActivity.title) }
    var description by remember(currentActivity.id) { mutableStateOf(currentActivity.description ?: "") }
    var selectedPriority by remember(currentActivity.id) { mutableStateOf(currentActivity.categoryColor) }
    var selectedActivityType by remember(currentActivity.id) { mutableStateOf(currentActivity.activityType) }
    var selectedVisibility by remember(currentActivity.id) {
        mutableStateOf(
            if (currentActivity.id == "new" || currentActivity.id.isBlank()) {
                VisibilityLevel.LOW
            } else {
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

    var showInCalendar by remember(currentActivity.id) { mutableStateOf(currentActivity.showInCalendar) }

    var notificationSettings by remember(currentActivity.id) {
        mutableStateOf(
            if (currentActivity.id == "new" || currentActivity.id.isBlank()) {
                currentActivity.notificationSettings.copy(
                    isEnabled = true,
                    notificationType = NotificationType.FIFTEEN_MINUTES_BEFORE
                )
            } else {
                currentActivity.notificationSettings
            }
        )
    }

    var syncWithGoogle by remember(currentActivity.id) { 
        mutableStateOf(currentActivity.isFromGoogle) 
    }
    
    var showAlarmScreen by remember { mutableStateOf(false) }
    var isRepetitionMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember(currentActivity.id) { mutableStateOf(LocalDate.parse(currentActivity.date)) }
    var datePickerKey by remember { mutableStateOf(0) }

    val repetitionOptions = listOf(
        stringResource(id = R.string.repetition_dont_repeat),
        stringResource(id = R.string.repetition_every_day),
        stringResource(id = R.string.repetition_every_week),
        stringResource(id = R.string.repetition_every_month),
        stringResource(id = R.string.repetition_every_year)
    )

    val repetitionMapping = mapOf(
        repetitionOptions[0] to "",
        repetitionOptions[1] to "DAILY",
        repetitionOptions[2] to "WEEKLY",
        repetitionOptions[3] to "MONTHLY",
        repetitionOptions[4] to "YEARLY"
    )

    var selectedRepetition by remember(currentActivity.id) {
        mutableStateOf(
            if (currentActivity.id != "new" && !currentActivity.id.isBlank()) {
                val option = convertRecurrenceRuleToOption(currentActivity.recurrenceRule, repetitionMapping)
                option
            } else {
                repetitionOptions.first()
            }
        )
    }

    val formatter = DateTimeFormatter.ofPattern(stringResource(id = R.string.date_format_day_month), java.util.Locale("pt", "BR"))
    val formattedDate = selectedDate.format(formatter)
    
    // Debug: Log para verificar a data formatada
    LaunchedEffect(selectedDate) {
        println("Data atualizada: $selectedDate -> $formattedDate")
    }

    LaunchedEffect(currentActivity.id) {
        if (currentActivity.id == "new" || currentActivity.id.isBlank()) {
            focusRequester.requestFocus()
            val validPriorities = listOf("1", "2", "3", "4")
            if (selectedPriority !in validPriorities) {
                selectedPriority = validPriorities.first()
            }
            selectedVisibility = VisibilityLevel.LOW
            notificationSettings = notificationSettings.copy(
                isEnabled = true,
                notificationType = NotificationType.FIFTEEN_MINUTES_BEFORE
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = titleText)
                        Text(
                            text = selectedDate.format(formatter), // Usar selectedDate diretamente
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showDatePicker = true }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val updatedActivity = currentActivity.copy(
                                title = title.trim(),
                                description = description.trim().takeIf { it.isNotEmpty() },
                                date = selectedDate.toString(),
                                categoryColor = selectedPriority,
                                activityType = selectedActivityType,
                                startTime = if (hasScheduledTime) startTime else null,
                                endTime = if (hasScheduledTime) endTime else null,
                                isAllDay = !hasScheduledTime,
                                notificationSettings = notificationSettings,
                                visibility = selectedVisibility,
                                showInCalendar = showInCalendar,
                                recurrenceRule = convertRepetitionOptionToRule(selectedRepetition, repetitionMapping)
                            )
                            if (updatedActivity.title.isNotBlank()) {
                                onSaveActivity(updatedActivity, syncWithGoogle)
                                onDismissRequest()
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text(stringResource(id = R.string.create_activity_modal_save))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
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

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(id = R.string.create_activity_modal_description)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 3,
                maxLines = 5
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

            if (selectedActivityType != ActivityType.NOTE && selectedActivityType != ActivityType.BIRTHDAY) {
                PrioritySelector(
                    selectedPriority = selectedPriority,
                    onPrioritySelected = { selectedPriority = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (selectedActivityType != ActivityType.NOTE) {
                VisibilitySelector(
                    selectedVisibility = selectedVisibility,
                    onVisibilitySelected = { selectedVisibility = it },
                    isCustomized = selectedVisibility != VisibilityLevel.LOW
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

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

            CalendarVisibilitySelector(
                showInCalendar = showInCalendar,
                onShowInCalendarToggle = { showInCalendar = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isGoogleLoggedIn && selectedActivityType != ActivityType.NOTE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.sync_with_google),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Switch(
                        checked = syncWithGoogle,
                        onCheckedChange = { syncWithGoogle = it }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (selectedActivityType != ActivityType.NOTE) {
                NotificationSelector(
                    notificationSettings = notificationSettings,
                    onNotificationSettingsChanged = { notificationSettings = it },
                    isCustomized = notificationSettings.notificationType != NotificationType.FIFTEEN_MINUTES_BEFORE
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Opção de Despertador
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAlarmScreen = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.alarm_card_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(id = R.string.alarm_card_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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

        if (showTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = if (isPickingStartTime) startTime?.hour ?: 9 else endTime?.hour ?: 10,
                initialMinute = if (isPickingStartTime) startTime?.minute ?: 0 else endTime?.minute ?: 0,
                is24Hour = true
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                title = { Text(text = if (isPickingStartTime) stringResource(id = R.string.start_time) else stringResource(id = R.string.end_time)) },
                text = { TimePicker(state = timePickerState) },
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
    }
    
    // DatePicker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        
        Dialog(
            onDismissRequest = { showDatePicker = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.91f)
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier.wrapContentSize(),
                        colors = DatePickerDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showDatePicker = false }
                        ) {
                            Text(stringResource(id = R.string.create_activity_modal_cancel))
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    // Converter milissegundos para LocalDate usando UTC
                                    val instant = java.time.Instant.ofEpochMilli(millis)
                                    val utcDateTime = instant.atZone(java.time.ZoneOffset.UTC)
                                    val newDate = utcDateTime.toLocalDate()
                                    
                                    selectedDate = newDate
                                    datePickerKey++ // Força recomposição
                                    
                                    // Debug: Log para verificar se a data está sendo atualizada
                                    println("Data selecionada: $newDate")
                                    println("Milissegundos: $millis")
                                    println("Instant UTC: $instant")
                                    println("UTC DateTime: $utcDateTime")
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text(stringResource(id = R.string.ok))
                        }
                    }
                }
            }
        }
    }
    
    // Tela de Despertador
    if (showAlarmScreen) {
        val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        AlarmScreen(
            onBackClick = { showAlarmScreen = false },
            onBackPressedDispatcher = backPressedDispatcher,
            activityToEdit = currentActivity
        )
    }
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

@SuppressLint("DefaultLocale")
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
            Switch(
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
fun CalendarVisibilitySelector(
    showInCalendar: Boolean,
    onShowInCalendarToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.show_in_calendar),
                style = MaterialTheme.typography.labelMedium
            )
            Switch(
                checked = showInCalendar,
                onCheckedChange = onShowInCalendarToggle
            )
        }
        if (!showInCalendar) {
            Text(
                text = stringResource(id = R.string.show_in_calendar_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun VisibilitySelector(
    selectedVisibility: VisibilityLevel,
    onVisibilitySelected: (VisibilityLevel) -> Unit,
    isCustomized: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isVisibilityMenuExpanded by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingVisibilitySelection by remember { mutableStateOf<VisibilityLevel?>(null) }

    val viewModel = LocalViewModelStoreOwner.current?.let {
        ViewModelProvider(it)[CalendarViewModel::class.java]
    }
    val hasOverlayPermission = viewModel?.hasOverlayPermission() ?: false

    val visibilityOptions = listOf(
        VisibilityLevel.LOW,
        VisibilityLevel.MEDIUM,
        VisibilityLevel.HIGH
    )

    val getVisibilityDescription = @Composable { visibility: VisibilityLevel ->
        when (visibility) {
            VisibilityLevel.LOW -> stringResource(id = R.string.visibility_low_description)
            VisibilityLevel.MEDIUM -> stringResource(id = R.string.visibility_medium_description)
            VisibilityLevel.HIGH -> stringResource(id = R.string.visibility_high_description)
        }
    }

    val getVisibilityLabel = @Composable { visibility: VisibilityLevel ->
        when (visibility) {
            VisibilityLevel.LOW -> stringResource(id = R.string.visibility_low)
            VisibilityLevel.MEDIUM -> stringResource(id = R.string.visibility_medium)
            VisibilityLevel.HIGH -> stringResource(id = R.string.visibility_high)
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
                    contentDescription = stringResource(id = R.string.visibility_custom_configuration_content_description),
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
                            if (option == VisibilityLevel.LOW) {
                                onVisibilitySelected(option)
                                isVisibilityMenuExpanded = false
                            } else {
                                if (hasOverlayPermission) {
                                    onVisibilitySelected(option)
                                    isVisibilityMenuExpanded = false
                                } else {
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

    var shouldCheckPermission by remember { mutableStateOf(false) }

    LaunchedEffect(shouldCheckPermission) {
        if (shouldCheckPermission) {
            kotlinx.coroutines.delay(500)
            if (viewModel?.hasOverlayPermission() == true) {
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

private fun convertRepetitionOptionToRule(selectedOption: String, repetitionMapping: Map<String, String>): String {
    return repetitionMapping[selectedOption] ?: ""
}

private fun convertRecurrenceRuleToOption(recurrenceRule: String?, repetitionMapping: Map<String, String>): String {
    return when (recurrenceRule) {
        null, "", "NONE" -> repetitionMapping.entries.find { it.value == "" }?.key ?: repetitionMapping.keys.first()
        "DAILY" -> repetitionMapping.entries.find { it.value == "DAILY" }?.key ?: repetitionMapping.keys.first()
        "WEEKLY" -> repetitionMapping.entries.find { it.value == "WEEKLY" }?.key ?: repetitionMapping.keys.first()
        "MONTHLY" -> repetitionMapping.entries.find { it.value == "MONTHLY" }?.key ?: repetitionMapping.keys.first()
        "YEARLY" -> repetitionMapping.entries.find { it.value == "YEARLY" }?.key ?: repetitionMapping.keys.first()
        "CUSTOM" -> repetitionMapping.entries.find { it.value == "CUSTOM" }?.key ?: repetitionMapping.keys.first()
        else -> repetitionMapping.keys.first()
    }
}
