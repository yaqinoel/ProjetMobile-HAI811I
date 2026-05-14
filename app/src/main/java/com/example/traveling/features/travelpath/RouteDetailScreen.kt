package com.example.traveling.features.travelpath

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traveling.data.model.TimeSlot
import com.example.traveling.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RouteDetailScreen(
    routeId: String,
    onBack: () -> Unit,
    onOpenPhotoDetail: (String) -> Unit = {},
    travelViewModel: TravelViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stops by travelViewModel.routeStops.collectAsState()
    val selectedDest by travelViewModel.selectedDestination.collectAsState()
    val selectedRoute by travelViewModel.selectedRoute.collectAsState()
    val weather by travelViewModel.weather.collectAsState()
    val pdfExportPath by travelViewModel.pdfExportPath.collectAsState()
    val stopTravelSharePhotos by travelViewModel.stopTravelSharePhotos.collectAsState()

    LaunchedEffect(Unit) {
        travelViewModel.initLocalStorage(context)
        travelViewModel.fetchWeather()
    }

    var liked by remember(routeId) {
        mutableStateOf(travelViewModel.isRouteLiked(routeId))
    }
    var saved by remember(routeId) {
        mutableStateOf(travelViewModel.isRouteSaved(routeId))
    }
    var expandedStopId by remember { mutableStateOf<String?>("s1") }
    var showRegenerateDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(stops) {
        if (stops.isNotEmpty()) {
            travelViewModel.loadTravelSharePhotosForStops(stops)
        }
    }

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

            RouteStatsBar(
                stops = stops,
                routeDuration = selectedRoute?.duration ?: "5-6h",
                weather = weather
            )

            RouteMiniMap(stops = stops)

            RouteTimeline(
                groups = groups,
                expandedStopId = expandedStopId,
                onToggleExpand = { id ->
                    expandedStopId = if (expandedStopId == id) null else id
                },
                stopTravelSharePhotos = stopTravelSharePhotos,
                onOpenPhotoDetail = onOpenPhotoDetail
            )

            Spacer(Modifier.height(16.dp))

            RouteActionButtons(
                saved = saved,
                onToggleSave = {
                    saved = travelViewModel.toggleRouteSave(routeId)
                },
                onExportPdf = {
                    travelViewModel.exportPdf(context)
                },
                onRegenerate = {
                    showRegenerateDialog = true
                },
                onShare = {
                    val intent = travelViewModel.buildShareIntent()
                    context.startActivity(intent)
                },
                onDownloadOffline = {
                    scope.launch {
                        val cached = travelViewModel.cacheCurrentRouteLite()
                        val message = if (cached) {
                            "Parcours et médias compressés disponibles hors ligne"
                        } else {
                            "Impossible de préparer le mode hors ligne"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(Modifier.height(80.dp))
        }
    }

    if (showRegenerateDialog) {
        RegenerateAdjustmentDialog(
            onDismiss = { showRegenerateDialog = false },
            onSelect = { adjustment ->
                travelViewModel.regenerateCurrentRoute(adjustment)
                showRegenerateDialog = false
                Toast.makeText(context, "Itinéraire régénéré", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun RegenerateAdjustmentDialog(
    onDismiss: () -> Unit,
    onSelect: (TravelViewModel.RegenerationAdjustment) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Régénération avec ajustements",
                fontWeight = FontWeight.Bold,
                color = StoneText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdjustmentButton(
                    label = "Plus d'activités à l'abri",
                    icon = { Icon(Icons.Default.Chair, null) },
                    onClick = { onSelect(TravelViewModel.RegenerationAdjustment.INDOOR) }
                )
                AdjustmentButton(
                    label = "Moins cher",
                    icon = { Icon(Icons.Default.AttachMoney, null) },
                    onClick = { onSelect(TravelViewModel.RegenerationAdjustment.CHEAPER) }
                )
                AdjustmentButton(
                    label = "Moins de marche",
                    icon = { Icon(Icons.Default.DirectionsWalk, null) },
                    onClick = { onSelect(TravelViewModel.RegenerationAdjustment.LESS_WALKING) }
                )
                AdjustmentButton(
                    label = "Surprends-moi",
                    icon = { Icon(Icons.Default.Casino, null) },
                    onClick = { onSelect(TravelViewModel.RegenerationAdjustment.SURPRISE) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent
            ) {
                Text(
                    "Annuler",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = StoneMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        containerColor = CardBg
    )
}

@Composable
private fun AdjustmentButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8F5F1),
        border = BorderStroke(1.dp, Color(0xFFE7E5E4))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF2F2),
                contentColor = RedPrimary
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = StoneText)
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
