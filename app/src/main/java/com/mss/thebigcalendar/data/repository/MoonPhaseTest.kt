package com.mss.thebigcalendar.data.repository

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.*

/**
 * Classe de teste para verificar o cálculo das fases da lua
 */
class MoonPhaseTest {
    
    /**
     * Testa o cálculo das fases da lua para setembro de 2025
     */
    fun testSeptember2025() {
        val yearMonth = YearMonth.of(2025, 9)
        val year = yearMonth.year
        val month = yearMonth.monthValue
        
        println("=== Fases da Lua - Setembro 2025 ===")
        println("Algoritmo atual:")
        
        // Calcular as principais fases da lua para o mês
        val newMoon = calculateMoonPhase(year, month, 0.0)
        val firstQuarter = calculateMoonPhase(year, month, 0.25)
        val fullMoon = calculateMoonPhase(year, month, 0.5)
        val lastQuarter = calculateMoonPhase(year, month, 0.75)
        
        println("Lua Nova: ${newMoon}")
        println("Lua Crescente: ${firstQuarter}")
        println("Lua Cheia: ${fullMoon}")
        println("Lua Minguante: ${lastQuarter}")
        
        println("\n=== Datas Corretas (Brasil) ===")
        println("Lua Cheia: 7 de setembro, às 15h08")
        println("Lua Minguante: 14 de setembro, às 7h32")
        println("Lua Nova: 21 de setembro, às 16h54")
        println("Lua Crescente: 29 de setembro, às 20h53")
        
        println("\n=== Análise ===")
        val correctFullMoon = LocalDate.of(2025, 9, 7)
        val correctLastQuarter = LocalDate.of(2025, 9, 14)
        val correctNewMoon = LocalDate.of(2025, 9, 21)
        val correctFirstQuarter = LocalDate.of(2025, 9, 29)
        
        println("Lua Cheia - Calculado: $fullMoon, Correto: $correctFullMoon, Diferença: ${fullMoon.dayOfMonth - correctFullMoon.dayOfMonth} dias")
        println("Lua Minguante - Calculado: $lastQuarter, Correto: $correctLastQuarter, Diferença: ${lastQuarter.dayOfMonth - correctLastQuarter.dayOfMonth} dias")
        println("Lua Nova - Calculado: $newMoon, Correto: $correctNewMoon, Diferença: ${newMoon.dayOfMonth - correctNewMoon.dayOfMonth} dias")
        println("Lua Crescente - Calculado: $firstQuarter, Correto: $correctFirstQuarter, Diferença: ${firstQuarter.dayOfMonth - correctFirstQuarter.dayOfMonth} dias")
    }
    
    /**
     * Calcula uma fase específica da lua para um mês (algoritmo atual)
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
}
