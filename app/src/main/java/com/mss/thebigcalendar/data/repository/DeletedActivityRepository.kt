package com.mss.thebigcalendar.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.DeletedActivity
import com.mss.thebigcalendar.data.model.proto.DeletedActivitySerializer
import com.mss.thebigcalendar.data.model.proto.TrashActivities
import com.mss.thebigcalendar.data.model.proto.TrashActivityProto
import com.mss.thebigcalendar.data.model.proto.TrashActivityTypeProto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.util.UUID

private val Context.deletedActivitiesDataStore: DataStore<TrashActivities> by dataStore(
    fileName = "deleted_activities.pb",
    serializer = DeletedActivitySerializer
)

class DeletedActivityRepository(private val context: Context) {

    val deletedActivities: Flow<List<DeletedActivity>> = context.deletedActivitiesDataStore.data
        .map { trashActivitiesProto ->
            trashActivitiesProto.trashActivitiesList.map { proto ->
                proto.toDeletedActivity()
            }
        }

    suspend fun addDeletedActivity(activity: com.mss.thebigcalendar.data.model.Activity) {
        val deletedActivity = DeletedActivity(
            id = UUID.randomUUID().toString(),
            originalActivity = activity
        )

        context.deletedActivitiesDataStore.updateData { currentTrashActivities ->
            val proto = deletedActivity.toProto()
            val newTrashActivities = currentTrashActivities.toBuilder()
            newTrashActivities.addTrashActivities(proto)
            newTrashActivities.build()
        }
    }

    suspend fun restoreActivity(deletedActivityId: String): com.mss.thebigcalendar.data.model.Activity? {
        val deletedActivity = context.deletedActivitiesDataStore.data.map { 
            it.trashActivitiesList.find { proto -> proto.id == deletedActivityId }
        }.firstOrNull()?.toDeletedActivity()

        if (deletedActivity != null) {
            // Remover da lixeira
            removeDeletedActivity(deletedActivityId)
            // Retornar a atividade original para ser restaurada
            return deletedActivity.originalActivity
        }
        return null
    }

    suspend fun removeDeletedActivity(deletedActivityId: String) {
        context.deletedActivitiesDataStore.updateData { currentTrashActivities ->
            val newTrashActivities = currentTrashActivities.toBuilder()
            val indexToRemove = currentTrashActivities.trashActivitiesList.indexOfFirst { it.id == deletedActivityId }
            if (indexToRemove != -1) {
                newTrashActivities.removeTrashActivities(indexToRemove)
            }
            newTrashActivities.build()
        }
    }

    suspend fun clearAllDeletedActivities() {
        context.deletedActivitiesDataStore.updateData { 
            TrashActivities.newBuilder().build()
        }
    }

    // --- Mappers ---

    private fun DeletedActivity.toProto(): TrashActivityProto {
        return TrashActivityProto.newBuilder()
            .setId(this.id)
            .setOriginalActivity(this.originalActivity.toProto())
            .setDeletedAt(this.deletedAt.toString())
            .setDeletedBy(this.deletedBy)
            .build()
    }

    private fun TrashActivityProto.toDeletedActivity(): DeletedActivity {
        return DeletedActivity(
            id = this.id,
            originalActivity = this.originalActivity.toActivity(),
            deletedAt = LocalDateTime.parse(this.deletedAt),
            deletedBy = this.deletedBy
        )
    }

    private fun com.mss.thebigcalendar.data.model.Activity.toProto(): com.mss.thebigcalendar.data.model.proto.TrashActivityDataProto {
        return com.mss.thebigcalendar.data.model.proto.TrashActivityDataProto.newBuilder()
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
            .build()
    }

    private fun com.mss.thebigcalendar.data.model.proto.TrashActivityDataProto.toActivity(): com.mss.thebigcalendar.data.model.Activity {
        return com.mss.thebigcalendar.data.model.Activity(
            id = this.id,
            title = this.title,
            description = this.description.takeIf { it.isNotEmpty() },
            date = this.date,
            startTime = this.startTime.takeIf { it.isNotEmpty() }?.let { java.time.LocalTime.parse(it) },
            endTime = this.endTime.takeIf { it.isNotEmpty() }?.let { java.time.LocalTime.parse(it) },
            isAllDay = this.isAllDay,
            location = this.location.takeIf { it.isNotEmpty() },
            categoryColor = this.categoryColor,
            activityType = this.activityType.toActivityType(),
            recurrenceRule = this.recurrenceRule.takeIf { it.isNotEmpty() },
            notificationSettings = com.mss.thebigcalendar.data.model.NotificationSettings(),
            isCompleted = this.isCompleted
        )
    }

    private fun com.mss.thebigcalendar.data.model.ActivityType.toProto(): com.mss.thebigcalendar.data.model.proto.TrashActivityTypeProto {
        return when (this) {
            ActivityType.EVENT -> TrashActivityTypeProto.TRASH_EVENT
            ActivityType.TASK -> TrashActivityTypeProto.TRASH_TASK
            ActivityType.BIRTHDAY -> TrashActivityTypeProto.TRASH_BIRTHDAY
            ActivityType.NOTE -> TrashActivityTypeProto.TRASH_NOTE
        }
    }

    private fun com.mss.thebigcalendar.data.model.proto.TrashActivityTypeProto.toActivityType(): com.mss.thebigcalendar.data.model.ActivityType {
        return when (this) {
            TrashActivityTypeProto.TRASH_EVENT -> ActivityType.EVENT
            TrashActivityTypeProto.TRASH_TASK -> ActivityType.TASK
            TrashActivityTypeProto.TRASH_NOTE -> ActivityType.NOTE
            TrashActivityTypeProto.TRASH_BIRTHDAY -> ActivityType.BIRTHDAY
            else -> ActivityType.EVENT // Fallback for unrecognized enum values
        }
    }
}
