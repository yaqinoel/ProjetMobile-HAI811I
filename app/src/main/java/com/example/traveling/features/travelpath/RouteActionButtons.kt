package com.example.traveling.features.travelpath

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traveling.ui.theme.AmberLight
import com.example.traveling.ui.theme.RedPrimary
import com.example.traveling.ui.theme.StoneMuted

@Composable
internal fun RouteActionButtons(
    liked: Boolean,
    saved: Boolean,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onExportPdf: () -> Unit,
    onRegenerate: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RouteActionButton(
                text = if (liked) "Aime" else "Aimer",
                icon = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                onClick = onToggleLike,
                modifier = Modifier.weight(1f),
                contentColor = RedPrimary,
                backgroundColor = if (liked) Color(0xFFFEF2F2) else Color(0xFFFFF7F7),
                borderColor = Color(0xFFFECACA)
            )
            RouteActionButton(
                text = if (saved) "Enregistre" else "Enregistrer",
                icon = Icons.Default.Download,
                onClick = onToggleSave,
                modifier = Modifier.weight(1f),
                contentColor = if (saved) Color.White else RedPrimary,
                backgroundColor = if (saved) RedPrimary else Color(0xFFFEF2F2),
                borderColor = Color(0xFFFECACA)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RouteActionButton(
                text = "Exporter PDF",
                icon = Icons.Default.Description,
                onClick = onExportPdf,
                modifier = Modifier.weight(1f),
                contentColor = Color(0xFFB45309),
                backgroundColor = AmberLight,
                borderColor = Color(0xFFFDE68A)
            )
            RouteActionButton(
                text = "Partager",
                icon = Icons.Default.Share,
                onClick = onShare,
                modifier = Modifier.weight(1f),
                contentColor = StoneMuted,
                backgroundColor = Color(0xFFF5F5F4),
                borderColor = Color(0xFFE7E5E4)
            )
        }

        RouteActionButton(
            text = "Regenerer",
            icon = Icons.Default.Refresh,
            onClick = onRegenerate,
            modifier = Modifier.fillMaxWidth(),
            contentColor = StoneMuted,
            backgroundColor = Color(0xFFF5F5F4),
            borderColor = Color(0xFFE7E5E4)
        )
    }
}

@Composable
private fun RouteActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color,
    backgroundColor: Color,
    borderColor: Color
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
        }
    }
}
