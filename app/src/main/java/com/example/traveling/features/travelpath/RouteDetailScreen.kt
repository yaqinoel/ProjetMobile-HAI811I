package com.example.traveling.features.travelpath

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.traveling.data.model.RouteStop
import com.example.traveling.data.model.TimeSlot
import com.example.traveling.data.model.TravelPathData
import androidx.lifecycle.viewmodel.compose.viewModel

// ─── Colors ───
private val RedPrimary = Color(0xFFB91C1C)
private val CardBg = Color(0xFFFFFBF5)
private val PageBg = Color(0xFFFDF8F4)
private val AmberAccent = Color(0xFFFBBF24)
private val AmberLight = Color(0xFFFEF3C7)
private val StoneText = Color(0xFF44403C)
private val StoneMuted = Color(0xFF78716C)
private val StoneLighter = Color(0xFFA8A29E)
private val StoneBorder = Color(0x14795548)

// ─── Time slot styling ───
private data class TimeSlotStyle(
    val label: String,
    val icon: ImageVector,
    val bgColor: Color,
    val textColor: Color,
    val borderColor: Color
)

private val timeSlotStyles = mapOf(
    TimeSlot.MATIN to TimeSlotStyle("Matin", Icons.Default.WbSunny, Color(0xFFFEF3C7), Color(0xFFB45309), Color(0xFFFDE68A)),
    TimeSlot.APRES_MIDI to TimeSlotStyle("Après-midi", Icons.Default.WbTwilight, Color(0xFFFFF7ED), Color(0xFFC2410C), Color(0xFFFED7AA)),
    TimeSlot.SOIR to TimeSlotStyle("Soir", Icons.Default.DarkMode, Color(0xFFEEF2FF), Color(0xFF4338CA), Color(0xFFC7D2FE))
)

@Composable
fun RouteDetailScreen(
    routeId: String,
    onBack: () -> Unit,
    travelViewModel: TravelViewModel = viewModel()
) {
    val stops by travelViewModel.routeStops.collectAsState()
    val selectedDest by travelViewModel.selectedDestination.collectAsState()
    val selectedRoute by travelViewModel.selectedRoute.collectAsState()
    var liked by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var expandedStopId by remember { mutableStateOf<String?>("s1") }

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
            // ── Hero Image ──
            Box(modifier = Modifier.height(208.dp).fillMaxWidth()) {
                AsyncImage(
                    model = heroImage,
                    contentDescription = destName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )

                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButtonGlass(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButtonGlass(onClick = { liked = !liked }) {
                            Icon(
                                if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                null,
                                tint = if (liked) Color(0xFFEF4444) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButtonGlass(onClick = { }) {
                            Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Bottom info
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = AmberAccent, modifier = Modifier.size(14.dp))
                        Text("$routeRating", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("($routeReviews avis)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                    Text(
                        "$routeName · $destName en un Jour",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = Color.White, letterSpacing = 1.sp
                    )
                    Text(
                        routeSubtitle,
                        fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // ── Stats Bar ──
            Surface(
                color = CardBg,
                border = BorderStroke(1.dp, StoneBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val totalCost = stops.sumOf { it.cost }
                    val routeDuration = selectedRoute?.duration ?: "5-6h"
                    StatItem(Icons.Default.AttachMoney, "$totalCost €", "Budget", RedPrimary)
                    StatItem(Icons.Default.Schedule, routeDuration, "Durée", Color(0xFFB45309))
                    StatItem(Icons.Outlined.DirectionsWalk, "${stops.size}", "Arrêts", StoneMuted)
                    StatItem(Icons.Default.LocationOn, "~20km", "Distance", Color(0xFF7C3AED))
                }
            }

            // ── Weather Info ──
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = AmberLight,
                border = BorderStroke(1.dp, Color(0x80FDE68A))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("☀️", fontSize = 24.sp)
                    Column {
                        Text(
                            "Météo du jour : Ensoleillé, 22°C",
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF92400E)
                        )
                        Text(
                            "Idéal pour les visites en plein air",
                            fontSize = 11.sp, color = Color(0xFFD97706)
                        )
                    }
                }
            }

            // ── Interactive Mini-Map ──
            RouteMiniMap(stops = stops)

            // ── Stops Timeline ──
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groups.forEach { (slot, slotStops) ->
                    if (slotStops.isEmpty()) return@forEach
                    val style = timeSlotStyles[slot] ?: return@forEach

                    Column {
                        // Time slot header
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = style.bgColor,
                            border = BorderStroke(1.dp, style.borderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(style.icon, null, tint = style.textColor, modifier = Modifier.size(16.dp))
                                Text(style.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = style.textColor)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Stop items
                        slotStops.forEachIndexed { idx, stop ->
                            val isExpanded = expandedStopId == stop.id

                            Column {
                                // Stop header (clickable)
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    // Timeline indicator
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(40.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(Color(0xFFFEE2E2), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${stop.arrivalTime.split(":")[0]}h",
                                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RedPrimary
                                            )
                                        }
                                        if (idx < slotStops.size - 1) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(if (isExpanded) 8.dp else 24.dp)
                                                    .background(Color(0xFFFEE2E2))
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    // Stop info
                                    Surface(
                                        onClick = { expandedStopId = if (isExpanded) null else stop.id },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Transparent
                                    ) {
                                        Column(modifier = Modifier.padding(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    stop.name,
                                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                                    color = StoneText, modifier = Modifier.weight(1f)
                                                )
                                                Icon(
                                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    null, tint = StoneLighter, modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFF5F5F4)
                                                ) {
                                                    Text(
                                                        stop.type,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        fontSize = 10.sp, color = StoneLighter
                                                    )
                                                }
                                                Text(stop.duration, fontSize = 10.sp, color = StoneLighter)
                                                if (stop.cost > 0) {
                                                    Text(
                                                        "${stop.cost} €",
                                                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                                        color = RedPrimary
                                                    )
                                                } else {
                                                    Text(
                                                        "Gratuit",
                                                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF059669)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Expanded detail
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Surface(
                                        modifier = Modifier.padding(start = 52.dp, top = 4.dp, bottom = 8.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = CardBg,
                                        border = BorderStroke(1.dp, StoneBorder)
                                    ) {
                                        Column {
                                            val galleryImages = listOf(
                                                stop.imageUrl,
                                                "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400",
                                                "https://images.unsplash.com/photo-1544985361-b420d7a77043?w=400"
                                            ).distinct()

                                            LazyRow(
                                                modifier = Modifier.fillMaxWidth().height(128.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 0.dp)
                                            ) {
                                                items(galleryImages) { url ->
                                                    AsyncImage(
                                                        model = url,
                                                        contentDescription = stop.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .fillMaxHeight()
                                                            .width(200.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                    )
                                                }
                                            }
                                            
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    stop.description,
                                                    fontSize = 12.sp, color = StoneMuted,
                                                    lineHeight = 18.sp
                                                )

                                                // Rating + hours
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Star, null,
                                                            tint = AmberAccent,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            "${stop.rating}",
                                                            fontSize = 11.sp, color = StoneLighter
                                                        )
                                                    }
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Schedule, null,
                                                            tint = StoneLighter,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            stop.openHours,
                                                            fontSize = 11.sp, color = StoneLighter
                                                        )
                                                    }
                                                }

                                                // Distance from previous
                                                if (stop.distance != "Départ") {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFFF5F5F4)
                                                    ) {
                                                        Text(
                                                            "Distance depuis la précédente étape : ${stop.distance} (${stop.walkTime})",
                                                            modifier = Modifier.padding(
                                                                horizontal = 12.dp,
                                                                vertical = 8.dp
                                                            ),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = StoneMuted
                                                        )
                                                    }
                                                }

                                                // Action buttons
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(
                                                        8.dp
                                                    )
                                                ) {
                                                    Surface(
                                                        onClick = { },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(32.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFFFEF2F2),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            Color(0xFFFECACA)
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxSize(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.Center
                                                        ) {
                                                            Icon(
                                                                Icons.Default.CameraAlt,
                                                                null,
                                                                tint = RedPrimary,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(
                                                                "Photos",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = RedPrimary
                                                            )
                                                        }
                                                    }
                                                    Surface(
                                                        onClick = { },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(32.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = AmberLight,
                                                        border = BorderStroke(
                                                            1.dp,
                                                            Color(0xFFFDE68A)
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxSize(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.Center
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Navigation,
                                                                null,
                                                                tint = Color(0xFFB45309),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(
                                                                "Navigation",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = Color(0xFFB45309)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Distance connector between stops
                                if (idx < slotStops.size - 1 && slotStops[idx + 1].distance != "Départ") {
                                    Row(
                                        modifier = Modifier.padding(start = 52.dp, top = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.DirectionsWalk, null,
                                            tint = Color(0xFFD6D3D1), modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            "${slotStops[idx + 1].distance} - ${slotStops[idx + 1].walkTime}",
                                            fontSize = 10.sp, color = Color(0xFFD6D3D1)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Bottom Action Buttons ──
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Save button
                    Surface(
                        onClick = { saved = !saved },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (saved) RedPrimary else Color(0xFFFEF2F2),
                        border = if (saved) BorderStroke(1.dp, Color(0xFFDC2626)) else BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Download, null,
                                tint = if (saved) Color.White else RedPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (saved) "Enregistré" else "Enregistrer l'itinéraire",
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                color = if (saved) Color.White else RedPrimary
                            )
                        }
                    }
                    // Export PDF
                    Surface(
                        onClick = { },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = AmberLight,
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Description, null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Exporter en PDF", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB45309))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5F5F4)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = StoneMuted, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Regénérer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = StoneMuted)
                        }
                    }
                    Surface(
                        onClick = { },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5F5F4)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.ThumbDown, null, tint = StoneMuted, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Inapproprié", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = StoneMuted)
                        }
                    }
                }

                // Offline banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmberLight,
                    border = BorderStroke(1.dp, Color(0x80FDE68A))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WifiOff, null, tint = StoneLighter, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Disponible hors ligne", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneMuted)
                            Text("Téléchargez pour consulter sans connexion internet", fontSize = 11.sp, color = StoneLighter)
                        }
                        Surface(
                            onClick = { },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE7E5E4))
                        ) {
                            Text(
                                "Télécharger",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RedPrimary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp)) // Bottom nav padding
        }
    }
}

// ─── Helper Composables ───

@Composable
private fun IconButtonGlass(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.3f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StoneText)
        Text(label, fontSize = 9.sp, color = StoneLighter)
    }
}

// ═════════════════════════════════════════════
//  MINI MAP (CANVAS)
// ═════════════════════════════════════════════
@Composable
private fun RouteMiniMap(stops: List<RouteStop>) {
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
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE7E5E4))
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    val w = size.width
                    val h = size.height

                    val minLat = validStops.minOf { it.lat }
                    val maxLat = validStops.maxOf { it.lat }
                    val minLng = validStops.minOf { it.lng }
                    val maxLng = validStops.maxOf { it.lng }

                    val latDiff = (maxLat - minLat).coerceAtLeast(0.0001)
                    val lngDiff = (maxLng - minLng).coerceAtLeast(0.0001)

                    val points = validStops.map {
                        // Invert Y mapping because latitude increases going North
                        androidx.compose.ui.geometry.Offset(
                            x = ((it.lng - minLng) / lngDiff * w).toFloat(),
                            y = (h - (it.lat - minLat) / latDiff * h).toFloat()
                        )
                    }

                    // Draw path
                    val path = androidx.compose.ui.graphics.Path()
                    points.forEachIndexed { index, point ->
                        if (index == 0) path.moveTo(point.x, point.y)
                        else path.lineTo(point.x, point.y)
                    }

                    drawPath(
                        path = path,
                        color = RedPrimary,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )

                    // Draw points
                    points.forEachIndexed { index, point ->
                        val isFirst = index == 0
                        val isLast = index == points.size - 1
                        val innerColor = if (isFirst) Color(0xFF10B981) else if (isLast) Color(0xFFEF4444) else Color.White

                        drawCircle(color = RedPrimary, radius = 5.dp.toPx(), center = point)
                        drawCircle(color = innerColor, radius = 3.dp.toPx(), center = point)
                    }
                }
            }
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