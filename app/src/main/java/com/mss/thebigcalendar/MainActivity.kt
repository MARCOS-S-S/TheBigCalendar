package com.mss.thebigcalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.ui.screens.CalendarScreen
import com.mss.thebigcalendar.ui.theme.TheBigCalendarTheme
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CalendarViewModel
    private var theme by mutableStateOf<Theme?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this).get(CalendarViewModel::class.java)

        lifecycleScope.launch {
            theme = viewModel.uiState.first().theme
            setContent {
                val uiState by viewModel.uiState.collectAsState()
                TheBigCalendarTheme(
                    darkTheme = when (theme) {
                        Theme.LIGHT -> false
                        Theme.DARK -> true
                        else -> isSystemInDarkTheme()
                    }
                ) {
                    CalendarScreen(viewModel)
                }
            }
        }
    }
}