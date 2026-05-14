package com.example.traveling.features.travelpath

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traveling.ui.theme.*

@Composable
internal fun LoadingScreen() {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (progress < 1f) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed / 1800f).coerceAtMost(1f)
            kotlinx.coroutines.delay(16)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFFF59E0B)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Planification Intelligente en Cours...",
            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StoneText
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "L'IA génère le meilleur itinéraire\nselon vos préférences",
            fontSize = 14.sp, color = StoneLighter,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .width(192.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = RedPrimary,
            trackColor = Color(0xFFF5F5F4)
        )
    }
}
