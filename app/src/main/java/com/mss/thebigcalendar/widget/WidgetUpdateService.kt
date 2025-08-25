package com.mss.thebigcalendar.widget

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*
import java.util.*

class WidgetUpdateService : Service() {
    
    private var updateJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        startPeriodicUpdates()
    }
    
    private fun startPeriodicUpdates() {
        updateJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    updateAllWidgets()
                    delay(60000) // Atualizar a cada minuto
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(60000)
                }
            }
        }
    }
    
    private fun updateAllWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, CalendarWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        
        if (widgetIds.isNotEmpty()) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            intent.component = componentName
            sendBroadcast(intent)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, WidgetUpdateService::class.java)
            context.startForegroundService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, WidgetUpdateService::class.java)
            context.stopService(intent)
        }
    }
}