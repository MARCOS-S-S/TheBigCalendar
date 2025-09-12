package com.mss.thebigcalendar.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mss.thebigcalendar.ui.components.PieChartComponent
import com.mss.thebigcalendar.ui.components.BarChartComponent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.mss.thebigcalendar.R

import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    onBackClick: () -> Unit,
    activities: List<com.mss.thebigcalendar.data.model.Activity> = emptyList(),
    completedActivities: List<com.mss.thebigcalendar.data.model.Activity> = emptyList(),
    last7DaysData: List<com.mss.thebigcalendar.ui.components.BarChartData> = emptyList(),
    lastYearData: List<com.mss.thebigcalendar.ui.components.BarChartData> = emptyList(),
    currentMonth: YearMonth, // New parameter
    onBackPressedDispatcher: OnBackPressedDispatcher? = null,
    onNavigateToCompletedTasks: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                            Icons.Filled.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Estatísticas dos Agendamentos",
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Card de boas-vindas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Análise de Agendamentos",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aqui você encontrará estatísticas detalhadas sobre seus agendamentos, incluindo gráficos de produtividade, distribuição de atividades e muito mais.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                // Gráfico de barras dos últimos 7 dias
                BarChartComponent(
                    data = last7DaysData,
                    title = "Tarefas Concluídas - Últimos 7 Dias",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                // Gráfico de barras do último ano
                BarChartComponent(
                    data = lastYearData,
                    title = "Tarefas Concluídas - Último Ano",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                // Gráfico de pizza com distribuição de atividades
                PieChartComponent(
                    activities = activities,
                    currentMonth = currentMonth, // Pass the new parameter
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                // Card para estatísticas básicas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Estatísticas Básicas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Calcular estatísticas reais
                        val totalActivities = activities.size
                        val completedTasks = completedActivities.size
                        val scheduledEvents = activities.count { 
                            it.activityType == com.mss.thebigcalendar.data.model.ActivityType.EVENT && 
                            !it.isCompleted 
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = totalActivities.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Total de Atividades",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onNavigateToCompletedTasks() }
                            ) {
                                Text(
                                    text = completedTasks.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Tarefas Concluídas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = scheduledEvents.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "Eventos Agendados",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
