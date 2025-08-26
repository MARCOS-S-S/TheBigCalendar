package com.mss.thebigcalendar.data.repository

import com.mss.thebigcalendar.data.model.WeatherCondition
import com.mss.thebigcalendar.data.model.WeatherEmoji
import com.mss.thebigcalendar.data.model.WeatherInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Random

class WeatherRepository {
    
    /**
     * Obtém a previsão do tempo atual
     * Por enquanto retorna dados mockados, mas pode ser facilmente expandido
     * para usar APIs reais como OpenWeatherMap, WeatherAPI, etc.
     */
    fun getCurrentWeather(): Flow<WeatherInfo> = flow {
        // Simular delay de rede
        kotlinx.coroutines.delay(500)
        
        // Gerar dados mockados realistas
        val weatherInfo = generateMockWeather()
        emit(weatherInfo)
    }
    
    /**
     * Gera dados mockados de previsão do tempo
     * Em uma implementação real, isso seria substituído por chamadas de API
     */
    private fun generateMockWeather(): WeatherInfo {
        val random = Random()
        
        // Simular variação sazonal de temperatura
        val currentMonth = java.time.LocalDate.now().monthValue
        val baseTemp = when (currentMonth) {
            in 12..2 -> random.nextInt(-5, 15) // Inverno
            in 3..5 -> random.nextInt(15, 25)  // Primavera
            in 6..8 -> random.nextInt(25, 35)  // Verão
            else -> random.nextInt(20, 30)     // Outono
        }
        
        // Distribuir condições climáticas de forma realista
        val condition = when (random.nextInt(100)) {
            in 0..40 -> WeatherCondition.SUNNY
            in 41..60 -> WeatherCondition.PARTLY_CLOUDY
            in 61..75 -> WeatherCondition.CLOUDY
            in 76..85 -> WeatherCondition.RAINY
            in 86..90 -> WeatherCondition.THUNDERSTORM
            in 91..95 -> WeatherCondition.FOGGY
            else -> WeatherCondition.WINDY
        }
        
        val emoji = WeatherEmoji.getEmoji(condition)
        val description = getWeatherDescription(condition)
        
        return WeatherInfo(
            temperature = baseTemp,
            condition = condition,
            emoji = emoji,
            description = description
        )
    }
    
    private fun getWeatherDescription(condition: WeatherCondition): String {
        return when (condition) {
            WeatherCondition.SUNNY -> "Ensolarado"
            WeatherCondition.PARTLY_CLOUDY -> "Parcialmente nublado"
            WeatherCondition.CLOUDY -> "Nublado"
            WeatherCondition.RAINY -> "Chuvoso"
            WeatherCondition.SNOWY -> "Nevando"
            WeatherCondition.THUNDERSTORM -> "Tempestade"
            WeatherCondition.FOGGY -> "Neblina"
            WeatherCondition.WINDY -> "Ventoso"
        }
    }
    
    /**
     * Método para implementação futura com API real
     * Exemplo de como seria com OpenWeatherMap:
     */
    /*
    suspend fun getRealWeatherFromAPI(latitude: Double, longitude: Double): WeatherInfo {
        // Implementar chamada real para API
        // val response = weatherApiService.getCurrentWeather(lat, lon, apiKey)
        // return response.toWeatherInfo()
        
        // Por enquanto, retorna mock
        return generateMockWeather()
    }
    */
}

