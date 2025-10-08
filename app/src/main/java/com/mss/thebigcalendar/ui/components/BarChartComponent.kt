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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mss.thebigcalendar.R

data class BarChartData(
    val label: String,
    val value: Int,
    val color: Color
)

@Composable
fun BarChartComponent(
    data: List<BarChartData>,
    title: String = stringResource(id = R.string.bar_chart_default_title),
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.no_data_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val maxValue = data.maxOfOrNull { it.value } ?: 1

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
                    // Gráfico de barras com eixo Y
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Eixo Y com valores
            Column(
                modifier = Modifier
                    .width(30.dp)
                    .height(150.dp)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                // Valores do eixo Y (de cima para baixo)
                val yValues = if (maxValue > 0) {
                    listOf(maxValue, (maxValue * 3) / 4, maxValue / 2, maxValue / 4, 0)
                } else {
                    listOf(0, 0, 0, 0, 0)
                }
                
                yValues.forEach { value ->
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Gráfico principal
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp)
                    .padding(end = 16.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawBarChart(data, maxValue)
                }
            }
        }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Labels dos dias
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEach { item ->
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBarChart(
    data: List<BarChartData>,
    maxValue: Int
) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val barSpacing = canvasWidth * 0.1f / (data.size + 1) // 10% do espaço para espaçamento
    val barWidth = (canvasWidth - barSpacing * (data.size + 1)) / data.size
    val chartHeight = canvasHeight * 0.8f // 80% da altura para o gráfico
    val chartBottom = canvasHeight * 0.9f // 10% de margem inferior
    val chartTop = canvasHeight * 0.1f // 10% de margem superior
    
    // Desenhar linhas de grade horizontais
    val gridColor = Color.Gray.copy(alpha = 0.3f)
    val gridStrokeWidth = 1.dp.toPx()
    
    // Linhas de grade para valores 0, 25%, 50%, 75% e 100% do máximo
    val gridValues = listOf(0, maxValue / 4, maxValue / 2, (maxValue * 3) / 4, maxValue)
    gridValues.forEach { value ->
        val y = chartBottom - (value.toFloat() / maxValue) * chartHeight
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(canvasWidth, y),
            strokeWidth = gridStrokeWidth
        )
    }
    
    // Desenhar linha do eixo Y (linha vertical à esquerda)
    drawLine(
        color = Color.Gray.copy(alpha = 0.5f),
        start = Offset(0f, chartTop),
        end = Offset(0f, chartBottom),
        strokeWidth = 2.dp.toPx()
    )
    
    data.forEachIndexed { index, item ->
        val barHeight = if (maxValue > 0) {
            (item.value.toFloat() / maxValue) * chartHeight
        } else {
            0f
        }
        
        val left = barSpacing + index * (barWidth + barSpacing)
        val top = chartBottom - barHeight
        
        // Desenhar a barra
        drawRect(
            color = item.color,
            topLeft = Offset(left, top),
            size = Size(barWidth, barHeight)
        )
        
        // Desenhar uma borda sutil na barra
        drawRect(
            color = Color.Black.copy(alpha = 0.1f),
            topLeft = Offset(left, top),
            size = Size(barWidth, barHeight),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
fun BarChartLegend(
    data: List<BarChartData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(id = R.string.legend_label),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(item.color)
                    )
                    Text(
                        text = "${item.value}",
                        style = MaterialTheme.typography.bodySmall,
                        color = item.color,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
