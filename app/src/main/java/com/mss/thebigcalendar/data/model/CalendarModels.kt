package com.mss.thebigcalendar.data.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Enums que definem estados e tipos fixos no aplicativo.
 */
enum class ViewMode { MONTHLY, YEARLY }

enum class Theme { LIGHT, DARK }

enum class ActivityType(val displayName: String) {
    EVENT("Evento"),
    TASK("Tarefa"),
    BIRTHDAY("Aniversário")
}

enum class RecurrenceOption(val displayName: String) {
    NONE("Não se repete"),
    DAILY("Todos os dias"),
    WEEKLY("Toda semana"),
    MONTHLY("Todo mês"),
    YEARLY("Todo ano"),
    CUSTOM("Personalizado...")
}

enum class HolidayType { NATIONAL, SAINT, COMMEMORATIVE }

enum class FrequencyUnit(val displayName: String) {
    DAY("dia(s)"),
    WEEK("semana(s)"),
    MONTH("mês(es)"),
    YEAR("ano(s)")
}


/**
 * Data classes que representam os modelos de dados principais.
 * São o equivalente das suas 'interfaces' em TypeScript.
 */
data class Activity(
    val id: String,
    val date: LocalDate, // Usamos o tipo nativo do Java para datas. Mais seguro!
    val title: String,
    val isAllDay: Boolean,
    val startTime: LocalTime?, // Tipo nativo para horas, pode ser nulo.
    val endTime: LocalTime?,
    val location: String?,
    val description: String?,
    val categoryColor: Long, // Usamos o tipo Color do Compose, em vez de uma string de classe CSS.
    val activityType: ActivityType,
    val recurrenceRule: String?
)

data class Holiday(
    val date: String, // Mantido como String para facilitar o parsing dos dados mocados (ex: "01-01")
    val name: String,
    val type: HolidayType
)

data class CalendarFilterOptions(
    val showHolidays: Boolean = true,
    val showSaintDays: Boolean = true,
    val showCommemorativeDates: Boolean = true,
    val showEvents: Boolean = true,
    val showTasks: Boolean = true
)

data class CustomRecurrenceValues(
    val interval: Int = 1,
    val frequencyUnit: FrequencyUnit = FrequencyUnit.WEEK,
    val daysOfWeek: List<String> = emptyList(), // "SU", "MO", etc.
    val endsOn: String = "never", // "never", "date", "occurrences"
    val endDate: LocalDate? = null,
    val occurrences: Int? = null
)

/**
 * Constantes de UI.
 */
val MONTH_NAMES_PT = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
)

val DAY_ABBREVIATIONS_PT = listOf("D", "S", "T", "Q", "Q", "S", "S")

val DAY_NAMES_PT = listOf(
    "Domingo", "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado"
)

val CUSTOM_RECURRENCE_DAY_CODES = listOf("SU", "MO", "TU", "WE", "TH", "FR", "SA")