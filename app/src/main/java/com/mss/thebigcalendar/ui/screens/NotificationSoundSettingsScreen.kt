package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.data.model.NotificationSoundSettings
import com.mss.thebigcalendar.data.model.NotificationSoundType
import com.mss.thebigcalendar.data.model.VisibilityLevel
import android.provider.CalendarContract.Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSoundSettingsScreen(
    currentSettings: NotificationSoundSettings,
    onSettingsChanged: (NotificationSoundSettings) -> Unit,
    onBackClick: () -> Unit
) {
    var lowVisibilitySound by remember { mutableStateOf(currentSettings.lowVisibilitySound) }
    var mediumVisibilitySound by remember { mutableStateOf(currentSettings.mediumVisibilitySound) }
    var highVisibilitySound by remember { mutableStateOf(currentSettings.highVisibilitySound) }

    // Atualizar configurações quando os valores mudarem
    LaunchedEffect(lowVisibilitySound, mediumVisibilitySound, highVisibilitySound) {
        onSettingsChanged(
            NotificationSoundSettings(
                lowVisibilitySound = lowVisibilitySound,
                mediumVisibilitySound = mediumVisibilitySound,
                highVisibilitySound = highVisibilitySound
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                    contentDescription = "Voltar"
                )
            }
            Text(
                text = "Sons de Notificação",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Descrição
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Configure o som de notificação para cada nível de visibilidade:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Baixa: Notificação padrão do sistema",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "• Média: Banner na tela com som personalizado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "• Alta: Alerta de tela cheia com som personalizado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Configurações por nível de visibilidade
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SoundSelectionCard(
                    title = "Visibilidade Baixa",
                    description = "Som para notificações padrão",
                    currentSound = lowVisibilitySound,
                    onSoundSelected = { lowVisibilitySound = it },
                    levelColor = Color.Green
                )
            }
            
            item {
                SoundSelectionCard(
                    title = "Visibilidade Média",
                    description = "Som para banners de notificação",
                    currentSound = mediumVisibilitySound,
                    onSoundSelected = { mediumVisibilitySound = it },
                    levelColor = Color.Yellow
                )
            }
            
            item {
                SoundSelectionCard(
                    title = "Visibilidade Alta",
                    description = "Som para alertas de tela cheia",
                    currentSound = highVisibilitySound,
                    onSoundSelected = { highVisibilitySound = it },
                    levelColor = Color.Red
                )
            }
        }
    }
}

@Composable
private fun SoundSelectionCard(
    title: String,
    description: String,
    currentSound: String,
    onSoundSelected: (String) -> Unit,
    levelColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(levelColor, shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de opções de som
            NotificationSoundType.values().forEach { soundType ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = currentSound == soundType.soundResource,
                            onClick = { onSoundSelected(soundType.soundResource) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentSound == soundType.soundResource,
                        onClick = { onSoundSelected(soundType.soundResource) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = soundType.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
