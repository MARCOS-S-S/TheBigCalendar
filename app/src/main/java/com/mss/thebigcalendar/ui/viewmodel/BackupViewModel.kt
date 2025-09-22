package com.mss.thebigcalendar.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.CalendarUiState
import com.mss.thebigcalendar.data.model.DeletedActivity
import com.mss.thebigcalendar.data.repository.ActivityRepository
import com.mss.thebigcalendar.data.repository.HolidayRepository
import com.mss.thebigcalendar.data.repository.SettingsRepository
import com.mss.thebigcalendar.data.repository.JsonCalendarRepository
import com.mss.thebigcalendar.data.repository.DeletedActivityRepository
import com.mss.thebigcalendar.data.repository.CompletedActivityRepository
import com.mss.thebigcalendar.data.repository.AlarmRepository
import com.mss.thebigcalendar.data.service.BackupService
import com.mss.thebigcalendar.data.service.BackupInfo
import com.mss.thebigcalendar.service.GoogleAuthService
import com.mss.thebigcalendar.service.GoogleCalendarService
import com.mss.thebigcalendar.service.ProgressiveSyncService
import com.mss.thebigcalendar.service.SearchService
import com.mss.thebigcalendar.service.RecurrenceService
import com.mss.thebigcalendar.service.VisibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.LocalDate
import java.time.YearMonth

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    // Repositories
    val settingsRepository = SettingsRepository(application)
    private val activityRepository = ActivityRepository(application)
    private val holidayRepository = HolidayRepository(application)
    private val deletedActivityRepository = DeletedActivityRepository(application)
    private val completedActivityRepository = CompletedActivityRepository(application)
    private val alarmRepository = AlarmRepository(application)
    private val jsonCalendarRepository = JsonCalendarRepository(application)
    
    // Services
    private val googleAuthService = GoogleAuthService(application)
    private val googleCalendarService = GoogleCalendarService(application)
    private val progressiveSyncService = ProgressiveSyncService(application, googleCalendarService)
    private val searchService = SearchService()
    private val recurrenceService = RecurrenceService()
    private val backupService = BackupService(application, activityRepository, deletedActivityRepository, completedActivityRepository)
    private val visibilityService = VisibilityService(application)

    // State Management
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    
    // Cache System
    private var updateJob: Job? = null
    private var cachedCalendarDays: List<com.mss.thebigcalendar.data.model.CalendarDay>? = null
    private var lastUpdateParams: String? = null
    private var cachedBirthdays: Map<LocalDate, List<Activity>> = emptyMap()
    private var cachedNotes: Map<LocalDate, List<Activity>> = emptyMap()
    private var cachedTasks: Map<LocalDate, List<Activity>> = emptyMap()

    // Backup Functions
    fun onBackupRequest() {
        viewModelScope.launch {
            try {
                // Verificar permissões antes de fazer backup
                if (!backupService.hasStoragePermission()) {
                    _uiState.update { it.copy(
                        backupMessage = "Permissão de armazenamento necessária para backup",
                        needsStoragePermission = true
                    ) }
                    return@launch
                }
                
                val result = backupService.createBackup()
                result.fold(
                    onSuccess = { backupPath ->
                        // Atualizar o estado para mostrar sucesso
                        _uiState.update { it.copy(
                            backupMessage = "Backup criado com sucesso: ${backupPath.substringAfterLast("/")}"
                        ) }
                        // Recarregar lista de backups
                        loadBackupFiles()
                    },
                    onFailure = { exception ->
                        Log.e("CalendarViewModel", "❌ Erro ao criar backup", exception)
                        // Atualizar o estado para mostrar erro
                        _uiState.update { it.copy(
                            backupMessage = "Erro ao criar backup: ${exception.message}"
                        ) }
                    }
                )
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "❌ Erro inesperado durante backup", e)
                _uiState.update { it.copy(
                    backupMessage = "Erro inesperado: ${e.message}"
                ) }
            }
        }
    }
    
    fun clearBackupMessage() {
        _uiState.update { it.copy(backupMessage = null) }
    }

    fun checkStoragePermission() {
        val hasPermission = backupService.hasStoragePermission()
        _uiState.update { it.copy(needsStoragePermission = !hasPermission) }
    }

    fun onRestoreRequest() { 
        println("ViewModel: Pedido de restauração recebido.") 
    }

    fun onBackupIconClick() {
        _uiState.update { it.copy(isBackupScreenOpen = true) }
        loadBackupFiles()
    }

    fun closeBackupScreen() {
        _uiState.update { it.copy(isBackupScreenOpen = false) }
    }

    fun loadBackupFiles() {
        viewModelScope.launch {
            try {
                val backupFiles = backupService.listBackupFiles()
                val backupInfos = backupFiles.mapNotNull { file ->
                    backupService.getBackupInfo(file).getOrNull()
                }
                _uiState.update { it.copy(backupFiles = backupInfos) }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Erro ao carregar arquivos de backup", e)
            }
        }
    }

    fun deleteBackupFile(filePath: String) {
        viewModelScope.launch {
            try {
                val file = java.io.File(filePath)
                val success = file.delete()
                if (success) {
                    _uiState.update { it.copy(
                        backupMessage = "Arquivo de backup excluído com sucesso"
                    ) }
                    // Recarregar lista de backups
                    loadBackupFiles()
                } else {
                    _uiState.update { it.copy(
                        backupMessage = "Erro ao excluir arquivo de backup"
                    ) }
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Erro ao excluir arquivo de backup", e)
                _uiState.update { it.copy(
                    backupMessage = "Erro ao excluir arquivo: ${e.message}"
                ) }
            }
        }
    }

    fun restoreFromBackup(filePath: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(
                    backupMessage = "Restaurando backup...",
                    isSyncing = true
                ) }
                
                val file = java.io.File(filePath)
                val result = backupService.restoreFromBackup(file)
                result.fold(
                    onSuccess = { restoreInfo ->
                        _uiState.update { it.copy(
                            backupMessage = "Backup restaurado com sucesso! ${restoreInfo.activities.size} atividades restauradas.",
                            isSyncing = false
                        ) }
                        
                        // Recarregar dados após restauração
                        loadData()
                        
                    },
                    onFailure = { exception ->
                        Log.e("CalendarViewModel", "❌ Erro ao restaurar backup", exception)
                        _uiState.update { it.copy(
                            backupMessage = "Erro ao restaurar backup: ${exception.message}",
                            isSyncing = false
                        ) }
                    }
                )
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "❌ Erro inesperado durante restauração", e)
                _uiState.update { it.copy(
                    backupMessage = "Erro inesperado: ${e.message}",
                    isSyncing = false
                ) }
            }
        }
    }

    // Trash Management Functions
    fun onTrashIconClick() {
        _uiState.update { it.copy(isTrashScreenOpen = true) }
    }

    fun closeTrashScreen() {
        _uiState.update { it.copy(isTrashScreenOpen = false) }
    }

    fun restoreDeletedActivity(deletedActivityId: String) {
        viewModelScope.launch {
            try {
                val deletedActivity = _uiState.value.deletedActivities.find { it.id == deletedActivityId }
                if (deletedActivity != null) {
                    // Restaurar atividade
                    activityRepository.saveActivity(deletedActivity.originalActivity)
                    
                    // Remover da lixeira
                    deletedActivityRepository.removeDeletedActivity(deletedActivityId)
                    
                    _uiState.update { it.copy(
                        backupMessage = "Atividade restaurada com sucesso!"
                    ) }
                    
                    Log.d("CalendarViewModel", "✅ Atividade restaurada: ${deletedActivity.originalActivity.title}")
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Erro ao restaurar atividade", e)
                _uiState.update { it.copy(
                    backupMessage = "Erro ao restaurar atividade: ${e.message}"
                ) }
            }
        }
    }

    fun removeDeletedActivity(deletedActivityId: String) {
        viewModelScope.launch {
            deletedActivityRepository.removeDeletedActivity(deletedActivityId)
        }
    }

    fun clearAllDeletedActivities() {
        viewModelScope.launch {
            try {
                deletedActivityRepository.clearAllDeletedActivities()
                _uiState.update { it.copy(
                    backupMessage = "Lixeira esvaziada com sucesso!"
                ) }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Erro ao esvaziar lixeira", e)
                _uiState.update { it.copy(
                    backupMessage = "Erro ao esvaziar lixeira: ${e.message}"
                ) }
            }
        }
    }

    fun onTrashSortOrderChange(sortOrder: String) {
        _uiState.update { it.copy(trashSortOrder = sortOrder) }
    }

    // Completed Activities Functions
    fun onCompletedTasksClick() {
        _uiState.update { it.copy(isCompletedTasksScreenOpen = true) }
    }

    fun closeCompletedTasksScreen() {
        _uiState.update { it.copy(isCompletedTasksScreenOpen = false) }
    }

    fun toggleCompletedActivitiesVisibility() {
        _uiState.update { it.copy(showCompletedActivities = !it.showCompletedActivities) }
    }

    fun getCompletedActivities(): List<Activity> {
        return _uiState.value.completedActivities
    }

    fun getLast7DaysCompletedTasksData(): List<com.mss.thebigcalendar.ui.components.BarChartData> {
        val completedActivities = _uiState.value.completedActivities
        val last7Days = mutableListOf<com.mss.thebigcalendar.ui.components.BarChartData>()
        
        for (i in 6 downTo 0) {
            val date = LocalDate.now().minusDays(i.toLong())
            val dayTasks = completedActivities.filter { 
                LocalDate.parse(it.date).isEqual(date) && it.activityType == com.mss.thebigcalendar.data.model.ActivityType.TASK 
            }
            
            last7Days.add(
                com.mss.thebigcalendar.ui.components.BarChartData(
                    label = when (date.dayOfWeek) {
                        java.time.DayOfWeek.MONDAY -> "Seg"
                        java.time.DayOfWeek.TUESDAY -> "Ter"
                        java.time.DayOfWeek.WEDNESDAY -> "Qua"
                        java.time.DayOfWeek.THURSDAY -> "Qui"
                        java.time.DayOfWeek.FRIDAY -> "Sex"
                        java.time.DayOfWeek.SATURDAY -> "Sáb"
                        java.time.DayOfWeek.SUNDAY -> "Dom"
                    },
                    value = dayTasks.size,
                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                )
            )
        }
        
        return last7Days
    }

    fun getLastYearCompletedTasksData(): List<com.mss.thebigcalendar.ui.components.BarChartData> {
        val completedActivities = _uiState.value.completedActivities
        val lastYearData = mutableListOf<com.mss.thebigcalendar.ui.components.BarChartData>()
        
        val monthNames = listOf(
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
            "Jul", "Ago", "Set", "Out", "Nov", "Dez"
        )
        
        for (month in 1..12) {
            val monthTasks = completedActivities.filter { 
                LocalDate.parse(it.date).monthValue == month && 
                it.activityType == com.mss.thebigcalendar.data.model.ActivityType.TASK 
            }
            
            lastYearData.add(
                com.mss.thebigcalendar.ui.components.BarChartData(
                    label = monthNames[month - 1],
                    value = monthTasks.size,
                    color = androidx.compose.ui.graphics.Color(0xFF2196F3)
                )
            )
        }
        
        return lastYearData
    }

    // Helper functions
    private fun loadData() {
        // This will be implemented in the main CalendarViewModel
    }
}
