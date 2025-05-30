package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.data.model.ViewMode
import com.mss.thebigcalendar.ui.components.MonthlyCalendar
import com.mss.thebigcalendar.ui.components.Sidebar
import com.mss.thebigcalendar.ui.components.TasksForSelectedDaySection // NOVO: Import
import com.mss.thebigcalendar.ui.components.YearlyCalendarView
import com.mss.thebigcalendar.ui.theme.TheBigCalendarTheme
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import com.mss.thebigcalendar.ui.components.CreateActivityModal


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(
        // ... (código do drawerState e LaunchedEffects permanecem os mesmos) ...
        initialValue = if (uiState.isSidebarOpen) DrawerValue.Open else DrawerValue.Closed,
        confirmStateChange = { newDrawerValue ->
            if (newDrawerValue == DrawerValue.Closed && uiState.isSidebarOpen) {
                viewModel.closeSidebar()
            } else if (newDrawerValue == DrawerValue.Open && !uiState.isSidebarOpen) {
                viewModel.openSidebar()
            }
            true
        }
    )

    LaunchedEffect(uiState.isSidebarOpen, drawerState.isAnimationRunning) {
        if (!drawerState.isAnimationRunning) {
            if (uiState.isSidebarOpen && !drawerState.isOpen) {
                scope.launch {
                    try { drawerState.open() } catch (e: CancellationException) { /* ok */ }
                }
            } else if (!uiState.isSidebarOpen && drawerState.isOpen) {
                scope.launch {
                    try { drawerState.close() } catch (e: CancellationException) { /* ok */ }
                }
            }
        }
    }

    TheBigCalendarTheme(darkTheme = uiState.theme == Theme.DARK) {
        ModalNavigationDrawer(
            // ... (configuração do ModalNavigationDrawer) ...
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                Sidebar(
                    uiState = uiState,
                    onViewModeChange = { viewModel.onViewModeChange(it) },
                    onFilterChange = { key, value -> viewModel.onFilterChange(key, value) },
                    onThemeChange = { viewModel.onThemeChange(it) },
                    onOpenSettingsModal = { viewModel.openSettingsModal(it) },
                    onBackup = { viewModel.onBackupRequest() },
                    onRestore = { viewModel.onRestoreRequest() },
                    onRequestClose = { viewModel.closeSidebar() }
                )
            }
        ) {
            Scaffold(
                topBar = { /* ... TopAppBar como antes ... */
                    TopAppBar(
                        title = {
                            when (uiState.viewMode) {
                                ViewMode.MONTHLY -> {
                                    val monthName = uiState.displayedYearMonth.month
                                        .getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                                        .replaceFirstChar { char ->
                                            if (char.isLowerCase()) char.titlecase(Locale("pt", "BR")) else char.toString()
                                        }
                                    Text("$monthName de ${uiState.displayedYearMonth.year}")
                                }
                                ViewMode.YEARLY -> {
                                    Text(uiState.displayedYearMonth.year.toString())
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (drawerState.isClosed) {
                                    viewModel.openSidebar()
                                } else {
                                    viewModel.closeSidebar()
                                }
                            }) {
                                Icon(Icons.Default.Menu, "Abrir/Fechar menu")
                            }
                        },
                        actions = {
                            when (uiState.viewMode) {
                                ViewMode.MONTHLY -> {
                                    IconButton(onClick = { viewModel.onPreviousMonth() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Mês Anterior")
                                    }
                                    IconButton(onClick = { viewModel.onNextMonth() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Próximo Mês")
                                    }
                                }
                                ViewMode.YEARLY -> {
                                    IconButton(onClick = { viewModel.onPreviousYear() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Ano Anterior")
                                    }
                                    IconButton(onClick = { viewModel.onNextYear() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Próximo Ano")
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                },
                floatingActionButton = {
                    if (uiState.viewMode == ViewMode.MONTHLY) {
                        FloatingActionButton(
                            onClick = {
                                // Ao clicar no FAB, abrimos o modal para criar uma TAREFA para a data selecionada
                                viewModel.openCreateActivityModal(activityType = ActivityType.TASK)
                            }
                        ) {
                            Icon(Icons.Filled.Add, "Adicionar Tarefa")
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    when (uiState.viewMode) {
                        ViewMode.MONTHLY -> {
                            MonthlyCalendar(
                                modifier = Modifier
                                    .fillMaxWidth() // Ocupa a largura
                                    .padding(horizontal = 8.dp, vertical = 16.dp),
                                // Removido .weight(1f) para permitir que a lista de tarefas apareça logo abaixo
                                calendarDays = uiState.calendarDays,
                                onDateSelected = { date -> viewModel.onDateSelected(date) }
                            )
                            // Seção de tarefas para o dia selecionado
                            TasksForSelectedDaySection(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                    .weight(1f), // Faz a lista de tarefas ocupar o espaço restante
                                tasks = uiState.tasksForSelectedDate,
                                selectedDate = uiState.selectedDate,
                                onTaskClick = { task ->
                                    viewModel.openCreateActivityModal(task, ActivityType.TASK)
                                },
                                onAddTaskClick = {
                                    viewModel.openCreateActivityModal(activityType = ActivityType.TASK)
                                }
                            )
                        }
                        ViewMode.YEARLY -> {
                            YearlyCalendarView(
                                modifier = Modifier.fillMaxSize(),
                                year = uiState.displayedYearMonth.year,
                                onMonthClicked = { yearMonth -> viewModel.onYearlyMonthClicked(yearMonth) },
                                onNavigateYear = { delta ->
                                    if (delta > 0) viewModel.onNextYear() else viewModel.onPreviousYear()
                                }
                            )
                        }
                    }
                }

                // ATUALIZADO: Chamada ao Modal
                if (uiState.activityToEdit != null) {
                    CreateActivityModal(
                        activityToEdit = uiState.activityToEdit,
                        onDismissRequest = { viewModel.closeCreateActivityModal() },
                        onSaveActivity = { activity -> viewModel.onSaveActivity(activity) }
                    )
                }
                // if (uiState.isSettingsModalOpen) { ... } // Mantenha se já tiver
            }
        }
    }
}