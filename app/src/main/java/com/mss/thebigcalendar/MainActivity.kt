package com.mss.thebigcalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.service.NotificationTestService
import com.mss.thebigcalendar.ui.screens.CalendarScreen
import com.mss.thebigcalendar.ui.theme.TheBigCalendarTheme
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CalendarViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this).get(CalendarViewModel::class.java)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            var isThemeLoaded by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                viewModel.uiState.first()
                isThemeLoaded = true
            }

            if (isThemeLoaded) {
                TheBigCalendarTheme(
                    darkTheme = when (uiState.theme) {
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