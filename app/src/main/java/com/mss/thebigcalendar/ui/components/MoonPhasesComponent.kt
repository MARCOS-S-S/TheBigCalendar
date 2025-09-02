package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.*

data class MoonPhase(
    val date: LocalDate,
    val phase: MoonPhaseType,
    val illumination: Double // 0.0 a 1.0
)

enum class MoonPhaseType(val emoji: String, val displayName: String) {
    NEW_MOON("🌑", "Nova"),
    WAXING_CRESCENT("🌒", "Crescente"),
    FIRST_QUARTER("🌓", "Crescente"),
    WAXING_GIBBOUS("🌔", "Crescente Gibosa"),
    FULL_MOON("🌕", "Cheia"),
    WANING_GIBBOUS("🌖", "Minguante Gibosa"),
    LAST_QUARTER("🌗", "Minguante"),
    WANING_CRESCENT("🌘", "Minguante")
}

@Composable
fun MoonPhasesComponent(
    yearMonth: YearMonth,
    modifier: Modifier = Modifier
) {
    val moonPhases = calculateMoonPhasesForMonth(yearMonth)
    
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.6f) // Ocupa 60% da largura
        ) {
            // Mostrar as principais fases da lua
            val mainPhases = moonPhases.filter { phase ->
                phase.phase == MoonPhaseType.NEW_MOON ||
                phase.phase == MoonPhaseType.FIRST_QUARTER ||
                phase.phase == MoonPhaseType.FULL_MOON ||
                phase.phase == MoonPhaseType.LAST_QUARTER
            }
            
            if (mainPhases.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    mainPhases.take(4).forEach { moonPhase ->
                        MoonPhaseItem(
                            moonPhase = moonPhase,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Text(
                    text = "Calculando...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MoonPhaseItem(
    moonPhase: MoonPhase,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = moonPhase.phase.emoji,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        
        Text(
            text = moonPhase.phase.displayName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontSize = 10.sp
        )
        
        Text(
            text = "${moonPhase.date.dayOfMonth}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 10.sp
        )
    }
}

/**
 * Calcula as fases da lua para um mês específico
 * Baseado no algoritmo de Jean Meeus
 */
private fun calculateMoonPhasesForMonth(yearMonth: YearMonth): List<MoonPhase> {
    val phases = mutableListOf<MoonPhase>()
    val year = yearMonth.year
    val month = yearMonth.monthValue
    
    // Calcular as principais fases da lua para o mês
    val newMoon = calculateMoonPhase(year, month, 0.0)
    val firstQuarter = calculateMoonPhase(year, month, 0.25)
    val fullMoon = calculateMoonPhase(year, month, 0.5)
    val lastQuarter = calculateMoonPhase(year, month, 0.75)
    
    // Adicionar as fases principais
    phases.add(MoonPhase(newMoon, MoonPhaseType.NEW_MOON, 0.0))
    phases.add(MoonPhase(firstQuarter, MoonPhaseType.FIRST_QUARTER, 0.5))
    phases.add(MoonPhase(fullMoon, MoonPhaseType.FULL_MOON, 1.0))
    phases.add(MoonPhase(lastQuarter, MoonPhaseType.LAST_QUARTER, 0.5))
    
    return phases.sortedBy { it.date }
}

/**
 * Calcula uma fase específica da lua para um mês
 */
private fun calculateMoonPhase(year: Int, month: Int, phase: Double): LocalDate {
    // Algoritmo simplificado baseado em Jean Meeus
    val k = (year - 2000) * 12.3685 + month - 1 + phase
    val t = k / 1236.85
    val e = 1 - 0.002516 * t - 0.0000074 * t * t
    
    // Correção para a fase
    val correction = when {
        phase < 0.25 -> 0.0
        phase < 0.5 -> 0.25
        phase < 0.75 -> 0.5
        else -> 0.75
    }
    
    val jd = 2451550.09766 + 29.530588861 * k + correction
    
    // Converter para data
    val daysSinceEpoch = jd - 2440588.0
    val date = LocalDate.ofEpochDay(daysSinceEpoch.toLong())
    
    return date
}

private fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "Janeiro"
        2 -> "Fevereiro"
        3 -> "Março"
        4 -> "Abril"
        5 -> "Maio"
        6 -> "Junho"
        7 -> "Julho"
        8 -> "Agosto"
        9 -> "Setembro"
        10 -> "Outubro"
        11 -> "Novembro"
        12 -> "Dezembro"
        else -> "Mês"
    }
}
