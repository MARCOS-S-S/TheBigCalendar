package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height

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
            // horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Agendamentos para ${selectedDate.format(dateFormatter)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f) // Permite que o texto ocupe o espaço disponível
            )
        }

        if (tasks.isEmpty()) {
            Text(
                text = "Mó paz.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(vertical = 16.dp)
            )
            // Você poderia adicionar um botão "Adicionar Tarefa" aqui se quisesse,
            // que chamaria onAddTaskClick()
            // Button(onClick = onAddTaskClick, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            //    Text("Adicionar Tarefa")
            // }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tasks.forEach { task ->
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
    //onClick: (Activity) -> Unit,
    onTaskClick: (Activity) -> Unit,
    modifier: Modifier = Modifier
) {
    val fallbackColor = MaterialTheme.colorScheme.secondary // Cor fallback

    val taskColor = remember(task.categoryColor) {
        when (task.categoryColor) {
            "1" -> Color.White
            "2" -> Color.Blue
            "3" -> Color.Yellow
            "4" -> Color.Red
            else -> fallbackColor
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable { onTaskClick(task) }
            .padding(start = 8.dp), // Padding inicial para a barra de cor
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Barra vertical com a cor da tarefa
        val boxModifier = if (taskColor == Color.White) {
            Modifier
                .width(4.dp)
                .height(36.dp) // Altura da barra, ajuste conforme necessário
                .background(taskColor, shape = RoundedCornerShape(2.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
        } else {
            Modifier
                .width(4.dp)
                .height(36.dp)
                .background(taskColor, shape = RoundedCornerShape(2.dp))
        }

        Box(modifier = boxModifier)

        Spacer(modifier = Modifier.width(12.dp)) // Espaço entre a barra e o texto

        Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) { // Adicionado padding vertical aqui
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