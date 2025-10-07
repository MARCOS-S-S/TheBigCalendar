
package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.data.model.ViewMode
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.ui.components.GreetingService
import com.mss.thebigcalendar.ui.components.rememberQuoteOfTheDay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle


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
    onRequestClose: () -> Unit,
    onDeleteJsonCalendar: (com.mss.thebigcalendar.data.model.JsonCalendar) -> Unit,
    onToggleSidebarFilterVisibility: (String) -> Unit
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
        val scrollState = rememberScrollState()
        
        Box(
            modifier = Modifier.width(320.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .verticalScroll(scrollState)
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
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic
                        ),
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

            // Linha de separação entre frases e visualização
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

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
                onClick = { onViewModeChange(ViewMode.MONTHLY) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                )
            )
            NavigationDrawerItem(
                label = { Text(stringResource(id = R.string.yearly)) },
                icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                selected = uiState.viewMode == ViewMode.YEARLY,
                onClick = { onViewModeChange(ViewMode.YEARLY) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                )
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
                val isVisible = when (key) {
                    "showHolidays" -> uiState.sidebarFilterVisibility.showHolidays
                    "showSaintDays" -> uiState.sidebarFilterVisibility.showSaintDays
                    "showEvents" -> uiState.sidebarFilterVisibility.showEvents
                    "showTasks" -> uiState.sidebarFilterVisibility.showTasks
                    "showBirthdays" -> uiState.sidebarFilterVisibility.showBirthdays
                    "showNotes" -> uiState.sidebarFilterVisibility.showNotes
                    else -> true
                }
                
                if (isVisible) {
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
                        onCheckedChange = { onFilterChange(key, it) },
                        onLongPress = { onToggleSidebarFilterVisibility(key) }
                    )
                }
            }
            
            // Opção para mostrar tarefas finalizadas
            if (uiState.sidebarFilterVisibility.showCompletedTasks) {
                FilterCheckboxItem(
                    label = stringResource(id = R.string.completed_tasks_filter),
                    checked = uiState.showCompletedActivities,
                    onCheckedChange = { onFilterChange("showCompletedActivities", it) },
                    onLongPress = { onToggleSidebarFilterVisibility("showCompletedActivities") }
                )
            }
            
            // Opção para mostrar fases da lua
            if (uiState.sidebarFilterVisibility.showMoonPhases) {
                FilterCheckboxItem(
                    label = stringResource(id = R.string.moon_phases_filter),
                    checked = uiState.showMoonPhases,
                    onCheckedChange = { onFilterChange("showMoonPhases", it) },
                    onLongPress = { onToggleSidebarFilterVisibility("showMoonPhases") }
                )
            }
            
            // Calendários JSON importados
            if (uiState.jsonCalendars.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = stringResource(id = R.string.imported_calendars),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                uiState.jsonCalendars.forEach { jsonCalendar ->
                    JsonCalendarItem(
                        jsonCalendar = jsonCalendar,
                        checked = jsonCalendar.isVisible,
                        onCheckedChange = { isVisible ->
                            onFilterChange("jsonCalendar_${jsonCalendar.id}", isVisible)
                        },
                        onDeleteClick = {
                            onDeleteJsonCalendar(jsonCalendar)
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
            
            // Scrollbar customizada seguindo Material Design
            CustomScrollbar(
                scrollState = scrollState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            )
        }
    }
}

/**
 * Componente de filtro com suporte a long press para mostrar ícone X
 */
@Composable
private fun FilterCheckboxItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onLongPress: () -> Unit = {}
) {
    var showRemoveIcon by remember { mutableStateOf(false) }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (!showRemoveIcon) {
                    onCheckedChange(!checked) 
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        showRemoveIcon = true
                    },
                    onTap = {
                        if (showRemoveIcon) {
                            showRemoveIcon = false
                        }
                    }
                )
            }
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
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        // Ícone X que aparece após long press
        if (showRemoveIcon) {
            IconButton(
                onClick = { 
                    showRemoveIcon = false
                    onLongPress() // Remove do sidebar
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remover do menu",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Item para calendários JSON com botão de deletar
 */
@Composable
private fun JsonCalendarItem(
    jsonCalendar: com.mss.thebigcalendar.data.model.JsonCalendar,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit
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
            text = jsonCalendar.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remover calendário",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Scrollbar customizada
 */
@Composable
private fun CustomScrollbar(
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var isHovered by remember { mutableFloatStateOf(0f) }
    
    // Obter cor da scrollbar baseada no tema Material Design
    val scrollbarColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = (0.1f + isHovered * 0.2f).coerceIn(0.1f, 0.3f)
    )
    
    // Anima a opacidade da scrollbar
    LaunchedEffect(isHovered) {
        // Implementação simples de hover - pode ser expandida
    }
    
    Canvas(
        modifier = modifier
            .width(4.dp)
            .pointerInput(Unit) {
                // Detectar hover/interação se necessário
            }
    ) {
        val canvasHeight = size.height
        val canvasWidth = size.width
        
        // Calcular dimensões da scrollbar
        val scrollbarThickness = with(density) { 4.dp.toPx() }
        val scrollbarPadding = with(density) { 2.dp.toPx() }
        
        // Calcular posição e tamanho do thumb
        val maxScrollValue = scrollState.maxValue.toFloat()
        val currentScrollValue = scrollState.value.toFloat()
        
        if (maxScrollValue > 0) {
            val thumbHeight = (canvasHeight * canvasHeight / (canvasHeight + maxScrollValue)).coerceAtLeast(scrollbarThickness * 2)
            val thumbTop = (currentScrollValue / maxScrollValue) * (canvasHeight - thumbHeight)
            
            // Desenhar o thumb da scrollbar
            drawRoundRect(
                color = scrollbarColor,
                topLeft = Offset(
                    x = (canvasWidth - scrollbarThickness) / 2,
                    y = thumbTop + scrollbarPadding
                ),
                size = Size(
                    width = scrollbarThickness,
                    height = thumbHeight - scrollbarPadding * 2
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(scrollbarThickness / 2)
            )
        }
    }
}