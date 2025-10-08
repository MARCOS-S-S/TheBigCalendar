package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Activity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TasksForSelectedDaySection(
    modifier: Modifier = Modifier,
    tasks: List<Activity>,
    selectedDate: LocalDate,
    activityIdWithDeleteVisible: String?,
    onTaskClick: (Activity) -> Unit,
    onTaskLongClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onCompleteClick: (String) -> Unit,
    onAddTaskClick: () -> Unit
) {
    val dateFormat = stringResource(id = R.string.date_format_day_month)
    val dateFormatter = remember(dateFormat) { DateTimeFormatter.ofPattern(dateFormat, Locale.getDefault()) }

    // NOTA: Não filtramos por showInCalendar aqui, pois queremos que todas as tarefas apareçam
    // na seção de agendamentos, mesmo as que não são mostradas no calendário
    val visibleTasks = tasks

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.appointments_for, selectedDate.format(dateFormatter)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            
            // Botão para adicionar tarefa ou evento
            IconButton(
                onClick = onAddTaskClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.add_task_or_event),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (visibleTasks.isEmpty()) {
            Text(
                text = stringResource(id = R.string.no_appointments),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleTasks.forEach { task ->
                    TaskItem(
                        task = task,
                        deleteButtonVisible = activityIdWithDeleteVisible == task.id,
                        onTaskClick = onTaskClick,
                        onTaskLongClick = onTaskLongClick,
                        onDeleteClick = onDeleteClick,
                        onCompleteClick = onCompleteClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: Activity,
    deleteButtonVisible: Boolean,
    onTaskClick: (Activity) -> Unit,
    onTaskLongClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onCompleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val fallbackColor = MaterialTheme.colorScheme.secondary
    val taskColor = remember(task.categoryColor) {
        when (task.categoryColor) {
            "1" -> Color.White
            "2" -> Color.Blue
            "3" -> Color.Yellow
            "4" -> Color.Red
            else -> {
                try {
                    Color(android.graphics.Color.parseColor(task.categoryColor))
                } catch (e: IllegalArgumentException) {
                    fallbackColor
                }
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .combinedClickable(
                onClick = { onTaskLongClick(task.id) },
                onLongClick = { onTaskClick(task) }
            )
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val boxModifier = if (taskColor == Color.White) {
            Modifier
                .width(4.dp)
                .height(36.dp)
                .background(taskColor, shape = RoundedCornerShape(2.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
        } else {
            Modifier
                .width(4.dp)
                .height(36.dp)
                .background(taskColor, shape = RoundedCornerShape(2.dp))
        }

        Box(modifier = boxModifier)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
            Text(text = task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (task.startTime != null) {
                Text(
                    text = stringResource(id = R.string.at_time, task.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (deleteButtonVisible) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                // Botão de concluir
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onCompleteClick(task.id) }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OK",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Botão de deletar
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onDeleteClick(task.id) }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DEL",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

