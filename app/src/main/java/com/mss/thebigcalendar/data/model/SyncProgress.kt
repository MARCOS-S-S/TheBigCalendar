package com.mss.thebigcalendar.data.model

data class SyncProgress(
    val currentStep: String,
    val progress: Int, // 0-100
    val totalEvents: Int,
    val processedEvents: Int,
    val currentPhase: SyncPhase = SyncPhase.IDLE
)

enum class SyncPhase {
    IDLE,
    QUICK_SYNC,      // Mês atual + próximo mês
    BACKGROUND_SYNC,  // Resto do ano
    FULL_SYNC        // Sincronização completa
}
