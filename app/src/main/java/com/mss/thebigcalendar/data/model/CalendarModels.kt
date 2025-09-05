package com.mss.thebigcalendar.data.model

import android.content.Intent
import androidx.compose.runtime.Immutable
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.mss.thebigcalendar.data.service.BackupInfo
//import com.google.android.gms.common.config.GservicesValue.value
//import com.google.android.gms.common.config.GservicesValue.value
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

// Enum para os diferentes tipos de atividade (já deve existir)
enum class ActivityType {
    EVENT,
    TASK,
    NOTE,
    BIRTHDAY
}

// Enum para os níveis de visibilidade
enum class VisibilityLevel {
    LOW,    // Baixa
    MEDIUM, // Média
    HIGH    // Alta
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
    val recurrenceRule: String?,
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val isCompleted: Boolean = false,
    val visibility: VisibilityLevel = VisibilityLevel.LOW, // Nova opção de visibilidade
    val showInCalendar: Boolean = true, // Nova opção para mostrar no calendário
    val isFromGoogle: Boolean = false,
    val excludedDates: List<String> = emptyList() // Datas de instâncias recorrentes excluídas
)

// Representa cada célula individual na grade do calendário
@Immutable
data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isSelected: Boolean = false,
    val isToday: Boolean = false,
    val tasks: List<Activity> = emptyList(), // NOVO: Lista de tarefas para este dia
    val holiday: Holiday? = null,
    val isWeekend: Boolean = false,
    val isNationalHoliday: Boolean = false,
    val isSaintDay: Boolean = false
)

// Definição da classe de filtros
data class FilterOptions(
    val showHolidays: Boolean = false,
    val showSaintDays: Boolean = false,
    val showEvents: Boolean = false,
    val showTasks: Boolean = false,
    val showNotes: Boolean = false,
    val showBirthdays: Boolean = false
)

// Classe de estado principal da UI
data class CalendarUiState(
    val displayedYearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val viewMode: ViewMode = ViewMode.MONTHLY,
    val theme: Theme = Theme.LIGHT,
    val username: String = "Usuário",
    val activities: List<Activity> = emptyList(),
    val nationalHolidays: Map<LocalDate, Holiday> = emptyMap(),
    val saintDays: Map<String, Holiday> = emptyMap(), // MM-dd -> Holiday
    val commemorativeDates: Map<LocalDate, Holiday> = emptyMap(),
    val filterOptions: CalendarFilterOptions = CalendarFilterOptions(),
    val isSidebarOpen: Boolean = false,
    val activityToEdit: Activity? = null,
    val activityIdToDelete: String? = null,
    val activityIdWithDeleteButtonVisible: String? = null,
    val saintInfoToShow: Holiday? = null,
    val currentSettingsScreen: String? = null,
    val calendarDays: List<CalendarDay> = emptyList(),
    val tasksForSelectedDate: List<Activity> = emptyList(), // NOVO: Lista de tarefas para o dia selecionado
    val birthdaysForSelectedDate: List<Activity> = emptyList(), // Lista de aniversários para o dia selecionado
    val notesForSelectedDate: List<Activity> = emptyList(), // Lista de notas para o dia selecionado
    val holidaysForSelectedDate: List<Holiday> = emptyList(),
    val saintDaysForSelectedDate: List<Holiday> = emptyList(),
    val googleSignInAccount: GoogleSignInAccount? = null,
    val signInIntent: Intent? = null,
    val loginMessage: String? = null,
    val isSyncing: Boolean = false,
    val syncErrorMessage: String? = null,
    val syncProgress: SyncProgress? = null,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearchScreenOpen: Boolean = false,
    val isChartScreenOpen: Boolean = false,
    val isCompletedTasksScreenOpen: Boolean = false,
    val deletedActivities: List<DeletedActivity> = emptyList(),
    val isTrashScreenOpen: Boolean = false,
    val isBackupScreenOpen: Boolean = false,
    val backupMessage: String? = null,
    val needsStoragePermission: Boolean = false,
    val backupFiles: List<BackupInfo> = emptyList(),
    val showCompletedActivities: Boolean = false,
    val completedActivities: List<Activity> = emptyList(),
    val trashSortOrder: String = "newest_first",
    val lastGoogleSyncTime: Long = 0L,
    val showMoonPhases: Boolean = true,

)

enum class Theme { LIGHT, DARK, SYSTEM }
enum class ViewMode { MONTHLY, YEARLY }
enum class HolidayType { NATIONAL, COMMEMORATIVE, SAINT }
@Immutable
data class Holiday(val name: String, val date: String, val type: HolidayType, val summary: String? = null, val wikipediaLink: String? = null)