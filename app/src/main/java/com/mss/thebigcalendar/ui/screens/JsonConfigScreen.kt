package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mss.thebigcalendar.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonConfigScreen(
    fileName: String?,
    onBackClick: () -> Unit,
    onSaveClick: (String, Color) -> Unit,
    onSelectFileClick: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.Blue) }
    var showColorPicker by remember { mutableStateOf(false) }
    
    val colors = listOf(
        Color.Red to stringResource(R.string.color_red),
        Color.Blue to stringResource(R.string.color_blue), 
        Color.Green to stringResource(R.string.color_green),
        Color.Magenta to stringResource(R.string.color_magenta),
        Color.Cyan to stringResource(R.string.color_cyan),
        Color.Yellow to stringResource(R.string.color_yellow)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.configure_calendar)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
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
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seção de seleção de arquivo
            if (fileName != null) {
                // Informações do arquivo selecionado
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.selected_file),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // Botão para selecionar arquivo
                Button(
                    onClick = onSelectFileClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.select_file))
                }
            }

            // Campo de título
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.calendar_title)) },
                placeholder = { Text(stringResource(R.string.calendar_title_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )

            // Seleção de cor
            Text(
                text = stringResource(R.string.day_color),
                style = MaterialTheme.typography.labelLarge
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Preview da cor selecionada
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    color = selectedColor,
                    border = BorderStroke(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                ) {}
                
                // Botão para abrir seletor de cor
                OutlinedButton(
                    onClick = { 
                        showColorPicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.choose_color))
                }
            }
            
            // Cores predefinidas como alternativa
            Text(
                text = stringResource(R.string.suggested_colors),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colors.forEach { (color, name) ->
                    Surface(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        color = color,
                        onClick = { selectedColor = color },
                        border = BorderStroke(
                            width = if (selectedColor == color) 3.dp else 1.dp,
                            color = if (selectedColor == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão de salvar
            Button(
                onClick = { onSaveClick(title, selectedColor) },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && fileName != null
            ) {
                Text(stringResource(R.string.save_configuration))
            }
        }
    }
    
    // Dialog do seletor de cores
    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = selectedColor,
            onColorSelected = { color ->
                selectedColor = color
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var tempColor by remember { mutableStateOf(currentColor) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.choose_color_dialog),
                    style = MaterialTheme.typography.headlineSmall
                )
                
                // Preview da cor
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        color = tempColor,
                        border = BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    ) {}
                    
                    Text(
                        text = stringResource(R.string.selected_color),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Paleta de cores
                Text(
                    text = stringResource(R.string.available_colors),
                    style = MaterialTheme.typography.labelLarge
                )
                
                val colorPalette = listOf(
                    Color.Red, Color.Blue, Color.Green, Color.Yellow,
                    Color.Magenta, Color.Cyan, Color(0xFFFF9800), Color(0xFF9C27B0),
                    Color(0xFF795548), Color(0xFF607D8B), Color(0xFFE91E63), Color(0xFF00BCD4),
                    Color(0xFF4CAF50), Color(0xFFFFEB3B), Color(0xFFFF5722), Color(0xFF3F51B5)
                )
                
                // Grid de cores
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorPalette.chunked(4).forEach { rowColors ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowColors.forEach { color ->
                                Surface(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    color = color,
                                    onClick = { tempColor = color },
                                    border = BorderStroke(
                                        width = if (tempColor == color) 3.dp else 1.dp,
                                        color = if (tempColor == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                ) {}
                            }
                        }
                    }
                }
                
                // Botões
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    
                    Button(
                        onClick = { onColorSelected(tempColor) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}
