package com.mss.thebigcalendar.data.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

// Enum para os diferentes tipos de atividade (já deve existir)
enum class ActivityType {
    EVENT,
    TASK,
    BIRTHDAY
}

// Classe de modelo para Atividades (já deve existir)
data class Activity(
    val id: String,
    val title: String,
    val description: String?,
    val date: String, // "yyyy-MM-dd"
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val isAllDay: Boolean,
    val location: String?,
    val categoryColor: String,
    val activityType: ActivityType, // Importante para diferenciar tarefas
    val recurrenceRule: String?
)

// Representa cada célula individual na grade do calendário
data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isSelected: Boolean = false,
    val tasks: List<Activity> = emptyList() // NOVO: Lista de tarefas para este dia
)

// Definição da classe de filtros (já deve existir)
data class FilterOptions(
    val showHolidays: Boolean = true,
    val showSaintDays: Boolean = true,
    val showCommemorativeDates: Boolean = true,
    val showEvents: Boolean = true,
    val showTasks: Boolean = true // Importante para filtrar tarefas
)

// Classe de estado principal da UI
data class CalendarUiState(
    val displayedYearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val viewMode: ViewMode = ViewMode.MONTHLY,
    val theme: Theme = Theme.LIGHT,
    val username: String = "Usuário",
    val activities: List<Activity> = emptyList(), // Lista principal de todas as atividades (eventos, tarefas)
    val nationalHolidays: Map<LocalDate, Holiday> = emptyMap(),
    val saintDays: Map<String, Holiday> = emptyMap(), // MM-dd -> Holiday
    val commemorativeDates: Map<LocalDate, Holiday> = emptyMap(),
    val filterOptions: FilterOptions = FilterOptions(),
    val isSidebarOpen: Boolean = false,
    val activityToEdit: Activity? = null,
    val activityIdToDelete: String? = null,
    val isSettingsModalOpen: Boolean = false,
    val settingsCategory: String = "General",
    val calendarDays: List<CalendarDay> = emptyList(),
    val tasksForSelectedDate: List<Activity> = emptyList() // NOVO: Lista de tarefas para o dia selecionado
)

// --- Enums e outras classes de modelo (já devem existir) ---
enum class Theme { LIGHT, DARK }
enum class ViewMode { MONTHLY, YEARLY }
enum class HolidayType { NATIONAL, COMMEMORATIVE, SAINT }
data class Holiday(val name: String, val date: String, val type: HolidayType)