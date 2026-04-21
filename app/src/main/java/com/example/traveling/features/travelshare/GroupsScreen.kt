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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
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
                    Text("Mes Groupes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Stone800)
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // Section: Mes Groupes
            Column {
                Text("Mes groupes (${MOCK_GROUPS.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Stone400, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MOCK_GROUPS.forEach { group ->
                        MyGroupCard(group)
                    }
                }
            }

            // Section: Découvrir
            Column {
                Text("Découvrir des groupes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Stone400, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DISCOVER_GROUPS.forEach { group ->
                        DiscoverGroupCard(group)
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
}

// Sous-composant : Carte de mes groupes
@Composable
private fun MyGroupCard(group: GroupItem) {
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
}

// Sous-composant : Carte découverte
@Composable
private fun DiscoverGroupCard(group: GroupItem) {
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
            modifier = Modifier.background(Color(0xFFFEF2F2), RoundedCornerShape(6.dp)).border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(6.dp)).clickable { }.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Rejoindre", color = RedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Sous-composant : Contenu de la Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupContent(onClose: () -> Unit) {
    var groupName by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }

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