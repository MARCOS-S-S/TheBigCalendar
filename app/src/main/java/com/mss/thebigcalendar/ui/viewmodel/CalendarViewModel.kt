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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.work.Constraints
import androidx.work.NetworkType
import com.mss.thebigcalendar.service.ProgressiveSyncService
import com.mss.thebigcalendar.worker.GoogleCalendarSyncWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepository = SettingsRepository(application)
    private val activityRepository = ActivityRepository(application)
    private val holidayRepository = HolidayRepository(application)
    private val googleAuthService = GoogleAuthService(application)
    private val googleCalendarService = GoogleCalendarService(application)
    private val progressiveSyncService = ProgressiveSyncService(application, googleCalendarService)
    private val searchService = SearchService()
    private val recurrenceService = RecurrenceService()
    private val deletedActivityRepository = DeletedActivityRepository(application)
    private val completedActivityRepository = CompletedActivityRepository(application)
    private val backupService = BackupService(application, activityRepository, deletedActivityRepository, completedActivityRepository)
    private val visibilityService = VisibilityService(application)

    // Debounce para otimizar atualizações do calendário
    private var updateJob: Job? = null
    
    // Cache para evitar recálculos desnecessários do calendário
    private var cachedCalendarDays: List<CalendarDay>? = null
    private var lastUpdateParams: String? = null
    
    // Cache para aniversários por data
    private var cachedBirthdays: Map<LocalDate, List<Activity>> = emptyMap()
    private var cachedNotes: Map<LocalDate, List<Activity>> = emptyMap()
    private var cachedTasks: Map<LocalDate, List<Activity>> = emptyMap()

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadData()
        checkForExistingSignIn()
        // Garantir que o calendário inicie no mês atual
        onGoToToday()
        
        // Registrar broadcast receiver para atualizações de notificações
        registerNotificationBroadcastReceiver()
    }

    override fun onCleared() {
        super.onCleared()
        // Limpar job pendente quando o ViewModel for destruído
        updateJob?.cancel()
        // Limpar cache
        clearCalendarCache()
        // Desregistrar broadcast receiver
        try {
            getApplication<Application>().unregisterReceiver(notificationBroadcastReceiver)
        } catch (e: Exception) {
            // Ignorar erro se o receiver não estiver registrado
        }
    }
    
    private val notificationBroadcastReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == "com.mss.thebigcalendar.ACTIVITY_COMPLETED") {
                val activityId = intent.getStringExtra("activity_id")
                if (activityId != null) {
                    Log.d("CalendarViewModel", "🔔 Recebido broadcast de atividade concluída: $activityId")
                    // Atualizar a UI
                    updateAllDateDependentUI()
                }
            }
        }
    }
    
    private fun registerNotificationBroadcastReceiver() {
        val filter = android.content.IntentFilter("com.mss.thebigcalendar.ACTIVITY_COMPLETED")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(notificationBroadcastReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            getApplication<Application>().registerReceiver(notificationBroadcastReceiver, filter)
        }
    }

    /**
     * Gera uma chave única baseada nos parâmetros que afetam o calendário
     * A data selecionada não afeta o cache do calendário, apenas a exibição dos detalhes
     */
    private fun generateCalendarCacheKey(): String {
        val state = _uiState.value
        return buildString {
            append("${state.displayedYearMonth}_")
            // Removido selectedDate - não afeta o cache do calendário
            append("${state.filterOptions.showHolidays}_")
            append("${state.filterOptions.showSaintDays}_")
            append("${state.filterOptions.showEvents}_")
            append("${state.filterOptions.showTasks}_")
            append("${state.filterOptions.showNotes}_")
            append("${state.filterOptions.showBirthdays}_")
            append("${state.showCompletedActivities}_")
            append("${state.showMoonPhases}_")
            append("${state.activities.size}_")
            append("${state.completedActivities.size}_")
            append("${state.nationalHolidays.size}_")
            append("${state.saintDays.size}")
        }
    }

    /**
     * Limpa o cache do calendário quando necessário
     * O cache deve ser limpo apenas quando:
     * - O mês/ano exibido muda
     * - As atividades/feriados mudam
     * - Os filtros mudam
     * - O ViewModel é destruído
     * NÃO deve ser limpo quando apenas a data selecionada muda
     */
    private fun clearCalendarCache() {
        cachedCalendarDays = null
        lastUpdateParams = null
    }
    
    private fun clearActivityCache() {
        cachedBirthdays = emptyMap()
        cachedNotes = emptyMap()
        cachedTasks = emptyMap()
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
        val account = googleAuthService.handleSignInResult(task)
        val loginSuccess = account != null
        _uiState.update { it.copy(
            googleSignInAccount = account,
            loginMessage = if(loginSuccess) "Login bem-sucedido!" else "Falha no login. Verifique os logs para detalhes."
        ) }
        if (loginSuccess) {
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
        
        // Verificar se é um evento recorrente anual (típico de aniversários)
        val isYearlyRecurring = event.recurrence?.any { rule ->
            rule.contains("FREQ=YEARLY") || rule.contains("RRULE:FREQ=YEARLY") ||
            rule.contains("INTERVAL=1") && rule.contains("FREQ=YEARLY")
        } == true
        
        // Verificar se é um evento de dia inteiro (aniversários geralmente são)
        val isAllDayEvent = isAllDay
        
        // Verificar se tem configurações específicas de aniversário do Google
        val hasBirthdaySettings = event.gadget?.preferences?.any { (key, value) ->
            key == "googCalEventType" && value == "birthday"
        } == true
        
        // Verificar se vem de um calendário específico de aniversários
        val isFromBirthdayCalendar = event.organizer?.email?.contains("birthday") == true ||
                                   event.creator?.email?.contains("birthday") == true
        
        // Verificar se tem padrões específicos de aniversário no título
        val hasBirthdayPatterns = title.matches(Regex(".*\\b\\d{1,2}/\\d{1,2}\\b.*")) || // Padrão DD/MM
                                 title.matches(Regex(".*\\b\\d{1,2}-\\d{1,2}\\b.*")) || // Padrão DD-MM
                                 title.matches(Regex(".*\\b\\d{1,2}\\.\\d{1,2}\\b.*"))   // Padrão DD.MM
        
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
                    _uiState.update { it.copy(isSyncing = false) }
                    return@launch
                }
                
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
                        com.google.api.services.calendar.model.Events()
                    }
                }
                
                // Buscar eventos de aniversários específicos
                val birthdayCalendarEvents = withContext(Dispatchers.IO) {
                    try {
                        calendarService.events().list("birthdays").execute()
                    } catch (e: Exception) {
                        // Se não conseguir acessar o calendário de aniversários, usar lista vazia
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
                        utcDate
                    } else {
                        // Para eventos com horário, usar fuso horário local
                        val localDate = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate()
                        localDate
                    }
                    
                    val startTime = if (event.start?.dateTime != null) Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalTime() else null
                    val endTime = if (end != null && event.end?.dateTime != null) Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).toLocalTime() else null

                    // Detectar se é um aniversário baseado em características específicas
                    val isBirthday = detectBirthdayEvent(event)
                    
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
                
                // 4. Fazer merge dos eventos (manter existentes + adicionar novos)
                val currentActivities = activityRepository.activities.first()
                activities.forEach { newActivity ->
                    // Verificar se já existe uma atividade com o mesmo ID
                    val existingActivity = currentActivities.find { it.id == newActivity.id }
                    if (existingActivity != null) {
                        // Se já existe, preservar a cor personalizada do usuário
                        val updatedActivity = newActivity.copy(categoryColor = existingActivity.categoryColor)
                        activityRepository.saveActivity(updatedActivity)
                    } else {
                        // Se não existe, adicionar nova com cor padrão
                        activityRepository.saveActivity(newActivity)
                    }
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
            settingsRepository.welcomeName.collect { welcomeName ->
                _uiState.update { it.copy(welcomeName = welcomeName) }
            }
        }
        viewModelScope.launch {
            settingsRepository.filterOptions.collect { filterOptions ->
                _uiState.update { it.copy(filterOptions = filterOptions) }
                updateAllDateDependentUI()
            }
        }
        viewModelScope.launch {
            settingsRepository.showMoonPhases.collect { showMoonPhases ->
                _uiState.update { it.copy(showMoonPhases = showMoonPhases) }
            }
        }
        viewModelScope.launch {
            // Observar o estado de login do Google e o nome de boas-vindas
            _uiState.collect { uiState ->
                val googleAccount = uiState.googleSignInAccount
                val currentWelcomeName = uiState.welcomeName

                if (googleAccount != null && currentWelcomeName == "Usuário") {
                    val firstName = googleAccount.displayName?.split(" ")?.firstOrNull()
                    if (!firstName.isNullOrBlank()) {
                        settingsRepository.saveWelcomeName(firstName)
                    }
                }
            }
        }
    }


    
    private fun loadData() {
        // Carregar apenas as atividades do mês atual
        loadActivitiesForCurrentMonth()
        
        viewModelScope.launch {
            deletedActivityRepository.deletedActivities.collect { deletedActivities ->
                _uiState.update { it.copy(deletedActivities = deletedActivities) }
            }
        }
        
        viewModelScope.launch {
            completedActivityRepository.completedActivities.collect { completedActivities ->
                _uiState.update { it.copy(completedActivities = completedActivities) }
                // Limpar cache quando as atividades completadas mudam
                clearCalendarCache()
                // Usar debounce para evitar múltiplas atualizações rápidas
                updateAllDateDependentUI()
            }
        }
        
        loadInitialHolidaysAndSaints()
    }

    /**
     * Carrega apenas as atividades do mês atual
     * Otimização para reduzir uso de memória e melhorar performance
     */
    private fun loadActivitiesForCurrentMonth() {
        val currentMonth = _uiState.value.displayedYearMonth
        
        viewModelScope.launch {
            activityRepository.getActivitiesForMonth(currentMonth).collect { activities ->
                _uiState.update { it.copy(activities = activities) }
                
                // Limpar cache quando as atividades mudam
                clearCalendarCache()
                clearActivityCache()
                // Usar debounce para evitar múltiplas atualizações rápidas
                updateAllDateDependentUI()
            }
        }
    }

    /**
     * Atualiza as atividades quando o mês muda
     */
    private fun updateActivitiesForNewMonth(newMonth: YearMonth) {
        viewModelScope.launch {
            activityRepository.getActivitiesForMonth(newMonth).collect { activities ->
                _uiState.update { it.copy(activities = activities) }
                
                // Limpar cache quando as atividades mudam
                clearCalendarCache()
                clearActivityCache()
                // Atualizar o calendário
                updateAllDateDependentUI()
            }
        }
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
            // Limpar cache quando os feriados mudam
            clearCalendarCache()
            // Não chamar updateCalendarDays() aqui para evitar loop infinito
            // updateCalendarDays() será chamado por updateAllDateDependentUI()
        }
    }
    


    private fun updateCalendarDays() {
        val state = _uiState.value
        
        // Verificar se podemos usar o cache
        val currentCacheKey = generateCalendarCacheKey()
        if (cachedCalendarDays != null && lastUpdateParams == currentCacheKey) {
            // Usar cache - não recalculamos
            return
        }
        
        // Cache miss - precisamos recalcular

        val firstDayOfMonth = state.displayedYearMonth.atDay(1)
        val daysFromPrevMonthOffset = (firstDayOfMonth.dayOfWeek.value % 7)
        val gridStartDate = firstDayOfMonth.minusDays(daysFromPrevMonthOffset.toLong())

        val newCalendarDays = List(42) { i ->
            val date = gridStartDate.plusDays(i.toLong())
            
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
                                
                                // Para atividades recorrentes, verificar se esta data específica foi excluída
                                val isExcluded = if (activity.recurrenceRule?.isNotEmpty() == true && activity.recurrenceRule != "CUSTOM") {
                                    activity.excludedDates.contains(date.toString())
                                } else {
                                    false
                                }
                                
                                if (shouldShowInCalendar && !isExcluded) {
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
                        // Erro ao processar atividade - continuar com outras atividades
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
                            }
                        } catch (e: Exception) {
                            // Erro ao processar tarefa finalizada - continuar com outras
                        }
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
        
        // Salvar no cache
        cachedCalendarDays = newCalendarDays
        lastUpdateParams = currentCacheKey
        
        _uiState.update { it.copy(calendarDays = newCalendarDays) }
    }

    private fun updateTasksForSelectedDate() {
        val state = _uiState.value
        
        // Separar aniversários das outras atividades usando cache
        val birthdays = if (state.filterOptions.showBirthdays) {
            cachedBirthdays[state.selectedDate] ?: run {
                val filtered = state.activities.filter { activity ->
                    try {
                        if (activity.activityType == ActivityType.BIRTHDAY) {
                            val activityDate = LocalDate.parse(activity.date)
                            // Para aniversários, verificar se é o mesmo dia e mês (ignorando o ano)
                            activityDate.month == state.selectedDate.month && activityDate.dayOfMonth == state.selectedDate.dayOfMonth
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false
                    }
                }.sortedBy { it.title }
                
                // Atualizar cache
                cachedBirthdays = cachedBirthdays + (state.selectedDate to filtered)
                filtered
            }
        } else {
            emptyList()
        }
        
        // Separar notas das outras atividades usando cache
        val notes = if (state.filterOptions.showNotes) {
            cachedNotes[state.selectedDate] ?: run {
                val filtered = state.activities.filter { activity ->
                    try {
                        if (activity.activityType == ActivityType.NOTE) {
                            val activityDate = LocalDate.parse(activity.date)
                            activityDate.isEqual(state.selectedDate)
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false
                    }
                }.sortedBy { it.title }
                
                // Atualizar cache
                cachedNotes = cachedNotes + (state.selectedDate to filtered)
                filtered
            }
        } else {
            emptyList()
        }
        
        // Filtrar outras atividades (excluindo aniversários e notas)
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
                        // Para atividades recorrentes, verificar se esta data específica foi excluída
                        val isExcluded = if (activity.recurrenceRule?.isNotEmpty() == true && activity.recurrenceRule != "CUSTOM") {
                            activity.excludedDates.contains(state.selectedDate.toString())
                        } else {
                            false
                        }
                        
                        if (!isExcluded) {
                            allTasksForSelectedDate.add(activity)
                        }
                    }
                    
                    // Se a atividade é repetitiva, calcular se deve aparecer neste dia
                    if (activity.recurrenceRule?.isNotEmpty() == true && 
                        activity.recurrenceRule != "CUSTOM") {
                        
                        val recurringInstances = calculateRecurringInstancesForDate(activity, state.selectedDate)
                        allTasksForSelectedDate.addAll(recurringInstances)
                        
                    }
                }
            } catch (e: Exception) {
                // Erro ao processar atividade - continuar com outras
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
                    }
                } catch (e: Exception) {
                    // Erro ao processar tarefa finalizada - continuar com outras
                }
            }
        }
        
        val otherTasks = allTasksForSelectedDate.sortedWith(
            compareByDescending<Activity> { it.categoryColor?.toIntOrNull() ?: 0 }
            .thenBy { it.startTime ?: LocalTime.MIN }
        )
        
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
        // Cancelar atualização anterior se ainda estiver pendente
        updateJob?.cancel()
        
        // Agendar nova atualização com debounce de 100ms
        updateJob = viewModelScope.launch {
            delay(100) // Debounce de 100ms
            updateCalendarDays()
            updateTasksForSelectedDate()
            updateHolidaysForSelectedDate()
            updateSaintDaysForSelectedDate()
        }
    }

    /**
     * Atualiza apenas a propriedade isSelected dos dias do calendário
     * sem recriar todo o cache - otimização para mudança de data selecionada
     * 
     * Esta otimização permite que o dia selecionado seja marcado visualmente
     * sem invalidar o cache do calendário, mantendo a performance
     */
    private fun updateSelectedDateInCalendar() {
        val state = _uiState.value
        val updatedCalendarDays = state.calendarDays.map { day ->
            day.copy(isSelected = day.date.isEqual(state.selectedDate))
        }
        _uiState.update { it.copy(calendarDays = updatedCalendarDays) }
    }

    fun onPreviousMonth() {
        val newMonth = _uiState.value.displayedYearMonth.minusMonths(1)
        _uiState.update { it.copy(displayedYearMonth = newMonth) }
        // Carregar atividades do novo mês
        updateActivitiesForNewMonth(newMonth)
    }

    fun onNextMonth() {
        val newMonth = _uiState.value.displayedYearMonth.plusMonths(1)
        _uiState.update { it.copy(displayedYearMonth = newMonth) }
        // Carregar atividades do novo mês
        updateActivitiesForNewMonth(newMonth)
    }

    fun onPreviousYear() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.minusYears(1)) }
    }

    fun onNextYear() {
        _uiState.update { it.copy(displayedYearMonth = it.displayedYearMonth.plusYears(1)) }
    }

    fun onGoToToday() {
        val currentState = _uiState.value
        val today = LocalDate.now()
        val currentMonth = YearMonth.now()
        val monthChanged = currentState.displayedYearMonth != currentMonth
        
        _uiState.update {
            it.copy(
                displayedYearMonth = currentMonth,
                selectedDate = today
            )
        }
        
        // Se o mês mudou, carregar atividades do novo mês
        if (monthChanged) {
            updateActivitiesForNewMonth(currentMonth)
        } else {
            // Se apenas o dia mudou, apenas atualizar a data selecionada
            updateSelectedDateInCalendar()
        }
    }

    fun onDateSelected(date: LocalDate) {
        val state = _uiState.value
        val shouldOpenModal = state.selectedDate.isEqual(date) && date.month == state.displayedYearMonth.month
        val monthChanged = state.displayedYearMonth.month != date.month || state.displayedYearMonth.year != date.year

        _uiState.update {
            it.copy(
                selectedDate = date,
                displayedYearMonth = if (monthChanged) {
                    YearMonth.from(date)
                } else {
                    it.displayedYearMonth
                },
                activityIdWithDeleteButtonVisible = null // Esconde o botão ao selecionar nova data
            )
        }
        
        if (monthChanged) {
            // Se o mês mudou, carregar atividades do novo mês
            val newMonth = YearMonth.from(date)
            updateActivitiesForNewMonth(newMonth)
        } else {
            // Se apenas a data selecionada mudou, atualizar apenas a marcação visual
            updateSelectedDateInCalendar()
            updateTasksForSelectedDate()
            updateHolidaysForSelectedDate()
            updateSaintDaysForSelectedDate()
        }

        if (shouldOpenModal) {
            openCreateActivityModal()
        }
    }

    fun onYearlyMonthClicked(yearMonth: YearMonth) {
        val currentState = _uiState.value
        val monthChanged = currentState.displayedYearMonth != yearMonth
        
        _uiState.update {
            it.copy(
                displayedYearMonth = yearMonth,
                selectedDate = yearMonth.atDay(1),
                viewMode = ViewMode.MONTHLY,
                isSidebarOpen = false
            )
        }
        
        // Se o mês mudou, carregar atividades do novo mês
        if (monthChanged) {
            updateActivitiesForNewMonth(yearMonth)
        }
    }

    fun onViewModeChange(newMode: ViewMode) {
        _uiState.update { it.copy(viewMode = newMode, isSidebarOpen = false) }
    }

    fun onFilterChange(key: String, value: Boolean) {
        when (key) {
            "showCompletedActivities" -> {
                // Atualizar o estado imediatamente
                _uiState.update { it.copy(showCompletedActivities = value) }
                
                // Atualizar a UI
                updateAllDateDependentUI()
            }
            "showMoonPhases" -> {
                // Atualizar o estado imediatamente
                _uiState.update { it.copy(showMoonPhases = value) }
                
                // Salvar a configuração
                viewModelScope.launch {
                    settingsRepository.saveShowMoonPhases(value)
                }
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



    fun onSaveActivity(activityData: Activity, syncWithGoogle: Boolean = false) {
        viewModelScope.launch {
            // Verificar se é uma edição de instância recorrente
            val isEditingRecurringInstance = activityData.id.contains("_") && 
                                           activityData.id != "new" && 
                                           !activityData.id.isBlank()

            
            if (isEditingRecurringInstance) {
                // É uma edição de instância recorrente - aplicar mudanças à atividade base
                val baseId = activityData.id.split("_").first()
                val baseActivity = _uiState.value.activities.find { it.id == baseId }
                
                if (baseActivity != null) {
                    // Aplicar mudanças à atividade base, mantendo o ID original
                    val updatedBaseActivity = baseActivity.copy(
                        title = activityData.title,
                        description = activityData.description,
                        startTime = activityData.startTime,
                        endTime = activityData.endTime,
                        isAllDay = activityData.isAllDay,
                        location = activityData.location,
                        categoryColor = activityData.categoryColor,
                        notificationSettings = activityData.notificationSettings,
                        visibility = activityData.visibility,
                        showInCalendar = activityData.showInCalendar
                        // NÃO alterar recurrenceRule para manter a recorrência
                    )
                    
                    // Salvar a atividade base atualizada
                    activityRepository.saveActivity(updatedBaseActivity)
                    
                    // Agendar notificação se configurada - usar a instância específica com data correta
                    if (activityData.notificationSettings.isEnabled &&
                        activityData.notificationSettings.notificationType != com.mss.thebigcalendar.data.model.NotificationType.NONE) {
                        
                        // Extrair a data da instância específica do ID
                        val instanceDate = activityData.id.split("_").getOrNull(1)
                        if (instanceDate != null) {
                            // Criar uma cópia da atividade com a data correta da instância
                            val instanceActivity = activityData.copy(date = instanceDate)
                            val notificationService = NotificationService(getApplication())
                            notificationService.scheduleNotification(instanceActivity)
                        }
                    }
                    
                    // Sincronizar com Google Calendar se for evento do Google
                    if (updatedBaseActivity.isFromGoogle) {
                        updateGoogleCalendarEvent(updatedBaseActivity)
                    }
                    
                    closeCreateActivityModal()
                    return@launch
                }
            }
            
            // Lógica original para atividades não recorrentes ou novas
            var activityToSave = if (activityData.id == "new" || activityData.id.isBlank()) {
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

            // Se for nova e o usuário optou por sincronizar com Google, tentar inserir primeiro no Google
            val isNewActivity = activityData.id == "new" || activityData.id.isBlank()
            if (isNewActivity && syncWithGoogle) {
                val account = _uiState.value.googleSignInAccount
                if (account != null) {
                    try {
                        val googleEventId = insertGoogleCalendarEvent(activityToSave, account)
                        if (!googleEventId.isNullOrBlank()) {
                            // Usar o ID do Google e marcar como vindo do Google para habilitar atualizações/exclusões
                            activityToSave = activityToSave.copy(id = googleEventId, isFromGoogle = true)
                        }
                    } catch (e: Exception) {
                        Log.e("CalendarViewModel", "❌ Falha ao inserir no Google Calendar", e)
                    }
                }
            }

            // Salvar a atividade principal (com possível ID do Google)
            activityRepository.saveActivity(activityToSave)

            // ✅ Agendar notificação se configurada
            Log.d("CalendarViewModel", "🔔 Verificando configurações de notificação para: ${activityToSave.title}")
            Log.d("CalendarViewModel", "🔔 Notificação habilitada: ${activityToSave.notificationSettings.isEnabled}")
            Log.d("CalendarViewModel", "🔔 Tipo de notificação: ${activityToSave.notificationSettings.notificationType}")
            
            if (activityToSave.notificationSettings.isEnabled &&
                activityToSave.notificationSettings.notificationType != com.mss.thebigcalendar.data.model.NotificationType.NONE) {

                Log.d("CalendarViewModel", "🔔 Agendando notificação para atividade: ${activityToSave.title}")
                
                // Para atividades repetitivas, agendar notificação para a data selecionada
                val activityForNotification = if (activityToSave.recurrenceRule?.isNotEmpty() == true) {
                    // Se é uma atividade repetitiva, usar a data selecionada no calendário
                    activityToSave.copy(date = _uiState.value.selectedDate.toString())
                } else {
                    // Se não é repetitiva, usar a data original
                    activityToSave
                }

                Log.d("CalendarViewModel", "🔔 Data da notificação: ${activityForNotification.date}")
                Log.d("CalendarViewModel", "🔔 Horário da atividade: ${activityForNotification.startTime}")
                
                val notificationService = NotificationService(getApplication())
                notificationService.scheduleNotification(activityForNotification)
                
                Log.d("CalendarViewModel", "🔔 Notificação agendada com sucesso!")
            } else {
                Log.d("CalendarViewModel", "🔔 Notificação não agendada - configurações desabilitadas")
            }

            // NOTA: Não geramos mais instâncias repetitivas automaticamente
            // As tarefas repetitivas serão calculadas dinamicamente quando o usuário navegar pelos meses
            if (activityToSave.recurrenceRule?.isNotEmpty() == true && activityToSave.recurrenceRule != "CUSTOM") {
            }

            // Sincronizar com Google Calendar se for edição de evento existente
            if (isEditingGoogleEvent) {
                updateGoogleCalendarEvent(activityToSave)
            }

            // Recarregar atividades do mês atual após salvar
            loadActivitiesForCurrentMonth()
            
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
            val targetDateString = targetDate.toString()
            
            // Verificar se esta data específica foi excluída
            if (baseActivity.excludedDates.contains(targetDateString)) {
                return instances
            }
            
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
            // Erro ao calcular instâncias repetitivas - retornar lista vazia
        }
        
        return instances
    }

    /**
     * Remove todas as instâncias recorrentes de uma atividade
     */
    private fun removeRecurringInstances(baseActivity: Activity) {
        viewModelScope.launch {
            try {
                // Buscar todas as atividades que são instâncias desta atividade base
                val allActivities = _uiState.value.activities
                
                val instancesToRemove = allActivities.filter { activity ->
                    // Verificar se é uma instância recorrente (ID contém o ID base + data)
                    val isInstance = activity.id.startsWith("${baseActivity.id}_") && 
                                   activity.id != baseActivity.id
                    isInstance
                }
                
                // Remover todas as instâncias
                instancesToRemove.forEach { instance ->
                    activityRepository.deleteActivity(instance.id)
                }
                
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
     * Insere um novo evento no Google Calendar e retorna o ID do evento criado
     */
    private suspend fun insertGoogleCalendarEvent(activity: Activity, account: GoogleSignInAccount): String? {
        return withContext(Dispatchers.IO) {
            try {
                val calendarService = googleCalendarService.getCalendarService(account)

                val googleEvent = com.google.api.services.calendar.model.Event().apply {
                    summary = activity.title
                    description = activity.description
                    location = activity.location

                    // Datas/horários
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
                            activity.endTime ?: (activity.startTime?.plusHours(1) ?: java.time.LocalTime.of(10, 0))
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

                    // Regra de repetição: para aniversários, padronizar YEARLY se não houver
                    when (activity.activityType) {
                        com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY -> {
                            val rule = activity.recurrenceRule?.takeIf { it.isNotEmpty() } ?: "RRULE:FREQ=YEARLY"
                            recurrence = listOf(rule)
                        }
                        else -> {
                            if (!activity.recurrenceRule.isNullOrEmpty()) {
                                recurrence = listOf(activity.recurrenceRule)
                            }
                        }
                    }
                }

                val created = calendarService.events().insert("primary", googleEvent).execute()
                created?.id
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "❌ Erro ao inserir evento no Google Calendar", e)
                null
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
            _uiState.value.activityIdToDelete?.let { activityId ->
                // Buscar a atividade pelo ID ou, se for instância recorrente, buscar pela atividade base
                var activityToDelete = _uiState.value.activities.find { it.id == activityId }
                
                // Se não encontrou pelo ID e parece ser uma instância recorrente, buscar pela atividade base
                if (activityToDelete == null && activityId.contains("_")) {
                    val baseId = activityId.split("_").first()
                    activityToDelete = _uiState.value.activities.find { it.id == baseId }
                }
                
                // Se ainda não encontrou, buscar por título e regra de recorrência
                if (activityToDelete == null && activityId.contains("_")) {
                    val baseId = activityId.split("_").first()
                    val allActivities = _uiState.value.activities
                    activityToDelete = allActivities.find { 
                        it.id == baseId || 
                        (it.id.contains(baseId) && it.id.contains("_"))
                    }
                }
                
                if (activityToDelete != null) {
                    // ✅ Cancelar notificação antes de deletar
                    val notificationService = NotificationService(getApplication())
                    notificationService.cancelNotification(activityId)
                    
                    // Se é uma atividade recorrente, deletar todas as instâncias
                    if (recurrenceService.isRecurring(activityToDelete)) {
                        val allActivities = _uiState.value.activities
                        val recurringActivities = allActivities.filter { 
                            it.title == activityToDelete.title && 
                            it.recurrenceRule == activityToDelete.recurrenceRule
                        }
                        
                        // Mover todas as instâncias para a lixeira
                        recurringActivities.forEach { activity ->
                            deletedActivityRepository.addDeletedActivity(activity)
                            activityRepository.deleteActivity(activity.id)
                            
                            // Sincronizar com Google Calendar se for evento do Google
                            if (activity.isFromGoogle) {
                                deleteFromGoogleCalendar(activity)
                            }
                        }
                        
                    } else {
                        // Mover para a lixeira
                        deletedActivityRepository.addDeletedActivity(activityToDelete)
                        
                        // Deletar da lista principal
                        activityRepository.deleteActivity(activityId)
                        
                        // Sincronizar com Google Calendar se for evento do Google
                        if (activityToDelete.isFromGoogle) {
                            deleteFromGoogleCalendar(activityToDelete)
                        }
                        
                    }
                }
            }
            cancelDeleteActivity()
        }
    }

    

    fun onBackupRequest() {
        viewModelScope.launch {
            try {
                // Verificar permissões antes de fazer backup
                if (!backupService.hasStoragePermission()) {
                    _uiState.update { it.copy(
                        backupMessage = "Permissão de armazenamento necessária para backup",
                        needsStoragePermission = true
                    ) }
                    return@launch
                }
                
                val result = backupService.createBackup()
                result.fold(
                    onSuccess = { backupPath ->
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
        _uiState.update { it.copy(isSettingsScreenOpen = true, isSidebarOpen = false) }
    }

    fun closeSettingsScreen() {
        _uiState.update { it.copy(isSettingsScreenOpen = false) }
    }

    fun onWelcomeNameChange(newName: String) {
        viewModelScope.launch {
            settingsRepository.saveWelcomeName(newName)
        }
    }

    fun openCreateActivityModal(activity: Activity? = null, activityType: ActivityType = ActivityType.EVENT) {
        val template = if (activity != null) {
            // Se for uma instância recorrente, carregar os dados da atividade base
            if (activity.id.contains("_") && activity.id != "new") {
                val baseId = activity.id.split("_").first()
                val baseActivity = _uiState.value.activities.find { it.id == baseId }
                baseActivity ?: activity
            } else {
                activity
            }
        } else {
            Activity(
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
        }
        _uiState.update { it.copy(activityToEdit = template, activityIdWithDeleteButtonVisible = null) }
    }

    fun closeCreateActivityModal() = _uiState.update { it.copy(activityToEdit = null) }

    fun onTaskLongPressed(activityId: String) {
        _uiState.update { currentState ->
            if (currentState.activityIdWithDeleteButtonVisible == activityId) {
                // If the same item is clicked again, hide the buttons
                currentState.copy(activityIdWithDeleteButtonVisible = null)
            } else {
                // Otherwise, show buttons for the new item
                currentState.copy(activityIdWithDeleteButtonVisible = activityId)
            }
        }
    }

    fun hideDeleteButton() {
        _uiState.update { it.copy(activityIdWithDeleteButtonVisible = null) }
    }
    
    fun markActivityAsCompleted(activityId: String) {
        viewModelScope.launch {
            markActivityAsCompletedInternal(activityId)
        }
    }
    
    /**
     * Função interna para marcar atividade como concluída (pode ser chamada por notificações)
     */
    fun markActivityAsCompletedInternal(activityId: String) {
        viewModelScope.launch {
            
            // Verificar se é uma instância recorrente (ID contém data)
            val isRecurringInstance = activityId.contains("_") && activityId.split("_").size == 2
            
            if (isRecurringInstance) {
                // Tratar instância recorrente específica
                val parts = activityId.split("_")
                val baseId = parts[0]
                val instanceDate = parts[1]

                
                // Buscar a atividade base
                val baseActivity = _uiState.value.activities.find { it.id == baseId }
                
                if (baseActivity != null && recurrenceService.isRecurring(baseActivity)) {

                    
                    // Criar instância específica para salvar como concluída
                    val instanceToComplete = baseActivity.copy(
                        id = activityId,
                        date = instanceDate,
                        isCompleted = true,
                        showInCalendar = false
                    )
                    
                    // Salvar instância específica como concluída
                    completedActivityRepository.addCompletedActivity(instanceToComplete)
                    
                    // Adicionar data à lista de exclusões da atividade base
                    val updatedExcludedDates = baseActivity.excludedDates + instanceDate
                    val updatedBaseActivity = baseActivity.copy(excludedDates = updatedExcludedDates)
                    
                    // Atualizar a atividade base com a nova lista de exclusões
                    activityRepository.saveActivity(updatedBaseActivity)

                    
                    // Atualizar a UI
                    updateAllDateDependentUI()
                    
                }
            } else {
                // Tratar atividade única ou atividade base
                val activityToComplete = _uiState.value.activities.find { it.id == activityId }
                
                if (activityToComplete != null) {
                    // Verificar se é uma atividade recorrente
                    if (recurrenceService.isRecurring(activityToComplete)) {
                        // Para atividades recorrentes (primeira instância), sempre tratar como instância específica
                        val activityDate = activityToComplete.date

                        
                        // Criar instância específica para salvar como concluída
                        val instanceToComplete = activityToComplete.copy(
                            id = activityId,
                            date = activityDate,
                            isCompleted = true,
                            showInCalendar = false
                        )
                        
                        // Salvar instância específica como concluída
                        completedActivityRepository.addCompletedActivity(instanceToComplete)
                        
                        // Adicionar data à lista de exclusões da atividade base
                        val updatedExcludedDates = activityToComplete.excludedDates + activityDate
                        val updatedBaseActivity = activityToComplete.copy(excludedDates = updatedExcludedDates)
                        
                        // Atualizar a atividade base com a nova lista de exclusões
                        activityRepository.saveActivity(updatedBaseActivity)

                        
                        // Atualizar a UI
                        updateAllDateDependentUI()
                        
                    } else {
                        // Tratar atividade única (não recorrente)
                        
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

                        
                        // Atualizar a UI após marcar como concluída
                        updateAllDateDependentUI()
                    }
                }
            }
        }
    }

    fun requestDeleteActivity(activityId: String) {
        _uiState.update { it.copy(activityIdToDelete = activityId, activityIdWithDeleteButtonVisible = null) }
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

    fun onChartIconClick() {
        println("📊 Abrindo tela de gráfico")
        _uiState.update { it.copy(isChartScreenOpen = true) }
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

    fun closeChartScreen() {
        println("🚪 Fechando tela de gráfico")
        _uiState.update { it.copy(isChartScreenOpen = false) }
    }

    // --- Tela de Notas ---
    fun onNotesClick() {
        println("📝 Abrindo tela de notas")
        _uiState.update { it.copy(isNotesScreenOpen = true, isSidebarOpen = false) }
    }

    fun closeNotesScreen() {
        println("🚪 Fechando tela de notas")
        _uiState.update { it.copy(isNotesScreenOpen = false) }
    }

    // --- Tarefas Concluídas ---
    fun onCompletedTasksClick() {
        println("📋 Abrindo tela de tarefas concluídas")
        _uiState.update { it.copy(isCompletedTasksScreenOpen = true) }
    }

    fun closeCompletedTasksScreen() {
        println("🚪 Fechando tela de tarefas concluídas")
        _uiState.update { it.copy(isCompletedTasksScreenOpen = false) }
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
            fetchGoogleCalendarEvents(account, forceSync = true)
        }
    }
    
    fun manualGoogleSync() {
        val account = _uiState.value.googleSignInAccount
        if (account != null) {
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
    
    fun toggleMoonPhasesVisibility() {
        _uiState.update { it.copy(showMoonPhases = !it.showMoonPhases) }
    }
    
    fun getCompletedActivities(): List<Activity> {
        return _uiState.value.completedActivities
    }
    
    /**
     * Calcula os dados para o gráfico de barras dos últimos 7 dias
     */
    fun getLast7DaysCompletedTasksData(): List<com.mss.thebigcalendar.ui.components.BarChartData> {
        val today = LocalDate.now()
        val completedActivities = _uiState.value.completedActivities
        
        // Criar lista dos últimos 7 dias
        val last7Days = (0..6).map { daysAgo ->
            today.minusDays(daysAgo.toLong())
        }.reversed() // Do mais antigo para o mais recente
        
        return last7Days.map { date ->
            val dateString = date.toString()
            val count = completedActivities.count { activity ->
                activity.date == dateString
            }
            
            // Formatar o label do dia (ex: "Seg", "Ter", etc.)
            val dayLabel = when (date.dayOfWeek.value) {
                1 -> "Seg"
                2 -> "Ter" 
                3 -> "Qua"
                4 -> "Qui"
                5 -> "Sex"
                6 -> "Sáb"
                7 -> "Dom"
                else -> date.dayOfWeek.name.take(3)
            }
            
            com.mss.thebigcalendar.ui.components.BarChartData(
                label = dayLabel,
                value = count,
                color = when (count) {
                    0 -> Color(0xFF79747E) // outline color
                    in 1..2 -> Color(0xFF6750A4) // primary color
                    in 3..5 -> Color(0xFF625B71) // secondary color
                    else -> Color(0xFF7D5260) // tertiary color
                }
            )
        }
    }
    
    /**
     * Calcula os dados para o gráfico de barras do último ano (12 meses)
     */
    fun getLastYearCompletedTasksData(): List<com.mss.thebigcalendar.ui.components.BarChartData> {
        val today = LocalDate.now()
        val completedActivities = _uiState.value.completedActivities
        
        // Criar lista dos últimos 12 meses
        val last12Months = (0..11).map { monthsAgo ->
            today.minusMonths(monthsAgo.toLong())
        }.reversed() // Do mais antigo para o mais recente
        
        return last12Months.map { date ->
            val yearMonth = "${date.year}-${date.monthValue.toString().padStart(2, '0')}"
            val count = completedActivities.count { activity ->
                activity.date.startsWith(yearMonth)
            }
            
            // Formatar o label do mês (ex: "Jan", "Fev", etc.)
            val monthLabel = when (date.monthValue) {
                1 -> "Jan"
                2 -> "Fev"
                3 -> "Mar"
                4 -> "Abr"
                5 -> "Mai"
                6 -> "Jun"
                7 -> "Jul"
                8 -> "Ago"
                9 -> "Set"
                10 -> "Out"
                11 -> "Nov"
                12 -> "Dez"
                else -> date.month.name.take(3)
            }
            
            com.mss.thebigcalendar.ui.components.BarChartData(
                label = monthLabel,
                value = count,
                color = when (count) {
                    0 -> Color(0xFF79747E) // outline color
                    in 1..5 -> Color(0xFF6750A4) // primary color
                    in 6..15 -> Color(0xFF625B71) // secondary color
                    in 16..30 -> Color(0xFF7D5260) // tertiary color
                    else -> Color(0xFF4A148C) // high productivity color
                }
            )
        }
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
                        println("   - Atividades concluídas: ${result.completedActivities.size}")
                        
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
                        
                        // Restaurar atividades concluídas
                        result.completedActivities.forEach { activity ->
                            completedActivityRepository.addCompletedActivity(activity)
                        }
                        
                        _uiState.update { it.copy(
                            backupMessage = "Backup restaurado com sucesso! ${result.activities.size} atividades, ${result.deletedActivities.size} itens da lixeira e ${result.completedActivities.size} atividades concluídas restaurados."
                        ) }
                        
                        // Recarregar dados da UI
                        loadData()
                        loadBackupFiles()

                        // Reagendar notificações
                        val notificationService = NotificationService(getApplication())
                        result.activities.forEach { activity ->
                            notificationService.scheduleNotification(activity)
                        }
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