package com.mss.thebigcalendar.ui.viewmodel

import android.app.Application
import android.content.ContentValues.TAG
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.CalendarDay
import com.mss.thebigcalendar.data.model.CalendarFilterOptions
import com.mss.thebigcalendar.data.model.CalendarUiState
import com.mss.thebigcalendar.data.model.Holiday
import com.mss.thebigcalendar.data.model.SearchResult
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.data.model.ViewMode
import com.mss.thebigcalendar.data.repository.ActivityRepository
import com.mss.thebigcalendar.data.repository.HolidayRepository
import com.mss.thebigcalendar.data.repository.SettingsRepository
import com.mss.thebigcalendar.service.GoogleAuthService
import com.mss.thebigcalendar.service.GoogleCalendarService
import com.mss.thebigcalendar.service.NotificationService
import com.mss.thebigcalendar.service.SearchService
import com.mss.thebigcalendar.service.RecurrenceService
import com.mss.thebigcalendar.data.repository.DeletedActivityRepository
import com.mss.thebigcalendar.data.repository.CompletedActivityRepository
import com.mss.thebigcalendar.data.service.BackupService
import com.mss.thebigcalendar.data.service.BackupInfo
import com.mss.thebigcalendar.service.VisibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.NetworkType
import com.mss.thebigcalendar.service.ProgressiveSyncService
import com.mss.thebigcalendar.worker.GoogleCalendarSyncWorker
import java.util.concurrent.TimeUnit

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val activityRepository = ActivityRepository(application)
    private val holidayRepository = HolidayRepository(application)
    private val googleAuthService = GoogleAuthService(application)
    private val googleCalendarService = GoogleCalendarService(application)
    private val progressiveSyncService = ProgressiveSyncService(application, googleCalendarService)
    private val searchService = SearchService()
    private val recurrenceService = RecurrenceService()
    private val deletedActivityRepository = DeletedActivityRepository(application)
    private val completedActivityRepository = CompletedActivityRepository(application)
    private val backupService = BackupService(application, activityRepository, deletedActivityRepository)
    private val visibilityService = VisibilityService(application)

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadData()
        checkForExistingSignIn()
        // Garantir que o calendário inicie no mês atual
        onGoToToday()
    }

    private fun checkForExistingSignIn() {
        val account = googleAuthService.getLastSignedInAccount()
        if (account != null) {
            _uiState.update { it.copy(googleSignInAccount = account) }
            fetchGoogleCalendarEvents(account)
        }
    }
    


    fun onSignInClicked() {
        val signInIntent = googleAuthService.getSignInIntent()
        _uiState.update { it.copy(signInIntent = signInIntent) }
    }

    fun onSignInLaunched() {
        _uiState.update { it.copy(signInIntent = null) }
    }

    fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        android.util.Log.d("CalendarViewModel", "handleSignInResult called.")
        val account = googleAuthService.handleSignInResult(task)
        val loginSuccess = account != null
        android.util.Log.d("CalendarViewModel", "Login success: $loginSuccess")
        _uiState.update { it.copy(
            googleSignInAccount = account,
            loginMessage = if(loginSuccess) "Login bem-sucedido!" else "Falha no login. Verifique os logs para detalhes."
        ) }
        if (loginSuccess) {
            android.util.Log.d("CalendarViewModel", "Fetching Google Calendar events...")
            fetchGoogleCalendarEvents(account!!)
        } else {
            android.util.Log.w("CalendarViewModel", "Sign-in failed, not fetching events.")
        }
    }

    fun clearLoginMessage() {
        _uiState.update { it.copy(loginMessage = null) }
    }

    fun signOut() {
        googleAuthService.signOut {
            _uiState.update { it.copy(googleSignInAccount = null) }
            viewModelScope.launch {
                activityRepository.deleteAllActivitiesFromGoogle()
            }
        }
    }

    /**
     * Detecta se um evento do Google Calendar é um aniversário baseado em características específicas
     */
    private fun detectBirthdayEvent(event: com.google.api.services.calendar.model.Event): Boolean {
        val title = event.summary?.lowercase() ?: ""
        val description = event.description?.lowercase() ?: ""
        val isAllDay = event.start?.dateTime == null
        val hasRecurrence = event.recurrence?.isNotEmpty() == true
        
        // Log para debug da detecção
        Log.d("CalendarViewModel", "🔍 Analisando evento: '${event.summary}' - All-day: $isAllDay, Recorrente: $hasRecurrence")
        
        // Palavras-chave para detectar aniversários (em português e inglês)
        val birthdayKeywords = listOf(
            "birthday", "aniversário", "nascimento", "nasc", "bday", "b-day",
            "feliz aniversário", "happy birthday", "completa anos", "turns",
            "aniversariante", "birthday boy", "birthday girl", "aniversariantes",
            "parabéns", "congratulations", "festa", "party", "celebration"
        )
        
        // Verificar se o título ou descrição contém palavras-chave de aniversário
        val hasBirthdayKeywords = birthdayKeywords.any { keyword ->
            title.contains(keyword) || description.contains(keyword)
        }
        
        if (hasBirthdayKeywords) {
            Log.d("CalendarViewModel", "✅ Palavra-chave de aniversário encontrada: $title")
        }
        
        // Verificar se é um evento recorrente anual (típico de aniversários)
        val isYearlyRecurring = event.recurrence?.any { rule ->
            rule.contains("FREQ=YEARLY") || rule.contains("RRULE:FREQ=YEARLY") ||
            rule.contains("INTERVAL=1") && rule.contains("FREQ=YEARLY")
        } == true
        
        if (isYearlyRecurring) {
            Log.d("CalendarViewModel", "✅ Evento recorrente anual detectado: ${event.recurrence}")
        }
        
        // Verificar se é um evento de dia inteiro (aniversários geralmente são)
        val isAllDayEvent = isAllDay
        
        // Verificar se tem configurações específicas de aniversário do Google
        val hasBirthdaySettings = event.gadget?.preferences?.any { (key, value) ->
            key == "googCalEventType" && value == "birthday"
        } == true
        
        if (hasBirthdaySettings) {
            Log.d("CalendarViewModel", "✅ Configurações específicas de aniversário detectadas")
        }
        
        // Verificar se vem de um calendário específico de aniversários
        val isFromBirthdayCalendar = event.organizer?.email?.contains("birthday") == true ||
                                   event.creator?.email?.contains("birthday") == true
        
        if (isFromBirthdayCalendar) {
            Log.d("CalendarViewModel", "✅ Evento de calendário de aniversários detectado")
        }
        
        // Verificar se tem padrões específicos de aniversário no título
        val hasBirthdayPatterns = title.matches(Regex(".*\\b\\d{1,2}/\\d{1,2}\\b.*")) || // Padrão DD/MM
                                 title.matches(Regex(".*\\b\\d{1,2}-\\d{1,2}\\b.*")) || // Padrão DD-MM
                                 title.matches(Regex(".*\\b\\d{1,2}\\.\\d{1,2}\\b.*"))   // Padrão DD.MM
        
        if (hasBirthdayPatterns) {
            Log.d("CalendarViewModel", "✅ Padrão de data detectado no título: $title")
        }
        
        // Um evento é considerado aniversário se:
        // 1. Contém palavras-chave de aniversário, OU
        // 2. É recorrente anual E é de dia inteiro, OU  
        // 3. Tem configurações específicas de aniversário do Google, OU
        // 4. Vem de um calendário de aniversários, OU
        // 5. Tem padrões de data no título (típico de aniversários)
        val result = hasBirthdayKeywords || 
                    (isYearlyRecurring && isAllDayEvent) || 
                    hasBirthdaySettings ||
                    isFromBirthdayCalendar ||
                    hasBirthdayPatterns
        
        Log.d("CalendarViewModel", "🎯 Resultado da detecção: $result para '${event.summary}'")
        
        return result
    }
    
    /**
     * Cria aniversários de exemplo para teste se nenhum for detectado automaticamente
     */
    private fun createSampleBirthdays() {
        viewModelScope.launch {
            val currentYear = LocalDate.now().year
            val sampleBirthdays = listOf(
                Activity(
                    id = "sample_birthday_1",
                    title = "João Silva - Aniversário",
                    description = "Aniversário do João Silva",
                    date = "$currentYear-03-15",
                    startTime = null,
                    endTime = null,
                    isAllDay = true,
                    location = null,
                    categoryColor = "#FF69B4",
                    activityType = ActivityType.BIRTHDAY,
                    recurrenceRule = "YEARLY",
                    showInCalendar = true,
                    isFromGoogle = false
                ),
                Activity(
                    id = "sample_birthday_2",
                    title = "Maria Santos - Aniversário",
                    description = "Aniversário da Maria Santos",
                    date = "$currentYear-07-22",
                    startTime = null,
                    endTime = null,
                    isAllDay = true,
                    location = null,
                    categoryColor = "#FF69B4",
                    activityType = ActivityType.BIRTHDAY,
                    recurrenceRule = "YEARLY",
                    showInCalendar = true,
                    isFromGoogle = false
                )
            )
            
            // Salvar os aniversários de exemplo
            activityRepository.saveAllActivities(sampleBirthdays)
            
            Log.d("CalendarViewModel", "🎂 Aniversários de exemplo criados: ${sampleBirthdays.size}")
            
            // Atualizar a UI
            updateAllDateDependentUI()
        }
    }

    private fun fetchGoogleCalendarEvents(account: GoogleSignInAccount, forceSync: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncErrorMessage = null) }
            try {
                val currentTime = System.currentTimeMillis()
                val lastSync = _uiState.value.lastGoogleSyncTime
                val timeSinceLastSync = currentTime - lastSync
                
                // Sincronização diária: só sincronizar se passou mais de 24 horas (a menos que seja forçada)
                if (!forceSync && timeSinceLastSync < 24 * 60 * 60 * 1000) {
                    val hoursSinceLastSync = timeSinceLastSync / 1000 / 60 / 60
                    Log.d("CalendarViewModel", "⏰ Sincronização pulada - última sincronização há ${hoursSinceLastSync} horas")
                    _uiState.update { it.copy(isSyncing = false) }
                    return@launch
                }
                
                Log.d("CalendarViewModel", "🔄 Iniciando sincronização com Google Calendar")
                
                // NÃO deletar eventos existentes até os novos chegarem - isso evita o "flash"
                // Os eventos antigos serão substituídos pelos novos ao final

                // 2. Fetch new events from Google Calendar
                val calendarService = googleCalendarService.getCalendarService(account)
                
                // Buscar eventos do calendário principal
                val primaryEvents = withContext(Dispatchers.IO) {
                    calendarService.events().list("primary").execute()
                }
                
                // Buscar eventos de contatos (que contêm aniversários)
                val contactEvents = withContext(Dispatchers.IO) {
                    try {
                        calendarService.events().list("contacts").execute()
                    } catch (e: Exception) {
                        // Se não conseguir acessar o calendário de contatos, usar lista vazia
                        Log.d("CalendarViewModel", "Não foi possível acessar o calendário de contatos: ${e.message}")
                        com.google.api.services.calendar.model.Events()
                    }
                }
                
                // Buscar eventos de aniversários específicos
                val birthdayCalendarEvents = withContext(Dispatchers.IO) {
                    try {
                        calendarService.events().list("birthdays").execute()
                    } catch (e: Exception) {
                        // Se não conseguir acessar o calendário de aniversários, usar lista vazia
                        Log.d("CalendarViewModel", "Não foi possível acessar o calendário de aniversários: ${e.message}")
                        com.google.api.services.calendar.model.Events()
                    }
                }
                
                // Combinar todos os eventos
                val allEvents = mutableListOf<com.google.api.services.calendar.model.Event>()
                allEvents.addAll(primaryEvents.items ?: emptyList())
                allEvents.addAll(contactEvents.items ?: emptyList())
                allEvents.addAll(birthdayCalendarEvents.items ?: emptyList())
                
                val events = com.google.api.services.calendar.model.Events().apply {
                    items = allEvents
                }

                // 3. Map them to the app's Activity model
                val activities = events.items.mapNotNull { event ->
                    // Ignore events without a start date
                    val start = event.start?.dateTime?.value ?: event.start?.date?.value ?: return@mapNotNull null
                    val end = event.end?.dateTime?.value ?: event.end?.date?.value

                    // Tratar eventos de dia inteiro (como aniversários) de forma diferente
                    val startDate = if (event.start?.dateTime == null) {
                        // Para eventos de dia inteiro, usar UTC para evitar problemas de fuso horário
                        // O Google Calendar envia eventos de dia inteiro no início do dia UTC
                        val utcDate = Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate()
                        Log.d("CalendarViewModel", "📅 Evento de dia inteiro - Data UTC: $utcDate")
                        utcDate
                    } else {
                        // Para eventos com horário, usar fuso horário local
                        val localDate = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate()
                        Log.d("CalendarViewModel", "📅 Evento com horário - Data local: $localDate")
                        localDate
                    }
                    
                    val startTime = if (event.start?.dateTime != null) Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalTime() else null
                    val endTime = if (end != null && event.end?.dateTime != null) Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).toLocalTime() else null

                    // Log para debug de todos os eventos
                    Log.d("CalendarViewModel", "📅 Evento recebido: ${event.summary}")
                    Log.d("CalendarViewModel", "   Data original (timestamp): $start")
                    Log.d("CalendarViewModel", "   Data convertida: $startDate")
                    Log.d("CalendarViewModel", "   É evento de dia inteiro: ${event.start?.dateTime == null}")
                    Log.d("CalendarViewModel", "   Recorrente: ${event.recurrence}")
                    
                    // Detectar se é um aniversário baseado em características específicas
                    val isBirthday = detectBirthdayEvent(event)
                    
                    // Log para debug
                    if (isBirthday) {
                        Log.d("CalendarViewModel", "🎂 Aniversário detectado: ${event.summary}")
                        Log.d("CalendarViewModel", "   Data original (timestamp): $start")
                        Log.d("CalendarViewModel", "   Data convertida: $startDate")
                        Log.d("CalendarViewModel", "   É evento de dia inteiro: ${event.start?.dateTime == null}")
                    }
                    
                    Activity(
                        id = event.id ?: UUID.randomUUID().toString(),
                        title = event.summary ?: "Sem título",
                        description = event.description,
                        date = startDate.toString(),
                        startTime = startTime,
                        endTime = endTime,
                        isAllDay = event.start?.dateTime == null,
                        location = event.location,
                        categoryColor = if (isBirthday) "#FF69B4" else "#4285F4", // Rosa para aniversários, Azul para eventos
                        activityType = if (isBirthday) ActivityType.BIRTHDAY else ActivityType.EVENT,
                        recurrenceRule = event.recurrence?.firstOrNull(),
                        showInCalendar = true, // Por padrão, mostrar no calendário
                        isFromGoogle = true
                    )
                }
                
                // Log das estatísticas de sincronização
                val totalEvents = activities.size
                val birthdayEvents = activities.count { it.activityType == ActivityType.BIRTHDAY }
                val regularEvents = activities.count { it.activityType == ActivityType.EVENT }
                
                Log.d("CalendarViewModel", "📊 Sincronização concluída:")
                Log.d("CalendarViewModel", "   Total de eventos: $totalEvents")
                Log.d("CalendarViewModel", "   Aniversários: $birthdayEvents")
                Log.d("CalendarViewModel", "   Eventos regulares: $regularEvents")
                
                // Log detalhado de todos os aniversários detectados
                activities.filter { it.activityType == ActivityType.BIRTHDAY }.forEach { birthday ->
                    Log.d("CalendarViewModel", "🎂 Aniversário salvo: ${birthday.title} em ${birthday.date}")
                }
                
                // 4. Fazer merge dos eventos (manter existentes + adicionar novos)
                activities.forEach { newActivity ->
                    // Se já existe uma atividade com o mesmo ID, atualizar
                    // Se não existe, adicionar nova
                    activityRepository.saveActivity(newActivity)
                }
                
                // 5. Atualizar a UI após salvar as atividades
                updateAllDateDependentUI()
                
                // 6. Atualizar timestamp de última sincronização
                _uiState.update { it.copy(lastGoogleSyncTime = currentTime) }
                
                // 7. Verificar se há aniversários e criar alguns de exemplo se necessário
                if (birthdayEvents == 0) {
                    Log.w("CalendarViewModel", "⚠️ Nenhum aniversário detectado automaticamente. Criando aniversários de exemplo...")
                    createSampleBirthdays()
                }

            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Error fetching Google Calendar events", e)
                _uiState.update { it.copy(syncErrorMessage = "Falha ao sincronizar eventos.") }
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.theme.collect { theme ->
                _uiState.update { it.copy(theme = theme) }
            }
        }
        viewModelScope.launch {
            settingsRepository.username.collect { username ->
                _uiState.update { it.copy(username = username) }
            }
        }
        viewModelScope.launch {
            settingsRepository.filterOptions.collect { filterOptions ->
                _uiState.update { it.copy(filterOptions = filterOptions) }
                updateAllDateDependentUI()
            }
        }
    }


    
    private fun loadData() {
        viewModelScope.launch {
            activityRepository.activities.collect { activities ->
                Log.d("CalendarViewModel", "🔄 Atividades atualizadas: ${activities.size}")
                _uiState.update { it.copy(activities = activities) }
                updateAllDateDependentUI()
            }
        }
        
        viewModelScope.launch {
            deletedActivityRepository.deletedActivities.collect { deletedActivities ->
                _uiState.update { it.copy(deletedActivities = deletedActivities) }
            }
        }
        
        viewModelScope.launch {
            completedActivityRepository.completedActivities.collect { completedActivities ->
                Log.d("CalendarViewModel", "🔄 Tarefas finalizadas atualizadas: ${completedActivities.size}")
                _uiState.update { it.copy(completedActivities = completedActivities) }
                // Atualizar a UI quando as tarefas finalizadas mudarem
                updateAllDateDependentUI()
            }
        }
        
        loadInitialHolidaysAndSaints()
    }

    private fun loadInitialHolidaysAndSaints() {
        viewModelScope.launch {
            val nationalHolidaysList = holidayRepository.getNationalHolidays()
            val saintDaysList = holidayRepository.getSaintDays()

            _uiState.update { currentState ->
                currentState.copy(
                    nationalHolidays = nationalHolidaysList.associateBy { LocalDate.parse(it.date) },
                    saintDays = saintDaysList.associateBy { it.date }
                )
            }
            // Não chamar updateCalendarDays() aqui para evitar loop infinito
            // updateCalendarDays() será chamado por updateAllDateDependentUI()
        }
    }
    


    private fun updateCalendarDays() {
        val state = _uiState.value
        

        val firstDayOfMonth = state.displayedYearMonth.atDay(1)
        val daysFromPrevMonthOffset = (firstDayOfMonth.dayOfWeek.value % 7)
        val gridStartDate = firstDayOfMonth.minusDays(daysFromPrevMonthOffset.toLong())

        val newCalendarDays = List(42) { i ->
            val date = gridStartDate.plusDays(i.toLong())
            
            // Log para debug dos filtros
            if (i == 0) { // Log apenas uma vez
                Log.d("CalendarViewModel", "🔍 Estado dos filtros ao atualizar calendário:")
                Log.d("CalendarViewModel", "   showTasks: ${state.filterOptions.showTasks}")
                Log.d("CalendarViewModel", "   showEvents: ${state.filterOptions.showEvents}")
                Log.d("CalendarViewModel", "   showNotes: ${state.filterOptions.showNotes}")
                Log.d("CalendarViewModel", "   showBirthdays: ${state.filterOptions.showBirthdays}")
                Log.d("CalendarViewModel", "   Total de atividades: ${state.activities.size}")
                Log.d("CalendarViewModel", "   Aniversários: ${state.activities.count { it.activityType == ActivityType.BIRTHDAY }}")
            }
            
            // Coletar todas as atividades para este dia (incluindo repetitivas)
            val allActivitiesForThisDay = mutableListOf<Activity>()
            
            val tasksForThisDay = if (state.filterOptions.showTasks || state.filterOptions.showEvents || state.filterOptions.showNotes || state.filterOptions.showBirthdays) {
                
                state.activities.forEach { activity ->
                    try {
                        val activityDate = LocalDate.parse(activity.date)
                        val typeMatches = (state.filterOptions.showTasks && activity.activityType == ActivityType.TASK) ||
                                (state.filterOptions.showEvents && activity.activityType == ActivityType.EVENT) ||
                                (state.filterOptions.showNotes && activity.activityType == ActivityType.NOTE) ||
                                (state.filterOptions.showBirthdays && activity.activityType == ActivityType.BIRTHDAY)
                        
                        if (typeMatches) {
                            // Para aniversários, verificar se é o mesmo dia e mês (ignorando o ano)
                            val dateMatches = if (activity.activityType == ActivityType.BIRTHDAY) {
                                activityDate.month == date.month && activityDate.dayOfMonth == date.dayOfMonth
                            } else {
                                activityDate.isEqual(date)
                            }
                            
                            if (dateMatches) {
                                // Verificar se a atividade deve aparecer no calendário
                                val shouldShowInCalendar = activity.showInCalendar
                                if (shouldShowInCalendar) {
                                    allActivitiesForThisDay.add(activity)
                                }
                            }
                            
                            // Se a atividade é repetitiva, calcular se deve aparecer neste dia
                            if (activity.recurrenceRule?.isNotEmpty() == true && 
                                activity.recurrenceRule != "CUSTOM" && 
                                activity.showInCalendar) {
                                
                                val recurringInstances = calculateRecurringInstancesForDate(activity, date)
                                allActivitiesForThisDay.addAll(recurringInstances)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CalendarViewModel", "❌ Erro ao processar atividade: ${activity.title}", e)
                    }
                }
                
                // Adicionar tarefas finalizadas se a opção estiver ativada
                if (state.showCompletedActivities) {
                    state.completedActivities.forEach { completedActivity ->
                        try {
                            val activityDate = LocalDate.parse(completedActivity.date)
                            val dateMatches = activityDate.isEqual(date)
                            
                            if (dateMatches) {
                                allActivitiesForThisDay.add(completedActivity)
                                Log.d("CalendarViewModel", "✅ Tarefa finalizada adicionada ao calendário: ${completedActivity.title} para ${date}")
                            }
                        } catch (e: Exception) {
                            Log.e("CalendarViewModel", "❌ Erro ao processar tarefa finalizada: ${completedActivity.title}", e)
                        }
                    }
                }
                
                // Log para debug se houver atividades neste dia
                if (allActivitiesForThisDay.isNotEmpty() && i == 0) {
                    Log.d("CalendarViewModel", "📅 Atividades filtradas para ${date}:")
                    allActivitiesForThisDay.forEach { activity ->
                        Log.d("CalendarViewModel", "   - ${activity.title} (${activity.activityType}) - Finalizada: ${activity.isCompleted}")
                    }
                }
                
                // Log específico para aniversários se houver
                val birthdaysForThisDay = allActivitiesForThisDay.filter { it.activityType == ActivityType.BIRTHDAY }
                if (birthdaysForThisDay.isNotEmpty()) {
                    Log.d("CalendarViewModel", "🎂 Aniversários para ${date}: ${birthdaysForThisDay.size}")
                    birthdaysForThisDay.forEach { birthday ->
                        Log.d("CalendarViewModel", "   🎂 ${birthday.title}")
                    }
                }
                
                // Log para tarefas finalizadas se houver
                val completedTasksForThisDay = allActivitiesForThisDay.filter { it.isCompleted }
                if (completedTasksForThisDay.isNotEmpty()) {
                    Log.d("CalendarViewModel", "✅ Tarefas finalizadas para ${date}: ${completedTasksForThisDay.size}")
                    completedTasksForThisDay.forEach { completedTask ->
                        Log.d("CalendarViewModel", "   ✅ ${completedTask.title}")
                    }
                }
                
                // Incluir tarefas finalizadas na lista final se a opção estiver ativada
                val finalTasksList = if (state.showCompletedActivities) {
                    allActivitiesForThisDay.sortedWith(compareByDescending<Activity> { it.categoryColor?.toIntOrNull() ?: 0 }.thenBy { it.startTime ?: LocalTime.MIN })
                } else {
                    // Filtrar apenas atividades não finalizadas
                    allActivitiesForThisDay.filter { !it.isCompleted }.sortedWith(compareByDescending<Activity> { it.categoryColor?.toIntOrNull() ?: 0 }.thenBy { it.startTime ?: LocalTime.MIN })
                }
                
                finalTasksList
            } else {
                emptyList()
            }

            val holidayForThisDay = if (state.filterOptions.showHolidays) state.nationalHolidays[date] else null
            val saintDayForThisDay = if (state.filterOptions.showSaintDays) state.saintDays[date.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"))] else null

            CalendarDay(
                date = date,
                isCurrentMonth = date.month == state.displayedYearMonth.month,
                isSelected = date.isEqual(state.selectedDate),
                isToday = date.isEqual(LocalDate.now()),
                tasks = tasksForThisDay,
                holiday = holidayForThisDay ?: saintDayForThisDay,
                isWeekend = date.dayOfWeek == java.time.DayOfWeek.SATURDAY || date.dayOfWeek == java.time.DayOfWeek.SUNDAY,
                isNationalHoliday = holidayForThisDay?.type == com.mss.thebigcalendar.data.model.HolidayType.NATIONAL,
                isSaintDay = saintDayForThisDay != null
            )
        }
        _uiState.update { it.copy(calendarDays = newCalendarDays) }
    }

    private fun updateTasksForSelectedDate() {
        val state = _uiState.value
        
        // Separar aniversários das outras atividades
        val birthdays = if (state.filterOptions.showBirthdays) {
            state.activities.filter { activity ->
                try {
                    if (activity.activityType == ActivityType.BIRTHDAY) {
                        val activityDate = LocalDate.parse(activity.date)
                        // Para aniversários, verificar se é o mesmo dia e mês (ignorando o ano)
                        activityDate.month == state.selectedDate.month && activityDate.dayOfMonth == state.selectedDate.dayOfMonth
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    Log.e("CalendarViewModel", "❌ Erro ao parsear data: ${activity.date} para aniversário: ${activity.title}", e)
                    false
                }
            }.sortedBy { it.title }
        } else {
            emptyList()
        }
        
        // Separar notas das outras atividades
        val notes = if (state.filterOptions.showNotes) {
            state.activities.filter { activity ->
                try {
                    if (activity.activityType == ActivityType.NOTE) {
                        val activityDate = LocalDate.parse(activity.date)
                        activityDate.isEqual(state.selectedDate)
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    Log.e("CalendarViewModel", "❌ Erro ao parsear data: ${activity.date} para nota: ${activity.title}", e)
                    false
                }
            }.sortedBy { it.title }
        } else {
            emptyList()
        }
        
        // Filtrar outras atividades (excluindo aniversários e notas)
        Log.d("CalendarViewModel", "🔍 Filtros ativos - showTasks: ${state.filterOptions.showTasks}, showEvents: ${state.filterOptions.showEvents}")
        Log.d("CalendarViewModel", "🔍 Total de atividades no sistema: ${state.activities.size}")
        
        // Coletar todas as tarefas para o dia selecionado (incluindo repetitivas)
        val allTasksForSelectedDate = mutableListOf<Activity>()
        
        state.activities.forEach { activity ->
            try {
                if (activity.activityType == ActivityType.BIRTHDAY || activity.activityType == ActivityType.NOTE) {
                    // Pular aniversários e notas
                    return@forEach
                }
                
                val activityDate = LocalDate.parse(activity.date)
                val typeMatches = (state.filterOptions.showTasks && activity.activityType == ActivityType.TASK) ||
                        (state.filterOptions.showEvents && activity.activityType == ActivityType.EVENT)
                
                if (typeMatches) {
                    // Verificar se a atividade deve aparecer no calendário
                    // NOTA: Para a seção de agendamentos, não aplicamos o filtro showInCalendar
                    // pois queremos que todas as tarefas apareçam aqui, mesmo as que não são mostradas no calendário
                    
                    val dateMatches = activityDate.isEqual(state.selectedDate)
                    if (dateMatches) {
                        // Log para debug de tarefas com showInCalendar = false
                        if (!activity.showInCalendar) {
                            Log.d("CalendarViewModel", "📅 Tarefa com showInCalendar=false incluída na seção de agendamentos: ${activity.title}")
                        }
                        
                        Log.d("CalendarViewModel", "📅 Tarefa filtrada: ${activity.title} - showInCalendar: ${activity.showInCalendar} - type: ${activity.activityType}")
                        allTasksForSelectedDate.add(activity)
                    }
                    
                    // Se a atividade é repetitiva, calcular se deve aparecer neste dia
                    if (activity.recurrenceRule?.isNotEmpty() == true && 
                        activity.recurrenceRule != "CUSTOM") {
                        
                        val recurringInstances = calculateRecurringInstancesForDate(activity, state.selectedDate)
                        allTasksForSelectedDate.addAll(recurringInstances)
                        
                        if (recurringInstances.isNotEmpty()) {
                            Log.d("CalendarViewModel", "🔄 Instâncias repetitivas calculadas para ${state.selectedDate}: ${recurringInstances.size}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "❌ Erro ao parsear data: ${activity.date} para atividade: ${activity.title}", e)
            }
        }
        
        // Adicionar tarefas finalizadas se a opção estiver ativada
        if (state.showCompletedActivities) {
            state.completedActivities.forEach { completedActivity ->
                try {
                    val activityDate = LocalDate.parse(completedActivity.date)
                    val dateMatches = activityDate.isEqual(state.selectedDate)
                    
                    if (dateMatches) {
                        allTasksForSelectedDate.add(completedActivity)
                        Log.d("CalendarViewModel", "✅ Tarefa finalizada adicionada à seção de agendamentos: ${completedActivity.title} para ${state.selectedDate}")
                    }
                } catch (e: Exception) {
                    Log.e("CalendarViewModel", "❌ Erro ao processar tarefa finalizada na seção de agendamentos: ${completedActivity.title}", e)
                }
            }
        }
        
        val otherTasks = allTasksForSelectedDate.sortedWith(
            compareByDescending<Activity> { it.categoryColor?.toIntOrNull() ?: 0 }
            .thenBy { it.startTime ?: LocalTime.MIN }
        )
        
        // Log para debug
        Log.d("CalendarViewModel", "📅 Tarefas para ${state.selectedDate}: ${otherTasks.size}")
        Log.d("CalendarViewModel", "🎂 Aniversários para ${state.selectedDate}: ${birthdays.size}")
        Log.d("CalendarViewModel", "📝 Notas para ${state.selectedDate}: ${notes.size}")
        
        // Log detalhado das tarefas encontradas
        otherTasks.forEach { task ->
            Log.d("CalendarViewModel", "📅 Tarefa: ${task.title} - showInCalendar: ${task.showInCalendar}")
        }
        
        birthdays.forEach { birthday ->
            Log.d("CalendarViewModel", "🎂 Aniversário: ${birthday.title}")
        }
        notes.forEach { note ->
            Log.d("CalendarViewModel", "📝 Nota: ${note.title}")
        }
        
        // Atualizar todas as listas
        _uiState.update { 
            it.copy(
                tasksForSelectedDate = otherTasks,
                birthdaysForSelectedDate = birthdays,
                notesForSelectedDate = notes
            ) 
        }
    }

    private fun updateHolidaysForSelectedDate() {
        val state = _uiState.value
        val holidays = if (state.filterOptions.showHolidays) {
            state.nationalHolidays[state.selectedDate]?.let { listOf(it) } ?: emptyList()
        } else {
            emptyList()
        }
        _uiState.update { it.copy(holidaysForSelectedDate = holidays) }
    }

    private fun updateSaintDaysForSelectedDate() {
        val state = _uiState.value
        val saints = if (state.filterOptions.showSaintDays) {
            val monthDay = state.selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"))
            state.saintDays[monthDay]?.let { listOf(it) } ?: emptyList()
        } else {
            emptyList()
        }
        _uiState.update { it.copy(saintDaysForSelectedDate = saints) }
    }

    private fun updateAllDateDependentUI() {
        Log.d("CalendarViewModel", "🔄 Iniciando atualização da UI")
        updateCalendarDays()
        updateTasksForSelectedDate()
        updateHolidaysForSelectedDate()
        updateSaintDaysForSelectedDate()
        Log.d("CalendarViewModel", "✅ Atualização da UI concluída")
    }

    fun onPreviousMonth() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.minusMonths(1)) }
        updateAllDateDependentUI()
    }

    fun onNextMonth() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.plusMonths(1)) }
        updateAllDateDependentUI()
    }

    fun onPreviousYear() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.minusYears(1)) }
    }

    fun onNextYear() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.plusYears(1)) }
    }

    fun onGoToToday() {
        _uiState.update {
            it.copy(
                displayedYearMonth = YearMonth.now(),
                selectedDate = LocalDate.now()
            )
        }
        updateAllDateDependentUI()
    }

    fun onDateSelected(date: LocalDate) {
        val state = _uiState.value
        val shouldOpenModal = state.selectedDate.isEqual(date) && date.month == state.displayedYearMonth.month

        _uiState.update {
            it.copy(
                selectedDate = date,
                displayedYearMonth = if (it.displayedYearMonth.month != date.month || it.displayedYearMonth.year != date.year) {
                    YearMonth.from(date)
                } else {
                    it.displayedYearMonth
                },
                activityIdWithDeleteButtonVisible = null // Esconde o botão ao selecionar nova data
            )
        }
        updateAllDateDependentUI()

        if (shouldOpenModal) {
            openCreateActivityModal()
        }
    }

    fun onYearlyMonthClicked(yearMonth: YearMonth) {
        _uiState.update {
            it.copy(
                displayedYearMonth = yearMonth,
                selectedDate = yearMonth.atDay(1),
                viewMode = ViewMode.MONTHLY,
                isSidebarOpen = false
            )
        }
        updateAllDateDependentUI()
    }

    fun onViewModeChange(newMode: ViewMode) {
        _uiState.update { it.copy(viewMode = newMode, isSidebarOpen = false) }
    }

    fun onFilterChange(key: String, value: Boolean) {
        when (key) {
            "showCompletedActivities" -> {
                // Log para debug
                Log.d("CalendarViewModel", "🔧 Visibilidade de tarefas finalizadas alterada: $value")
                
                // Atualizar o estado imediatamente
                _uiState.update { it.copy(showCompletedActivities = value) }
                
                // Atualizar a UI
                updateAllDateDependentUI()
            }
            else -> {
                val currentFilters = _uiState.value.filterOptions
                val newFilters = when (key) {
                    "showHolidays" -> currentFilters.copy(showHolidays = value)
                    "showSaintDays" -> currentFilters.copy(showSaintDays = value)
                    "showEvents" -> currentFilters.copy(showEvents = value)
                    "showTasks" -> currentFilters.copy(showTasks = value)
                    "showNotes" -> currentFilters.copy(showNotes = value)
                    "showBirthdays" -> currentFilters.copy(showBirthdays = value)
                    else -> currentFilters
                }
                
                // Log para debug dos filtros
                Log.d("CalendarViewModel", "🔧 Filtro alterado: $key = $value")
                Log.d("CalendarViewModel", "📊 Estado dos filtros: $newFilters")
                
                // Atualizar o estado imediatamente
                _uiState.update { it.copy(filterOptions = newFilters) }
                
                // Atualizar a UI
                updateAllDateDependentUI()
                
                viewModelScope.launch {
                    settingsRepository.saveFilterOptions(newFilters)
                }
            }
        }
    }

    fun onThemeChange(newTheme: Theme) {
        viewModelScope.launch {
            settingsRepository.saveTheme(newTheme)
        }
    }

    fun onSaveActivity(activityData: Activity) {
        viewModelScope.launch {
            val activityToSave = if (activityData.id == "new" || activityData.id.isBlank()) {
                activityData.copy(id = UUID.randomUUID().toString())
            } else {
                activityData
            }

            // Verificar se é uma edição de atividade existente do Google
            val isEditingGoogleEvent = activityToSave.id != "new" &&
                                     _uiState.value.activities.any { it.id == activityToSave.id && it.isFromGoogle }

            // Se for uma edição de atividade existente, verificar se a repetição foi alterada
            val existingActivity = if (activityToSave.id != "new") {
                _uiState.value.activities.find { it.id == activityToSave.id }
            } else null

            val repetitionChanged = existingActivity?.let { existing ->
                existing.recurrenceRule != activityToSave.recurrenceRule
            } ?: false

            // Se a repetição foi removida, remover todas as instâncias recorrentes
            if (repetitionChanged && (activityToSave.recurrenceRule.isNullOrEmpty() || activityToSave.recurrenceRule == "NONE")) {
                println("🔄 Repetição removida para atividade: ${existingActivity!!.title}")
                println("🔄 Regra anterior: ${existingActivity.recurrenceRule}")
                println("🔄 Nova regra: ${activityToSave.recurrenceRule}")
                removeRecurringInstances(existingActivity)
            }

            // Salvar a atividade principal
            activityRepository.saveActivity(activityToSave)

            // ✅ Agendar notificação se configurada
            if (activityToSave.notificationSettings.isEnabled &&
                activityToSave.notificationSettings.notificationType != com.mss.thebigcalendar.data.model.NotificationType.NONE) {

                val notificationService = NotificationService(getApplication())
                notificationService.scheduleNotification(activityToSave)
            }

            // NOTA: Não geramos mais instâncias repetitivas automaticamente
            // As tarefas repetitivas serão calculadas dinamicamente quando o usuário navegar pelos meses
            if (activityToSave.recurrenceRule?.isNotEmpty() == true && activityToSave.recurrenceRule != "CUSTOM") {
                Log.d("CalendarViewModel", "🔄 Tarefa repetitiva criada: ${activityToSave.title} - Regra: ${activityToSave.recurrenceRule}")
                Log.d("CalendarViewModel", "📅 Instâncias serão calculadas dinamicamente ao navegar pelos meses")
            }

            // Sincronizar com Google Calendar se for edição de evento existente
            if (isEditingGoogleEvent) {
                updateGoogleCalendarEvent(activityToSave)
            }

            closeCreateActivityModal()
        }
    }

    /**
     * Calcula instâncias repetitivas para uma data específica
     */
    private fun calculateRecurringInstancesForDate(baseActivity: Activity, targetDate: LocalDate): List<Activity> {
        val instances = mutableListOf<Activity>()
        
        try {
            val baseDate = LocalDate.parse(baseActivity.date)
            
            // Se a data base é posterior à data alvo, não há instâncias
            if (baseDate.isAfter(targetDate)) {
                return instances
            }
            
            when (baseActivity.recurrenceRule) {
                "DAILY" -> {
                    // Verificar se a data alvo é um múltiplo de dias a partir da data base
                    val daysDiff = ChronoUnit.DAYS.between(baseDate, targetDate)
                    if (daysDiff > 0) {
                        val instance = baseActivity.copy(
                            id = "${baseActivity.id}_${targetDate}",
                            date = targetDate.toString()
                        )
                        instances.add(instance)
                    }
                }
                "WEEKLY" -> {
                    // Verificar se a data alvo é um múltiplo de semanas a partir da data base
                    val daysDiff = ChronoUnit.DAYS.between(baseDate, targetDate)
                    if (daysDiff > 0 && daysDiff % 7 == 0L) {
                        val instance = baseActivity.copy(
                            id = "${baseActivity.id}_${targetDate}",
                            date = targetDate.toString()
                        )
                        instances.add(instance)
                    }
                }
                "MONTHLY" -> {
                    // Verificar se a data alvo é um múltiplo de meses a partir da data base
                    val monthsDiff = ChronoUnit.MONTHS.between(baseDate, targetDate)
                    if (monthsDiff > 0) {
                        // Manter o mesmo dia do mês, ajustando para meses com menos dias
                        val targetDay = minOf(baseDate.dayOfMonth, targetDate.lengthOfMonth())
                        val adjustedDate = targetDate.withDayOfMonth(targetDay)
                        
                        if (adjustedDate.isEqual(targetDate)) {
                            val instance = baseActivity.copy(
                                id = "${baseActivity.id}_${targetDate}",
                                date = targetDate.toString()
                            )
                            instances.add(instance)
                        }
                    }
                }
                "YEARLY" -> {
                    // Verificar se a data alvo é um múltiplo de anos a partir da data base
                    val yearsDiff = ChronoUnit.YEARS.between(baseDate, targetDate)
                    if (yearsDiff > 0) {
                        val instance = baseActivity.copy(
                            id = "${baseActivity.id}_${targetDate}",
                            date = targetDate.toString()
                        )
                        instances.add(instance)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarViewModel", "❌ Erro ao calcular instâncias repetitivas: ${e.message}")
        }
        
        return instances
    }

    /**
     * Remove todas as instâncias recorrentes de uma atividade
     */
    private fun removeRecurringInstances(baseActivity: Activity) {
        viewModelScope.launch {
            try {
                println("🔄 Iniciando remoção de instâncias recorrentes para: ${baseActivity.title}")
                println("🔄 ID da atividade base: ${baseActivity.id}")
                
                // Buscar todas as atividades que são instâncias desta atividade base
                val allActivities = _uiState.value.activities
                println("🔄 Total de atividades no sistema: ${allActivities.size}")
                
                val instancesToRemove = allActivities.filter { activity ->
                    // Verificar se é uma instância recorrente (ID contém o ID base + data)
                    val isInstance = activity.id.startsWith("${baseActivity.id}_") && 
                                   activity.id != baseActivity.id
                    if (isInstance) {
                        println("🔄 Encontrada instância para remover: ${activity.id} - ${activity.title}")
                    }
                    isInstance
                }
                
                println("🔄 Instâncias encontradas para remoção: ${instancesToRemove.size}")
                
                // Remover todas as instâncias
                instancesToRemove.forEach { instance ->
                    activityRepository.deleteActivity(instance.id)
                    println("🗑️ Instância recorrente removida: ${instance.id}")
                }
                
                println("🗑️ Total de instâncias recorrentes removidas: ${instancesToRemove.size}")
            } catch (e: Exception) {
                println("❌ Erro ao remover instâncias recorrentes: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Atualiza um evento no Google Calendar quando ele é editado no app
     */
    private fun updateGoogleCalendarEvent(activity: Activity) {
        viewModelScope.launch {
            try {
                val account = _uiState.value.googleSignInAccount
                if (account != null) {
                    val calendarService = googleCalendarService.getCalendarService(account)
                    
                    withContext(Dispatchers.IO) {
                        try {
                            // Criar objeto de evento do Google Calendar
                            val googleEvent = com.google.api.services.calendar.model.Event().apply {
                                summary = activity.title
                                description = activity.description
                                location = activity.location
                                
                                // Configurar data e hora
                                if (activity.isAllDay) {
                                    start = com.google.api.services.calendar.model.EventDateTime().apply {
                                        date = com.google.api.client.util.DateTime(activity.date)
                                    }
                                    end = com.google.api.services.calendar.model.EventDateTime().apply {
                                        date = com.google.api.client.util.DateTime(activity.date)
                                    }
                                } else {
                                    val startDateTime = java.time.LocalDateTime.of(
                                        java.time.LocalDate.parse(activity.date),
                                        activity.startTime ?: java.time.LocalTime.of(9, 0)
                                    )
                                    val endDateTime = java.time.LocalDateTime.of(
                                        java.time.LocalDate.parse(activity.date),
                                        activity.endTime ?: java.time.LocalTime.of(10, 0)
                                    )
                                    
                                    start = com.google.api.services.calendar.model.EventDateTime().apply {
                                        dateTime = com.google.api.client.util.DateTime(
                                            startDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                                        )
                                    }
                                    end = com.google.api.services.calendar.model.EventDateTime().apply {
                                        dateTime = com.google.api.client.util.DateTime(
                                            endDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                                        )
                                    }
                                }
                                
                                // Configurar regra de repetição se houver
                                if (!activity.recurrenceRule.isNullOrEmpty()) {
                                    recurrence = listOf(activity.recurrenceRule)
                                }
                            }
                            
                            // Atualizar o evento no Google Calendar
                            calendarService.events().update("primary", activity.id, googleEvent).execute()
                            Log.d("CalendarViewModel", "✏️ Evento atualizado no Google Calendar: ${activity.title}")
                            
                        } catch (e: Exception) {
                            Log.w("CalendarViewModel", "⚠️ Não foi possível atualizar evento no Google Calendar: ${activity.title}", e)
                        }
                    }
                } else {
                    Log.w("CalendarViewModel", "⚠️ Usuário não está logado no Google, não é possível atualizar no Google Calendar")
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "❌ Erro ao atualizar evento no Google Calendar: ${activity.title}", e)
            }
        }
    }

    /**
     * Deleta um evento do Google Calendar quando ele é removido do app
     */
    private fun deleteFromGoogleCalendar(activity: Activity) {
        viewModelScope.launch {
            try {
                val account = _uiState.value.googleSignInAccount
                if (account != null) {
                    val calendarService = googleCalendarService.getCalendarService(account)
                    
                    // Tentar deletar o evento usando o ID original do Google
                    withContext(Dispatchers.IO) {
                        try {
                            calendarService.events().delete("primary", activity.id).execute()
                            Log.d("CalendarViewModel", "🗑️ Evento deletado do Google Calendar: ${activity.title}")
                        } catch (e: Exception) {
                            Log.w("CalendarViewModel", "⚠️ Não foi possível deletar evento do Google Calendar: ${activity.title}", e)
                            
                            // Se falhar, tentar buscar o evento pelo título e data
                            try {
                                val events = calendarService.events().list("primary")
                                    .setQ(activity.title) // Buscar por título
                                    .setTimeMin(com.google.api.client.util.DateTime(Instant.parse("${activity.date}T00:00:00Z").toEpochMilli()))
                                    .setTimeMax(com.google.api.client.util.DateTime(Instant.parse("${activity.date}T23:59:59Z").toEpochMilli()))
                                    .execute()
                                
                                events.items?.forEach { event ->
                                    if (event.summary == activity.title) {
                                        calendarService.events().delete("primary", event.id).execute()
                                        Log.d("CalendarViewModel", "🗑️ Evento deletado do Google Calendar por busca: ${activity.title}")
                                    }
                                }
                            } catch (searchException: Exception) {
                                Log.e("CalendarViewModel", "❌ Falha ao buscar e deletar evento do Google Calendar: ${activity.title}", searchException)
                            }
                        }
                    }
                } else {
                    Log.w("CalendarViewModel", "⚠️ Usuário não está logado no Google, não é possível deletar do Google Calendar")
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "❌ Erro ao deletar evento do Google Calendar: ${activity.title}", e)
            }
        }
    }

    fun onDeleteActivityConfirm() {
        viewModelScope.launch {
            Log.d("CalendarViewModel", "🗑️ Função onDeleteActivityConfirm chamada")
            
            _uiState.value.activityIdToDelete?.let { activityId ->
                Log.d("CalendarViewModel", "🎯 ID da atividade a deletar: $activityId")
                
                // Buscar a atividade pelo ID ou, se for instância recorrente, buscar pela atividade base
                var activityToDelete = _uiState.value.activities.find { it.id == activityId }
                
                // Se não encontrou pelo ID e parece ser uma instância recorrente, buscar pela atividade base
                if (activityToDelete == null && activityId.contains("_")) {
                    val baseId = activityId.split("_").first()
                    activityToDelete = _uiState.value.activities.find { it.id == baseId }
                    Log.d("CalendarViewModel", "🔍 Buscando atividade base para deletar com ID: $baseId")
                }
                
                // Se ainda não encontrou, buscar por título e regra de recorrência
                if (activityToDelete == null && activityId.contains("_")) {
                    val baseId = activityId.split("_").first()
                    val allActivities = _uiState.value.activities
                    activityToDelete = allActivities.find { 
                        it.id == baseId || 
                        (it.id.contains(baseId) && it.id.contains("_"))
                    }
                    Log.d("CalendarViewModel", "🔍 Buscando por ID base ou similar: $baseId")
                }
                
                if (activityToDelete != null) {
                    Log.d("CalendarViewModel", "📋 Atividade encontrada para deletar: ${activityToDelete.title}")
                    Log.d("CalendarViewModel", "📅 Data: ${activityToDelete.date}")
                    Log.d("CalendarViewModel", "🔄 Regra de recorrência: '${activityToDelete.recurrenceRule}'")
                    Log.d("CalendarViewModel", "🔍 É recorrente? ${recurrenceService.isRecurring(activityToDelete)}")
                    Log.d("CalendarViewModel", "🔍 Tipo da regra: ${activityToDelete.recurrenceRule?.javaClass?.simpleName}")
                    Log.d("CalendarViewModel", "🔍 Tamanho da regra: ${activityToDelete.recurrenceRule?.length}")
                    
                    // ✅ Cancelar notificação antes de deletar
                    val notificationService = NotificationService(getApplication())
                    notificationService.cancelNotification(activityId)
                    
                    // Se é uma atividade recorrente, deletar todas as instâncias
                    if (recurrenceService.isRecurring(activityToDelete)) {
                        Log.d("CalendarViewModel", "🗑️ Deletando atividade recorrente: ${activityToDelete.title}")
                        Log.d("CalendarViewModel", "📅 Regra de recorrência: ${activityToDelete.recurrenceRule}")
                        Log.d("CalendarViewModel", "📅 Data da instância selecionada: ${activityToDelete.date}")
                        
                        val allActivities = _uiState.value.activities
                        val recurringActivities = allActivities.filter { 
                            it.title == activityToDelete.title && 
                            it.recurrenceRule == activityToDelete.recurrenceRule
                        }
                        
                        Log.d("CalendarViewModel", "🔄 Encontradas ${recurringActivities.size} instâncias da atividade recorrente")
                        
                        // Mover todas as instâncias para a lixeira
                        recurringActivities.forEach { activity ->
                            Log.d("CalendarViewModel", "🗑️ Movendo instância para lixeira: ${activity.title} - ${activity.date}")
                            deletedActivityRepository.addDeletedActivity(activity)
                            activityRepository.deleteActivity(activity.id)
                            
                            // Sincronizar com Google Calendar se for evento do Google
                            if (activity.isFromGoogle) {
                                Log.d("CalendarViewModel", "🌐 Sincronizando com Google Calendar: ${activity.title}")
                                deleteFromGoogleCalendar(activity)
                            }
                        }
                        
                        Log.d("CalendarViewModel", "✅ Atividade recorrente deletada com sucesso: ${activityToDelete.title}")
                        println("🗑️ Atividade recorrente deletada: ${activityToDelete.title}")
                        println("📅 Instâncias deletadas: ${recurringActivities.size}")
                    } else {
                        Log.d("CalendarViewModel", "🗑️ Deletando atividade única: ${activityToDelete.title}")
                        Log.d("CalendarViewModel", "📅 Data da atividade: ${activityToDelete.date}")
                        
                        // Mover para a lixeira
                        deletedActivityRepository.addDeletedActivity(activityToDelete)
                        
                        // Deletar da lista principal
                        activityRepository.deleteActivity(activityId)
                        
                        // Sincronizar com Google Calendar se for evento do Google
                        if (activityToDelete.isFromGoogle) {
                            Log.d("CalendarViewModel", "🌐 Sincronizando com Google Calendar: ${activityToDelete.title}")
                            deleteFromGoogleCalendar(activityToDelete)
                        }
                        
                        Log.d("CalendarViewModel", "✅ Atividade única deletada com sucesso: ${activityToDelete.title}")
                    }
                }
            }
            cancelDeleteActivity()
        }
    }

    

    fun onBackupRequest() {
        viewModelScope.launch {
            try {
                Log.d("CalendarViewModel", "🔄 Iniciando processo de backup...")
                
                // Verificar permissões antes de fazer backup
                if (!backupService.hasStoragePermission()) {
                    Log.w("CalendarViewModel", "⚠️ Sem permissão de armazenamento")
                    _uiState.update { it.copy(
                        backupMessage = "Permissão de armazenamento necessária para backup",
                        needsStoragePermission = true
                    ) }
                    return@launch
                }
                
                val result = backupService.createBackup()
                result.fold(
                    onSuccess = { backupPath ->
                        Log.d("CalendarViewModel", "✅ Backup criado com sucesso: $backupPath")
                        // Atualizar o estado para mostrar sucesso
                        _uiState.update { it.copy(
                            backupMessage = "Backup criado com sucesso: ${backupPath.substringAfterLast("/")}"
                        ) }
                        // Recarregar lista de backups
                        loadBackupFiles()
                    },
                    onFailure = { exception ->
                        Log.e("CalendarViewModel", "❌ Erro ao criar backup", exception)
                        // Atualizar o estado para mostrar erro
                        _uiState.update { it.copy(
                            backupMessage = "Erro ao criar backup: ${exception.message}"
                        ) }
                    }
                )
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "❌ Erro inesperado durante backup", e)
                _uiState.update { it.copy(
                    backupMessage = "Erro inesperado: ${e.message}"
                ) }
            }
        }
    }
    
    fun clearBackupMessage() {
        _uiState.update { it.copy(backupMessage = null, needsStoragePermission = false) }
    }
    fun onRestoreRequest() { println("ViewModel: Pedido de restauração recebido.") }

    fun openSidebar() = _uiState.update { it.copy(isSidebarOpen = true) }
    fun closeSidebar() = _uiState.update { it.copy(isSidebarOpen = false) }

    fun onNavigateToSettings(screen: String?) {
        _uiState.update { it.copy(currentSettingsScreen = screen, isSidebarOpen = false) }
    }

    fun closeSettingsScreen() {
        _uiState.update { it.copy(currentSettingsScreen = null) }
    }

    fun openCreateActivityModal(activity: Activity? = null, activityType: ActivityType = ActivityType.EVENT) {
        val template = activity ?: Activity(
            id = "new",
            title = "",
            description = null,
            date = _uiState.value.selectedDate.toString(),
            startTime = null,
            endTime = null,
            isAllDay = true,
            location = null,
            categoryColor = if (activityType == ActivityType.TASK) "#3B82F6" else "#F43F5E",
            activityType = activityType,
            recurrenceRule = null,
            showInCalendar = true // Por padrão, mostrar no calendário
        )
        _uiState.update { it.copy(activityToEdit = template, activityIdWithDeleteButtonVisible = null) }
    }

    fun closeCreateActivityModal() = _uiState.update { it.copy(activityToEdit = null) }

    fun onTaskLongPressed(activityId: String) {
        _uiState.update { it.copy(activityIdWithDeleteButtonVisible = activityId) }
    }

    fun hideDeleteButton() {
        _uiState.update { it.copy(activityIdWithDeleteButtonVisible = null) }
    }
    
    fun markActivityAsCompleted(activityId: String) {
        viewModelScope.launch {
            Log.d("CalendarViewModel", "🎯 Função markActivityAsCompleted chamada para ID: $activityId")
            
            // Buscar a atividade pelo ID ou, se for instância recorrente, buscar pela atividade base
            var activityToComplete = _uiState.value.activities.find { it.id == activityId }
            
            // Se não encontrou pelo ID e parece ser uma instância recorrente, buscar pela atividade base
            if (activityToComplete == null && activityId.contains("_")) {
                val baseId = activityId.split("_").first()
                activityToComplete = _uiState.value.activities.find { it.id == baseId }
                Log.d("CalendarViewModel", "🔍 Buscando atividade base com ID: $baseId")
            }
            
            if (activityToComplete != null) {
                Log.d("CalendarViewModel", "📋 Atividade encontrada: ${activityToComplete.title}")
                Log.d("CalendarViewModel", "📅 Data: ${activityToComplete.date}")
                Log.d("CalendarViewModel", "🔄 Regra de recorrência: '${activityToComplete.recurrenceRule}'")
                Log.d("CalendarViewModel", "🔍 É recorrente? ${recurrenceService.isRecurring(activityToComplete)}")
                Log.d("CalendarViewModel", "🔍 Tipo da regra: ${activityToComplete.recurrenceRule?.javaClass?.simpleName}")
                Log.d("CalendarViewModel", "🔍 Tamanho da regra: ${activityToComplete.recurrenceRule?.length}")
                
                // Verificar se é uma tarefa recorrente
                if (recurrenceService.isRecurring(activityToComplete)) {
                    Log.d("CalendarViewModel", "🔄 Marcando tarefa recorrente como concluída: ${activityToComplete.title}")
                    Log.d("CalendarViewModel", "📅 Regra de recorrência: ${activityToComplete.recurrenceRule}")
                    Log.d("CalendarViewModel", "📅 Data da instância: ${activityToComplete.date}")
                    
                    // Para tarefas recorrentes, marcar como concluída e salvar no repositório de finalizadas
                    val completedActivity = activityToComplete.copy(
                        isCompleted = true,
                        showInCalendar = false // Ocultar do calendário mensal
                    )
                    
                    // Salvar no repositório de atividades finalizadas
                    completedActivityRepository.addCompletedActivity(completedActivity)
                    
                    // Remover da lista principal
                    activityRepository.deleteActivity(activityToComplete.id)
                    
                    // Sincronizar com Google Calendar se for evento do Google
                    if (activityToComplete.isFromGoogle) {
                        deleteFromGoogleCalendar(activityToComplete)
                    }
                    
                    Log.d("CalendarViewModel", "✅ Tarefa recorrente marcada como concluída: ${completedActivity.title}")
                    println("✅ Tarefa recorrente marcada como concluída: ${completedActivity.title}")
                    
                    // Atualizar a UI após marcar como concluída
                    updateAllDateDependentUI()
                } else {
                    Log.d("CalendarViewModel", "✅ Marcando tarefa única como concluída: ${activityToComplete.title}")
                    
                    // Marcar como concluída e salvar no repositório de finalizadas
                    val completedActivity = activityToComplete.copy(
                        isCompleted = true,
                        showInCalendar = false // Ocultar do calendário mensal
                    )
                    
                    // Salvar no repositório de atividades finalizadas
                    completedActivityRepository.addCompletedActivity(completedActivity)
                    
                    // Remover da lista principal
                    activityRepository.deleteActivity(activityId)
                    
                    // Sincronizar com Google Calendar se for evento do Google
                    if (activityToComplete.isFromGoogle) {
                        deleteFromGoogleCalendar(activityToComplete)
                    }
                    
                    Log.d("CalendarViewModel", "✅ Tarefa única marcada como concluída: ${completedActivity.title}")
                    println("✅ Tarefa única marcada como concluída: ${completedActivity.title}")
                    
                    // Atualizar a UI após marcar como concluída
                    updateAllDateDependentUI()
                }
            } else {
                Log.w("CalendarViewModel", "⚠️ Atividade não encontrada para ID: $activityId")
            }
        }
    }

    fun requestDeleteActivity(activityId: String) {
        Log.d("CalendarViewModel", "🗑️ Função requestDeleteActivity chamada para ID: $activityId")
        _uiState.update { it.copy(activityIdToDelete = activityId, activityIdWithDeleteButtonVisible = null) }
        Log.d("CalendarViewModel", "✅ Estado atualizado com activityIdToDelete: $activityId")
    }

    fun cancelDeleteActivity() = _uiState.update { it.copy(activityIdToDelete = null) }

    fun onSaintDayClick(saint: Holiday) {
        _uiState.update { it.copy(saintInfoToShow = saint) }
    }

    fun onSaintInfoDialogDismiss() {
        _uiState.update { it.copy(saintInfoToShow = null) }
    }

    // Funções de pesquisa
    fun onSearchQueryChange(query: String) {
        println("🔍 Pesquisando por: '$query'")
        _uiState.update { it.copy(searchQuery = query) }
        
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        
        // Verificar dados disponíveis
        println("📊 Dados disponíveis para pesquisa:")
        println("  - Atividades: ${_uiState.value.activities.size}")
        println("  - Feriados nacionais: ${_uiState.value.nationalHolidays.size}")
        println("  - Dias de santos: ${_uiState.value.saintDays.size}")
        println("  - Datas comemorativas: ${_uiState.value.commemorativeDates.size}")
        
        // Realizar pesquisa
        val results = searchService.search(
            query = query,
            activities = _uiState.value.activities,
            nationalHolidays = _uiState.value.nationalHolidays,
            saintDays = _uiState.value.saintDays,
            commemorativeDates = _uiState.value.commemorativeDates
        )
        
        println("🔍 Resultados encontrados: ${results.size}")
        results.forEach { result ->
            println("  - ${result.title} (${result.type})")
        }
        
        _uiState.update { it.copy(searchResults = results) }
    }

    fun onSearchResultClick(result: SearchResult) {
        println("🎯 Resultado selecionado: ${result.title} (${result.type})")
        result.date?.let { targetDate ->
            println("📅 Navegando para data: $targetDate")
            // Navegar para o mês da data encontrada
            val targetYearMonth = java.time.YearMonth.from(targetDate)
            _uiState.update { 
                it.copy(
                    displayedYearMonth = targetYearMonth,
                    selectedDate = targetDate
                )
            }
            
            // Atualizar a UI para mostrar o mês correto
            updateAllDateDependentUI()
        }
        
        // Limpar pesquisa
        _uiState.update { 
            it.copy(
                searchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    fun clearSearch() {
        _uiState.update { 
            it.copy(
                searchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    fun onSearchIconClick() {
        println("🔍 Abrindo tela de pesquisa")
        _uiState.update { it.copy(isSearchScreenOpen = true) }
    }

        fun closeSearchScreen() {
        println("🚪 Fechando tela de pesquisa")
        _uiState.update {
            it.copy(
                isSearchScreenOpen = false,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    // --- Lixeira ---
    
    fun onTrashIconClick() {
        println("🗑️ Abrindo lixeira")
        _uiState.update { it.copy(isTrashScreenOpen = true) }
    }
    
    fun onTrashSortOrderChange(sortOrder: String) {
        println("🔄 Alterando ordem da lixeira: $sortOrder")
        _uiState.update { it.copy(trashSortOrder = sortOrder) }
    }
    
    fun forceGoogleSync() {
        val account = _uiState.value.googleSignInAccount
        if (account != null) {
            Log.d("CalendarViewModel", "🔄 Forçando sincronização manual com Google Calendar")
            fetchGoogleCalendarEvents(account, forceSync = true)
        }
    }
    
    fun manualGoogleSync() {
        val account = _uiState.value.googleSignInAccount
        if (account != null) {
            Log.d("CalendarViewModel", "🔄 Sincronização manual solicitada pelo usuário")
            fetchGoogleCalendarEvents(account, forceSync = true)
        }
    }
    
    fun onManualSync() {
        val account = _uiState.value.googleSignInAccount
        if (account != null) {
            Log.d("CalendarViewModel", "🔄 Sincronização manual progressiva solicitada")
            performProgressiveSync(account)
        }
    }
    
    private fun performProgressiveSync(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncErrorMessage = null, syncProgress = null) }
            
            try {
                val result = progressiveSyncService.syncProgressively(account) { progress ->
                    _uiState.update { it.copy(syncProgress = progress) }
                }
                
                result.fold(
                    onSuccess = { totalEvents ->
                        Log.d("CalendarViewModel", "✅ Sincronização progressiva concluída: $totalEvents eventos")
                        _uiState.update { it.copy(
                            isSyncing = false,
                            lastGoogleSyncTime = System.currentTimeMillis()
                        ) }
                        
                        // Agendar próxima sincronização automática
                        scheduleAutomaticSync()
                        
                        updateAllDateDependentUI()
                    },
                    onFailure = { exception ->
                        Log.e("CalendarViewModel", "❌ Erro na sincronização progressiva", exception)
                        _uiState.update { it.copy(
                            isSyncing = false,
                            syncErrorMessage = "Falha na sincronização: ${exception.message}"
                        ) }
                    }
                )
                
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "❌ Erro inesperado na sincronização", e)
                _uiState.update { it.copy(
                    isSyncing = false,
                    syncErrorMessage = "Erro inesperado: ${e.message}"
                ) }
            }
        }
    }
    
    /**
     * Agenda sincronização automática diária
     */
    private fun scheduleAutomaticSync() {
        val workManager = WorkManager.getInstance(getApplication())
        
        // Cancelar trabalhos anteriores
        workManager.cancelAllWorkByTag("google_calendar_sync")
        
        // Criar restrições (só sincronizar com internet)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Agendar sincronização diária
        val syncWorkRequest = PeriodicWorkRequestBuilder<GoogleCalendarSyncWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .addTag("google_calendar_sync")
            .build()
        
        workManager.enqueue(syncWorkRequest)
        Log.d("CalendarViewModel", "📅 Sincronização automática agendada para executar diariamente")
    }
    
    fun closeTrashScreen() {
        println("🚪 Fechando lixeira")
        _uiState.update { it.copy(isTrashScreenOpen = false) }
    }
    
    fun restoreDeletedActivity(deletedActivityId: String) {
        viewModelScope.launch {
            val restoredActivity = deletedActivityRepository.restoreActivity(deletedActivityId)
            if (restoredActivity != null) {
                // Restaurar a atividade
                activityRepository.addActivity(restoredActivity)
                println("✅ Atividade restaurada: ${restoredActivity.title}")
            }
        }
    }

    private suspend fun ActivityRepository.addActivity(activity: Activity) {
        saveActivity(activity)
    }

    fun removeDeletedActivity(deletedActivityId: String) {
        viewModelScope.launch {
            deletedActivityRepository.removeDeletedActivity(deletedActivityId)
            println("🗑️ Atividade removida permanentemente da lixeira")
        }
    }
    
    fun clearAllDeletedActivities() {
        viewModelScope.launch {
            deletedActivityRepository.clearAllDeletedActivities()
            println("🗑️ Lixeira esvaziada")
        }
    }
    
    // --- Tarefas Finalizadas ---
    
    fun toggleCompletedActivitiesVisibility() {
        _uiState.update { it.copy(showCompletedActivities = !it.showCompletedActivities) }
        updateAllDateDependentUI()
    }
    
    fun getCompletedActivities(): List<Activity> {
        return _uiState.value.completedActivities
    }
    
    // --- Backup ---
    
    fun onBackupIconClick() {
        println("💾 Abrindo tela de backup")
        _uiState.update { it.copy(isBackupScreenOpen = true, isSidebarOpen = false) }
    }
    
    fun closeBackupScreen() {
        println("🚪 Fechando tela de backup")
        _uiState.update { it.copy(
            isBackupScreenOpen = false,
            backupMessage = null
        ) }
    }
    
    /**
     * Verifica se o app tem permissão para sobrepor outros apps
     */
    fun hasOverlayPermission(): Boolean {
        return visibilityService.hasOverlayPermission()
    }
    
    /**
     * Solicita permissão para sobrepor outros apps
     */
    fun requestOverlayPermission(): Intent {
        return visibilityService.requestOverlayPermission()
    }
    
    /**
     * Testa o alerta de visibilidade alta
     */
    fun testHighVisibilityAlert() {
        Log.d(TAG, "🧪 Testando alerta de visibilidade alta")
        visibilityService.testHighVisibilityAlert()
    }
    
    fun loadBackupFiles() {
        viewModelScope.launch {
            try {
                val backupFiles = backupService.listBackupFiles()
                val backupInfos = mutableListOf<BackupInfo>()
                
                backupFiles.forEach { file ->
                    val info = backupService.getBackupInfo(file)
                    info.onSuccess { backupInfo ->
                        backupInfos.add(backupInfo)
                    }
                }
                
                _uiState.update { it.copy(backupFiles = backupInfos) }
                println("📁 ${backupInfos.size} arquivos de backup carregados")
            } catch (e: Exception) {
                println("❌ Erro ao carregar arquivos de backup: ${e.message}")
            }
        }
    }
    
    fun deleteBackupFile(filePath: String) {
        viewModelScope.launch {
            try {
                val file = java.io.File(filePath)
                if (file.exists()) {
                    val deleted = file.delete()
                    if (deleted) {
                        println("🗑️ Arquivo de backup deletado: $filePath")
                        // Recarregar lista de backups
                        loadBackupFiles()
                        _uiState.update { it.copy(
                            backupMessage = "Backup deletado com sucesso"
                        ) }
                    } else {
                        println("❌ Falha ao deletar arquivo de backup: $filePath")
                        _uiState.update { it.copy(
                            backupMessage = "Erro ao deletar backup"
                        ) }
                    }
                }
            } catch (e: Exception) {
                println("❌ Erro ao deletar arquivo de backup: ${e.message}")
                _uiState.update { it.copy(
                    backupMessage = "Erro ao deletar backup: ${e.message}"
                ) }
            }
        }
    }
    
    fun restoreFromBackup(filePath: String) {
        viewModelScope.launch {
            try {
                println("🔄 Iniciando restauração do backup: $filePath")
                _uiState.update { it.copy(
                    backupMessage = "Restauração em andamento..."
                ) }
                
                val backupFile = java.io.File(filePath)
                if (!backupFile.exists()) {
                    _uiState.update { it.copy(
                        backupMessage = "Arquivo de backup não encontrado"
                    ) }
                    return@launch
                }
                
                // Restaurar dados do backup
                val restoreResult = backupService.restoreFromBackup(backupFile)
                restoreResult.fold(
                    onSuccess = { result ->
                        println("✅ Backup restaurado com sucesso:")
                        println("   - Atividades: ${result.activities.size}")
                        println("   - Itens da lixeira: ${result.deletedActivities.size}")
                        
                        // Limpar dados atuais
                        clearAllCurrentData()
                        
                        // Restaurar atividades
                        result.activities.forEach { activity ->
                            activityRepository.saveActivity(activity)
                        }
                        
                        // Restaurar itens da lixeira
                        result.deletedActivities.forEach { deletedActivity ->
                            deletedActivityRepository.addDeletedActivity(deletedActivity.originalActivity)
                        }
                        
                        _uiState.update { it.copy(
                            backupMessage = "Backup restaurado com sucesso! ${result.activities.size} atividades e ${result.deletedActivities.size} itens da lixeira restaurados."
                        ) }
                        
                        // Recarregar dados da UI
                        loadData()
                        loadBackupFiles()
                    },
                    onFailure = { exception ->
                        println("❌ Erro ao restaurar backup: ${exception.message}")
                        _uiState.update { it.copy(
                            backupMessage = "Erro ao restaurar backup: ${exception.message}"
                        ) }
                    }
                )
                
            } catch (e: Exception) {
                println("❌ Erro inesperado ao restaurar backup: ${e.message}")
                _uiState.update { it.copy(
                    backupMessage = "Erro inesperado: ${e.message}"
                ) }
            }
        }
    }
    
    /**
     * Limpa todos os dados atuais antes da restauração
     */
    private suspend fun clearAllCurrentData() {
        try {
            // Limpar todas as atividades
            val currentActivities = activityRepository.activities.first()
            currentActivities.forEach { activity ->
                activityRepository.deleteActivity(activity.id)
            }
            
            // Limpar lixeira
            deletedActivityRepository.clearAllDeletedActivities()
            
            println("🗑️ Dados atuais limpos para restauração")
        } catch (e: Exception) {
            println("⚠️ Erro ao limpar dados atuais: ${e.message}")
        }
    }
}