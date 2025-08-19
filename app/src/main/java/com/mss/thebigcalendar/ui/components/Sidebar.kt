package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.data.model.ViewMode

private val filterItems = listOf(
    "showHolidays" to "Feriados nacionais",
    "showSaintDays" to "Dias de Santos Católicos",
    "showCommemorativeDates" to "Datas comemorativas",
    "showEvents" to "Eventos",
    "showTasks" to "Tarefas"
)

@Composable
fun Sidebar(
    uiState: com.mss.thebigcalendar.data.model.CalendarUiState,
    onViewModeChange: (ViewMode) -> Unit,
    onFilterChange: (key: String, value: Boolean) -> Unit,
    onThemeChange: (Theme) -> Unit,
    onOpenSettingsModal: (String) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onRequestClose: () -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .padding(NavigationDrawerItemDefaults.ItemPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Cabeçalho com o botão de fechar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onRequestClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar menu"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Seção de Tema (Modo Escuro/Claro)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (uiState.theme == Theme.LIGHT) "Modo escuro" else "Modo claro",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = uiState.theme == Theme.DARK,
                    onCheckedChange = { isChecked ->
                        onThemeChange(if (isChecked) Theme.DARK else Theme.LIGHT)
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Seção de Visualização (Mensal/Anual)
            Text(
                text = "Visualização",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NavigationDrawerItem(
                label = { Text("Mensal") },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                selected = uiState.viewMode == ViewMode.MONTHLY,
                onClick = { onViewModeChange(ViewMode.MONTHLY) }
            )
            NavigationDrawerItem(
                label = { Text("Anual") },
                icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                selected = uiState.viewMode == ViewMode.YEARLY,
                onClick = { onViewModeChange(ViewMode.YEARLY) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Seção de Filtros
            Text(
                text = "Mostrar no Calendário",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            filterItems.forEach { (key, label) ->
                val isChecked = when (key) {
                    "showHolidays" -> uiState.filterOptions.showHolidays
                    "showSaintDays" -> uiState.filterOptions.showSaintDays
                    "showCommemorativeDates" -> uiState.filterOptions.showCommemorativeDates
                    "showEvents" -> uiState.filterOptions.showEvents
                    "showTasks" -> uiState.filterOptions.showTasks
                    else -> false
                }
                FilterCheckboxItem(
                    label = label,
                    checked = isChecked,
                    onCheckedChange = { onFilterChange(key, it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Seção de Configurações
            Text(
                text = "Configurações",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NavigationDrawerItem(
                label = { Text("Geral") },
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                selected = false,
                onClick = { onOpenSettingsModal("General") }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Seção de Backup
            Text(
                text = "Backup e Restauração",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NavigationDrawerItem(
                label = { Text("Fazer Backup") },
                icon = { Icon(Icons.Outlined.Backup, contentDescription = null) },
                selected = false,
                onClick = onBackup
            )
            NavigationDrawerItem(
                label = { Text("Restaurar Backup") },
                icon = { Icon(Icons.Outlined.Restore, contentDescription = null) },
                selected = false,
                onClick = onRestore
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
            onCheckedChange = null,
            enabled = false // Desabilita interação direta no checkbox
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
