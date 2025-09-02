package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import com.mss.thebigcalendar.data.model.Activity

data class ChartData(
    val label: String,
    val value: Int,
    val color: Color,
    val percentage: Float
)

@Composable
fun PieChartComponent(
    activities: List<Activity>,
    modifier: Modifier = Modifier
) {
    // Lógica de filtragem e categorização
    val chartData = categorizeActivities(activities)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Distribuição de Atividades",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (chartData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma atividade encontrada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Gráfico de pizza simples usando círculos
                SimplePieChart(
                    data = chartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                
                // Legenda
                ChartLegend(
                    data = chartData,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SimplePieChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(200.dp)
        ) {
            val totalValue = data.sumOf { it.value }
            if (totalValue > 0) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = minOf(size.width, size.height) / 2 * 0.8f
                var startAngle = -90f // Começar do topo
                
                data.forEach { item ->
                    val sweepAngle = (item.value.toFloat() / totalValue) * 360f
                    
                    // Desenhar o arco
                    drawArc(
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(
                            center.x - radius,
                            center.y - radius
                        ),
                        size = Size(radius * 2, radius * 2)
                    )
                    
                    startAngle += sweepAngle
                }
            }
        }
    }
}

@Composable
private fun ChartLegend(
    data: List<ChartData>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(item.color),
                        contentAlignment = Alignment.Center
                    ) {
                        // Círculo colorido para a legenda
                    }
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = item.color
                    )
                }
                
                Text(
                    text = "${item.value} (${item.percentage.toInt()}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun categorizeActivities(activities: List<Activity>): List<ChartData> {
    val totalActivities = activities.size
    
    if (totalActivities == 0) return emptyList()
    
    // Categorizar atividades baseado no título
    val tasks = activities.count { activity ->
        activity.title.contains("tarefa", ignoreCase = true) ||
        activity.title.contains("task", ignoreCase = true) ||
        activity.activityType.name.contains("TASK", ignoreCase = true)
    }
    
    val events = activities.count { activity ->
        activity.title.contains("evento", ignoreCase = true) ||
        activity.title.contains("event", ignoreCase = true) ||
        activity.activityType.name.contains("EVENT", ignoreCase = true)
    }
    
    val birthdays = activities.count { activity ->
        activity.title.contains("aniversário", ignoreCase = true) ||
        activity.title.contains("birthday", ignoreCase = true) ||
        activity.activityType.name.contains("BIRTHDAY", ignoreCase = true)
    }
    
    val notes = activities.count { activity ->
        activity.title.contains("nota", ignoreCase = true) ||
        activity.title.contains("note", ignoreCase = true) ||
        activity.activityType.name.contains("NOTE", ignoreCase = true)
    }
    
    val other = totalActivities - tasks - events - birthdays - notes
    
    val chartData = mutableListOf<ChartData>()
    
    if (tasks > 0) {
        chartData.add(
            ChartData(
                label = "Tarefas",
                value = tasks,
                color = Color(0xFF4CAF50), // Verde
                percentage = (tasks.toFloat() / totalActivities) * 100f
            )
        )
    }
    
    if (events > 0) {
        chartData.add(
            ChartData(
                label = "Eventos",
                value = events,
                color = Color(0xFF2196F3), // Azul
                percentage = (events.toFloat() / totalActivities) * 100f
            )
        )
    }
    
    if (birthdays > 0) {
        chartData.add(
            ChartData(
                label = "Aniversários",
                value = birthdays,
                color = Color(0xFFE91E63), // Rosa
                percentage = (birthdays.toFloat() / totalActivities) * 100f
            )
        )
    }
    
    if (notes > 0) {
        chartData.add(
            ChartData(
                label = "Notas",
                value = notes,
                color = Color(0xFFFF9800), // Laranja
                percentage = (notes.toFloat() / totalActivities) * 100f
            )
        )
    }
    
    if (other > 0) {
        chartData.add(
            ChartData(
                label = "Outros",
                value = other,
                color = Color(0xFF9E9E9E), // Cinza
                percentage = (other.toFloat() / totalActivities) * 100f
            )
        )
    }
    
    return chartData.sortedByDescending { it.value }
}
