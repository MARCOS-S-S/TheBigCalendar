package com.mss.thebigcalendar.data.model

import java.time.LocalTime

/**
 * Configurações de notificação para uma atividade
 */
data class NotificationSettings(
    val isEnabled: Boolean = false,
    val notificationTime: LocalTime? = null,
    val notificationType: NotificationType = NotificationType.BEFORE_ACTIVITY,
    val customMinutesBefore: Int? = null
)

/**
 * Tipos de notificação disponíveis
 */
enum class NotificationType(val displayName: String, val minutesBefore: Int?) {
    NONE("Sem notificação", null),
    BEFORE_ACTIVITY("Antes da atividade", 0),
    FIVE_MINUTES_BEFORE("5 minutos antes", 5),
    TEN_MINUTES_BEFORE("10 minutos antes", 10),
    FIFTEEN_MINUTES_BEFORE("15 minutos antes", 15),
    THIRTY_MINUTES_BEFORE("30 minutos antes", 30),
    ONE_HOUR_BEFORE("1 hora antes", 60),
    TWO_HOURS_BEFORE("2 horas antes", 120),
    ONE_DAY_BEFORE("1 dia antes", 1440), // 24 * 60 minutos
    CUSTOM("Personalizado", null)
}

/**
 * Extensão para calcular o horário da notificação baseado no tipo
 */
fun NotificationType.calculateNotificationTime(activityTime: LocalTime, customMinutes: Int? = null): LocalTime {
    return when (this) {
        NotificationType.NONE -> activityTime
        NotificationType.BEFORE_ACTIVITY -> activityTime
        NotificationType.CUSTOM -> {
            val minutes = customMinutes ?: 15
            activityTime.minusMinutes(minutes.toLong())
        }
        else -> {
            val minutes = this.minutesBefore ?: 15
            activityTime.minusMinutes(minutes.toLong())
        }
    }
}

/**
 * Extensão para obter o texto descritivo da notificação
 */
fun NotificationType.getDescription(): String {
    return when (this) {
        NotificationType.NONE -> "Sem notificação"
        NotificationType.BEFORE_ACTIVITY -> "No momento da atividade"
        NotificationType.CUSTOM -> "Personalizado"
        else -> this.displayName
    }
}

