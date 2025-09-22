package com.mss.thebigcalendar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mss.thebigcalendar.data.model.CalendarDay
import com.mss.thebigcalendar.data.model.CalendarUiState
import com.mss.thebigcalendar.data.model.ViewMode
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.data.model.CalendarFilterOptions
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

class CalendarNavigationViewModel(application: Application) : AndroidViewModel(application) {

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
    private var cachedCalendarDays: List<CalendarDay>? = null
    private var lastUpdateParams: String? = null
    private var cachedBirthdays: Map<LocalDate, List<com.mss.thebigcalendar.data.model.Activity>> = emptyMap()
    private var cachedNotes: Map<LocalDate, List<com.mss.thebigcalendar.data.model.Activity>> = emptyMap()
    private var cachedTasks: Map<LocalDate, List<com.mss.thebigcalendar.data.model.Activity>> = emptyMap()

    // Navigation Functions
    fun onPreviousMonth() {
        val newMonth = _uiState.value.displayedYearMonth.minusMonths(1)
        _uiState.update { it.copy(displayedYearMonth = newMonth) }
        // Carregar atividades do novo mês
        updateActivitiesForNewMonth(newMonth)
    }

    fun onNextMonth() {
        val newMonth = _uiState.value.displayedYearMonth.plusMonths(1)
        _uiState.update { it.copy(displayedYearMonth = newMonth) }
        // Carregar atividades do novo mês
        updateActivitiesForNewMonth(newMonth)
    }

    fun onPreviousYear() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.minusYears(1)) }
    }

    fun onNextYear() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.plusYears(1)) }
    }

    fun onGoToToday() {
        val currentState = _uiState.value
        val today = LocalDate.now()
        val currentMonth = YearMonth.now()
        val monthChanged = currentState.displayedYearMonth != currentMonth
        
        _uiState.update {
            it.copy(
                displayedYearMonth = currentMonth,
                selectedDate = today
            )
        }
        
        // Se o mês mudou, carregar atividades do novo mês
        if (monthChanged) {
            updateActivitiesForNewMonth(currentMonth)
        } else {
            // Se apenas o dia mudou, apenas atualizar a data selecionada
            updateSelectedDateInCalendar()
        }
    }
    
    fun onDateSelected(date: LocalDate) {
        val state = _uiState.value
        val shouldOpenModal = state.selectedDate.isEqual(date) && date.month == state.displayedYearMonth.month
        val monthChanged = state.displayedYearMonth.month != date.month || state.displayedYearMonth.year != date.year
        
        _uiState.update { 
            it.copy(
                selectedDate = date,
                displayedYearMonth = if (monthChanged) YearMonth.from(date) else it.displayedYearMonth
            ) 
        }
        
        if (monthChanged) {
            updateActivitiesForNewMonth(YearMonth.from(date))
        } else {
            updateSelectedDateInCalendar()
        }
        
        // Se clicou na mesma data, abrir modal de criação
        if (shouldOpenModal) {
            openCreateActivityModal()
        }
    }

    fun onYearlyMonthClicked(yearMonth: YearMonth) {
        val currentState = _uiState.value
        val monthChanged = currentState.displayedYearMonth != yearMonth
        
        _uiState.update { 
            it.copy(
                displayedYearMonth = yearMonth,
                viewMode = ViewMode.MONTHLY
            ) 
        }
        
        if (monthChanged) {
            updateActivitiesForNewMonth(yearMonth)
        }
    }

    fun onViewModeChange(newMode: ViewMode) {
        _uiState.update { it.copy(viewMode = newMode) }
    }

    fun onFilterChange(key: String, value: Boolean) {
        val currentFilters = _uiState.value.filterOptions
        val newFilters = when (key) {
            "holidays" -> currentFilters.copy(showHolidays = value)
            "saintDays" -> currentFilters.copy(showSaintDays = value)
            "events" -> currentFilters.copy(showEvents = value)
            "tasks" -> currentFilters.copy(showTasks = value)
            "notes" -> currentFilters.copy(showNotes = value)
            "birthdays" -> currentFilters.copy(showBirthdays = value)
            // "jsonHolidays" -> currentFilters.copy(showJsonHolidays = value) // Implementar se necessário
            else -> currentFilters
        }
        
        _uiState.update { it.copy(filterOptions = newFilters) }
        
        // Salvar configurações
        viewModelScope.launch {
            settingsRepository.saveFilterOptions(newFilters)
        }
        
        // Atualizar UI
        updateAllDateDependentUI()
    }

    fun onThemeChange(newTheme: Theme) {
        _uiState.update { it.copy(theme = newTheme) }
        
        // Salvar configuração
        viewModelScope.launch {
            settingsRepository.saveTheme(newTheme)
        }
    }

    // Helper functions for navigation
    private fun updateActivitiesForNewMonth(newMonth: YearMonth) {
        viewModelScope.launch {
            activityRepository.getActivitiesForMonth(newMonth).collect { activities ->
                _uiState.update { it.copy(activities = activities) }
                
                // Limpar cache quando as atividades mudam
                clearCalendarCache()
                clearActivityCache()
                // Usar debounce para evitar múltiplas atualizações rápidas
                updateAllDateDependentUI()
            }
        }
    }

    private fun updateSelectedDateInCalendar() {
        val state = _uiState.value
        val updatedCalendarDays = state.calendarDays.map { day ->
            day.copy(isSelected = day.date.isEqual(state.selectedDate))
        }
        _uiState.update { it.copy(calendarDays = updatedCalendarDays) }
    }

    private fun updateAllDateDependentUI() {
        // Cancelar job anterior se existir
        updateJob?.cancel()
        
        // Criar novo job com debounce
        updateJob = viewModelScope.launch {
            kotlinx.coroutines.delay(100) // Debounce de 100ms
            
            updateCalendarDays()
            updateTasksForSelectedDate()
            updateHolidaysForSelectedDate()
            updateSaintDaysForSelectedDate()
            updateJsonCalendarActivitiesForSelectedDate()
        }
    }

    private fun clearCalendarCache() {
        cachedCalendarDays = null
        lastUpdateParams = null
    }

    private fun clearActivityCache() {
        cachedBirthdays = emptyMap()
        cachedNotes = emptyMap()
        cachedTasks = emptyMap()
    }

    // Placeholder functions that will be implemented in the main ViewModel
    private fun openCreateActivityModal() {
        // This will be handled by the main CalendarViewModel
    }

    private fun updateCalendarDays() {
        // This will be implemented in the main CalendarViewModel
    }

    private fun updateTasksForSelectedDate() {
        // This will be implemented in the main CalendarViewModel
    }

    private fun updateHolidaysForSelectedDate() {
        // This will be implemented in the main CalendarViewModel
    }

    private fun updateSaintDaysForSelectedDate() {
        // This will be implemented in the main CalendarViewModel
    }

    private fun updateJsonCalendarActivitiesForSelectedDate() {
        // This will be implemented in the main CalendarViewModel
    }
}
