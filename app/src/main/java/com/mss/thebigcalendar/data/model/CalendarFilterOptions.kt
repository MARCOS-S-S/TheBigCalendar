package com.mss.thebigcalendar.data.model

data class CalendarFilterOptions(
    val showHolidays: Boolean = true,
    val showSaintDays: Boolean = true,
    val showCommemorativeDates: Boolean = true,
    val showEvents: Boolean = true,
    val showTasks: Boolean = true,
    val showBirthdays: Boolean = true,
    val showNotes: Boolean = false
)
