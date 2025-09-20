package com.mss.thebigcalendar.data.model

import java.time.LocalDateTime

/**
 * Representa uma atividade que foi apagada e está na lixeira
 */
data class DeletedActivity(
    val id: String,
    val originalActivity: Activity,
    val deletedAt: LocalDateTime = LocalDateTime.now(),
    val deletedBy: String = "Usuário"
)
