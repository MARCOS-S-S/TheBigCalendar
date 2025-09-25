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
import com.mss.thebigcalendar.data.model.AnimationType
import com.mss.thebigcalendar.data.model.SidebarFilterVisibility

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to create the DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val WELCOME_NAME = stringPreferencesKey("welcome_name")
        val SHOW_HOLIDAYS = booleanPreferencesKey("show_holidays")
        val SHOW_SAINT_DAYS = booleanPreferencesKey("show_saint_days")
        val SHOW_EVENTS = booleanPreferencesKey("show_events")
        val SHOW_TASKS = booleanPreferencesKey("show_tasks")
        val SHOW_BIRTHDAYS = booleanPreferencesKey("show_birthdays")
        val SHOW_NOTES = booleanPreferencesKey("show_notes")
        val SHOW_MOON_PHASES = booleanPreferencesKey("show_moon_phases")
        val ANIMATION_TYPE = stringPreferencesKey("animation_type")
        
        // Sidebar filter visibility
        val SIDEBAR_SHOW_HOLIDAYS = booleanPreferencesKey("sidebar_show_holidays")
        val SIDEBAR_SHOW_SAINT_DAYS = booleanPreferencesKey("sidebar_show_saint_days")
        val SIDEBAR_SHOW_EVENTS = booleanPreferencesKey("sidebar_show_events")
        val SIDEBAR_SHOW_TASKS = booleanPreferencesKey("sidebar_show_tasks")
        val SIDEBAR_SHOW_BIRTHDAYS = booleanPreferencesKey("sidebar_show_birthdays")
        val SIDEBAR_SHOW_NOTES = booleanPreferencesKey("sidebar_show_notes")
        val SIDEBAR_SHOW_COMPLETED_TASKS = booleanPreferencesKey("sidebar_show_completed_tasks")
        val SIDEBAR_SHOW_MOON_PHASES = booleanPreferencesKey("sidebar_show_moon_phases")

    }

    val theme: Flow<Theme> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME] ?: Theme.SYSTEM.name
            Theme.valueOf(themeName)
        }

    val welcomeName: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WELCOME_NAME] ?: "Usuário"
        }

    val showMoonPhases: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_MOON_PHASES] ?: false
        }

    val animationType: Flow<AnimationType> = context.dataStore.data
        .map { preferences ->
            val animationName = preferences[PreferencesKeys.ANIMATION_TYPE] ?: AnimationType.NONE.name
            AnimationType.valueOf(animationName)
        }

    val sidebarFilterVisibility: Flow<SidebarFilterVisibility> = context.dataStore.data
        .map { preferences ->
            SidebarFilterVisibility(
                showHolidays = preferences[PreferencesKeys.SIDEBAR_SHOW_HOLIDAYS] ?: true,
                showSaintDays = preferences[PreferencesKeys.SIDEBAR_SHOW_SAINT_DAYS] ?: false, // Desativado por padrão
                showEvents = preferences[PreferencesKeys.SIDEBAR_SHOW_EVENTS] ?: true,
                showTasks = preferences[PreferencesKeys.SIDEBAR_SHOW_TASKS] ?: true,
                showBirthdays = preferences[PreferencesKeys.SIDEBAR_SHOW_BIRTHDAYS] ?: true,
                showNotes = preferences[PreferencesKeys.SIDEBAR_SHOW_NOTES] ?: true,
                showCompletedTasks = preferences[PreferencesKeys.SIDEBAR_SHOW_COMPLETED_TASKS] ?: true,
                showMoonPhases = preferences[PreferencesKeys.SIDEBAR_SHOW_MOON_PHASES] ?: true
            )
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

    suspend fun saveWelcomeName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WELCOME_NAME] = name
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

    suspend fun saveShowMoonPhases(showMoonPhases: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_MOON_PHASES] = showMoonPhases
        }
    }

    suspend fun saveAnimationType(animationType: AnimationType) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ANIMATION_TYPE] = animationType.name
        }
    }

    suspend fun saveSidebarFilterVisibility(visibility: SidebarFilterVisibility) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SIDEBAR_SHOW_HOLIDAYS] = visibility.showHolidays
            preferences[PreferencesKeys.SIDEBAR_SHOW_SAINT_DAYS] = visibility.showSaintDays
            preferences[PreferencesKeys.SIDEBAR_SHOW_EVENTS] = visibility.showEvents
            preferences[PreferencesKeys.SIDEBAR_SHOW_TASKS] = visibility.showTasks
            preferences[PreferencesKeys.SIDEBAR_SHOW_BIRTHDAYS] = visibility.showBirthdays
            preferences[PreferencesKeys.SIDEBAR_SHOW_NOTES] = visibility.showNotes
            preferences[PreferencesKeys.SIDEBAR_SHOW_COMPLETED_TASKS] = visibility.showCompletedTasks
            preferences[PreferencesKeys.SIDEBAR_SHOW_MOON_PHASES] = visibility.showMoonPhases
        }
    }


}
