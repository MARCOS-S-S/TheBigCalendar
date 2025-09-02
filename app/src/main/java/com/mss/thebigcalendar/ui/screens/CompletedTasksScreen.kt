package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.runtime.DisposableEffect
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedTasksScreen(
    onBackClick: () -> Unit,
    completedActivities: List<Activity> = emptyList(),
    onBackPressedDispatcher: OnBackPressedDispatcher? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Tratar o botão de voltar do sistema
    onBackPressedDispatcher?.let { dispatcher ->
        DisposableEffect(dispatcher) {
            val callback = object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBackClick()
                }
            }
            dispatcher.addCallback(callback)
            onDispose {
                callback.remove()
            }
        }
    }
    
    // Filtrar atividades baseado na busca
    val filteredActivities = completedActivities.filter { activity ->
        activity.title.contains(searchQuery, ignoreCase = true) ||
        activity.description?.contains(searchQuery, ignoreCase = true) == true
    }.sortedByDescending { activity ->
        try {
            LocalDate.parse(activity.date)
        } catch (e: Exception) {
            LocalDate.MIN
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Tarefas Concluídas",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campo de busca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar tarefas concluídas") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Buscar"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Contador de tarefas
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total de tarefas concluídas:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = filteredActivities.size.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            // Lista de tarefas concluídas
            if (filteredActivities.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📝",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            text = if (searchQuery.isEmpty()) {
                                "Nenhuma tarefa concluída ainda"
                            } else {
                                "Nenhuma tarefa encontrada"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (searchQuery.isEmpty()) {
                                "Complete algumas tarefas para vê-las aqui"
                            } else {
                                "Tente ajustar sua busca"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredActivities) { activity ->
                        CompletedTaskItem(activity = activity)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedTaskItem(
    activity: Activity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ícone do tipo de atividade
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(getActivityTypeColor(activity.activityType)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getActivityTypeIcon(activity.activityType),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }
            
            // Informações da atividade
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                activity.description?.let { description ->
                    if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Data de conclusão
                    Text(
                        text = formatDate(activity.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Horário se disponível
                    activity.startTime?.let { time ->
                        Text(
                            text = "• ${String.format("%02d:%02d", time.hour, time.minute)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Ícone de concluído
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Concluída",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

private fun getActivityTypeColor(activityType: ActivityType): Color {
    return when (activityType) {
        ActivityType.TASK -> Color(0xFF4CAF50) // Verde
        ActivityType.EVENT -> Color(0xFF2196F3) // Azul
        ActivityType.NOTE -> Color(0xFFFF9800) // Laranja
        ActivityType.BIRTHDAY -> Color(0xFFE91E63) // Rosa
    }
}

private fun getActivityTypeIcon(activityType: ActivityType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (activityType) {
        ActivityType.TASK -> Icons.Filled.CheckCircle
        ActivityType.EVENT -> Icons.Filled.Event
        ActivityType.NOTE -> Icons.Filled.Note
        ActivityType.BIRTHDAY -> Icons.Filled.Cake
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        date.format(formatter)
    } catch (e: Exception) {
        dateString
    }
}
