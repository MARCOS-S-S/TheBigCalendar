package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BirthdaysForSelectedDaySection(
    modifier: Modifier = Modifier,
    birthdays: List<Activity>,
    selectedDate: LocalDate,
    activityIdWithDeleteVisible: String?,
    onBirthdayClick: (Activity) -> Unit = {},
    onBirthdayLongClick: (String) -> Unit = {},
    onDeleteClick: (String) -> Unit = {},
    onCompleteClick: (String) -> Unit = {},
    onAddBirthdayClick: () -> Unit = {}
) {
    // Otimização: Usar derivedStateOf para evitar recálculos desnecessários
    val hasBirthdays by remember(birthdays) {
        derivedStateOf { birthdays.isNotEmpty() }
    }
    
    if (!hasBirthdays) return
    
    val dateFormat = stringResource(id = R.string.date_format_day_month)
    val dateFormatter = remember(dateFormat) { DateTimeFormatter.ofPattern(dateFormat, Locale("pt", "BR")) }
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Cake,
                contentDescription = null,
                tint = Color(0xFFE91E63), // Rosa para aniversários
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.birthdays_for, selectedDate.format(dateFormatter)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onAddBirthdayClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar aniversário",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Otimização: Usar Column com forEach para evitar LazyColumn aninhado
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            birthdays.forEach { birthday ->
                BirthdayItem(
                    birthday = birthday,
                    deleteButtonVisible = activityIdWithDeleteVisible == birthday.id,
                    onClick = { onBirthdayClick(birthday) },
                    onLongClick = { onBirthdayLongClick(birthday.id) },
                    onDeleteClick = { onDeleteClick(birthday.id) },
                    onCompleteClick = { onCompleteClick(birthday.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BirthdayItem(
    birthday: Activity,
    deleteButtonVisible: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Barra colorida rosa (similar à barra de prioridade das tarefas)
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .background(
                    color = Color(0xFFE91E63), // Rosa para aniversários
                    shape = RoundedCornerShape(2.dp)
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Informações do aniversário
        Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
            Text(
                text = birthday.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (!birthday.description.isNullOrBlank()) {
                Text(
                    text = birthday.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (birthday.location != null) {
                Text(
                    text = "📍 ${birthday.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // Indicador de evento recorrente
        if (!birthday.recurrenceRule.isNullOrEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Evento recorrente",
                tint = Color(0xFFE91E63),
                modifier = Modifier.size(20.dp)
            )
        }

        // Botões de ação (aparecem quando deleteButtonVisible é true)
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
                        .clickable { onCompleteClick() }
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
                        .clickable { onDeleteClick() }
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
