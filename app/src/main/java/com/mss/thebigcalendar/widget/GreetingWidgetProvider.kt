package com.mss.thebigcalendar.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.mss.thebigcalendar.MainActivity
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.repository.ActivityRepository
import com.mss.thebigcalendar.service.RecurrenceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class GreetingWidgetProvider : AppWidgetProvider() {

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
        val views = RemoteViews(context.packageName, R.layout.greeting_widget)

        // Atualiza a saudação baseada no horário
        val greeting = getGreetingBasedOnTime(context)
        views.setTextViewText(R.id.widget_greeting, greeting)

        // Atualiza a data
        val locale = Locale("pt", "BR")
        val dayOfWeekFormat = java.text.SimpleDateFormat("EEE", locale)
        val dayMonthFormat = java.text.SimpleDateFormat("dd/MM", locale)
        val date = java.util.Date()

        val dayOfWeekShort = dayOfWeekFormat.format(date).let { it.first().uppercase() + it.substring(1) }
        val dayMonth = dayMonthFormat.format(date)

        // Colocar o dia da semana primeiro e depois a data
        val dayWithDate = "$dayOfWeekShort - $dayMonth"
        views.setTextViewText(R.id.widget_day_of_week, dayWithDate)

        // Configurar clique para abrir o app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_main_content, pendingIntent)

        // Configurar botão de refresh
        val refreshIntent = Intent(context, GreetingWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

        // Carregar tarefas do dia
        loadTodayTasks(context, views, appWidgetManager, appWidgetId)

        appWidgetManager.updateAppWidget(appWidgetId, views)
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
                val tomorrow = today.plusDays(1)
                val currentTime = LocalTime.now()
                
                // Verificar se está no período noturno (pôr do sol até meia-noite)
                val isNightTime = isNightTime(currentTime)
                
                repository.activities.collect { activities ->
                    val recurrenceService = RecurrenceService()
                    val todayTasks = mutableListOf<com.mss.thebigcalendar.data.model.Activity>()
                    
                    activities.forEach { activity ->
                        try {
                            val activityDate = LocalDate.parse(activity.date)
                            
                            // Verificar se esta data específica foi excluída para atividades recorrentes
                            val isExcluded = if (activity.recurrenceRule?.isNotEmpty() == true && activity.recurrenceRule != "CUSTOM") {
                                activity.excludedDates.contains(today.toString())
                            } else {
                                false
                            }
                            
                            if (!isExcluded) {
                                // Para aniversários, verificar se é o mesmo dia e mês (ignorando o ano)
                                if (activity.activityType == com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY) {
                                    if (activityDate.month == today.month && activityDate.dayOfMonth == today.dayOfMonth) {
                                        todayTasks.add(activity)
                                    }
                                } else {
                                    // Para atividades normais e recorrentes
                                    if (activity.recurrenceRule?.isNotEmpty() == true && activity.recurrenceRule != "CUSTOM") {
                                        // Verificar se a atividade base é para hoje
                                        if (activityDate == today) {
                                            todayTasks.add(activity)
                                        }
                                        
                                        // Gerar instâncias recorrentes para o dia atual
                                        val recurringInstances = recurrenceService.generateRecurringInstances(activity, today, today)
                                        val instancesForToday = recurringInstances.filter { instance ->
                                            val instanceDate = LocalDate.parse(instance.date)
                                            instanceDate == today
                                        }
                                        todayTasks.addAll(instancesForToday)
                                    } else {
                                        // Atividade única - verificar se é para hoje
                                        if (activityDate == today) {
                                            todayTasks.add(activity)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("GreetingWidget", "Erro ao processar atividade: ${activity.title}", e)
                        }
                    }
                    
                    // Ordenar as tarefas de hoje por horário crescente
                    todayTasks.sortWith(
                        compareBy<com.mss.thebigcalendar.data.model.Activity> { 
                            it.startTime ?: LocalTime.MAX 
                        }.thenByDescending { 
                            it.categoryColor?.toIntOrNull() ?: 0 
                        }
                    )
                    
                    val tomorrowTasks = if (isNightTime) {
                        val tomorrowTasksList = mutableListOf<com.mss.thebigcalendar.data.model.Activity>()
                        
                        activities.forEach { activity ->
                            try {
                                val activityDate = LocalDate.parse(activity.date)
                                
                                // Verificar se esta data específica foi excluída para atividades recorrentes
                                val isExcluded = if (activity.recurrenceRule?.isNotEmpty() == true && activity.recurrenceRule != "CUSTOM") {
                                    activity.excludedDates.contains(tomorrow.toString())
                                } else {
                                    false
                                }
                                
                                if (!isExcluded) {
                                    // Para aniversários, verificar se é o mesmo dia e mês (ignorando o ano)
                                    if (activity.activityType == com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY) {
                                        if (activityDate.month == tomorrow.month && activityDate.dayOfMonth == tomorrow.dayOfMonth) {
                                            tomorrowTasksList.add(activity)
                                        }
                                    } else {
                                        // Para atividades normais e recorrentes
                                        if (activity.recurrenceRule?.isNotEmpty() == true && activity.recurrenceRule != "CUSTOM") {
                                            // Verificar se a atividade base é para amanhã
                                            if (activityDate == tomorrow) {
                                                tomorrowTasksList.add(activity)
                                            }
                                            
                                            // Gerar instâncias recorrentes para amanhã
                                            val recurringInstances = recurrenceService.generateRecurringInstances(activity, tomorrow, tomorrow)
                                            val instancesForTomorrow = recurringInstances.filter { instance ->
                                                val instanceDate = LocalDate.parse(instance.date)
                                                instanceDate == tomorrow
                                            }
                                            tomorrowTasksList.addAll(instancesForTomorrow)
                                        } else {
                                            // Atividade única - verificar se é para amanhã
                                            if (activityDate == tomorrow) {
                                                tomorrowTasksList.add(activity)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w("GreetingWidget", "Erro ao processar atividade para amanhã: ${activity.title}", e)
                            }
                        }
                        
                        // Ordenar as tarefas de amanhã por horário crescente
                        tomorrowTasksList.sortWith(
                            compareBy<com.mss.thebigcalendar.data.model.Activity> { 
                                it.startTime ?: LocalTime.MAX 
                            }.thenByDescending { 
                                it.categoryColor?.toIntOrNull() ?: 0 
                            }
                        )
                        
                        tomorrowTasksList
                    } else {
                        emptyList()
                    }
                    
                    val tasksText = buildTasksText(context, todayTasks, tomorrowTasks, isNightTime)
                    views.setTextViewText(R.id.widget_tasks, tasksText)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e("GreetingWidget", "Erro ao carregar tarefas", e)
                views.setTextViewText(R.id.widget_tasks, context.getString(R.string.widget_error_loading_tasks))
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    /**
     * Verifica se está no período noturno (18h às 23h59)
     */
    private fun isNightTime(currentTime: LocalTime): Boolean {
        return currentTime.hour >= 18
    }

    /**
     * Obtém a saudação baseada no horário atual
     */
    private fun getGreetingBasedOnTime(context: Context): String {
        val currentTime = LocalTime.now()
        val hour = currentTime.hour
        
        return when (hour) {
            in 5..11 -> context.getString(R.string.widget_greeting_morning)
            in 12..17 -> context.getString(R.string.widget_greeting_afternoon)
            in 18..23 -> context.getString(R.string.widget_greeting_evening)
            else -> context.getString(R.string.widget_greeting_dawn) // 0-4
        }
    }

    /**
     * Constrói o texto das tarefas para exibição no widget
     */
    private fun buildTasksText(
        context: Context,
        todayTasks: List<com.mss.thebigcalendar.data.model.Activity>,
        tomorrowTasks: List<com.mss.thebigcalendar.data.model.Activity>,
        isNightTime: Boolean
    ): String {
        val maxTasksPerDay = 5 // Máximo de tarefas por dia para não sobrecarregar o widget
        
        // Construir texto das tarefas de hoje
        val todayText = if (todayTasks.isEmpty()) {
            context.getString(R.string.widget_no_tasks_today)
        } else {
            val tasksToShow = todayTasks.take(maxTasksPerDay)
            tasksToShow.joinToString("\n") { task ->
                val prefix = if (task.activityType == com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY) {
                    "${context.getString(R.string.widget_birthday_icon)} " // Ícone de aniversário
                } else if (task.startTime != null) {
                    "${task.startTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} "
                } else {
                    ""
                }
                "$prefix${task.title}"
            } + if (todayTasks.size > maxTasksPerDay) "\n..." else ""
        }
        
        // Se não for noite ou não há tarefas de amanhã, retornar apenas as de hoje
        if (!isNightTime || tomorrowTasks.isEmpty()) {
            return todayText
        }
        
        // Construir texto das tarefas de amanhã
        val tomorrowText = if (tomorrowTasks.isEmpty()) {
            ""
        } else {
            val tasksToShow = tomorrowTasks.take(maxTasksPerDay)
            val tomorrowHeader = "\n\n${context.getString(R.string.widget_tomorrow_header)}\n"
            val tasksList = tasksToShow.joinToString("\n") { task ->
                val prefix = if (task.activityType == com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY) {
                    "${context.getString(R.string.widget_birthday_icon)} " // Ícone de aniversário
                } else if (task.startTime != null) {
                    "${task.startTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} "
                } else {
                    ""
                }
                "$prefix${task.title}"
            }
            tomorrowHeader + tasksList + if (tomorrowTasks.size > maxTasksPerDay) "\n..." else ""
        }
        
        return todayText + tomorrowText
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, GreetingWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}
