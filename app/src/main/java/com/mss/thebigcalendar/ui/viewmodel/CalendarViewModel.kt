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
import com.mss.thebigcalendar.data.repository.HolidayRepository // Supondo que esta classe exista
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

    // Supondo que HolidayRepository exista. Se não, você pode comentar esta linha
    // e a chamada a loadInitialData() por enquanto.
    private val holidayRepository = HolidayRepository()

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        // Carrega os dias do calendário para o mês/ano inicial
        updateCalendarDays()
        // loadInitialData() // Carrega dados de feriados, etc. (podemos habilitar depois)
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Exemplo de como carregar feriados (precisa que os métodos no repositório existam)
            // val nationalHolidays = holidayRepository.getNationalHolidays()
            //     .associateBy { LocalDate.parse(it.date) } // Supondo que Holiday.date é String
            // val saintDays = holidayRepository.getSaintDays()
            //     .associateBy { it.date } // Supondo que Holiday.date é String "MM-dd"
            // val commemorativeDates = holidayRepository.getCommemorativeDates()
            //     .associateBy { LocalDate.parse(it.date) }

            _uiState.update {
                it.copy(
                    // nationalHolidays = nationalHolidays,
                    // saintDays = saintDays,
                    // commemorativeDates = commemorativeDates
                )
            }
        }
    }

    /**
     * Atualiza a lista de CalendarDay no uiState com base no displayedYearMonth e selectedDate.
     */
    private fun updateCalendarDays() {
        val currentYearMonth = _uiState.value.displayedYearMonth
        val currentSelectedDate = _uiState.value.selectedDate

        val firstDayOfMonth = currentYearMonth.atDay(1)
        // DayOfWeek.MONDAY.value é 1, DayOfWeek.SUNDAY.value é 7.
        // Queremos que a grade comece no domingo.
        // Se o primeiro dia do mês é domingo (valor 7), o offset é 0.
        // Se o primeiro dia do mês é segunda (valor 1), o offset é 1.
        val daysFromPrevMonthOffset = (firstDayOfMonth.dayOfWeek.value % 7)

        val gridStartDate = firstDayOfMonth.minusDays(daysFromPrevMonthOffset.toLong())

        val newCalendarDays = mutableListOf<CalendarDay>()
        var dateIterator = gridStartDate
        repeat(42) { // 6 semanas * 7 dias
            newCalendarDays.add(
                CalendarDay(
                    date = dateIterator,
                    isCurrentMonth = dateIterator.month == currentYearMonth.month,
                    isSelected = dateIterator.isEqual(currentSelectedDate)
                )
            )
            dateIterator = dateIterator.plusDays(1)
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

    fun onDateSelected(date: LocalDate) {
        val currentUiState = _uiState.value
        // Verifica se a data clicada é a mesma já selecionada E se é do mês corrente da visualização
        val shouldOpenModal = currentUiState.selectedDate.isEqual(date) &&
                date.month == currentUiState.displayedYearMonth.month &&
                date.year == currentUiState.displayedYearMonth.year

        _uiState.update {
            it.copy(
                selectedDate = date,
                displayedYearMonth = if (it.displayedYearMonth.month != date.month || it.displayedYearMonth.year != date.year) {
                    YearMonth.from(date) // Muda o mês/ano da visualização se a data selecionada for de outro mês
                } else {
                    it.displayedYearMonth // Mantém o mês/ano da visualização
                }
            )
        }
        updateCalendarDays() // Sempre atualiza os dias para refletir a nova seleção

        if (shouldOpenModal) {
            openCreateActivityModal()
        }
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
                    activities = currentActivities.sortedBy { act -> act.startTime ?: LocalTime.MIN },
                    activityToEdit = null // Fecha o modal
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
                    activityIdToDelete = null // Fecha o modal de confirmação
                )
            }
        }
    }

    fun onSaveSettings(username: String, theme: Theme) {
        _uiState.update { it.copy(username = username, theme = theme, isSettingsModalOpen = false) }
        // Lógica futura para salvar no DataStore
    }

    fun onBackupRequest() {
        println("ViewModel: Pedido de backup recebido.")
        // Implementar lógica de backup
    }

    fun onRestoreRequest() {
        println("ViewModel: Pedido de restauração recebido.")
        // Implementar lógica de restauração
    }

    // --- Funções para controlar a visibilidade de componentes da UI ---

    fun openSidebar() = _uiState.update { it.copy(isSidebarOpen = true) }
    fun closeSidebar() = _uiState.update { it.copy(isSidebarOpen = false) }

    fun openSettingsModal(category: String) = _uiState.update {
        it.copy(isSettingsModalOpen = true, settingsCategory = category, isSidebarOpen = false)
    }
    fun closeSettingsModal() = _uiState.update { it.copy(isSettingsModalOpen = false) }

    fun openCreateActivityModal(activity: Activity? = null) {
        val templateDate = _uiState.value.selectedDate
        val newActivityTemplate = Activity(
            id = "new", // ID temporário para nova atividade
            title = "",
            description = null,
            date = templateDate.toString(), // Converte LocalDate para String "yyyy-MM-dd"
            startTime = null,
            endTime = null,
            isAllDay = true,
            location = null,
            categoryColor = "#F43F5E", // Cor padrão (exemplo: rosa)
            activityType = ActivityType.EVENT,
            recurrenceRule = null
        )
        _uiState.update { it.copy(activityToEdit = activity ?: newActivityTemplate) }
    }

    fun closeCreateActivityModal() = _uiState.update { it.copy(activityToEdit = null) }

    fun requestDeleteActivity(activityId: String) = _uiState.update { it.copy(activityIdToDelete = activityId) }
    fun cancelDeleteActivity() = _uiState.update { it.copy(activityIdToDelete = null) }
}