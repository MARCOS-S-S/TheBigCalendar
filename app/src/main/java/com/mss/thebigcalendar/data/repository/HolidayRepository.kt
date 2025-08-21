package com.mss.thebigcalendar.data.repository

import com.mss.thebigcalendar.data.model.HolidayType
import com.mss.thebigcalendar.data.model.Holiday

class HolidayRepository {

    fun getNationalHolidays(): List<Holiday> {
        return listOf(
            Holiday("Confraternização Universal", "2025-01-01", HolidayType.NATIONAL),
            Holiday("Carnaval", "2025-03-03", HolidayType.NATIONAL),
            Holiday("Carnaval", "2025-03-04", HolidayType.NATIONAL),
            Holiday("Paixão de Cristo", "2025-04-18", HolidayType.NATIONAL),
            Holiday("Tiradentes", "2025-04-21", HolidayType.NATIONAL),
            Holiday("Dia do Trabalho", "2025-05-01", HolidayType.NATIONAL),
            Holiday("Corpus Christi", "2025-06-19", HolidayType.NATIONAL),
            Holiday("Independência do Brasil", "2025-09-07", HolidayType.NATIONAL),
            Holiday("Nossa Sr.a Aparecida - Padroeira do Brasil", "2025-10-12", HolidayType.NATIONAL),
            Holiday("Finados", "2025-11-02", HolidayType.NATIONAL),
            Holiday("Proclamação da República", "2025-11-15", HolidayType.NATIONAL),
            Holiday("Dia Nacional de Zumbi e da Consciência Negra", "2025-11-20", HolidayType.NATIONAL),
            Holiday("Natal", "2025-12-25", HolidayType.NATIONAL)
        )
    }

    fun getSaintDays(): List<Holiday> {
        return listOf(
            Holiday("Nossa Senhora da Defesa", "01-18", HolidayType.SAINT),
            Holiday("São Sebastião", "01-20", HolidayType.SAINT),
            Holiday("Santa Inês", "01-21", HolidayType.SAINT),
            Holiday("Dom Bosco", "01-31", HolidayType.SAINT),

            Holiday("São Braz", "02-03", HolidayType.SAINT),
            Holiday("Santa Josefina Bakhita", "02-08", HolidayType.SAINT),
            Holiday("Santa Apolônia", "02-09", HolidayType.SAINT),
            Holiday("Nossa Senhora de Lourdes", "02-11", HolidayType.SAINT),

            Holiday("São Longuinho", "03-15", HolidayType.SAINT),
            Holiday("São José", "03-19", HolidayType.SAINT),

            Holiday("Santo Expedito", "04-19", HolidayType.SAINT),
            Holiday("São Jorge", "04-23", HolidayType.SAINT),
            Holiday("São Marcos", "04-25", HolidayType.SAINT),

            Holiday("São José Carpinteiro", "05-01", HolidayType.SAINT),
            Holiday("São Peregrino", "05-04", HolidayType.SAINT),
            Holiday("Nossa Senhora de Fátima", "05-13", HolidayType.SAINT),
            Holiday("Santo Ivo", "05-19", HolidayType.SAINT),
            Holiday("Santa Rita", "05-22", HolidayType.SAINT),

            Holiday("Santo Antônio", "06-13", HolidayType.SAINT),
            Holiday("São João Batista", "06-24", HolidayType.SAINT),
            Holiday("São Paulo", "06-29", HolidayType.SAINT),
            Holiday("São Pedro", "06-29", HolidayType.SAINT),

            Holiday("Santa Isabel", "07-04", HolidayType.SAINT),
            Holiday("Santa Paulina", "07-09", HolidayType.SAINT),
            Holiday("São Bento", "07-11", HolidayType.SAINT),
            Holiday("Santa Verônica", "07-12", HolidayType.SAINT),
            Holiday("Nossa Senhora da Rosa Mística", "07-13", HolidayType.SAINT),
            Holiday("São Camilo de Lellis", "07-14", HolidayType.SAINT),
            Holiday("Nossa Senhora do Carmo", "07-16", HolidayType.SAINT),
            Holiday("São Cristovão", "07-25", HolidayType.SAINT),
            Holiday("Santa Ana", "07-26", HolidayType.SAINT),
            Holiday("Santa Marta", "07-29", HolidayType.SAINT),
            Holiday("São Lazaro", "07-29", HolidayType.SAINT),

            Holiday("Santa Clara", "08-11", HolidayType.SAINT),
            Holiday("Santa Filomena", "08-11", HolidayType.SAINT),
            Holiday("Nossa Senhora da Assunção", "08-15", HolidayType.SAINT),
            Holiday("Nossa Senhora do Sorriso", "08-15", HolidayType.SAINT),
            Holiday("São Roque", "08-16", HolidayType.SAINT),
            Holiday("Santa Helena", "08-18", HolidayType.SAINT),
            Holiday("Santa Mônica", "08-27", HolidayType.SAINT),
            Holiday("Santo Agostinho", "08-28", HolidayType.SAINT),
            Holiday("Santa Rosa de Lima", "08-30", HolidayType.SAINT),

            Holiday("Santa Rosália", "09-04", HolidayType.SAINT),
            Holiday("Nossa Senhora da Piedade", "09-15", HolidayType.SAINT),
            Holiday("Nossa Senhora das Angústias", "09-15", HolidayType.SAINT),
            Holiday("Nossa Senhora das Dores", "09-15", HolidayType.SAINT),
            Holiday("Nossa Senhora do Calvário", "09-15", HolidayType.SAINT),
            Holiday("Nossa Senhora do Pranto", "09-15", HolidayType.SAINT),
            Holiday("Nossa Senhora da Salete", "09-19", HolidayType.SAINT),
            Holiday("São Mateus", "09-21", HolidayType.SAINT),
            Holiday("Padre Pio", "09-23", HolidayType.SAINT),
            Holiday("Nossa Senhora das Mercês", "09-24", HolidayType.SAINT),
            Holiday("São Cosme e São Damião", "09-26", HolidayType.SAINT),
            Holiday("São Cosme e Damião", "09-26", HolidayType.SAINT),
            Holiday("São Miguel", "09-29", HolidayType.SAINT),
            Holiday("São Rafael", "09-29", HolidayType.SAINT),
            Holiday("São Gabriel", "09-29", HolidayType.SAINT),

            Holiday("Santa Teresa", "10-01", HolidayType.SAINT),
            Holiday("Anjo da Guarda", "10-02", HolidayType.SAINT),
            Holiday("São Francisco de Assis", "10-04", HolidayType.SAINT),
            Holiday("São Benedito", "10-05", HolidayType.SAINT),
            Holiday("Nossa Senhora do Rosário", "10-07", HolidayType.SAINT),
            Holiday("Nossa Senhora Aparecida", "10-12", HolidayType.SAINT),
            Holiday("Nossa Senhora de Nazaré", "10-12", HolidayType.SAINT),
            Holiday("Santa Edwiges", "10-16", HolidayType.SAINT),
            Holiday("São Geraldo", "10-16", HolidayType.SAINT),
            Holiday("São Lucas", "10-18", HolidayType.SAINT),
            Holiday("Frei Galvão", "10-25", HolidayType.SAINT),
            Holiday("São Judas Tadeu", "10-28", HolidayType.SAINT),

            Holiday("Santa Cecília", "11-22", HolidayType.SAINT),
            Holiday("Santa Catarina", "11-25", HolidayType.SAINT),
            Holiday("Nossa Senhora das Graças", "11-27", HolidayType.SAINT),
            Holiday("Santo André", "11-30", HolidayType.SAINT),

            Holiday("Santa Barbara", "12-04", HolidayType.SAINT),
            Holiday("Nossa Senhora da Conceição", "12-08", HolidayType.SAINT),
            Holiday("Nossa Senhora Imaculada Conceição", "12-08", HolidayType.SAINT),
            Holiday("Nossa Senhora de Guadalupe", "12-12", HolidayType.SAINT),
            Holiday("Santa Luzia", "12-13", HolidayType.SAINT),
            Holiday("São João", "12-27", HolidayType.SAINT)
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