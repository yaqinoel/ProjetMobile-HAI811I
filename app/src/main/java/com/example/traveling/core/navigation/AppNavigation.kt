package com.example.traveling.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traveling.features.travelpath.TravelPathScreen

// ─── Colors ───
private val RedPrimary = Color(0xFFB91C1C)
private val RedDark = Color(0xFF991B1B)
private val PageBg = Color(0xFFFDF8F4)
private val CardBg = Color(0xFFFFFBF5)
private val StoneText = Color(0xFF44403C)
private val StoneMuted = Color(0xFF78716C)
private val StoneLighter = Color(0xFFA8A29E)

// ─── Tab definition ───
enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    PHOTOS("Galerie", Icons.Filled.CameraAlt, Icons.Outlined.CameraAlt),
    PARCOURS("Parcours", Icons.Filled.Route, Icons.Outlined.Route),
    EXPLORER("Explorer", Icons.Filled.Search, Icons.Outlined.Search),
    PROFIL("Profil", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun MainScreen(
    isAnonymous: Boolean = false,
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(MainTab.PHOTOS) }

    Scaffold(
        containerColor = PageBg,
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        floatingActionButton = {
            if (selectedTab == MainTab.PHOTOS && !isAnonymous) {
                FloatingActionButton(
                    onClick = { /* TODO: Upload photo */ },
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(RedPrimary, RedDark)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Ajouter une photo",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                MainTab.PHOTOS -> PlaceholderScreen(
                    title = "Galerie de Voyages",
                    subtitle = "Partagez vos plus belles découvertes",
                    icon = Icons.Default.CameraAlt
                )
                MainTab.PARCOURS -> TravelPathScreen(isAnonymous = isAnonymous)
                MainTab.EXPLORER -> PlaceholderScreen(
                    title = "Explorer",
                    subtitle = "Découvrez de nouvelles destinations",
                    icon = Icons.Default.Search
                )
                MainTab.PROFIL -> PlaceholderScreen(
                    title = "Mon Profil",
                    subtitle = if (isAnonymous) "Connectez-vous pour accéder à votre profil" else "Gérez vos informations",
                    icon = Icons.Default.Person
                )
            }
        }
    }
}

// ═════════════════════════════════════════════
//  BOTTOM NAV BAR — Chinese styled
// ═════════════════════════════════════════════
@Composable
private fun BottomNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBg.copy(alpha = 0.95f),
        shadowElevation = 12.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                Surface(
                    onClick = { onTabSelected(tab) },
                    color = Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        // Active indicator dot
                        Box(
                            modifier = Modifier
                                .width(if (selected) 24.dp else 0.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                                .background(if (selected) RedPrimary else Color.Transparent)
                        )
                        Spacer(Modifier.height(4.dp))
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                            tint = if (selected) RedPrimary else StoneLighter,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) RedPrimary else StoneLighter
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════
//  PLACEHOLDER for unimplemented tabs
// ═════════════════════════════════════════════
@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFEF2F2)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = RedPrimary, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            fontSize = 20.sp, fontWeight = FontWeight.Bold,
            color = StoneText, letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            fontSize = 13.sp, color = StoneLighter
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFEF2F2)
        ) {
            Text(
                "🚧 Bientôt disponible",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 12.sp, fontWeight = FontWeight.Medium,
                color = Color(0xFFB91C1C)
            )
        }
    }
}
