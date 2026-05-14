package com.example.traveling.core.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

object ImageMigrationHelper {

    private val storage = FirebaseStorage.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private const val TAG = "ImageMigration"
    private const val STORAGE_ROOT = "travelpath"
    private const val IMAGE_VERSION = "2026-05-11-real"

    data class MigrationResult(
        val total: Int,
        val success: Int,
        val failed: Int,
        val errors: List<String>
    )

    private data class ImageTarget(
        val collection: String,
        val storageFolder: String,
        val id: String,
        val name: String,
        val context: String,
        val type: String,
        val label: String
    )

    private data class SourceImage(
        val url: String,
        val contentType: String
    )

    suspend fun migrateAll(
        onProgress: (current: Int, total: Int, name: String) -> Unit
    ): MigrationResult {
        val targets = loadTargets()
        val errors = mutableListOf<String>()
        var success = 0

        targets.forEachIndexed { index, target ->
            onProgress(index + 1, targets.size, target.label)

            try {
                deleteLegacyImages(target)

                val requestedCount = if (target.collection == "attractions") 3 else 1
                val sourceImages = photoSources(target, requestedCount)

                val imageUrls = sourceImages.take(requestedCount).mapIndexed { imageIndex, source ->
                    val fileName = if (target.collection == "attractions") {
                        "photo-${imageIndex + 1}.jpg"
                    } else {
                        "cover.jpg"
                    }
                    uploadRemoteImage(
                        target = target,
                        source = source,
                        storagePath = "$STORAGE_ROOT/${target.storageFolder}/${target.id}/$fileName",
                        variant = imageIndex + 1
                    )
                }

                val updates = mutableMapOf<String, Any>("imageUrl" to imageUrls.first())
                if (target.collection == "attractions") {
                    updates["imageUrls"] = imageUrls
                }

                db.collection(target.collection).document(target.id).update(updates).await()

                success++
                Log.d(TAG, "Migrated ${target.collection}/${target.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Migration failed for ${target.collection}/${target.id}", e)
                errors.add("${target.collection}/${target.id}: ${e.message ?: "unknown error"}")
            }
        }

        return MigrationResult(
            total = targets.size,
            success = success,
            failed = errors.size,
            errors = errors
        )
    }

    private suspend fun loadTargets(): List<ImageTarget> {
        val destinationDocs = db.collection("destinations").get().await().documents
        val destinationNames = destinationDocs.associate { doc ->
            doc.id to (doc.getString("name") ?: doc.id)
        }

        val destinations = destinationDocs
            .mapNotNull { doc ->
                doc.id.takeIf(String::isNotBlank)?.let { id ->
                    val name = doc.getString("name") ?: id
                    val country = doc.getString("country").orEmpty()
                    ImageTarget(
                        collection = "destinations",
                        storageFolder = "destinations",
                        id = id,
                        name = name,
                        context = country,
                        type = "destination",
                        label = "Dest: $id"
                    )
                }
            }
            .sortedBy { it.id }

        val attractions = db.collection("attractions").get().await().documents
            .mapNotNull { doc ->
                doc.id.takeIf(String::isNotBlank)?.let { id ->
                    val destinationId = doc.getString("destinationId").orEmpty()
                    ImageTarget(
                        collection = "attractions",
                        storageFolder = "attractions",
                        id = id,
                        name = doc.getString("name") ?: id,
                        context = destinationNames[destinationId] ?: destinationId,
                        type = doc.getString("type") ?: "travel",
                        label = "Attr: $id"
                    )
                }
            }
            .sortedBy { it.id }

        return destinations + attractions
    }

    private fun photoSources(target: ImageTarget, count: Int): List<SourceImage> {
        return (1..count).map { variant ->
            val query = listOf(target.name, target.context, target.type, "travel")
                .filter { it.isNotBlank() }
                .joinToString(",")
            val encoded = URLEncoder.encode(query, "UTF-8").replace("+", ",")
            val lock = abs("${target.id}-$variant-$IMAGE_VERSION".hashCode())
            SourceImage(
                url = "https://loremflickr.com/1200/800/$encoded?lock=$lock",
                contentType = "image/jpeg"
            )
        }
    }

    private suspend fun uploadRemoteImage(
        target: ImageTarget,
        source: SourceImage,
        storagePath: String,
        variant: Int
    ): String {
        return withContext(Dispatchers.IO) {
            val bytes = downloadBytes(source.url)
            val ref = storage.reference.child(storagePath)
            val metadata = storageMetadata {
                contentType = source.contentType.ifBlank { "image/jpeg" }
                setCustomMetadata("entityId", target.id)
                setCustomMetadata("entityName", target.name)
                setCustomMetadata("imageVersion", IMAGE_VERSION)
                setCustomMetadata("source", source.url)
                setCustomMetadata("variant", variant.toString())
            }

            ref.putBytes(bytes, metadata).await()
            ref.downloadUrl.await().toString()
        }
    }

    private suspend fun deleteLegacyImages(target: ImageTarget) {
        val paths = buildList {
            add("images/${target.storageFolder}/${target.id}.jpg")
            if (target.collection == "attractions") {
                add("$STORAGE_ROOT/attractions/${target.id}/photo-1.png")
                add("$STORAGE_ROOT/attractions/${target.id}/photo-2.png")
                add("$STORAGE_ROOT/attractions/${target.id}/photo-3.png")
            } else {
                add("$STORAGE_ROOT/destinations/${target.id}/cover.png")
            }
        }

        paths.forEach { path ->
            runCatching {
                storage.reference.child(path).delete().await()
            }.onFailure {
                Log.d(TAG, "No legacy image to delete at $path")
            }
        }
    }

    private fun downloadBytes(url: String): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 25_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "TravelingImageMigration/1.0")
        }

        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("HTTP $responseCode while downloading $url")
            }

            connection.inputStream.use { input ->
                input.readBytes().also { bytes ->
                    if (bytes.isEmpty()) {
                        throw IllegalStateException("Empty response from $url")
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
