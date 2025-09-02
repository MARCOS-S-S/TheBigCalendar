package com.mss.thebigcalendar.data.repository

import android.content.Context
import com.mss.thebigcalendar.data.service.MoonPhasesJsonService
import com.mss.thebigcalendar.ui.components.MoonPhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.time.ZoneId

class MoonPhaseRepository(private val context: Context) {
    
    private val jsonService = MoonPhasesJsonService(context)
    
    /**
     * Obtém as fases da lua para um mês específico
     */
    suspend fun getMoonPhasesForMonth(
        yearMonth: YearMonth
    ): List<MoonPhase> = withContext(Dispatchers.IO) {
        
        // Usar fuso horário do Brasil (America/Sao_Paulo)
        val userTimezone = ZoneId.of("America/Sao_Paulo")
        
        // Obter fases da lua do arquivo JSON
        return@withContext jsonService.getMoonPhasesForMonthWithTimezone(yearMonth, userTimezone)
    }
}
