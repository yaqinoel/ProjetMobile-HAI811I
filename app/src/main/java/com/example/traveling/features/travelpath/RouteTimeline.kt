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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.traveling.data.model.PhotoPostDocument
import com.example.traveling.data.model.RouteStop
import com.example.traveling.data.model.TimeSlot
import com.example.traveling.ui.theme.*

@Composable
internal fun RouteTimeline(
    groups: List<Pair<TimeSlot, List<RouteStop>>>,
    expandedStopId: String?,
    onToggleExpand: (String) -> Unit,
    stopTravelSharePhotos: Map<String, List<PhotoPostDocument>> = emptyMap(),
    onOpenPhotoDetail: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // les stops sont déjà groupés par moment de la journée dans le ViewModel
        groups.forEach { (slot, slotStops) ->
            if (slotStops.isEmpty()) return@forEach
            val style = timeSlotStyles[slot] ?: return@forEach

            Column {

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

                slotStops.forEachIndexed { idx, stop ->
                    val isExpanded = expandedStopId == stop.id

                    // chaque arrêt peut s'ouvrir pour afficher photos, coût et source
                    Column {

                        Row(modifier = Modifier.fillMaxWidth()) {

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

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            StopExpandedDetail(
                                stop = stop,
                                travelSharePhotos = stopTravelSharePhotos[stop.id].orEmpty(),
                                onOpenPhotoDetail = onOpenPhotoDetail
                            )
                        }

                        if (idx < slotStops.size - 1 && slotStops[idx + 1].distance != "Départ") {
                            // distance affichée entre l'arrêt actuel et le suivant
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

@Composable
private fun StopExpandedDetail(
    stop: RouteStop,
    travelSharePhotos: List<PhotoPostDocument> = emptyList(),
    onOpenPhotoDetail: (String) -> Unit = {}
) {
    Surface(
        modifier = Modifier.padding(start = 52.dp, top = 4.dp, bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = CardBg,
        border = BorderStroke(1.dp, StoneBorder)
    ) {
        Column {
            // on garde toutes les images disponibles, sans doublons
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

                if (stop.source == "travelshare" && !stop.sourcePostId.isNullOrBlank()) {
                    // un arrêt créé depuis TravelShare peut rouvrir le post original
                    Surface(
                        onClick = { onOpenPhotoDetail(stop.sourcePostId) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.TravelExplore, null, tint = Color(0xFF047857), modifier = Modifier.size(14.dp))
                            Text(
                                "Voir la publication TravelShare",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }

                if (travelSharePhotos.isNotEmpty()) {
                    // photos proches ou liées à cet arrêt, sans les mélanger au contenu officiel
                    Text(
                        "Photos partagées par les voyageurs",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StoneText
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(travelSharePhotos, key = { it.postId }) { post ->
                            TravelShareStopPhotoCard(
                                post = post,
                                onClick = { onOpenPhotoDetail(post.postId) }
                            )
                        }
                    }
                }

            }
        }
    }
}

@Composable
private fun TravelShareStopPhotoCard(
    post: PhotoPostDocument,
    onClick: () -> Unit
) {
    val title = post.title.ifBlank { post.locationName.ifBlank { "Photo TravelShare" } }
    val imageUrl = post.imageUrls.firstOrNull().orEmpty()

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8F5F1),
        border = BorderStroke(1.dp, StoneBorder),
        modifier = Modifier.width(140.dp)
    ) {
        Column {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .background(Stone100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhotoCamera, null, tint = Stone400)
                }
            }
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StoneText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    post.authorName.ifBlank { "Voyageur" },
                    fontSize = 10.sp,
                    color = StoneMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
