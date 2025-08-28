package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    onBirthdayClick: (Activity) -> Unit = {},
    onAddBirthdayClick: () -> Unit = {}
) {
    if (birthdays.isEmpty()) return
    
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
                text = "Aniversários em ${selectedDate.format(dateFormatter)}",
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
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            birthdays.forEach { birthday ->
                BirthdayItem(
                    birthday = birthday,
                    onClick = { onBirthdayClick(birthday) }
                )
            }
        }
    }
}

@Composable
private fun BirthdayItem(
    birthday: Activity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick)
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
    }
}
