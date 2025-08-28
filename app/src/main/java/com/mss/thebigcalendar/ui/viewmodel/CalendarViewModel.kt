package com.mss.thebigcalendar.ui.viewmodel

import android.app.Application
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val activityRepository = ActivityRepository(application)
    private val holidayRepository = HolidayRepository(application)
    private val googleAuthService = GoogleAuthService(application)
    private val googleCalendarService = GoogleCalendarService(application)
    private val searchService = SearchService()
    private val recurrenceService = RecurrenceService()
    private val deletedActivityRepository = DeletedActivityRepository(application)

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadData()
        checkForExistingSignIn()
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

    private fun fetchGoogleCalendarEvents(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncErrorMessage = null) }
            try {
                // 1. Clear old Google events
                activityRepository.deleteAllActivitiesFromGoogle()

                // 2. Fetch new events from Google Calendar
                val calendarService = googleCalendarService.getCalendarService(account)
                val events = withContext(Dispatchers.IO) {
                    calendarService.events().list("primary").execute()
                }

                // 3. Map them to the app's Activity model
                val activities = events.items.mapNotNull { event ->
                    // Ignore events without a start date
                    val start = event.start?.dateTime?.value ?: event.start?.date?.value ?: return@mapNotNull null
                    val end = event.end?.dateTime?.value ?: event.end?.date?.value

                    val startDate = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate()
                    val startTime = if (event.start?.dateTime != null) Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalTime() else null
                    val endTime = if (end != null && event.end?.dateTime != null) Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).toLocalTime() else null

                    Activity(
                        id = event.id ?: UUID.randomUUID().toString(),
                        title = event.summary ?: "Sem título",
                        description = event.description,
                        date = startDate.toString(),
                        startTime = startTime,
                        endTime = endTime,
                        isAllDay = event.start?.dateTime == null,
                        location = event.location,
                        categoryColor = "#4285F4", // Google Blue
                        activityType = ActivityType.EVENT,
                        recurrenceRule = event.recurrence?.firstOrNull(),
                        isFromGoogle = true
                    )
                }
                // 4. Save new events to the local repository
                activityRepository.saveAllActivities(activities)

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
                _uiState.update { it.copy(activities = activities) }
                updateAllDateDependentUI()
            }
        }
        
        viewModelScope.launch {
            deletedActivityRepository.deletedActivities.collect { deletedActivities ->
                _uiState.update { it.copy(deletedActivities = deletedActivities) }
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
            updateCalendarDays()
        }
    }

    private fun updateCalendarDays() {
        val state = _uiState.value
        val firstDayOfMonth = state.displayedYearMonth.atDay(1)
        val daysFromPrevMonthOffset = (firstDayOfMonth.dayOfWeek.value % 7)
        val gridStartDate = firstDayOfMonth.minusDays(daysFromPrevMonthOffset.toLong())

        val newCalendarDays = List(42) { i ->
            val date = gridStartDate.plusDays(i.toLong())
            val tasksForThisDay = if (state.filterOptions.showTasks || state.filterOptions.showEvents) {
                state.activities.filter { activity ->
                    val activityDate = LocalDate.parse(activity.date)
                    val typeMatches = (state.filterOptions.showTasks && activity.activityType == ActivityType.TASK) ||
                            (state.filterOptions.showEvents && activity.activityType == ActivityType.EVENT)
                    activityDate.isEqual(date) && typeMatches
                }.sortedWith(compareByDescending<Activity> { it.categoryColor?.toIntOrNull() ?: 0 }.thenBy { it.startTime ?: LocalTime.MIN })
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
        val tasks = if (state.filterOptions.showTasks || state.filterOptions.showEvents) {
            state.activities.filter { activity ->
                val activityDate = LocalDate.parse(activity.date)
                val typeMatches = (state.filterOptions.showTasks && activity.activityType == ActivityType.TASK) ||
                        (state.filterOptions.showEvents && activity.activityType == ActivityType.EVENT)
                activityDate.isEqual(state.selectedDate) && typeMatches
            }.sortedWith(compareByDescending<Activity> { it.categoryColor?.toIntOrNull() ?: 0 }.thenBy { it.startTime ?: LocalTime.MIN })
        } else {
            emptyList()
        }
        _uiState.update { it.copy(tasksForSelectedDate = tasks) }
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
        updateCalendarDays()
        updateTasksForSelectedDate()
        updateHolidaysForSelectedDate()
        updateSaintDaysForSelectedDate()
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
        val currentFilters = _uiState.value.filterOptions
        val newFilters = when (key) {
            "showHolidays" -> currentFilters.copy(showHolidays = value)
            "showSaintDays" -> currentFilters.copy(showSaintDays = value)
            "showCommemorativeDates" -> currentFilters.copy(showCommemorativeDates = value)
            "showEvents" -> currentFilters.copy(showEvents = value)
            "showTasks" -> currentFilters.copy(showTasks = value)
            else -> currentFilters
        }
        viewModelScope.launch {
            settingsRepository.saveFilterOptions(newFilters)
        }
    }

    fun onThemeChange(newTheme: Theme) {
        viewModelScope.launch {
            settingsRepository.saveTheme(newTheme)
        }
    }

    fun onSaveActivity(activityData: Activity) {
        viewModelScope.launch {
            // Salvar a atividade principal
            activityRepository.saveActivity(activityData)
            
            // ✅ Agendar notificação se configurada
            if (activityData.notificationSettings.isEnabled && 
                activityData.notificationSettings.notificationType != com.mss.thebigcalendar.data.model.NotificationType.NONE &&
                activityData.startTime != null) {
                
                val notificationService = NotificationService(getApplication())
                notificationService.scheduleNotification(activityData)
            }
            
            // Se a atividade tem repetição, gerar instâncias recorrentes
            if (activityData.recurrenceRule?.isNotEmpty() == true && activityData.recurrenceRule != "CUSTOM") {
                val startDate = java.time.LocalDate.parse(activityData.date)
                val endDate = startDate.plusYears(1) // Gerar repetições por 1 ano
                
                val recurringInstances = recurrenceService.generateRecurringInstances(
                    baseActivity = activityData,
                    startDate = startDate,
                    endDate = endDate
                )
                
                // Salvar todas as instâncias recorrentes
                recurringInstances.forEach { instance ->
                    if (instance.id != activityData.id) { // Não salvar a atividade principal novamente
                        activityRepository.saveActivity(instance)
                    }
                }
                
                println("🔄 Atividade recorrente criada: ${activityData.title}")
                println("📅 Instâncias geradas: ${recurringInstances.size}")
            }
            
            closeCreateActivityModal()
        }
    }

    fun onDeleteActivityConfirm() {
        viewModelScope.launch {
            _uiState.value.activityIdToDelete?.let { activityId ->
                val activityToDelete = _uiState.value.activities.find { it.id == activityId }
                
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
                        }
                        
                        println("🗑️ Atividade recorrente deletada: ${activityToDelete.title}")
                        println("📅 Instâncias deletadas: ${recurringActivities.size}")
                    } else {
                        // Mover para a lixeira
                        deletedActivityRepository.addDeletedActivity(activityToDelete)
                        
                        // Deletar da lista principal
                        activityRepository.deleteActivity(activityId)
                    }
                }
            }
            cancelDeleteActivity()
        }
    }

    

    fun onBackupRequest() { println("ViewModel: Pedido de backup recebido.") }
    fun onRestoreRequest() { println("ViewModel: Pedido de restauração recebido.") }

    fun openSidebar() = _uiState.update { it.copy(isSidebarOpen = true) }
    fun closeSidebar() = _uiState.update { it.copy(isSidebarOpen = false) }

    fun onNavigateToSettings(screen: String?) {
        _uiState.update { it.copy(currentSettingsScreen = screen, isSidebarOpen = false) }
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
            recurrenceRule = null
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
            val activityToComplete = _uiState.value.activities.find { it.id == activityId }
            
            if (activityToComplete != null) {
                // Marcar como concluída
                val completedActivity = activityToComplete.copy(isCompleted = true)
                activityRepository.saveActivity(completedActivity)
                
                // Mover para a lixeira
                deletedActivityRepository.addDeletedActivity(completedActivity)
                
                // Remover da lista principal
                activityRepository.deleteActivity(activityId)
                
                println("✅ Tarefa marcada como concluída: ${completedActivity.title}")
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
}