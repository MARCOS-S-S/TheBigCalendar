package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.mss.thebigcalendar.data.model.CalendarDay
import java.time.LocalDate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.mss.thebigcalendar.data.model.Activity
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

@Composable
fun MonthlyCalendar(
    modifier: Modifier = Modifier,
    calendarDays: List<CalendarDay>,
    onDateSelected: (LocalDate) -> Unit,
    theme: com.mss.thebigcalendar.data.model.Theme
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

        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            val weeks = remember(calendarDays) { calendarDays.chunked(7) }
            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    week.forEach { day ->
                        Box(modifier = Modifier.weight(1f)) {
                            DayCell(
                                day = day,
                                onDateSelected = onDateSelected,
                                theme = theme
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    theme: com.mss.thebigcalendar.data.model.Theme
) {
    val cellModifier = modifier
        .padding(1.dp)
        .aspectRatio(1f / 1.35f)
        .clip(MaterialTheme.shapes.small)
        .then(
            when {
                day.isToday -> Modifier.border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                day.isCurrentMonth -> Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), MaterialTheme.shapes.small)
                else -> Modifier
            }
        )
        .background(
            when {
                day.isSelected -> MaterialTheme.colorScheme.primaryContainer
                day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else -> Color.Transparent
            }
        )
        //Dias possiveis de serem clicados:
        .clickable {
            onDateSelected(day.date)
        }
        .padding(vertical = 0.dp, horizontal = 2.dp)

    Column(
        modifier = cellModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            textAlign = TextAlign.Center,
            style = if (day.isSelected) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyLarge,
            color = when {
                day.isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                day.isNationalHoliday -> MaterialTheme.colorScheme.error
                day.isSaintDay -> when (theme) {
                    com.mss.thebigcalendar.data.model.Theme.DARK -> Color.Yellow
                    com.mss.thebigcalendar.data.model.Theme.LIGHT -> Color.Blue
                    com.mss.thebigcalendar.data.model.Theme.SYSTEM -> if (isSystemInDarkTheme()) Color.Yellow else Color.Blue
                }
                day.isWeekend -> MaterialTheme.colorScheme.primary
                day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            },
            modifier = Modifier.padding(top = 2.dp)
        )

        if (day.holiday != null) {
            val isSaintDay = day.isSaintDay
            Text(
                text = day.holiday.name,
                color = if (isSaintDay) {
                    when (theme) {
                        com.mss.thebigcalendar.data.model.Theme.DARK -> Color.Yellow
                        com.mss.thebigcalendar.data.model.Theme.LIGHT -> Color.Blue
                        com.mss.thebigcalendar.data.model.Theme.SYSTEM -> if (isSystemInDarkTheme()) Color.Yellow else Color.Blue
                    }
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                fontSize = if (isSaintDay) 7.sp else 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 1.dp),
                fontWeight = if (isSaintDay) FontWeight.Bold else FontWeight.Normal
            )
        }

        // Filtrar apenas tarefas que devem aparecer no calendário (memoizado por lista de tarefas)
        val visibleTasks = remember(day.tasks) { day.tasks.filter { it.showInCalendar } }
        
        if (visibleTasks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(1.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                val fallbackColor = MaterialTheme.colorScheme.secondaryContainer
                visibleTasks.take(2).forEach { task ->
                    val taskColor = remember(task.categoryColor, task.activityType) {
                        when {
                            task.activityType == com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY -> Color(0xFFE91E63) // Rosa para aniversários
                            task.activityType == com.mss.thebigcalendar.data.model.ActivityType.NOTE -> Color(0xFF9C27B0) // Roxo para notas
                            task.categoryColor == "1" -> Color.White
                            task.categoryColor == "2" -> Color.Blue
                            task.categoryColor == "3" -> Color.Yellow
                            task.categoryColor == "4" -> Color.Red
                            else -> fallbackColor
                        }
                    }

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
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            //textAlign = TextAlign.Start // Se a Row preencher a largura
                        )
                    }
                }
                if (visibleTasks.size > 2) {
                    Row (
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 1.dp)
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }
        }
    }
}