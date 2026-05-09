package com.example.traveling.features.travelpath

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traveling.data.model.TimeSlot
import com.example.traveling.ui.theme.*

// Route detail page — assembles hero, stats, map, timeline, and action buttons
@Composable
fun RouteDetailScreen(
    routeId: String,
    onBack: () -> Unit,
    travelViewModel: TravelViewModel = viewModel()
) {
    val context = LocalContext.current
    val stops by travelViewModel.routeStops.collectAsState()
    val selectedDest by travelViewModel.selectedDestination.collectAsState()
    val selectedRoute by travelViewModel.selectedRoute.collectAsState()
    val weather by travelViewModel.weather.collectAsState()
    val pdfExportPath by travelViewModel.pdfExportPath.collectAsState()

    // Initialize local storage for SharedPreferences access
    LaunchedEffect(Unit) {
        travelViewModel.initLocalStorage(context)
        travelViewModel.fetchWeather()
    }

    // Like / Save state persisted via SharedPreferences
    var liked by remember(routeId) {
        mutableStateOf(travelViewModel.isRouteLiked(routeId))
    }
    var saved by remember(routeId) {
        mutableStateOf(travelViewModel.isRouteSaved(routeId))
    }
    var expandedStopId by remember { mutableStateOf<String?>("s1") }

    // Handle PDF export result
    LaunchedEffect(pdfExportPath) {
        pdfExportPath?.let { file ->
            Toast.makeText(context, "PDF enregistré : ${file.name}", Toast.LENGTH_LONG).show()
            travelViewModel.clearPdfExport()
        }
    }

    val destName = selectedDest?.name ?: "Destination"
    val routeName = selectedRoute?.name ?: "Route"
    val routeSubtitle = selectedRoute?.subtitle ?: ""
    val routeRating = selectedRoute?.rating ?: 4.5f
    val routeReviews = selectedRoute?.reviews ?: 0
    val heroImage = selectedDest?.imageUrl ?: ""
    val groups = listOf(
        TimeSlot.MATIN to stops.filter { it.timeSlot == TimeSlot.MATIN },
        TimeSlot.APRES_MIDI to stops.filter { it.timeSlot == TimeSlot.APRES_MIDI },
        TimeSlot.SOIR to stops.filter { it.timeSlot == TimeSlot.SOIR }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Hero image with back/like/share
            RouteHeroSection(
                heroImage = heroImage,
                destName = destName,
                routeName = routeName,
                routeSubtitle = routeSubtitle,
                routeRating = routeRating,
                routeReviews = routeReviews,
                liked = liked,
                onBack = onBack,
                onToggleLike = {
                    liked = travelViewModel.toggleRouteLike(routeId)
                },
                onShare = {
                    val intent = travelViewModel.buildShareIntent()
                    context.startActivity(intent)
                }
            )

            // 2. Stats bar + real weather info
            RouteStatsBar(
                stops = stops,
                routeDuration = selectedRoute?.duration ?: "5-6h",
                weather = weather
            )

            // 3. Mini map
            RouteMiniMap(stops = stops)

            // 4. Stops timeline
            RouteTimeline(
                groups = groups,
                expandedStopId = expandedStopId,
                onToggleExpand = { id ->
                    expandedStopId = if (expandedStopId == id) null else id
                }
            )

            Spacer(Modifier.height(16.dp))

            // 5. Action buttons + offline banner
            RouteActionButtons(
                saved = saved,
                onToggleSave = {
                    saved = travelViewModel.toggleRouteSave(routeId)
                },
                onExportPdf = {
                    travelViewModel.exportPdf(context)
                },
                onRegenerate = {
                    travelViewModel.regenerateCurrentRoute()
                },
                onShare = {
                    val intent = travelViewModel.buildShareIntent()
                    context.startActivity(intent)
                },
                onDownloadOffline = {
                    travelViewModel.cacheCurrentRoute()
                    Toast.makeText(context, "Itinéraire téléchargé pour usage hors ligne", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RouteDetailScreenPreview() {
    RouteDetailScreen(
        routeId = "1",
        onBack = { }
    )
}