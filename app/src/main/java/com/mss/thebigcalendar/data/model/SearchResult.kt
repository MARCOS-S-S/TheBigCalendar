package com.mss.thebigcalendar.data.model

import java.time.LocalDate

/**
 * Representa um resultado de pesquisa no calendário
 */
data class SearchResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val date: LocalDate?,
    val type: Type,
    val originalData: Any // Pode ser Activity, Holiday, etc.
) {
    enum class Type {
        ACTIVITY,    // Eventos, tarefas, aniversários
        HOLIDAY,     // Feriados nacionais
        SAINT_DAY    // Dias de santos
    }
}

/**
 * Extensões para converter diferentes tipos de dados em SearchResult
 */
fun Activity.toSearchResult(): SearchResult {
    val date = try {
        LocalDate.parse(this.date)
    } catch (e: Exception) {
        null
    }
    
    val subtitle = when (this.activityType) {
        ActivityType.EVENT -> "Evento"
        ActivityType.TASK -> "Tarefa"
        ActivityType.BIRTHDAY -> "Aniversário"
    }
    
    return SearchResult(
        id = this.id,
        title = this.title,
        subtitle = subtitle,
        date = date,
        type = SearchResult.Type.ACTIVITY,
        originalData = this
    )
}

fun Holiday.toSearchResult(): SearchResult {
    val date = parseHolidayDate(this.date, this.type)
    val subtitle = when (this.type) {
        HolidayType.NATIONAL -> "Feriado Nacional"
        HolidayType.COMMEMORATIVE -> "Data Comemorativa"
        HolidayType.SAINT -> "Dia de Santo"
    }
    
    return SearchResult(
        id = this.date, // Usa a data como ID para feriados
        title = this.name,
        subtitle = subtitle,
        date = date,
        type = if (this.type == HolidayType.SAINT) SearchResult.Type.SAINT_DAY else SearchResult.Type.HOLIDAY,
        originalData = this
    )
}

/**
 * Função auxiliar para fazer o parsing de datas de feriados
 * que podem estar em diferentes formatos
 */
private fun parseHolidayDate(dateString: String, type: HolidayType): LocalDate? {
    return try {
        when (type) {
            HolidayType.SAINT -> {
                // Para dias de santos, o formato é MM-dd
                if (dateString.matches(Regex("\\d{2}-\\d{2}"))) {
                    val parts = dateString.split("-")
                    val month = parts[0].toInt()
                    val day = parts[1].toInt()
                    val currentYear = java.time.LocalDate.now().year
                    java.time.LocalDate.of(currentYear, month, day)
                } else {
                    // Tentar formato ISO padrão
                    java.time.LocalDate.parse(dateString)
                }
            }
            else -> {
                // Para outros tipos, usar formato ISO padrão
                java.time.LocalDate.parse(dateString)
            }
        }
    } catch (e: Exception) {
        // Se falhar o parsing, retornar null
        null
    }
}
