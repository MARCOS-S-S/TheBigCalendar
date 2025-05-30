package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.data.model.CalendarDay
import java.time.LocalDate

/**
 * Composable que exibe a grade do calendário mensal.
 */
@Composable
fun MonthlyCalendar(
    modifier: Modifier = Modifier,
    calendarDays: List<CalendarDay>, // Recebe a lista de dias do ViewModel/UiState
    onDateSelected: (LocalDate) -> Unit
) {
    val weekDayAbbreviations = getWeekDayAbbreviations() // Do CalendarUtils.kt

    Column(modifier = modifier) {
        // Cabeçalho com os dias da semana
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekDayAbbreviations.forEach { dayAbbreviation ->
                Text(
                    text = dayAbbreviation,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Grade de dias
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            userScrollEnabled = false // Desabilita scroll se a grade tem tamanho fixo
        ) {
            items(calendarDays) { day ->
                DayCell(
                    day = day,
                    onDateSelected = onDateSelected
                )
            }
        }
    }
}

/**
 * Composable para exibir uma única célula de dia no calendário.
 */
@Composable
private fun DayCell(
    day: CalendarDay,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val cellModifier = modifier
        .padding(1.dp) // Pequeno espaçamento entre as células
        .aspectRatio(1f) // Para manter as células quadradas
        .clip(CircleShape) // Formato da célula, pode ser RectangleShape ou RoundedCornerShape
        .clickable(enabled = day.isCurrentMonth) { // Só permite clique em dias do mês atual
            onDateSelected(day.date)
        }
        .then(
            if (day.isSelected) {
                Modifier.background(MaterialTheme.colorScheme.primaryContainer)
            } else {
                Modifier
            }
        )

    Box(
        modifier = cellModifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                day.isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) // Dias de outros meses
            }
        )
        // Aqui você pode adicionar lógica futura para desenhar os "pontos" de eventos
        // com base em dados adicionais que o CalendarDay pode ter.
    }
}