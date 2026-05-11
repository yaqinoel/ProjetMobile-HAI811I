package com.example.traveling.features.travelshare.ai

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await

data class ImageAnnotationResult(
    val tags: List<String>
)

class ImageAnnotationRepository {
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    suspend fun annotateImages(context: Context, imageUris: List<Uri>): ImageAnnotationResult {
        val tags = imageUris
            .take(3)
            .flatMap { uri -> annotateOneImage(context, uri) }
            .distinctBy { it.lowercase() }
            .take(10)

        return ImageAnnotationResult(tags = tags)
    }

    private suspend fun annotateOneImage(context: Context, uri: Uri): List<String> {
        val image = InputImage.fromFilePath(context, uri)
        return labeler.process(image)
            .await()
            .filter { it.confidence >= 0.55f }
            .sortedByDescending { it.confidence }
            .take(5)
            .map { it.text.toTravelTag() }
            .filter { it.isNotBlank() }
    }
}

private fun String.toTravelTag(): String {
    val normalized = trim().lowercase()
    val mapped = when (normalized) {
        "mountain", "hill", "forest", "tree", "plant", "flower", "landscape", "natural landscape" -> "nature"
        "sky", "cloud", "sunset", "sunrise" -> "ciel"
        "water", "lake", "river", "sea", "ocean" -> "eau"
        "beach", "coast" -> "plage"
        "food", "dish", "meal", "cuisine", "restaurant" -> "food"
        "building", "architecture", "skyscraper", "tower", "bridge" -> "architecture"
        "city", "town", "metropolis" -> "ville"
        "street", "road", "alley" -> "rue"
        "museum", "art", "sculpture", "monument", "statue" -> "culture"
        "person", "people", "crowd" -> "voyageur"
        "animal", "wildlife" -> "animal"
        else -> normalized
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
    }
    return mapped.trim().trim('-')
}
