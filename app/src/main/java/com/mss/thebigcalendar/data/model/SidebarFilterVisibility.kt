package com.mss.thebigcalendar.data.model

data class SidebarFilterVisibility(
    val showHolidays: Boolean = true,
    val showSaintDays: Boolean = false, // Desativado por padrão
    val showEvents: Boolean = true,
    val showTasks: Boolean = true,
    val showBirthdays: Boolean = true,
    val showNotes: Boolean = true,
    val showCompletedTasks: Boolean = true,
    val showMoonPhases: Boolean = true
)

