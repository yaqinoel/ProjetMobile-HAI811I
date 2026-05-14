package com.example.traveling.data.repository

import com.example.traveling.data.model.RouteStop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class DirectionStepResult(
    val distanceText: String,
    val durationText: String,
    val polyline: String
)

class GoogleDirectionsService {

    suspend fun getDirections(
        stops: List<RouteStop>,
        apiKey: String
    ): List<DirectionStepResult> = withContext(Dispatchers.IO) {
        if (stops.size < 2) return@withContext emptyList()

        val validStops = stops.filter { it.lat != 0.0 && it.lng != 0.0 }
        if (validStops.size < 2) return@withContext emptyList()

        val origin = "${validStops.first().lat},${validStops.first().lng}"
        val destination = "${validStops.last().lat},${validStops.last().lng}"

        val waypoints = if (validStops.size > 2) {
            val wpList = validStops.subList(1, validStops.size - 1)
                .joinToString("|") { "${it.lat},${it.lng}" }
            "&waypoints=$wpList"
        } else {
            ""
        }

        val urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=$origin&destination=$destination$waypoints" +
                "&mode=walking&key=$apiKey&language=fr"

        val results = mutableListOf<DirectionStepResult>()

        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(responseString)
                val status = jsonObject.optString("status")

                if (status == "OK") {
                    val routes = jsonObject.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val legs = route.optJSONArray("legs")

                        if (legs != null) {
                            for (i in 0 until legs.length()) {
                                val leg = legs.getJSONObject(i)
                                val distanceText = leg.getJSONObject("distance").getString("text")
                                val durationText = leg.getJSONObject("duration").getString("text")

                                val steps = leg.optJSONArray("steps")

                                val stepPolylines = mutableListOf<String>()
                                if (steps != null) {
                                    for (j in 0 until steps.length()) {
                                        val step = steps.getJSONObject(j)
                                        val poly = step.getJSONObject("polyline").getString("points")
                                        stepPolylines.add(poly)
                                    }
                                }

                                results.add(
                                    DirectionStepResult(
                                        distanceText = distanceText,
                                        durationText = durationText,
                                        polyline = stepPolylines.joinToString("||")
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext results
    }
}
