package com.example.traveling.features.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traveling.data.model.User
import com.example.traveling.ui.components.UserAvatar
import com.example.traveling.ui.theme.*

data class ProfileMenuItem(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val badge: Int? = null,
    val action: () -> Unit = {}
)

data class ProfileStat(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val action: () -> Unit = {}
)

@Composable
fun ProfileScreen(
    isAnonymous: Boolean = false,
    onNavigateLogin: () -> Unit = {},
    onNavigateRegister: () -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenGroups: () -> Unit = {},
    onOpenMyPhotos: () -> Unit = {},
    onOpenLikedPosts: () -> Unit = {},
    onOpenSavedPosts: () -> Unit = {},
    onOpenLikedRoutes: () -> Unit = {},
    onOpenSavedRoutes: () -> Unit = {},
    onOpenImageMigration: () -> Unit = {},
    onOpenFollowing: () -> Unit = {}
) {
    if (isAnonymous) {
        AnonymousProfileView(
            onLogin = onNavigateLogin,
            onRegister = onNavigateRegister,
            onOpenLikedPosts = onOpenLikedPosts,
            onOpenSavedPosts = onOpenSavedPosts
        )
    } else {
        val profileViewModel: ProfileViewModel = viewModel()
        val uiState by profileViewModel.uiState.collectAsState()

        LaunchedEffect(Unit) {
            profileViewModel.observeCurrentUser()
        }

        when (val state = uiState) {
            ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(ProfilePageBg),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RedPrimary)
                }
            }
            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(ProfilePageBg).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Erreur de profil", color = Stone800, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, color = StoneMuted, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onLogout,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Retour à l'accueil")
                        }
                    }
                }
            }
            is ProfileUiState.Success -> {
                AuthenticatedProfileView(
                    user = state.user,
                    followedUserCount = state.followedUserCount,
                    onLogout = onLogout,
                    onOpenNotifications = onOpenNotifications,
                    onOpenGroups = onOpenGroups,
                    onOpenMyPhotos = onOpenMyPhotos,
                    onOpenLikedPosts = onOpenLikedPosts,
                    onOpenSavedPosts = onOpenSavedPosts,
                    onOpenLikedRoutes = onOpenLikedRoutes,
                    onOpenSavedRoutes = onOpenSavedRoutes,
                    onOpenImageMigration = onOpenImageMigration,
                    onOpenFollowing = onOpenFollowing
                )
            }
        }
    }
}

@Composable
private fun AnonymousProfileView(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onOpenLikedPosts: () -> Unit,
    onOpenSavedPosts: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(ProfilePageBg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFFFEE2E2), CircleShape)
                .border(1.dp, Color(0xFFFECACA), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(40.dp))
        }

        Spacer(Modifier.height(20.dp))

        Text("Mode Navigation Anonyme", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StoneText)
        Spacer(Modifier.height(8.dp))
        Text(
            "Vous pouvez consulter vos photos aimées et enregistrées. Connectez-vous pour publier, commenter et rejoindre des groupes.",
            fontSize = 14.sp, color = StoneMuted, textAlign = TextAlign.Center, lineHeight = 22.sp
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnonymousProfileAction(
                label = "Posts aimés",
                subtitle = "Photos que vous avez aimées",
                icon = Icons.Default.Favorite,
                iconColor = Color(0xFFDC2626),
                onClick = onOpenLikedPosts
            )
            AnonymousProfileAction(
                label = "Posts enregistrés",
                subtitle = "Photos sauvegardées pour plus tard",
                icon = Icons.Default.Bookmark,
                iconColor = Color(0xFFD97706),
                onClick = onOpenSavedPosts
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(RedPrimary, RedDark)), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Connexion", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF5F5F4)),
            border = BorderStroke(0.dp, Color.Transparent),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Créer un compte", color = StoneText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AnonymousProfileAction(
    label: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ProfileCardBg)
            .border(1.dp, StoneBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = StoneText)
            Text(subtitle, fontSize = 11.sp, color = StoneMuted)
        }
        Icon(Icons.Default.ChevronRight, null, tint = StoneMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AuthenticatedProfileView(
    user: User,
    followedUserCount: Int,
    onLogout: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenMyPhotos: () -> Unit,
    onOpenLikedPosts: () -> Unit,
    onOpenSavedPosts: () -> Unit,
    onOpenLikedRoutes: () -> Unit,
    onOpenSavedRoutes: () -> Unit,
    onOpenImageMigration: () -> Unit,
    onOpenFollowing: () -> Unit
) {
    val scrollState = rememberScrollState()

    val menuItems = listOf(
        ProfileMenuItem("Mes groupes", "Créer, rejoindre et gérer vos groupes", Icons.Default.Group, Color(0xFFB91C1C), action = onOpenGroups),
        ProfileMenuItem("Notifications", "Préférences et alertes", Icons.Default.Notifications, Color(0xFF10B981), action = onOpenNotifications),
        ProfileMenuItem("Itinéraires aimés", "Routes que vous avez aimées", Icons.Default.FavoriteBorder, Color(0xFFE11D48), action = onOpenLikedRoutes),
        ProfileMenuItem("Itinéraires enregistrés", "Routes sauvegardées pour plus tard", Icons.Default.BookmarkBorder, Color(0xFFCA8A04), action = onOpenSavedRoutes),
        ProfileMenuItem("Migration images", "Remplacer les anciennes URLs par Firebase Storage", Icons.Default.CloudUpload, Color(0xFF2563EB), action = onOpenImageMigration)
    )

    val stats = listOf(
        ProfileStat("Photos", user.postCount.toString(), Icons.Default.PhotoCamera, action = onOpenMyPhotos),
        ProfileStat("Favoris", user.likedCount.toString(), Icons.Default.Favorite, action = onOpenLikedPosts),
        ProfileStat("Enregistrés", user.savedCount.toString(), Icons.Default.Bookmark, action = onOpenSavedPosts),
        ProfileStat("Suivis", followedUserCount.toString(), Icons.Default.Person, action = onOpenFollowing)
    )

    Column(modifier = Modifier.fillMaxSize().background(ProfilePageBg).verticalScroll(scrollState)) {

        Box {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF991B1B), Color(0xFF7F1D1D))))
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 64.dp)
            ) {

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Profil", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onLogout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Déconnexion", color = Color.White.copy(0.6f), fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {

                    UserAvatar(
                        avatarUrl = user.avatarUrl,
                        fallbackText = user.displayName.firstOrNull()?.uppercase() ?: "V",
                        backgroundColor = Color(0xFFD97706),
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(8.dp, CircleShape)
                            .border(3.dp, Color(0x80FCD34D), CircleShape),
                        textSize = 32.sp
                    )
                    Spacer(Modifier.width(16.dp))

                    Column {
                        Text(user.displayName.ifBlank { "Voyageur" }, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        val subtitle = listOfNotNull(
                            user.bio?.takeIf { it.isNotBlank() },
                            user.homeCity?.takeIf { it.isNotBlank() }
                        ).joinToString(" · ").ifBlank { "Voyageur" }
                        Text(subtitle, color = Color.White.copy(0.6f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = ProfileCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 40.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    stats.forEach { stat ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).clickable { stat.action() }
                        ) {
                            Icon(stat.icon, null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp).padding(bottom = 4.dp))
                            Text(stat.value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StoneText)
                            Text(stat.label, fontSize = 10.sp, color = StoneMuted)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(56.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            menuItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { item.action() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(modifier = Modifier.size(40.dp).background(item.iconColor.copy(0.12f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Icon(item.icon, null, tint = item.iconColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = StoneText)
                        Text(item.subtitle, fontSize = 11.sp, color = StoneMuted)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.badge != null) {
                            Box(modifier = Modifier.size(20.dp).background(Color(0xFFEF4444), CircleShape), contentAlignment = Alignment.Center) {
                                Text(item.badge.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFD6D3D1), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Preview(showBackground = true, name = "Connecté")
@Composable
fun ProfileScreenPreview() {
    AuthenticatedProfileView(
        user = User(
            userId = "preview",
            displayName = "Voyageur Preview",
            email = "preview@example.com",
            bio = "Travel photographer",
            homeCity = "Montpellier",
            postCount = 12,
            likedCount = 42,
            savedCount = 18,
            groupCount = 3
        ),
        followedUserCount = 5,
        onLogout = {},
        onOpenNotifications = {},
        onOpenGroups = {},
        onOpenMyPhotos = {},
        onOpenLikedPosts = {},
        onOpenSavedPosts = {},
        onOpenLikedRoutes = {},
        onOpenSavedRoutes = {},
        onOpenImageMigration = {},
        onOpenFollowing = {}
    )
}

@Preview(showBackground = true, name = "Anonyme")
@Composable
fun ProfileAnonymousPreview() {
    ProfileScreen(isAnonymous = true)
}
