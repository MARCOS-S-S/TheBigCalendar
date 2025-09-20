package com.mss.thebigcalendar.data.repository

import android.content.Context
import android.util.Log
import com.mss.thebigcalendar.data.model.AlarmSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.time.LocalTime

/**
 * Repositório para gerenciar configurações de alarmes
 * Implementa padrão Repository para abstração de dados
 */
class AlarmRepository(
    private val context: Context
) {
    companion object {
        private const val TAG = "AlarmRepository"
        private const val PREFS_NAME = "alarm_preferences"
        private const val KEY_ALARMS = "alarms"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _alarms = MutableStateFlow<List<AlarmSettings>>(emptyList())
    val alarms: Flow<List<AlarmSettings>> = _alarms.asStateFlow()
    
    init {
        loadAlarms()
    }
    
    /**
     * Salva um novo alarme ou atualiza um existente
     */
    suspend fun saveAlarm(alarm: AlarmSettings): Result<AlarmSettings> {
        return try {
            Log.d(TAG, "💾 Salvando alarme: ${alarm.label} às ${alarm.time}")
            
            // Validar configurações
            val validation = AlarmSettings.validate(alarm)
            if (validation is AlarmSettings.ValidationResult.Error) {
                return Result.failure(Exception(validation.message))
            }
            
            val currentAlarms = _alarms.value.toMutableList()
            val existingIndex = currentAlarms.indexOfFirst { it.id == alarm.id }
            
            val updatedAlarm = alarm.copy(
                lastModified = System.currentTimeMillis()
            )
            
            if (existingIndex >= 0) {
                currentAlarms[existingIndex] = updatedAlarm
                Log.d(TAG, "💾 Alarme atualizado: ${updatedAlarm.id}")
            } else {
                currentAlarms.add(updatedAlarm)
                Log.d(TAG, "💾 Novo alarme criado: ${updatedAlarm.id}")
            }
            
            _alarms.value = currentAlarms
            persistAlarms(currentAlarms)
            
            Result.success(updatedAlarm)
        } catch (e: Exception) {
            Log.e(TAG, "💾 Erro ao salvar alarme", e)
            Result.failure(e)
        }
    }
    
    /**
     * Remove um alarme
     */
    suspend fun deleteAlarm(alarmId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🗑️ Removendo alarme: $alarmId")
            
            val currentAlarms = _alarms.value.toMutableList()
            val removed = currentAlarms.removeAll { it.id == alarmId }
            
            if (removed) {
                _alarms.value = currentAlarms
                persistAlarms(currentAlarms)
                Log.d(TAG, "🗑️ Alarme removido com sucesso")
                Result.success(Unit)
            } else {
                Log.w(TAG, "🗑️ Alarme não encontrado: $alarmId")
                Result.failure(Exception(context.getString(com.mss.thebigcalendar.R.string.alarm_not_found_error)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "🗑️ Erro ao remover alarme", e)
            Result.failure(e)
        }
    }
    
    /**
     * Busca um alarme por ID
     */
    suspend fun getAlarmById(alarmId: String): AlarmSettings? {
        return _alarms.value.find { it.id == alarmId }
    }
    
    /**
     * Busca alarmes ativos para um horário específico
     */
    suspend fun getActiveAlarmsAtTime(time: LocalTime): List<AlarmSettings> {
        return _alarms.value.filter { alarm ->
            alarm.isEnabled && 
            alarm.time == time &&
            (alarm.repeatDays.isEmpty() || isTodayInRepeatDays(alarm.repeatDays))
        }
    }
    
    /**
     * Busca todos os alarmes ativos
     */
    suspend fun getActiveAlarms(): List<AlarmSettings> {
        return _alarms.value.filter { it.isEnabled }
    }
    
    /**
     * Busca todos os alarmes (ativos e inativos)
     */
    suspend fun getAllAlarms(): List<AlarmSettings> {
        return _alarms.value
    }
    
    /**
     * Ativa/desativa um alarme
     */
    suspend fun toggleAlarm(alarmId: String): Result<AlarmSettings> {
        return try {
            val alarm = getAlarmById(alarmId)
            if (alarm != null) {
                val updatedAlarm = alarm.copy(
                    isEnabled = !alarm.isEnabled,
                    lastModified = System.currentTimeMillis()
                )
                saveAlarm(updatedAlarm)
            } else {
                Result.failure(Exception(context.getString(com.mss.thebigcalendar.R.string.alarm_not_found_error)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔄 Erro ao alternar alarme", e)
            Result.failure(e)
        }
    }
    
    /**
     * Carrega alarmes do armazenamento persistente
     */
    private fun loadAlarms() {
        try {
            val alarmsJson = prefs.getString(KEY_ALARMS, null)
            if (alarmsJson != null) {
                val alarms = parseAlarmsFromJson(alarmsJson)
                _alarms.value = alarms
                Log.d(TAG, "📂 ${alarms.size} alarmes carregados")
            } else {
                Log.d(TAG, "📂 Nenhum alarme encontrado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "📂 Erro ao carregar alarmes", e)
            _alarms.value = emptyList()
        }
    }
    
    /**
     * Persiste alarmes no armazenamento
     */
    private fun persistAlarms(alarms: List<AlarmSettings>) {
        try {
            val alarmsJson = convertAlarmsToJson(alarms)
            prefs.edit()
                .putString(KEY_ALARMS, alarmsJson)
                .apply()
            Log.d(TAG, "💾 ${alarms.size} alarmes persistidos")
        } catch (e: Exception) {
            Log.e(TAG, "💾 Erro ao persistir alarmes", e)
        }
    }
    
    /**
     * Converte lista de alarmes para JSON (implementação simples)
     */
    private fun convertAlarmsToJson(alarms: List<AlarmSettings>): String {
        return alarms.joinToString(
            separator = "|",
            prefix = "[",
            postfix = "]"
        ) { alarm ->
            "${alarm.id};${alarm.label};${alarm.time};${alarm.isEnabled};${alarm.repeatDays.joinToString(",")};${alarm.soundEnabled};${alarm.vibrationEnabled};${alarm.snoozeMinutes};${alarm.createdAt};${alarm.lastModified}"
        }
    }
    
    /**
     * Converte JSON para lista de alarmes (implementação simples)
     */
    private fun parseAlarmsFromJson(json: String): List<AlarmSettings> {
        return try {
            if (json == "[]") return emptyList()
            
            val content = json.removeSurrounding("[", "]")
            if (content.isEmpty()) return emptyList()
            
            content.split("|").mapNotNull { alarmStr ->
                try {
                    val parts = alarmStr.split(";")
                    if (parts.size >= 10) {
                        AlarmSettings(
                            id = parts[0],
                            label = parts[1],
                            time = LocalTime.parse(parts[2]),
                            isEnabled = parts[3].toBoolean(),
                            repeatDays = if (parts[4].isEmpty()) emptySet() else parts[4].split(",").toSet(),
                            soundEnabled = parts[5].toBoolean(),
                            vibrationEnabled = parts[6].toBoolean(),
                            snoozeMinutes = parts[7].toInt(),
                            createdAt = parts[8].toLong(),
                            lastModified = parts[9].toLong()
                        )
                    } else null
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erro ao parsear alarme: $alarmStr", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Erro ao parsear JSON de alarmes", e)
            emptyList()
        }
    }
    
    /**
     * Verifica se hoje está nos dias de repetição
     */
    private fun isTodayInRepeatDays(repeatDays: Set<String>): Boolean {
        val today = java.time.LocalDate.now()
        val dayOfWeek = when (today.dayOfWeek) {
            java.time.DayOfWeek.SUNDAY -> context.getString(com.mss.thebigcalendar.R.string.day_of_week_sunday)
            java.time.DayOfWeek.MONDAY -> context.getString(com.mss.thebigcalendar.R.string.day_of_week_monday)
            java.time.DayOfWeek.TUESDAY -> context.getString(com.mss.thebigcalendar.R.string.day_of_week_tuesday)
            java.time.DayOfWeek.WEDNESDAY -> context.getString(com.mss.thebigcalendar.R.string.day_of_week_wednesday)
            java.time.DayOfWeek.THURSDAY -> context.getString(com.mss.thebigcalendar.R.string.day_of_week_thursday)
            java.time.DayOfWeek.FRIDAY -> context.getString(com.mss.thebigcalendar.R.string.day_of_week_friday)
            java.time.DayOfWeek.SATURDAY -> context.getString(com.mss.thebigcalendar.R.string.day_of_week_saturday)
        }
        return repeatDays.contains(dayOfWeek)
    }
}
