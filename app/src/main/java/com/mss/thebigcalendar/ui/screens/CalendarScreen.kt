package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.data.model.ViewMode
import com.mss.thebigcalendar.ui.components.MonthlyCalendar
import com.mss.thebigcalendar.ui.components.Sidebar
import com.mss.thebigcalendar.ui.components.YearlyCalendarView // NOVO: Import
import com.mss.thebigcalendar.ui.theme.TheBigCalendarTheme
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel
import java.time.format.TextStyle
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(uiState.isSidebarOpen) {
        if (uiState.isSidebarOpen) {
            if (!drawerState.isOpen) drawerState.open()
        } else {
            if (drawerState.isOpen) drawerState.close()
        }
    }
    // Sincroniza o estado do drawer com o uiState se o usuário fechar arrastando
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Closed && uiState.isSidebarOpen) {
            viewModel.closeSidebar()
        }
    }

    TheBigCalendarTheme(darkTheme = uiState.theme == Theme.DARK) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
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
                topBar = {
                    TopAppBar(
                        title = {
                            // ATUALIZADO: Título dinâmico baseado no ViewMode
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
                            IconButton(onClick = { viewModel.openSidebar() }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Abrir menu"
                                )
                            }
                        },
                        actions = {
                            // ATUALIZADO: Ações dinâmicas baseadas no ViewMode
                            when (uiState.viewMode) {
                                ViewMode.MONTHLY -> {
                                    IconButton(onClick = { viewModel.onPreviousMonth() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mês Anterior")
                                    }
                                    IconButton(onClick = { viewModel.onNextMonth() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Próximo Mês")
                                    }
                                }
                                ViewMode.YEARLY -> {
                                    IconButton(onClick = { viewModel.onPreviousYear() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ano Anterior")
                                    }
                                    IconButton(onClick = { viewModel.onNextYear() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Próximo Ano")
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
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    // ATUALIZADO: Alterna entre visualização mensal e anual
                    when (uiState.viewMode) {
                        ViewMode.MONTHLY -> {
                            MonthlyCalendar(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                                calendarDays = uiState.calendarDays,
                                onDateSelected = { date -> viewModel.onDateSelected(date) }
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

                    // Lógica para exibir os Modais (mantida)
                    if (uiState.activityToEdit != null) {
                        // CreateActivityModal(...)
                    }
                    if (uiState.isSettingsModalOpen) {
                        // SettingsModal(...)
                    }
                }
            }
        }
    }
}