package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Storage
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
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
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
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Título da seção
            Text(
                text = stringResource(R.string.backup_options),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Opção de Backup Local
            BackupOptionItem(
                title = stringResource(R.string.local_backup),
                description = stringResource(R.string.local_backup_description),
                icon = Icons.Default.Storage,
                onClick = { viewModel.onBackupRequest() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Opção de Backup na Nuvem (desabilitada por enquanto)
            BackupOptionItem(
                title = stringResource(R.string.cloud_backup),
                description = stringResource(R.string.cloud_backup_description),
                icon = Icons.Default.CloudUpload,
                onClick = { /* Implementar futuramente */ },
                enabled = false
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Mensagem de backup se existir
            uiState.backupMessage?.let { message ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (message.contains("sucesso", ignoreCase = true)) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (message.contains("sucesso", ignoreCase = true)) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BackupOptionItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ícone
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (enabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Conteúdo
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Botão de ação
        if (enabled) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Backup,
                    contentDescription = stringResource(R.string.do_backup),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
