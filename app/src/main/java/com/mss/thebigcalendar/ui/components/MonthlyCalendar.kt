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
import androidx.compose.foundation.shape.CircleShape // Ou RectangleShape se preferir células mais quadradas no geral
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
import com.mss.thebigcalendar.data.model.CalendarDay
import java.time.LocalDate

@Composable
fun MonthlyCalendar(
    modifier: Modifier = Modifier,
    calendarDays: List<CalendarDay>,
    onDateSelected: (LocalDate) -> Unit
) {
    val weekDayAbbreviations = getWeekDayAbbreviations()

    Column(modifier = modifier) {
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

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            userScrollEnabled = false
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
        .padding(1.dp)
        .aspectRatio(1f / 1.25f)
        .clip(MaterialTheme.shapes.small)
        .clickable(enabled = day.isCurrentMonth) {
            onDateSelected(day.date)
        }
        .then(
            if (day.isSelected) {
                Modifier.background(MaterialTheme.colorScheme.primaryContainer)
            } else {
                Modifier
            }
        )
        .padding(vertical = 4.dp, horizontal = 2.dp)

    Column(
        modifier = cellModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            textAlign = TextAlign.Center,
            style = if (day.isSelected) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodySmall,
            color = when {
                day.isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            },
            modifier = Modifier.padding(top = 2.dp)
        )

        if (day.isCurrentMonth && day.tasks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // ATUALIZADO: Reduzindo o espaçamento entre as tarefas.
                // Experimente 0.dp para nenhum espaço extra, ou um valor bem pequeno.
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                day.tasks.take(2).forEach { task ->
                    Text(
                        text = task.title,
                        fontSize = 7.sp,
                        color = if (day.isSelected) LocalContentColor.current.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}