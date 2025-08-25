package com.mss.thebigcalendar.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.ViewMode
import com.mss.thebigcalendar.ui.components.CreateActivityModal
import com.mss.thebigcalendar.ui.components.DeleteConfirmationDialog
import com.mss.thebigcalendar.ui.components.HolidaysForSelectedDaySection
import com.mss.thebigcalendar.ui.components.MonthlyCalendar
import com.mss.thebigcalendar.ui.components.SaintDaysForSelectedDaySection
import com.mss.thebigcalendar.ui.components.SaintInfoDialog
import com.mss.thebigcalendar.ui.components.Sidebar
import com.mss.thebigcalendar.ui.components.TasksForSelectedDaySection
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
    val drawerState = remember(viewModel) {
        DrawerState(initialValue = DrawerValue.Closed)
    }

    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Closed) {
            viewModel.closeSidebar()
        } else {
            viewModel.openSidebar()
        }
    }
    LaunchedEffect(uiState.isSidebarOpen) {
        if (uiState.isSidebarOpen != drawerState.isOpen) {
            if (uiState.isSidebarOpen) {
                drawerState.open()
            } else {
                drawerState.close()
            }
        }
    }


    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = uiState.currentSettingsScreen == null,
        drawerContent = {
            Sidebar(
                uiState = uiState,
                onViewModeChange = { viewModel.onViewModeChange(it) },
                onFilterChange = { key, value -> viewModel.onFilterChange(key, value) },
                onNavigateToSettings = { viewModel.onNavigateToSettings(it) },
                onBackup = { viewModel.onBackupRequest() },
                onRestore = { viewModel.onRestoreRequest() },
                onRequestClose = { viewModel.closeSidebar() }
            )
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
                            onThemeChange = { viewModel.onThemeChange(it) }
                        )
                    }
                }
            }
            else -> {
                MainCalendarView(viewModel, uiState, scope, drawerState)
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
    drawerState: androidx.compose.material3.DrawerState
) {
    Scaffold(
        modifier = Modifier.clickableWithoutRipple { viewModel.hideDeleteButton() },
        topBar = {
            TopAppBar(
                title = {
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
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        scope.launch {
                            drawerState.animateTo(DrawerValue.Open, anim = tween(durationMillis = 300, easing = FastOutSlowInEasing))
                        }
                    }) {
                        Icon(Icons.Default.Menu, stringResource(id = R.string.open_close_menu))
                    }
                },
                actions = {
                    val (prevAction, nextAction) = when (uiState.viewMode) {
                        ViewMode.MONTHLY -> Pair({ viewModel.onPreviousMonth() }, { viewModel.onNextMonth() })
                        ViewMode.YEARLY -> Pair({ viewModel.onPreviousYear() }, { viewModel.onNextYear() })
                    }
                    IconButton(onClick = prevAction) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.previous))
                    }
                    IconButton(onClick = nextAction) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(id = R.string.next))
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
                    Icon(Icons.Filled.Add, stringResource(id = R.string.add_appointment))
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
    }
}
