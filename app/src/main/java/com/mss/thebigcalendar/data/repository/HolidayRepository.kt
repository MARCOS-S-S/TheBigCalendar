package com.mss.thebigcalendar.data.repository

import android.content.Context
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Holiday
import com.mss.thebigcalendar.data.model.HolidayType
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

class HolidayRepository(private val context: Context) {

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
        val inputStream = context.resources.openRawResource(R.raw.saints_data)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = reader.readText()
        val jsonArray = JSONArray(jsonString)
        val saints = mutableListOf<Holiday>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            saints.add(
                Holiday(
                    name = jsonObject.getString("name"),
                    date = jsonObject.getString("date"),
                    type = HolidayType.SAINT,
                    summary = jsonObject.optString("summary"),
                    wikipediaLink = jsonObject.optString("wikipediaLink")
                )
            )
        }
        return saints
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