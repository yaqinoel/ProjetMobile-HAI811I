package com.example.traveling.features.travelpath

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traveling.data.model.RouteStop
import com.example.traveling.ui.theme.*

// Stats bar (Budget / Duration / Stops / Distance) + Weather info banner
@Composable
internal fun RouteStatsBar(
    stops: List<RouteStop>,
    routeDuration: String
) {
    // Stats row
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
            DetailStatItem(Icons.Default.AttachMoney, "$totalCost €", "Budget", RedPrimary)
            DetailStatItem(Icons.Default.Schedule, routeDuration, "Durée", Color(0xFFB45309))
            DetailStatItem(Icons.Outlined.DirectionsWalk, "${stops.size}", "Arrêts", StoneMuted)
            DetailStatItem(Icons.Default.LocationOn, "~20km", "Distance", Color(0xFF7C3AED))
        }
    }

    // Weather info
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
}
