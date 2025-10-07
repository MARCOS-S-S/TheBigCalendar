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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Cake
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    onBackClick: () -> Unit,
    activities: List<Activity> = emptyList(),
    onBackPressedDispatcher: OnBackPressedDispatcher? = null,
    modifier: Modifier = Modifier
) {
    // Estado para controlar o tipo selecionado
    var selectedType by remember { mutableStateOf(ActivityType.NOTE) }
    var showDropdown by remember { mutableStateOf(false) }
    
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
                            imageVector = getIconForType(selectedType),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(id = R.string.schedules_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val filteredActivities = activities.filter { it.activityType == selectedType }
                            if (filteredActivities.isNotEmpty()) {
                                Text(
                                    text = "${filteredActivities.size} ${getCountTextForType(selectedType)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                actions = {
                    // Seletor de tipo
                    Box {
                        TextButton(
                            onClick = { showDropdown = true }
                        ) {
                            Text(
                                text = stringResource(id = R.string.filter_button),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false }
                        ) {
                            ActivityType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = getIconForType(type),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(getTitleForType(type))
                                        }
                                    },
                                    onClick = {
                                        selectedType = type
                                        showDropdown = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val filteredActivities = activities.filter { it.activityType == selectedType }
            
            if (filteredActivities.isEmpty()) {
                // Tela vazia quando não há atividades do tipo selecionado
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = getIconForType(selectedType),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.no_items_found, getTitleForType(selectedType).lowercase()),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.create_first_item, getTitleForType(selectedType).lowercase()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Lista de atividades filtradas
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Agrupar atividades por data e ordenar
                    val activitiesByDate = filteredActivities.groupBy { 
                        LocalDate.parse(it.date)
                    }.toSortedMap(compareByDescending { it })
                        .mapValues { (_, activitiesForDate) ->
                            // Ordenar atividades dentro de cada data por horário (se tiver) ou por título
                            activitiesForDate.sortedWith(compareBy<Activity> { 
                                it.startTime ?: LocalTime.MIN
                            }.thenBy { it.title })
                        }
                    
                    items(activitiesByDate.toList()) { (date, activitiesForDate) ->
                        ActivityDateSection(
                            date = date,
                            activities = activitiesForDate,
                            activityType = selectedType
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityDateSection(
    date: LocalDate,
    activities: List<Activity>,
    activityType: ActivityType
) {
    val formatter = DateTimeFormatter.ofPattern(stringResource(id = R.string.date_format_full), Locale.getDefault())
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    
    Column {
        // Cabeçalho da data
        Text(
            text = "$dayOfWeek, ${date.format(formatter)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Atividades do dia
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activities.forEach { activity ->
                ActivityCard(activity = activity, activityType = activityType)
            }
        }
    }
}

@Composable
private fun ActivityCard(
    activity: Activity,
    activityType: ActivityType
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* TODO: Implementar edição da atividade */ }
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Ícone da atividade baseado no tipo
            Icon(
                imageVector = getIconForType(activityType),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = getColorForType(activityType)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Título da atividade
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Descrição da atividade
                activity.description?.let { description ->
                    if (description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                // Informações adicionais
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Horário se não for dia inteiro
                    if (!activity.isAllDay && activity.startTime != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getIconForType(activityType),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activity.startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Localização se tiver
                    activity.location?.let { location ->
                        if (location.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = getIconForType(activityType),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = location,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Funções auxiliares para mapear tipos de atividade
private fun getIconForType(type: ActivityType) = when (type) {
    ActivityType.EVENT -> Icons.Default.Event
    ActivityType.TASK -> Icons.Default.Assignment
    ActivityType.NOTE -> Icons.Default.Note
    ActivityType.BIRTHDAY -> Icons.Default.Cake
}

@Composable
private fun getTitleForType(type: ActivityType) = when (type) {
    ActivityType.EVENT -> stringResource(id = R.string.events_title)
    ActivityType.TASK -> stringResource(id = R.string.tasks_title)
    ActivityType.NOTE -> stringResource(id = R.string.notes_title)
    ActivityType.BIRTHDAY -> stringResource(id = R.string.birthdays_title)
}

@Composable
private fun getCountTextForType(type: ActivityType) = when (type) {
    ActivityType.EVENT -> stringResource(id = R.string.events_count)
    ActivityType.TASK -> stringResource(id = R.string.tasks_count)
    ActivityType.NOTE -> stringResource(id = R.string.notes_count)
    ActivityType.BIRTHDAY -> stringResource(id = R.string.birthdays_count)
}

private fun getColorForType(type: ActivityType) = when (type) {
    ActivityType.EVENT -> androidx.compose.ui.graphics.Color(0xFF2196F3) // Azul
    ActivityType.TASK -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Verde
    ActivityType.NOTE -> androidx.compose.ui.graphics.Color(0xFF9C27B0) // Roxo
    ActivityType.BIRTHDAY -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Laranja
}
