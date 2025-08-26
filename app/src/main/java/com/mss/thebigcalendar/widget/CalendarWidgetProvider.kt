package com.mss.thebigcalendar.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mss.thebigcalendar.MainActivity
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.repository.ActivityRepository
import com.mss.thebigcalendar.data.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    @SuppressLint("RemoteViewLayout")
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.calendar_widget)
        
        // Atualizar data com cor branca e tamanho maior
        val currentDate = LocalDate.now()
        val dateFormat = DateTimeFormatter.ofPattern("EEE., dd 'de' MMM.", Locale("pt", "BR"))
        val whiteDate = "<font color='#FFFFFF'>${currentDate.format(dateFormat)}</font>"
        views.setTextViewText(R.id.widget_date, android.text.Html.fromHtml(whiteDate, android.text.Html.FROM_HTML_MODE_COMPACT))
        
        // Atualizar previsão do tempo
        updateWeatherInfo(context, views, appWidgetManager, appWidgetId)
        
        // Intent para abrir o app ao clicar
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_main_content, pendingIntent)

        // Intent para o botão de refresh
        val refreshIntent = Intent(context, CalendarWidgetProvider::class.java).apply {
            action = ACTION_APPWIDGET_REFRESH
            component = ComponentName(context, CalendarWidgetProvider::class.java)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)
        
        // Carregar tarefas do dia
        loadTodayTasks(context, views, appWidgetManager, appWidgetId)
    }
    
    private fun updateWeatherInfo(
        context: Context,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val weatherRepository = WeatherRepository()
                weatherRepository.getCurrentWeather().collect { weatherInfo ->
                    val weatherText = "${weatherInfo.emoji} ${weatherInfo.temperature}°C"
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        views.setTextViewText(R.id.widget_weather, weatherText)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } catch (e: Exception) {
                // Em caso de erro, mostrar informação padrão
                CoroutineScope(Dispatchers.Main).launch {
                    val defaultWeather = "🌤️ --°C"
                    views.setTextViewText(R.id.widget_weather, defaultWeather)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
    
    private fun loadTodayTasks(
        context: Context,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ActivityRepository(context)
                val today = LocalDate.now()
                
                repository.activities.collect { activities ->
                    val todayTasks = activities.filter { activity ->
                        LocalDate.parse(activity.date) == today
                    }.sortedWith(
                        compareByDescending<com.mss.thebigcalendar.data.model.Activity> { 
                            it.categoryColor?.toIntOrNull() ?: 0 
                        }.thenBy { 
                            it.startTime ?: LocalTime.MIN
                        }
                    )
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        val tasksText = if (todayTasks.isEmpty()) {
                            "Nenhuma tarefa para hoje"
                        } else {
                            todayTasks.take(7).joinToString("<br>") { task ->
                                if (task.startTime != null) {
                                    "${task.startTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} ${task.title}"
                                } else {
                                    task.title
                                }
                            } + if (todayTasks.size > 7) "<br>..." else ""
                        }

                        // Aplicar cor branca às tarefas
                        val whiteTasks = "<font color='#FFFFFF'>$tasksText</font>"
                        views.setTextViewText(R.id.widget_tasks, android.text.Html.fromHtml(whiteTasks, android.text.Html.FROM_HTML_MODE_COMPACT))
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    val errorText = "<font color='#FFFFFF'>Erro ao carregar tarefas</font>"
                    views.setTextViewText(R.id.widget_tasks, android.text.Html.fromHtml(errorText, android.text.Html.FROM_HTML_MODE_COMPACT))
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (ACTION_APPWIDGET_REFRESH == intent?.action) {
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
        private const val ACTION_APPWIDGET_REFRESH = "com.mss.thebigcalendar.ACTION_APPWIDGET_REFRESH"
    }
}