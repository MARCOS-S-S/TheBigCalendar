package com.mss.thebigcalendar.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_preferences")

class SyncRepository(private val context: Context) {
    
    private object PreferencesKeys {
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    }
    
    val lastSyncTime: Flow<Long> = context.syncDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIME] ?: 0L
        }
    
    suspend fun updateLastSyncTime(timestamp: Long) {
        context.syncDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIME] = timestamp
        }
    }
    
    suspend fun getLastSyncTime(): Long {
        return context.syncDataStore.data.map { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIME] ?: 0L
        }.first()
    }
}
