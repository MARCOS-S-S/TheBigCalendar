package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarVisualizationSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: CalendarViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentScale = uiState.calendarScale
    val sliderValue = remember(currentScale) { mutableFloatStateOf(currentScale) }
    var hideOtherMonths by remember { mutableStateOf(uiState.hideOtherMonthDays) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.calendar_visualization_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.calendar_visualization_settings_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Tamanho do calendário", style = MaterialTheme.typography.bodyLarge)
                Text(text = "%.2fx".format(sliderValue.floatValue), style = MaterialTheme.typography.bodyMedium)
            }
            Slider(
                value = sliderValue.floatValue,
                onValueChange = { newValue ->
                    val clamped = newValue.coerceIn(0.6f, 1.22f)
                    sliderValue.floatValue = clamped
                    viewModel.setCalendarScale(clamped)
                },
                valueRange = 0.6f..1.22f,
                steps = 12
            )

            // Placeholder: Ocultar dias de outros meses (sem lógica aplicada ainda)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.hide_other_month_days),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(id = R.string.hide_other_month_days_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = hideOtherMonths,
                    onCheckedChange = { 
                        hideOtherMonths = it
                        viewModel.setHideOtherMonthDays(it)
                    }
                )
            }

            // Opção do tema escuro
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.sidebar_change_theme),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = when (uiState.theme) {
                        com.mss.thebigcalendar.data.model.Theme.LIGHT -> false
                        com.mss.thebigcalendar.data.model.Theme.DARK -> true
                        com.mss.thebigcalendar.data.model.Theme.SYSTEM -> isSystemInDarkTheme()
                    },
                    onCheckedChange = { isChecked ->
                        val newTheme = if (isChecked) {
                            com.mss.thebigcalendar.data.model.Theme.DARK
                        } else {
                            com.mss.thebigcalendar.data.model.Theme.LIGHT
                        }
                        viewModel.onThemeChange(newTheme)
                    }
                )
            }

            // Opção do preto puro
            val isDarkThemeActive = when (uiState.theme) {
                com.mss.thebigcalendar.data.model.Theme.DARK -> true
                com.mss.thebigcalendar.data.model.Theme.SYSTEM -> isSystemInDarkTheme()
                else -> false
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.pure_black_theme),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDarkThemeActive) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                    Text(
                        text = stringResource(id = R.string.pure_black_theme_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkThemeActive) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = uiState.pureBlackTheme,
                    onCheckedChange = { enabled ->
                        viewModel.setPureBlackTheme(enabled)
                    },
                    enabled = isDarkThemeActive
                )
            }

            // Opção da cor primária
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.primary_color),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(id = R.string.primary_color_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Seletor de cores customizado
                val predefinedColors = listOf(
                    "#6650a4", // Roxo padrão
                    "#1976d2", // Azul
                    "#388e3c", // Verde
                    "#f57c00", // Laranja
                    "#d32f2f", // Vermelho
                    "#7b1fa2", // Roxo escuro
                    "#0288d1", // Azul claro
                    "#689f38", // Verde claro
                    "#fbc02d", // Amarelo
                    "#e91e63", // Rosa
                    "#795548", // Marrom
                    "#607d8b"  // Azul acinzentado
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botão automático como primeira opção
                    item {
                        val isAutoSelected = uiState.primaryColor == "AUTO" // Modo automático
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = if (isAutoSelected) 3.dp else 1.dp,
                                    color = if (isAutoSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.setPrimaryColor("AUTO")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoMode,
                                contentDescription = "Automático",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Cores predefinidas
                    items(predefinedColors) { colorHex ->
                        val isSelected = uiState.primaryColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorHex)))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.setPrimaryColor(colorHex)
                                }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(id = R.string.changes_saved_automatically),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


