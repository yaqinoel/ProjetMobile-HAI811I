package com.example.traveling.features.travelshare

import com.example.traveling.features.travelshare.GalleryFilter
import com.example.traveling.features.travelshare.PhotoPostUi
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
    // on applique d'abord les filtres simples, puis le mode spécial si besoin
    val filtered = posts.filter { post ->
        matchesQuery(post, query) &&
            matchesPlaceType(post, filter.placeType) &&
            matchesPeriod(post, filter)
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
    // texte unique pour chercher titre, lieu, auteur, tags et groupe
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
    if (placeType == "all") return true

    val expectedTerms = placeTypeAliases(placeType)
    // les types peuvent venir du formulaire, des tags ou du texte du post
    val postTerms = buildSet {
        add(normalizeFilterTerm(post.placeType))
        post.tags.forEach { add(normalizeFilterTerm(it)) }
        searchableText(post)
            .split(' ', ',', ';', '#', '.', '/', '-', '_')
            .mapTo(this) { normalizeFilterTerm(it) }
    }.filter { it.isNotBlank() }.toSet()

    return expectedTerms.any { expected -> expected in postTerms }
}

private fun placeTypeAliases(placeType: String): Set<String> {
    // synonymes anglais/français pour éviter les résultats vides
    return when (normalizeFilterTerm(placeType)) {
        "nature" -> setOf("nature", "park", "parc", "garden", "jardin", "beach", "plage", "mountain", "montagne", "forest", "foret", "lac", "lake")
        "museum" -> setOf("museum", "musee", "culture", "cultural", "art", "gallery", "galerie", "exposition")
        "street" -> setOf("street", "rue", "avenue", "boulevard", "road", "route", "place", "square", "quartier")
        "shop" -> setOf("shop", "shopping", "magasin", "market", "marche", "mall", "boutique", "store")
        "monument" -> setOf("monument", "landmark", "historic", "historique", "statue", "temple", "church", "eglise", "cathedral", "cathedrale")
        "architecture" -> setOf("architecture", "architectural", "building", "batiment", "design")
        else -> setOf(normalizeFilterTerm(placeType))
    }
}

private fun normalizeFilterTerm(value: String): String {
    // petite normalisation sans dépendance externe
    return value
        .trim()
        .lowercase()
        .replace("é", "e")
        .replace("è", "e")
        .replace("ê", "e")
        .replace("ë", "e")
        .replace("à", "a")
        .replace("â", "a")
        .replace("ä", "a")
        .replace("î", "i")
        .replace("ï", "i")
        .replace("ô", "o")
        .replace("ö", "o")
        .replace("ù", "u")
        .replace("û", "u")
        .replace("ü", "u")
        .replace("ç", "c")
}

private fun matchesPeriod(post: PhotoPostUi, filter: GalleryFilter): Boolean {
    val start = filter.dateRangeStartMillis
    val end = filter.dateRangeEndMillis
    val period = filter.period
    if (start == null && end == null && (period == "all" || period == "custom")) return true

    val createdAt = post.createdAtMillis ?: return false
    if (start != null || end != null) {
        // plage personnalisée choisie avec les rouleaux de date
        val min = start ?: Long.MIN_VALUE
        val max = end?.let { endOfDayMillis(it) } ?: Long.MAX_VALUE
        return createdAt in min..max
    }

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

private fun endOfDayMillis(timeMillis: Long): Long {
    // la date de fin doit inclure toute la journée sélectionnée
    return Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}

private fun filterNearby(
    posts: List<PhotoPostUi>,
    filter: GalleryFilter
): List<PhotoPostUi> {
    val centerLat = filter.centerLatitude
    val centerLng = filter.centerLongitude
    if (centerLat == null || centerLng == null) return posts

    // on compare la distance réelle entre le centre choisi et chaque photo
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
        // recherche depuis une photo précise
        return candidates
            .filterNot { it.id == source.id }
            .map { it to similarityScore(source, it) }
            .filter { (_, score) -> score > 0 }
            .sortedWith(
                compareByDescending<Pair<PhotoPostUi, Int>> { (_, score) -> score }
                    .thenByDescending { (post, _) -> post.createdAtMillis ?: 0L }
            )
            .map { (post, _) -> post }
    }

    val queryTokens = filter.query
        .split(' ', ',', ';', '#')
        .map { it.trim().lowercase() }
        .filter { it.length >= 2 }
    val anchorTerms = queryTokens + listOf(filter.placeType).filter { it != "all" }
    if (anchorTerms.isEmpty()) return candidates

    // recherche similaire plus simple quand on part d'un texte ou d'un filtre
    return candidates
        .map { post -> post to anchorSimilarityScore(post, anchorTerms) }
        .filter { (_, score) -> score > 0 }
        .sortedByDescending { (_, score) -> score }
        .map { (post, _) -> post }
}

private fun similarityScore(source: PhotoPostUi, candidate: PhotoPostUi): Int {
    val sourceTags = source.tags.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    val candidateTags = candidate.tags.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    val sharedTags = sourceTags.intersect(candidateTags).size

    // tags en commun > type > ville > pays > même auteur
    var score = sharedTags * 3
    if (source.placeType.isNotBlank() && source.placeType.equals(candidate.placeType, ignoreCase = true)) score += 2
    if (source.city.isNotBlank() && source.city.equals(candidate.city, ignoreCase = true)) score += 2
    if (source.country.isNotBlank() && source.country.equals(candidate.country, ignoreCase = true)) score += 1
    if (score > 0 && source.authorId.isNotBlank() && source.authorId == candidate.authorId) score += 1
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
