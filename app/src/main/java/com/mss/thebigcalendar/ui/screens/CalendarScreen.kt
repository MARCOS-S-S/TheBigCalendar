package com.mss.thebigcalendar.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.SearchResult
import com.mss.thebigcalendar.data.model.ViewMode
import com.mss.thebigcalendar.ui.components.BirthdaysForSelectedDaySection
import com.mss.thebigcalendar.ui.components.CreateActivityModal
import com.mss.thebigcalendar.ui.components.DeleteConfirmationDialog
import com.mss.thebigcalendar.ui.components.HolidaysForSelectedDaySection
import com.mss.thebigcalendar.ui.components.MonthlyCalendar
import com.mss.thebigcalendar.ui.components.SaintDaysForSelectedDaySection
import com.mss.thebigcalendar.ui.components.SaintInfoDialog
import com.mss.thebigcalendar.ui.components.Sidebar
import com.mss.thebigcalendar.ui.components.TasksForSelectedDaySection
import com.mss.thebigcalendar.ui.components.NotesForSelectedDaySection
import com.mss.thebigcalendar.ui.components.StoragePermissionDialog
import com.mss.thebigcalendar.ui.components.YearlyCalendarView
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel
import kotlinx.coroutines.launch
import java.util.Locale

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


    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = uiState.currentSettingsScreen == null,
            drawerContent = {
                // Renderização condicional para melhor performance
                if (drawerState.targetValue == DrawerValue.Open || drawerState.isOpen) {
                                    Sidebar(
                    uiState = uiState,
                    onViewModeChange = { viewModel.onViewModeChange(it) },
                    onFilterChange = { key, value -> viewModel.onFilterChange(key, value) },
                    onNavigateToSettings = { viewModel.onNavigateToSettings(it) },
                    onBackup = { viewModel.onBackupIconClick() },
                    onRequestClose = { 
                        scope.launch { drawerState.close() }
                        viewModel.closeSidebar() 
                    }
                )
                }
            }
        ) {
            when (uiState.currentSettingsScreen) {
                "General" -> {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(stringResource(id = R.string.general)) },
                                navigationIcon = {
                                    IconButton(onClick = { viewModel.onNavigateToSettings(null) }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back))
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->
                        Column(
                            modifier = Modifier
                                .padding(paddingValues)
                                .fillMaxSize()
                        ) {
                            GeneralSettingsScreen(
                                currentTheme = uiState.theme,
                                onThemeChange = { viewModel.onThemeChange(it) },
                                googleAccount = uiState.googleSignInAccount,
                                onSignInClicked = { viewModel.onSignInClicked() },
                                onSignOutClicked = { viewModel.signOut() }
                            )
                        }
                    }
                }
                else -> {
                    MainCalendarView(viewModel, uiState, scope, drawerState, snackbarHostState)
                }
            }
        }
        
        // Snackbar por cima de tudo
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCalendarView(
    viewModel: CalendarViewModel,
    uiState: com.mss.thebigcalendar.data.model.CalendarUiState,
    scope: kotlinx.coroutines.CoroutineScope,
    drawerState: androidx.compose.material3.DrawerState,
    snackbarHostState: SnackbarHostState
) {
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
    Scaffold(
        modifier = Modifier.clickableWithoutRipple { viewModel.hideDeleteButton() },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val text = when (uiState.viewMode) {
                            ViewMode.MONTHLY -> {
                                val monthName = uiState.displayedYearMonth.month
                                    .getDisplayName(java.time.format.TextStyle.FULL, Locale("pt", "BR"))
                                    .replaceFirstChar { it.titlecase(Locale("pt", "BR")) }
                                stringResource(id = R.string.month_year_format, monthName, uiState.displayedYearMonth.year)
                            }
                            ViewMode.YEARLY -> uiState.displayedYearMonth.year.toString()
                        }
                        Text(text)
                        if (uiState.viewMode == ViewMode.MONTHLY) {
                            IconButton(onClick = { viewModel.onGoToToday() }) {
                                Icon(Icons.Default.Today, contentDescription = stringResource(id = R.string.go_to_today))
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        viewModel.openSidebar()
                    }) {
                        Icon(Icons.Default.Menu, stringResource(id = R.string.open_close_menu))
                    }
                },
                actions = {
                    val (prevAction, nextAction) = when (uiState.viewMode) {
                        ViewMode.MONTHLY -> Pair({ viewModel.onPreviousMonth() }, { viewModel.onNextMonth() })
                        ViewMode.YEARLY -> Pair({ viewModel.onPreviousYear() }, { viewModel.onNextYear() })
                    }
                    
                    // Botão de pesquisa (apenas na visualização mensal)
                    if (uiState.viewMode == ViewMode.MONTHLY) {
                        IconButton(onClick = { viewModel.onSearchIconClick() }) {
                            Icon(Icons.Default.Search, stringResource(id = R.string.search))
                        }
                        
                        // Botão de criar agendamento (menor)
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
                        
                        // Botão da lixeira (menor)
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
                        // Para visualização anual, manter as setas de navegação
                        IconButton(onClick = prevAction) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.previous))
                        }
                        IconButton(onClick = nextAction) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(id = R.string.next))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },

            ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (uiState.viewMode) {
                ViewMode.MONTHLY -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().clickableWithoutRipple { viewModel.hideDeleteButton() }) {
                        item {
                            MonthlyCalendar(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 16.dp)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(vertical = 8.dp)
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onHorizontalDrag = { _, dragAmount ->
                                                horizontalDragOffset += dragAmount
                                            },
                                            onDragEnd = {
                                                val swipeThreshold = 100f
                                                if (horizontalDragOffset > swipeThreshold) {
                                                    viewModel.onPreviousMonth()
                                                } else if (horizontalDragOffset < -swipeThreshold) {
                                                    viewModel.onNextMonth()
                                                }
                                                horizontalDragOffset = 0f
                                            }
                                        )
                                    },
                                calendarDays = uiState.calendarDays,
                                onDateSelected = { viewModel.onDateSelected(it) },
                                theme = uiState.theme
                            )
                        }
                        // Seção de Aniversários
                        if (uiState.birthdaysForSelectedDate.isNotEmpty()) {
                            item {
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
                        }
                        
                        // Seção de Notas
                        if (uiState.notesForSelectedDate.isNotEmpty()) {
                            item {
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
                        }
                        
                        // Seção de Tarefas e Eventos (excluindo aniversários e notas)
                        if (uiState.tasksForSelectedDate.isNotEmpty()) {
                            item {
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
                        }
                        if (uiState.holidaysForSelectedDate.isNotEmpty()) {
                            item {
                                HolidaysForSelectedDaySection(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    holidays = uiState.holidaysForSelectedDate
                                )
                            }
                        }
                        if (uiState.saintDaysForSelectedDate.isNotEmpty()) {
                            item {
                                SaintDaysForSelectedDaySection(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    saints = uiState.saintDaysForSelectedDate,
                                    onSaintClick = { viewModel.onSaintDayClick(it) }
                                )
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
        }

        if (uiState.activityToEdit != null) {
            CreateActivityModal(
                activityToEdit = uiState.activityToEdit!!,
                onDismissRequest = { viewModel.closeCreateActivityModal() },
                onSaveActivity = { viewModel.onSaveActivity(it) }
            )
        }

        uiState.activityIdToDelete?.let { activityId ->
            val activityToDelete = uiState.activities.find { it.id == activityId }
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
        
        // Diálogo de permissão de armazenamento
        if (uiState.needsStoragePermission) {
            StoragePermissionDialog(
                onDismiss = { viewModel.clearBackupMessage() },
                onPermissionGranted = {
                    viewModel.clearBackupMessage()
                    // Tentar fazer backup novamente
                    viewModel.onBackupRequest()
                }
            )
        }


    }
}
