package com.mss.thebigcalendar.service

import com.mss.thebigcalendar.data.model.Activity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Serviço para gerenciar repetições de atividades
 */
class RecurrenceService {

    /**
     * Gera todas as instâncias de uma atividade baseada em sua regra de repetição
     */
    fun generateRecurringInstances(
        baseActivity: Activity,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Activity> {
        val recurrenceRule = baseActivity.recurrenceRule ?: return listOf(baseActivity)
        
        if (recurrenceRule.isEmpty() || recurrenceRule == "NONE") {
            return listOf(baseActivity)
        }

        val instances = mutableListOf<Activity>()
        val baseDate = LocalDate.parse(baseActivity.date)
        
        // NÃO adicionar a atividade base aqui - ela já aparece no calendário como atividade original
        
        // Gerar instâncias recorrentes
        when (recurrenceRule) {
            "DAILY" -> generateDailyInstances(baseActivity, baseDate, endDate, instances)
            "WEEKLY" -> generateWeeklyInstances(baseActivity, baseDate, endDate, instances)
            "MONTHLY" -> generateMonthlyInstances(baseActivity, baseDate, endDate, instances)
            "YEARLY" -> generateYearlyInstances(baseActivity, baseDate, endDate, instances)
            else -> parseCustomRecurrenceRule(baseActivity, baseDate, endDate, instances)
        }
        
        return instances
    }

    /**
     * Gera instâncias diárias
     */
    private fun generateDailyInstances(
        baseActivity: Activity,
        baseDate: LocalDate,
        endDate: LocalDate,
        instances: MutableList<Activity>
    ) {
        var currentDate = baseDate.plusDays(1)
        while (!currentDate.isAfter(endDate)) {
            // Verificar se esta data não foi excluída
            if (!baseActivity.excludedDates.contains(currentDate.toString())) {
                instances.add(createRecurringInstance(baseActivity, currentDate))
            }
            currentDate = currentDate.plusDays(1)
        }
    }

    /**
     * Gera instâncias semanais
     */
    private fun generateWeeklyInstances(
        baseActivity: Activity,
        baseDate: LocalDate,
        endDate: LocalDate,
        instances: MutableList<Activity>
    ) {
        var currentDate = baseDate.plusWeeks(1)
        while (!currentDate.isAfter(endDate)) {
            // Verificar se esta data não foi excluída
            if (!baseActivity.excludedDates.contains(currentDate.toString())) {
                instances.add(createRecurringInstance(baseActivity, currentDate))
            }
            currentDate = currentDate.plusWeeks(1)
        }
    }

    /**
     * Gera instâncias mensais
     */
    private fun generateMonthlyInstances(
        baseActivity: Activity,
        baseDate: LocalDate,
        endDate: LocalDate,
        instances: MutableList<Activity>
    ) {
        var currentDate = baseDate.plusMonths(1)
        while (!currentDate.isAfter(endDate)) {
            // Manter o mesmo dia do mês, ajustando para meses com menos dias
            val targetDay = minOf(baseDate.dayOfMonth, currentDate.lengthOfMonth())
            val adjustedDate = currentDate.withDayOfMonth(targetDay)
            
            // Verificar se esta data não foi excluída
            if (!baseActivity.excludedDates.contains(adjustedDate.toString())) {
                instances.add(createRecurringInstance(baseActivity, adjustedDate))
            }
            currentDate = currentDate.plusMonths(1)
        }
    }

    /**
     * Gera instâncias anuais
     */
    private fun generateYearlyInstances(
        baseActivity: Activity,
        baseDate: LocalDate,
        endDate: LocalDate,
        instances: MutableList<Activity>
    ) {
        var currentDate = baseDate.plusYears(1)
        while (!currentDate.isAfter(endDate)) {
            // Verificar se esta data não foi excluída
            if (!baseActivity.excludedDates.contains(currentDate.toString())) {
                instances.add(createRecurringInstance(baseActivity, currentDate))
            }
            currentDate = currentDate.plusYears(1)
        }
    }

    /**
     * Parse de regras de repetição customizadas (formato: FREQ=DAILY;INTERVAL=1;UNTIL=2025-12-31)
     */
    private fun parseCustomRecurrenceRule(
        baseActivity: Activity,
        baseDate: LocalDate,
        endDate: LocalDate,
        instances: MutableList<Activity>
    ) {
        try {
            val rule = baseActivity.recurrenceRule ?: return
            val parts = rule.split(";")
            val freq = parts.find { it.startsWith("FREQ=") }?.substringAfter("=")
            val interval = parts.find { it.startsWith("INTERVAL=") }?.substringAfter("=")?.toIntOrNull() ?: 1
            val until = parts.find { it.startsWith("UNTIL=") }?.substringAfter("=")?.let { LocalDate.parse(it) }
            val count = parts.find { it.startsWith("COUNT=") }?.substringAfter("=")?.toIntOrNull()
            val byDay = parts.find { it.startsWith("BYDAY=") }?.substringAfter("=")
            
            // Determinar data de término baseada em COUNT ou UNTIL
            val actualEndDate = when {
                count != null -> {
                    // Se COUNT está definido, calcular a data de término baseada no número de ocorrências
                    // COUNT inclui a atividade base, então precisamos de (count-1) ocorrências adicionais
                    when (freq) {
                        "DAILY" -> baseDate.plusDays(count * interval.toLong())
                        "WEEKLY" -> baseDate.plusWeeks(count * interval.toLong())
                        "MONTHLY" -> baseDate.plusMonths(count * interval.toLong())
                        "YEARLY" -> baseDate.plusYears(count * interval.toLong())
                        else -> endDate
                    }
                }
                until != null && until.isBefore(endDate) -> until
                else -> endDate
            }
            
            when (freq) {
                "DAILY" -> generateCustomDailyInstancesWithCount(baseActivity, baseDate, actualEndDate, interval, count, instances)
                "WEEKLY" -> generateCustomWeeklyInstancesWithCount(baseActivity, baseDate, actualEndDate, interval, count, byDay, instances)
                "MONTHLY" -> generateCustomMonthlyInstancesWithCount(baseActivity, baseDate, actualEndDate, interval, count, instances)
                "YEARLY" -> generateCustomYearlyInstancesWithCount(baseActivity, baseDate, actualEndDate, interval, count, instances)
            }
        } catch (e: Exception) {
        }
    }

    /**
     * Gera instâncias diárias customizadas com intervalo e contagem
     */
    private fun generateCustomDailyInstancesWithCount(
        baseActivity: Activity,
        baseDate: LocalDate,
        endDate: LocalDate,
        interval: Int,
        count: Int?,
        instances: MutableList<Activity>
    ) {
        var currentDate = baseDate.plusDays(interval.toLong())
        var occurrenceCount = 0 // Contador de ocorrências (incluindo a base)
        
        while (!currentDate.isAfter(endDate) && (count == null || occurrenceCount < count)) {
            // Verificar se esta data não foi excluída
            if (!baseActivity.excludedDates.contains(currentDate.toString())) {
                instances.add(createRecurringInstance(baseActivity, currentDate))
                occurrenceCount++
            }
            currentDate = currentDate.plusDays(interval.toLong())
        }
    }

    /**
     * Gera instâncias semanais customizadas com intervalo e contagem
     */
    private fun generateCustomWeeklyInstancesWithCount(
        baseActivity: Activity,
        baseDate: LocalDate,
        endDate: LocalDate,
        interval: Int,
        count: Int?,
        byDay: String?,
        instances: MutableList<Activity>
    ) {
        // Se não há BYDAY especificado, usar comportamento padrão (mesmo dia da semana)
        if (byDay == null || byDay.isEmpty()) {
            var currentDate = baseDate.plusWeeks(interval.toLong())
            var occurrenceCount = 0 // Contador de ocorrências (incluindo a base)
            
            while (!currentDate.isAfter(endDate) && (count == null || occurrenceCount < count)) {
                instances.add(createRecurringInstance(baseActivity, currentDate))
                occurrenceCount++
                currentDate = currentDate.plusWeeks(interval.toLong())
            }
            return
        }
        
        // Parse dos dias da semana especificados
        val targetDays = byDay.split(",").mapNotNull { day ->
            when (day.trim()) {
                "SU" -> 7 // Domingo (Java usa 1-7, onde 7 = Domingo)
                "MO" -> 1 // Segunda
                "TU" -> 2 // Terça
                "WE" -> 3 // Quarta
                "TH" -> 4 // Quinta
                "FR" -> 5 // Sexta
                "SA" -> 6 // Sábado
                else -> null
            }
        }
        
        if (targetDays.isEmpty()) return
        
        var occurrenceCount = 0
        var currentWeekStart = baseDate
        
        // Encontrar a próxima semana que contenha os dias especificados
        while (!currentWeekStart.isAfter(endDate) && (count == null || occurrenceCount < count)) {
            // Para cada dia da semana especificado nesta semana
            targetDays.forEach { targetDay ->
                val targetDate = currentWeekStart.with(TemporalAdjusters.nextOrSame(
                    java.time.DayOfWeek.of(targetDay)
                ))
                
                // Verificar se a data está dentro do período e não foi excluída
                if (!targetDate.isAfter(endDate) && 
                    !baseActivity.excludedDates.contains(targetDate.toString()) &&
                    (count == null || occurrenceCount < count)) {
                    instances.add(createRecurringInstance(baseActivity, targetDate))
                    occurrenceCount++
                }
            }
            
            // Avançar para a próxima semana baseada no intervalo
            currentWeekStart = currentWeekStart.plusWeeks(interval.toLong())
        }
    }

    /**
     * Gera instâncias mensais customizadas com intervalo e contagem
     */
    private fun generateCustomMonthlyInstancesWithCount(
        baseActivity: Activity,
        baseDate: LocalDate,
        endDate: LocalDate,
        interval: Int,
        count: Int?,
        instances: MutableList<Activity>
    ) {
        var currentDate = baseDate.plusMonths(interval.toLong())
        var occurrenceCount = 0 // Contador de ocorrências (incluindo a base)
        
        while (!currentDate.isAfter(endDate) && (count == null || occurrenceCount < count)) {
            val targetDay = minOf(baseDate.dayOfMonth, currentDate.lengthOfMonth())
            val adjustedDate = currentDate.withDayOfMonth(targetDay)
            
            instances.add(createRecurringInstance(baseActivity, adjustedDate))
            occurrenceCount++
            currentDate = currentDate.plusMonths(interval.toLong())
        }
    }

    /**
     * Gera instâncias anuais customizadas com intervalo e contagem
     */
    private fun generateCustomYearlyInstancesWithCount(
        baseActivity: Activity,
        baseDate: LocalDate,
        endDate: LocalDate,
        interval: Int,
        count: Int?,
        instances: MutableList<Activity>
    ) {
        var currentDate = baseDate.plusYears(interval.toLong())
        var occurrenceCount = 0 // Contador de ocorrências (incluindo a base)
        
        while (!currentDate.isAfter(endDate) && (count == null || occurrenceCount < count)) {
            instances.add(createRecurringInstance(baseActivity, currentDate))
            occurrenceCount++
            currentDate = currentDate.plusYears(interval.toLong())
        }
    }

    /**
     * Cria uma instância de atividade para uma data específica
     */
    private fun createRecurringInstance(baseActivity: Activity, date: LocalDate): Activity {
        return baseActivity.copy(
            id = "${baseActivity.id}_${date}",
            date = date.toString()
        )
    }

    /**
     * Converte a opção de repetição selecionada para uma regra de repetição
     */
    fun convertRepetitionOptionToRule(option: String): String {
        return when (option) {
            "NONE" -> ""
            "DAILY" -> "DAILY"
            "WEEKLY" -> "WEEKLY"
            "MONTHLY" -> "MONTHLY"
            "YEARLY" -> "YEARLY"
            else -> ""
        }
    }

    /**
     * Verifica se uma atividade é recorrente
     */
    fun isRecurring(activity: Activity): Boolean {
        val rule = activity.recurrenceRule ?: return false
        return rule.isNotEmpty() && rule != "NONE"
    }

    /**
     * Obtém a próxima ocorrência de uma atividade recorrente
     */
    fun getNextOccurrence(activity: Activity, fromDate: LocalDate): LocalDate? {
        val rule = activity.recurrenceRule ?: return null
        if (!isRecurring(activity)) return null

        var currentDate = fromDate
        // Loop a reasonable number of times to find the next valid, non-excluded date
        for (i in 1..365 * 5) { // Check for the next 5 years
            val nextDate = when (rule) {
                "DAILY" -> currentDate.plusDays(1)
                "WEEKLY" -> currentDate.plusWeeks(1)
                "MONTHLY" -> {
                    val baseDate = LocalDate.parse(activity.date)
                    val nextMonth = currentDate.plusMonths(1)
                    val day = minOf(baseDate.dayOfMonth, nextMonth.lengthOfMonth())
                    nextMonth.withDayOfMonth(day)
                }
                "YEARLY" -> {
                    val baseDate = LocalDate.parse(activity.date)
                    val nextYear = currentDate.plusYears(1)
                    val day = minOf(baseDate.dayOfMonth, nextYear.lengthOfMonth())
                    nextYear.withDayOfMonth(day)
                }
                else -> return null
            }

            if (!activity.excludedDates.contains(nextDate.toString())) {
                return nextDate
            }
            currentDate = nextDate // Continue searching from the excluded date
        }
        return null // No valid occurrence found in the next 5 years
    }

    /**
     * Obtém todas as ocorrências de uma atividade em um período
     */
    fun getOccurrencesInPeriod(
        activity: Activity,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<LocalDate> {
        if (!isRecurring(activity)) {
            val activityDate = LocalDate.parse(activity.date)
            return if (activityDate.isBefore(startDate) || activityDate.isAfter(endDate)) {
                emptyList()
            } else {
                listOf(activityDate)
            }
        }

        val occurrences = mutableListOf<LocalDate>()
        val baseDate = LocalDate.parse(activity.date)
        
        // Adicionar a data base se estiver no período
        if (!baseDate.isBefore(startDate) && !baseDate.isAfter(endDate)) {
            occurrences.add(baseDate)
        }
        
        // Gerar ocorrências recorrentes
        val rule = activity.recurrenceRule ?: return occurrences
        
        when (rule) {
            "DAILY" -> {
                var currentDate = baseDate.plusDays(1)
                while (!currentDate.isAfter(endDate)) {
                    if (!currentDate.isBefore(startDate)) {
                        occurrences.add(currentDate)
                    }
                    currentDate = currentDate.plusDays(1)
                }
            }
            "WEEKLY" -> {
                var currentDate = baseDate.plusWeeks(1)
                while (!currentDate.isAfter(endDate)) {
                    if (!currentDate.isBefore(startDate)) {
                        occurrences.add(currentDate)
                    }
                    currentDate = currentDate.plusWeeks(1)
                }
            }
            "MONTHLY" -> {
                var currentDate = baseDate.plusMonths(1)
                while (!currentDate.isAfter(endDate)) {
                    if (!currentDate.isBefore(startDate)) {
                        val targetDay = minOf(baseDate.dayOfMonth, currentDate.lengthOfMonth())
                        val adjustedDate = currentDate.withDayOfMonth(targetDay)
                        occurrences.add(adjustedDate)
                    }
                    currentDate = currentDate.plusMonths(1)
                }
            }
            "YEARLY" -> {
                var currentDate = baseDate.plusYears(1)
                while (!currentDate.isAfter(endDate)) {
                    if (!currentDate.isBefore(startDate)) {
                        occurrences.add(currentDate)
                    }
                    currentDate = currentDate.plusYears(1)
                }
            }
        }
        
        return occurrences
    }
}
