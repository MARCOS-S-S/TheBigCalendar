package com.mss.thebigcalendar.data.model

import androidx.compose.ui.graphics.toArgb
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Representa um agendamento importado de arquivo JSON
 */
data class JsonSchedule(
    val name: String,
    val date: String, // Formato MM-dd (ex: "01-20")
    val summary: String? = null, // Descrição/resumo do agendamento
    val wikipediaLink: String? = null // Link para mais informações
)

/**
 * Representa a estrutura completa do arquivo JSON de agendamentos
 */
data class JsonScheduleData(
    val title: String,
    val color: String,
    val schedules: List<JsonSchedule>
)

/**
 * Converte JsonSchedule para Activity
 */
fun JsonSchedule.toActivity(
    year: Int,
    calendarTitle: String,
    calendarColor: androidx.compose.ui.graphics.Color
): Activity {
    val scheduleDate = LocalDate.of(year, 
        date.split("-")[0].toInt(), 
        date.split("-")[1].toInt()
    )
    
    // Debug log
    android.util.Log.d("JsonSchedule", "🔄 Convertendo JsonSchedule para Activity:")
    android.util.Log.d("JsonSchedule", "  📋 Name: $name")
    android.util.Log.d("JsonSchedule", "  🔗 Wikipedia Link: $wikipediaLink")
    android.util.Log.d("JsonSchedule", "  📅 Date: $date")
    
    return Activity(
        id = UUID.randomUUID().toString(),
        title = name,
        description = summary ?: "",
        date = scheduleDate.toString(),
        startTime = null, // Sem hora específica
        endTime = null, // Sem hora específica
        isAllDay = true, // Evento de dia inteiro
        categoryColor = String.format("#%08X", calendarColor.toArgb()),
        activityType = ActivityType.EVENT, // Todos são eventos por padrão
        recurrenceRule = null,
        notificationSettings = NotificationSettings(),
        isCompleted = false,
        visibility = VisibilityLevel.MEDIUM, // Visibilidade média por padrão
        showInCalendar = true,
        isFromGoogle = false,
        excludedDates = emptyList(),
        // Marcar como atividade JSON importada usando o campo location
        // (que não é usado para atividades JSON)
        location = "JSON_IMPORTED_${calendarTitle}",
        wikipediaLink = wikipediaLink // Preservar o link da Wikipedia
    ).also { activity ->
        android.util.Log.d("JsonSchedule", "  ✅ Activity criada com Wikipedia Link: ${activity.wikipediaLink}")
    }
}
