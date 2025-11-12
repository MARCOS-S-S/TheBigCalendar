package com.mss.thebigcalendar.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.DeletedActivity
import com.mss.thebigcalendar.data.repository.ActivityRepository
import com.mss.thebigcalendar.data.repository.CompletedActivityRepository
import com.mss.thebigcalendar.data.repository.BackupFrequency
import com.mss.thebigcalendar.data.repository.BackupType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.mss.thebigcalendar.service.GoogleDriveService
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.mss.thebigcalendar.data.repository.DeletedActivityRepository
import java.time.Instant
import com.google.api.services.drive.model.File as DriveFile

class BackupService(
    private val context: Context,
    private val activityRepository: ActivityRepository,
    private val deletedActivityRepository: DeletedActivityRepository,
    private val completedActivityRepository: CompletedActivityRepository
) {

    private fun getGoogleDriveService(account: GoogleSignInAccount): GoogleDriveService {
        return GoogleDriveService(context, account)
    }

    suspend fun createCloudBackup(account: GoogleSignInAccount): Result<String> = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile(BACKUP_FILE_PREFIX, BACKUP_FILE_EXTENSION, context.cacheDir)
        try {
            val activities = activityRepository.activities.first()
            val deletedActivities = deletedActivityRepository.deletedActivities.first()
            val completedActivities = completedActivityRepository.completedActivities.first()

            val backupData = createBackupJson(activities, deletedActivities, completedActivities)
            tempFile.writeText(backupData, Charsets.UTF_8)

            val appProperties = mapOf(
                "totalActivities" to activities.size.toString(),
                "totalDeletedActivities" to deletedActivities.size.toString(),
                "totalCompletedActivities" to completedActivities.size.toString()
            )

            val driveService = getGoogleDriveService(account)
            val uploadedFile = driveService.uploadBackupFile(tempFile, appProperties)

            if (uploadedFile != null) {
                Result.success(uploadedFile.id)
            } else {
                Result.failure(Exception("Falha ao fazer upload do arquivo de backup para o Google Drive"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    suspend fun listCloudBackupFiles(account: GoogleSignInAccount): Result<List<DriveFile>> = withContext(Dispatchers.IO) {
        try {
            val driveService = getGoogleDriveService(account)
            val files = driveService.getBackupFiles()
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloudBackup(account: GoogleSignInAccount, fileId: String, fileName: String): Result<RestoreResult> = withContext(Dispatchers.IO) {
        try {
            val driveService = getGoogleDriveService(account)
            val tempFile = File.createTempFile("restore_", ".json", context.cacheDir)
            driveService.downloadBackupFile(fileId, tempFile)

            val result = restoreFromBackup(tempFile)

            tempFile.delete()

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCloudBackup(account: GoogleSignInAccount, fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val driveService = getGoogleDriveService(account)
            driveService.deleteBackupFile(fileId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    companion object {
        private const val TAG = "BackupService"
        private const val BACKUP_FOLDER = "TBCalendar/Backup"
        private const val BACKUP_FILE_PREFIX = "TBCalendar_Backup_"
        private const val BACKUP_FILE_EXTENSION = ".json"
    }
    
    /**
     * Verifica se o app tem permissão para escrever no armazenamento
     */
    fun hasStoragePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ (API 30+)
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10+ (API 29+)
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                // Android < 10
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    
    /**
     * Verifica se precisa solicitar permissão de gerenciamento de armazenamento
     */
    fun needsManageExternalStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()
    }
    
    /**
     * Verifica se precisa solicitar permissão de escrita
     */
    fun needsWritePermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R && 
               ContextCompat.checkSelfPermission(
                   context,
                   Manifest.permission.WRITE_EXTERNAL_STORAGE
               ) != PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Gera um backup completo de todas as atividades e itens da lixeira
     */
    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun createBackup(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Coletar dados para backup
            val activities = activityRepository.activities.first()
            val deletedActivities = deletedActivityRepository.deletedActivities.first()
            val completedActivities = completedActivityRepository.completedActivities.first()
            
            // Criar estrutura JSON do backup
            val backupData = createBackupJson(activities, deletedActivities, completedActivities)
            
            // Criar diretório de backup se não existir
            val backupDir = createBackupDirectory()
            
            // Gerar nome do arquivo com timestamp
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val backupFileName = "$BACKUP_FILE_PREFIX$timestamp$BACKUP_FILE_EXTENSION"
            val backupFile = File(backupDir, backupFileName)
            
            // Escrever arquivo de backup
            backupFile.writeText(backupData, Charsets.UTF_8)
            
            // Verificar se o arquivo foi criado
            if (backupFile.exists()) {
                Result.success(backupFile.absolutePath)
            } else {
                Result.failure(Exception("Falha ao criar arquivo de backup"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cria o diretório de backup se não existir
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun createBackupDirectory(): File {
        val backupDir = if (hasStoragePermission()) {
            // Se tem permissão, criar na pasta pública
            File(Environment.getExternalStorageDirectory(), BACKUP_FOLDER)
        } else {
            // Fallback para pasta privada do app
            File(context.getExternalFilesDir(null), BACKUP_FOLDER)
        }
        
        if (!backupDir.exists()) {
            val created = backupDir.mkdirs()
            if (!created) {
                // Não foi possível criar o diretório de backup
            }
        } else {
        }
        
        return backupDir
    }
    
    /**
     * Cria a estrutura JSON do backup
     */
    private fun createBackupJson(activities: List<Activity>, deletedActivities: List<DeletedActivity>, completedActivities: List<Activity>): String {
        val backupJson = JSONObject()
        
        // Metadados do backup
        backupJson.put("backupVersion", "1.0")
        backupJson.put("createdAt", LocalDateTime.now().toString())
        backupJson.put("appVersion", "TheBigCalendar")
        backupJson.put("totalActivities", activities.size)
        backupJson.put("totalDeletedActivities", deletedActivities.size)
        backupJson.put("totalCompletedActivities", completedActivities.size)
        
        // Atividades ativas
        val activitiesArray = JSONArray()
        activities.forEach { activity ->
            val activityJson = JSONObject()
            activityJson.put("id", activity.id)
            activityJson.put("title", activity.title)
            activityJson.put("description", activity.description ?: "")
            activityJson.put("date", activity.date)
            activityJson.put("startTime", activity.startTime?.toString() ?: "")
            activityJson.put("endTime", activity.endTime?.toString() ?: "")
            activityJson.put("isAllDay", activity.isAllDay)
            activityJson.put("location", activity.location ?: "")
            activityJson.put("categoryColor", activity.categoryColor)
            activityJson.put("activityType", activity.activityType.name)
            activityJson.put("recurrenceRule", activity.recurrenceRule ?: "")
            activityJson.put("isCompleted", activity.isCompleted)
            activityJson.put("visibility", activity.visibility.name)
            activityJson.put("showInCalendar", activity.showInCalendar)
            activityJson.put("isFromGoogle", activity.isFromGoogle)
            
            // Campos de recorrência
            val excludedDatesArray = JSONArray()
            activity.excludedDates.forEach { date ->
                excludedDatesArray.put(date)
            }
            activityJson.put("excludedDates", excludedDatesArray)
            
            val excludedInstancesArray = JSONArray()
            activity.excludedInstances.forEach { instance ->
                excludedInstancesArray.put(instance)
            }
            activityJson.put("excludedInstances", excludedInstancesArray)
            
            activityJson.put("wikipediaLink", activity.wikipediaLink ?: "")
            
            // Configurações de notificação
            val notificationJson = JSONObject()
            notificationJson.put("isEnabled", activity.notificationSettings.isEnabled)
            notificationJson.put("notificationType", activity.notificationSettings.notificationType.name)
            notificationJson.put("customMinutesBefore", activity.notificationSettings.customMinutesBefore)
            notificationJson.put("notificationTime", activity.notificationSettings.notificationTime?.toString() ?: "")
            activityJson.put("notificationSettings", notificationJson)
            
            activitiesArray.put(activityJson)
        }
        backupJson.put("activities", activitiesArray)
        
        // Itens da lixeira
        val deletedActivitiesArray = JSONArray()
        deletedActivities.forEach { deletedActivity ->
            val deletedJson = JSONObject()
            deletedJson.put("id", deletedActivity.id)
            deletedJson.put("deletedAt", deletedActivity.deletedAt)
            deletedJson.put("deletedBy", deletedActivity.deletedBy)
            
            // Dados da atividade original
            val originalActivity = deletedActivity.originalActivity
            val originalJson = JSONObject()
            originalJson.put("id", originalActivity.id)
            originalJson.put("title", originalActivity.title)
            originalJson.put("description", originalActivity.description ?: "")
            originalJson.put("date", originalActivity.date)
            originalJson.put("startTime", originalActivity.startTime?.toString() ?: "")
            originalJson.put("endTime", originalActivity.endTime?.toString() ?: "")
            originalJson.put("isAllDay", originalActivity.isAllDay)
            originalJson.put("location", originalActivity.location ?: "")
            originalJson.put("categoryColor", originalActivity.categoryColor)
            originalJson.put("activityType", originalActivity.activityType.name)
            originalJson.put("recurrenceRule", originalActivity.recurrenceRule ?: "")
            originalJson.put("isCompleted", originalActivity.isCompleted)
            originalJson.put("visibility", originalActivity.visibility.name)
            originalJson.put("showInCalendar", originalActivity.showInCalendar)
            originalJson.put("isFromGoogle", originalActivity.isFromGoogle)
            
            deletedJson.put("originalActivity", originalJson)
            deletedActivitiesArray.put(deletedJson)
        }
        backupJson.put("deletedActivities", deletedActivitiesArray)
        
        // Atividades concluídas
        val completedActivitiesArray = JSONArray()
        completedActivities.forEach { activity ->
            val activityJson = JSONObject()
            activityJson.put("id", activity.id)
            activityJson.put("title", activity.title)
            activityJson.put("description", activity.description ?: "")
            activityJson.put("date", activity.date)
            activityJson.put("startTime", activity.startTime?.toString() ?: "")
            activityJson.put("endTime", activity.endTime?.toString() ?: "")
            activityJson.put("isAllDay", activity.isAllDay)
            activityJson.put("location", activity.location ?: "")
            activityJson.put("categoryColor", activity.categoryColor)
            activityJson.put("activityType", activity.activityType.name)
            activityJson.put("recurrenceRule", activity.recurrenceRule ?: "")
            activityJson.put("isCompleted", activity.isCompleted)
            activityJson.put("visibility", activity.visibility.name)
            activityJson.put("showInCalendar", activity.showInCalendar)
            activityJson.put("isFromGoogle", activity.isFromGoogle)
            
            // Campos de recorrência
            val excludedDatesArray = JSONArray()
            activity.excludedDates.forEach { date ->
                excludedDatesArray.put(date)
            }
            activityJson.put("excludedDates", excludedDatesArray)
            
            val excludedInstancesArray = JSONArray()
            activity.excludedInstances.forEach { instance ->
                excludedInstancesArray.put(instance)
            }
            activityJson.put("excludedInstances", excludedInstancesArray)
            
            activityJson.put("wikipediaLink", activity.wikipediaLink ?: "")
            
            // Configurações de notificação
            val notificationJson = JSONObject()
            notificationJson.put("isEnabled", activity.notificationSettings.isEnabled)
            notificationJson.put("notificationType", activity.notificationSettings.notificationType.name)
            notificationJson.put("customMinutesBefore", activity.notificationSettings.customMinutesBefore)
            notificationJson.put("notificationTime", activity.notificationSettings.notificationTime?.toString() ?: "")
            activityJson.put("notificationSettings", notificationJson)
            
            completedActivitiesArray.put(activityJson)
        }
        backupJson.put("completedActivities", completedActivitiesArray)
        
        return backupJson.toString(2) // Pretty print com indentação
    }
    
    /**
     * Lista todos os arquivos de backup disponíveis
     */
    suspend fun listBackupFiles(): List<File> = withContext(Dispatchers.IO) {
        try {
            val backupDir = if (hasStoragePermission()) {
                File(Environment.getExternalStorageDirectory(), BACKUP_FOLDER)
            } else {
                File(context.getExternalFilesDir(null), BACKUP_FOLDER)
            }
            
            if (!backupDir.exists()) {
                return@withContext emptyList()
            }
            
            val backupFiles = backupDir.listFiles { file ->
                file.isFile && file.name.startsWith(BACKUP_FILE_PREFIX) && file.name.endsWith(BACKUP_FILE_EXTENSION)
            } ?: emptyArray()
            
            backupFiles.sortedByDescending { it.lastModified() }.toList()
            
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Obtém informações sobre um arquivo de backup
     */
    suspend fun getBackupInfo(backupFile: File): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            val content = backupFile.readText(Charsets.UTF_8)
            val json = JSONObject(content)
            
            val info = BackupInfo(
                fileName = backupFile.name,
                filePath = backupFile.absolutePath,
                fileSize = backupFile.length(),
                createdAt = json.optString("createdAt", ""),
                totalActivities = json.optInt("totalActivities", 0),
                totalDeletedActivities = json.optInt("totalDeletedActivities", 0),
                totalCompletedActivities = json.optInt("totalCompletedActivities", 0),
                backupVersion = json.optString("backupVersion", "1.0")
            )
            
            Result.success(info)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Restaura dados de um arquivo de backup
     */
    suspend fun restoreFromBackup(backupFile: File): Result<RestoreResult> = withContext(Dispatchers.IO) {
        try {
            val content = backupFile.readText(Charsets.UTF_8)
            val json = JSONObject(content)
            
            // Verificar versão do backup
            val backupVersion = json.optString("backupVersion", "1.0")
            if (backupVersion != "1.0") {
                return@withContext Result.failure(Exception("Versão de backup não suportada: $backupVersion"))
            }
            
            // Extrair atividades
            val activitiesArray = json.optJSONArray("activities") ?: JSONArray()
            val restoredActivities = mutableListOf<com.mss.thebigcalendar.data.model.Activity>()
            
            for (i in 0 until activitiesArray.length()) {
                val activityJson = activitiesArray.getJSONObject(i)
                try {
                    val activity = parseActivityFromJson(activityJson)
                    restoredActivities.add(activity)
                } catch (e: Exception) {
                    // Erro ao parsear atividade - continuar com outras
                }
            }
            
            // Extrair itens da lixeira
            val deletedActivitiesArray = json.optJSONArray("deletedActivities") ?: JSONArray()
            val restoredDeletedActivities = mutableListOf<com.mss.thebigcalendar.data.model.DeletedActivity>()
            
            for (i in 0 until deletedActivitiesArray.length()) {
                val deletedJson = deletedActivitiesArray.getJSONObject(i)
                try {
                    val deletedActivity = parseDeletedActivityFromJson(deletedJson)
                    restoredDeletedActivities.add(deletedActivity)
                } catch (e: Exception) {
                    // Erro ao parsear item da lixeira - continuar com outros
                }
            }
            
            // Extrair atividades concluídas
            val completedActivitiesArray = json.optJSONArray("completedActivities") ?: JSONArray()
            val restoredCompletedActivities = mutableListOf<com.mss.thebigcalendar.data.model.Activity>()
            
            for (i in 0 until completedActivitiesArray.length()) {
                val activityJson = completedActivitiesArray.getJSONObject(i)
                try {
                    val activity = parseActivityFromJson(activityJson)
                    restoredCompletedActivities.add(activity)
                } catch (e: Exception) {
                    // Erro ao parsear atividade concluída - continuar com outras
                }
            }
            
            val result = RestoreResult(
                activities = restoredActivities,
                deletedActivities = restoredDeletedActivities,
                completedActivities = restoredCompletedActivities,
                backupFileName = backupFile.name,
                backupCreatedAt = json.optString("createdAt", "")
            )
            
            Result.success(result)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Parseia uma atividade a partir do JSON
     */
    private fun parseActivityFromJson(activityJson: JSONObject): com.mss.thebigcalendar.data.model.Activity {
        val startTime = activityJson.optString("startTime").takeIf { it.isNotEmpty() }?.let {
            try {
                java.time.LocalTime.parse(it)
            } catch (e: Exception) {
                null
            }
        }
        
        val endTime = activityJson.optString("endTime").takeIf { it.isNotEmpty() }?.let {
            try {
                java.time.LocalTime.parse(it)
            } catch (e: Exception) {
                null
            }
        }
        
        val activityType = try {
            com.mss.thebigcalendar.data.model.ActivityType.valueOf(activityJson.getString("activityType"))
        } catch (e: Exception) {
            com.mss.thebigcalendar.data.model.ActivityType.EVENT
        }
        
        val visibility = try {
            com.mss.thebigcalendar.data.model.VisibilityLevel.valueOf(activityJson.optString("visibility", "LOW"))
        } catch (e: Exception) {
            com.mss.thebigcalendar.data.model.VisibilityLevel.LOW
        }
        
        // Parsear configurações de notificação
        val notificationSettings = try {
            val notificationJson = activityJson.optJSONObject("notificationSettings")
            if (notificationJson != null) {
                val notificationType = try {
                    com.mss.thebigcalendar.data.model.NotificationType.valueOf(
                        notificationJson.optString("notificationType", "FIFTEEN_MINUTES_BEFORE")
                    )
                } catch (e: Exception) {
                    com.mss.thebigcalendar.data.model.NotificationType.FIFTEEN_MINUTES_BEFORE
                }
                
                val notificationTime = notificationJson.optString("notificationTime").takeIf { it.isNotEmpty() }?.let {
                    try {
                        java.time.LocalTime.parse(it)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                com.mss.thebigcalendar.data.model.NotificationSettings(
                    isEnabled = notificationJson.optBoolean("isEnabled", true),
                    notificationType = notificationType,
                    customMinutesBefore = notificationJson.optInt("customMinutesBefore", 15),
                    notificationTime = notificationTime
                )
            } else {
                com.mss.thebigcalendar.data.model.NotificationSettings(
                    isEnabled = false,
                    notificationTime = null,
                    notificationType = com.mss.thebigcalendar.data.model.NotificationType.BEFORE_ACTIVITY,
                    customMinutesBefore = null
                )
            }
        } catch (e: Exception) {
            com.mss.thebigcalendar.data.model.NotificationSettings(
                isEnabled = false,
                notificationTime = null,
                notificationType = com.mss.thebigcalendar.data.model.NotificationType.BEFORE_ACTIVITY,
                customMinutesBefore = null
            )
        }
        
        return com.mss.thebigcalendar.data.model.Activity(
            id = activityJson.getString("id"),
            title = activityJson.getString("title"),
            description = activityJson.optString("description").takeIf { it.isNotEmpty() },
            date = activityJson.getString("date"),
            startTime = startTime,
            endTime = endTime,
            isAllDay = activityJson.optBoolean("isAllDay", false),
            location = activityJson.optString("location").takeIf { it.isNotEmpty() },
            categoryColor = activityJson.optString("categoryColor", "#3B82F6"),
            activityType = activityType,
            recurrenceRule = activityJson.optString("recurrenceRule").takeIf { it.isNotEmpty() },
            notificationSettings = notificationSettings,
            isCompleted = activityJson.optBoolean("isCompleted", false),
            visibility = visibility,
            showInCalendar = activityJson.optBoolean("showInCalendar", true), // Por padrão, mostrar no calendário
            isFromGoogle = activityJson.optBoolean("isFromGoogle", false),
            excludedDates = parseStringArray(activityJson.optJSONArray("excludedDates")),
            excludedInstances = parseStringArray(activityJson.optJSONArray("excludedInstances")),
            wikipediaLink = activityJson.optString("wikipediaLink").takeIf { it.isNotEmpty() } // Preservar link da Wikipedia se existir
        )
    }
    
    /**
     * Parseia um array de strings do JSON
     */
    private fun parseStringArray(jsonArray: JSONArray?): List<String> {
        if (jsonArray == null) return emptyList()
        
        val result = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            try {
                val value = jsonArray.getString(i)
                if (value.isNotEmpty()) {
                    result.add(value)
                }
            } catch (e: Exception) {
                // Ignorar valores inválidos
            }
        }
        return result
    }
    
    /**
     * Parseia um item da lixeira a partir do JSON
     */
    private fun parseDeletedActivityFromJson(deletedJson: JSONObject): com.mss.thebigcalendar.data.model.DeletedActivity {
        val originalActivityJson = deletedJson.getJSONObject("originalActivity")
        val originalActivity = parseActivityFromJson(originalActivityJson)
        
        return com.mss.thebigcalendar.data.model.DeletedActivity(
            id = deletedJson.getString("id"),
            originalActivity = originalActivity,
            deletedAt = deletedJson.optString("deletedAt").let { dateString ->
                try {
                    java.time.LocalDateTime.parse(dateString)
                } catch (e: Exception) {
                    java.time.LocalDateTime.now()
                }
            },
            deletedBy = deletedJson.optString("deletedBy", "Sistema")
        )
    }
}

/**
 * Classe para armazenar informações sobre um arquivo de backup
 */
data class BackupInfo(
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val createdAt: String,
    val totalActivities: Int,
    val totalDeletedActivities: Int,
    val totalCompletedActivities: Int,
    val backupVersion: String
)

/**
 * Classe para armazenar o resultado da restauração
 */
data class RestoreResult(
    val activities: List<com.mss.thebigcalendar.data.model.Activity>,
    val deletedActivities: List<com.mss.thebigcalendar.data.model.DeletedActivity>,
    val completedActivities: List<com.mss.thebigcalendar.data.model.Activity>,
    val backupFileName: String,
    val backupCreatedAt: String
)
