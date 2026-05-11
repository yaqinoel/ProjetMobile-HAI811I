package com.example.traveling.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.traveling.data.model.RouteStop
import com.example.traveling.data.model.TimeSlot
import com.example.traveling.data.model.TravelRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages local persistence for saved/liked routes and offline caching.
 * Uses SharedPreferences for lightweight key-value storage.
 */
class LocalStorageRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("traveling_prefs", Context.MODE_PRIVATE)

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

    /** Store lightweight route metadata so profile can display it */
    fun storeRouteInfo(route: TravelRoute, destName: String) {
        val effectiveDestName = destName.ifBlank { route.destName }
        val json = JSONObject().apply {
            put("id", route.id)
            put("name", route.name)
            put("subtitle", route.subtitle)
            put("budget", route.budget)
            put("duration", route.duration)
            put("stops", route.stops)
            put("rating", route.rating.toDouble())
            put("imageUrl", route.imageUrl)
            put("destName", effectiveDestName)
        }
        prefs.edit().putString("route_info_${route.id}", json.toString()).apply()
    }

    data class RouteInfoSummary(
        val id: String,
        val name: String,
        val subtitle: String,
        val budget: Int,
        val duration: String,
        val stops: Int,
        val rating: Float,
        val imageUrl: String,
        val destName: String
    )

    private fun getRouteInfo(routeId: String): RouteInfoSummary? {
        val raw = prefs.getString("route_info_$routeId", null) ?: return null
        return try {
            val j = JSONObject(raw)
            RouteInfoSummary(
                id = j.getString("id"), name = j.getString("name"),
                subtitle = j.optString("subtitle", ""),
                budget = j.optInt("budget", 0),
                duration = j.optString("duration", ""),
                stops = j.optInt("stops", 0),
                rating = j.optDouble("rating", 0.0).toFloat(),
                imageUrl = j.optString("imageUrl", ""),
                destName = j.optString("destName", "")
            )
        } catch (e: Exception) { null }
    }

    fun getLikedRoutes(): List<RouteInfoSummary> {
        val ids = prefs.getStringSet("liked_routes", emptySet()) ?: emptySet()
        return ids.mapNotNull { getRouteInfo(it) }
    }

    fun getSavedRoutes(): List<RouteInfoSummary> {
        val ids = prefs.getStringSet("saved_routes", emptySet()) ?: emptySet()
        return ids.mapNotNull { getRouteInfo(it) }
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

    suspend fun cacheRouteLite(routeKey: String, route: TravelRoute, stops: List<RouteStop>) {
        withContext(Dispatchers.IO) {
            val mediaMap = mutableMapOf<String, String>()
            val urls = buildList {
                add(route.imageUrl)
                stops.forEach { stop ->
                    add(stop.imageUrl)
                    addAll(stop.imageUrls)
                }
            }.filter { it.startsWith("http", ignoreCase = true) }.distinct()

            urls.forEachIndexed { index, url ->
                cacheCompressedMedia(url, "${routeKey}_$index")?.let { localPath ->
                    mediaMap[url] = localPath
                }
            }

            val offlineRoute = route.copy(imageUrl = mediaMap[route.imageUrl] ?: route.imageUrl)
            val offlineStops = stops.map { stop ->
                val offlineImageUrls = stop.imageUrls.map { mediaMap[it] ?: it }
                stop.copy(
                    imageUrl = mediaMap[stop.imageUrl] ?: stop.imageUrl,
                    imageUrls = offlineImageUrls.ifEmpty {
                        listOf(mediaMap[stop.imageUrl] ?: stop.imageUrl).filter { it.isNotBlank() }
                    }
                )
            }

            cacheRoute(routeKey, offlineRoute, offlineStops)
        }
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
        put("destName", r.destName)
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
        subtitle = j.getString("subtitle"),
        destName = j.optString("destName", ""),
        budget = j.getInt("budget"),
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
        put("imageUrls", JSONArray(s.imageUrls))
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
        imageUrls = j.optJSONArray("imageUrls")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: listOf(j.getString("imageUrl")).filter { it.isNotBlank() },
        rating = j.getDouble("rating").toFloat(), openHours = j.getString("openHours"),
        lat = j.getDouble("lat"), lng = j.getDouble("lng"),
        polylineToNext = j.optString("polylineToNext", null)
    )

    private fun cacheCompressedMedia(url: String, key: String): String? {
        return runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 12000
                instanceFollowRedirects = true
            }

            connection.inputStream.use { input ->
                val bitmap = BitmapFactory.decodeStream(input) ?: return null
                val dir = File(appContext.cacheDir, "travelpath_media").apply { mkdirs() }
                val file = File(dir, "${sanitizeFileName(key)}.jpg")
                file.outputStream().use { output ->
                    val scaled = scaleBitmap(bitmap, maxWidth = 720)
                    scaled.compress(Bitmap.CompressFormat.JPEG, 68, output)
                    if (scaled !== bitmap) scaled.recycle()
                    bitmap.recycle()
                }
                file.absolutePath
            }
        }.getOrNull()
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val ratio = maxWidth.toFloat() / bitmap.width.toFloat()
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, maxWidth, height, true)
    }

    private fun sanitizeFileName(value: String): String =
        value.replace(Regex("[^A-Za-z0-9_-]"), "_").take(80)
}
