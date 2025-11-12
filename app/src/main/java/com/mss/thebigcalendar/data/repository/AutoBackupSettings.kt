package com.mss.thebigcalendar.data.repository

data class AutoBackupSettings(
    val enabled: Boolean,
    val frequency: BackupFrequency,
    val hour: Int,
    val minute: Int,
    val backupType: BackupType
)
