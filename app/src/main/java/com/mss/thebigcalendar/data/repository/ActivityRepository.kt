package com.mss.thebigcalendar.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.mss.thebigcalendar.data.model.proto.Activities
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.proto.ActivityProto
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.proto.ActivityTypeProto
import com.mss.thebigcalendar.data.model.NotificationSettings
import com.mss.thebigcalendar.data.model.NotificationType
import com.mss.thebigcalendar.data.model.proto.NotificationSettingsProto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

private val Context.activitiesDataStore: DataStore<Activities> by dataStore(
    fileName = "activities.pb",
    serializer = ActivitySerializer
)

class ActivityRepository(private val context: Context) {

    val activities: Flow<List<Activity>> = context.activitiesDataStore.data
        .map { activitiesProto ->
            activitiesProto.activitiesList.map { proto ->
                proto.toActivity()
            }
        }

    suspend fun saveActivity(activity: Activity) {
        // ✅ Log para debug da visibilidade
        android.util.Log.d("ActivityRepository", "💾 Salvando atividade: ${activity.title}")
        android.util.Log.d("ActivityRepository", "🔍 Visibilidade: ${activity.visibility}")
        android.util.Log.d("ActivityRepository", "🔔 Notificações: ${activity.notificationSettings}")

        context.activitiesDataStore.updateData { currentActivities ->
            val existingIndex = currentActivities.activitiesList.indexOfFirst { it.id == activity.id }
            val proto = activity.toProto()
            val newActivities = currentActivities.toBuilder()
            if (existingIndex != -1) {
                newActivities.setActivities(existingIndex, proto)
            } else {
                newActivities.addActivities(proto)
            }
            newActivities.build()
        }
    }

    suspend fun saveAllActivities(activities: List<Activity>) {
        context.activitiesDataStore.updateData { currentActivities ->
            val newActivities = currentActivities.toBuilder()
            activities.forEach { activity ->
                val existingIndex = newActivities.activitiesList.indexOfFirst { it.id == activity.id }
                val proto = activity.toProto()
                if (existingIndex != -1) {
                    newActivities.setActivities(existingIndex, proto)
                } else {
                    newActivities.addActivities(proto)
                }
            }
            newActivities.build()
        }
    }

    suspend fun deleteActivity(activityId: String) {
        context.activitiesDataStore.updateData { currentActivities ->
            val newActivities = currentActivities.toBuilder()
            val indexToDelete = currentActivities.activitiesList.indexOfFirst { it.id == activityId }
            if (indexToDelete != -1) {
                newActivities.removeActivities(indexToDelete)
            }
            newActivities.build()
        }
    }

    suspend fun deleteAllActivitiesFromGoogle() {
        context.activitiesDataStore.updateData { currentActivities ->
            val newActivities = currentActivities.toBuilder()
            val activitiesToKeep = currentActivities.activitiesList.filter { !it.isFromGoogle }
            newActivities.clearActivities()
            newActivities.addAllActivities(activitiesToKeep)
            newActivities.build()
        }
    }

    // --- Mappers ---

    private fun Activity.toProto(): ActivityProto {
        return ActivityProto.newBuilder()
            .setId(this.id)
            .setTitle(this.title)
            .setDescription(this.description ?: "")
            .setDate(this.date)
            .setStartTime(this.startTime?.toString() ?: "")
            .setEndTime(this.endTime?.toString() ?: "")
            .setIsAllDay(this.isAllDay)
            .setLocation(this.location ?: "")
            .setCategoryColor(this.categoryColor)
            .setActivityType(this.activityType.toProto())
            .setRecurrenceRule(this.recurrenceRule ?: "")
            .setIsCompleted(this.isCompleted)
            .setIsFromGoogle(this.isFromGoogle)
            .setVisibility(this.visibility.name)
            .setShowInCalendar(this.showInCalendar)
            .setNotificationSettings(this.notificationSettings.toProto())
            .build()
    }

    private fun ActivityProto.toActivity(): Activity {
        return Activity(
            id = this.id,
            title = this.title,
            description = this.description.takeIf { it.isNotEmpty() },
            date = this.date,
            startTime = this.startTime.takeIf { it.isNotEmpty() }?.let { LocalTime.parse(it) },
            endTime = this.endTime.takeIf { it.isNotEmpty() }?.let { LocalTime.parse(it) },
            isAllDay = this.isAllDay,
            location = this.location.takeIf { it.isNotEmpty() },
            categoryColor = this.categoryColor,
            activityType = this.activityType.toActivityType(),
            recurrenceRule = this.recurrenceRule.takeIf { it.isNotEmpty() },
            notificationSettings = this.notificationSettings.toNotificationSettings(),
            isCompleted = this.isCompleted,
            isFromGoogle = this.isFromGoogle,
            visibility = try {
                com.mss.thebigcalendar.data.model.VisibilityLevel.valueOf(this.visibility)
            } catch (e: Exception) {
                com.mss.thebigcalendar.data.model.VisibilityLevel.LOW
            },
            showInCalendar = this.showInCalendar
        )
    }

    private fun ActivityType.toProto(): ActivityTypeProto {
        return when (this) {
            ActivityType.EVENT -> ActivityTypeProto.EVENT
            ActivityType.TASK -> ActivityTypeProto.TASK
            ActivityType.BIRTHDAY -> ActivityTypeProto.BIRTHDAY
            ActivityType.NOTE -> ActivityTypeProto.NOTE
        }
    }

    private fun ActivityTypeProto.toActivityType(): ActivityType {
        return when (this) {
            ActivityTypeProto.EVENT -> ActivityType.EVENT
            ActivityTypeProto.TASK -> ActivityType.TASK
            ActivityTypeProto.NOTE -> ActivityType.NOTE
            ActivityTypeProto.BIRTHDAY -> ActivityType.BIRTHDAY
            else -> ActivityType.EVENT // Fallback for unrecognized enum values
        }
    }
    
    private fun NotificationSettings.toProto(): NotificationSettingsProto {
        return NotificationSettingsProto.newBuilder()
            .setIsEnabled(this.isEnabled)
            .setNotificationTime(this.notificationTime?.toString() ?: "")
            .setNotificationType(this.notificationType.name)
            .setCustomMinutesBefore(this.customMinutesBefore ?: 0)
            .build()
    }
    
    private fun NotificationSettingsProto.toNotificationSettings(): NotificationSettings {
        val notificationType = try {
            NotificationType.valueOf(this.notificationType)
        } catch (e: Exception) {
            NotificationType.FIFTEEN_MINUTES_BEFORE
        }
        
        val notificationTime = this.notificationTime.takeIf { it.isNotEmpty() }?.let {
            try {
                LocalTime.parse(it)
            } catch (e: Exception) {
                null
            }
        }
        
        return NotificationSettings(
            isEnabled = this.isEnabled,
            notificationTime = notificationTime,
            notificationType = notificationType,
            customMinutesBefore = if (this.customMinutesBefore > 0) this.customMinutesBefore else null
        )
    }
}
