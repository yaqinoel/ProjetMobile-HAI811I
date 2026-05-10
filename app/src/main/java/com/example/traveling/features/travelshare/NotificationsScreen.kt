package com.example.traveling.features.travelshare.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traveling.data.model.NotificationDocument
import com.example.traveling.data.model.NotificationSettingsDocument
import com.example.traveling.ui.theme.BgColor
import com.example.traveling.ui.theme.CardBg
import com.example.traveling.ui.theme.HeaderBg
import com.example.traveling.ui.theme.RedPrimary
import com.example.traveling.ui.theme.Stone100
import com.example.traveling.ui.theme.Stone400
import com.example.traveling.ui.theme.Stone500
import com.example.traveling.ui.theme.Stone600
import com.example.traveling.ui.theme.Stone800
import com.example.traveling.ui.theme.StoneBorder
import java.util.concurrent.TimeUnit

data class FilterTab(val key: String, val label: String)

private val FILTER_TABS = listOf(
    FilterTab("all", "Toutes"),
    FilterTab("unread", "Non lues"),
    FilterTab("users", "Utilisateurs"),
    FilterTab("groups", "Groupes"),
    FilterTab("places", "Lieux"),
    FilterTab("tags", "Tags")
)

@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenPhotoDetail: (String) -> Unit = {},
    onOpenGroupDetail: (String) -> Unit = {}
) {
    val viewModel: NotificationsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentFilter by remember { mutableStateOf("all") }
    var showActionsMenu by remember { mutableStateOf(false) }
    var showSettingsPanel by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.observeNotifications()
    }

    Scaffold(
        containerColor = BgColor,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when (val state = uiState) {
            NotificationsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RedPrimary)
                }
            }

            is NotificationsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Stone500)
                }
            }

            is NotificationsUiState.Success -> {
                val notifications = state.notifications
                val settings = state.settings
                val unreadCount = notifications.count { !it.isRead }
                val filteredList = notifications.filter { n ->
                    when (currentFilter) {
                        "unread" -> !n.isRead
                        "users" -> n.type == "user_publish" || n.type == "like" || n.type == "comment"
                        "groups" -> n.type == "group_publish"
                        "places" -> n.type == "place_type_match"
                        "tags" -> n.type == "tag_match"
                        else -> true
                    }
                }

                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    Surface(
                        color = HeaderBg,
                        border = BorderStroke(1.dp, Color(0x1478350F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                                            .clickable { onBack() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Stone600, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))

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

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(if (showSettingsPanel) RedPrimary else Stone100, RoundedCornerShape(8.dp))
                                            .clickable {
                                                showSettingsPanel = !showSettingsPanel
                                                onOpenSettings()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Settings, "Paramètres", tint = if (showSettingsPanel) Color.White else Stone500, modifier = Modifier.size(16.dp))
                                    }

                                    Box {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Stone100, RoundedCornerShape(8.dp))
                                                .clickable { showActionsMenu = !showActionsMenu },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.DoneAll, "Actions", tint = Stone500, modifier = Modifier.size(16.dp))
                                        }

                                        DropdownMenu(
                                            expanded = showActionsMenu,
                                            onDismissRequest = { showActionsMenu = false },
                                            modifier = Modifier.background(Color(0xFFFFFBF5)),
                                            offset = DpOffset(x = (-10).dp, y = 8.dp)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Tout marquer comme lu", fontSize = 14.sp, color = Stone800) },
                                                leadingIcon = { Icon(Icons.Default.Check, null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp)) },
                                                onClick = {
                                                    viewModel.markAllAsRead()
                                                    showActionsMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

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
                                                    modifier = Modifier.background(if (isActive) Color(0x33FFFFFF) else Color(0xFFFEE2E2), CircleShape)
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(count.toString(), fontSize = 9.sp, color = if (isActive) Color.White else RedPrimary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(visible = showSettingsPanel, enter = fadeIn(), exit = fadeOut()) {
                                NotificationSettingsPanel(settings = settings, onUpdate = { viewModel.updateSettings(it) })
                            }
                        }
                    }

                    if (filteredList.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(top = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.size(64.dp).background(Stone100, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Notifications, null, tint = Color(0xFFD6D3D1), modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Text("Aucune notification", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone400)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredList, key = { it.notificationId }) { notif ->
                                NotificationCard(
                                    notif = notif,
                                    onClick = {
                                        viewModel.markAsRead(notif.notificationId)
                                        when {
                                            !notif.relatedPostId.isNullOrBlank() -> notif.relatedPostId?.let(onOpenPhotoDetail)
                                            !notif.relatedGroupId.isNullOrBlank() -> notif.relatedGroupId?.let(onOpenGroupDetail)
                                        }
                                    },
                                    onDelete = { viewModel.deleteNotification(notif.notificationId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSettingsPanel(
    settings: NotificationSettingsDocument,
    onUpdate: (NotificationSettingsDocument) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Alertes suivies", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Stone800)

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            settings.followedTags.take(3).forEach { tag -> FollowChip(Icons.Default.Tag, "#$tag") }
            settings.followedPlaceTypes.take(3).forEach { place -> FollowChip(Icons.Default.LocationOn, place) }
            settings.followedGroupIds.take(2).forEach { group -> FollowChip(Icons.Default.Group, group) }
            settings.followedUserIds.take(2).forEach { user -> FollowChip(Icons.Default.Person, user) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NotificationToggleCard(
                "Groupes",
                "Publications de groupes",
                Icons.Default.Group,
                settings.notifyFromGroups,
                Modifier.weight(1f),
                onCheckedChange = { onUpdate(settings.copy(notifyFromGroups = it)) }
            )
            NotificationToggleCard(
                "Lieux & tags",
                "Publications correspondantes",
                Icons.Default.NotificationsActive,
                settings.notifyByTags || settings.notifyByPlaces,
                Modifier.weight(1f),
                onCheckedChange = {
                    onUpdate(settings.copy(notifyByTags = it, notifyByPlaces = it))
                }
            )
        }
    }
}

@Composable
private fun FollowChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFEF2F2),
        border = BorderStroke(1.dp, Color(0xFFFEE2E2))
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = RedPrimary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = RedPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun NotificationToggleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CardBg,
        border = BorderStroke(1.dp, StoneBorder)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).background(Color(0xFFFEF2F2), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = RedPrimary, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Stone800)
                Text(subtitle, fontSize = 10.sp, color = Stone400)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun NotificationCard(
    notif: NotificationDocument,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val badgeColor = when (notif.type) {
        "like" -> Color(0xFFEF4444)
        "comment" -> Color(0xFF3B82F6)
        "group_publish" -> Color(0xFFF97316)
        "tag_match" -> Color(0xFF14B8A6)
        "place_type_match" -> Color(0xFFF59E0B)
        else -> Color(0xFFB91C1C)
    }

    val badgeIcon = when (notif.type) {
        "group_publish" -> Icons.Default.Group
        "tag_match" -> Icons.Default.Tag
        "place_type_match" -> Icons.Default.LocationOn
        "comment" -> Icons.Default.NotificationsActive
        "like" -> Icons.Default.Notifications
        else -> Icons.Default.Person
    }

    val timeText = notif.createdAt?.toDate()?.let {
        val diffMs = System.currentTimeMillis() - it.time
        val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        when {
            mins < 1 -> "À l'instant"
            mins < 60 -> "Il y a ${mins} min"
            mins < 1440 -> "Il y a ${TimeUnit.MINUTES.toHours(mins)} h"
            else -> "Il y a ${TimeUnit.MINUTES.toDays(mins)} j"
        }
    } ?: ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (notif.isRead) Color.Transparent else Color(0x99FEF2F2), RoundedCornerShape(12.dp))
            .border(1.dp, if (notif.isRead) Color.Transparent else Color(0x80FEE2E2), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            if (!notif.isRead) {
                Box(modifier = Modifier.padding(top = 4.dp, end = 6.dp).size(6.dp).background(Color.Red, CircleShape))
            } else {
                Spacer(Modifier.width(12.dp))
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFE7E5E4), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(notif.title.firstOrNull()?.uppercase() ?: "N", color = Stone800, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(20.dp)
                        .background(badgeColor, CircleShape)
                        .border(2.dp, BgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(badgeIcon, null, tint = Color.White, modifier = Modifier.size(11.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Stone800)) { append(notif.title) }
                        append(" ")
                        withStyle(style = SpanStyle(color = Stone600)) { append(notif.message) }
                    },
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Text(timeText, fontSize = 10.sp, color = Stone400, modifier = Modifier.padding(top = 2.dp))
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "Supprimer", tint = Stone400, modifier = Modifier.size(14.dp))
            }
        }
    }
}
