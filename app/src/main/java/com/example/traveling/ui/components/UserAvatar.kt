package com.example.traveling.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun UserAvatar(
    avatarUrl: String?,
    fallbackText: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    textSize: TextUnit = 14.sp
) {
    val normalizedAvatarUrl = avatarUrl?.takeIf { it.isNotBlank() }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (normalizedAvatarUrl != null) {
            AsyncImage(
                model = normalizedAvatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = fallbackText.ifBlank { "V" },
                color = Color.White,
                fontSize = textSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
