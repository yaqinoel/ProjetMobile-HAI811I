package com.example.traveling.data.model

import com.google.firebase.Timestamp

data class Destination(
    val id: String = "",
    val name: String = "",
    val country: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val source: String = "official",
    val normalizedName: String = "",
    val createdFromPostId: String? = null,
    val createdByUserId: String? = null,
    val createdAt: Timestamp? = null,
    val photoCount: Int = 0,
    val isVerified: Boolean = false
)

data class Attraction(
    val id: String = "",
    val destinationId: String = "",
    val name: String = "",
    val type: String = "",
    val cost: Int = 0,
    val duration: Int = 0,
    val rating: Double = 0.0,
    val description: String = "",
    val imageUrl: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val openHours: String = "",
    val closedDay: String = "",
    val effortLevel: Int = 1,
    val weatherType: String = "both",
    val bestTimeSlots: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val imageUrls: List<String> = emptyList(),
    val source: String = "official",
    val sourcePostId: String? = null
)

data class TravelShareAttractionDocument(
    val id: String = "",
    val destinationId: String = "",
    val name: String = "",
    val type: String = "",
    val cost: Int = 0,
    val duration: Int = 45,
    val rating: Double = 4.2,
    val description: String = "",
    val imageUrl: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val openHours: String = "Horaires non renseignés",
    val closedDay: String = "",
    val effortLevel: Int = 1,
    val weatherType: String = "both",
    val bestTimeSlots: List<String> = listOf("apres-midi"),
    val tags: List<String> = emptyList(),
    val imageUrls: List<String> = emptyList(),
    val source: String = "travelshare",
    val sourcePostId: String = "",
    val destinationName: String = "",
    val authorId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val status: String = "active"
)

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
