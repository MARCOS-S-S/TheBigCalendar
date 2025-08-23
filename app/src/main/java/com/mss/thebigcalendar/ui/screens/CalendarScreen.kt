package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.data.model.ViewMode
import com.mss.thebigcalendar.ui.components.CreateActivityModal
import com.mss.thebigcalendar.ui.components.DeleteConfirmationDialog
import com.mss.thebigcalendar.ui.components.HolidaysForSelectedDaySection
import com.mss.thebigcalendar.ui.components.MonthlyCalendar
import com.mss.thebigcalendar.ui.components.SaintDaysForSelectedDaySection
import com.mss.thebigcalendar.ui.components.Sidebar
import com.mss.thebigcalendar.ui.components.TasksForSelectedDaySection
import com.mss.thebigcalendar.ui.components.YearlyCalendarView
import com.mss.thebigcalendar.ui.theme.TheBigCalendarTheme
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

// Função para lidar com cliques fora dos itens interativos
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
    val drawerState = rememberDrawerState(
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
                scope.launch { try { drawerState.open() } catch (e: CancellationException) { /* ok */ } }
            } else if (!uiState.isSidebarOpen && drawerState.isOpen) {
                scope.launch { try { drawerState.close() } catch (e: CancellationException) { /* ok */ } }
            }
        }
    }

    TheBigCalendarTheme(darkTheme = when (uiState.theme) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM -> isSystemInDarkTheme()
    }) {
        ModalNavigationDrawer(
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
                modifier = Modifier.clickableWithoutRipple { viewModel.hideDeleteButton() },
                topBar = {
                    TopAppBar(
                        title = {
                            val text = when (uiState.viewMode) {
                                ViewMode.MONTHLY -> {
                                    val monthName = uiState.displayedYearMonth.month
                                        .getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                                        .replaceFirstChar { it.titlecase(Locale("pt", "BR")) }
                                    "$monthName de ${uiState.displayedYearMonth.year}"
                                }
                                ViewMode.YEARLY -> uiState.displayedYearMonth.year.toString()
                            }
                            Text(text)
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, "Abrir menu")
                            }
                        },
                        actions = {
                            val (prevAction, nextAction) = when (uiState.viewMode) {
                                ViewMode.MONTHLY -> Pair({ viewModel.onPreviousMonth() }, { viewModel.onNextMonth() })
                                ViewMode.YEARLY -> Pair({ viewModel.onPreviousYear() }, { viewModel.onNextYear() })
                            }
                            IconButton(onClick = prevAction) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Anterior")
                            }
                            IconButton(onClick = nextAction) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Próximo")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                },
                floatingActionButton = {
                    if (uiState.viewMode == ViewMode.MONTHLY) {
                        FloatingActionButton(
                            onClick = { viewModel.openCreateActivityModal(activityType = ActivityType.TASK) }
                        ) {
                            Icon(Icons.Filled.Add, "Adicionar Agendamento")
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
                            LazyColumn(modifier = Modifier.fillMaxSize().clickableWithoutRipple { viewModel.hideDeleteButton() }) {
                                item {
                                    MonthlyCalendar(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                                        calendarDays = uiState.calendarDays,
                                        onDateSelected = { viewModel.onDateSelected(it) },
                                        theme = uiState.theme
                                    )
                                }
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
                                        onAddTaskClick = { viewModel.openCreateActivityModal(activityType = ActivityType.TASK) }
                                    )
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
                                            saints = uiState.saintDaysForSelectedDate
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
            }
        }
    }
}