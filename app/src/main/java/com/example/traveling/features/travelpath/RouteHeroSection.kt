package com.example.traveling.features.travelpath

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.traveling.ui.theme.*

@Composable
internal fun RouteHeroSection(
    heroImage: String,
    destName: String,
    routeName: String,
    routeSubtitle: String,
    routeRating: Float,
    routeReviews: Int,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.height(208.dp).fillMaxWidth()) {
        AsyncImage(
            model = heroImage,
            contentDescription = destName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButtonGlass(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.size(40.dp))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Star, null, tint = AmberAccent, modifier = Modifier.size(14.dp))
                Text("$routeRating", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text("($routeReviews avis)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
            }
            Text(
                "$routeName · $destName en un Jour",
                fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = Color.White, letterSpacing = 1.sp
            )
            Text(
                routeSubtitle,
                fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
