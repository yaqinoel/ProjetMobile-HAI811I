package com.example.traveling.data.model

/**
 * Firestore document model for a user's saved/liked route.
 * Collection: "users/{userId}/saved_routes"
 *
 * Stores the full route + stops data so it can be loaded
 * from any device without relying on local cache.
 */
data class SavedRouteDocument(
    val routeId: String = "",
    val type: String = "",             // "liked" or "saved"
    val destName: String = "",
    val routeName: String = "",
    val routeSubtitle: String = "",
    val budget: Int = 0,
    val duration: String = "",
    val effort: String = "",
    val effortLevel: Int = 1,
    val stopCount: Int = 0,
    val rating: Double = 0.0,
    val reviews: Int = 0,
    val imageUrl: String = "",
    val highlights: List<String> = emptyList(),
    val gradientColor1: Long = 0L,
    val gradientColor2: Long = 0L,
    val stops: List<SavedRouteStopEntry> = emptyList(),
    val savedAt: Long = System.currentTimeMillis()
)

/**
 * Embedded stop entry inside SavedRouteDocument.
 * Stores all fields needed to reconstruct a RouteStop.
 */
data class SavedRouteStopEntry(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val timeSlot: String = "",         // MATIN, APRES_MIDI, SOIR
    val arrivalTime: String = "",
    val duration: String = "",
    val distance: String = "",
    val walkTime: String = "",
    val cost: Int = 0,
    val description: String = "",
    val imageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val rating: Double = 0.0,
    val openHours: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val polylineToNext: String = ""
)
