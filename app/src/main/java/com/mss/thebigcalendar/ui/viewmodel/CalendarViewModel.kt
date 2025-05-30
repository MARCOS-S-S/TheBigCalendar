// Caminho: app/src/main/java/com/mss/thebigcalendar/ui/viewmodel/CalendarViewModel.kt

package com.mss.thebigcalendar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.CalendarUiState
import com.mss.thebigcalendar.data.model.FilterOptions
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.data.model.ViewMode
import com.mss.thebigcalendar.data.repository.HolidayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID

class CalendarViewModel : ViewModel() {

    private val holidayRepository = HolidayRepository()
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // loadInitialData() // Podemos habilitar isso quando o repositório estiver completo
    }

    fun onPreviousMonth() = _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.minusMonths(1)) }
    fun onNextMonth() = _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.plusMonths(1)) }

    fun onDateSelected(date: LocalDate) {
        val shouldOpenModal = _uiState.value.selectedDate == date
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
        if (shouldOpenModal) {
            openCreateActivityModal()
        }
    }

    fun onViewModeChange(newMode: ViewMode) {
        _uiState.update { it.copy(viewMode = newMode, isSidebarOpen = false) }
    }

    fun onFilterChange(key: String, value: Boolean) {
        // CORREÇÃO: Usando a classe correta 'FilterOptions'
        val newFilters: FilterOptions = when (key) {
            "showHolidays" -> _uiState.value.filterOptions.copy(showHolidays = value)
            "showSaintDays" -> _uiState.value.filterOptions.copy(showSaintDays = value)
            "showCommemorativeDates" -> _uiState.value.filterOptions.copy(showCommemorativeDates = value)
            "showEvents" -> _uiState.value.filterOptions.copy(showEvents = value)
            "showTasks" -> _uiState.value.filterOptions.copy(showTasks = value)
            else -> _uiState.value.filterOptions
        }
        _uiState.update { it.copy(filterOptions = newFilters) }
    }

    fun onSaveActivity(activityData: Activity) {
        viewModelScope.launch {
            val currentActivities = _uiState.value.activities.toMutableList()
            val activityToSave = if (activityData.id.isBlank() || activityData.id == "new") {
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
                    activities = it.activities.filter { act -> act.id != activityId },
                    activityIdToDelete = null
                )
            }
        }
    }

    fun onThemeChange(newTheme: Theme) = _uiState.update { it.copy(theme = newTheme) }

    fun onSaveSettings(username: String, theme: Theme) {
        _uiState.update { it.copy(username = username, theme = theme, isSettingsModalOpen = false) }
    }

    fun onBackupRequest() { println("Pedido de backup recebido.") }
    fun onRestoreRequest() { println("Pedido de restauração recebido.") }

    fun openSidebar() = _uiState.update { it.copy(isSidebarOpen = true) }
    fun closeSidebar() = _uiState.update { it.copy(isSidebarOpen = false) }

    fun openSettingsModal(category: String) = _uiState.update {
        it.copy(isSettingsModalOpen = true, settingsCategory = category, isSidebarOpen = false)
    }
    fun closeSettingsModal() = _uiState.update { it.copy(isSettingsModalOpen = false) }

    fun openCreateActivityModal(activity: Activity? = null) {
        val newActivityTemplate = Activity(
            id = "new",
            // CORREÇÃO: Convertendo LocalDate para String
            date = _uiState.value.selectedDate.toString(),
            title = "",
            isAllDay = true,
            startTime = null,
            endTime = null,
            location = null,
            description = null,
            categoryColor = "#F43F5E", // Formato de string padrão para cores
            activityType = ActivityType.EVENT,
            recurrenceRule = null
        )
        _uiState.update { it.copy(activityToEdit = activity ?: newActivityTemplate) }
    }

    fun closeCreateActivityModal() = _uiState.update { it.copy(activityToEdit = null) }
    fun requestDeleteActivity(activityId: String) = _uiState.update { it.copy(activityIdToDelete = activityId) }
    fun cancelDeleteActivity() = _uiState.update { it.copy(activityIdToDelete = null) }
}