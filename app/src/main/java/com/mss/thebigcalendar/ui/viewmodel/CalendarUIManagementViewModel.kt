package com.mss.thebigcalendar.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.CalendarUiState
import com.mss.thebigcalendar.data.model.Holiday
import com.mss.thebigcalendar.data.model.JsonHoliday
import com.mss.thebigcalendar.data.model.JsonCalendar
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

class CalendarUIManagementViewModel(application: Application) : AndroidViewModel(application) {

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

    // UI Management Functions
    fun openSidebar() = _uiState.update { it.copy(isSidebarOpen = true) }

    fun closeSidebar() = _uiState.update { it.copy(isSidebarOpen = false) }

    fun onNavigateToSettings(screen: String?) {
        _uiState.update { it.copy(currentSettingsScreen = screen) }
    }

    fun closeSettingsScreen() {
        _uiState.update { it.copy(currentSettingsScreen = null) }
    }

    fun onWelcomeNameChange(newName: String) {
        _uiState.update { it.copy(welcomeName = newName) }
        
        // Salvar nome
        viewModelScope.launch {
            settingsRepository.saveWelcomeName(newName)
        }
    }

    fun onSaintDayClick(saint: Holiday) {
        _uiState.update { it.copy(saintInfoToShow = saint) }
    }

    fun onSaintInfoDialogDismiss() {
        _uiState.update { it.copy(saintInfoToShow = null) }
    }

    fun onChartIconClick() {
        _uiState.update { it.copy(isChartScreenOpen = true) }
    }

    fun closeChartScreen() {
        _uiState.update { it.copy(isChartScreenOpen = false) }
    }

    fun onNotesClick() {
        _uiState.update { it.copy(isNotesScreenOpen = true) }
    }

    fun closeNotesScreen() {
        _uiState.update { it.copy(isNotesScreenOpen = false) }
    }

    fun onAlarmsClick() {
        _uiState.update { it.copy(isAlarmsScreenOpen = true) }
    }

    fun closeAlarmsScreen() {
        _uiState.update { it.copy(isAlarmsScreenOpen = false) }
    }

    fun toggleMoonPhasesVisibility() {
        _uiState.update { it.copy(showMoonPhases = !it.showMoonPhases) }
        
        // Salvar configuração
        viewModelScope.launch {
            settingsRepository.saveShowMoonPhases(_uiState.value.showMoonPhases)
        }
    }

    // Overlay Permission Functions
    fun hasOverlayPermission(): Boolean {
        return visibilityService.hasOverlayPermission()
    }

    fun requestOverlayPermission(): Intent {
        return visibilityService.requestOverlayPermission()
    }

    fun testHighVisibilityAlert() {
        viewModelScope.launch {
            try {
                visibilityService.testHighVisibilityAlert()
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Erro ao testar alerta de alta visibilidade", e)
            }
        }
    }

    // JSON Calendar Functions
    fun openJsonConfigScreen(fileName: String, uri: android.net.Uri) {
        _uiState.update { 
            it.copy(
                isJsonConfigScreenOpen = true,
                selectedJsonFileName = fileName,
                selectedJsonUri = uri
            ) 
        }
    }

    fun closeJsonConfigScreen() {
        _uiState.update { 
            it.copy(
                isJsonConfigScreenOpen = false,
                selectedJsonFileName = null,
                selectedJsonUri = null
            ) 
        }
    }

    fun saveJsonConfig(title: String, color: androidx.compose.ui.graphics.Color) {
        val uri = _uiState.value.selectedJsonUri
        val fileName = _uiState.value.selectedJsonFileName
        
        if (uri != null && fileName != null) {
            viewModelScope.launch {
                try {
                    // Ler conteúdo do arquivo JSON
                    val contentResolver = getApplication<Application>().contentResolver
                    val inputStream = contentResolver.openInputStream(uri)
                    val jsonContent = inputStream?.bufferedReader()?.use { it.readText() }
                    
                    if (jsonContent != null) {
                        // Criar novo calendário JSON
                        val jsonCalendar = JsonCalendar(
                            id = java.util.UUID.randomUUID().toString(),
                            title = title,
                            color = color,
                            fileName = fileName,
                            importDate = System.currentTimeMillis(),
                            isVisible = true
                        )
                        
                        // Salvar no repositório
                        jsonCalendarRepository.saveJsonCalendar(jsonCalendar)
                        
                        // Recarregar calendários JSON
                        loadJsonCalendars()
                        
                        // Fechar tela de configuração
                        closeJsonConfigScreen()
                        
                        Log.d("CalendarViewModel", "✅ Calendário JSON salvo: $title")
                    } else {
                        Log.e("CalendarViewModel", "❌ Erro ao ler conteúdo do arquivo JSON")
                    }
                } catch (e: Exception) {
                    Log.e("CalendarViewModel", "❌ Erro ao salvar calendário JSON", e)
                }
            }
        }
    }

    fun toggleJsonCalendarVisibility(calendarId: String, isVisible: Boolean) {
        viewModelScope.launch {
            try {
                val calendar = _uiState.value.jsonCalendars.find { it.id == calendarId }
                if (calendar != null) {
                    val updatedCalendar = calendar.copy(isVisible = isVisible)
                    jsonCalendarRepository.saveJsonCalendar(updatedCalendar)
                    
                    // Recarregar calendários JSON
                    loadJsonCalendars()
                    
                    Log.d("CalendarViewModel", "✅ Visibilidade do calendário JSON alterada: ${calendar.title}")
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "❌ Erro ao alterar visibilidade do calendário JSON", e)
            }
        }
    }

    fun removeJsonCalendar(calendarId: String) {
        viewModelScope.launch {
            try {
                jsonCalendarRepository.removeJsonCalendar(calendarId)
                
                // Recarregar calendários JSON
                loadJsonCalendars()
                
                Log.d("CalendarViewModel", "✅ Calendário JSON removido")
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "❌ Erro ao remover calendário JSON", e)
            }
        }
    }

    fun showJsonHolidayInfo(jsonHoliday: JsonHoliday) {
        // Implementar se necessário
        Log.d("CalendarViewModel", "Mostrando informações do feriado JSON: ${jsonHoliday.name}")
    }

    fun hideJsonHolidayInfo() {
        // Implementar se necessário
        Log.d("CalendarViewModel", "Ocultando informações do feriado JSON")
    }

    // Helper functions
    private fun loadJsonCalendars() {
        viewModelScope.launch {
            try {
                jsonCalendarRepository.getAllJsonCalendars().collect { jsonCalendars ->
                    _uiState.update { it.copy(jsonCalendars = jsonCalendars) }
                    
                    Log.d("CalendarViewModel", "✅ Calendários JSON carregados: ${jsonCalendars.size}")
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "❌ Erro ao carregar calendários JSON", e)
            }
        }
    }
}
