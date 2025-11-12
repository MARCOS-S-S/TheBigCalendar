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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.api.services.drive.model.File as DriveFile
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.repository.BackupFrequency
import com.mss.thebigcalendar.data.repository.BackupType
import com.mss.thebigcalendar.data.service.BackupInfo
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel
import com.mss.thebigcalendar.ui.components.StoragePermissionDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRestoreConfirmation by remember { mutableStateOf<BackupInfo?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<BackupInfo?>(null) }
    var showCloudRestoreConfirmation by remember { mutableStateOf<DriveFile?>(null) }
    var showCloudDeleteConfirmation by remember { mutableStateOf<DriveFile?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadBackupFiles()
        if (uiState.googleSignInAccount != null) {
            viewModel.listCloudBackups()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkStoragePermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.backup_options),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            item {
                BackupOptionItem(
                    title = stringResource(R.string.local_backup),
                    description = stringResource(R.string.local_backup_description),
                    icon = Icons.Default.Storage,
                    onClick = { viewModel.onBackupRequest() }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                BackupOptionItem(
                    title = stringResource(R.string.cloud_backup),
                    description = stringResource(R.string.cloud_backup_description),
                    icon = Icons.Default.CloudUpload,
                    onClick = { viewModel.createCloudBackup() },
                    enabled = uiState.googleSignInAccount != null
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                AutoBackupSettings(
                    settings = uiState.autoBackupSettings,
                    onSettingsChange = { viewModel.saveAutoBackupSettings(it) },
                    isCloudBackupEnabled = uiState.googleSignInAccount != null
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.backup_limitations_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                if (uiState.isBackingUp || uiState.isRestoring) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.isBackingUp) "Creating backup..." else "Restoring backup...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                uiState.backupMessage?.let { message ->
                    InfoMessageBox(message = message, isError = false)
                }
                uiState.restoreMessage?.let { message ->
                    InfoMessageBox(message = message, isError = message.contains("failed", ignoreCase = true))
                }
                uiState.cloudBackupError?.let { error ->
                    InfoMessageBox(message = error, isError = true)
                }
            }

            // Cloud Backups
            if (uiState.googleSignInAccount != null) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.cloud_backups_list),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                if (uiState.isListingCloudBackups) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (uiState.cloudBackupFiles.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.CloudDone,
                            title = stringResource(R.string.no_cloud_backups_yet),
                            description = stringResource(R.string.no_cloud_backups_description)
                        )
                    }
                } else {
                    items(uiState.cloudBackupFiles) { backupFile ->
                        CloudBackupFileItem(
                            backupFile = backupFile,
                            onDelete = { showCloudDeleteConfirmation = backupFile },
                            onRestore = { showCloudRestoreConfirmation = backupFile }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    EmptyState(
                        icon = Icons.Default.CloudOff,
                        title = stringResource(R.string.login_to_see_cloud_backups),
                        description = stringResource(R.string.login_to_see_cloud_backups_description)
                    )
                }
            }

            // Local Backups
            if (uiState.backupFiles.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.local_backups_list),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                items(uiState.backupFiles) { backupInfo ->
                    BackupFileItem(
                        backupInfo = backupInfo,
                        onDelete = { showDeleteConfirmation = backupInfo },
                        onRestore = { showRestoreConfirmation = backupInfo }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    EmptyState(
                        icon = Icons.Default.Backup,
                        title = stringResource(R.string.no_backups_yet),
                        description = stringResource(R.string.no_backups_description)
                    )
                }
            }
        }

        showRestoreConfirmation?.let { backupInfo ->
            RestoreConfirmationDialog(
                backupName = backupInfo.fileName,
                confirmationMessage = stringResource(
                    id = R.string.restore_backup_confirmation_message,
                    backupInfo.fileName,
                    backupInfo.totalActivities,
                    backupInfo.totalDeletedActivities
                ),
                onConfirm = {
                    viewModel.restoreFromBackup(backupInfo.filePath)
                    showRestoreConfirmation = null
                },
                onDismiss = { showRestoreConfirmation = null }
            )
        }

        showDeleteConfirmation?.let { backupInfo ->
            DeleteConfirmationDialog(
                backupName = backupInfo.fileName,
                onConfirm = {
                    viewModel.deleteBackupFile(backupInfo.filePath)
                    showDeleteConfirmation = null
                },
                onDismiss = { showDeleteConfirmation = null }
            )
        }

        showCloudRestoreConfirmation?.let { file ->
            RestoreConfirmationDialog(
                backupName = file.name,
                confirmationMessage = stringResource(id = R.string.restore_cloud_backup_confirmation_message, file.name),
                onConfirm = {
                    viewModel.restoreFromCloudBackup(file.id, file.name)
                    showCloudRestoreConfirmation = null
                },
                onDismiss = { showCloudRestoreConfirmation = null }
            )
        }

        showCloudDeleteConfirmation?.let { file ->
            DeleteConfirmationDialog(
                backupName = file.name,
                onConfirm = {
                    viewModel.deleteCloudBackup(file.id)
                    showCloudDeleteConfirmation = null
                },
                onDismiss = { showCloudDeleteConfirmation = null }
            )
        }

        if (uiState.needsStoragePermission) {
            StoragePermissionDialog(
                onDismiss = { viewModel.clearBackupMessage() },
                onPermissionGranted = {
                    viewModel.clearBackupMessage()
                    viewModel.onBackupRequest()
                    viewModel.loadBackupFiles()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoBackupSettings(
    settings: com.mss.thebigcalendar.data.repository.AutoBackupSettings,
    onSettingsChange: (com.mss.thebigcalendar.data.repository.AutoBackupSettings) -> Unit,
    isCloudBackupEnabled: Boolean
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.auto_backup_settings_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.auto_backup_enable),
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = settings.enabled,
                onCheckedChange = { onSettingsChange(settings.copy(enabled = it)) }
            )
        }

        if (settings.enabled) {
            Spacer(modifier = Modifier.height(16.dp))

            // Frequency Dropdown
            var frequencyExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = frequencyExpanded,
                onExpandedChange = { frequencyExpanded = !frequencyExpanded }
            ) {
                TextButton(onClick = { frequencyExpanded = true }) {
                    Text(text = stringResource(id = R.string.auto_backup_frequency) + ": " + when (settings.frequency) {
                        BackupFrequency.DAILY -> stringResource(id = R.string.auto_backup_frequency_daily)
                        BackupFrequency.TWO_DAYS -> stringResource(id = R.string.auto_backup_frequency_two_days)
                        BackupFrequency.WEEKLY -> stringResource(id = R.string.auto_backup_frequency_weekly)
                        BackupFrequency.MONTHLY -> stringResource(id = R.string.auto_backup_frequency_monthly)
                    })
                }
                ExposedDropdownMenu(
                    expanded = frequencyExpanded,
                    onDismissRequest = { frequencyExpanded = false }
                ) {
                    BackupFrequency.values().forEach { frequency ->
                        DropdownMenuItem(
                            text = { Text(text = when (frequency) {
                                BackupFrequency.DAILY -> stringResource(id = R.string.auto_backup_frequency_daily)
                                BackupFrequency.TWO_DAYS -> stringResource(id = R.string.auto_backup_frequency_two_days)
                                BackupFrequency.WEEKLY -> stringResource(id = R.string.auto_backup_frequency_weekly)
                                BackupFrequency.MONTHLY -> stringResource(id = R.string.auto_backup_frequency_monthly)
                            }) },
                            onClick = {
                                onSettingsChange(settings.copy(frequency = frequency))
                                frequencyExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Backup Type Dropdown
            var typeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                TextButton(onClick = { typeExpanded = true }) {
                    Text(text = stringResource(id = R.string.auto_backup_type) + ": " + when (settings.backupType) {
                        BackupType.LOCAL -> stringResource(id = R.string.auto_backup_type_local)
                        BackupType.CLOUD -> stringResource(id = R.string.auto_backup_type_cloud)
                    })
                }
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    BackupType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(text = when (type) {
                                BackupType.LOCAL -> stringResource(id = R.string.auto_backup_type_local)
                                BackupType.CLOUD -> stringResource(id = R.string.auto_backup_type_cloud)
                            }) },
                            onClick = {
                                onSettingsChange(settings.copy(backupType = type))
                                typeExpanded = false
                            },
                            enabled = if (type == BackupType.CLOUD) isCloudBackupEnabled else true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time Picker
            TextButton(onClick = { showTimePicker = true }) {
                Text(text = stringResource(id = R.string.auto_backup_time) + ": ${String.format("%02d:%02d", settings.hour, settings.minute)}")
            }

            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(
                    initialHour = settings.hour,
                    initialMinute = settings.minute,
                    is24Hour = true
                )
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    title = { Text(text = stringResource(id = R.string.auto_backup_time)) },
                    text = {
                        TimePicker(state = timePickerState)
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onSettingsChange(settings.copy(hour = timePickerState.hour, minute = timePickerState.minute))
                                showTimePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RestoreConfirmationDialog(
    backupName: String,
    confirmationMessage: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restore_backup_confirmation_title)) },
        text = { Text(confirmationMessage) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.restore_backup_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(backupName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_backup_confirmation_title)) },
        text = { Text(stringResource(R.string.delete_backup_confirmation_message, backupName)) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.delete_backup_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
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
            .background(if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        if (enabled) {
            IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Backup, contentDescription = stringResource(R.string.do_backup), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun CloudBackupFileItem(backupFile: DriveFile, onDelete: () -> Unit, onRestore: () -> Unit) {
    val appProperties = backupFile.appProperties ?: emptyMap()
    val totalActivities = appProperties["totalActivities"]?.toIntOrNull() ?: 0
    val totalDeleted = appProperties["totalDeletedActivities"]?.toIntOrNull() ?: 0
    val format = stringResource(id = R.string.backup_date_time_format)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = backupFile.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.cloud_backup_info_details, totalActivities, totalDeleted),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.backup_created_at, formatDriveDate(backupFile.createdTime.value, format)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onRestore, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Download, contentDescription = stringResource(R.string.restore_backup), tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_backup), tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun BackupFileItem(backupInfo: BackupInfo, onDelete: () -> Unit, onRestore: () -> Unit) {
    val format = stringResource(id = R.string.backup_date_time_format)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = backupInfo.fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.backup_info_details, backupInfo.totalActivities, backupInfo.totalDeletedActivities, formatFileSize(backupInfo.fileSize)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.backup_created_at, formatBackupDate(backupInfo.createdAt, format)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onRestore, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Download, contentDescription = stringResource(R.string.restore_backup), tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_backup), tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
fun InfoMessageBox(message: String, isError: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

private fun formatFileSize(sizeInBytes: Long): String {
    return when {
        sizeInBytes < 1024 -> "$sizeInBytes B"
        sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
        else -> "${sizeInBytes / (1024 * 1024)} MB"
    }
}

private fun formatBackupDate(dateString: String, format: String): String {
    return try {
        val dateTime = java.time.LocalDateTime.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern(format, Locale.getDefault())
        dateTime.format(formatter)
    } catch (e: Exception) {
        dateString
    }
}

private fun formatDriveDate(epochMillis: Long, format: String): String {
    return try {
        val date = Date(epochMillis)
        val displayFormatter = SimpleDateFormat(format, Locale.getDefault())
        displayFormatter.format(date)
    } catch (e: Exception) {
        "Invalid date"
    }
}
