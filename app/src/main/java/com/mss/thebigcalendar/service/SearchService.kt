package com.mss.thebigcalendar.service

import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.Holiday
import com.mss.thebigcalendar.data.model.SearchResult
import com.mss.thebigcalendar.data.model.toSearchResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Serviço responsável por realizar pesquisas no calendário
 */
class SearchService {
    
    /**
     * Realiza uma pesquisa por texto em todas as fontes de dados do calendário
     */
    fun search(
        query: String,
        activities: List<Activity>,
        nationalHolidays: Map<LocalDate, Holiday>,
        saintDays: Map<String, Holiday>,
        commemorativeDates: Map<LocalDate, Holiday>
    ): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        
        val normalizedQuery = query.trim().lowercase(Locale("pt", "BR"))
        val results = mutableListOf<SearchResult>()
        
        // Pesquisar em atividades
        val activityResults = searchActivities(activities, normalizedQuery)
        results.addAll(activityResults)
        
        // Pesquisar em feriados nacionais
        val holidayResults = searchHolidays(nationalHolidays, normalizedQuery)
        results.addAll(holidayResults)
        
        // Pesquisar em dias de santos
        val saintResults = searchSaintDays(saintDays, normalizedQuery)
        results.addAll(saintResults)
        
        // Pesquisar em datas comemorativas
        val commemorativeResults = searchCommemorativeDates(commemorativeDates, normalizedQuery)
        results.addAll(commemorativeResults)
        
        // Ordenar resultados por relevância e data
        return results.sortedWith(
            compareBy<SearchResult> { result ->
                // Priorizar resultados que começam com a query
                when {
                    result.title.lowercase(Locale("pt", "BR")).startsWith(normalizedQuery) -> 0
                    result.title.lowercase(Locale("pt", "BR")).contains(normalizedQuery) -> 1
                    else -> 2
                }
            }.thenBy { result ->
                // Depois ordenar por data (mais próximos primeiro)
                result.date?.let { date ->
                    val today = LocalDate.now()
                    kotlin.math.abs(date.toEpochDay() - today.toEpochDay())
                } ?: Long.MAX_VALUE
            }
        )
    }
    
    /**
     * Pesquisa em atividades (eventos, tarefas, aniversários)
     */
    private fun searchActivities(
        activities: List<Activity>,
        query: String
    ): List<SearchResult> {
        return activities.filter { activity ->
            activity.title.lowercase(Locale("pt", "BR")).contains(query) ||
            (activity.description?.lowercase(Locale("pt", "BR"))?.contains(query) == true) ||
            (activity.location?.lowercase(Locale("pt", "BR"))?.contains(query) == true)
        }.map { it.toSearchResult() }
    }
    
    /**
     * Pesquisa em feriados nacionais
     */
    private fun searchHolidays(
        holidays: Map<LocalDate, Holiday>,
        query: String
    ): List<SearchResult> {
        return holidays.values.filter { holiday ->
            holiday.name.lowercase(Locale("pt", "BR")).contains(query) ||
            (holiday.summary?.lowercase(Locale("pt", "BR"))?.contains(query) == true)
        }.map { it.toSearchResult() }
    }
    
    /**
     * Pesquisa em dias de santos
     */
    private fun searchSaintDays(
        saintDays: Map<String, Holiday>,
        query: String
    ): List<SearchResult> {
        return saintDays.values.filter { saint ->
            saint.name.lowercase(Locale("pt", "BR")).contains(query) ||
            (saint.summary?.lowercase(Locale("pt", "BR"))?.contains(query) == true)
        }.map { it.toSearchResult() }
    }
    
    /**
     * Pesquisa em datas comemorativas
     */
    private fun searchCommemorativeDates(
        commemorativeDates: Map<LocalDate, Holiday>,
        query: String
    ): List<SearchResult> {
        return commemorativeDates.values.filter { commemorative ->
            commemorative.name.lowercase(Locale("pt", "BR")).contains(query) ||
            (commemorative.summary?.lowercase(Locale("pt", "BR"))?.contains(query) == true)
        }.map { it.toSearchResult() }
    }
    
    /**
     * Pesquisa por data específica (formato dd/MM ou dd/MM/yyyy)
     */
    fun searchByDate(
        dateQuery: String,
        activities: List<Activity>,
        nationalHolidays: Map<LocalDate, Holiday>,
        saintDays: Map<String, Holiday>,
        commemorativeDates: Map<LocalDate, Holiday>
    ): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        try {
            val targetDate = parseDateQuery(dateQuery)
            if (targetDate != null) {
                // Buscar atividades na data específica
                val dateActivities = activities.filter { 
                    LocalDate.parse(it.date) == targetDate 
                }.map { it.toSearchResult() }
                results.addAll(dateActivities)
                
                // Buscar feriados na data específica
                nationalHolidays[targetDate]?.let { holiday ->
                    results.add(holiday.toSearchResult())
                }
                
                // Buscar datas comemorativas na data específica
                commemorativeDates[targetDate]?.let { commemorative ->
                    results.add(commemorative.toSearchResult())
                }
            }
        } catch (e: Exception) {
            // Ignora erros de parsing de data
        }
        
        return results
    }
    
    /**
     * Tenta fazer o parsing de uma query de data
     */
    private fun parseDateQuery(dateQuery: String): LocalDate? {
        val normalizedQuery = dateQuery.trim()
        
        return try {
            when {
                // Formato dd/MM
                normalizedQuery.matches(Regex("\\d{1,2}/\\d{1,2}")) -> {
                    val parts = normalizedQuery.split("/")
                    val day = parts[0].toInt()
                    val month = parts[1].toInt()
                    val currentYear = LocalDate.now().year
                    LocalDate.of(currentYear, month, day)
                }
                // Formato dd/MM/yyyy
                normalizedQuery.matches(Regex("\\d{1,2}/\\d{1,2}/\\d{4}")) -> {
                    val parts = normalizedQuery.split("/")
                    val day = parts[0].toInt()
                    val month = parts[1].toInt()
                    val year = parts[2].toInt()
                    LocalDate.of(year, month, day)
                }
                // Formato dd-MM
                normalizedQuery.matches(Regex("\\d{1,2}-\\d{1,2}")) -> {
                    val parts = normalizedQuery.split("-")
                    val day = parts[0].toInt()
                    val month = parts[1].toInt()
                    val currentYear = LocalDate.now().year
                    LocalDate.of(currentYear, month, day)
                }
                // Formato dd-MM-yyyy
                normalizedQuery.matches(Regex("\\d{1,2}-\\d{1,2}-\\d{4}")) -> {
                    val parts = normalizedQuery.split("-")
                    val day = parts[0].toInt()
                    val month = parts[1].toInt()
                    val year = parts[2].toInt()
                    LocalDate.of(year, month, day)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
