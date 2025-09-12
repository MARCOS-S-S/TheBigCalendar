package com.mss.thebigcalendar.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.repository.ActivityRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class EventListRemoteViewsFactory(private val context: Context, intent: Intent) : RemoteViewsService.RemoteViewsFactory {

    private var activities: List<Activity> = emptyList()
    private lateinit var activityRepository: ActivityRepository

    override fun onCreate() {
        activityRepository = ActivityRepository(context)
    }

    override fun onDataSetChanged() {
        // This is called by the app widget manager whenever the data set has changed.
        // You can use this to update your data.
        runBlocking {
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            val currentTime = LocalTime.now()
            val isNightTime = isNightTime(currentTime)

            val allActivities = activityRepository.activities.firstOrNull() ?: emptyList()

            val todayTasks = allActivities.filter { activity ->
                try {
                    val activityDate = LocalDate.parse(activity.date)
                    val isExcluded = if (activity.recurrenceRule?.isNotEmpty() == true && activity.recurrenceRule != "CUSTOM") {
                        activity.excludedDates.contains(today.toString())
                    } else {
                        false
                    }

                    if (isExcluded) {
                        false
                    } else {
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
                compareByDescending<Activity> { it.categoryColor?.toIntOrNull() ?: 0 }
                    .thenBy { it.startTime ?: LocalTime.MIN }
            )

            val tomorrowTasks = if (isNightTime) {
                allActivities.filter { activity ->
                    try {
                        val activityDate = LocalDate.parse(activity.date)
                        val isExcluded = if (activity.recurrenceRule?.isNotEmpty() == true && activity.recurrenceRule != "CUSTOM") {
                            activity.excludedDates.contains(tomorrow.toString())
                        } else {
                            false
                        }

                        if (isExcluded) {
                            false
                        } else {
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
                    compareByDescending<Activity> { it.categoryColor.toIntOrNull() ?: 0 }
                        .thenBy { it.startTime ?: LocalTime.MIN }
                )
            } else {
                emptyList()
            }

            activities = todayTasks + tomorrowTasks
        }
    }

    override fun onDestroy() {
        activities = emptyList()
    }

    override fun getCount(): Int {
        return activities.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= activities.size) {
            return RemoteViews(context.packageName, R.layout.event_list_widget_item)
        }

        val activity = activities[position]
        val views = RemoteViews(context.packageName, R.layout.event_list_widget_item)

        val prefix = if (activity.activityType == com.mss.thebigcalendar.data.model.ActivityType.BIRTHDAY) {
            "🎂 " // Birthday icon
        } else if (activity.startTime != null) {
            "${activity.startTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} "
        } else {
            ""
        }

        views.setTextViewText(R.id.event_item_title, "$prefix${activity.title}")

        val descriptionText = activity.description.takeIf { !it.isNullOrBlank() } ?: ""
        views.setTextViewText(R.id.event_item_time, descriptionText)

        // Set fill-in intent for click handling (optional, but good practice)
        val fillInIntent = Intent()
        // You can put extras here if you want to pass data to the activity when an item is clicked
        views.setOnClickFillInIntent(R.id.event_item_title, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null // You can return a custom loading view here if needed
    }

    override fun getViewTypeCount(): Int {
        return 1 // All items are the same type
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    /**
     * Checks if it's night time (sunset to midnight)
     * Considers approximate sunset times based on the season (Brazil)
     */
    private fun isNightTime(currentTime: LocalTime): Boolean {
        val currentDate = LocalDate.now()
        val month = currentDate.monthValue

        // Approximate sunset times by season (Brazil)
        val sunsetTime = when (month) {
            in 12..2 -> LocalTime.of(19, 30) // Summer (Dec-Feb): ~19:30
            in 3..5 -> LocalTime.of(18, 30)  // Autumn (Mar-May): ~18:30
            in 6..8 -> LocalTime.of(17, 30)  // Winter (Jun-Aug): ~17:30
            in 9..11 -> LocalTime.of(18, 0)  // Spring (Sep-Nov): ~18:00
            else -> LocalTime.of(18, 0)      // Default
        }

        val midnight = LocalTime.of(0, 0) // 00:00 (midnight)

        return currentTime.isAfter(sunsetTime) || currentTime.isBefore(midnight)
    }
}