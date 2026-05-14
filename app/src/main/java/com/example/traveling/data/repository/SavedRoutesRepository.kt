package com.example.traveling.data.repository

import com.example.traveling.data.model.RouteStop
import com.example.traveling.data.model.SavedRouteDocument
import com.example.traveling.data.model.SavedRouteStopEntry
import com.example.traveling.data.model.TimeSlot
import com.example.traveling.data.model.TravelRoute
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SavedRoutesRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun userId(): String? = auth.currentUser?.uid

    private fun collection() = userId()?.let {
        db.collection("users").document(it).collection("saved_routes")
    }

    suspend fun saveRoute(
        route: TravelRoute,
        stops: List<RouteStop>,
        destName: String,
        type: String
    ) {
        val col = collection() ?: return
        val docId = "${type}_${route.id}"

        val doc = SavedRouteDocument(
            routeId = route.id,
            type = type,
            destName = destName,
            routeName = route.name,
            routeSubtitle = route.subtitle,
            budget = route.budget,
            duration = route.duration,
            effort = route.effort,
            effortLevel = route.effortLevel,
            stopCount = route.stops,
            rating = route.rating.toDouble(),
            reviews = route.reviews,
            imageUrl = route.imageUrl,
            highlights = route.highlights,
            gradientColor1 = route.gradientColors.first,
            gradientColor2 = route.gradientColors.second,
            stops = stops.map { s ->
                SavedRouteStopEntry(
                    id = s.id, name = s.name, type = s.type,
                    timeSlot = s.timeSlot.name,
                    arrivalTime = s.arrivalTime, duration = s.duration,
                    distance = s.distance, walkTime = s.walkTime,
                    cost = s.cost, description = s.description,
                    imageUrl = s.imageUrl, rating = s.rating.toDouble(),
                    imageUrls = s.imageUrls,
                    openHours = s.openHours, lat = s.lat, lng = s.lng,
                    polylineToNext = s.polylineToNext ?: "",
                    source = s.source,
                    sourcePostId = s.sourcePostId
                )
            },
            savedAt = System.currentTimeMillis()
        )

        col.document(docId).set(doc).await()
    }

    suspend fun removeRoute(routeId: String, type: String) {
        val col = collection() ?: return
        val docId = "${type}_${routeId}"
        col.document(docId).delete().await()
    }

    fun getRoutes(type: String): Flow<List<SavedRouteDocument>> = callbackFlow {
        val col = collection()
        if (col == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = col
            .whereEqualTo("type", type)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(SavedRouteDocument::class.java)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getRoute(routeId: String, type: String): SavedRouteDocument? {
        val col = collection() ?: return null
        val docId = "${type}_${routeId}"
        return try {
            val snapshot = col.document(docId).get().await()
            snapshot.toObject(SavedRouteDocument::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun toRouteAndStops(doc: SavedRouteDocument): Pair<TravelRoute, List<RouteStop>> {
        val route = TravelRoute(
            id = doc.routeId, name = doc.routeName,
            subtitle = doc.routeSubtitle,
            destName = doc.destName,
            budget = doc.budget,
            duration = doc.duration, effort = doc.effort,
            effortLevel = doc.effortLevel, stops = doc.stopCount,
            rating = doc.rating.toFloat(), reviews = doc.reviews,
            imageUrl = doc.imageUrl, highlights = doc.highlights,
            gradientColors = Pair(doc.gradientColor1, doc.gradientColor2)
        )
        val stops = doc.stops.map { s ->
            RouteStop(
                id = s.id, name = s.name, type = s.type,
                timeSlot = try { TimeSlot.valueOf(s.timeSlot) } catch (_: Exception) { TimeSlot.MATIN },
                arrivalTime = s.arrivalTime, duration = s.duration,
                distance = s.distance, walkTime = s.walkTime,
                cost = s.cost, description = s.description,
                imageUrl = s.imageUrl,
                imageUrls = s.imageUrls.ifEmpty { listOf(s.imageUrl).filter { it.isNotBlank() } },
                rating = s.rating.toFloat(),
                openHours = s.openHours, lat = s.lat, lng = s.lng,
                polylineToNext = s.polylineToNext.ifBlank { null },
                source = s.source,
                sourcePostId = s.sourcePostId
            )
        }
        return route to stops
    }
}
