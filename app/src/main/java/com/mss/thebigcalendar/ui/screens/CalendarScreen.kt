package com.mss.thebigcalendar.ui.screens

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
 
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.mss.thebigcalendar.ui.components.TasksForSelectedDaySection
import com.mss.thebigcalendar.ui.components.NotesForSelectedDaySection
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
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = uiState.currentSettingsScreen == null,
            scrimColor = Color.Black.copy(alpha = 0.7f),
            drawerContent = {
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
                    if (uiState.currentSettingsScreen == null) {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    when (uiState.viewMode) {
                                        ViewMode.MONTHLY -> {
                                            val monthName = uiState.displayedYearMonth.month
                                                .getDisplayName(
                                                    java.time.format.TextStyle.FULL,
                                                    Locale("pt", "BR")
                                                )
                                                .replaceFirstChar {
                                                    it.titlecase(
                                                        Locale(
                                                            "pt",
                                                            "BR"
                                                        )
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
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (uiState.currentSettingsScreen) {
                        "General" -> {
                            GeneralSettingsScreen(
                                currentTheme = uiState.theme,
                                onThemeChange = { viewModel.onThemeChange(it) },
                                googleAccount = uiState.googleSignInAccount,
                                onSignInClicked = { viewModel.onSignInClicked() },
                                onSignOutClicked = { viewModel.signOut() },
                                isSyncing = uiState.isSyncing,
                                onManualSync = { viewModel.onManualSync() },
                                syncProgress = uiState.syncProgress,
                            )
                        }

                        else -> {
                            MainCalendarView(
                                viewModel,
                                uiState,
                                scope,
                                drawerState,
                                snackbarHostState
                            )
                        }
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
    modifier: Modifier = Modifier
) {
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickableWithoutRipple { viewModel.hideDeleteButton() }
    ) {
        when (uiState.viewMode) {
            ViewMode.MONTHLY -> {
                val listState = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier.fillMaxSize().clickableWithoutRipple { viewModel.hideDeleteButton() },
                    state = listState
                ) {
                    item(
                        key = "calendar-${uiState.displayedYearMonth}",
                        contentType = "calendarHeader"
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 16.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(vertical = 8.dp)
                        ) {
                            MonthlyCalendar(
                                modifier = Modifier
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
                            
                            if (uiState.showMoonPhases) {
                                MoonPhasesComponent(
                                    yearMonth = uiState.displayedYearMonth,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp)
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
}