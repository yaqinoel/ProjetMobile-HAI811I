package com.example.traveling.features.travelshare.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// --- Modèles de données ---
data class NotificationItem(
    val id: String,
    val type: String, // "user_publish", "group_publish", "location", "tag", "like", "comment", "follow"
    val title: String,
    val message: String,
    val time: String,
    val read: Boolean,
    val avatar: String,
    val avatarColor: Color,
    val imageUrl: String? = null
)

data class FilterTab(val key: String, val label: String)

// --- Constantes & Données Simulées ---
private val MOCK_NOTIFICATIONS = listOf(
    NotificationItem("n1", "user_publish", "Li Xiaofang", "a publié une nouvelle photo à la Grande Muraille", "Il y a 5 min", false, "L", Color(0xFFB91C1C), "https://images.unsplash.com/photo-1558507564-c573429b9ceb?w=100&fit=crop"),
    NotificationItem("n2", "group_publish", "Groupe : Voyageurs de Pékin", "Wang Wanqing a partagé 3 photos dans le groupe", "Il y a 20 min", false, "V", Color(0xFFD97706), "https://images.unsplash.com/photo-1603120527222-33f28c2ce89e?w=100&fit=crop"),
    NotificationItem("n3", "location", "Lieu suivi : Guilin", "2 nouvelles photos publiées à Paysages de Guilin", "Il y a 1 heure", false, "G", Color(0xFF7C3AED), "https://images.unsplash.com/photo-1773318901379-aac92fdf5611?w=100&fit=crop"),
    NotificationItem("n4", "tag", "Tag suivi : #Lever du soleil", "5 nouvelles photos correspondent à votre tag suivi", "Il y a 2 heures", false, "#", Color(0xFF0D9488)),
    NotificationItem("n5", "like", "Zhang Zhiyuan", "a aimé votre photo « Cité Interdite au crépuscule »", "Il y a 3 heures", true, "Z", Color(0xFF7C3AED)),
    NotificationItem("n6", "comment", "Chen Minghui", "a commenté : « Magnifique composition ! Quel objectif ? »", "Il y a 4 heures", true, "C", Color(0xFF0D9488)),
    NotificationItem("n7", "follow", "Lin Yuxuan", "a commencé à vous suivre", "Il y a 5 heures", true, "L", Color(0xFF2563EB)),
    NotificationItem("n8", "location", "Lieu suivi : Shanghai", "Zhao Zixuan a publié une photo au Bund de Shanghai", "Il y a 6 heures", true, "S", Color(0xFFDC2626), "https://images.unsplash.com/photo-1647067151201-0b37c7555870?w=100&fit=crop"),
    NotificationItem("n9", "group_publish", "Groupe : Amateurs de paysages", "3 nouvelles publications cette semaine", "Il y a 1 jour", true, "A", Color(0xFF16A34A)),
    NotificationItem("n10", "tag", "Tag suivi : #Architecture", "12 nouvelles photos correspondent à votre tag", "Il y a 1 jour", true, "#", Color(0xFFEA580C)),
    NotificationItem("n11", "user_publish", "Zhao Zixuan", "a publié une nouvelle collection « Nuits de Shanghai »", "Il y a 2 jours", true, "Z", Color(0xFFDC2626))
)

private val FILTER_TABS = listOf(
    FilterTab("all", "Toutes"),
    FilterTab("unread", "Non lues"),
    FilterTab("users", "Utilisateurs"),
    FilterTab("groups", "Groupes"),
    FilterTab("places", "Lieux"),
    FilterTab("tags", "Tags")
)

// --- Couleurs thématiques ---
private val BgColor = Color(0xFFFDF8F4)
private val HeaderBg = Color(0xF2FFFBF5) // Ajout de transparence pour l'effet blur
private val RedPrimary = Color(0xFFB91C1C)
private val Stone800 = Color(0xFF292524)
private val Stone600 = Color(0xFF57534E)
private val Stone500 = Color(0xFF78716C)
private val Stone400 = Color(0xFFA8A29E)
private val Stone100 = Color(0xFFF5F5F4)

@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    // États
    var notifications by remember { mutableStateOf(MOCK_NOTIFICATIONS) }
    var currentFilter by remember { mutableStateOf("all") }
    var showActionsMenu by remember { mutableStateOf(false) }

    // Dérivés
    val unreadCount = notifications.count { !it.read }
    val filteredList = notifications.filter { n ->
        when (currentFilter) {
            "unread" -> !n.read
            "users" -> n.type == "user_publish" || n.type == "follow"
            "groups" -> n.type == "group_publish"
            "places" -> n.type == "location"
            "tags" -> n.type == "tag"
            else -> true
        }
    }

    // Actions
    val markAllRead = {
        notifications = notifications.map { it.copy(read = true) }
        showActionsMenu = false
    }
    val clearAll = {
        notifications = emptyList()
        showActionsMenu = false
    }
    val markRead = { id: String ->
        notifications = notifications.map { if (it.id == id) it.copy(read = true) else it }
    }
    val deleteNotif = { id: String ->
        notifications = notifications.filterNot { it.id == id }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        // --- 1. HEADER (Sticky) ---
        Surface(
            color = HeaderBg,
            border = BorderStroke(1.dp, Color(0x1478350F)), // amber-900/8
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Barre supérieure
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Bouton retour
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp)) // red-50
                                .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(8.dp)) // red-100
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Stone600, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))

                        // Titre et sous-titre
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Notifications", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Stone800)
                                if (unreadCount > 0) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier.background(Color(0xFFDC2626), CircleShape).padding(horizontal = 6.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(unreadCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text("Restez informé de l'activité", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Stone400)
                        }
                    }

                    // Boutons actions droite
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier.size(36.dp).background(Stone100, RoundedCornerShape(8.dp)).clickable { onOpenSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Settings, "Paramètres", tint = Stone500, modifier = Modifier.size(16.dp))
                        }

                        Box {
                            Box(
                                modifier = Modifier.size(36.dp).background(Stone100, RoundedCornerShape(8.dp)).clickable { showActionsMenu = !showActionsMenu },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DoneAll, "Actions", tint = Stone500, modifier = Modifier.size(16.dp))
                            }

                            // Menu Dropdown
                            DropdownMenu(
                                expanded = showActionsMenu,
                                onDismissRequest = { showActionsMenu = false },
                                modifier = Modifier.background(Color(0xFFFFFBF5)),
                                offset = DpOffset(x = (-10).dp, y = 8.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tout marquer comme lu", fontSize = 14.sp, color = Stone800) },
                                    leadingIcon = { Icon(Icons.Default.Check, null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp)) },
                                    onClick = markAllRead
                                )
                                DropdownMenuItem(
                                    text = { Text("Supprimer toutes", fontSize = 14.sp, color = Color(0xFFDC2626)) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp)) },
                                    onClick = clearAll
                                )
                            }
                        }
                    }
                }

                // --- Onglets de filtrage ---
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FILTER_TABS.forEach { tab ->
                        val isActive = currentFilter == tab.key
                        val count = if (tab.key == "unread") unreadCount else 0

                        Box(
                            modifier = Modifier
                                .background(if (isActive) RedPrimary else Stone100, CircleShape)
                                .clickable { currentFilter = tab.key }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isActive) Color.White else Stone500
                                )
                                if (count > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier.background(if (isActive) Color(0x33FFFFFF) else Color(0xFFFEE2E2), CircleShape).padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(count.toString(), fontSize = 9.sp, color = if (isActive) Color.White else RedPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. LISTE DES NOTIFICATIONS ---
        if (filteredList.isEmpty()) {
            // État vide
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(64.dp).background(Stone100, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Notifications, null, tint = Color(0xFFD6D3D1), modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Aucune notification", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone400)
                Text("Vous êtes à jour !", fontSize = 12.sp, color = Color(0xFFD6D3D1), modifier = Modifier.padding(top = 4.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredList, key = { it.id }) { notif ->
                    NotificationCard(
                        notif = notif,
                        onClick = { markRead(notif.id) },
                        onDelete = { deleteNotif(notif.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notif: NotificationItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    // Mapping des icônes et couleurs de badge
    val badgeIcon: ImageVector
    val badgeColor: Color

    when (notif.type) {
        "like" -> { badgeIcon = Icons.Default.Favorite; badgeColor = Color(0xFFEF4444) } // red-500
        "comment" -> { badgeIcon = Icons.Default.ChatBubble; badgeColor = Color(0xFF3B82F6) } // blue-500
        "follow" -> { badgeIcon = Icons.Default.PersonAdd; badgeColor = Color(0xFFA855F7) } // purple-500
        "location" -> { badgeIcon = Icons.Default.LocationOn; badgeColor = Color(0xFFF59E0B) } // amber-500
        "tag" -> { badgeIcon = Icons.Default.Tag; badgeColor = Color(0xFF14B8A6) } // teal-500
        "group_publish" -> { badgeIcon = Icons.Default.Group; badgeColor = Color(0xFFF97316) } // orange-500
        else -> { badgeIcon = Icons.Default.PhotoCamera; badgeColor = Color(0xFFB91C1C) } // user_publish (red-700)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (notif.read) Color.Transparent else Color(0x99FEF2F2)) // bg-red-50/60
            .border(1.dp, if (notif.read) Color.Transparent else Color(0x80FEE2E2), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {

            // Point non-lu
            if (!notif.read) {
                Box(modifier = Modifier.padding(top = 4.dp, end = 6.dp).size(6.dp).background(Color.Red, CircleShape))
            } else {
                Spacer(Modifier.width(12.dp))
            }

            // Avatar & Badge
            Box(modifier = Modifier.size(44.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).background(notif.avatarColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(notif.avatar, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                // Petit badge en bas à droite
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(20.dp)
                        .background(badgeColor, CircleShape)
                        .border(2.dp, BgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(badgeIcon, null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            // Contenu Texte
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Stone800)) {
                            append(notif.title)
                        }
                        append(" ")
                        withStyle(style = SpanStyle(color = Stone600)) {
                            append(notif.message)
                        }
                    },
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                Text(notif.time, fontSize = 10.sp, color = Stone400, modifier = Modifier.padding(top = 2.dp))

                // Bouton d'action "Suivre en retour"
                if (notif.type == "follow" && !notif.read) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(RedPrimary, RoundedCornerShape(8.dp))
                            .clickable { /* Logique d'abonnement */ }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Suivre en retour", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Image de prévisualisation (le cas échéant)
            if (notif.imageUrl != null) {
                Spacer(Modifier.width(12.dp))
                AsyncImage(
                    model = notif.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x0D78350F), RoundedCornerShape(8.dp))
                )
            }

            // Bouton supprimer (Croix)
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp).padding(start = 4.dp).align(Alignment.Top)
            ) {
                Icon(Icons.Default.Close, "Supprimer", tint = Stone400, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationsScreenPreview() {
    NotificationsScreen()
}