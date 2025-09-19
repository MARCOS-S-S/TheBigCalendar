package com.mss.thebigcalendar.data.model

import java.time.LocalTime

/**
 * Configurações de um despertador
 */
data class AlarmSettings(
    val id: String,
    val label: String,
    val time: LocalTime,
    val isEnabled: Boolean,
    val repeatDays: Set<String>, // Dom, Seg, Ter, Qua, Qui, Sex, Sáb
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val snoozeMinutes: Int = 5,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Cria um novo alarme com configurações padrão
         */
        fun createDefault(
            label: String = "Despertador",
            time: LocalTime = LocalTime.of(8, 0)
        ): AlarmSettings {
            return AlarmSettings(
                id = "alarm_${System.currentTimeMillis()}",
                label = label,
                time = time,
                isEnabled = true,
                repeatDays = emptySet()
            )
        }
        
        /**
         * Valida se as configurações do alarme são válidas
         */
        fun validate(settings: AlarmSettings): ValidationResult {
            return when {
                settings.label.isBlank() -> ValidationResult.Error("Rótulo não pode estar vazio")
                settings.label.length > 50 -> ValidationResult.Error("Rótulo muito longo (máximo 50 caracteres)")
                settings.repeatDays.any { it !in listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb") } -> 
                    ValidationResult.Error("Dias da semana inválidos")
                settings.snoozeMinutes < 1 || settings.snoozeMinutes > 60 -> 
                    ValidationResult.Error("Snooze deve estar entre 1 e 60 minutos")
                else -> ValidationResult.Success
            }
        }
    }
    
    /**
     * Resultado de validação
     */
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}

/**
 * Tipo de atividade para alarmes
 */
enum class AlarmType {
    SINGLE,    // Alarme único
    REPEATING  // Alarme recorrente
}
