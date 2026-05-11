package com.example.traveling.features.travelshare.model

data class GalleryFilter(
    val query: String = "",
    val placeType: String = "all",
    val period: String = "all",
    val dateRangeStartMillis: Long? = null,
    val dateRangeEndMillis: Long? = null,
    val discoveryMode: String = "all",
    val centerLatitude: Double? = null,
    val centerLongitude: Double? = null,
    val radiusKm: Double = 10.0,
    val similarToPostId: String? = null
)
