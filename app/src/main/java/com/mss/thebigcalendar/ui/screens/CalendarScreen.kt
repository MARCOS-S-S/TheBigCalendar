package com.mss.thebigcalendar.ui.screens

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
 
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
 
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
// import androidx.compose.ui.input.pointer APIs for low-level event if needed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.SearchResult
import com.mss.thebigcalendar.data.model.ViewMode
import com.mss.thebigcalendar.ui.components.BirthdaysForSelectedDaySection
import com.mss.thebigcalendar.ui.screens.CreateActivityScreen
import com.mss.thebigcalendar.ui.components.DeleteConfirmationDialog
import com.mss.thebigcalendar.ui.components.HolidaysForSelectedDaySection
import com.mss.thebigcalendar.ui.components.MonthlyCalendar
import com.mss.thebigcalendar.ui.components.MoonPhasesComponent
import com.mss.thebigcalendar.ui.components.SaintDaysForSelectedDaySection
import com.mss.thebigcalendar.ui.components.SaintInfoDialog
import com.mss.thebigcalendar.ui.components.Sidebar
import com.mss.thebigcalendar.ui.components.CustomDrawer
import com.mss.thebigcalendar.ui.components.DeleteJsonCalendarDialog
import com.mss.thebigcalendar.ui.components.TasksForSelectedDaySection
import com.mss.thebigcalendar.ui.components.NotesForSelectedDaySection
import com.mss.thebigcalendar.ui.components.JsonCalendarForSelectedDaySection
import com.mss.thebigcalendar.ui.components.JsonHolidayInfoDialog
import com.mss.thebigcalendar.ui.components.StoragePermissionDialog
import com.mss.thebigcalendar.ui.components.YearlyCalendarView
import com.mss.thebigcalendar.ui.screens.ChartScreen
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import java.time.YearMonth

fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier = composed {
    clickable(indication = null,
        interactionSource = remember { MutableInteractionSource() }) {
        onClick()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    // Sincronização bidirecional entre ViewModel e DrawerState
    LaunchedEffect(uiState.isSidebarOpen) {
        if (uiState.isSidebarOpen && !drawerState.isOpen) {
            drawerState.open()
        } else if (!uiState.isSidebarOpen && drawerState.isOpen) {
            drawerState.close()
        }
    }

    // Detecta quando o drawer é fechado por gesto e atualiza o ViewModel
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Closed && uiState.isSidebarOpen) {
            viewModel.closeSidebar()
        }
    }

    // Mostra mensagens de backup
    LaunchedEffect(uiState.backupMessage) {
        uiState.backupMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearBackupMessage()
        }
    }

    // Mostra mensagem de mudança de idioma
    LaunchedEffect(uiState.languageChangedMessage) {
        uiState.languageChangedMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearLanguageChangedMessage()
        }
    }

    val blurRadius = if (drawerState.targetValue == DrawerValue.Open || drawerState.isOpen) 8f else 0f
    


    if (uiState.activityToEdit != null) {
        CreateActivityScreen(
            activityToEdit = uiState.activityToEdit,
            onDismissRequest = { viewModel.closeCreateActivityModal() },
            onSaveActivity = { activity, syncWithGoogle ->
                viewModel.onSaveActivity(activity, syncWithGoogle)
            },
            isGoogleLoggedIn = uiState.googleSignInAccount != null
        )
    } else {
        CustomDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            scrimColor = Color.Black.copy(alpha = 0.7f),
            animationDuration = 350, // Animação de abertura mais rápida
            drawerContent = {
                Sidebar(
                    uiState = uiState,
                    onViewModeChange = { viewModel.onViewModeChange(it) },
                    onFilterChange = { key, value -> viewModel.onFilterChange(key, value) },
                    onNavigateToSettings = { viewModel.onNavigateToSettings(it) },
                    onBackup = { viewModel.onBackupIconClick() },
                    onNotesClick = { viewModel.onNotesClick() },
                    onAlarmsClick = { viewModel.onAlarmsClick() },
                    onRequestClose = {
                        scope.launch { drawerState.close() }
                        viewModel.closeSidebar()
                    },
                    onDeleteJsonCalendar = { jsonCalendar ->
                        viewModel.requestDeleteJsonCalendar(jsonCalendar)
                    },
                    onToggleSidebarFilterVisibility = { filterKey ->
                        viewModel.toggleSidebarFilterVisibility(filterKey)
                    }
                )
            }
        ) {
            val scaffoldModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.graphicsLayer(
                    renderEffect = if (blurRadius > 0) {
                        RenderEffect.createBlurEffect(
                            blurRadius,
                            blurRadius,
                            Shader.TileMode.DECAL
                        ).asComposeRenderEffect()
                    } else {
                        null
                    }
                )
            } else {
                Modifier
            }

            Scaffold(
                modifier = scaffoldModifier,
                topBar = {
                    TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    when (uiState.viewMode) {
                                        ViewMode.MONTHLY -> {
                                            val monthName = uiState.displayedYearMonth.month
                                                .getDisplayName(
                                                    java.time.format.TextStyle.FULL,
                                                    Locale.getDefault()
                                                )
                                                .replaceFirstChar {
                                                    it.titlecase(
                                                        Locale.getDefault()
                                                    )
                                                }
                                            Column(horizontalAlignment = Alignment.Start) {
                                                Text(
                                                    text = monthName,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    text = uiState.displayedYearMonth.year.toString(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        ViewMode.YEARLY -> {
                                            Text(
                                                text = uiState.displayedYearMonth.year.toString(),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                    
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    viewModel.openSidebar()
                                }) {
                                    Icon(
                                        Icons.Default.Menu,
                                        stringResource(id = R.string.open_close_menu)
                                    )
                                }
                            },
                            actions = {
                                val (prevAction, nextAction) = when (uiState.viewMode) {
                                    ViewMode.MONTHLY -> Pair(
                                        { viewModel.onPreviousMonth() },
                                        { viewModel.onNextMonth() })

                                    ViewMode.YEARLY -> Pair(
                                        { viewModel.onPreviousYear() },
                                        { viewModel.onNextYear() })
                                }

                                if (uiState.viewMode == ViewMode.MONTHLY) {
                                    IconButton(onClick = { viewModel.onSearchIconClick() }) {
                                        Icon(
                                            Icons.Default.Search,
                                            stringResource(id = R.string.search)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.onGoToToday() }) {
                                        Icon(
                                            Icons.Default.Today,
                                            contentDescription = stringResource(id = R.string.go_to_today)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.onChartIconClick() }) {
                                        Icon(
                                            Icons.Filled.BarChart,
                                            stringResource(id = R.string.chart)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.openCreateActivityModal(activityType = ActivityType.TASK) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = stringResource(id = R.string.add_appointment),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.onTrashIconClick() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(id = R.string.trash),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    IconButton(onClick = prevAction) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            stringResource(id = R.string.previous)
                                        )
                                    }
                                    IconButton(onClick = nextAction) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            stringResource(id = R.string.next)
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Conteúdo principal do calendário
                    MainCalendarView(
                                viewModel,
                                uiState,
                                scope,
                                drawerState,
                                snackbarHostState,
                                animationType = uiState.animationType
                            )
                    }

                    uiState.activityIdToDelete?.let { activityId ->
                        var activityToDelete = uiState.activities.find { it.id == activityId }
                        if (activityToDelete == null && activityId.contains("_")) {
                            val baseId = activityId.split("_").first()
                            activityToDelete = uiState.activities.find { it.id == baseId }
                        }
                        if (activityToDelete == null && activityId.contains("_")) {
                            val baseId = activityId.split("_").first()
                            activityToDelete = uiState.activities.find { 
                                it.id == baseId || 
                                (it.id.contains(baseId) && it.id.contains("_"))
                            }
                        }
                        
                        if (activityToDelete != null) {
                            DeleteConfirmationDialog(
                                activityTitle = activityToDelete.title,
                                onConfirm = { viewModel.onDeleteActivityConfirm() },
                                onDismiss = { viewModel.cancelDeleteActivity() }
                            )
                        }
                    }

                    uiState.saintInfoToShow?.let { saint ->
                        SaintInfoDialog(
                            saint = saint,
                            onDismiss = { viewModel.onSaintInfoDialogDismiss() }
                        )
                    }
                    
                    if (uiState.needsStoragePermission) {
                        StoragePermissionDialog(
                            onDismiss = { viewModel.clearBackupMessage() },
                            onPermissionGranted = {
                                viewModel.clearBackupMessage()
                                viewModel.onBackupRequest()
                            }
                        )
                    }

                    // Dialog de confirmação para deletar calendário JSON
                    if (uiState.showDeleteJsonCalendarDialog) {
                        DeleteJsonCalendarDialog(
                            jsonCalendar = uiState.jsonCalendarToDelete,
                            onDismissRequest = { viewModel.cancelDeleteJsonCalendar() },
                            onConfirmDelete = { viewModel.confirmDeleteJsonCalendar() }
                        )
                    }
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCalendarView(
    viewModel: CalendarViewModel,
    uiState: com.mss.thebigcalendar.data.model.CalendarUiState,
    scope: kotlinx.coroutines.CoroutineScope,
    drawerState: androidx.compose.material3.DrawerState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    animationType: com.mss.thebigcalendar.data.model.AnimationType = com.mss.thebigcalendar.data.model.AnimationType.NONE
) {
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
    
    // Estados de animação para o calendário mensal
    var isAnimating by remember { mutableStateOf(false) }
    var animationDirection by remember { mutableStateOf(0f) } // -1 = previous, 1 = next
    var previousYearMonth by remember { mutableStateOf<YearMonth?>(null) }
    
    // Detectar mudanças no yearMonth para iniciar animação
    LaunchedEffect(uiState.displayedYearMonth) {
        println("DEBUG: LaunchedEffect executado - yearMonth: ${uiState.displayedYearMonth}, previousYearMonth: $previousYearMonth")
        
        if (previousYearMonth != null && uiState.displayedYearMonth != previousYearMonth) {
            println("DEBUG: Mudança detectada!")
            
            // Determinar direção da animação baseada na mudança do mês
            val currentMonth = uiState.displayedYearMonth.monthValue
            val previousMonth = previousYearMonth!!.monthValue
            val currentYear = uiState.displayedYearMonth.year
            val previousYear = previousYearMonth!!.year
            
            // Calcular se está indo para frente ou para trás
            val isGoingForward = if (currentYear > previousYear) {
                true
            } else if (currentYear < previousYear) {
                false
            } else {
                currentMonth > previousMonth
            }
            
            animationDirection = if (isGoingForward) 1f else -1f
            isAnimating = true
            println("DEBUG: Animação iniciada - Direção: $animationDirection, Animando: $isAnimating")
            
            kotlinx.coroutines.delay(400) // Duração da animação
            isAnimating = false
            println("DEBUG: Animação finalizada - Animando: $isAnimating")
        } else {
            println("DEBUG: Nenhuma mudança detectada - Primeira vez ou mesmo mês")
        }
        
        // Sempre atualizar o previousYearMonth
        previousYearMonth = uiState.displayedYearMonth
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickableWithoutRipple { viewModel.hideDeleteButton() }
    ) {
        when (uiState.viewMode) {
            ViewMode.MONTHLY -> {
                val listState = rememberLazyListState()
                var isZooming by remember { mutableStateOf(false) }
                var calendarScale by remember { mutableFloatStateOf(uiState.calendarScale) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().clickableWithoutRipple { viewModel.hideDeleteButton() },
                    state = listState,
                    userScrollEnabled = !isZooming
                ) {
                    item(
                        key = "calendar-${uiState.displayedYearMonth}",
                        contentType = "calendarHeader"
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 16.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.medium
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                AnimatedMonthlyCalendar(
                                    calendarDays = uiState.calendarDays,
                                    onDateSelected = { viewModel.onDateSelected(it) },
                                    theme = uiState.theme,
                                    yearMonth = uiState.displayedYearMonth,
                                    onPreviousMonth = { viewModel.onPreviousMonth() },
                                    onNextMonth = { viewModel.onNextMonth() },
                                    isAnimating = isAnimating,
                                    animationDirection = animationDirection,
                                    animationType = animationType,
                                    verticalScale = calendarScale,
                                    hideOtherMonthDays = uiState.hideOtherMonthDays
                                )

                                if (uiState.showMoonPhases) {
                                    MoonPhasesComponent(
                                        yearMonth = uiState.displayedYearMonth,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .size(20.dp)
                                    .pointerInput("drag-resize-handle") {
                                        detectVerticalDragGestures(
                                            onDragStart = { isZooming = true },
                                            onVerticalDrag = { _, dragAmount ->
                                                // Arrastar para baixo aumenta, para cima diminui
                                                val delta = (dragAmount) / 400f
                                                val newScale = (calendarScale + delta).coerceIn(0.5f, 1.22f)
                                                if (newScale != calendarScale) {
                                                    calendarScale = newScale
                                                    viewModel.setCalendarScale(newScale)
                                                }
                                            },
                                            onDragEnd = { isZooming = false },
                                            onDragCancel = { isZooming = false }
                                        )
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInFull,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (uiState.holidaysForSelectedDate.isNotEmpty()) {
                        item(
                            key = "holidays-${uiState.selectedDate}",
                            contentType = "holidays"
                        ) {
                            HolidaysForSelectedDaySection(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                holidays = uiState.holidaysForSelectedDate
                            )
                        }
                    }

                    if (uiState.saintDaysForSelectedDate.isNotEmpty()) {
                        item(
                            key = "saints-${uiState.selectedDate}",
                            contentType = "saints"
                        ) {
                            SaintDaysForSelectedDaySection(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                saints = uiState.saintDaysForSelectedDate,
                                onSaintClick = { viewModel.onSaintDayClick(it) }
                            )
                        }
                    }
                    
                    item(
                        key = "birthdays-${uiState.selectedDate}",
                        contentType = "birthdays"
                    ) {
                        BirthdaysForSelectedDaySection(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            birthdays = uiState.birthdaysForSelectedDate,
                            selectedDate = uiState.selectedDate,
                            activityIdWithDeleteVisible = uiState.activityIdWithDeleteButtonVisible,
                            onBirthdayClick = {
                                if (uiState.activityIdWithDeleteButtonVisible != null) {
                                    viewModel.hideDeleteButton()
                                } else {
                                    viewModel.openCreateActivityModal(it, it.activityType)
                                }
                            },
                            onBirthdayLongClick = { viewModel.onTaskLongPressed(it) },
                            onDeleteClick = { viewModel.requestDeleteActivity(it) },
                            onCompleteClick = { viewModel.markActivityAsCompleted(it) },
                            onAddBirthdayClick = { viewModel.openCreateActivityModal(activityType = ActivityType.BIRTHDAY) }
                        )
                    }
                    
                    item(
                        key = "notes-${uiState.selectedDate}",
                        contentType = "notes"
                    ) {
                        NotesForSelectedDaySection(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            notes = uiState.notesForSelectedDate,
                            selectedDate = uiState.selectedDate,
                            activityIdWithDeleteVisible = uiState.activityIdWithDeleteButtonVisible,
                            onNoteClick = {
                                if (uiState.activityIdWithDeleteButtonVisible != null) {
                                    viewModel.hideDeleteButton()
                                } else {
                                    viewModel.openCreateActivityModal(it, it.activityType)
                                }
                            },
                            onNoteLongClick = { viewModel.onTaskLongPressed(it) },
                            onDeleteClick = { viewModel.requestDeleteActivity(it) },
                            onCompleteClick = { viewModel.markActivityAsCompleted(it) },
                            onAddNoteClick = { viewModel.openCreateActivityModal(activityType = ActivityType.NOTE) }
                        )
                    }
                    
                    item(
                        key = "tasks-${uiState.selectedDate}",
                        contentType = "tasks"
                    ) {
                        TasksForSelectedDaySection(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            tasks = uiState.tasksForSelectedDate,
                            selectedDate = uiState.selectedDate,
                            activityIdWithDeleteVisible = uiState.activityIdWithDeleteButtonVisible,
                            onTaskClick = {
                                if (uiState.activityIdWithDeleteButtonVisible != null) {
                                    viewModel.hideDeleteButton()
                                } else {
                                    viewModel.openCreateActivityModal(it, it.activityType)
                                }
                            },
                            onTaskLongClick = { viewModel.onTaskLongPressed(it) },
                            onDeleteClick = { viewModel.requestDeleteActivity(it) },
                            onCompleteClick = { viewModel.markActivityAsCompleted(it) },
                            onAddTaskClick = { viewModel.openCreateActivityModal(activityType = ActivityType.TASK) }
                        )
                    }
                    
                    // Seções dos calendários JSON importados
                    uiState.jsonCalendarActivitiesForSelectedDate.forEach { (calendarId, activities) ->
                        val jsonCalendar = uiState.jsonCalendars.find { it.id == calendarId }
                        if (jsonCalendar != null && activities.isNotEmpty()) {
                            item(
                                key = "json-calendar-${calendarId}-${uiState.selectedDate}",
                                contentType = "json-calendar"
                            ) {
                                JsonCalendarForSelectedDaySection(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    jsonCalendar = jsonCalendar,
                                    activities = activities,
                                    onActivityClick = { activity ->
                                        // Para atividades JSON, mostrar informações do agendamento
                                        // Converter Activity para JsonHoliday para mostrar no diálogo
                                        
                                        val jsonHoliday = com.mss.thebigcalendar.data.model.JsonHoliday(
                                            id = activity.id,
                                            name = activity.title,
                                            date = activity.date.substring(5, 10), // MM-dd
                                            summary = activity.description,
                                            wikipediaLink = activity.wikipediaLink, // Agora temos essa informação na Activity
                                            calendarId = jsonCalendar.id,
                                            calendarTitle = jsonCalendar.title,
                                            calendarColor = jsonCalendar.color
                                        )
                                        
                                        viewModel.showJsonHolidayInfo(jsonHoliday)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            ViewMode.YEARLY -> {
                YearlyCalendarView(
                    modifier = Modifier.fillMaxSize(),
                    year = uiState.displayedYearMonth.year,
                    onMonthClicked = { viewModel.onYearlyMonthClicked(it) },
                    onNavigateYear = { delta ->
                        if (delta > 0) viewModel.onNextYear() else viewModel.onPreviousYear()
                    }
                )
            }
        }
        
        // Dialog de informações do santo
        uiState.saintInfoToShow?.let { saint ->
            SaintInfoDialog(
                saint = saint,
                onDismiss = { viewModel.onSaintInfoDialogDismiss() }
            )
        }
        
        // Dialog de informações do agendamento JSON
        uiState.jsonHolidayInfoToShow?.let { jsonHoliday ->
            JsonHolidayInfoDialog(
                jsonHoliday = jsonHoliday,
                onDismiss = { viewModel.hideJsonHolidayInfo() }
            )
        }
    }
}

@Composable
fun AnimatedMonthlyCalendar(
    modifier: Modifier = Modifier,
    calendarDays: List<com.mss.thebigcalendar.data.model.CalendarDay>,
    onDateSelected: (java.time.LocalDate) -> Unit,
    theme: com.mss.thebigcalendar.data.model.Theme,
    yearMonth: java.time.YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    isAnimating: Boolean,
    animationDirection: Float,
    animationType: com.mss.thebigcalendar.data.model.AnimationType = com.mss.thebigcalendar.data.model.AnimationType.NONE,
    verticalScale: Float = 1f,
    hideOtherMonthDays: Boolean = false
) {
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    // Pinch-to-zoom removido. Escala vem do pai via verticalScale.
    
    // Usar o sistema de animações modular
    val animationValues = com.mss.thebigcalendar.ui.animations.CalendarAnimationState(
        isAnimating = isAnimating,
        animationDirection = animationDirection,
        animationType = animationType
    )
    
    
    Column(
        modifier = modifier
            .graphicsLayer {
                this.translationX = animationValues.translationX
                this.translationY = animationValues.translationY
                this.scaleX = animationValues.scaleX
                this.scaleY = animationValues.scaleY
                this.transformOrigin = TransformOrigin(0.5f, 0f)
                this.rotationX = animationValues.rotationX
                this.rotationY = animationValues.rotationY
                this.rotationZ = animationValues.rotationZ
                this.alpha = animationValues.alpha
                this.shadowElevation = animationValues.shadowElevation
                if (isAnimating) {
                    println("DEBUG: Aplicando animação ${animationType.name} - scale: ${animationValues.scaleX}, alpha: ${animationValues.alpha}")
                }
            }
            // nenhum manipulador de pinça aqui
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        horizontalDragOffset += dragAmount
                    },
                    onDragEnd = {
                        // Usar a lógica de swipe baseada no offset acumulado
                        val swipeThreshold = 100f
                        if (horizontalDragOffset > swipeThreshold) {
                            // Swipe para a direita = mês anterior
                            onPreviousMonth()
                        } else if (horizontalDragOffset < -swipeThreshold) {
                            // Swipe para a esquerda = próximo mês
                            onNextMonth()
                        }
                        horizontalDragOffset = 0f
                    }
                )
            }
    ) {
        MonthlyCalendar(
            calendarDays = calendarDays,
            onDateSelected = onDateSelected,
            theme = theme,
            verticalScale = verticalScale,
            hideOtherMonthDays = hideOtherMonthDays
        )
    }
}