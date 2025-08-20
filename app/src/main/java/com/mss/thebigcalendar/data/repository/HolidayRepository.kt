package com.mss.thebigcalendar.data.repository

import com.mss.thebigcalendar.data.model.HolidayType
import com.mss.thebigcalendar.data.model.Holiday

class HolidayRepository {

    fun getNationalHolidays(): List<Holiday> {
        return listOf(
            Holiday("Confraternização Universal", "2024-01-01", HolidayType.NATIONAL),
            Holiday("Carnaval (Ponto Facultativo Nacional)", "2024-02-13", HolidayType.NATIONAL),
            Holiday("Sexta-feira Santa", "2024-03-29", HolidayType.NATIONAL),
            Holiday("Tiradentes", "2024-04-21", HolidayType.NATIONAL),
            Holiday("Dia do Trabalho", "2024-05-01", HolidayType.NATIONAL),
            Holiday("Corpus Christi (Ponto Facultativo Nacional)", "2024-05-30", HolidayType.NATIONAL),
            Holiday("Independência do Brasil", "2024-09-07", HolidayType.NATIONAL),
            Holiday("Nossa Senhora Aparecida", "2024-10-12", HolidayType.NATIONAL),
            Holiday("Finados", "2024-11-02", HolidayType.NATIONAL),
            Holiday("Proclamação da República", "2024-11-15", HolidayType.NATIONAL),
            Holiday("Natal", "2024-12-25", HolidayType.NATIONAL),

            Holiday("Confraternização Universal", "2025-01-01", HolidayType.NATIONAL),
            Holiday("Carnaval (Ponto Facultativo Nacional)", "2025-03-04", HolidayType.NATIONAL),
            Holiday("Sexta-feira Santa", "2025-04-18", HolidayType.NATIONAL),
            Holiday("Tiradentes", "2025-04-21", HolidayType.NATIONAL),
            Holiday("Dia do Trabalho", "2025-05-01", HolidayType.NATIONAL),
            Holiday("Corpus Christi (Ponto Facultativo Nacional)", "2025-06-19", HolidayType.NATIONAL),
            Holiday("Independência do Brasil", "2025-09-07", HolidayType.NATIONAL),
            Holiday("Nossa Senhora Aparecida", "2025-10-12", HolidayType.NATIONAL),
            Holiday("Finados", "2025-11-02", HolidayType.NATIONAL),
            Holiday("Proclamação da República", "2025-11-15", HolidayType.NATIONAL),
            Holiday("Natal", "2025-12-25", HolidayType.NATIONAL)
        )
    }

    fun getSaintDays(): List<Holiday> {
        return listOf(
            Holiday("Santa Maria, Mãe de Deus", "01-01", HolidayType.SAINT),
            Holiday("Santo Antão, abade", "01-17", HolidayType.SAINT),
            Holiday("São Sebastião, mártir", "01-20", HolidayType.SAINT),
            Holiday("Santa Inês, virgem e mártir", "01-21", HolidayType.SAINT)
        )
    }

    fun getCommemorativeDates(): List<Holiday> {
        return listOf(
            Holiday("Dia Internacional da Mulher", "2024-03-08", HolidayType.COMMEMORATIVE),
            Holiday("Dia das Mães", "2024-05-12", HolidayType.COMMEMORATIVE),
            Holiday("Dia dos Namorados", "2024-06-12", HolidayType.COMMEMORATIVE),
            Holiday("Dia do Amigo", "2024-07-20", HolidayType.COMMEMORATIVE),
            Holiday("Dia dos Pais", "2024-08-11", HolidayType.COMMEMORATIVE),
            Holiday("Dia das Crianças", "2024-10-12", HolidayType.COMMEMORATIVE),

            Holiday("Dia Internacional da Mulher", "2025-03-08", HolidayType.COMMEMORATIVE),
            Holiday("Dia das Mães", "2025-05-11", HolidayType.COMMEMORATIVE)
        )
    }
}