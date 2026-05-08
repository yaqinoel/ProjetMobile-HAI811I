package com.example.traveling.features.travelshare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traveling.data.repository.PlacePredictionUi
import com.example.traveling.data.repository.PlaceSearchRepository
import com.example.traveling.data.repository.SelectedLocationCandidate
import com.example.traveling.features.travelshare.model.SelectedLocationUi
import com.example.traveling.features.travelshare.model.buildSelectedLocation
import com.example.traveling.ui.theme.CardBg
import com.example.traveling.ui.theme.RedPrimary
import com.example.traveling.ui.theme.Stone400
import com.example.traveling.ui.theme.Stone500
import com.example.traveling.ui.theme.Stone800
import com.example.traveling.ui.theme.StoneBorder
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MapLocationPickerOverlay(
    initialLocation: SelectedLocationUi?,
    onDismiss: () -> Unit,
    onConfirm: (SelectedLocationUi) -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { PlaceSearchRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val defaultLatLng = LatLng(43.6108, 3.8767) // Montpellier

    var selectedLatLng by remember {
        mutableStateOf(initialLocation?.let { LatLng(it.rawLatitude, it.rawLongitude) } ?: defaultLatLng)
    }
    var precision by remember { mutableStateOf(initialLocation?.precision ?: "exact") }
    var searchQuery by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf(emptyList<PlacePredictionUi>()) }
    var isSearching by remember { mutableStateOf(false) }
    var isResolvingPlace by remember { mutableStateOf(false) }
    var selectedCandidate by remember { mutableStateOf(initialLocation?.toCandidate() ?: repository.defaultCandidate(selectedLatLng)) }
    var editableName by remember { mutableStateOf(initialLocation?.name ?: selectedCandidate.name) }
    var placeError by remember { mutableStateOf<String?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedLatLng, 13f)
    }
    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = true,
            scrollGesturesEnabled = true,
            zoomGesturesEnabled = true,
            rotationGesturesEnabled = true,
            tiltGesturesEnabled = true,
            mapToolbarEnabled = false
        )
    }
    val mapProperties = remember { MapProperties(isBuildingEnabled = true) }

    LaunchedEffect(searchQuery) {
        val query = searchQuery.trim()
        if (query.length < 2) {
            predictions = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(350)
        repository.searchPlaces(query)
            .onSuccess {
                predictions = it
                placeError = null
            }
            .onFailure {
                predictions = emptyList()
                placeError = "Recherche indisponible"
            }
        isSearching = false
    }

    fun selectCandidate(candidate: SelectedLocationCandidate) {
        selectedCandidate = candidate
        selectedLatLng = LatLng(candidate.latitude, candidate.longitude)
        editableName = candidate.name
    }

    fun resolveMapClick(latLng: LatLng) {
        selectedLatLng = latLng
        searchQuery = ""
        predictions = emptyList()
        val fallback = repository.defaultCandidate(latLng)
        selectedCandidate = fallback
        editableName = fallback.name
        placeError = null
        isResolvingPlace = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f))) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = mapUiSettings,
            properties = mapProperties,
            onMapClick = { latLng -> resolveMapClick(latLng) },
            onPOIClick = { poi ->
                val candidate = SelectedLocationCandidate(
                    name = poi.name,
                    address = null,
                    city = null,
                    country = null,
                    latitude = poi.latLng.latitude,
                    longitude = poi.latLng.longitude,
                    googlePlaceId = poi.placeId,
                    source = "map_poi"
                )
                selectCandidate(candidate)
                coroutineScope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(poi.latLng, 16f)
                    )
                }
                searchQuery = ""
                predictions = emptyList()
                placeError = null
            }
        ) {
            Marker(
                state = MarkerState(position = selectedLatLng),
                title = editableName.ifBlank { "Position sélectionnée" }
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth().statusBarsPadding(),
            color = CardBg.copy(alpha = 0.97f)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Stone800)
                    }
                    Text("Choisir un lieu", color = Stone800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                SearchBox(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    onClear = {
                        searchQuery = ""
                        predictions = emptyList()
                    },
                    isSearching = isSearching
                )

                if (predictions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                            items(predictions, key = { it.placeId }) { prediction ->
                                PredictionRow(
                                    prediction = prediction,
                                    onClick = {
                                        isResolvingPlace = true
                                        coroutineScope.launch {
                                            repository.fetchPlaceDetails(prediction.placeId)
                                                .onSuccess { candidate ->
                                                    selectCandidate(candidate)
                                                    cameraPositionState.animate(
                                                        CameraUpdateFactory.newLatLngZoom(
                                                            LatLng(candidate.latitude, candidate.longitude),
                                                            15f
                                                        )
                                                    )
                                                    searchQuery = ""
                                                    predictions = emptyList()
                                                    placeError = null
                                                }
                                                .onFailure {
                                                    placeError = "Impossible de charger ce lieu"
                                                }
                                            isResolvingPlace = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = CardBg,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Lieu sélectionné", color = Stone800, style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = editableName,
                    onValueChange = { editableName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = RedPrimary) },
                    label = { Text("Nom du lieu") },
                    placeholder = { Text("Ex : Cité Interdite, Musée Fabre...") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = StoneBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                selectedCandidate.address?.let {
                    Text(it, color = Stone500, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    "%.5f, %.5f".format(selectedLatLng.latitude, selectedLatLng.longitude),
                    color = Stone400,
                    fontSize = 11.sp
                )

                if (isResolvingPlace) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = RedPrimary)
                }

                placeError?.let {
                    Text(it, color = Color(0xFFB45309), fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = precision == "exact",
                        onClick = { precision = "exact" },
                        label = { Text("Exacte") }
                    )
                    FilterChip(
                        selected = precision == "approx",
                        onClick = { precision = "approx" },
                        label = { Text("Approximative") }
                    )
                }

                Button(
                    onClick = {
                        onConfirm(
                            buildSelectedLocation(
                                name = editableName.ifBlank { selectedCandidate.name },
                                rawLatitude = selectedLatLng.latitude,
                                rawLongitude = selectedLatLng.longitude,
                                precision = precision,
                                address = selectedCandidate.address,
                                city = selectedCandidate.city,
                                country = selectedCandidate.country,
                                googlePlaceId = selectedCandidate.googlePlaceId,
                                source = selectedCandidate.source
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Confirmer")
                }
            }
        }
    }
}

@Composable
private fun SearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    isSearching: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = Stone500, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = Stone800, fontSize = 14.sp),
            decorationBox = { inner ->
                if (value.isBlank()) Text("Rechercher un lieu...", color = Stone400, fontSize = 14.sp)
                inner()
            }
        )
        if (isSearching) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = RedPrimary)
        } else if (value.isNotBlank()) {
            IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Effacer", tint = Stone500, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun PredictionRow(
    prediction: PlacePredictionUi,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.TravelExplore, null, tint = RedPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(prediction.primaryText, fontSize = 14.sp, color = Stone800, fontWeight = FontWeight.SemiBold)
            prediction.secondaryText?.let {
                Text(it, fontSize = 12.sp, color = Stone500, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun SelectedLocationUi.toCandidate(): SelectedLocationCandidate {
    return SelectedLocationCandidate(
        name = name,
        address = address,
        city = city,
        country = country,
        latitude = rawLatitude,
        longitude = rawLongitude,
        googlePlaceId = googlePlaceId,
        source = source
    )
}
