package com.mss.thebigcalendar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.CalendarDay
import com.mss.thebigcalendar.data.model.CalendarUiState
import com.mss.thebigcalendar.data.model.FilterOptions
import com.mss.thebigcalendar.data.model.Holiday // Import para Holiday se for usar no loadInitialData
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.data.model.ViewMode
import com.mss.thebigcalendar.data.repository.HolidayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID
import kotlin.comparisons.*

class CalendarViewModel : ViewModel() {

    // Supondo que HolidayRepository exista.
    private val holidayRepository = HolidayRepository()

    private val _uiState = MutableStateFlow(CalendarUiState(theme = Theme.SYSTEM))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        updateCalendarDays()
        updateTasksForSelectedDate()
        updateHolidaysForSelectedDate()
        updateSaintDaysForSelectedDate()
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val nationalHolidaysList = holidayRepository.getNationalHolidays()
            val saintDaysList = holidayRepository.getSaintDays()

            _uiState.update { currentState ->
                currentState.copy(
                    nationalHolidays = nationalHolidaysList.associateBy { LocalDate.parse(it.date) },
                    saintDays = saintDaysList.associateBy { it.date } // MM-dd -> Holiday
                )
            }
            updateCalendarDays()
        }
    }

    /**
     * Atualiza a lista de CalendarDay no uiState com base no displayedYearMonth, selectedDate e atividades.
     */
    private fun updateCalendarDays() {
        val currentUiStateValue = _uiState.value
        val currentActivities = currentUiStateValue.activities
        val currentYearMonth = currentUiStateValue.displayedYearMonth
        val currentSelectedDate = currentUiStateValue.selectedDate

        val firstDayOfMonth = currentYearMonth.atDay(1)
        val daysFromPrevMonthOffset = (firstDayOfMonth.dayOfWeek.value % 7)
        val gridStartDate = firstDayOfMonth.minusDays(daysFromPrevMonthOffset.toLong())

        val newCalendarDays = List(42) { i ->
            val date = gridStartDate.plusDays(i.toLong())
            val tasksForThisDay = currentActivities.filter { activity ->
                LocalDate.parse(activity.date).isEqual(date)
            }.sortedWith(compareByDescending<Activity> { it.categoryColor?.toIntOrNull() ?: 0 }.thenBy { it.startTime ?: LocalTime.MIN })
            
            val holidayForThisDay = if (currentUiStateValue.filterOptions.showHolidays) {
                currentUiStateValue.nationalHolidays[date]
            } else {
                null
            }

            val saintDayForThisDay = if (currentUiStateValue.filterOptions.showSaintDays) {
                val monthDay = date.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"))
                currentUiStateValue.saintDays[monthDay]
            } else {
                null
            }

            CalendarDay(
                date = date,
                isCurrentMonth = date.month == currentYearMonth.month,
                isSelected = date.isEqual(currentSelectedDate),
                tasks = tasksForThisDay,
                holiday = holidayForThisDay ?: saintDayForThisDay,
                isWeekend = date.dayOfWeek == java.time.DayOfWeek.SATURDAY || date.dayOfWeek == java.time.DayOfWeek.SUNDAY,
                isNationalHoliday = holidayForThisDay?.type == com.mss.thebigcalendar.data.model.HolidayType.NATIONAL,
                isSaintDay = saintDayForThisDay != null
            )
        }
        _uiState.update { it.copy(calendarDays = newCalendarDays) }
    }

    /**
     * Atualiza a lista de tarefas para o dia atualmente selecionado.
     */
    private fun updateTasksForSelectedDate() {
        val currentUiStateValue = _uiState.value
        val selectedDate = currentUiStateValue.selectedDate
        val tasks = currentUiStateValue.activities.filter { activity ->
            LocalDate.parse(activity.date).isEqual(selectedDate)
        }.sortedWith(compareByDescending<Activity> { it.categoryColor?.toIntOrNull() ?: 0 }
            .thenBy { it.startTime ?: LocalTime.MIN })

        _uiState.update { it.copy(tasksForSelectedDate = tasks) }
    }

    private fun updateHolidaysForSelectedDate() {
        val currentUiStateValue = _uiState.value
        val selectedDate = currentUiStateValue.selectedDate
        val holidays = mutableListOf<Holiday>()
        if (currentUiStateValue.filterOptions.showHolidays) {
            currentUiStateValue.nationalHolidays[selectedDate]?.let { holidays.add(it) }
        }
        _uiState.update { it.copy(holidaysForSelectedDate = holidays) }
    }

    private fun updateSaintDaysForSelectedDate() {
        val currentUiStateValue = _uiState.value
        val selectedDate = currentUiStateValue.selectedDate
        val saints = mutableListOf<Holiday>()
        if (currentUiStateValue.filterOptions.showSaintDays) {
            val monthDay = selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"))
            currentUiStateValue.saintDays[monthDay]?.let { saints.add(it) }
        }
        _uiState.update { it.copy(saintDaysForSelectedDate = saints) }
    }

    // --- MÉTODOS PÚBLICOS (EVENTOS VINDOS DA UI) ---

    fun onPreviousMonth() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.minusMonths(1)) }
        updateCalendarDays()
        updateTasksForSelectedDate()
        updateHolidaysForSelectedDate()
        updateSaintDaysForSelectedDate()
    }

    fun onNextMonth() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.plusMonths(1)) }
        updateCalendarDays()
        updateTasksForSelectedDate()
        updateHolidaysForSelectedDate()
        updateSaintDaysForSelectedDate()
    }

    fun onPreviousYear() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.minusYears(1)) }
        // Para visualização anual, não precisamos recalcular os 'calendarDays' do mês imediatamente,
        // mas a YearlyCalendarView vai redesenhar com o novo ano.
        // Se voltarmos para a visualização mensal, os dias corretos serão calculados.
    }

    fun onNextYear() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.plusYears(1)) }
    }

    fun onDateSelected(date: LocalDate) {
        val currentUiStateValue = _uiState.value
        val shouldOpenModal = currentUiStateValue.selectedDate.isEqual(date) &&
                date.month == currentUiStateValue.displayedYearMonth.month &&
                date.year == currentUiStateValue.displayedYearMonth.year

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
        updateTasksForSelectedDate()
        updateHolidaysForSelectedDate()
        updateSaintDaysForSelectedDate()

        if (shouldOpenModal) {
            // Decidimos se abrimos para criar evento ou tarefa baseado em algum critério
            // ou oferecemos opções. Por agora, vamos focar em tarefas.
            openCreateActivityModal(activityType = ActivityType.TASK)
        }
    }

    fun onYearlyMonthClicked(yearMonth: YearMonth) {
        _uiState.update {
            it.copy(
                displayedYearMonth = yearMonth,
                selectedDate = yearMonth.atDay(1),
                viewMode = ViewMode.MONTHLY,
                isSidebarOpen = false
            )
        }
        updateCalendarDays()
        updateTasksForSelectedDate()
        updateHolidaysForSelectedDate()
        updateSaintDaysForSelectedDate()
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
        // Re-filtrar os dias do calendário e tarefas do dia se a visibilidade das tarefas mudar
        if (key == "showTasks" || key == "showEvents" || key == "showHolidays" || key == "showSaintDays") { // Adicione outros filtros se necessário
            updateCalendarDays()
            updateTasksForSelectedDate()
            updateHolidaysForSelectedDate()
            updateSaintDaysForSelectedDate()
        }
    }

    fun onThemeChange(newTheme: Theme) {
        _uiState.update { it.copy(theme = newTheme) }
        // Lógica futura para salvar no DataStore
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
                    activities = currentActivities, // A ordenação acontece em updateTasksForSelectedDate
                    activityToEdit = null
                )
            }
            updateCalendarDays()
            updateTasksForSelectedDate()
            updateHolidaysForSelectedDate()
            updateSaintDaysForSelectedDate()
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
            updateCalendarDays()
            updateTasksForSelectedDate()
            updateHolidaysForSelectedDate()
            updateSaintDaysForSelectedDate()
        }
    }

    fun onSaveSettings(username: String, theme: Theme) {
        _uiState.update { it.copy(username = username, theme = theme, isSettingsModalOpen = false) }
        // Lógica futura para salvar no DataStore
    }

    fun onBackupRequest() { println("ViewModel: Pedido de backup recebido.") }
    fun onRestoreRequest() { println("ViewModel: Pedido de restauração recebido.") }

    // --- Funções para controlar a visibilidade de componentes da UI ---

    fun openSidebar() = _uiState.update { it.copy(isSidebarOpen = true) }
    fun closeSidebar() = _uiState.update { it.copy(isSidebarOpen = false) }

    fun openSettingsModal(category: String) = _uiState.update {
        it.copy(isSettingsModalOpen = true, settingsCategory = category, isSidebarOpen = false)
    }
    fun closeSettingsModal() = _uiState.update { it.copy(isSettingsModalOpen = false) }

    fun openCreateActivityModal(activity: Activity? = null, activityType: ActivityType = ActivityType.EVENT) {
        val templateDate = _uiState.value.selectedDate
        val newActivityTemplate = Activity(
            id = "new",
            title = "",
            description = null,
            date = templateDate.toString(), // Converte LocalDate para String "yyyy-MM-dd"
            startTime = null,
            endTime = null,
            isAllDay = true,
            location = null,
            categoryColor = if (activityType == ActivityType.TASK) "#3B82F6" else "#F43F5E", // Azul para tarefa, Rosa para evento
            activityType = activityType,
            recurrenceRule = null
        )
        _uiState.update { it.copy(activityToEdit = activity ?: newActivityTemplate) }
    }

    fun closeCreateActivityModal() = _uiState.update { it.copy(activityToEdit = null) }
    fun requestDeleteActivity(activityId: String) = _uiState.update { it.copy(activityIdToDelete = activityId) }
    fun cancelDeleteActivity() = _uiState.update { it.copy(activityIdToDelete = null) }
}