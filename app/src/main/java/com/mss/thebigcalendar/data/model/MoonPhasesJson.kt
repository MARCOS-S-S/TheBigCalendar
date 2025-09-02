package com.mss.thebigcalendar.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modelos de dados para o arquivo JSON de fases da lua
 */

data class MoonPhasesJson(
    @SerializedName("ano") val year: Int,
    @SerializedName("meses") val months: Map<String, List<MoonPhaseJson>>
)

data class MoonPhaseJson(
    @SerializedName("fase") val phase: String,
    @SerializedName("dia") val day: Int? = null, // Campo antigo (opcional)
    @SerializedName("data_utc") val utcDate: String? = null, // Campo UTC (opcional)
    @SerializedName("data_hora") val localDateTime: String? = null // Campo para horário local do Brasil
)

/**
 * Enum para mapear os nomes dos meses em português
 */
enum class MonthName(val portuguese: String, val number: Int) {
    JANUARY("janeiro", 1),
    FEBRUARY("fevereiro", 2),
    MARCH("marco", 3),
    APRIL("abril", 4),
    MAY("maio", 5),
    JUNE("junho", 6),
    JULY("julho", 7),
    AUGUST("agosto", 8),
    SEPTEMBER("setembro", 9),
    OCTOBER("outubro", 10),
    NOVEMBER("novembro", 11),
    DECEMBER("dezembro", 12);

    companion object {
        fun fromNumber(monthNumber: Int): MonthName? {
            return values().find { it.number == monthNumber }
        }
    }
}

/**
 * Enum para mapear os nomes das fases em português
 */
enum class PhaseName(val portuguese: String, val moonPhaseType: com.mss.thebigcalendar.ui.components.MoonPhaseType) {
    NEW_MOON("Lua Nova", com.mss.thebigcalendar.ui.components.MoonPhaseType.NEW_MOON),
    FIRST_QUARTER("Lua Crescente", com.mss.thebigcalendar.ui.components.MoonPhaseType.FIRST_QUARTER),
    FULL_MOON("Lua Cheia", com.mss.thebigcalendar.ui.components.MoonPhaseType.FULL_MOON),
    LAST_QUARTER("Lua Minguante", com.mss.thebigcalendar.ui.components.MoonPhaseType.LAST_QUARTER);

    companion object {
        fun fromPortuguese(portuguese: String): PhaseName? {
            return values().find { it.portuguese == portuguese }
        }
    }
}
