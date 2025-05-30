package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.data.model.Activity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


@Composable
fun TasksForSelectedDaySection(
    modifier: Modifier = Modifier,
    tasks: List<Activity>,
    selectedDate: LocalDate,
    onTaskClick: (Activity) -> Unit,
    onAddTaskClick: () -> Unit // Este parâmetro ainda pode ser útil se você quiser outro botão em outro lugar
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd 'de' MMMM", Locale("pt", "BR")) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            // ATUALIZADO: Removido Arrangement.SpaceBetween para que o título possa centralizar ou alinhar à esquerda
            // horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tarefas para ${selectedDate.format(dateFormatter)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f) // Permite que o texto ocupe o espaço disponível
            )
            // ATUALIZADO: Botão de adicionar tarefa REMOVIDO desta seção
            // IconButton(onClick = onAddTaskClick) {
            //     Icon(Icons.Filled.Add, contentDescription = "Adicionar Tarefa")
            // }
        }

        if (tasks.isEmpty()) {
            Text(
                text = "Nenhuma tarefa para este dia.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 16.dp)
            )
            // Você poderia adicionar um botão "Adicionar Tarefa" aqui se quisesse,
            // que chamaria onAddTaskClick()
            // Button(onClick = onAddTaskClick, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            //    Text("Adicionar Tarefa")
            // }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks) { task ->
                    TaskItem(task = task, onTaskClick = onTaskClick)
                }
            }
        }
    }
}

// A função TaskItem permanece a mesma da resposta anterior
@Composable
fun TaskItem(
    task: Activity,
    onTaskClick: (Activity) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable { onTaskClick(task) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (task.startTime != null) {
                Text(
                    text = "Às ${task.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}