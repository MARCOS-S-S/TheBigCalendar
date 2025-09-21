package com.mss.thebigcalendar.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Representa um calendário importado de arquivo JSON
 */
data class JsonCalendar(
    val id: String,
    val title: String,
    val color: Color,
    val fileName: String,
    val importDate: Long,
    val isVisible: Boolean = true
)

/**
 * Converte JsonCalendar para string de cor
 */
fun JsonCalendar.colorToString(): String {
    return String.format("#%08X", color.toArgb())
}

/**
 * Converte string de cor para JsonCalendar
 */
fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color.Blue // Cor padrão
    }
}
