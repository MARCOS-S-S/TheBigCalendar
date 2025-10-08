package com.mss.thebigcalendar.data.service

import android.content.Context
import com.google.gson.Gson
import com.mss.thebigcalendar.data.model.MonthName
import com.mss.thebigcalendar.data.model.MoonPhasesJson
import com.mss.thebigcalendar.data.model.PhaseName
import com.mss.thebigcalendar.ui.components.MoonPhase
import com.mss.thebigcalendar.ui.components.MoonPhaseType
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Serviço para ler e processar o arquivo JSON de fases da lua
 */
class MoonPhasesJsonService(private val context: Context) {
    
    private val gson = Gson()
    private var moonPhasesData: MoonPhasesJson? = null
    
    /**
     * Carrega o arquivo JSON de fases da lua
     */
    private fun loadMoonPhasesData(): MoonPhasesJson? {
        if (moonPhasesData != null) {
            return moonPhasesData
        }
        
        return try {
            // Tentar primeiro o arquivo específico do Brasil
            val jsonString = try {
                context.assets.open("brazil_moon_phases.json").bufferedReader().use { it.readText() }
            } catch (e: IOException) {
                // Fallback para o arquivo original
                context.assets.open("moon_phases.json").bufferedReader().use { it.readText() }
            }
            moonPhasesData = gson.fromJson(jsonString, MoonPhasesJson::class.java)
            moonPhasesData
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Obtém as fases da lua para um mês específico
     */
    fun getMoonPhasesForMonth(yearMonth: YearMonth, userTimezone: ZoneId = ZoneId.systemDefault()): List<MoonPhase> {
        val data = loadMoonPhasesData() ?: return emptyList()
        
        // Verificar se o ano está disponível no JSON
        if (data.year != yearMonth.year) {
            return emptyList()
        }
        
        val monthName = MonthName.fromNumber(yearMonth.monthValue) ?: return emptyList()
        val monthPhases = data.months[monthName.portuguese] ?: return emptyList()
        
        val phases = monthPhases.map { phaseJson ->
            val phaseType = PhaseName.fromPortuguese(phaseJson.phase)?.moonPhaseType ?: MoonPhaseType.NEW_MOON
            
            // Calcular iluminação baseada na fase
            val illumination = when (phaseType) {
                MoonPhaseType.NEW_MOON -> 0.0
                MoonPhaseType.FIRST_QUARTER -> 0.5
                MoonPhaseType.FULL_MOON -> 1.0
                MoonPhaseType.LAST_QUARTER -> 0.5
                else -> 0.0
            }
            
            // Processar data - priorizar data_hora (horário local do Brasil)
            val date = if (phaseJson.localDateTime != null) {
                // Usar data/hora local do Brasil (já no fuso correto)
                println("🇧🇷 Usando horário local do Brasil: ${phaseJson.localDateTime}")
                parseLocalDateTime(phaseJson.localDateTime)
            } else if (phaseJson.utcDate != null) {
                // Converter data UTC para o fuso horário do usuário
                println("🔄 Convertendo UTC: ${phaseJson.utcDate} para $userTimezone")
                convertUtcToUserTimezone(phaseJson.utcDate, userTimezone)
            } else if (phaseJson.day != null) {
                // Fallback para o campo dia (formato antigo)
                println("📅 Usando campo dia: ${phaseJson.day}")
                LocalDate.of(yearMonth.year, yearMonth.monthValue, phaseJson.day)
            } else {
                // Fallback para o primeiro dia do mês
                println("⚠️ Fallback para dia 1")
                LocalDate.of(yearMonth.year, yearMonth.monthValue, 1)
            }
            
            MoonPhase(date, phaseType, illumination)
        }
        
        // Log para debug (remover em produção)
        phases.forEach { phase ->
            println("🌙 Fase da Lua: ${phase.date} - ${phase.phase} (Fuso: $userTimezone)")
        }
        
        return phases.sortedBy { it.date }
    }
    
    /**
     * Processa uma data/hora local do Brasil (formato: "2025-09-07T15:08")
     */
    private fun parseLocalDateTime(localDateTimeString: String): LocalDate {
        return try {
            // Parse da data/hora local (formato: "2025-09-07T15:08")
            val localDateTime = LocalDateTime.parse(localDateTimeString)
            
            // Log para debug
            println("📅 Data local parseada: $localDateTime")
            
            // Retornar apenas a data (sem horário)
            localDateTime.toLocalDate()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: tentar extrair apenas a data da string
            try {
                val datePart = localDateTimeString.substring(0, 10) // "2025-09-07"
                LocalDate.parse(datePart)
            } catch (e2: Exception) {
                e2.printStackTrace()
                LocalDate.now() // Fallback final
            }
        }
    }

    /**
     * Converte uma data UTC para o fuso horário do usuário
     */
    private fun convertUtcToUserTimezone(utcDateString: String, userTimezone: ZoneId): LocalDate {
        return try {
            // Parse da data UTC (formato: "2025-09-07T18:08Z")
            val utcDateTime = ZonedDateTime.parse(utcDateString)
            
            // Converter para o fuso horário do usuário
            val userDateTime = utcDateTime.withZoneSameInstant(userTimezone)
            
            // Log para debug
            println("🕐 UTC: $utcDateTime -> $userTimezone: $userDateTime")
            
            // Retornar apenas a data (sem horário)
            userDateTime.toLocalDate()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: tentar extrair apenas a data da string
            try {
                val datePart = utcDateString.substring(0, 10) // "2025-09-07"
                LocalDate.parse(datePart)
            } catch (e2: Exception) {
                e2.printStackTrace()
                LocalDate.now() // Fallback final
            }
        }
    }
    
    /**
     * Obtém as fases da lua para um mês específico com ajuste de fuso horário
     */
    fun getMoonPhasesForMonthWithTimezone(
        yearMonth: YearMonth, 
        userTimezone: ZoneId = ZoneId.systemDefault()
    ): List<MoonPhase> {
        return getMoonPhasesForMonth(yearMonth, userTimezone)
    }
    
    /**
     * Verifica se há dados disponíveis para um ano específico
     */
    fun hasDataForYear(year: Int): Boolean {
        val data = loadMoonPhasesData()
        return data?.year == year
    }
    
    /**
     * Obtém o ano dos dados disponíveis
     */
    fun getAvailableYear(): Int? {
        val data = loadMoonPhasesData()
        return data?.year
    }
}
