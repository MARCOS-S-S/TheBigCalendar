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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarWidgetFixedColorsProvider : AppWidgetProvider() {

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
        val views = RemoteViews(context.packageName, R.layout.calendar_widget_fixed_colors)

        // Atualiza a data
        val locale = Locale("pt", "BR")
        val dayOfWeekFormat = java.text.SimpleDateFormat("EEEE", locale)
        val dayMonthFormat = java.text.SimpleDateFormat("dd/MM", locale)
        val date = java.util.Date()

        val dayOfWeek = dayOfWeekFormat.format(date).let { it.first().uppercase() + it.substring(1) }
        val dayMonth = dayMonthFormat.format(date)

        views.setTextViewText(R.id.widget_day_of_week, dayOfWeek)
        views.setTextViewText(R.id.widget_day_month, dayMonth)
        
//         Atualizar horário com cores diferentes para hora e minutos
//        val hoje = LocalDate.now()
//        val dia: DayOfWeek = hoje.dayOfWeek
//        val formatador = DateTimeFormatter.ofPattern("dd ' de ' MMMM", Locale("pt", "BR"))
//        val dataFormatada = hoje.format(formatador)
//        val minutes = String.format("%02d", currentTime.minute)
//
        // Criar texto HTML com cores diferentes
//        val coloredTime = "<font color='#FF0000'>$dia</font><font color='#FFFFFF'>:$dataFormatada</font>"
//        views.setTextViewText(R.id.widget_date, Html.fromHtml(dataFormatada, Html.FROM_HTML_MODE_COMPACT))
//
//        // Atualizar data
//        val currentDate = LocalDate.now()
//        val dateFormat = DateTimeFormatter.ofPattern("EEE., dd 'de' MMM.", Locale("pt", "BR"))
//        views.setTextViewText(R.id.widget_date, currentDate.format(dateFormat))
        
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
        val refreshIntent = Intent(context, CalendarWidgetFixedColorsProvider::class.java).apply {
            action = ACTION_APPWIDGET_REFRESH
            component = ComponentName(context, CalendarWidgetFixedColorsProvider::class.java)
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
                    val todayTasks = activities.filter { activity ->
                        try {
                            val activityDate = LocalDate.parse(activity.date)
                            
                            // Verificar se esta data específica foi excluída para atividades recorrentes
                            val isExcluded = if (activity.recurrenceRule?.isNotEmpty() == true && activity.recurrenceRule != "CUSTOM") {
                                activity.excludedDates.contains(today.toString())
                            } else {
                                false
                            }
                            
                            if (isExcluded) {
                                false
                            } else {
                                // Para aniversários, verificar se é o mesmo dia e mês (ignorando o ano)
                                if (activity.activityType == com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY) {
                                    activityDate.month == today.month && activityDate.dayOfMonth == today.dayOfMonth
                                } else {
                                    activityDate == today
                                }
                            }
                        } catch (_: Exception) {
                            false
                        }
                    }.sortedWith(
                        compareByDescending<com.mss.thebigcalendar.data.model.Activity> { 
                            it.categoryColor?.toIntOrNull() ?: 0 
                        }.thenBy { 
                            it.startTime ?: LocalTime.MIN 
                        }
                    )
                    
                    val tomorrowTasks = if (isNightTime) {
                        activities.filter { activity ->
                            try {
                                val activityDate = LocalDate.parse(activity.date)
                                
                                // Verificar se esta data específica foi excluída para atividades recorrentes
                                val isExcluded = if (activity.recurrenceRule?.isNotEmpty() == true && activity.recurrenceRule != "CUSTOM") {
                                    activity.excludedDates.contains(tomorrow.toString())
                                } else {
                                    false
                                }
                                
                                if (isExcluded) {
                                    false
                                } else {
                                    // Para aniversários, verificar se é o mesmo dia e mês (ignorando o ano)
                                    if (activity.activityType == com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY) {
                                        activityDate.month == tomorrow.month && activityDate.dayOfMonth == tomorrow.dayOfMonth
                                    } else {
                                        activityDate == tomorrow
                                    }
                                }
                            } catch (e: Exception) {
                                false
                            }
                        }.sortedWith(
                            compareByDescending<com.mss.thebigcalendar.data.model.Activity> { 
                                it.categoryColor.toIntOrNull() ?: 0
                            }.thenBy { 
                                it.startTime ?: LocalTime.MIN 
                            }
                        )
                    } else {
                        emptyList()
                    }
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        val tasksText = buildTasksText(todayTasks, tomorrowTasks, isNightTime)
                        
                        // Log para debug
                        Log.d("CalendarWidget", "📅 Widget atualizado: ${todayTasks.size} tarefas hoje, ${tomorrowTasks.size} amanhã")
                        todayTasks.forEach { task ->
                            Log.d("CalendarWidget", "   📅 ${task.title} (${task.activityType})")
                        }
                        
                        // Aplicar HTML para quebras de linha funcionarem
                        views.setTextViewText(R.id.widget_tasks, android.text.Html.fromHtml(tasksText, android.text.Html.FROM_HTML_MODE_COMPACT))
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } catch (_: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    views.setTextViewText(R.id.widget_tasks, android.text.Html.fromHtml("Erro ao carregar tarefas", android.text.Html.FROM_HTML_MODE_COMPACT))
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
    
    /**
     * Verifica se está no período noturno (pôr do sol até meia-noite)
     * Considera o horário real do pôr do sol baseado na estação do ano
     */
    private fun isNightTime(currentTime: LocalTime): Boolean {
        val currentDate = LocalDate.now()
        val month = currentDate.monthValue
        
        // Horários de pôr do sol aproximados por estação (Brasil)
        val sunsetTime = when (month) {
            in 12..2 -> LocalTime.of(19, 30) // Verão (dez-fev): ~19:30
            in 3..5 -> LocalTime.of(18, 30)  // Outono (mar-mai): ~18:30
            in 6..8 -> LocalTime.of(17, 30)  // Inverno (jun-ago): ~17:30
            in 9..11 -> LocalTime.of(18, 0)  // Primavera (set-nov): ~18:00
            else -> LocalTime.of(18, 0)      // Padrão
        }
        
        val midnight = LocalTime.of(0, 0) // 00:00 (meia-noite)
        
        return currentTime.isAfter(sunsetTime) || currentTime.isBefore(midnight)
    }
    
    /**
     * Constrói o texto das tarefas incluindo as de amanhã se for noite
     */
    private fun buildTasksText(
        todayTasks: List<com.mss.thebigcalendar.data.model.Activity>,
        tomorrowTasks: List<com.mss.thebigcalendar.data.model.Activity>,
        isNightTime: Boolean
    ): String {
        val maxTasksPerDay = 5 // Máximo de tarefas por dia para não sobrecarregar o widget
        
        // Construir texto das tarefas de hoje
        val todayText = if (todayTasks.isEmpty()) {
            "Nenhuma tarefa para hoje"
        } else {
            val tasksToShow = todayTasks.take(maxTasksPerDay)
            tasksToShow.joinToString("<br>") { task ->
                val prefix = if (task.activityType == com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY) {
                    "🎂 " // Ícone de aniversário
                } else if (task.startTime != null) {
                    "${task.startTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} "
                } else {
                    ""
                }
                "$prefix${task.title}"
            } + if (todayTasks.size > maxTasksPerDay) "<br>..." else ""
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
            val tomorrowHeader = "<br><br><b>Amanhã:</b><br>"
            val tasksList = tasksToShow.joinToString("<br>") { task ->
                val prefix = if (task.activityType == com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY) {
                    "🎂 " // Ícone de aniversário
                } else if (task.startTime != null) {
                    "${task.startTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} "
                } else {
                    ""
                }
                "$prefix${task.title}"
            }
            tomorrowHeader + tasksList + if (tomorrowTasks.size > maxTasksPerDay) "<br>..." else ""
        }
        
        return todayText + tomorrowText
    }

    override fun onEnabled(context: Context) {
        // Agendar atualizações automáticas do widget
        scheduleWidgetUpdates(context)
    }

    override fun onDisabled(context: Context) {
        // Cancelar atualizações automáticas quando o widget for desabilitado
        cancelWidgetUpdates(context)
    }
    
    /**
     * Agenda atualizações automáticas do widget para mostrar tarefas de amanhã à noite
     */
    private fun scheduleWidgetUpdates(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, CalendarWidgetFixedColorsProvider::class.java).apply {
            action = ACTION_APPWIDGET_UPDATE_NIGHT
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Calcular próximo horário de pôr do sol
        val nextSunset = getNextSunsetTime()
        val initialDelay = java.time.Duration.between(LocalTime.now(), nextSunset).toMillis()
        
        // Agendar primeira atualização no pôr do sol
        if (initialDelay > 0) {
            alarmManager.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + initialDelay,
                pendingIntent
            )
        }
        
        // Agendar atualizações diárias às 00:00 para resetar o widget
        val midnightIntent = Intent(context, CalendarWidgetFixedColorsProvider::class.java).apply {
            action = ACTION_APPWIDGET_UPDATE_MIDNIGHT
        }
        
        val midnightPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            midnightIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }
        
        alarmManager.setRepeating(
            android.app.AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            android.app.AlarmManager.INTERVAL_DAY,
            midnightPendingIntent
        )
    }
    
    /**
     * Cancela as atualizações automáticas do widget
     */
    private fun cancelWidgetUpdates(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        
        val intent = Intent(context, CalendarWidgetFixedColorsProvider::class.java).apply {
            action = ACTION_APPWIDGET_UPDATE_NIGHT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        
        val midnightIntent = Intent(context, CalendarWidgetFixedColorsProvider::class.java).apply {
            action = ACTION_APPWIDGET_UPDATE_MIDNIGHT
        }
        val midnightPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            midnightIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(midnightPendingIntent)
    }
    
    /**
     * Calcula o próximo horário de pôr do sol
     */
    private fun getNextSunsetTime(): LocalTime {
        val currentDate = LocalDate.now()
        val month = currentDate.monthValue
        
        return when (month) {
            in 12..2 -> LocalTime.of(19, 30) // Verão (dez-fev): ~19:30
            in 3..5 -> LocalTime.of(18, 30)  // Outono (mar-mai): ~18:30
            in 6..8 -> LocalTime.of(17, 30)  // Inverno (jun-ago): ~17:30
            in 9..11 -> LocalTime.of(18, 0)  // Primavera (set-nov): ~18:00
            else -> LocalTime.of(18, 0)      // Padrão
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        when (intent?.action) {
            ACTION_APPWIDGET_REFRESH -> {
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
            ACTION_APPWIDGET_UPDATE_NIGHT -> {
                // Atualizar todos os widgets para mostrar tarefas de amanhã
                context?.let {
                    val appWidgetManager = AppWidgetManager.getInstance(it)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        ComponentName(it, CalendarWidgetFixedColorsProvider::class.java)
                    )
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(it, appWidgetManager, appWidgetId)
                    }
                }
            }
            ACTION_APPWIDGET_UPDATE_MIDNIGHT -> {
                // Atualizar todos os widgets para resetar (não mostrar tarefas de amanhã)
                context?.let {
                    val appWidgetManager = AppWidgetManager.getInstance(it)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        ComponentName(it, CalendarWidgetFixedColorsProvider::class.java)
                    )
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(it, appWidgetManager, appWidgetId)
                    }
                }
            }
        }
    }

    companion object {
        private const val ACTION_APPWIDGET_REFRESH = "com.mss.thebigcalendar.ACTION_APPWIDGET_REFRESH_FIXED_COLORS"
        private const val ACTION_APPWIDGET_UPDATE_NIGHT = "com.mss.thebigcalendar.ACTION_APPWIDGET_UPDATE_NIGHT"
        private const val ACTION_APPWIDGET_UPDATE_MIDNIGHT = "com.mss.thebigcalendar.ACTION_APPWIDGET_UPDATE_MIDNIGHT"
    }
}