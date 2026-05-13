package com.example.traveling.data.repository

import com.example.traveling.data.model.FirestoreCollections
import com.example.traveling.data.model.PhotoPostDocument
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class TravelBridgeRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun observeTravelSharePhotosForDestination(city: String): Flow<List<PhotoPostDocument>> = callbackFlow {
        if (city.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = db.collection(FirestoreCollections.PHOTO_POSTS)
            .whereEqualTo("visibility", "public")
            .whereEqualTo("status", "published")
            .whereEqualTo("city", city)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(12)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents
                    ?.mapNotNull { it.toObject(PhotoPostDocument::class.java) }
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

    suspend fun findNearbyPublicPhotos(
        lat: Double,
        lng: Double,
        radiusKm: Double
    ): List<PhotoPostDocument> {
        val posts = db.collection(FirestoreCollections.PHOTO_POSTS)
            .whereEqualTo("visibility", "public")
            .whereEqualTo("status", "published")
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
