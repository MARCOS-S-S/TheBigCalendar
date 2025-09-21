package com.mss.thebigcalendar.data.model

import androidx.compose.ui.graphics.Color

/**
 * Representa um agendamento JSON como um Holiday para exibição no calendário
 */
data class JsonHoliday(
    val id: String,
    val name: String,
    val date: String, // Formato MM-dd
    val summary: String?,
    val wikipediaLink: String?,
    val calendarId: String,
    val calendarTitle: String,
    val calendarColor: Color
) {
    /**
     * Converte para Holiday para compatibilidade com o sistema existente
     */
    fun toHoliday(): Holiday {
        return Holiday(
            name = name,
            date = date,
            type = HolidayType.JSON_IMPORT,
            summary = summary,
            wikipediaLink = wikipediaLink
        )
    }
}

