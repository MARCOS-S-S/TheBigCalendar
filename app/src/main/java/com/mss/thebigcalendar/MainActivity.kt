package com.mss.thebigcalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.ui.screens.CalendarScreen
import com.mss.thebigcalendar.ui.theme.TheBigCalendarTheme
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Habilita o app para desenhar sob as barras de status e navegação do sistema
        enableEdgeToEdge()
        setContent {
            val viewModelFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            val viewModel: CalendarViewModel = viewModel(factory = viewModelFactory)
            val uiState = viewModel.uiState.collectAsState().value

            // O tema envolve toda a aplicação, mas a tela principal também
            // pode selecioná-lo para garantir consistência.
            TheBigCalendarTheme(
                darkTheme = when (uiState.theme) {
                    Theme.LIGHT -> false
                    Theme.DARK -> true
                    Theme.SYSTEM -> isSystemInDarkTheme()
                }
            ) {
                CalendarScreen(viewModel)
            }
        }
    }
}