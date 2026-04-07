package com.example.traveling.data.model

data class TravelRoute(
    val id: String,
    val name: String,
    val subtitle: String,
    val budget: Int,
    val duration: String,
    val effort: String,
    val effortLevel: Int,
    val stops: Int,
    val rating: Float,
    val reviews: Int,
    val imageUrl: String,
    val highlights: List<String>,
    val gradientColors: Pair<Long, Long> // Start and end gradient color
)

data class RouteStop(
    val id: String,
    val name: String,
    val type: String,
    val timeSlot: TimeSlot,
    val arrivalTime: String,
    val duration: String,
    val distance: String,
    val walkTime: String,
    val cost: Int,
    val description: String,
    val imageUrl: String,
    val rating: Float,
    val openHours: String,
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

enum class TimeSlot(val label: String) {
    MATIN("Matin"),
    APRES_MIDI("Après-midi"),
    SOIR("Soir")
}

data class Activity(
    val id: String,
    val label: String,
    val icon: String
)

// Données UI statiques (labels et villes par défaut)
object TravelPathData {

    val activities = listOf(
        Activity("culture", "Culture", "🏛️"),
        Activity("food", "Gastronomie", "🍽️"),
        Activity("nature", "Nature", "🌳"),
        Activity("leisure", "Loisirs", "🎮"),
        Activity("shopping", "Shopping", "🛍️"),
        Activity("nightlife", "Vie nocturne", "🌃"),
        Activity("sport", "Sport", "⚽"),
        Activity("photo", "Photo", "📸"),
    )

    // Villes par défaut (sera remplacé par Firestore si disponible)
    val defaultQuickCities = listOf("Paris", "Lyon", "Nice", "Pékin", "Hangzhou")
}
