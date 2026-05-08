package com.example.traveling.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PlacePredictionUi(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String?
)

data class SelectedLocationCandidate(
    val name: String,
    val address: String?,
    val city: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
    val googlePlaceId: String?,
    val source: String
)

class PlaceSearchRepository(context: Context) {
    private val appContext = context.applicationContext
    private val placesClient: PlacesClient? = createPlacesClient(appContext)

    suspend fun searchPlaces(query: String): Result<List<PlacePredictionUi>> {
        val client = placesClient ?: return Result.success(emptyList())
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 2) return Result.success(emptyList())

        return runCatching {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(trimmedQuery)
                .build()

            client.findAutocompletePredictions(request).awaitResult()
                .autocompletePredictions
                .map {
                    PlacePredictionUi(
                        placeId = it.placeId,
                        primaryText = it.getPrimaryText(null).toString(),
                        secondaryText = it.getSecondaryText(null).toString().ifBlank { null }
                    )
                }
        }
    }

    suspend fun fetchPlaceDetails(placeId: String): Result<SelectedLocationCandidate> {
        val client = placesClient ?: return Result.failure(IllegalStateException("Places SDK non initialisé"))

        return runCatching {
            val request = FetchPlaceRequest.builder(
                placeId,
                listOf(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.ADDRESS,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS_COMPONENTS
                )
            ).build()

            val place = client.fetchPlace(request).awaitResult().place
            val latLng = requireNotNull(place.latLng) { "Lieu sans coordonnées" }
            val components = place.addressComponents?.asList().orEmpty()

            SelectedLocationCandidate(
                name = place.name ?: place.address ?: "Lieu sélectionné",
                address = place.address,
                city = components.firstOrNull { "locality" in it.types }?.name
                    ?: components.firstOrNull { "postal_town" in it.types }?.name
                    ?: components.firstOrNull { "administrative_area_level_2" in it.types }?.name,
                country = components.firstOrNull { "country" in it.types }?.name,
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                googlePlaceId = place.id,
                source = "search"
            )
        }
    }

    suspend fun reverseLookup(latLng: LatLng): Result<List<SelectedLocationCandidate>> {
        return runCatching {
            val geocoderCandidates = reverseGeocodeWithAndroid(latLng)
            geocoderCandidates.ifEmpty {
                listOf(defaultCandidate(latLng))
            }
        }
    }

    fun defaultCandidate(latLng: LatLng): SelectedLocationCandidate {
        return SelectedLocationCandidate(
            name = "Position sélectionnée",
            address = "%.5f, %.5f".format(Locale.US, latLng.latitude, latLng.longitude),
            city = null,
            country = null,
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            googlePlaceId = null,
            source = "default"
        )
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocodeWithAndroid(latLng: LatLng): List<SelectedLocationCandidate> {
        return withContext(Dispatchers.IO) {
            val geocoder = Geocoder(appContext, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 3).orEmpty()
            addresses.mapIndexed { index, address ->
                val name = listOfNotNull(
                    address.featureName,
                    address.thoroughfare,
                    address.subLocality,
                    address.locality
                ).firstOrNull { !it.isNullOrBlank() } ?: "Position sélectionnée"

                SelectedLocationCandidate(
                    name = name,
                    address = address.getAddressLine(0),
                    city = address.locality ?: address.subAdminArea,
                    country = address.countryName,
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    googlePlaceId = null,
                    source = if (index == 0) "map_click" else "reverse_geocode"
                )
            }
        }
    }

    private fun createPlacesClient(context: Context): PlacesClient? {
        val apiKey = readMapsApiKey(context).takeIf { it.isNotBlank() } ?: return null
        if (!Places.isInitialized()) {
            Places.initialize(context, apiKey, Locale.getDefault())
        }
        return Places.createClient(context)
    }

    private fun readMapsApiKey(context: Context): String {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        return appInfo.metaData?.getString("com.google.android.geo.API_KEY").orEmpty()
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { ex ->
        if (cont.isActive) cont.resumeWithException(ex)
    }
}
