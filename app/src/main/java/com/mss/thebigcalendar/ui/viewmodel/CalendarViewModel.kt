// Caminho: app/src/main/java/com/mss/thebigcalendar/ui/viewmodel/CalendarViewModel.kt

package com.mss.thebigcalendar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.CalendarDay
import com.mss.thebigcalendar.data.model.CalendarUiState
import com.mss.thebigcalendar.data.model.FilterOptions
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.data.model.ViewMode
import com.mss.thebigcalendar.data.repository.HolidayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID

class CalendarViewModel : ViewModel() {

    private val holidayRepository = HolidayRepository() // Supondo que esta classe exista

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        updateCalendarDays()
        // loadInitialData() // Descomentar quando o repositório estiver funcional
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Lógica para carregar feriados, etc.
        }
    }

    private fun updateCalendarDays() {
        val currentYearMonth = _uiState.value.displayedYearMonth
        val currentSelectedDate = _uiState.value.selectedDate
        val firstDayOfMonth = currentYearMonth.atDay(1)
        val daysFromPrevMonthOffset = (firstDayOfMonth.dayOfWeek.value % 7)
        val gridStartDate = firstDayOfMonth.minusDays(daysFromPrevMonthOffset.toLong())
        val newCalendarDays = List(42) { i ->
            val date = gridStartDate.plusDays(i.toLong())
            CalendarDay(
                date = date,
                isCurrentMonth = date.month == currentYearMonth.month,
                isSelected = date.isEqual(currentSelectedDate)
            )
        }
        _uiState.update { it.copy(calendarDays = newCalendarDays) }
    }

    // --- MÉTODOS PÚBLICOS (EVENTOS VINDOS DA UI) ---

    fun onPreviousMonth() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.minusMonths(1)) }
        updateCalendarDays()
    }

    fun onNextMonth() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.plusMonths(1)) }
        updateCalendarDays()
    }

    // NOVO: Navegação Anual
    fun onPreviousYear() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.minusYears(1)) }
        // Para a visualização anual, não precisamos recalcular os 'calendarDays' do mês
    }

    // NOVO: Navegação Anual
    fun onNextYear() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.plusYears(1)) }
    }

    fun onDateSelected(date: LocalDate) {
        val currentUiState = _uiState.value
        val shouldOpenModal = currentUiState.selectedDate.isEqual(date) &&
                date.month == currentUiState.displayedYearMonth.month &&
                date.year == currentUiState.displayedYearMonth.year

        _uiState.update {
            it.copy(
                selectedDate = date,
                displayedYearMonth = if (it.displayedYearMonth.month != date.month || it.displayedYearMonth.year != date.year) {
                    YearMonth.from(date)
                } else {
                    it.displayedYearMonth
                }
            )
        }
        updateCalendarDays()

        if (shouldOpenModal) {
            openCreateActivityModal()
        }
    }

    // NOVO: Ação quando um mês é clicado na visualização anual
    fun onYearlyMonthClicked(yearMonth: YearMonth) {
        _uiState.update {
            it.copy(
                displayedYearMonth = yearMonth,
                selectedDate = yearMonth.atDay(1), // Seleciona o dia 1 do mês clicado
                viewMode = ViewMode.MONTHLY, // Muda para a visualização mensal
                isSidebarOpen = false // Fecha a sidebar
            )
        }
        updateCalendarDays() // Prepara os dias para a nova visualização mensal
    }

    fun onViewModeChange(newMode: ViewMode) {
        _uiState.update { it.copy(viewMode = newMode, isSidebarOpen = false) }
    }

    fun onFilterChange(key: String, value: Boolean) {
        val currentFilters = _uiState.value.filterOptions
        val newFilters = when (key) {
            "showHolidays" -> currentFilters.copy(showHolidays = value)
            "showSaintDays" -> currentFilters.copy(showSaintDays = value)
            "showCommemorativeDates" -> currentFilters.copy(showCommemorativeDates = value)
            "showEvents" -> currentFilters.copy(showEvents = value)
            "showTasks" -> currentFilters.copy(showTasks = value)
            else -> currentFilters
        }
        _uiState.update { it.copy(filterOptions = newFilters) }
    }

    fun onThemeChange(newTheme: Theme) {
        _uiState.update { it.copy(theme = newTheme) }
    }

    fun onSaveActivity(activityData: Activity) {
        viewModelScope.launch {
            val currentActivities = _uiState.value.activities.toMutableList()
            val activityToSave = if (activityData.id == "new" || activityData.id.isBlank()) {
                activityData.copy(id = UUID.randomUUID().toString())
            } else {
                activityData
            }
            val existingIndex = currentActivities.indexOfFirst { it.id == activityToSave.id }
            if (existingIndex != -1) {
                currentActivities[existingIndex] = activityToSave
            } else {
                currentActivities.add(activityToSave)
            }
            _uiState.update {
                it.copy(
                    activities = currentActivities.sortedBy { act -> act.startTime ?: LocalTime.MIN },
                    activityToEdit = null
                )
            }
        }
    }

    fun onDeleteActivityConfirm() {
        viewModelScope.launch {
            val activityId = _uiState.value.activityIdToDelete ?: return@launch
            _uiState.update {
                it.copy(
                    activities = it.activities.filterNot { act -> act.id == activityId },
                    activityIdToDelete = null
                )
            }
        }
    }

    fun onSaveSettings(username: String, theme: Theme) {
        _uiState.update { it.copy(username = username, theme = theme, isSettingsModalOpen = false) }
    }

    fun onBackupRequest() { println("ViewModel: Pedido de backup recebido.") }
    fun onRestoreRequest() { println("ViewModel: Pedido de restauração recebido.") }

    fun openSidebar() = _uiState.update { it.copy(isSidebarOpen = true) }
    fun closeSidebar() = _uiState.update { it.copy(isSidebarOpen = false) }

    fun openSettingsModal(category: String) = _uiState.update {
        it.copy(isSettingsModalOpen = true, settingsCategory = category, isSidebarOpen = false)
    }
    fun closeSettingsModal() = _uiState.update { it.copy(isSettingsModalOpen = false) }

    fun openCreateActivityModal(activity: Activity? = null) {
        val templateDate = _uiState.value.selectedDate
        val newActivityTemplate = Activity(
            id = "new",
            title = "",
            description = null,
            date = templateDate.toString(),
            startTime = null,
            endTime = null,
            isAllDay = true,
            location = null,
            categoryColor = "#F43F5E",
            activityType = ActivityType.EVENT,
            recurrenceRule = null
        )
        _uiState.update { it.copy(activityToEdit = activity ?: newActivityTemplate) }
    }

    fun closeCreateActivityModal() = _uiState.update { it.copy(activityToEdit = null) }
    fun requestDeleteActivity(activityId: String) = _uiState.update { it.copy(activityIdToDelete = activityId) }
    fun cancelDeleteActivity() = _uiState.update { it.copy(activityIdToDelete = null) }
}