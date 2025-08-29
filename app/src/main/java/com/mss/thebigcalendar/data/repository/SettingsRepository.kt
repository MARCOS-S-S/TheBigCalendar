package com.mss.thebigcalendar.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mss.thebigcalendar.data.model.CalendarFilterOptions
import com.mss.thebigcalendar.data.model.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to create the DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val USERNAME = stringPreferencesKey("username")
        val SHOW_HOLIDAYS = booleanPreferencesKey("show_holidays")
        val SHOW_SAINT_DAYS = booleanPreferencesKey("show_saint_days")
        val SHOW_EVENTS = booleanPreferencesKey("show_events")
        val SHOW_TASKS = booleanPreferencesKey("show_tasks")
        val SHOW_BIRTHDAYS = booleanPreferencesKey("show_birthdays")
        val SHOW_NOTES = booleanPreferencesKey("show_notes")
    }

    val theme: Flow<Theme> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME] ?: Theme.SYSTEM.name
            Theme.valueOf(themeName)
        }

    val username: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USERNAME] ?: "Usuário"
        }

            val filterOptions: Flow<CalendarFilterOptions> = context.dataStore.data
        .map { preferences ->
            CalendarFilterOptions(
                showHolidays = preferences[PreferencesKeys.SHOW_HOLIDAYS] ?: true,
                showSaintDays = preferences[PreferencesKeys.SHOW_SAINT_DAYS] ?: true,
                showEvents = preferences[PreferencesKeys.SHOW_EVENTS] ?: true,
                showTasks = preferences[PreferencesKeys.SHOW_TASKS] ?: true,
                showBirthdays = preferences[PreferencesKeys.SHOW_BIRTHDAYS] ?: true,
                showNotes = preferences[PreferencesKeys.SHOW_NOTES] ?: true,
            )
        }

    suspend fun saveTheme(theme: Theme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun saveUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USERNAME] = username
        }
    }

    suspend fun saveFilterOptions(filterOptions: CalendarFilterOptions) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_HOLIDAYS] = filterOptions.showHolidays
            preferences[PreferencesKeys.SHOW_SAINT_DAYS] = filterOptions.showSaintDays
            preferences[PreferencesKeys.SHOW_EVENTS] = filterOptions.showEvents
            preferences[PreferencesKeys.SHOW_TASKS] = filterOptions.showTasks
            preferences[PreferencesKeys.SHOW_BIRTHDAYS] = filterOptions.showBirthdays
            preferences[PreferencesKeys.SHOW_NOTES] = filterOptions.showNotes
        }
    }
}
