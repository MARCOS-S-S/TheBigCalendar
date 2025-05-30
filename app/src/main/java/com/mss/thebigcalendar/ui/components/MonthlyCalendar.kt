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
import androidx.compose.foundation.shape.RoundedCornerShape // Import para RoundedCornerShape
import androidx.compose.runtime.remember // Import para remember
import com.mss.thebigcalendar.data.model.Activity
import androidx.compose.foundation.layout.size // Para Modifier.size()
import androidx.compose.foundation.layout.width  // Para Modifier.width()



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
            Spacer(modifier = Modifier.height(2.dp)) // Espaço entre o número do dia e a primeira tarefa

            Column(
                horizontalAlignment = Alignment.CenterHorizontally, // Centraliza as Rows das tarefas
                verticalArrangement = Arrangement.spacedBy(1.dp) // Espaço vertical entre as tarefas
            ) {
                val fallbackColor = MaterialTheme.colorScheme.secondaryContainer
                day.tasks.take(2).forEach { task ->
                    val taskColor = remember(task.categoryColor, fallbackColor) {
                        try {
                            Color(android.graphics.Color.parseColor(task.categoryColor))
                        } catch (e: Exception) {
                            fallbackColor
                        }
                    }

                    // ATUALIZADO: Row para alinhar a linha colorida e o texto
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        // Se quiser que todas as linhas de tarefa comecem na mesma posição horizontal:
                        // modifier = Modifier.fillMaxWidth(),
                        // horizontalArrangement = Arrangement.Start // Ou CenterHorizontally para centralizar o conjunto linha+texto
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 3.dp, height = 7.dp) // Tamanho da linha colorida
                                .background(taskColor, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(3.dp)) // Espaço entre a linha e o texto
                        Text(
                            text = task.title,
                            // ATUALIZADO: Cor do texto padrão, sem fundo no próprio Text
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                            fontSize = 7.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            // textAlign = TextAlign.Start // Se a Row preencher a largura
                        )
                    }
                }
            }
        }
    }
}