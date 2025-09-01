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
        
        // Adicionar a atividade base
        instances.add(baseActivity)
        
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
            instances.add(createRecurringInstance(baseActivity, currentDate))
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
            instances.add(createRecurringInstance(baseActivity, currentDate))
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
            
            instances.add(createRecurringInstance(baseActivity, adjustedDate))
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
            instances.add(createRecurringInstance(baseActivity, currentDate))
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
            
            val actualEndDate = if (until != null && until.isBefore(endDate)) until else endDate
            
            when (freq) {
                "DAILY" -> generateCustomDailyInstances(baseActivity, baseDate, actualEndDate, interval, instances)
                "WEEKLY" -> generateCustomWeeklyInstances(baseActivity, baseDate, actualEndDate, interval, instances)
                "MONTHLY" -> generateCustomMonthlyInstances(baseActivity, baseDate, actualEndDate, interval, instances)
                "YEARLY" -> generateCustomYearlyInstances(baseActivity, baseDate, actualEndDate, interval, instances)
            }
        } catch (e: Exception) {
        }
    }

    /**
     * Gera instâncias diárias customizadas com intervalo
     */
    private fun generateCustomDailyInstances(
        baseActivity: Activity,
        baseDate: LocalDate,
        endDate: LocalDate,
        interval: Int,
        instances: MutableList<Activity>
    ) {
        var currentDate = baseDate.plusDays(interval.toLong())
        while (!currentDate.isAfter(endDate)) {
            instances.add(createRecurringInstance(baseActivity, currentDate))
            currentDate = currentDate.plusDays(interval.toLong())
        }
    }

    /**
     * Gera instâncias semanais customizadas com intervalo
     */
    private fun generateCustomWeeklyInstances(
        baseActivity: Activity,
        baseDate: LocalDate,
        endDate: LocalDate,
        interval: Int,
        instances: MutableList<Activity>
    ) {
        var currentDate = baseDate.plusWeeks(interval.toLong())
        while (!currentDate.isAfter(endDate)) {
            instances.add(createRecurringInstance(baseActivity, currentDate))
            currentDate = currentDate.plusWeeks(interval.toLong())
        }
    }

    /**
     * Gera instâncias mensais customizadas com intervalo
     */
    private fun generateCustomMonthlyInstances(
        baseActivity: Activity,
        baseDate: LocalDate,
        endDate: LocalDate,
        interval: Int,
        instances: MutableList<Activity>
    ) {
        var currentDate = baseDate.plusMonths(interval.toLong())
        while (!currentDate.isAfter(endDate)) {
            val targetDay = minOf(baseDate.dayOfMonth, currentDate.lengthOfMonth())
            val adjustedDate = currentDate.withDayOfMonth(targetDay)
            
            instances.add(createRecurringInstance(baseActivity, adjustedDate))
            currentDate = currentDate.plusMonths(interval.toLong())
        }
    }

    /**
     * Gera instâncias anuais customizadas com intervalo
     */
    private fun generateCustomYearlyInstances(
        baseActivity: Activity,
        baseDate: LocalDate,
        endDate: LocalDate,
        interval: Int,
        instances: MutableList<Activity>
    ) {
        var currentDate = baseDate.plusYears(interval.toLong())
        while (!currentDate.isAfter(endDate)) {
            instances.add(createRecurringInstance(baseActivity, currentDate))
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
    fun getNextOccurrence(activity: Activity, fromDate: LocalDate = LocalDate.now()): LocalDate? {
        val rule = activity.recurrenceRule ?: return null
        if (!isRecurring(activity)) return null
        
        val baseDate = LocalDate.parse(activity.date)
        return when (rule) {
            "DAILY" -> baseDate.plusDays(1)
            "WEEKLY" -> baseDate.plusWeeks(1)
            "MONTHLY" -> {
                val nextMonth = baseDate.plusMonths(1)
                val targetDay = minOf(baseDate.dayOfMonth, nextMonth.lengthOfMonth())
                nextMonth.withDayOfMonth(targetDay)
            }
            "YEARLY" -> baseDate.plusYears(1)
            else -> null
        }
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
