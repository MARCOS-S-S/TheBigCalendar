package com.mss.thebigcalendar.ui.viewmodel.util

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mss.thebigcalendar.data.repository.ActivityRepository
import com.mss.thebigcalendar.data.repository.AlarmRepository
import com.mss.thebigcalendar.data.repository.CompletedActivityRepository
import com.mss.thebigcalendar.data.repository.DeletedActivityRepository
import com.mss.thebigcalendar.data.repository.HolidayRepository
import com.mss.thebigcalendar.data.repository.JsonCalendarRepository
import com.mss.thebigcalendar.data.repository.SettingsRepository
import com.mss.thebigcalendar.data.service.BackupService
import com.mss.thebigcalendar.service.GoogleAuthService
import com.mss.thebigcalendar.service.GoogleCalendarService
import com.mss.thebigcalendar.service.NotificationService
import com.mss.thebigcalendar.service.ProgressiveSyncService
import com.mss.thebigcalendar.service.RecurrenceService
import com.mss.thebigcalendar.service.SearchService
import com.mss.thebigcalendar.service.VisibilityService

class NotUsedCalendarViewModelCoordinator(application: Application) : AndroidViewModel(application) {

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
    private val notificationService = NotificationService(application) // Assuming this is needed for general notifications

    // Specialized ViewModels
    val navigationViewModel = NotUsedCalendarNavigationViewModel(application)
    val activityManagementViewModel = NotUsedActivityManagementViewModel(application)
    val googleSyncViewModel = NotUsedGoogleSyncViewModel(application)
    val backupViewModel = NotUsedBackupViewModel(application)
    val uiManagementViewModel = NotUsedCalendarUIManagementViewModel(application)

    // The coordinator will expose a combined UI state or delegate state management
    // For now, we can observe individual ViewModel states and combine them if needed.
    // Or, the UI can directly observe the specialized ViewModels.
}
