package com.example.traveling.features.travelshare

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await

data class ImageAnnotationResult(
    val tags: List<String>
)

class ImageAnnotationService {
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    suspend fun annotateImages(context: Context, imageUris: List<Uri>): ImageAnnotationResult {
        val candidates = imageUris
            .take(MAX_IMAGES_TO_ANALYZE)
            .flatMap { uri -> annotateOneImage(context, uri) }

        val tags = candidates
            .groupBy { it.tag.lowercase() }
            .map { (_, values) ->
                values.first().tag to (values.maxOf { it.confidence } + values.size * FREQUENCY_BONUS)
            }
            .sortedByDescending { (_, score) -> score }
            .map { (tag, _) -> tag }
            .take(MAX_TAGS_TO_RETURN)

        return ImageAnnotationResult(tags = tags)
    }

    private suspend fun annotateOneImage(context: Context, uri: Uri): List<LabelCandidate> {
        val image = InputImage.fromFilePath(context, uri)
        return labeler.process(image)
            .await()
            .filter { it.confidence >= MIN_CONFIDENCE }
            .sortedByDescending { it.confidence }
            .take(MAX_LABELS_PER_IMAGE)
            .mapNotNull { label ->
                label.text.toTravelTag()?.let { tag ->
                    LabelCandidate(tag = tag, confidence = label.confidence)
                }
            }
    }

    private companion object {
        const val MAX_IMAGES_TO_ANALYZE = 14
        const val MAX_LABELS_PER_IMAGE = 4
        const val MAX_TAGS_TO_RETURN = 6
        const val MIN_CONFIDENCE = 0.60f
        const val FREQUENCY_BONUS = 0.08f
    }
}

private data class LabelCandidate(
    val tag: String,
    val confidence: Float
)

private fun String.toTravelTag(): String? {
    val normalized = trim().lowercase()
    if (normalized in ignoredLabels) return null

    val mapped = when (normalized) {
        "mountain", "hill", "forest", "tree", "plant", "flower", "landscape", "natural landscape", "grass", "garden" -> "nature"
        "sky", "cloud", "sunset", "sunrise", "horizon" -> "ciel"
        "water", "lake", "river", "sea", "ocean" -> "eau"
        "beach", "coast", "sand" -> "plage"
        "food", "dish", "meal", "cuisine", "restaurant" -> "food"
        "building", "architecture", "skyscraper", "tower", "bridge", "facade" -> "architecture"
        "city", "town", "metropolis" -> "ville"
        "street", "road", "alley", "path", "walkway" -> "rue"
        "museum", "art", "sculpture", "monument", "statue", "historic site" -> "culture"
        "person", "people", "crowd" -> "voyageur"
        "animal", "wildlife" -> "animal"
        else -> normalized
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
    }
    return mapped.trim().trim('-').takeIf { it.length >= 2 && it !in ignoredLabels }
}

private val ignoredLabels = setOf(
    "image",
    "photo",
    "photograph",
    "snapshot",
    "picture",
    "travel",
    "vacation",
    "tourism",
    "outdoor",
    "daytime",
    "event",
    "fun",
    "leisure"
)
