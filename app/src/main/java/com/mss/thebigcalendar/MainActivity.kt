package com.mss.thebigcalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mss.thebigcalendar.ui.screens.CalendarScreen
import com.mss.thebigcalendar.ui.theme.TheBigCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Habilita o app para desenhar sob as barras de status e navegação do sistema
        enableEdgeToEdge()
        setContent {
            // O tema envolve toda a aplicação, mas a tela principal também
            // pode selecioná-lo para garantir consistência.
            TheBigCalendarTheme {
                CalendarScreen()
            }
        }
    }
}