package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mss.thebigcalendar.data.model.CalendarDay // Certifique-se que o import está correto
import java.time.LocalDate

@Composable
fun MonthlyCalendar(
    modifier: Modifier = Modifier,
    calendarDays: List<CalendarDay>,
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

@Composable
private fun DayCell(
    day: CalendarDay,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val cellModifier = modifier
        .padding(1.dp) // Pequeno espaçamento entre as células
        .aspectRatio(1f) // Para manter as células quadradas
        .clip(CircleShape)
        .clickable(enabled = day.isCurrentMonth) {
            onDateSelected(day.date)
        }
        .then(
            if (day.isSelected) {
                Modifier.background(MaterialTheme.colorScheme.primaryContainer)
            } else {
                Modifier // Sem fundo extra se não estiver selecionado
            }
        )
        .padding(4.dp) // Padding interno para o conteúdo

    Column(
        modifier = cellModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (day.tasks.isNotEmpty() && day.isCurrentMonth) Arrangement.SpaceBetween else Arrangement.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            textAlign = TextAlign.Center,
            style = if (day.isSelected) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyMedium,
            color = when {
                day.isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }
        )

        if (day.isCurrentMonth && day.tasks.isNotEmpty()) {
            // Spacer(modifier = Modifier.height(1.dp)) // Espaço mínimo se houver tarefas
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                day.tasks.take(2).forEach { task -> // Mostra no máximo 2 tarefas
                    Text(
                        text = task.title,
                        fontSize = 8.sp,
                        color = if (day.isSelected) LocalContentColor.current.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}