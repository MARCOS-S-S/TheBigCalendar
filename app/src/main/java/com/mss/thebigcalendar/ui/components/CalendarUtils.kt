package com.mss.thebigcalendar.ui.components

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Retorna uma lista com as abreviações dos dias da semana, começando por Domingo.
 * Ex: [DOM, SEG, TER, QUA, QUI, SEX, SAB]
 */

fun getContrastingTextColor(backgroundColor: Color): Color {
    // Fórmula simples de luminância percebida
    val luminance = (0.299 * backgroundColor.red +
            0.587 * backgroundColor.green +
            0.114 * backgroundColor.blue)
    // Se a luminância for maior que 0.5, a cor de fundo é considerada "clara",
    // então usamos texto preto. Caso contrário, texto branco.
    return if (luminance > 0.5) Color.Black else Color.White
}

fun getWeekDayAbbreviations(locale: Locale = Locale("pt", "BR")): List<String> {
    // DayOfWeek.values() retorna [MONDAY, TUESDAY, ..., SUNDAY]
    // Queremos [SUNDAY, MONDAY, ..., SATURDAY] para a exibição comum de calendários.
    val daysOfWeek = DayOfWeek.values()
    val sunday = daysOfWeek.first { it == DayOfWeek.SUNDAY }
    val otherDays = daysOfWeek.filterNot { it == DayOfWeek.SUNDAY }
    val orderedDays = listOf(sunday) + otherDays // Domingo primeiro

    return orderedDays.map {
        it.getDisplayName(TextStyle.SHORT_STANDALONE, locale).uppercase(locale)
    }
}

/**
 * Retorna uma lista de objetos CalendarDay para o mês/ano especificado.
 * Inclui dias do mês anterior/seguinte para preencher a grade de 6 semanas.
 */
fun generateCalendarDays(
    yearMonth: YearMonth,
    selectedDate: LocalDate
): List<com.mss.thebigcalendar.data.model.CalendarDay> {
    val firstDayOfMonth = yearMonth.atDay(1)
    val firstDayOfWeekValue = firstDayOfMonth.dayOfWeek.value // MONDAY (1) to SUNDAY (7)
    val daysFromPrevMonthOffset = (firstDayOfWeekValue % 7) // Offset para iniciar a semana no Domingo

    val gridStartDate = firstDayOfMonth.minusDays(daysFromPrevMonthOffset.toLong())

    val calendarDays = mutableListOf<com.mss.thebigcalendar.data.model.CalendarDay>()
    var currentDateIterator = gridStartDate

    // Gera 42 dias (6 semanas * 7 dias)
    repeat(42) {
        val day = com.mss.thebigcalendar.data.model.CalendarDay(
            date = currentDateIterator,
            isCurrentMonth = currentDateIterator.month == yearMonth.month,
            isSelected = currentDateIterator.isEqual(selectedDate)
        )
        calendarDays.add(day)
        currentDateIterator = currentDateIterator.plusDays(1)
    }
    return calendarDays
}