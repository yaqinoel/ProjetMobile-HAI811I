package com.example.traveling.features.travelpath

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traveling.ui.theme.*

// Bottom action buttons: Save / Export PDF / Regenerate / Report + Offline banner
@Composable
internal fun RouteActionButtons(
    saved: Boolean,
    onToggleSave: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Save button
            Surface(
                onClick = onToggleSave,
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
}
