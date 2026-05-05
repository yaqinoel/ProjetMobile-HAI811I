package com.example.traveling.features.travelshare

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.traveling.ui.theme.*

// --- Modèles de données ---
data class GroupItem(
    val id: String, val name: String, val members: Int, val photos: Int,
    val color: Color, val isPrivate: Boolean, val lastActivity: String
)

// --- Constantes & Données Simulées ---
private val MOCK_GROUPS = listOf(
    GroupItem("g1", "Voyageurs de Chine", 24, 156, Color(0xFFB91C1C), false, "Il y a 2h"),
    GroupItem("g2", "Route de la Soie 2026", 6, 89, Color(0xFF7C3AED), true, "Il y a 5h"),
    GroupItem("g3", "Photo Paysages", 42, 312, Color(0xFF10B981), false, "Il y a 1 jour")
)

private val DISCOVER_GROUPS = listOf(
    GroupItem("d1", "Backpackers Asie", 128, 890, Color(0xFFD97706), false, ""),
    GroupItem("d2", "Amateurs d'Architecture", 67, 445, Color(0xFFEC4899), false, ""),
    GroupItem("d3", "Voyage en Famille", 35, 210, Color(0xFF06B6D4), false, "")
)



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    onBack: () -> Unit = {}
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var groupFilter by remember { mutableStateOf("all") }
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
    Column(modifier = Modifier.fillMaxSize()) {
        // HEADER
        Surface(
            color = HeaderBg,
            border = BorderStroke(1.dp, Color(0x1478350F)), // amber-900/8
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().systemBarsPadding().padding(horizontal = 16.dp),
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Stone500, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Groupes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Stone800)
                        Text("Partager des photos avec une communauté", fontSize = 10.sp, color = Stone400)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(RedPrimary, RoundedCornerShape(8.dp))
                        .clickable { showCreateSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // CONTENU PRINCIPAL
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                GroupFilterChip("Tous", "all", groupFilter, { groupFilter = it }, Modifier.weight(1f))
                GroupFilterChip("Mes groupes", "mine", groupFilter, { groupFilter = it }, Modifier.weight(1f))
                GroupFilterChip("Découvrir", "discover", groupFilter, { groupFilter = it }, Modifier.weight(1f))
            }

            // Section: Mes Groupes
            if (groupFilter == "all" || groupFilter == "mine") Column {
                Text("Mes groupes (${MOCK_GROUPS.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Stone400, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MOCK_GROUPS.forEach { group ->
                        MyGroupCard(
                            group = group,
                            onPublishHere = { coroutineScope.launch { snackbarHostState.showSnackbar("Publication vers ${group.name} sélectionnée.") } },
                            onFollow = { followed ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(if (followed) "Notifications activées pour ${group.name}." else "Notifications désactivées pour ${group.name}.")
                                }
                            }
                        )
                    }
                }
            }

            // Section: Découvrir
            if (groupFilter == "all" || groupFilter == "discover") Column {
                Text("Découvrir des groupes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Stone400, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DISCOVER_GROUPS.forEach { group ->
                        DiscoverGroupCard(
                            group = group,
                            onJoin = { joined ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(if (joined) "Vous avez rejoint ${group.name}." else "Vous avez quitté ${group.name}.")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // BOTTOM SHEET : Créer un groupe
    if (showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = sheetState,
            containerColor = CardBg,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Stone300) }
        ) {
            CreateGroupContent(
                onClose = {
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) showCreateSheet = false
                    }
                }
            )
        }
    }
    SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun GroupFilterChip(
    label: String,
    value: String,
    selectedValue: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = selectedValue == value
    Surface(
        onClick = { onSelected(value) },
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) RedPrimary else Stone100
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Color.White else Stone500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// Sous-composant : Carte de mes groupes
@Composable
private fun MyGroupCard(
    group: GroupItem,
    onPublishHere: () -> Unit,
    onFollow: (Boolean) -> Unit
) {
    var followed by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x0D78350F), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(group.color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Group, null, tint = group.color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(group.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone800, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    Icon(if (group.isPrivate) Icons.Default.Lock else Icons.Default.Public, null, tint = Stone400, modifier = Modifier.size(12.dp))
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${group.members} membres", fontSize = 11.sp, color = Stone400)
                    Text(" · ", fontSize = 11.sp, color = Stone300)
                    Text("${group.photos} photos", fontSize = 11.sp, color = Stone400)
                    Text(" · ", fontSize = 11.sp, color = Stone300)
                    Text(group.lastActivity, fontSize = 11.sp, color = Stone400)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = Stone300, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.weight(1f),
                onClick = onPublishHere,
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFFEE2E2))
            ) {
                Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Publier ici", color = RedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                onClick = {
                    followed = !followed
                    onFollow(followed)
                },
                shape = RoundedCornerShape(8.dp),
                color = if (followed) Color(0xFFFEF2F2) else Stone100
            ) {
                Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = if (followed) RedPrimary else Stone500, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (followed) "Suivi" else "Suivre", color = if (followed) RedPrimary else Stone500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Sous-composant : Carte découverte
@Composable
private fun DiscoverGroupCard(group: GroupItem, onJoin: (Boolean) -> Unit) {
    var joined by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x0D78350F), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp).background(group.color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Group, null, tint = group.color, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(group.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone800, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("${group.members} membres · ${group.photos} photos", fontSize = 11.sp, color = Stone400)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .background(if (joined) RedPrimary else Color(0xFFFEF2F2), RoundedCornerShape(6.dp))
                .border(1.dp, if (joined) RedPrimary else Color(0xFFFEE2E2), RoundedCornerShape(6.dp))
                .clickable {
                    joined = !joined
                    onJoin(joined)
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(if (joined) "Rejoint" else "Rejoindre", color = if (joined) Color.White else RedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Sous-composant : Contenu de la Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupContent(onClose: () -> Unit) {
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf("Voyage") }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Créer un groupe", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Stone800)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Stone400) }
        }

        Spacer(Modifier.height(16.dp))

        // Input Nom
        Text("Nom du groupe", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Stone500, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            placeholder = { Text("Ex : Amis voyageurs", color = Stone300) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF5F5F4),
                focusedContainerColor = Color(0xFFF5F5F4),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color(0xFFFECACA)
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text("Description", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Stone500, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Objectif du groupe, destination, type de photos...", color = Stone300) },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF5F5F4),
                focusedContainerColor = Color(0xFFF5F5F4),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color(0xFFFECACA)
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text("Thème principal", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Stone500, modifier = Modifier.padding(bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Voyage", "Nature", "Culture").forEach { theme ->
                AssistChip(
                    onClick = { selectedTheme = theme },
                    label = { Text(theme) },
                    leadingIcon = {
                        if (selectedTheme == theme) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Visibilité Toggle
        Text("Visibilité", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Stone500, modifier = Modifier.padding(bottom = 8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Public Button
            Button(
                onClick = { isPrivate = false },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (!isPrivate) Color(0xFFFEF2F2) else Color(0xFFF5F5F4)),
                border = if (!isPrivate) BorderStroke(2.dp, Color(0xFFF87171)) else null
            ) {
                Icon(Icons.Default.Public, null, tint = if (!isPrivate) RedPrimary else Stone500, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Public", color = if (!isPrivate) RedPrimary else Stone500, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            // Privé Button
            Button(
                onClick = { isPrivate = true },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isPrivate) Color(0xFFFEF2F2) else Color(0xFFF5F5F4)),
                border = if (isPrivate) BorderStroke(2.dp, Color(0xFFF87171)) else null
            ) {
                Icon(Icons.Default.Lock, null, tint = if (isPrivate) RedPrimary else Stone500, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Privé", color = if (isPrivate) RedPrimary else Stone500, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Bouton Créer
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            contentPadding = PaddingValues(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(RedPrimary, Color(0xFF991B1B))), RoundedCornerShape(8.dp)).border(1.dp, Color(0x4DDC2626), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Créer le groupe", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GroupsScreenPreview() {
    GroupsScreen()
}
