package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.ui.components.Sidebar
import com.mss.thebigcalendar.ui.theme.TheBigCalendarTheme
import com.mss.thebigcalendar.ui.theme.TheBigCalendarTheme
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel

// A anotação OptIn é necessária para usar a TopAppBar, que é experimental
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(uiState.isSidebarOpen) {
        if (uiState.isSidebarOpen) {
            drawerState.open()
        } else {
            drawerState.close()
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
                    // Garanta que esta função exista no seu ViewModel: fun closeSidebar()
                    onRequestClose = { viewModel.closeSidebar() }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    // Barra no topo da tela
                    TopAppBar(
                        title = { Text("The Big Calendar") },
                        navigationIcon = {
                            // Botão para abrir o menu (sidebar)
                            IconButton(onClick = {
                                // Garanta que esta função exista no seu ViewModel: fun openSidebar()
                                viewModel.openSidebar()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Abrir menu"
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tela Principal do Calendário")
                }

                // A lógica dos modais continua aqui, dentro do Box, se necessário
            }
        }
    }
}