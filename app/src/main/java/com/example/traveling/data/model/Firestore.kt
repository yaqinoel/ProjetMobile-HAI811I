package com.example.traveling.data.model

/**
 * Destination (城市/目的地)
 * Firestore collection: "destinations"
 */
data class Destination(
    val id: String = "",
    val name: String = "",
    val country: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

/**
 * Attraction (景点)
 * Firestore collection: "attractions"
 */
data class Attraction(
    val id: String = "",
    val destinationId: String = "",
    val name: String = "",
    val type: String = "",           // Culture, Nature, Gastronomie, Loisirs, Shopping, Monument, Photo
    val cost: Int = 0,               // 门票/费用 (¥)
    val duration: Int = 0,           // 平均游览时间 (分钟)
    val rating: Double = 0.0,
    val description: String = "",
    val imageUrl: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val openHours: String = "",
    val closedDay: String = "",      // 休息日
    val effortLevel: Int = 1,        // 体力消耗 1-5
    val weatherType: String = "both",// indoor / outdoor / both
    val bestTimeSlots: List<String> = emptyList(), // matin, apres-midi, soir
    val tags: List<String> = emptyList(),
    val imageUrls: List<String> = emptyList()
)

/**
 * GeneratedRoute (生成的路线)
 * Firestore collection: "generated_routes"
 */
data class GeneratedRoute(
    val id: String = "",
    val userId: String = "",
    val destinationId: String = "",
    val name: String = "",
    val totalBudget: Int = 0,
    val totalDuration: String = "",
    val effortLevel: Int = 1,
    val rating: Double = 0.0,
    val createdAt: Long = 0L,
    val preferences: RoutePreferences = RoutePreferences(),
    val stops: List<RouteStopEntry> = emptyList()
)

data class RoutePreferences(
    val activities: List<String> = emptyList(),
    val budget: Int = 0,
    val duration: Int = 0,
    val effort: Int = 3
)

data class RouteStopEntry(
    val attractionId: String = "",
    val attractionName: String = "",
    val order: Int = 0,
    val timeSlot: String = "",
    val arrivalTime: String = "",
    val duration: String = "",
    val distanceFromPrev: String = "",
    val walkTimeFromPrev: String = "",
    val cost: Int = 0,
    val imageUrl: String = "",
    val imageUrls: List<String> = emptyList()
)
