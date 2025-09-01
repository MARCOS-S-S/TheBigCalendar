package com.mss.thebigcalendar.ui.components

import java.time.LocalTime

/**
 * Serviço para gerenciar mensagens de boas-vindas baseadas no horário do dia
 */
object GreetingService {
    
    /**
     * Obtém a mensagem de boas-vindas apropriada baseada no horário atual
     */
    fun getGreetingMessage(): String {
        val currentTime = LocalTime.now()
        val hour = currentTime.hour
        
        return when (hour) {
            in 5..11 -> "Bom dia"
            in 12..17 -> "Boa tarde"
            in 18..23 -> "Boa noite"
            else -> "Boa madrugada" // 0-4
        }
    }
    
    /**
     * Obtém a mensagem de boas-vindas com nome do usuário (se disponível)
     */
    fun getGreetingMessage(userName: String? = null): String {
        val greeting = getGreetingMessage()
        return if (userName != null && userName.isNotBlank()) {
            "$greeting, $userName!"
        } else {
            "$greeting!"
        }
    }
    
    /**
     * Obtém um emoji correspondente ao horário do dia
     */
    fun getGreetingEmoji(): String {
        val currentTime = LocalTime.now()
        val hour = currentTime.hour
        
        return when (hour) {
            in 5..11 -> "🌅" // Manhã
            in 12..17 -> "☀️" // Tarde
            in 18..23 -> "🌙" // Noite
            else -> "🌃" // Madrugada
        }
    }
    
    /**
     * Obtém uma mensagem completa com emoji e saudação
     */
    fun getFullGreetingMessage(userName: String? = null): String {
        val emoji = getGreetingEmoji()
        val greeting = getGreetingMessage(userName)
        return "$emoji $greeting"
    }
}
