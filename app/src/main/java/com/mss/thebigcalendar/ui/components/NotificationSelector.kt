package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.data.model.NotificationSettings
import com.mss.thebigcalendar.data.model.NotificationType
import com.mss.thebigcalendar.data.model.getDescription

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSelector(
    notificationSettings: NotificationSettings,
    onNotificationSettingsChanged: (NotificationSettings) -> Unit,
    isCustomized: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showCustomTimeDialog by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf(notificationSettings.customMinutesBefore?.toString() ?: "15") }

    Column(modifier = modifier) {
        // Cabeçalho com toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (notificationSettings.isEnabled) {
                        Icons.Default.Notifications
                    } else {
                        Icons.Default.NotificationsOff
                    },
                    contentDescription = "Notificações",
                    tint = if (notificationSettings.isEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = "Notificações",
                    style = MaterialTheme.typography.labelMedium
                )
                if (isCustomized) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Configuração personalizada",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Switch(
                checked = notificationSettings.isEnabled,
                onCheckedChange = { enabled ->
                    onNotificationSettingsChanged(
                        notificationSettings.copy(
                            isEnabled = enabled,
                            notificationType = if (enabled) NotificationType.FIFTEEN_MINUTES_BEFORE else NotificationType.NONE
                        )
                    )
                }
            )
        }

        // Opções de notificação (visível apenas quando habilitado)
        if (notificationSettings.isEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Seletor de tipo de notificação
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Tipo de notificação",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = notificationSettings.notificationType.getDescription(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                DropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false }
                ) {
                    NotificationType.values().forEach { notificationType ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = notificationType.getDescription(),
                                    color = if (notificationType == notificationSettings.notificationType) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            },
                            onClick = {
                                onNotificationSettingsChanged(
                                    notificationSettings.copy(
                                        notificationType = notificationType,
                                        customMinutesBefore = if (notificationType == NotificationType.CUSTOM) {
                                            customMinutes.toIntOrNull() ?: 15
                                        } else {
                                            null
                                        }
                                    )
                                )
                                isExpanded = false
                                
                                if (notificationType == NotificationType.CUSTOM) {
                                    showCustomTimeDialog = true
                                }
                            }
                        )
                    }
                }
            }

            // Campo personalizado para minutos (visível apenas para tipo CUSTOM)
            if (notificationSettings.notificationType == NotificationType.CUSTOM) {
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { 
                        customMinutes = it
                        onNotificationSettingsChanged(
                            notificationSettings.copy(
                                customMinutesBefore = it.toIntOrNull() ?: 15
                            )
                        )
                    },
                    label = { Text("Minutos antes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        }
    }

    // Dialog para configurar tempo personalizado
    if (showCustomTimeDialog) {
        AlertDialog(
            onDismissRequest = { showCustomTimeDialog = false },
            title = { Text("Tempo personalizado") },
            text = { 
                Column {
                    Text("Digite quantos minutos antes da atividade você quer ser notificado:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { customMinutes = it },
                        label = { Text("Minutos") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minutes = customMinutes.toIntOrNull() ?: 15
                        onNotificationSettingsChanged(
                            notificationSettings.copy(
                                customMinutesBefore = minutes
                            )
                        )
                        showCustomTimeDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTimeDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
