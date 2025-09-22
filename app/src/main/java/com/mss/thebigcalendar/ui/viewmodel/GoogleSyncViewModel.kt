package com.mss.thebigcalendar.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.CalendarUiState
import com.mss.thebigcalendar.data.model.SyncProgress
import com.mss.thebigcalendar.data.repository.ActivityRepository
import com.mss.thebigcalendar.data.repository.HolidayRepository
import com.mss.thebigcalendar.data.repository.SettingsRepository
import com.mss.thebigcalendar.data.repository.JsonCalendarRepository
import com.mss.thebigcalendar.data.repository.DeletedActivityRepository
import com.mss.thebigcalendar.data.repository.CompletedActivityRepository
import com.mss.thebigcalendar.data.repository.AlarmRepository
import com.mss.thebigcalendar.data.service.BackupService
import com.mss.thebigcalendar.service.GoogleAuthService
import com.mss.thebigcalendar.service.GoogleCalendarService
import com.mss.thebigcalendar.service.ProgressiveSyncService
import com.mss.thebigcalendar.service.SearchService
import com.mss.thebigcalendar.service.RecurrenceService
import com.mss.thebigcalendar.service.VisibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.NetworkType
import com.mss.thebigcalendar.worker.GoogleCalendarSyncWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth

class GoogleSyncViewModel(application: Application) : AndroidViewModel(application) {

    // Repositories
    val settingsRepository = SettingsRepository(application)
    private val activityRepository = ActivityRepository(application)
    private val holidayRepository = HolidayRepository(application)
    private val deletedActivityRepository = DeletedActivityRepository(application)
    private val completedActivityRepository = CompletedActivityRepository(application)
    private val alarmRepository = AlarmRepository(application)
    private val jsonCalendarRepository = JsonCalendarRepository(application)
    
    // Services
    private val googleAuthService = GoogleAuthService(application)
    private val googleCalendarService = GoogleCalendarService(application)
    private val progressiveSyncService = ProgressiveSyncService(application, googleCalendarService)
    private val searchService = SearchService()
    private val recurrenceService = RecurrenceService()
    private val backupService = BackupService(application, activityRepository, deletedActivityRepository, completedActivityRepository)
    private val visibilityService = VisibilityService(application)

    // State Management
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    
    // Cache System
    private var updateJob: Job? = null
    private var cachedCalendarDays: List<com.mss.thebigcalendar.data.model.CalendarDay>? = null
    private var lastUpdateParams: String? = null
    private var cachedBirthdays: Map<LocalDate, List<Activity>> = emptyMap()
    private var cachedNotes: Map<LocalDate, List<Activity>> = emptyMap()
    private var cachedTasks: Map<LocalDate, List<Activity>> = emptyMap()

    // Google Authentication Functions
    fun onSignInClicked() {
        val signInIntent = googleAuthService.getSignInIntent()
        _uiState.update { it.copy(signInIntent = signInIntent) }
    }

    fun onSignInLaunched() {
        _uiState.update { it.copy(signInIntent = null) }
    }

    fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        viewModelScope.launch {
            try {
                val account = googleAuthService.handleSignInResult(task)
                if (account != null) {
                    _uiState.update { 
                        it.copy(
                            googleSignInAccount = account,
                            loginMessage = "Login realizado com sucesso!"
                        ) 
                    }
                    
                    // Salvar conta (implementar se necessário)
                    // settingsRepository.saveGoogleAccount(account)
                    
                    // Iniciar sincronização
                    fetchGoogleCalendarEvents(account)
                } else {
                    _uiState.update { it.copy(loginMessage = "Falha no login. Tente novamente.") }
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Erro no login do Google", e)
                _uiState.update { it.copy(loginMessage = "Erro no login: ${e.message}") }
            }
        }
    }

    fun clearLoginMessage() {
        _uiState.update { it.copy(loginMessage = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                googleAuthService.signOut { }
                _uiState.update { 
                    it.copy(
                        googleSignInAccount = null,
                        loginMessage = "Logout realizado com sucesso!"
                    ) 
                }
                
                // Limpar dados salvos (implementar se necessário)
                // settingsRepository.clearGoogleAccount()
                
                // Cancelar sincronização automática
                WorkManager.getInstance(getApplication()).cancelAllWorkByTag("google_sync")
                
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Erro no logout do Google", e)
                _uiState.update { it.copy(loginMessage = "Erro no logout: ${e.message}") }
            }
        }
    }

    // Google Calendar Sync Functions
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
            _uiState.update { it.copy(isSyncing = true, syncErrorMessage = null) }
            performProgressiveSync(account)
        }
    }

    private fun fetchGoogleCalendarEvents(account: GoogleSignInAccount, forceSync: Boolean = false) {
        viewModelScope.launch {
            try {
                _uiState.update { 
                    it.copy(
                        isSyncing = true,
                        syncErrorMessage = null,
                        syncProgress = SyncProgress(
                            currentStep = "Iniciando sincronização...",
                            progress = 0,
                            totalEvents = 0,
                            processedEvents = 0
                        )
                    ) 
                }
                
                val calendarService = googleCalendarService.getCalendarService(account)
                // Implementar busca de eventos usando calendarService
                val events = emptyList<com.google.api.services.calendar.model.Event>() // Placeholder
                var processedCount = 0
                val totalEvents = events.size
                
                for (event in events) {
                    try {
                        // Verificar se é um evento de aniversário
                        if (detectBirthdayEvent(event)) {
                            continue // Pular eventos de aniversário
                        }
                        
                        // Implementar conversão de evento para atividade
                        val activity = com.mss.thebigcalendar.data.model.Activity(
                            id = java.util.UUID.randomUUID().toString(),
                            title = event.summary ?: "Evento sem título",
                            description = event.description,
                            date = java.time.LocalDate.now().toString(), // Implementar conversão de data
                            startTime = null,
                            endTime = null,
                            isAllDay = event.start?.date != null,
                            location = event.location,
                            categoryColor = "FF6B6B",
                            activityType = com.mss.thebigcalendar.data.model.ActivityType.EVENT,
                            recurrenceRule = null,
                            notificationSettings = com.mss.thebigcalendar.data.model.NotificationSettings(),
                            visibility = com.mss.thebigcalendar.data.model.VisibilityLevel.LOW,
                            showInCalendar = true,
                            isFromGoogle = true
                        )
                        
                        // Verificar se já existe
                        val existingActivity = _uiState.value.activities.find { 
                            it.isFromGoogle && it.title == activity.title 
                        }
                        
                        if (existingActivity == null) {
                            // Nova atividade - salvar
                            activityRepository.saveActivity(activity)
                        } else {
                            // Atividade existente - verificar se precisa atualizar
                            if (existingActivity.title != activity.title) {
                                activityRepository.saveActivity(activity)
                            }
                        }
                        
                        processedCount++
                        _uiState.update { 
                            it.copy(
                                syncProgress = SyncProgress(
                                    currentStep = "Processando evento: ${activity.title}",
                                    progress = (processedCount * 100 / totalEvents).coerceAtMost(100),
                                    totalEvents = totalEvents,
                                    processedEvents = processedCount
                                )
                            ) 
                        }
                        
                        // Pequena pausa para não sobrecarregar
                        delay(50)
                        
                    } catch (e: Exception) {
                        Log.e("CalendarViewModel", "Erro ao processar evento: ${event.summary}", e)
                    }
                }
                
                // Criar alguns aniversários de exemplo se não existirem
                createSampleBirthdays()
                
                _uiState.update { 
                    it.copy(
                        isSyncing = false,
                        syncProgress = null,
                        lastGoogleSyncTime = System.currentTimeMillis()
                    ) 
                }
                
                // Agendar sincronização automática
                scheduleAutomaticSync()
                
                Log.d("CalendarViewModel", "✅ Sincronização do Google Calendar concluída: $processedCount eventos processados")
                
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "❌ Erro na sincronização do Google Calendar", e)
                _uiState.update { 
                    it.copy(
                        isSyncing = false,
                        syncProgress = null,
                        syncErrorMessage = "Erro na sincronização: ${e.message}"
                    ) 
                }
            }
        }
    }

    private fun performProgressiveSync(account: GoogleSignInAccount) {
        viewModelScope.launch {
            try {
                _uiState.update { 
                    it.copy(
                        syncProgress = SyncProgress(
                            currentStep = "Iniciando sincronização progressiva...",
                            progress = 0,
                            totalEvents = 0,
                            processedEvents = 0
                        )
                    ) 
                }
                
                val result = progressiveSyncService.syncProgressively(account) { progress ->
                    _uiState.update { it.copy(syncProgress = progress) }
                }
                
                result.fold(
                    onSuccess = { syncResult ->
                        _uiState.update { 
                            it.copy(
                                isSyncing = false,
                                syncProgress = null,
                                lastGoogleSyncTime = System.currentTimeMillis()
                            ) 
                        }
                        Log.d("CalendarViewModel", "✅ Sincronização progressiva concluída: $syncResult eventos")
                    },
                    onFailure = { exception ->
                        _uiState.update { 
                            it.copy(
                                isSyncing = false,
                                syncProgress = null,
                                syncErrorMessage = "Erro na sincronização: ${exception.message}"
                            ) 
                        }
                        Log.e("CalendarViewModel", "❌ Erro na sincronização progressiva", exception)
                    }
                )
                
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "❌ Erro inesperado na sincronização", e)
                _uiState.update { 
                    it.copy(
                        isSyncing = false,
                        syncProgress = null,
                        syncErrorMessage = "Erro inesperado: ${e.message}"
                    ) 
                }
            }
        }
    }

    private fun scheduleAutomaticSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<GoogleCalendarSyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag("google_sync")
            .build()
        
        WorkManager.getInstance(getApplication()).enqueue(syncRequest)
    }

    // Helper functions
    private fun detectBirthdayEvent(event: com.google.api.services.calendar.model.Event): Boolean {
        // Verificar se é um evento de aniversário baseado em padrões comuns
        val summary = event.summary?.lowercase() ?: ""
        val description = event.description?.lowercase() ?: ""
        
        val birthdayKeywords = listOf(
            "birthday", "aniversário", "aniversario", "nascimento", "birth day"
        )
        
        val hasBirthdayKeyword = birthdayKeywords.any { keyword ->
            summary.contains(keyword) || description.contains(keyword)
        }
        
        // Verificar se é um evento recorrente anual
        val isAnnualRecurring = event.recurrence?.any { rule ->
            rule.contains("FREQ=YEARLY")
        } ?: false
        
        // Verificar se tem duração de 1 dia (comum em aniversários)
        val isAllDay = event.start?.date != null && event.end?.date != null
        
        return hasBirthdayKeyword || (isAnnualRecurring && isAllDay)
    }

    private fun createSampleBirthdays() {
        viewModelScope.launch {
            val existingBirthdays = _uiState.value.activities.filter { 
                it.activityType == com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY 
            }
            
            if (existingBirthdays.isEmpty()) {
                val sampleBirthdays = listOf(
                    Activity(
                        id = "birthday_sample_1",
                        title = "Aniversário da Maria",
                        description = "Aniversário da Maria Silva",
                        date = LocalDate.now().withDayOfMonth(15).toString(),
                        startTime = java.time.LocalTime.of(0, 0),
                        endTime = java.time.LocalTime.of(23, 59),
                        isAllDay = true,
                        activityType = com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY,
                        categoryColor = "FF6B6B",
                        recurrenceRule = "FREQ=YEARLY",
                        isFromGoogle = false,
                        location = "",
                        notificationSettings = com.mss.thebigcalendar.data.model.NotificationSettings(),
                        visibility = com.mss.thebigcalendar.data.model.VisibilityLevel.LOW,
                        showInCalendar = true
                    ),
                    Activity(
                        id = "birthday_sample_2",
                        title = "Aniversário do João",
                        description = "Aniversário do João Santos",
                        date = LocalDate.now().withDayOfMonth(25).toString(),
                        startTime = java.time.LocalTime.of(0, 0),
                        endTime = java.time.LocalTime.of(23, 59),
                        isAllDay = true,
                        activityType = com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY,
                        categoryColor = "4ECDC4",
                        recurrenceRule = "FREQ=YEARLY",
                        isFromGoogle = false,
                        location = "",
                        notificationSettings = com.mss.thebigcalendar.data.model.NotificationSettings(),
                        visibility = com.mss.thebigcalendar.data.model.VisibilityLevel.LOW,
                        showInCalendar = true
                    )
                )
                
                sampleBirthdays.forEach { birthday ->
                    activityRepository.saveActivity(birthday)
                }
                
                Log.d("CalendarViewModel", "✅ Aniversários de exemplo criados")
            }
        }
    }
}
