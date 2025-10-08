package com.mss.thebigcalendar.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mss.thebigcalendar.data.model.JsonCalendar
import com.mss.thebigcalendar.data.model.colorToString
import com.mss.thebigcalendar.data.model.toColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "json_calendars")

class JsonCalendarRepository(private val context: Context) {
    
    companion object {
        private const val JSON_CALENDARS_KEY = "json_calendars"
    }
    
    /**
     * Obtém todos os calendários JSON importados
     */
    fun getAllJsonCalendars(): Flow<List<JsonCalendar>> {
        return context.dataStore.data.map { preferences ->
            val jsonString = preferences[stringPreferencesKey(JSON_CALENDARS_KEY)] ?: "[]"
            parseJsonCalendars(jsonString)
        }
    }
    
    /**
     * Salva um novo calendário JSON
     */
    suspend fun saveJsonCalendar(jsonCalendar: JsonCalendar) {
        context.dataStore.edit { preferences ->
            val currentCalendars = preferences[stringPreferencesKey(JSON_CALENDARS_KEY)] ?: "[]"
            val calendars = parseJsonCalendars(currentCalendars).toMutableList()
            
            // Verificar se já existe um calendário com o mesmo ID
            val existingIndex = calendars.indexOfFirst { it.id == jsonCalendar.id }
            if (existingIndex >= 0) {
                calendars[existingIndex] = jsonCalendar
            } else {
                calendars.add(jsonCalendar)
            }
            
            preferences[stringPreferencesKey(JSON_CALENDARS_KEY)] = calendarsToJson(calendars)
        }
    }
    
    /**
     * Remove um calendário JSON
     */
    suspend fun removeJsonCalendar(calendarId: String) {
        context.dataStore.edit { preferences ->
            val currentCalendars = preferences[stringPreferencesKey(JSON_CALENDARS_KEY)] ?: "[]"
            val calendars = parseJsonCalendars(currentCalendars).toMutableList()
            
            calendars.removeAll { it.id == calendarId }
            preferences[stringPreferencesKey(JSON_CALENDARS_KEY)] = calendarsToJson(calendars)
        }
    }
    
    /**
     * Atualiza a visibilidade de um calendário JSON
     */
    suspend fun updateJsonCalendarVisibility(calendarId: String, isVisible: Boolean) {
        context.dataStore.edit { preferences ->
            val currentCalendars = preferences[stringPreferencesKey(JSON_CALENDARS_KEY)] ?: "[]"
            val calendars = parseJsonCalendars(currentCalendars).toMutableList()
            
            val index = calendars.indexOfFirst { it.id == calendarId }
            if (index >= 0) {
                calendars[index] = calendars[index].copy(isVisible = isVisible)
                preferences[stringPreferencesKey(JSON_CALENDARS_KEY)] = calendarsToJson(calendars)
            }
        }
    }
    
    /**
     * Converte lista de calendários para JSON
     */
    private fun calendarsToJson(calendars: List<JsonCalendar>): String {
        val jsonArray = JSONArray()
        calendars.forEach { calendar ->
            val jsonObject = JSONObject().apply {
                put("id", calendar.id)
                put("title", calendar.title)
                put("color", calendar.colorToString())
                put("fileName", calendar.fileName)
                put("importDate", calendar.importDate)
                put("isVisible", calendar.isVisible)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }
    
    /**
     * Converte JSON para lista de calendários
     */
    private fun parseJsonCalendars(jsonString: String): List<JsonCalendar> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val calendars = mutableListOf<JsonCalendar>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val calendar = JsonCalendar(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getString("title"),
                    color = jsonObject.getString("color").toColor(),
                    fileName = jsonObject.getString("fileName"),
                    importDate = jsonObject.getLong("importDate"),
                    isVisible = jsonObject.optBoolean("isVisible", true)
                )
                calendars.add(calendar)
            }
            
            calendars
        } catch (e: Exception) {
            emptyList()
        }
    }
}
