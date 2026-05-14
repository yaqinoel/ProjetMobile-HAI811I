package com.example.traveling.data.repository

import com.example.traveling.data.model.FirestoreCollections
import com.example.traveling.data.model.PhotoPostDocument
import com.example.traveling.data.model.TravelShareAttractionDocument
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.Normalizer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class TravelBridgeRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun observeLinkedTravelSharePhotosForDestination(
        destinationId: String,
        fallbackCity: String?
    ): Flow<List<PhotoPostDocument>> = callbackFlow {
        if (destinationId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = db.collection(FirestoreCollections.PHOTO_POSTS)
            .whereEqualTo("visibility", "public")
            .whereEqualTo("status", "published")
            .whereEqualTo("isLinkedToTravelPath", true)
            .whereEqualTo("travelPathDestinationId", destinationId)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents
                    ?.mapNotNull { it.toObject(PhotoPostDocument::class.java) }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    ?: emptyList()

                trySend(posts)
            }

        awaitClose { listener.remove() }
    }

    fun observeLinkedTravelSharePhotosForDestination(city: String): Flow<List<PhotoPostDocument>> = callbackFlow {
        if (city.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = db.collection(FirestoreCollections.PHOTO_POSTS)
            .whereEqualTo("visibility", "public")
            .whereEqualTo("status", "published")
            .whereEqualTo("isLinkedToTravelPath", true)
            .whereEqualTo("city", city)
            .limit(12)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents
                    ?.mapNotNull { it.toObject(PhotoPostDocument::class.java) }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    ?: emptyList()

                trySend(posts)
            }

        awaitClose { listener.remove() }
    }

    suspend fun getPost(postId: String): PhotoPostDocument? {
        val post = db.collection(FirestoreCollections.PHOTO_POSTS)
            .document(postId)
            .get()
            .await()
            .toObject(PhotoPostDocument::class.java)
            ?: return null

        return if (post.visibility == "public" && post.status == "published") {
            post
        } else {
            null
        }
    }

    suspend fun findNearbyLinkedTravelSharePhotos(
        lat: Double,
        lng: Double,
        radiusKm: Double
    ): List<PhotoPostDocument> {
        val posts = db.collection(FirestoreCollections.PHOTO_POSTS)
            .whereEqualTo("visibility", "public")
            .whereEqualTo("status", "published")
            .whereEqualTo("isLinkedToTravelPath", true)
            .limit(100)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(PhotoPostDocument::class.java) }

        return posts.filter { post ->
            val visibleLat = post.displayLatitude ?: post.latitude ?: post.rawLatitude
            val visibleLng = post.displayLongitude ?: post.longitude ?: post.rawLongitude
            if (visibleLat == null || visibleLng == null) return@filter false

            haversineKm(lat, lng, visibleLat, visibleLng) <= radiusKm
        }
    }

    suspend fun getLinkedTravelSharePhotosForDestination(destinationId: String): List<PhotoPostDocument> {
        if (destinationId.isBlank()) return emptyList()

        val posts = db.collection(FirestoreCollections.PHOTO_POSTS)
            .whereEqualTo("visibility", "public")
            .whereEqualTo("status", "published")
            .whereEqualTo("isLinkedToTravelPath", true)
            .whereEqualTo("travelPathDestinationId", destinationId)
            .limit(100)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(PhotoPostDocument::class.java) }

        return posts.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    suspend fun getTravelShareAttractionsForDestination(destinationId: String): List<TravelShareAttractionDocument> {
        if (destinationId.isBlank()) return emptyList()

        val attractions = db.collection(FirestoreCollections.TRAVEL_SHARE_ATTRACTIONS)
            .whereEqualTo("destinationId", destinationId)
            .whereEqualTo("status", "active")
            .limit(100)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(TravelShareAttractionDocument::class.java) }

        return attractions.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    suspend fun findNearbyLinkedTravelSharePhotos(
        destinationId: String?,
        lat: Double,
        lng: Double,
        radiusKm: Double
    ): List<PhotoPostDocument> {
        val query = db.collection(FirestoreCollections.PHOTO_POSTS)
            .whereEqualTo("visibility", "public")
            .whereEqualTo("status", "published")
            .whereEqualTo("isLinkedToTravelPath", true)
            .let { base ->
                if (destinationId.isNullOrBlank()) base else base.whereEqualTo("travelPathDestinationId", destinationId)
            }
            .limit(100)

        val posts = query.get()
            .await()
            .documents
            .mapNotNull { it.toObject(PhotoPostDocument::class.java) }

        return posts.filter { post ->
            val visibleLat = post.displayLatitude ?: post.latitude ?: post.rawLatitude
            val visibleLng = post.displayLongitude ?: post.longitude ?: post.rawLongitude
            if (visibleLat == null || visibleLng == null) return@filter false

            haversineKm(lat, lng, visibleLat, visibleLng) <= radiusKm
        }
    }

    suspend fun findPhotosForRouteStop(
        stopName: String,
        lat: Double?,
        lng: Double?,
        city: String?,
        radiusKm: Double = 0.5
    ): List<PhotoPostDocument> {
        val posts = db.collection(FirestoreCollections.PHOTO_POSTS)
            .whereEqualTo("visibility", "public")
            .whereEqualTo("status", "published")
            .whereEqualTo("isLinkedToTravelPath", true)
            .limit(100)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(PhotoPostDocument::class.java) }

        return posts.filter { post ->
            matchesRouteStop(post, stopName, lat, lng, city, radiusKm)
        }.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    private fun matchesRouteStop(
        post: PhotoPostDocument,
        stopName: String,
        lat: Double?,
        lng: Double?,
        city: String?,
        radiusKm: Double
    ): Boolean {
        if (!city.isNullOrBlank() && !post.city.equals(city, ignoreCase = true)) {
            return false
        }

        val nameMatch = areNamesSimilar(post.locationName, stopName)

        val visibleLat = post.displayLatitude ?: post.latitude ?: post.rawLatitude
        val visibleLng = post.displayLongitude ?: post.longitude ?: post.rawLongitude
        val distanceMatch = if (lat != null && lng != null && visibleLat != null && visibleLng != null) {
            haversineKm(lat, lng, visibleLat, visibleLng) <= radiusKm
        } else {
            false
        }

        return nameMatch || distanceMatch
    }

    private fun areNamesSimilar(a: String, b: String): Boolean {
        val left = normalizePlaceName(a)
        val right = normalizePlaceName(b)
        if (left.isBlank() || right.isBlank()) return false
        if (left == right) return true
        return left.contains(right) || right.contains(left)
    }

    private fun normalizePlaceName(input: String): String {
        val ascii = Normalizer.normalize(input.lowercase(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return ascii
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .split("\\s+".toRegex())
            .filter { token ->
                token.isNotBlank() && token !in setOf("the", "le", "la", "les", "de", "des", "du", "d")
            }
            .joinToString(" ")
            .trim()
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
