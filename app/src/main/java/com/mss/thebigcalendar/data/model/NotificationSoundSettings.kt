package com.mss.thebigcalendar.data.model

/**
 * Configurações de som para notificações por nível de visibilidade
 */
data class NotificationSoundSettings(
    val lowVisibilitySound: String = "default", // Som para visibilidade baixa
    val mediumVisibilitySound: String = "default", // Som para visibilidade média
    val highVisibilitySound: String = "default" // Som para visibilidade alta
)

/**
 * Tipos de sons de notificação disponíveis
 */
enum class NotificationSoundType(val displayName: String, val soundResource: String) {
    DEFAULT("Padrão do sistema", "default"),
    BEEP("Bip", "beep"),
    CHIME("Sino", "chime"),
    BELL("Sino clássico", "bell"),
    ALERT("Alerta", "alert"),
    NOTIFICATION("Notificação", "notification"),
    RINGTONE("Toque", "ringtone"),
    VIBRATION_ONLY("Apenas vibração", "vibration_only")
}

/**
 * Extensão para obter o som baseado no nível de visibilidade
 */
fun NotificationSoundSettings.getSoundForVisibility(visibility: VisibilityLevel): String {
    return when (visibility) {
        VisibilityLevel.LOW -> lowVisibilitySound
        VisibilityLevel.MEDIUM -> mediumVisibilitySound
        VisibilityLevel.HIGH -> highVisibilitySound
    }
}
