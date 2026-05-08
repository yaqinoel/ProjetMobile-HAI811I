package com.example.traveling.features.travelshare.model

import kotlin.math.round

data class SelectedLocationUi(
    val name: String,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val rawLatitude: Double,
    val rawLongitude: Double,
    val displayLatitude: Double,
    val displayLongitude: Double,
    val precision: String, // "exact" / "approx"
    val googlePlaceId: String? = null,
    val source: String = "manual" // "search", "map_click", "default", "manual"
)

fun blurCoordinate(value: Double, gridSize: Double = 0.01): Double {
    return round(value / gridSize) * gridSize
}

fun buildSelectedLocation(
    name: String,
    rawLatitude: Double,
    rawLongitude: Double,
    precision: String,
    address: String? = null,
    city: String? = null,
    country: String? = null,
    googlePlaceId: String? = null,
    source: String = "manual"
): SelectedLocationUi {
    val displayLat = if (precision == "approx") blurCoordinate(rawLatitude) else rawLatitude
    val displayLng = if (precision == "approx") blurCoordinate(rawLongitude) else rawLongitude

    return SelectedLocationUi(
        name = name,
        address = address,
        city = city,
        country = country,
        rawLatitude = rawLatitude,
        rawLongitude = rawLongitude,
        displayLatitude = displayLat,
        displayLongitude = displayLng,
        precision = precision,
        googlePlaceId = googlePlaceId,
        source = source
    )
}
