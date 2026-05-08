package com.example.traveling.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

fun openNavigationToPlace(
    context: Context,
    placeName: String,
    latitude: Double?,
    longitude: Double?
): Boolean {
    val safeName = placeName.ifBlank { "Destination" }
    val encodedName = Uri.encode(safeName)

    val googleUri = if (latitude != null && longitude != null) {
        Uri.parse("google.navigation:q=$latitude,$longitude")
    } else {
        Uri.parse("geo:0,0?q=$encodedName")
    }

    val googleIntent = Intent(Intent.ACTION_VIEW, googleUri).apply {
        setPackage("com.google.android.apps.maps")
    }

    if (googleIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(googleIntent)
        return true
    }

    val fallbackUri = if (latitude != null && longitude != null) {
        Uri.parse("geo:0,0?q=$latitude,$longitude($encodedName)")
    } else {
        Uri.parse("geo:0,0?q=$encodedName")
    }

    val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
    if (fallbackIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(fallbackIntent)
        return true
    }

    return false
}
