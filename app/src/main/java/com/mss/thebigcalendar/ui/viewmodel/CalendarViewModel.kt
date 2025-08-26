package com.mss.thebigcalendar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.CalendarDay
import com.mss.thebigcalendar.data.model.CalendarFilterOptions
import com.mss.thebigcalendar.data.model.CalendarUiState
import com.mss.thebigcalendar.data.model.Holiday
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.data.model.ViewMode
import com.mss.thebigcalendar.data.repository.ActivityRepository
import com.mss.thebigcalendar.data.repository.HolidayRepository
import com.mss.thebigcalendar.data.repository.SettingsRepository
import com.mss.thebigcalendar.service.NotificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val activityRepository = ActivityRepository(application)
    private val holidayRepository = HolidayRepository(application)

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadData()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.theme.collect { theme ->
                _uiState.update { it.copy(theme = theme) }
            }
        }
        viewModelScope.launch {
            settingsRepository.username.collect { username ->
                _uiState.update { it.copy(username = username) }
            }
        }
        viewModelScope.launch {
            settingsRepository.filterOptions.collect { filterOptions ->
                _uiState.update { it.copy(filterOptions = filterOptions) }
                updateAllDateDependentUI()
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            activityRepository.activities.collect { activities ->
                _uiState.update { it.copy(activities = activities) }
                updateAllDateDependentUI()
            }
        }
        loadInitialHolidaysAndSaints()
    }

    private fun loadInitialHolidaysAndSaints() {
        viewModelScope.launch {
            val nationalHolidaysList = holidayRepository.getNationalHolidays()
            val saintDaysList = holidayRepository.getSaintDays()

            _uiState.update { currentState ->
                currentState.copy(
                    nationalHolidays = nationalHolidaysList.associateBy { LocalDate.parse(it.date) },
                    saintDays = saintDaysList.associateBy { it.date }
                )
            }
            updateCalendarDays()
        }
    }

    private fun updateCalendarDays() {
        val state = _uiState.value
        val firstDayOfMonth = state.displayedYearMonth.atDay(1)
        val daysFromPrevMonthOffset = (firstDayOfMonth.dayOfWeek.value % 7)
        val gridStartDate = firstDayOfMonth.minusDays(daysFromPrevMonthOffset.toLong())

        val newCalendarDays = List(42) { i ->
            val date = gridStartDate.plusDays(i.toLong())
            val tasksForThisDay = if (state.filterOptions.showTasks || state.filterOptions.showEvents) {
                state.activities.filter { activity ->
                    val activityDate = LocalDate.parse(activity.date)
                    val typeMatches = (state.filterOptions.showTasks && activity.activityType == ActivityType.TASK) ||
                            (state.filterOptions.showEvents && activity.activityType == ActivityType.EVENT)
                    activityDate.isEqual(date) && typeMatches
                }.sortedWith(compareByDescending<Activity> { it.categoryColor?.toIntOrNull() ?: 0 }.thenBy { it.startTime ?: LocalTime.MIN })
            } else {
                emptyList()
            }

            val holidayForThisDay = if (state.filterOptions.showHolidays) state.nationalHolidays[date] else null
            val saintDayForThisDay = if (state.filterOptions.showSaintDays) state.saintDays[date.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"))] else null

            CalendarDay(
                date = date,
                isCurrentMonth = date.month == state.displayedYearMonth.month,
                isSelected = date.isEqual(state.selectedDate),
                isToday = date.isEqual(LocalDate.now()),
                tasks = tasksForThisDay,
                holiday = holidayForThisDay ?: saintDayForThisDay,
                isWeekend = date.dayOfWeek == java.time.DayOfWeek.SATURDAY || date.dayOfWeek == java.time.DayOfWeek.SUNDAY,
                isNationalHoliday = holidayForThisDay?.type == com.mss.thebigcalendar.data.model.HolidayType.NATIONAL,
                isSaintDay = saintDayForThisDay != null
            )
        }
        _uiState.update { it.copy(calendarDays = newCalendarDays) }
    }

    private fun updateTasksForSelectedDate() {
        val state = _uiState.value
        val tasks = if (state.filterOptions.showTasks || state.filterOptions.showEvents) {
            state.activities.filter { activity ->
                val activityDate = LocalDate.parse(activity.date)
                val typeMatches = (state.filterOptions.showTasks && activity.activityType == ActivityType.TASK) ||
                        (state.filterOptions.showEvents && activity.activityType == ActivityType.EVENT)
                activityDate.isEqual(state.selectedDate) && typeMatches
            }.sortedWith(compareByDescending<Activity> { it.categoryColor?.toIntOrNull() ?: 0 }.thenBy { it.startTime ?: LocalTime.MIN })
        } else {
            emptyList()
        }
        _uiState.update { it.copy(tasksForSelectedDate = tasks) }
    }

    private fun updateHolidaysForSelectedDate() {
        val state = _uiState.value
        val holidays = if (state.filterOptions.showHolidays) {
            state.nationalHolidays[state.selectedDate]?.let { listOf(it) } ?: emptyList()
        } else {
            emptyList()
        }
        _uiState.update { it.copy(holidaysForSelectedDate = holidays) }
    }

    private fun updateSaintDaysForSelectedDate() {
        val state = _uiState.value
        val saints = if (state.filterOptions.showSaintDays) {
            val monthDay = state.selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"))
            state.saintDays[monthDay]?.let { listOf(it) } ?: emptyList()
        } else {
            emptyList()
        }
        _uiState.update { it.copy(saintDaysForSelectedDate = saints) }
    }

    private fun updateAllDateDependentUI() {
        updateCalendarDays()
        updateTasksForSelectedDate()
        updateHolidaysForSelectedDate()
        updateSaintDaysForSelectedDate()
    }

    fun onPreviousMonth() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.minusMonths(1)) }
        updateAllDateDependentUI()
    }

    fun onNextMonth() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.plusMonths(1)) }
        updateAllDateDependentUI()
    }

    fun onPreviousYear() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.minusYears(1)) }
    }

    fun onNextYear() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.plusYears(1)) }
    }

    fun onDateSelected(date: LocalDate) {
        val state = _uiState.value
        val shouldOpenModal = state.selectedDate.isEqual(date) && date.month == state.displayedYearMonth.month

        _uiState.update {
            it.copy(
                selectedDate = date,
                displayedYearMonth = if (it.displayedYearMonth.month != date.month || it.displayedYearMonth.year != date.year) {
                    YearMonth.from(date)
                } else {
                    it.displayedYearMonth
                },
                activityIdWithDeleteButtonVisible = null // Esconde o botão ao selecionar nova data
            )
        }
        updateAllDateDependentUI()

        if (shouldOpenModal) {
            openCreateActivityModal()
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
        updateAllDateDependentUI()
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
        viewModelScope.launch {
            settingsRepository.saveFilterOptions(newFilters)
        }
    }

    fun onThemeChange(newTheme: Theme) {
        viewModelScope.launch {
            settingsRepository.saveTheme(newTheme)
        }
    }

    fun onSaveActivity(activityData: Activity) {
        viewModelScope.launch {
            activityRepository.saveActivity(activityData)
            
            // ✅ Agendar notificação se configurada
            if (activityData.notificationSettings.isEnabled && 
                activityData.notificationSettings.notificationType != com.mss.thebigcalendar.data.model.NotificationType.NONE &&
                activityData.startTime != null) {
                
                val notificationService = NotificationService(getApplication())
                notificationService.scheduleNotification(activityData)
            }
            
            closeCreateActivityModal()
        }
    }

    fun onDeleteActivityConfirm() {
        viewModelScope.launch {
            _uiState.value.activityIdToDelete?.let { activityId ->
                // ✅ Cancelar notificação antes de deletar
                val notificationService = NotificationService(getApplication())
                notificationService.cancelNotification(activityId)
                
                activityRepository.deleteActivity(activityId)
            }
            cancelDeleteActivity()
        }
    }

    

    fun onBackupRequest() { println("ViewModel: Pedido de backup recebido.") }
    fun onRestoreRequest() { println("ViewModel: Pedido de restauração recebido.") }

    fun openSidebar() = _uiState.update { it.copy(isSidebarOpen = true) }
    fun closeSidebar() = _uiState.update { it.copy(isSidebarOpen = false) }

    fun onNavigateToSettings(screen: String?) {
        _uiState.update { it.copy(currentSettingsScreen = screen, isSidebarOpen = false) }
    }

    fun openCreateActivityModal(activity: Activity? = null, activityType: ActivityType = ActivityType.EVENT) {
        val template = activity ?: Activity(
            id = "new",
            title = "",
            description = null,
            date = _uiState.value.selectedDate.toString(),
            startTime = null,
            endTime = null,
            isAllDay = true,
            location = null,
            categoryColor = if (activityType == ActivityType.TASK) "#3B82F6" else "#F43F5E",
            activityType = activityType,
            recurrenceRule = null
        )
        _uiState.update { it.copy(activityToEdit = template, activityIdWithDeleteButtonVisible = null) }
    }

    fun closeCreateActivityModal() = _uiState.update { it.copy(activityToEdit = null) }

    fun onTaskLongPressed(activityId: String) {
        _uiState.update { it.copy(activityIdWithDeleteButtonVisible = activityId) }
    }

    fun hideDeleteButton() {
        _uiState.update { it.copy(activityIdWithDeleteButtonVisible = null) }
    }

    fun requestDeleteActivity(activityId: String) {
        _uiState.update { it.copy(activityIdToDelete = activityId, activityIdWithDeleteButtonVisible = null) }
    }

    fun cancelDeleteActivity() = _uiState.update { it.copy(activityIdToDelete = null) }

    fun onSaintDayClick(saint: Holiday) {
        _uiState.update { it.copy(saintInfoToShow = saint) }
    }

    fun onSaintInfoDialogDismiss() {
        _uiState.update { it.copy(saintInfoToShow = null) }
    }
}