package com.example.traveling.features.travelpath

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traveling.ui.theme.*

//  SHARED COMPOSABLE COMPONENTS
/** Section card wrapper used throughout forms and results. */
@Composable
internal fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, StoneBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/** Small stat chip used in RouteCard (Budget, Duration, Stops). */
@Composable
internal fun StatChip(
    icon: ImageVector,
    value: String,
    label: String,
    bgColor: Color,
    iconColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StoneText)
            Text(label, fontSize = 10.sp, color = StoneLighter)
        }
    }
}

/** Toggle button for weather preferences (Froid, Chaleur, Humidité). */
@Composable
internal fun WeatherToggle(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    selectedColor: Color,
    selectedBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, if (selected) selectedColor else Color.Transparent),
        color = if (selected) selectedBg else Color(0xFFF5F5F4)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) selectedColor else StoneLighter
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                color = if (selected) selectedColor else StoneMuted
            )
        }
    }
}
