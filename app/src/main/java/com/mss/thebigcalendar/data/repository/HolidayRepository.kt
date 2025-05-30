package com.mss.thebigcalendar.data.repository

import com.mss.thebigcalendar.data.model.Holiday
import com.mss.thebigcalendar.data.model.HolidayType

/**
 * Uma classe responsável por fornecer os dados de feriados.
 * Atualmente usa dados mocados, mas no futuro pode buscar de uma API ou banco de dados.
 */
class HolidayRepository {

    fun getNationalHolidays(): List<Holiday> {
        return listOf(
            Holiday("2024-01-01", "Confraternização Universal", HolidayType.NATIONAL),
            Holiday("2024-02-13", "Carnaval (Ponto Facultativo Nacional)", HolidayType.NATIONAL),
            Holiday("2024-03-29", "Sexta-feira Santa", HolidayType.NATIONAL),
            Holiday("2024-04-21", "Tiradentes", HolidayType.NATIONAL),
            Holiday("2024-05-01", "Dia do Trabalho", HolidayType.NATIONAL),
            Holiday("2024-05-30", "Corpus Christi (Ponto Facultativo Nacional)", HolidayType.NATIONAL),
            Holiday("2024-09-07", "Independência do Brasil", HolidayType.NATIONAL),
            Holiday("2024-10-12", "Nossa Senhora Aparecida", HolidayType.NATIONAL),
            Holiday("2024-11-02", "Finados", HolidayType.NATIONAL),
            Holiday("2024-11-15", "Proclamação da República", HolidayType.NATIONAL),
            Holiday("2024-12-25", "Natal", HolidayType.NATIONAL),

            Holiday("2025-01-01", "Confraternização Universal", HolidayType.NATIONAL),
            Holiday("2025-03-04", "Carnaval (Ponto Facultativo Nacional)", HolidayType.NATIONAL),
            Holiday("2025-04-18", "Sexta-feira Santa", HolidayType.NATIONAL),
            Holiday("2025-04-21", "Tiradentes", HolidayType.NATIONAL),
            Holiday("2025-05-01", "Dia do Trabalho", HolidayType.NATIONAL),
            Holiday("2025-06-19", "Corpus Christi (Ponto Facultativo Nacional)", HolidayType.NATIONAL),
            Holiday("2025-09-07", "Independência do Brasil", HolidayType.NATIONAL),
            Holiday("2025-10-12", "Nossa Senhora Aparecida", HolidayType.NATIONAL),
            Holiday("2025-11-02", "Finados", HolidayType.NATIONAL),
            Holiday("2025-11-15", "Proclamação da República", HolidayType.NATIONAL),
            Holiday("2025-12-25", "Natal", HolidayType.NATIONAL),
            // ...cole o resto dos feriados nacionais aqui
        )
    }

    fun getSaintDays(): List<Holiday> {
        return listOf(
            Holiday("01-01", "Santa Maria, Mãe de Deus", HolidayType.SAINT),
            Holiday("01-17", "Santo Antão, abade", HolidayType.SAINT),
            Holiday("01-20", "São Sebastião, mártir", HolidayType.SAINT),
            Holiday("01-21", "Santa Inês, virgem e mártir", HolidayType.SAINT),
            // ...cole o resto dos dias de santos aqui
        )
    }

    fun getCommemorativeDates(): List<Holiday> {
        return listOf(
            Holiday("2024-03-08", "Dia Internacional da Mulher", HolidayType.COMMEMORATIVE),
            Holiday("2024-05-12", "Dia das Mães", HolidayType.COMMEMORATIVE),
            Holiday("2024-06-12", "Dia dos Namorados", HolidayType.COMMEMORATIVE),
            Holiday("2024-07-20", "Dia do Amigo", HolidayType.COMMEMORATIVE),
            Holiday("2024-08-11", "Dia dos Pais", HolidayType.COMMEMORATIVE),
            Holiday("2024-10-12", "Dia das Crianças", HolidayType.COMMEMORATIVE),

            Holiday("2025-03-08", "Dia Internacional da Mulher", HolidayType.COMMEMORATIVE),
            Holiday("2025-05-11", "Dia das Mães", HolidayType.COMMEMORATIVE),
            // ...cole o resto das datas comemorativas aqui
        )
    }
}