package com.mss.thebigcalendar.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.CalendarUiState
import com.mss.thebigcalendar.data.model.Holiday
import com.mss.thebigcalendar.data.model.SearchResult
import com.mss.thebigcalendar.data.repository.ActivityRepository
import com.mss.thebigcalendar.data.repository.HolidayRepository
import com.mss.thebigcalendar.data.repository.SettingsRepository
import com.mss.thebigcalendar.data.repository.JsonCalendarRepository
import com.mss.thebigcalendar.data.repository.DeletedActivityRepository
import com.mss.thebigcalendar.data.repository.CompletedActivityRepository
import com.mss.thebigcalendar.data.repository.AlarmRepository
import com.mss.thebigcalendar.data.service.BackupService
import com.mss.thebigcalendar.service.GoogleAuthService
import com.mss.thebigcalendar.service.GoogleCalendarService
import com.mss.thebigcalendar.service.NotificationService
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

class ActivityManagementViewModel(application: Application) : AndroidViewModel(application) {

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
    // Removed ProgressiveSyncService as it's not needed in this ViewModel
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

    // Activity Management Functions
    fun onSaveActivity(activityData: Activity, syncWithGoogle: Boolean = false) {
        viewModelScope.launch {
            // Verificar se é uma edição de instância recorrente
            val isEditingRecurringInstance = activityData.id.contains("_") && 
                                           activityData.id != "new" && 
                                           !activityData.id.isBlank()

            
            if (isEditingRecurringInstance) {
                // É uma edição de instância recorrente - aplicar mudanças à atividade base
                val baseId = activityData.id.split("_").first()
                val baseActivity = _uiState.value.activities.find { it.id == baseId }
                
                if (baseActivity != null) {
                    // Aplicar mudanças à atividade base, mantendo o ID original
                    val updatedBaseActivity = baseActivity.copy(
                        title = activityData.title,
                        description = activityData.description,
                        startTime = activityData.startTime,
                        endTime = activityData.endTime,
                        isAllDay = activityData.isAllDay,
                        location = activityData.location,
                        categoryColor = activityData.categoryColor,
                        notificationSettings = activityData.notificationSettings,
                        visibility = activityData.visibility,
                        showInCalendar = activityData.showInCalendar
                        // NÃO alterar recurrenceRule para manter a recorrência
                    )
                    
                    // Salvar a atividade base atualizada
                    activityRepository.saveActivity(updatedBaseActivity)
                    
                    // Agendar notificação se configurada - usar a instância específica com data correta
                    if (activityData.notificationSettings.isEnabled &&
                        activityData.notificationSettings.notificationType != com.mss.thebigcalendar.data.model.NotificationType.NONE) {
                        
                        // Extrair a data da instância específica do ID
                        val instanceDate = activityData.id.split("_").getOrNull(1)
                        if (instanceDate != null) {
                            // Criar uma cópia da atividade com a data correta da instância
                            val instanceActivity = activityData.copy(date = instanceDate)
                            val notificationService = NotificationService(getApplication())
                            notificationService.scheduleNotification(instanceActivity)
                        }
                    }
                    
                    // Sincronizar com Google Calendar se for evento do Google
                    if (updatedBaseActivity.isFromGoogle) {
                        updateGoogleCalendarEvent(updatedBaseActivity)
                    }
                }
            } else {
                // É uma nova atividade ou edição de atividade não recorrente
                val activityToSave = if (activityData.id == "new") {
                    activityData.copy(id = java.util.UUID.randomUUID().toString())
                } else {
                    activityData
                }
                
                // Salvar atividade
                activityRepository.saveActivity(activityToSave)
                
                // Agendar notificação se configurada
                if (activityToSave.notificationSettings.isEnabled &&
                    activityToSave.notificationSettings.notificationType != com.mss.thebigcalendar.data.model.NotificationType.NONE) {
                    val notificationService = NotificationService(getApplication())
                    notificationService.scheduleNotification(activityToSave)
                }
                
                // Sincronizar com Google Calendar se solicitado
                if (syncWithGoogle) {
                    val account = _uiState.value.googleSignInAccount
                    if (account != null) {
                        insertGoogleCalendarEvent(activityToSave, account)
                    }
                }
                
                // Se for evento do Google, atualizar
                if (activityToSave.isFromGoogle) {
                    updateGoogleCalendarEvent(activityToSave)
                }
            }
            
            // Fechar modal
            closeCreateActivityModal()
        }
    }

    fun openCreateActivityModal(activity: Activity? = null, activityType: ActivityType = ActivityType.EVENT) {
        _uiState.update { it.copy(activityToEdit = activity) }
    }

    fun closeCreateActivityModal() = _uiState.update { it.copy(activityToEdit = null) }

    fun onTaskLongPressed(activityId: String) {
        _uiState.update { it.copy(activityIdWithDeleteButtonVisible = activityId) }
    }

    fun hideDeleteButton() {
        _uiState.update { it.copy(activityIdWithDeleteButtonVisible = null) }
    }

    fun markActivityAsCompleted(activityId: String) {
        viewModelScope.launch {
            markActivityAsCompletedInternal(activityId)
        }
    }

    fun markActivityAsCompletedInternal(activityId: String) {
        viewModelScope.launch {
            val activity = _uiState.value.activities.find { it.id == activityId }
            if (activity != null) {
                // Mover para atividades completadas
                completedActivityRepository.addCompletedActivity(activity)
                
                // Remover da lista de atividades ativas
                activityRepository.deleteActivity(activityId)
                
                // Se for uma atividade recorrente, remover todas as instâncias futuras
                if (!activity.recurrenceRule.isNullOrBlank()) {
                    removeRecurringInstances(activity)
                }
                
                // Se for do Google Calendar, deletar do Google também
                if (activity.isFromGoogle) {
                    deleteFromGoogleCalendar(activity)
                }
                
                // Cancelar notificação se existir
                val notificationService = NotificationService(getApplication())
                notificationService.cancelNotification(activityId)
                
                Log.d("CalendarViewModel", "✅ Atividade marcada como concluída: ${activity.title}")
            }
        }
    }

    fun requestDeleteActivity(activityId: String) {
        _uiState.update { it.copy(activityIdToDelete = activityId) }
    }

    fun onDeleteActivityConfirm() {
        val activityId = _uiState.value.activityIdToDelete
        if (activityId != null) {
            viewModelScope.launch {
                val activity = _uiState.value.activities.find { it.id == activityId }
                if (activity != null) {
                    // Mover para lixeira
                    deletedActivityRepository.addDeletedActivity(activity)
                    
                    // Remover da lista de atividades ativas
                    activityRepository.deleteActivity(activityId)
                    
                    // Se for uma atividade recorrente, remover todas as instâncias futuras
                    if (!activity.recurrenceRule.isNullOrBlank()) {
                        removeRecurringInstances(activity)
                    }
                    
                    // Se for do Google Calendar, deletar do Google também
                    if (activity.isFromGoogle) {
                        deleteFromGoogleCalendar(activity)
                    }
                    
                    // Cancelar notificação se existir
                    val notificationService = NotificationService(getApplication())
                    notificationService.cancelNotification(activityId)
                    
                    Log.d("CalendarViewModel", "🗑️ Atividade movida para lixeira: ${activity.title}")
                }
                
                // Limpar estado
                _uiState.update { it.copy(activityIdToDelete = null) }
            }
        }
    }

    fun cancelDeleteActivity() = _uiState.update { it.copy(activityIdToDelete = null) }

    // Search Functions
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        
        viewModelScope.launch {
            val results = searchService.search(
                query = query,
                activities = _uiState.value.activities,
                nationalHolidays = _uiState.value.nationalHolidays,
                saintDays = _uiState.value.saintDays,
                commemorativeDates = _uiState.value.commemorativeDates
            )
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    fun onSearchResultClick(result: SearchResult) {
        when (result.type) {
            SearchResult.Type.ACTIVITY -> {
                val activity = _uiState.value.activities.find { it.id == result.id }
                if (activity != null && result.date != null) {
                    // Navegar para a data da atividade
                    _uiState.update { 
                        it.copy(
                            selectedDate = result.date,
                            displayedYearMonth = YearMonth.from(result.date),
                            isSearchScreenOpen = false,
                            searchQuery = ""
                        ) 
                    }
                }
            }
            SearchResult.Type.HOLIDAY -> {
                if (result.date != null) {
                    _uiState.update { 
                        it.copy(
                            selectedDate = result.date,
                            displayedYearMonth = YearMonth.from(result.date),
                            isSearchScreenOpen = false,
                            searchQuery = ""
                        ) 
                    }
                }
            }
            SearchResult.Type.SAINT_DAY -> {
                if (result.date != null) {
                    _uiState.update { 
                        it.copy(
                            selectedDate = result.date,
                            displayedYearMonth = YearMonth.from(result.date),
                            isSearchScreenOpen = false,
                            searchQuery = ""
                        ) 
                    }
                }
            }
        }
    }

    fun clearSearch() {
        _uiState.update { 
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                isSearchScreenOpen = false
            ) 
        }
    }

    fun onSearchIconClick() {
        _uiState.update { it.copy(isSearchScreenOpen = true) }
    }

    // Helper functions
    private fun calculateRecurringInstancesForDate(baseActivity: Activity, targetDate: LocalDate): List<Activity> {
        val occurrences = recurrenceService.getOccurrencesInPeriod(
            baseActivity, 
            targetDate, 
            targetDate
        )
        return occurrences.map { date ->
            baseActivity.copy(
                id = "${baseActivity.id}_${date}",
                date = date.toString()
            )
        }
    }

    private fun removeRecurringInstances(baseActivity: Activity) {
        viewModelScope.launch {
            val futureInstances = recurrenceService.generateRecurringInstances(
                baseActivity,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusYears(1)
            )
            futureInstances.forEach { instance ->
                activityRepository.deleteActivity(instance.id)
            }
        }
    }

    private fun updateGoogleCalendarEvent(activity: Activity) {
        viewModelScope.launch {
            val account = _uiState.value.googleSignInAccount
            if (account != null) {
                try {
                    val calendarService = googleCalendarService.getCalendarService(account)
                    // Implementar atualização de evento usando calendarService
                    Log.d("CalendarViewModel", "Atualizando evento no Google Calendar: ${activity.title}")
                } catch (e: Exception) {
                    Log.e("CalendarViewModel", "Erro ao atualizar evento no Google Calendar", e)
                }
            }
        }
    }

    private suspend fun insertGoogleCalendarEvent(activity: Activity, account: GoogleSignInAccount): String? {
        return try {
            val calendarService = googleCalendarService.getCalendarService(account)
            // Implementar inserção de evento usando calendarService
            Log.d("CalendarViewModel", "Inserindo evento no Google Calendar: ${activity.title}")
            null // Retornar ID do evento quando implementado
        } catch (e: Exception) {
            Log.e("CalendarViewModel", "Erro ao inserir evento no Google Calendar", e)
            null
        }
    }

    private fun deleteFromGoogleCalendar(activity: Activity) {
        viewModelScope.launch {
            val account = _uiState.value.googleSignInAccount
            if (account != null && activity.isFromGoogle) {
                try {
                    val calendarService = googleCalendarService.getCalendarService(account)
                    // Implementar deleção de evento usando calendarService
                    Log.d("CalendarViewModel", "Deletando evento do Google Calendar: ${activity.title}")
                } catch (e: Exception) {
                    Log.e("CalendarViewModel", "Erro ao deletar evento do Google Calendar", e)
                }
            }
        }
    }
}
