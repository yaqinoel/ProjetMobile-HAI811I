package com.example.traveling.features.travelpath

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traveling.data.model.RouteStop
import com.example.traveling.ui.theme.CardBg
import com.example.traveling.ui.theme.RedPrimary
import com.example.traveling.ui.theme.StoneBorder
import com.example.traveling.ui.theme.StoneText
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.*

@Composable
internal fun RouteMiniMap(stops: List<RouteStop>) {
    val validStops = stops.filter { it.lat != 0.0 && it.lng != 0.0 }
    if (validStops.size < 2) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = CardBg,
        border = BorderStroke(1.dp, StoneBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Aperçu du Trajet", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneText)
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {

                var mapLoaded by remember { mutableStateOf(false) }
                val cameraPositionState = rememberCameraPositionState()

                LaunchedEffect(validStops, mapLoaded) {
                    if (mapLoaded) {
                        val builder = LatLngBounds.builder()
                        validStops.forEach { builder.include(LatLng(it.lat, it.lng)) }

                        validStops.forEach { stop ->
                            if (!stop.polylineToNext.isNullOrEmpty()) {
                                stop.polylineToNext.split("||").forEach { polyStr ->
                                    val decoded = PolyUtil.decode(polyStr)
                                    decoded.forEach { builder.include(it) }
                                }
                            }
                        }

                        val bounds = builder.build()
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(bounds, 100)
                        )
                    }
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = { mapLoaded = true },
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        compassEnabled = false,
                        mapToolbarEnabled = false
                    )
                ) {

                    validStops.forEach { stop ->
                        if (!stop.polylineToNext.isNullOrEmpty()) {
                            stop.polylineToNext.split("||").forEach { polyStr ->
                                val decodedPath = PolyUtil.decode(polyStr)
                                Polyline(
                                    points = decodedPath,
                                    color = RedPrimary,
                                    width = 12f
                                )
                            }
                        }
                    }

                    validStops.forEachIndexed { index, stop ->
                        val position = LatLng(stop.lat, stop.lng)
                        val title = "${index + 1}. ${stop.name}"
                        Marker(
                            state = MarkerState(position = position),
                            title = title,
                            snippet = stop.duration
                        )
                    }
                }
            }
        }
    }
}
