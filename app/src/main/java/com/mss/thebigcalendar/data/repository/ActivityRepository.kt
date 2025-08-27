package com.mss.thebigcalendar.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.mss.thebigcalendar.data.model.proto.Activities
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.proto.ActivityProto
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.proto.ActivityTypeProto
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
        val activityToSave = if (activity.id == "new" || activity.id.isBlank()) {
            activity.copy(id = UUID.randomUUID().toString())
        } else {
            activity
        }

        context.activitiesDataStore.updateData { currentActivities ->
            val existingIndex = currentActivities.activitiesList.indexOfFirst { it.id == activityToSave.id }
            val proto = activityToSave.toProto()
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
            notificationSettings = com.mss.thebigcalendar.data.model.NotificationSettings(),
            isCompleted = this.isCompleted,
            isFromGoogle = this.isFromGoogle
        )
    }

    private fun ActivityType.toProto(): ActivityTypeProto {
        return when (this) {
            ActivityType.EVENT -> ActivityTypeProto.EVENT
            ActivityType.TASK -> ActivityTypeProto.TASK
            ActivityType.BIRTHDAY -> ActivityTypeProto.BIRTHDAY
        }
    }

    private fun ActivityTypeProto.toActivityType(): ActivityType {
        return when (this) {
            ActivityTypeProto.EVENT -> ActivityType.EVENT
            ActivityTypeProto.TASK -> ActivityType.TASK
            ActivityTypeProto.BIRTHDAY -> ActivityType.BIRTHDAY
            else -> ActivityType.EVENT // Fallback for unrecognized enum values
        }
    }
}
