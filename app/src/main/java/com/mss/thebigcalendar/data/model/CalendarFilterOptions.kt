package com.mss.thebigcalendar.data.model

data class CalendarFilterOptions(
    val showHolidays: Boolean = true,
    val showSaintDays: Boolean = false, // Desativado por padrão para consistência com o sidebar
    val showEvents: Boolean = true,
    val showTasks: Boolean = true,
    val showBirthdays: Boolean = true,
    val showNotes: Boolean = true
)
