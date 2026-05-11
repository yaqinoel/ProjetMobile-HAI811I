package com.example.traveling.features.travelpath

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.traveling.data.model.RouteStop
import com.example.traveling.data.model.TimeSlot
import com.example.traveling.ui.theme.*

// Timeline section showing stops grouped by Matin / Après-midi / Soir
@Composable
internal fun RouteTimeline(
    groups: List<Pair<TimeSlot, List<RouteStop>>>,
    expandedStopId: String?,
    onToggleExpand: (String) -> Unit
) {
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
                                onClick = { onToggleExpand(stop.id) },
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
                            StopExpandedDetail(stop = stop)
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
}

// Expanded detail card for a single stop (gallery + description + actions)
@Composable
private fun StopExpandedDetail(stop: RouteStop) {
    Surface(
        modifier = Modifier.padding(start = 52.dp, top = 4.dp, bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = CardBg,
        border = BorderStroke(1.dp, StoneBorder)
    ) {
        Column {
            val galleryImages = stop.imageUrls
                .ifEmpty { listOf(stop.imageUrl) }
                .filter { it.isNotBlank() }
                .distinct()

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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = AmberAccent, modifier = Modifier.size(12.dp))
                        Text("${stop.rating}", fontSize = 11.sp, color = StoneLighter)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Schedule, null, tint = StoneLighter, modifier = Modifier.size(12.dp))
                        Text(stop.openHours, fontSize = 11.sp, color = StoneLighter)
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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = StoneMuted
                        )
                    }
                }

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { },
                        modifier = Modifier.weight(1f).height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = RedPrimary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Photos", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RedPrimary)
                        }
                    }
                    Surface(
                        onClick = { },
                        modifier = Modifier.weight(1f).height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = AmberLight,
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Navigation, null, tint = Color(0xFFB45309), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Navigation", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB45309))
                        }
                    }
                }
            }
        }
    }
}
