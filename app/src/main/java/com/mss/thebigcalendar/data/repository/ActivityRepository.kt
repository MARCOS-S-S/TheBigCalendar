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
import java.time.YearMonth
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

    /**
     * Carrega apenas as atividades de um mês específico
     * Otimização para reduzir o uso de memória e melhorar performance
     */
    fun getActivitiesForMonth(yearMonth: YearMonth): Flow<List<Activity>> {
        return context.activitiesDataStore.data
            .map { activitiesProto ->
                val allActivities = activitiesProto.activitiesList.map { proto ->
                    proto.toActivity()
                }
                
                android.util.Log.d("ActivityRepository", "📅 Carregando atividades para mês: ${yearMonth}")
                android.util.Log.d("ActivityRepository", "📊 Total de atividades no banco: ${allActivities.size}")
                
                val filteredActivities = allActivities.filter { activity ->
                    try {
                        val activityDate = LocalDate.parse(activity.date)
                        val isInMonth = activityDate.year == yearMonth.year && 
                                       activityDate.month == yearMonth.month
                        
                        // Para aniversários, verificar se é o mesmo mês (ignorando ano)
                        val isBirthdayInMonth = if (activity.activityType == ActivityType.BIRTHDAY) {
                            // Para aniversários, incluir apenas se for do mês correto (independente do ano)
                            activityDate.month == yearMonth.month
                        } else {
                            // Para outros tipos, verificar se está exatamente no mês solicitado
                            isInMonth
                        }
                        
                        // Log para debug de aniversários
                        if (activity.activityType == ActivityType.BIRTHDAY) {
                            android.util.Log.d("ActivityRepository", "🎂 Verificando aniversário: ${activity.title} - Data: ${activityDate} - Mês: ${activityDate.month} - Mês solicitado: ${yearMonth.month} - Incluir: ${isBirthdayInMonth}")
                        }
                        
                        // Para atividades recorrentes, verificar se há instâncias no mês
                        val hasRecurringInstances = if (activity.recurrenceRule?.isNotEmpty() == true && 
                                                       activity.recurrenceRule != "CUSTOM") {
                            val hasInstances = hasRecurringInstancesInMonth(activity, yearMonth)
                            if (hasInstances) {
                                android.util.Log.d("ActivityRepository", "🔄 Atividade recorrente incluída: ${activity.title} - Regra: ${activity.recurrenceRule}")
                            }
                            hasInstances
                        } else {
                            false
                        }
                        
                        val shouldInclude = isBirthdayInMonth || hasRecurringInstances
                        
                        if (shouldInclude) {
                            android.util.Log.d("ActivityRepository", "✅ Incluindo atividade: ${activity.title} (${activity.date}) - Tipo: ${activity.activityType} - isBirthdayInMonth: ${isBirthdayInMonth} - hasRecurringInstances: ${hasRecurringInstances}")
                        }
                        
                        shouldInclude
                    } catch (e: Exception) {
                        android.util.Log.e("ActivityRepository", "❌ Erro ao processar atividade: ${activity.title}", e)
                        false
                    }
                }
                
                android.util.Log.d("ActivityRepository", "📈 Atividades filtradas para ${yearMonth}: ${filteredActivities.size}")
                android.util.Log.d("ActivityRepository", "🎯 Aniversários: ${filteredActivities.count { it.activityType == ActivityType.BIRTHDAY }}")
                android.util.Log.d("ActivityRepository", "📝 Tarefas: ${filteredActivities.count { it.activityType == ActivityType.TASK }}")
                android.util.Log.d("ActivityRepository", "📅 Eventos: ${filteredActivities.count { it.activityType == ActivityType.EVENT }}")
                android.util.Log.d("ActivityRepository", "📋 Notas: ${filteredActivities.count { it.activityType == ActivityType.NOTE }}")
                
                // Log detalhado dos aniversários do mês
                val augustBirthdays = filteredActivities.filter { it.activityType == ActivityType.BIRTHDAY }
                if (augustBirthdays.isNotEmpty()) {
                    android.util.Log.d("ActivityRepository", "🎂 Aniversários de ${yearMonth.month.name}:")
                    augustBirthdays.forEach { birthday ->
                        android.util.Log.d("ActivityRepository", "   - ${birthday.title} (${birthday.date})")
                    }
                }
                
                filteredActivities
            }
    }

    /**
     * Verifica se uma atividade recorrente tem instâncias em um mês específico
     */
    private fun hasRecurringInstancesInMonth(activity: Activity, yearMonth: YearMonth): Boolean {
        try {
            val startDate = LocalDate.parse(activity.date)
            val monthStart = yearMonth.atDay(1)
            val monthEnd = yearMonth.atEndOfMonth()
            
            // Se a data de início é posterior ao fim do mês, não há instâncias
            if (startDate.isAfter(monthEnd)) {
                return false
            }
            
            // Para atividades recorrentes, verificar se há pelo menos uma instância no mês
            return when (activity.recurrenceRule) {
                "DAILY" -> {
                    // Diárias só se a data de início for antes ou igual ao fim do mês
                    !startDate.isAfter(monthEnd)
                }
                "WEEKLY" -> {
                    // Semanais só se houver pelo menos uma semana no mês
                    val weeksInMonth = (monthEnd.dayOfMonth - monthStart.dayOfMonth + 1) / 7 + 1
                    weeksInMonth > 0 && !startDate.isAfter(monthEnd)
                }
                "MONTHLY" -> {
                    // Mensais só se o dia de início for válido no mês
                    val startDay = startDate.dayOfMonth
                    startDay <= monthEnd.dayOfMonth && startDate.month == yearMonth.month
                }
                "YEARLY" -> {
                    // Anuais só se for do mesmo mês
                    startDate.month == yearMonth.month
                }
                else -> {
                    // Para regras complexas, não incluir automaticamente
                    false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ActivityRepository", "❌ Erro ao verificar atividade recorrente: ${activity.title}", e)
            return false
        }
    }

    /**
     * Carrega atividades para um período específico (útil para visualização mensal)
     */
    fun getActivitiesForPeriod(startDate: LocalDate, endDate: LocalDate): Flow<List<Activity>> {
        return context.activitiesDataStore.data
            .map { activitiesProto ->
                activitiesProto.activitiesList.map { proto ->
                    proto.toActivity()
                }.filter { activity ->
                    try {
                        val activityDate = LocalDate.parse(activity.date)
                        val isInPeriod = !activityDate.isBefore(startDate) && !activityDate.isAfter(endDate)
                        
                        // Para aniversários, verificar se está no período (ignorando ano)
                        val isBirthdayInPeriod = if (activity.activityType == ActivityType.BIRTHDAY) {
                            val birthdayInYear = LocalDate.of(startDate.year, activityDate.month, activityDate.dayOfMonth)
                            !birthdayInYear.isBefore(startDate) && !birthdayInYear.isAfter(endDate)
                        } else {
                            isInPeriod
                        }
                        
                        isBirthdayInPeriod
                    } catch (e: Exception) {
                        false
                    }
                }
            }
    }

    suspend fun saveActivity(activity: Activity) {
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
            .addAllExcludedDates(this.excludedDates)
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
            showInCalendar = this.showInCalendar,
            excludedDates = this.excludedDatesList.toList()
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
