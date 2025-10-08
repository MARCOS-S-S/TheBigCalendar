package com.mss.thebigcalendar.service

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.services.calendar.Calendar
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import com.mss.thebigcalendar.data.model.SyncPhase
import com.mss.thebigcalendar.data.model.SyncProgress
import com.mss.thebigcalendar.data.repository.ActivityRepository
import com.mss.thebigcalendar.data.repository.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class ProgressiveSyncService(
    private val context: Context,
    private val googleCalendarService: GoogleCalendarService
) {
    
    private val syncRepository = SyncRepository(context)
    
    companion object {
        private const val TAG = "ProgressiveSyncService"
    }
    
    /**
     * Sincronização progressiva: primeiro mês atual, depois resto do ano
     */
    suspend fun syncProgressively(
        account: GoogleSignInAccount,
        onProgressUpdate: (SyncProgress) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Fase 1: Sincronização rápida (mês atual + próximo mês)
            val quickSyncResult = performQuickSync(account, onProgressUpdate)
            if (quickSyncResult.isFailure) {
                return@withContext Result.failure(quickSyncResult.exceptionOrNull() ?: Exception("Falha na sincronização rápida"))
            }
            
            // Fase 2: Sincronização em background (resto do ano)
            val backgroundSyncResult = performBackgroundSync(account, onProgressUpdate)
            if (backgroundSyncResult.isFailure) {
                Log.w(TAG, "⚠️ Sincronização em background falhou, mas sincronização rápida foi bem-sucedida")
            }
            
            val totalEvents = quickSyncResult.getOrNull() ?: 0 + (backgroundSyncResult.getOrNull() ?: 0)
            
            // Atualizar timestamp da última sincronização
            syncRepository.updateLastSyncTime(System.currentTimeMillis())
            
            Result.success(totalEvents)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro na sincronização progressiva", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sincronização rápida: apenas mês atual e próximo mês
     */
    private suspend fun performQuickSync(
        account: GoogleSignInAccount,
        onProgressUpdate: (SyncProgress) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            onProgressUpdate(SyncProgress(
                currentStep = "Sincronizando mês atual...",
                progress = 10,
                totalEvents = 0,
                processedEvents = 0,
                currentPhase = SyncPhase.QUICK_SYNC
            ))
            
            val calendarService = googleCalendarService.getCalendarService(account)
            val now = LocalDate.now()
            val nextMonth = now.plusMonths(1)
            
            // Buscar eventos do mês atual e próximo com timeout
            val events = withTimeout(30.seconds) {
                fetchEventsForPeriod(calendarService, now, nextMonth, useIncrementalSync = true)
            }
            
            onProgressUpdate(SyncProgress(
                currentStep = "Processando eventos...",
                progress = 30,
                totalEvents = events.size,
                processedEvents = 0,
                currentPhase = SyncPhase.QUICK_SYNC
            ))
            
            // Converter e salvar eventos em lote
            val activities = convertEventsToActivities(events)
            val repository = ActivityRepository(context)
            
            onProgressUpdate(SyncProgress(
                currentStep = "Salvando eventos em lote...",
                progress = 50,
                totalEvents = activities.size,
                processedEvents = 0,
                currentPhase = SyncPhase.QUICK_SYNC
            ))
            
            // Salvar todos os eventos de uma vez
            repository.saveAllActivities(activities)
            
            onProgressUpdate(SyncProgress(
                currentStep = "Sincronização rápida concluída",
                progress = 70,
                totalEvents = activities.size,
                processedEvents = activities.size,
                currentPhase = SyncPhase.QUICK_SYNC
            ))
            
            onProgressUpdate(SyncProgress(
                currentStep = "Sincronização rápida concluída",
                progress = 70,
                totalEvents = activities.size,
                processedEvents = activities.size,
                currentPhase = SyncPhase.QUICK_SYNC
            ))
            
            Result.success(activities.size)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro na sincronização rápida", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sincronização em background: resto do ano
     */
    private suspend fun performBackgroundSync(
        account: GoogleSignInAccount,
        onProgressUpdate: (SyncProgress) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            onProgressUpdate(SyncProgress(
                currentStep = "Sincronizando resto do ano...",
                progress = 75,
                totalEvents = 0,
                processedEvents = 0,
                currentPhase = SyncPhase.BACKGROUND_SYNC
            ))
            
            val calendarService = googleCalendarService.getCalendarService(account)
            val now = LocalDate.now()
            val endOfYear = now.withMonth(12).withDayOfMonth(31)
            
            // Buscar eventos do resto do ano com timeout
            val events = withTimeout(60.seconds) {
                fetchEventsForPeriod(calendarService, now.plusMonths(2), endOfYear, useIncrementalSync = true)
            }
            
            onProgressUpdate(SyncProgress(
                currentStep = "Processando eventos restantes...",
                progress = 80,
                totalEvents = events.size,
                processedEvents = 0,
                currentPhase = SyncPhase.BACKGROUND_SYNC
            ))
            
            // Converter e salvar eventos em lote
            val activities = convertEventsToActivities(events)
            val repository = ActivityRepository(context)
            
            onProgressUpdate(SyncProgress(
                currentStep = "Salvando eventos restantes em lote...",
                progress = 85,
                totalEvents = activities.size,
                processedEvents = 0,
                currentPhase = SyncPhase.BACKGROUND_SYNC
            ))
            
            // Salvar todos os eventos de uma vez
            repository.saveAllActivities(activities)
            
            onProgressUpdate(SyncProgress(
                currentStep = "Sincronização concluída",
                progress = 100,
                totalEvents = activities.size,
                processedEvents = activities.size,
                currentPhase = SyncPhase.BACKGROUND_SYNC
            ))
            
            onProgressUpdate(SyncProgress(
                currentStep = "Sincronização concluída",
                progress = 100,
                totalEvents = activities.size,
                processedEvents = activities.size,
                currentPhase = SyncPhase.BACKGROUND_SYNC
            ))
            
            Result.success(activities.size)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro na sincronização em background", e)
            Result.failure(e)
        }
    }
    
    /**
     * Busca eventos para um período específico com paginação e sincronização incremental
     */
    private suspend fun fetchEventsForPeriod(
        calendarService: Calendar,
        startDate: LocalDate,
        endDate: LocalDate,
        useIncrementalSync: Boolean = false
    ): List<com.google.api.services.calendar.model.Event> = withContext(Dispatchers.IO) {
        try {
            val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTime = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            // Para sincronização incremental, usar timestamp da última sincronização
            val lastSyncTime = if (useIncrementalSync) {
                syncRepository.getLastSyncTime()
            } else {
                0L
            }
            
            val allEvents = mutableListOf<com.google.api.services.calendar.model.Event>()
            var pageToken: String? = null
            var pageCount = 0
            
            do {
                pageCount++
                
                val events = withTimeout(15.seconds) {
                    val request = calendarService.events()
                        .list("primary")
                        .setTimeMin(com.google.api.client.util.DateTime(startTime))
                        .setTimeMax(com.google.api.client.util.DateTime(endTime))
                        .setMaxResults(2500) // Limite da API
                        .setPageToken(pageToken)
                    
                    // Adicionar filtro incremental se necessário
                    if (useIncrementalSync && lastSyncTime > 0) {
                        request.setUpdatedMin(com.google.api.client.util.DateTime(lastSyncTime))
                    }
                    
                    request.execute()
                }
                
                val pageEvents = events.items ?: emptyList()
                allEvents.addAll(pageEvents)
                
                pageToken = events.nextPageToken
                
                // Limite de segurança para evitar loop infinito
                if (pageCount > 10) {
                    Log.w(TAG, "⚠️ Limite de páginas atingido (10), parando paginação")
                    break
                }
                
            } while (pageToken != null)
            
            allEvents
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar eventos para período", e)
            emptyList()
        }
    }
    
    /**
     * Converte eventos do Google para atividades do app
     */
    private fun convertEventsToActivities(events: List<com.google.api.services.calendar.model.Event>): List<Activity> {
        return events.mapNotNull { event ->
            try {
                val start = event.start?.dateTime?.value ?: event.start?.date?.value ?: return@mapNotNull null
                val end = event.end?.dateTime?.value ?: event.end?.date?.value
                
                val startDate = if (event.start?.dateTime == null) {
                    Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate()
                } else {
                    Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate()
                }
                
                val startTime = if (event.start?.dateTime != null) {
                    Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalTime()
                } else null
                
                val endTime = if (end != null && event.end?.dateTime != null) {
                    Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).toLocalTime()
                } else null
                
                val isBirthday = detectBirthdayEvent(event)
                
                Activity(
                    id = event.id ?: UUID.randomUUID().toString(),
                    title = event.summary ?: "Sem título",
                    description = event.description,
                    date = startDate.toString(),
                    startTime = startTime,
                    endTime = endTime,
                    isAllDay = event.start?.dateTime == null,
                    location = event.location,
                    categoryColor = if (isBirthday) "#FF69B4" else "#4285F4",
                    activityType = if (isBirthday) ActivityType.BIRTHDAY else ActivityType.EVENT,
                    recurrenceRule = event.recurrence?.firstOrNull(),
                    showInCalendar = true,
                    isFromGoogle = true,
                    excludedDates = emptyList(),
                    wikipediaLink = null // Eventos do Google Calendar não têm links da Wikipedia
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao converter evento: ${event.summary}", e)
                null
            }
        }
    }
    
    /**
     * Detecta se um evento é um aniversário
     */
    private fun detectBirthdayEvent(event: com.google.api.services.calendar.model.Event): Boolean {
        return event.start?.dateTime == null && // Evento de dia inteiro
               (event.summary?.contains("aniversário", ignoreCase = true) == true ||
                event.summary?.contains("birthday", ignoreCase = true) == true ||
                event.description?.contains("aniversário", ignoreCase = true) == true)
    }
}
