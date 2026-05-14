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
import com.example.traveling.data.repository.WeatherInfo
import com.example.traveling.ui.theme.*

@Composable
internal fun RouteStatsBar(
    stops: List<RouteStop>,
    routeDuration: String,
    weather: WeatherInfo? = null
) {

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

            val totalDist = stops.mapNotNull { s ->
                if (s.distance == "Départ") null
                else s.distance.replace(" km", "").replace(" m", "").replace(",", ".").toDoubleOrNull()
            }.sum()
            val distLabel = if (totalDist > 0) {
                if (totalDist < 1.0) "${(totalDist * 1000).toInt()} m" else "%.1f km".format(totalDist)
            } else "—"
            DetailStatItem(Icons.Default.LocationOn, distLabel, "Distance", Color(0xFF7C3AED))
        }
    }

    val emoji = weather?.emoji ?: "☀️"
    val title = if (weather != null) "Météo du jour : ${weather.description}, ${weather.temperature.toInt()}°C"
                else "Météo du jour : Chargement…"
    val advice = weather?.advice ?: "Consultez la météo locale"

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
            Text(emoji, fontSize = 24.sp)
            Column {
                Text(
                    title,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF92400E)
                )
                Text(
                    advice,
                    fontSize = 11.sp, color = Color(0xFFD97706)
                )
            }
        }
    }
}
