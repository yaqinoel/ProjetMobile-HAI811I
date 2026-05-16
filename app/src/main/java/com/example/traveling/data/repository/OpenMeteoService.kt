package com.example.traveling.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherInfo(
    val temperature: Double,
    val weatherCode: Int,
    val description: String,
    val emoji: String,
    val advice: String
)

class OpenMeteoService {

    suspend fun getWeather(lat: Double, lng: Double): WeatherInfo? = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=$lat&longitude=$lng" +
                    "&current=temperature_2m,weather_code" +
                    "&timezone=auto"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m")
                val weatherCode = current.getInt("weather_code")

                // code météo open-meteo transformé en texte lisible pour l'utilisateur
                val (desc, emoji, advice) = interpretWeatherCode(weatherCode, temp)
                return@withContext WeatherInfo(temp, weatherCode, desc, emoji, advice)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private fun interpretWeatherCode(code: Int, temp: Double): Triple<String, String, String> {
        // quelques conseils simples, suffisants pour guider le choix du parcours
        return when (code) {
            0 -> Triple("Ciel dégagé", "☀️",
                if (temp > 28) "Pensez à vous hydrater et à chercher l'ombre"
                else "Idéal pour les visites en plein air")
            1, 2 -> Triple("Partiellement nuageux", "⛅",
                "Bonnes conditions pour les activités extérieures")
            3 -> Triple("Couvert", "☁️",
                "Temps agréable pour marcher sans trop de soleil")
            45, 48 -> Triple("Brouillard", "🌫️",
                "Visibilité réduite, privilégiez les lieux intérieurs")
            51, 53, 55 -> Triple("Bruine", "🌦️",
                "Prenez un parapluie léger")
            61, 63, 65 -> Triple("Pluie", "🌧️",
                "Privilégiez les musées et lieux couverts")
            66, 67 -> Triple("Pluie verglaçante", "🌨️",
                "Attention aux surfaces glissantes")
            71, 73, 75, 77 -> Triple("Neige", "❄️",
                "Couvrez-vous bien et choisissez des lieux intérieurs")
            80, 81, 82 -> Triple("Averses", "🌦️",
                "Emportez un imperméable")
            85, 86 -> Triple("Averses de neige", "🌨️",
                "Restez au chaud si possible")
            95 -> Triple("Orage", "⛈️",
                "Restez à l'intérieur, orages en cours")
            96, 99 -> Triple("Orage avec grêle", "⛈️",
                "Danger ! Restez à l'abri")
            else -> Triple("Variable", "🌤️",
                "Consultez les prévisions locales")
        }
    }
}
