package com.mss.thebigcalendar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mss.thebigcalendar.MainActivity
import com.mss.thebigcalendar.R
import java.text.SimpleDateFormat
import java.util.*

class CompactGreetingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent?.action == ACTION_REFRESH_COMPACT_GREETING_WIDGET) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                context?.let {
                    val appWidgetManager = AppWidgetManager.getInstance(it)
                    updateAppWidget(it, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_COMPACT_GREETING_WIDGET = "com.mss.thebigcalendar.ACTION_REFRESH_COMPACT_GREETING_WIDGET"
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.compact_greeting_widget)

        // Atualiza a saudação baseada no horário
        val greeting = getGreetingBasedOnTime(context)
        views.setTextViewText(R.id.compact_widget_greeting, greeting)

        // Atualiza a data
        val locale = Locale.getDefault()
        val dayOfWeekFormat = SimpleDateFormat("EEE", locale)
        val dayMonthFormat = SimpleDateFormat("dd/MM", locale)
        val date = Date()

        val dayOfWeekShort = dayOfWeekFormat.format(date).let { it.first().uppercase() + it.substring(1) }
        val dayMonth = dayMonthFormat.format(date)

        // Colocar o dia da semana primeiro e depois a data
        val dayWithDate = "$dayOfWeekShort - $dayMonth"
        views.setTextViewText(R.id.compact_widget_day_of_week, dayWithDate)

        // Configurar clique para abrir o app
        val appIntent = Intent(context, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.compact_widget_greeting, appPendingIntent)

        // Configurar botão de refresh
        val refreshIntent = Intent(context, CompactGreetingWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_COMPACT_GREETING_WIDGET
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.compact_widget_refresh_button, refreshPendingIntent)

        // Carregar tarefas do dia
        loadTodayTasks(context, views, appWidgetManager, appWidgetId)

        // Notify the AppWidgetManager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun loadTodayTasks(
        context: Context,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Carregar tarefas de forma simples e síncrona
        try {
            val tasksText = loadTasksSync(context)
            views.setTextViewText(R.id.compact_widget_tasks, tasksText)
        } catch (e: Exception) {
            views.setTextViewText(R.id.compact_widget_tasks, context.getString(R.string.widget_no_tasks_today))
        }
    }

    /**
     * Carregamento síncrono de tarefas (simples e confiável)
     */
    private fun loadTasksSync(context: Context): String {
        return try {
            // Por enquanto, retornar texto simples para testar
            "09:00 Reunião\n10:30 Café\n12:00 Almoço"
        } catch (e: Exception) {
            context.getString(R.string.widget_no_tasks_today)
        }
    }

    /**
     * Obtém a saudação baseada no horário atual
     */
    private fun getGreetingBasedOnTime(context: Context): String {
        val currentTime = java.time.LocalTime.now()
        val hour = currentTime.hour
        
        return when (hour) {
            in 5..11 -> context.getString(R.string.widget_greeting_morning)
            in 12..17 -> context.getString(R.string.widget_greeting_afternoon)
            in 18..23 -> context.getString(R.string.widget_greeting_evening)
            else -> context.getString(R.string.widget_greeting_dawn) // 0-4
        }
    }

}
