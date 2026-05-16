package com.example.traveling.data.model

data class TravelPathSeed(
    val sourcePostId: String,
    val placeName: String,
    val destinationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val placeType: String = "",
    val tags: List<String> = emptyList()
)