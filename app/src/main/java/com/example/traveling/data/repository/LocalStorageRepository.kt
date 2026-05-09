package com.example.traveling.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.traveling.data.model.RouteStop
import com.example.traveling.data.model.TimeSlot
import com.example.traveling.data.model.TravelRoute
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages local persistence for saved/liked routes and offline caching.
 * Uses SharedPreferences for lightweight key-value storage.
 */
class LocalStorageRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("traveling_prefs", Context.MODE_PRIVATE)

    // ─── Like / Save ───

    fun isRouteLiked(routeId: String): Boolean =
        prefs.getStringSet("liked_routes", emptySet())?.contains(routeId) == true

    fun toggleRouteLike(routeId: String): Boolean {
        val set = prefs.getStringSet("liked_routes", emptySet())?.toMutableSet() ?: mutableSetOf()
        val nowLiked = if (set.contains(routeId)) {
            set.remove(routeId); false
        } else {
            set.add(routeId); true
        }
        prefs.edit().putStringSet("liked_routes", set).apply()
        return nowLiked
    }

    fun isRouteSaved(routeId: String): Boolean =
        prefs.getStringSet("saved_routes", emptySet())?.contains(routeId) == true

    fun toggleRouteSave(routeId: String): Boolean {
        val set = prefs.getStringSet("saved_routes", emptySet())?.toMutableSet() ?: mutableSetOf()
        val nowSaved = if (set.contains(routeId)) {
            set.remove(routeId); false
        } else {
            set.add(routeId); true
        }
        prefs.edit().putStringSet("saved_routes", set).apply()
        return nowSaved
    }

    // ─── Offline Route Caching ───

    fun cacheRoute(routeKey: String, route: TravelRoute, stops: List<RouteStop>) {
        val json = JSONObject().apply {
            put("route", routeToJson(route))
            put("stops", JSONArray(stops.map { stopToJson(it) }))
        }
        prefs.edit().putString("cache_route_$routeKey", json.toString()).apply()

        // Track cached keys
        val keys = prefs.getStringSet("cached_route_keys", emptySet())?.toMutableSet() ?: mutableSetOf()
        keys.add(routeKey)
        prefs.edit().putStringSet("cached_route_keys", keys).apply()
    }

    fun getCachedRoute(routeKey: String): Pair<TravelRoute, List<RouteStop>>? {
        val raw = prefs.getString("cache_route_$routeKey", null) ?: return null
        return try {
            val json = JSONObject(raw)
            val route = jsonToRoute(json.getJSONObject("route"))
            val stopsArray = json.getJSONArray("stops")
            val stops = (0 until stopsArray.length()).map { jsonToStop(stopsArray.getJSONObject(it)) }
            route to stops
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getCachedRouteKeys(): Set<String> =
        prefs.getStringSet("cached_route_keys", emptySet()) ?: emptySet()

    // ─── JSON serialization helpers ───

    private fun routeToJson(r: TravelRoute) = JSONObject().apply {
        put("id", r.id); put("name", r.name); put("subtitle", r.subtitle)
        put("budget", r.budget); put("duration", r.duration)
        put("effort", r.effort); put("effortLevel", r.effortLevel)
        put("stops", r.stops); put("rating", r.rating.toDouble())
        put("reviews", r.reviews); put("imageUrl", r.imageUrl)
        put("highlights", JSONArray(r.highlights))
        put("gradientColor1", r.gradientColors.first)
        put("gradientColor2", r.gradientColors.second)
    }

    private fun jsonToRoute(j: JSONObject) = TravelRoute(
        id = j.getString("id"), name = j.getString("name"),
        subtitle = j.getString("subtitle"), budget = j.getInt("budget"),
        duration = j.getString("duration"), effort = j.getString("effort"),
        effortLevel = j.getInt("effortLevel"), stops = j.getInt("stops"),
        rating = j.getDouble("rating").toFloat(), reviews = j.getInt("reviews"),
        imageUrl = j.getString("imageUrl"),
        highlights = (0 until j.getJSONArray("highlights").length()).map {
            j.getJSONArray("highlights").getString(it)
        },
        gradientColors = Pair(j.getLong("gradientColor1"), j.getLong("gradientColor2"))
    )

    private fun stopToJson(s: RouteStop) = JSONObject().apply {
        put("id", s.id); put("name", s.name); put("type", s.type)
        put("timeSlot", s.timeSlot.name); put("arrivalTime", s.arrivalTime)
        put("duration", s.duration); put("distance", s.distance)
        put("walkTime", s.walkTime); put("cost", s.cost)
        put("description", s.description); put("imageUrl", s.imageUrl)
        put("rating", s.rating.toDouble()); put("openHours", s.openHours)
        put("lat", s.lat); put("lng", s.lng)
        put("polylineToNext", s.polylineToNext ?: "")
    }

    private fun jsonToStop(j: JSONObject) = RouteStop(
        id = j.getString("id"), name = j.getString("name"),
        type = j.getString("type"),
        timeSlot = TimeSlot.valueOf(j.getString("timeSlot")),
        arrivalTime = j.getString("arrivalTime"),
        duration = j.getString("duration"), distance = j.getString("distance"),
        walkTime = j.getString("walkTime"), cost = j.getInt("cost"),
        description = j.getString("description"), imageUrl = j.getString("imageUrl"),
        rating = j.getDouble("rating").toFloat(), openHours = j.getString("openHours"),
        lat = j.getDouble("lat"), lng = j.getDouble("lng"),
        polylineToNext = j.optString("polylineToNext", null)
    )
}
