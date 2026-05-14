package com.example.traveling.data.model

data class SavedRouteDocument(
    val routeId: String = "",
    val type: String = "",
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

data class SavedRouteStopEntry(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val timeSlot: String = "",
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
    val polylineToNext: String = "",
    val source: String = "official",
    val sourcePostId: String? = null
)
