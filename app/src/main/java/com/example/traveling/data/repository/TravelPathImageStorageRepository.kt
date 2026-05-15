package com.example.traveling.data.repository

import com.example.traveling.data.model.Attraction
import com.example.traveling.data.model.Destination
import com.example.traveling.data.model.FirestoreCollections
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class TravelPathImageMigrationProgress(
    val label: String,
    val done: Int,
    val total: Int
)

data class TravelPathImageMigrationResult(
    val total: Int,
    val success: Int,
    val failed: Int,
    val errors: List<String>
)

class TravelPathImageStorageRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val oldChineseDestinationIds = listOf("pekin", "xian", "hangzhou", "chengdu", "guilin")

    suspend fun migrateAllToStorage(
        onProgress: (TravelPathImageMigrationProgress) -> Unit = {}
    ): TravelPathImageMigrationResult = withContext(Dispatchers.IO) {
        onProgress(TravelPathImageMigrationProgress("Suppression des donnees chinoises", 0, 1))
        removeOldChineseContent()

        onProgress(TravelPathImageMigrationProgress("Mise a jour des donnees", 0, 1))
        FirestoreSeeder.seedAll(clearFirst = false)
        delay(4_000)

        val destinationDocs = db.collection(FirestoreCollections.DESTINATIONS).get().await().documents
        val attractionDocs = db.collection(FirestoreCollections.ATTRACTIONS).get().await().documents
        val total = destinationDocs.size + attractionDocs.size
        var done = 0
        var success = 0
        val errors = mutableListOf<String>()

        destinationDocs.forEach { doc ->
            val destination = doc.toObject(Destination::class.java)
            if (destination == null) {
                errors += "destinations/${doc.id}: document illisible"
            } else {
                try {
                    val urls = uploadPlaceImages(
                        storagePath = "travelpath/destinations/${doc.id}",
                        lat = destination.lat,
                        lng = destination.lng,
                        count = 1
                    )
                    if (urls.isNotEmpty()) {
                        doc.reference.set(mapOf("imageUrl" to urls.first()), SetOptions.merge()).await()
                        success++
                    } else {
                        errors += "destinations/${doc.id}: coordonnees manquantes"
                    }
                } catch (e: Exception) {
                    errors += "destinations/${doc.id}: ${e.message ?: e.javaClass.simpleName}"
                }
            }
            done++
            onProgress(TravelPathImageMigrationProgress("Destinations", done, total))
        }

        attractionDocs.forEach { doc ->
            val attraction = doc.toObject(Attraction::class.java)
            if (attraction == null) {
                errors += "attractions/${doc.id}: document illisible"
            } else {
                try {
                    val urls = uploadPlaceImages(
                        storagePath = "travelpath/attractions/${doc.id}",
                        lat = attraction.lat,
                        lng = attraction.lng,
                        count = 3
                    )
                    if (urls.size == 3) {
                        doc.reference.set(
                            mapOf(
                                "imageUrl" to urls.first(),
                                "imageUrls" to urls
                            ),
                            SetOptions.merge()
                        ).await()
                        success++
                    } else {
                        errors += "attractions/${doc.id}: ${urls.size}/3 image(s) uploadee(s)"
                    }
                } catch (e: Exception) {
                    errors += "attractions/${doc.id}: ${e.message ?: e.javaClass.simpleName}"
                }
            }
            done++
            onProgress(TravelPathImageMigrationProgress("Attractions", done, total))
        }

        TravelPathImageMigrationResult(
            total = total,
            success = success,
            failed = errors.size,
            errors = errors
        )
    }

    private suspend fun removeOldChineseContent() {
        val attractionDocs = db.collection(FirestoreCollections.ATTRACTIONS)
            .whereIn("destinationId", oldChineseDestinationIds)
            .get()
            .await()
            .documents

        attractionDocs.forEach { doc ->
            deleteStorageImages("travelpath/attractions/${doc.id}", 3)
            doc.reference.delete().await()
        }

        oldChineseDestinationIds.forEach { destinationId ->
            deleteStorageImages("travelpath/destinations/$destinationId", 1)
            db.collection(FirestoreCollections.DESTINATIONS).document(destinationId).delete().await()
        }
    }

    private suspend fun deleteStorageImages(storagePath: String, count: Int) {
        repeat(count) { index ->
            runCatching {
                storage.reference.child("$storagePath/image_${index + 1}.jpg").delete().await()
            }
        }
    }

    private suspend fun uploadPlaceImages(
        storagePath: String,
        lat: Double,
        lng: Double,
        count: Int
    ): List<String> {
        if (lat == 0.0 && lng == 0.0) return emptyList()
        val headings = listOf(0, 120, 240).take(count)
        return headings.mapIndexed { index, heading ->
            val bytes = downloadBytes(googleStreetViewUrl(lat, lng, heading))
            val ref = storage.reference.child("$storagePath/image_${index + 1}.jpg")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()
            ref.putBytes(bytes, metadata).await()
            ref.downloadUrl.await().toString()
        }
    }

    private fun googleStreetViewUrl(lat: Double, lng: Double, heading: Int): String {
        return "https://maps.googleapis.com/maps/api/streetview" +
            "?size=900x600" +
            "&location=$lat,$lng" +
            "&heading=$heading" +
            "&pitch=5" +
            "&fov=80" +
            "&source=outdoor" +
            "&key=${com.example.traveling.BuildConfig.MAPS_API_KEY}"
    }

    private fun downloadBytes(urlString: String): ByteArray {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }
}
