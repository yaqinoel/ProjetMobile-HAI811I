package com.example.traveling.features.travelshare.util

import com.example.traveling.features.travelshare.model.GalleryFilter
import com.example.traveling.features.travelshare.model.PhotoPostUi
import java.util.Calendar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

fun filterGalleryPosts(
    posts: List<PhotoPostUi>,
    filter: GalleryFilter
): List<PhotoPostUi> {
    val query = filter.query.trim()
    val filtered = posts.filter { post ->
        matchesQuery(post, query) &&
            matchesPlaceType(post, filter.placeType) &&
            matchesPeriod(post, filter.period)
    }

    return when (filter.discoveryMode) {
        "nearby" -> filterNearby(filtered, filter)
        "similar" -> filterSimilar(posts = posts, candidates = filtered, filter = filter)
        else -> filtered
    }
}

fun distanceKm(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double
): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2.0) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusKm * c
}

private fun matchesQuery(post: PhotoPostUi, query: String): Boolean {
    if (query.isBlank()) return true
    return searchableText(post).contains(query, ignoreCase = true)
}

private fun searchableText(post: PhotoPostUi): String {
    return buildString {
        append(post.title).append(' ')
        append(post.location).append(' ')
        append(post.city).append(' ')
        append(post.country).append(' ')
        append(post.author).append(' ')
        append(post.authorId).append(' ')
        append(post.description).append(' ')
        append(post.placeType).append(' ')
        append(post.visibility).append(' ')
        append(post.groupName.orEmpty()).append(' ')
        post.tags.forEach { append(it).append(' ') }
    }
}

private fun matchesPlaceType(post: PhotoPostUi, placeType: String): Boolean {
    return placeType == "all" || post.placeType.equals(placeType, ignoreCase = true)
}

private fun matchesPeriod(post: PhotoPostUi, period: String): Boolean {
    if (period == "all") return true
    val createdAt = post.createdAtMillis ?: return false
    val now = System.currentTimeMillis()
    val millisPerDay = 24L * 60L * 60L * 1000L

    return when (period) {
        "week" -> createdAt >= now - 7L * millisPerDay
        "month" -> createdAt >= now - 30L * millisPerDay
        "3months" -> createdAt >= now - 90L * millisPerDay
        "year" -> {
            val createdCalendar = Calendar.getInstance().apply { timeInMillis = createdAt }
            val currentCalendar = Calendar.getInstance()
            createdCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)
        }
        else -> true
    }
}

private fun filterNearby(
    posts: List<PhotoPostUi>,
    filter: GalleryFilter
): List<PhotoPostUi> {
    val centerLat = filter.centerLatitude
    val centerLng = filter.centerLongitude
    if (centerLat == null || centerLng == null) return posts

    return posts.filter { post ->
        val lat = post.latitude
        val lng = post.longitude
        lat != null && lng != null && distanceKm(centerLat, centerLng, lat, lng) <= filter.radiusKm
    }
}

private fun filterSimilar(
    posts: List<PhotoPostUi>,
    candidates: List<PhotoPostUi>,
    filter: GalleryFilter
): List<PhotoPostUi> {
    val source = filter.similarToPostId
        ?.let { sourceId -> posts.find { it.id == sourceId } }

    if (source != null) {
        return candidates
            .filterNot { it.id == source.id }
            .map { it to similarityScore(source, it) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (post, _) -> post }
    }

    val queryTokens = filter.query
        .split(' ', ',', ';', '#')
        .map { it.trim().lowercase() }
        .filter { it.length >= 2 }
    val anchorTerms = queryTokens + listOf(filter.placeType).filter { it != "all" }
    if (anchorTerms.isEmpty()) return candidates

    return candidates
        .map { post -> post to anchorSimilarityScore(post, anchorTerms) }
        .filter { (_, score) -> score > 0 }
        .sortedByDescending { (_, score) -> score }
        .map { (post, _) -> post }
}

private fun similarityScore(source: PhotoPostUi, candidate: PhotoPostUi): Int {
    val sourceTags = source.tags.map { it.lowercase() }.toSet()
    val candidateTags = candidate.tags.map { it.lowercase() }.toSet()
    val sharedTags = sourceTags.intersect(candidateTags).size

    var score = sharedTags * 2
    if (source.placeType.equals(candidate.placeType, ignoreCase = true)) score += 3
    if (source.city.isNotBlank() && source.city.equals(candidate.city, ignoreCase = true)) score += 1
    if (source.country.isNotBlank() && source.country.equals(candidate.country, ignoreCase = true)) score += 1
    return score
}

private fun anchorSimilarityScore(post: PhotoPostUi, anchorTerms: List<String>): Int {
    val searchable = searchableText(post).lowercase()
    return anchorTerms.sumOf { term ->
        when {
            post.placeType.equals(term, ignoreCase = true) -> 3
            post.tags.any { it.equals(term, ignoreCase = true) } -> 2
            searchable.contains(term) -> 1
            else -> 0
        }
    }
}
