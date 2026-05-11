package com.example.traveling.features.travelpath

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.traveling.data.model.TravelRoute
import com.example.traveling.ui.theme.*

//  RESULTS SCREEN
@Composable
internal fun ResultsScreen(
    travelViewModel: TravelViewModel,
    onBack: () -> Unit,
    onViewDetail: (String) -> Unit
) {
    val routes by travelViewModel.routes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        // ── Header ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardBg.copy(alpha = 0.9f),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Icon(
                        Icons.Default.ArrowBack, null,
                        modifier = Modifier.padding(8.dp).size(18.dp),
                        tint = StoneMuted
                    )
                }
                Column {
                    Text(
                        "Itinéraires Recommandés",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = StoneText, letterSpacing = 1.sp
                    )
                    Text(
                        "3 routes trouvées pour vous",
                        fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        color = StoneLighter
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recommendation tip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFFECACA))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Star, null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                    Text(
                        "La Route Équilibrée correspond le mieux à vos préférences",
                        fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF991B1B)
                    )
                }
            }

            // Route cards
            routes.forEachIndexed { index, route ->
                RouteCard(
                    route = route,
                    isRecommended = index == 1,
                    onViewDetail = { onViewDetail(route.id) }
                )
            }

            // Offline mode banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AmberLight,
                border = BorderStroke(1.dp, Color(0x80FDE68A))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.WifiOff, null, tint = StoneLighter, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Mode Hors Ligne", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneMuted)
                        Text(
                            "Téléchargez l'itinéraire pour consulter sans Internet",
                            fontSize = 11.sp, color = StoneLighter
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

//  ROUTE CARD COMPONENT
@Composable
internal fun RouteCard(
    route: TravelRoute,
    isRecommended: Boolean,
    onViewDetail: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, StoneBorder)
    ) {
        Column {
            // Image header
            Box(modifier = Modifier.height(144.dp).fillMaxWidth()) {
                AsyncImage(
                    model = route.imageUrl,
                    contentDescription = route.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )
                // Recommended badge
                if (isRecommended) {
                    Surface(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopStart),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF59E0B)
                    ) {
                        Text(
                            "Recommandé",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                    }
                }
                // Rating badge
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = AmberAccent, modifier = Modifier.size(12.dp))
                        Text("${route.rating}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
                // Title
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(route.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(route.subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatChip(
                        icon = Icons.Default.AttachMoney,
                        value = "${route.budget} €",
                        label = "Budget",
                        bgColor = Color(0xFFFEF2F2),
                        iconColor = RedPrimary,
                        borderColor = Color(0xFFFECACA),
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        icon = Icons.Default.Schedule,
                        value = route.duration,
                        label = "Durée",
                        bgColor = AmberLight,
                        iconColor = Color(0xFFB45309),
                        borderColor = Color(0xFFFDE68A),
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        icon = Icons.Outlined.DirectionsWalk,
                        value = "${route.stops}",
                        label = "Arrêts",
                        bgColor = Color(0xFFF5F5F4),
                        iconColor = StoneMuted,
                        borderColor = Color(0xFFE7E5E4),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Highlights timeline
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    route.highlights.take(4).forEachIndexed { i, highlight ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color(0xFFFEE2E2), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${i + 1}",
                                        fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RedPrimary
                                    )
                                }
                                if (i < 3) {
                                    Box(
                                        modifier = Modifier
                                            .width(1.5.dp)
                                            .height(8.dp)
                                            .background(Color(0xFFFEE2E2))
                                    )
                                }
                            }
                            Text(highlight, fontSize = 12.sp, color = StoneMuted)
                        }
                    }
                    if (route.highlights.size > 4) {
                        Text(
                            "+ ${route.highlights.size - 4} arrêts",
                            fontSize = 11.sp, color = StoneLighter,
                            modifier = Modifier.padding(start = 28.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onViewDetail,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(RedPrimary, RedDark))),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Voir Détails", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Surface(
                        onClick = { },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5F5F4)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Download, null, tint = StoneMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
