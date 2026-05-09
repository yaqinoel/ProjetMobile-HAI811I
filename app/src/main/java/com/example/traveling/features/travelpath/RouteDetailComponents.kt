package com.example.traveling.features.travelpath

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traveling.data.model.TimeSlot
import com.example.traveling.ui.theme.*

// Time slot styling data
internal data class TimeSlotStyle(
    val label: String,
    val icon: ImageVector,
    val bgColor: Color,
    val textColor: Color,
    val borderColor: Color
)

internal val timeSlotStyles = mapOf(
    TimeSlot.MATIN to TimeSlotStyle("Matin", Icons.Default.WbSunny, Color(0xFFFEF3C7), Color(0xFFB45309), Color(0xFFFDE68A)),
    TimeSlot.APRES_MIDI to TimeSlotStyle("Après-midi", Icons.Default.WbTwilight, Color(0xFFFFF7ED), Color(0xFFC2410C), Color(0xFFFED7AA)),
    TimeSlot.SOIR to TimeSlotStyle("Soir", Icons.Default.DarkMode, Color(0xFFEEF2FF), Color(0xFF4338CA), Color(0xFFC7D2FE))
)

// Glass-style icon button (used in hero header)
@Composable
internal fun IconButtonGlass(
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

// Stat item used in the stats bar
@Composable
internal fun DetailStatItem(
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
