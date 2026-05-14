package com.example.traveling.data.repository

import com.example.traveling.data.model.Attraction
import com.example.traveling.data.model.Destination
import com.example.traveling.data.model.FirestoreCollections
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.Normalizer

/**
 * Repository pour accéder aux données Firestore (destinations & attractions).
 */
class TravelRepository {

    private val db = FirebaseFirestore.getInstance()

    /** Flux temps-réel de toutes les destinations. */
    fun getDestinations(): Flow<List<Destination>> = callbackFlow {
        val listener = db.collection(FirestoreCollections.DESTINATIONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Destination::class.java)
                }?.sortedWith(
                    compareBy<Destination> { if (it.source == "travelshare") 1 else 0 }
                        .thenBy { it.name.lowercase() }
                ) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Flux temps-réel des attractions pour une destination donnée. */
    fun getAttractionsByDestination(destinationId: String): Flow<List<Attraction>> = callbackFlow {
        val listener = db.collection(FirestoreCollections.ATTRACTIONS)
            .whereEqualTo("destinationId", destinationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Attraction::class.java)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Flux temps-réel de TOUTES les attractions. */
    fun getAllAttractions(): Flow<List<Attraction>> = callbackFlow {
        val listener = db.collection(FirestoreCollections.ATTRACTIONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Attraction::class.java)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun findDestinationByName(city: String): Destination? {
        val normalized = normalizeCityName(city)
        if (normalized.isBlank()) return null

        val normalizedMatch = db.collection(FirestoreCollections.DESTINATIONS)
            .whereEqualTo("normalizedName", normalized)
            .limit(1)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Destination::class.java) }
            .firstOrNull()
        if (normalizedMatch != null) return normalizedMatch

        return db.collection(FirestoreCollections.DESTINATIONS)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Destination::class.java) }
            .firstOrNull { destination ->
                normalizeCityName(destination.normalizedName.ifBlank { destination.name }) == normalized
            }
    }

    suspend fun ensureTravelPathDestinationForPost(
        city: String?,
        country: String?,
        lat: Double?,
        lng: Double?,
        imageUrl: String?,
        sourcePostId: String,
        userId: String
    ): Destination? {
        val cityName = city?.trim().orEmpty()
        if (cityName.isBlank() || lat == null || lng == null) return null

        findDestinationByName(cityName)?.let { existing ->
            incrementTravelSharePhotoCount(existing.id)
            return existing
        }

        return createTravelShareDestinationFromPost(
            city = cityName,
            country = country.orEmpty(),
            lat = lat,
            lng = lng,
            imageUrl = imageUrl.orEmpty(),
            sourcePostId = sourcePostId,
            userId = userId
        )
    }

    suspend fun createTravelShareDestinationFromPost(
        city: String,
        country: String,
        lat: Double,
        lng: Double,
        imageUrl: String,
        sourcePostId: String,
        userId: String
    ): Destination {
        val normalized = normalizeCityName(city)
        val destinationId = "ts_$normalized"
        val ref = db.collection(FirestoreCollections.DESTINATIONS).document(destinationId)
        val destination = Destination(
            id = destinationId,
            name = city.trim(),
            country = country.trim(),
            description = "Destination ajoutée depuis TravelShare.",
            imageUrl = imageUrl,
            lat = lat,
            lng = lng,
            source = "travelshare",
            normalizedName = normalized,
            createdFromPostId = sourcePostId,
            createdByUserId = userId,
            createdAt = Timestamp.now(),
            photoCount = 1,
            isVerified = false
        )

        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            if (!snapshot.exists()) {
                transaction.set(ref, destination)
                destination
            } else {
                transaction.set(ref, mapOf("photoCount" to FieldValue.increment(1)), SetOptions.merge())
                snapshot.toObject(Destination::class.java) ?: destination
            }
        }.await()

        return ref.get().await().toObject(Destination::class.java) ?: destination
    }

    private suspend fun incrementTravelSharePhotoCount(destinationId: String) {
        db.collection(FirestoreCollections.DESTINATIONS)
            .document(destinationId)
            .set(mapOf("photoCount" to FieldValue.increment(1)), SetOptions.merge())
            .await()
    }

    companion object {
        fun normalizeCityName(city: String): String {
            val withoutCountry = city.substringBefore(",")
            val ascii = Normalizer.normalize(withoutCountry.lowercase().trim(), Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            val normalized = ascii
                .replace("[^a-z0-9\\s]".toRegex(), " ")
                .split("\\s+".toRegex())
                .filter { token ->
                    token.isNotBlank() && token !in setOf("city", "ville", "france", "chine", "china")
                }
                .joinToString("_")
                .trim('_')
            return normalized.ifBlank {
                "city_${city.trim().lowercase().hashCode().toString().replace("-", "n")}"
            }
        }
    }
}
