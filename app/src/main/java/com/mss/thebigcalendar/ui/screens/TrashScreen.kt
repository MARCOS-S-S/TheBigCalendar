package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.DeletedActivity
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val deletedActivities = uiState.deletedActivities

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (deletedActivities.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearAllDeletedActivities() }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.empty_trash)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (deletedActivities.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.deleted_activities_count, deletedActivities.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(deletedActivities) { deletedActivity ->
                        DeletedActivityItem(
                            deletedActivity = deletedActivity,
                            onRestore = { viewModel.restoreDeletedActivity(deletedActivity.id) },
                            onDeletePermanently = { viewModel.removeDeletedActivity(deletedActivity.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                // Lixeira vazia
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.trash_empty),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.trash_empty_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeletedActivityItem(
    deletedActivity: DeletedActivity,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val activity = deletedActivity.originalActivity
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ícone baseado no tipo de atividade
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (activity.activityType) {
                        com.mss.thebigcalendar.data.model.ActivityType.EVENT -> MaterialTheme.colorScheme.primaryContainer
                        com.mss.thebigcalendar.data.model.ActivityType.TASK -> MaterialTheme.colorScheme.secondaryContainer
                        com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY -> MaterialTheme.colorScheme.tertiaryContainer
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (activity.activityType) {
                    com.mss.thebigcalendar.data.model.ActivityType.EVENT -> "📅"
                    com.mss.thebigcalendar.data.model.ActivityType.TASK -> "📋"
                    com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY -> "🎂"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Conteúdo da atividade
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = activity.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = when (activity.activityType) {
                    com.mss.thebigcalendar.data.model.ActivityType.EVENT -> stringResource(R.string.event)
                    com.mss.thebigcalendar.data.model.ActivityType.TASK -> stringResource(R.string.task)
                    com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY -> stringResource(R.string.birthday)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (activity.date.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(activity.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.deleted_at, formatDateTime(deletedActivity.deletedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Botões de ação
        Column {
            IconButton(
                onClick = onRestore,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.RestoreFromTrash,
                    contentDescription = stringResource(R.string.restore_activity),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            IconButton(
                onClick = onDeletePermanently,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_permanently),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
        date.format(formatter).replaceFirstChar { it.titlecase(Locale("pt", "BR")) }
    } catch (e: Exception) {
        dateString
    }
}

private fun formatDateTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
    return dateTime.format(formatter)
}
