package com.example.traveling.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.traveling.data.repository.LocalStorageRepository
import com.example.traveling.data.repository.SavedRoutesRepository
import com.example.traveling.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LikedRoutesScreen(
    onBack: () -> Unit = {},
    onOpenRoute: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val localStorage = remember { LocalStorageRepository(context.applicationContext) }
    val savedRoutesRepo = remember { SavedRoutesRepository() }
    val scope = rememberCoroutineScope()
    var routes by remember { mutableStateOf(localStorage.getLikedRoutes()) }

    Scaffold(
        containerColor = ProfilePageBg,
        topBar = {
            Surface(color = ProfileCardBg) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Stone800)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Itinéraires aimés", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StoneText)
                        Text("${routes.size} itinéraires", fontSize = 12.sp, color = StoneMuted)
                    }
                    Icon(Icons.Default.Favorite, null, tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp))
                }
            }
        }
    ) { innerPadding ->
        if (routes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FavoriteBorder, null, tint = Color(0xFFDC2626).copy(0.3f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Aucun itinéraire aimé", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = StoneText)
                    Spacer(Modifier.height(4.dp))
                    Text("Appuyez sur ♥ dans une route pour l'ajouter ici", fontSize = 13.sp, color = StoneMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
            ) {
                items(routes, key = { it.id }) { route ->
                    RouteListCard(
                        route = route,
                        accentColor = Color(0xFFDC2626),
                        onOpen = { onOpenRoute(route.id) },
                        onRemove = {
                            localStorage.toggleRouteLike(route.id)
                            routes = localStorage.getLikedRoutes()
                            // Sync delete to Firestore
                            scope.launch {
                                try { savedRoutesRepo.removeRoute(route.id, "liked") }
                                catch (_: Exception) {}
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedRoutesScreen(
    onBack: () -> Unit = {},
    onOpenRoute: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val localStorage = remember { LocalStorageRepository(context.applicationContext) }
    val savedRoutesRepo = remember { SavedRoutesRepository() }
    val scope = rememberCoroutineScope()
    var routes by remember { mutableStateOf(localStorage.getSavedRoutes()) }

    Scaffold(
        containerColor = ProfilePageBg,
        topBar = {
            Surface(color = ProfileCardBg) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Stone800)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Itinéraires enregistrés", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StoneText)
                        Text("${routes.size} itinéraires", fontSize = 12.sp, color = StoneMuted)
                    }
                    Icon(Icons.Default.Bookmark, null, tint = Color(0xFFD97706), modifier = Modifier.size(22.dp))
                }
            }
        }
    ) { innerPadding ->
        if (routes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BookmarkBorder, null, tint = Color(0xFFD97706).copy(0.3f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Aucun itinéraire enregistré", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = StoneText)
                    Spacer(Modifier.height(4.dp))
                    Text("Appuyez sur Enregistrer dans une route pour l'ajouter ici", fontSize = 13.sp, color = StoneMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
            ) {
                items(routes, key = { it.id }) { route ->
                    RouteListCard(
                        route = route,
                        accentColor = Color(0xFFD97706),
                        onOpen = { onOpenRoute(route.id) },
                        onRemove = {
                            localStorage.toggleRouteSave(route.id)
                            routes = localStorage.getSavedRoutes()
                            // Sync delete to Firestore
                            scope.launch {
                                try { savedRoutesRepo.removeRoute(route.id, "saved") }
                                catch (_: Exception) {}
                            }
                        }
                    )
                }
            }
        }
    }
}

// Shared route card component
@Composable
private fun RouteListCard(
    route: LocalStorageRepository.RouteInfoSummary,
    accentColor: Color,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(1.dp, StoneBorder, RoundedCornerShape(12.dp))
            .clickable { onOpen() }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Route image
        AsyncImage(
            model = route.imageUrl,
            contentDescription = route.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        // Route name + destination
        Text(route.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Stone800)
        Text(route.destName, fontSize = 13.sp, color = accentColor, fontWeight = FontWeight.Medium)

        // Subtitle
        if (route.subtitle.isNotBlank()) {
            Text(route.subtitle, maxLines = 2, overflow = TextOverflow.Ellipsis, color = StoneMuted, fontSize = 12.sp)
        }

        // Stats row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.AttachMoney, null, tint = StoneMuted, modifier = Modifier.size(14.dp))
                Text("${route.budget} €", fontSize = 12.sp, color = StoneMuted)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Schedule, null, tint = StoneMuted, modifier = Modifier.size(14.dp))
                Text(route.duration, fontSize = 12.sp, color = StoneMuted)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Place, null, tint = StoneMuted, modifier = Modifier.size(14.dp))
                Text("${route.stops} arrêts", fontSize = 12.sp, color = StoneMuted)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                Text("${route.rating}", fontSize = 12.sp, color = StoneMuted)
            }
        }

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Open detail button
            Button(
                onClick = onOpen,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Voir détail", fontSize = 12.sp)
            }
            // Remove button
            OutlinedButton(
                onClick = onRemove,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 1.dp)
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Retirer", fontSize = 12.sp)
            }
        }
    }
}
