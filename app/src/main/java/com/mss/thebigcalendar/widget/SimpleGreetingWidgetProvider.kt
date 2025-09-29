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

class SimpleGreetingWidgetProvider : AppWidgetProvider() {

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
        if (intent?.action == ACTION_REFRESH_SIMPLE_GREETING_WIDGET) {
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
        const val ACTION_REFRESH_SIMPLE_GREETING_WIDGET = "com.mss.thebigcalendar.ACTION_REFRESH_SIMPLE_GREETING_WIDGET"
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.simple_greeting_widget)

        // Atualiza a saudação baseada no horário
        val greeting = getGreetingBasedOnTime(context)
        views.setTextViewText(R.id.simple_widget_greeting, greeting)

        // Atualiza o dia da semana
        val dayOfWeek = getDayOfWeek(context)
        views.setTextViewText(R.id.simple_widget_day_of_week, dayOfWeek)

        // Atualiza o dia do mês
        val dayOfMonth = getDayOfMonth(context)
        views.setTextViewText(R.id.simple_widget_day_of_month, dayOfMonth)

        // Configurar clique para abrir o app
        val appIntent = Intent(context, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.simple_widget_greeting, appPendingIntent)

        // Configurar botão de refresh
        val refreshIntent = Intent(context, SimpleGreetingWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_SIMPLE_GREETING_WIDGET
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.simple_widget_refresh_button, refreshPendingIntent)

        // Notify the AppWidgetManager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
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

    /**
     * Obtém o dia da semana
     */
    private fun getDayOfWeek(context: Context): String {
        val locale = Locale("pt", "BR")
        val dayOfWeekFormat = SimpleDateFormat("EEEE", locale)
        val date = Date()
        return dayOfWeekFormat.format(date)
    }

    /**
     * Obtém o dia do mês
     */
    private fun getDayOfMonth(context: Context): String {
        val locale = Locale("pt", "BR")
        val dayFormat = SimpleDateFormat("dd", locale)
        val monthFormat = SimpleDateFormat("MMMM", locale)
        val date = Date()
        val day = dayFormat.format(date)
        val month = monthFormat.format(date)
        return "$day de $month"
    }
}
