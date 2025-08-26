# Implementação de API de Previsão do Tempo

## Visão Geral
Este projeto atualmente usa dados mockados para a previsão do tempo no widget. Para implementar dados reais, você pode integrar com várias APIs gratuitas e pagas.

## APIs Recomendadas

### 1. OpenWeatherMap (Gratuita - 1000 chamadas/dia)
- **URL**: https://openweathermap.org/api
- **Vantagens**: Gratuita, boa documentação, suporte a português
- **Desvantagens**: Limite de chamadas, dados básicos na versão gratuita

### 2. WeatherAPI.com (Gratuita - 1.000.000 chamadas/mês)
- **URL**: https://www.weatherapi.com/
- **Vantagens**: Muito generosa na versão gratuita, dados detalhados
- **Desvantagens**: Pode ser lenta às vezes

### 3. AccuWeather (Paga)
- **URL**: https://developer.accuweather.com/
- **Vantagens**: Dados muito precisos, boa cobertura global
- **Desvantagens**: Paga, limite de chamadas

## Implementação com OpenWeatherMap

### 1. Adicionar Dependências
```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
}
```

### 2. Criar Modelos de Resposta
```kotlin
data class OpenWeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val name: String
)

data class Main(
    val temp: Double,
    val humidity: Int,
    val pressure: Int
)

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)
```

### 3. Criar Interface da API
```kotlin
interface WeatherApiService {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "pt_br"
    ): OpenWeatherResponse
}
```

### 4. Atualizar WeatherRepository
```kotlin
class WeatherRepository {
    private val weatherApiService: WeatherApiService
    
    suspend fun getRealWeatherFromAPI(latitude: Double, longitude: Double): WeatherInfo {
        try {
            val response = weatherApiService.getCurrentWeather(
                lat = latitude,
                lon = longitude,
                apiKey = BuildConfig.WEATHER_API_KEY
            )
            
            return response.toWeatherInfo()
        } catch (e: Exception) {
            // Fallback para dados mockados
            return generateMockWeather()
        }
    }
    
    private fun OpenWeatherResponse.toWeatherInfo(): WeatherInfo {
        val condition = when (weather.firstOrNull()?.id) {
            in 200..299 -> WeatherCondition.THUNDERSTORM
            in 300..399 -> WeatherCondition.RAINY
            in 500..599 -> WeatherCondition.RAINY
            in 600..699 -> WeatherCondition.SNOWY
            in 700..799 -> WeatherCondition.FOGGY
            in 800..800 -> WeatherCondition.SUNNY
            in 801..899 -> WeatherCondition.PARTLY_CLOUDY
            else -> WeatherCondition.CLOUDY
        }
        
        return WeatherInfo(
            temperature = main.temp.toInt(),
            condition = condition,
            emoji = WeatherEmoji.getEmoji(condition),
            description = weather.firstOrNull()?.description ?: "Desconhecido"
        )
    }
}
```

### 5. Configurar API Key
```kotlin
// local.properties (não commitar no git)
WEATHER_API_KEY=sua_chave_aqui

// app/build.gradle.kts
android {
    buildTypes {
        debug {
            buildConfigField("String", "WEATHER_API_KEY", "\"${properties['WEATHER_API_KEY']}\"")
        }
        release {
            buildConfigField("String", "WEATHER_API_KEY", "\"${properties['WEATHER_API_KEY']}\"")
        }
    }
}
```

## Implementação com Localização

### 1. Adicionar Permissões
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

### 2. Implementar Localização
```kotlin
class LocationManager(private val context: Context) {
    suspend fun getCurrentLocation(): Pair<Double, Double>? {
        // Implementar lógica de localização
        // Retornar latitude e longitude
    }
}
```

### 3. Atualizar Widget
```kotlin
private fun updateWeatherInfo(context: Context, views: RemoteViews, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val locationManager = LocationManager(context)
            val location = locationManager.getCurrentLocation()
            
            if (location != null) {
                val (lat, lon) = location
                val weatherRepository = WeatherRepository()
                val weatherInfo = weatherRepository.getRealWeatherFromAPI(lat, lon)
                
                val weatherText = "${weatherInfo.emoji} ${weatherInfo.temperature}°C"
                
                CoroutineScope(Dispatchers.Main).launch {
                    views.setTextViewText(R.id.widget_weather, weatherText)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } else {
                // Fallback para dados mockados
                val mockWeather = generateMockWeather()
                val weatherText = "${mockWeather.emoji} ${mockWeather.temperature}°C"
                
                CoroutineScope(Dispatchers.Main).launch {
                    views.setTextViewText(R.id.widget_weather, weatherText)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        } catch (e: Exception) {
            // Tratamento de erro
        }
    }
}
```

## Considerações de Performance

1. **Cache**: Implementar cache local para evitar chamadas desnecessárias
2. **Atualização**: Atualizar apenas a cada 30-60 minutos
3. **Fallback**: Sempre ter dados mockados como backup
4. **Tratamento de Erro**: Implementar retry e fallback robustos

## Próximos Passos

1. Escolher uma API (recomendo OpenWeatherMap para começar)
2. Implementar os modelos de resposta
3. Configurar Retrofit e fazer as chamadas
4. Implementar cache local
5. Adicionar tratamento de erros robusto
6. Testar em diferentes condições de rede

