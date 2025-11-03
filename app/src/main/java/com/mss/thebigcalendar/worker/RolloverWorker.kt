package com.mss.thebigcalendar.worker

import android.content.Context
import android.util.Log
import androidx.room.util.copy
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mss.thebigcalendar.data.repository.ActivityRepository
import kotlinx.coroutines.flow.first

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class RolloverWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "RolloverWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "RolloverWorker starting.")

            val activityRepository = ActivityRepository(applicationContext)
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            val activitiesToRollover = activityRepository.activities.first()
                .filter { it.date == yesterday.toString() && it.rollover && !it.isCompleted }

            if (activitiesToRollover.isNotEmpty()) {
                Log.d(TAG, "Found ${'$'}{activitiesToRollover.size} activities to roll over.")
                val updatedActivities = activitiesToRollover.map { activity ->
                    activity.copy(date = today.toString())
                }
                activityRepository.saveAllActivities(updatedActivities)
                Log.d(TAG, "Successfully rolled over ${'$'}{updatedActivities.size} activities to today.")
            } else {
                Log.d(TAG, "No activities to roll over for ${'$'}yesterday.")
            }

            Log.d(TAG, "RolloverWorker finished.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in RolloverWorker", e)
            Result.failure()
        }
    }
}
