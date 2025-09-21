
package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.data.model.ViewMode
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.ui.components.GreetingService
import com.mss.thebigcalendar.ui.components.rememberQuoteOfTheDay
import androidx.compose.ui.platform.LocalContext


private val filterItems = listOf(
    "showHolidays" to R.string.national_holidays,
    "showSaintDays" to R.string.catholic_saint_days,
    "showEvents" to R.string.events,
    "showTasks" to R.string.tasks,
    "showBirthdays" to R.string.birthday,
    "showNotes" to R.string.note
)

@Composable
fun Sidebar(
    uiState: com.mss.thebigcalendar.data.model.CalendarUiState,
    onViewModeChange: (ViewMode) -> Unit,
    onFilterChange: (key: String, value: Boolean) -> Unit,
    onNavigateToSettings: (String) -> Unit,
    onBackup: () -> Unit,
    onNotesClick: () -> Unit,
    onAlarmsClick: () -> Unit,
    onRequestClose: () -> Unit
) {
    val context = LocalContext.current
    val quoteOfTheDay = rememberQuoteOfTheDay(context)
    
    // Obter o primeiro nome do usuário do Google Sign-In
    val userName = uiState.googleSignInAccount?.displayName?.let { fullName ->
        GreetingService.getFirstName(fullName)
    }
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.width(320.dp)
                .padding(NavigationDrawerItemDefaults.ItemPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Cabeçalho com mensagem de boas-vindas e botão de fechar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = GreetingService.getFullGreetingMessage(uiState.welcomeName),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onRequestClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.close_menu),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Frase do dia
            quoteOfTheDay?.let { quote ->
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${quote.frase}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "— ${quote.autor}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seção de Visualização (Mensal/Anual)
            Text(
                text = stringResource(id = R.string.visualization),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NavigationDrawerItem(
                label = { Text(stringResource(id = R.string.monthly)) },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                selected = uiState.viewMode == ViewMode.MONTHLY,
                onClick = { onViewModeChange(ViewMode.MONTHLY) }
            )
            NavigationDrawerItem(
                label = { Text(stringResource(id = R.string.yearly)) },
                icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                selected = uiState.viewMode == ViewMode.YEARLY,
                onClick = { onViewModeChange(ViewMode.YEARLY) }
            )
            NavigationDrawerItem(
                label = { Text(stringResource(id = R.string.notes_menu)) },
                icon = { Icon(Icons.Filled.Note, contentDescription = null) },
                selected = false,
                onClick = { onNotesClick() }
            )
            NavigationDrawerItem(
                label = { Text(stringResource(id = R.string.alarms)) },
                icon = { Icon(Icons.Filled.Alarm, contentDescription = null) },
                selected = false,
                onClick = { onAlarmsClick() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Seção de Filtros
            Text(
                text = stringResource(id = R.string.show_on_calendar),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Cache dos recursos de string para evitar recomposição
            val filterLabels = remember {
                filterItems.associate { (key, labelResId) -> key to labelResId }
            }
            
            filterLabels.forEach { (key, labelResId) ->
                val isChecked = when (key) {
                    "showHolidays" -> uiState.filterOptions.showHolidays
                    "showSaintDays" -> uiState.filterOptions.showSaintDays
                    "showEvents" -> uiState.filterOptions.showEvents
                    "showTasks" -> uiState.filterOptions.showTasks
                    "showBirthdays" -> uiState.filterOptions.showBirthdays
                    "showNotes" -> uiState.filterOptions.showNotes
                    else -> false
                }
                FilterCheckboxItem(
                    label = stringResource(id = labelResId),
                    checked = isChecked,
                    onCheckedChange = { onFilterChange(key, it) }
                )
            }
            
            // Opção para mostrar tarefas finalizadas
            FilterCheckboxItem(
                label = stringResource(id = R.string.completed_tasks_filter),
                checked = uiState.showCompletedActivities,
                onCheckedChange = { onFilterChange("showCompletedActivities", it) }
            )
            
            // Opção para mostrar fases da lua
            FilterCheckboxItem(
                label = stringResource(id = R.string.moon_phases_filter),
                checked = uiState.showMoonPhases,
                onCheckedChange = { onFilterChange("showMoonPhases", it) }
            )
            
            // Calendários JSON importados
            if (uiState.jsonCalendars.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Calendários Importados",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                uiState.jsonCalendars.forEach { jsonCalendar ->
                    FilterCheckboxItem(
                        label = jsonCalendar.title,
                        checked = jsonCalendar.isVisible,
                        onCheckedChange = { isVisible ->
                            onFilterChange("jsonCalendar_${jsonCalendar.id}", isVisible)
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Seção de Configurações
            Text(
                text = stringResource(id = R.string.settings),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NavigationDrawerItem(
                label = { Text(stringResource(id = R.string.general)) },
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                selected = false,
                onClick = { onNavigateToSettings("General") }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Seção de Backup
            Text(
                text = stringResource(id = R.string.backup),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NavigationDrawerItem(
                label = { Text(stringResource(id = R.string.do_backup)) },
                icon = { Icon(Icons.Outlined.Backup, contentDescription = null) },
                selected = false,
                onClick = { onBackup() }
            )
        }
    }
}

/**
 * Versão 1: Linha toda clicável, Checkbox só reflete o estado.
 * (Se preferir só o clique no Checkbox, use a versão comentada abaixo)
 */
@Composable
private fun FilterCheckboxItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            enabled = true
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}