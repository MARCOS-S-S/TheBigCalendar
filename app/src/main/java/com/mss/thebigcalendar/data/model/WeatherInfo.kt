package com.mss.thebigcalendar.data.model

data class WeatherInfo(
    val temperature: Int,
    val condition: WeatherCondition,
    val emoji: String,
    val description: String
)

enum class WeatherCondition {
    SUNNY,
    PARTLY_CLOUDY,
    CLOUDY,
    RAINY,
    SNOWY,
    THUNDERSTORM,
    FOGGY,
    WINDY
}

object WeatherEmoji {
    fun getEmoji(condition: WeatherCondition): String {
        return when (condition) {
            WeatherCondition.SUNNY -> "☀️"
            WeatherCondition.PARTLY_CLOUDY -> "⛅"
            WeatherCondition.CLOUDY -> "☁️"
            WeatherCondition.RAINY -> "🌧️"
            WeatherCondition.SNOWY -> "❄️"
            WeatherCondition.THUNDERSTORM -> "⛈️"
            WeatherCondition.FOGGY -> "🌫️"
            WeatherCondition.WINDY -> "💨"
        }
    }
}

