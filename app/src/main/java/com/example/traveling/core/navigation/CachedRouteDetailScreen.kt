package com.example.traveling.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traveling.features.travelpath.RouteDetailScreen
import com.example.traveling.features.travelpath.TravelViewModel
import com.example.traveling.ui.theme.*

/**
 * Wrapper screen that loads a cached route from local cache or Firestore
 * into TravelViewModel, then displays the standard RouteDetailScreen.
 */
@Composable
fun CachedRouteDetailScreen(
    routeId: String,
    onBack: () -> Unit,
    onOpenPhotoDetail: (String) -> Unit = {},
    travelViewModel: TravelViewModel = viewModel()
) {
    val context = LocalContext.current
    var loaded by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(routeId) {
        travelViewModel.initLocalStorage(context)
        // Try async version which checks local cache then Firestore
        val success = travelViewModel.loadCachedRouteAsync(routeId)
        if (success) {
            loaded = true
        } else {
            loadFailed = true
        }
    }

    when {
        loadFailed -> {
            Box(
                modifier = Modifier.fillMaxSize().background(PageBg),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Itinéraire introuvable", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = StoneText)
                    Spacer(Modifier.height(4.dp))
                    Text("Les données de cette route n'ont pas été trouvées.", fontSize = 13.sp, color = StoneMuted)
                }
            }
        }
        loaded -> {
            RouteDetailScreen(
                routeId = routeId,
                onBack = onBack,
                onOpenPhotoDetail = onOpenPhotoDetail,
                travelViewModel = travelViewModel
            )
        }
        else -> {
            Box(
                modifier = Modifier.fillMaxSize().background(PageBg),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RedPrimary)
            }
        }
    }
}
