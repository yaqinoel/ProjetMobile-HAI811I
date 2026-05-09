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

    /**
     * Fetches real directions between consecutive stops.
     * We do sequential pairwise requests or a single request with waypoints.
     * A single request with waypoints is cheaper but only gives one overall polyline and legs.
     * Since we need distance/time between EACH stop and the next, we can use the "legs" array
     * returned by a waypoint request.
     */
    suspend fun getDirections(
        stops: List<RouteStop>,
        apiKey: String
    ): List<DirectionStepResult> = withContext(Dispatchers.IO) {
        if (stops.size < 2) return@withContext emptyList()

        // Extract valid coordinates
        val validStops = stops.filter { it.lat != 0.0 && it.lng != 0.0 }
        if (validStops.size < 2) return@withContext emptyList()

        val origin = "${validStops.first().lat},${validStops.first().lng}"
        val destination = "${validStops.last().lat},${validStops.last().lng}"

        // Waypoints (all stops between first and last)
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
                                
                                // To get the polyline for this specific leg, we have to iterate through steps
                                val steps = leg.optJSONArray("steps")
                                // We can just concatenate the step polylines, or take the overall route polyline 
                                // but we need polyline per stop. It's easier to just take the step end locations
                                // Wait, PolyUtil can decode them. But let's just collect all points.
                                // Actually, for simplicity, we can just grab the overall overview_polyline and 
                                // assign it to the first stop, or we can extract each leg's polyline.
                                // Let's extract points from each step of the leg.
                                // Wait, the Directions API doesn't provide a single polyline per leg directly.
                                // We have to combine the steps, OR we can just use the overall polyline and 
                                // not worry about segmenting it. But wait, `PolyUtil.decode` decodes a string.
                                // Let's just grab the steps and we'll decode them in the UI.
                                
                                // Actually, there is no "polyline" per leg in the JSON. 
                                // Each `step` has a `polyline.points`.
                                // Let's just return a list of polylines (one per step) joined by a comma, 
                                // or better, we can just extract the points directly if we used Maps Utils here,
                                // but we don't have Maps Utils here (it's Android side).
                                // Let's just collect the step polyline strings and join them with a delimiter 
                                // like "||", so the UI can split and decode them.
                                
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
