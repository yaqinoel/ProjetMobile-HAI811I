package com.example.traveling.features.travelshare

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traveling.data.model.GroupDocument
import com.example.traveling.ui.theme.BgColor
import com.example.traveling.ui.theme.CardBg
import com.example.traveling.ui.theme.RedPrimary
import com.example.traveling.ui.theme.Stone300
import com.example.traveling.ui.theme.Stone400
import com.example.traveling.ui.theme.Stone500
import com.example.traveling.ui.theme.Stone800
import com.example.traveling.ui.theme.StoneBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    onBack: () -> Unit = {},
    onOpenGroupDetail: (String) -> Unit = {}
) {
    val viewModel: GroupsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateSheet by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("all") }

    // les groupes rejoints et à découvrir sont observés ensemble
    LaunchedEffect(Unit) {
        viewModel.observeGroups()
    }

    // les actions groupe renvoient des messages courts dans le snackbar
    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = BgColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = CardBg) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .systemBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Stone800)
                        }
                        Column {
                            Text("Groupes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Stone800)
                            Text("Partagez avec vos communautés", fontSize = 11.sp, color = Stone400)
                        }
                    }

                    IconButton(onClick = { showCreateSheet = true }) {
                        Icon(Icons.Default.Add, null, tint = RedPrimary)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // filtre local pour ne pas multiplier les écrans de groupes
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text("Tous") })
                FilterChip(selected = filter == "mine", onClick = { filter = "mine" }, label = { Text("Mes groupes") })
                FilterChip(selected = filter == "discover", onClick = { filter = "discover" }, label = { Text("Découvrir") })
            }

            if (uiState.errorMessage != null) {
                Text(uiState.errorMessage ?: "", color = Stone500, fontSize = 12.sp)
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RedPrimary)
                }
            }

            if (filter == "all" || filter == "mine") {
                Text("Mes groupes (${uiState.myGroups.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Stone500)
                if (uiState.myGroups.isEmpty()) {
                    EmptyHint("Vous n'avez rejoint aucun groupe pour l'instant.")
                } else {
                    uiState.myGroups.forEach { group ->
                        GroupCard(
                            group = group,
                            actionLabel = "Quitter",
                            onAction = { viewModel.leaveGroup(group.groupId) },
                            onOpen = { onOpenGroupDetail(group.groupId) }
                        )
                    }
                }
            }

            if (filter == "all" || filter == "discover") {
                Text("Découvrir des groupes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Stone500)
                if (uiState.discoverGroups.isEmpty()) {
                    EmptyHint("Aucun nouveau groupe à découvrir.")
                } else {
                    uiState.discoverGroups.forEach { group ->
                        GroupCard(
                            group = group,
                            actionLabel = "Rejoindre",
                            onAction = { viewModel.joinGroup(group.groupId) },
                            onOpen = { onOpenGroupDetail(group.groupId) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateGroupSheet(
            onDismiss = { showCreateSheet = false },
            onCreate = { name, description, isPrivate ->
                viewModel.createGroup(name, description, isPrivate)
                showCreateSheet = false
            }
        )
    }
}

@Composable
private fun GroupCard(
    group: GroupDocument,
    actionLabel: String,
    onAction: () -> Unit,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(1.dp, StoneBorder, RoundedCornerShape(12.dp))
            .clickable { onOpen() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Group, null, tint = RedPrimary)
        }

        Spacer(Modifier.size(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(group.name, color = Stone800, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // un groupe privé reste visible seulement si l'utilisateur y a accès
                Icon(
                    if (group.visibility == "private") Icons.Default.Lock else Icons.Default.Public,
                    null,
                    tint = Stone400,
                    modifier = Modifier.size(12.dp)
                )
            }
            if (group.description.isNotBlank()) {
                Text(group.description, color = Stone400, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("${group.memberCount} membres · ${group.postCount} posts", color = Stone500, fontSize = 11.sp)
        }

        TextButton(onClick = onAction) {
            Text(actionLabel, color = RedPrimary)
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(10.dp))
            .border(1.dp, StoneBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(text, color = Stone400, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, isPrivate: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }

    // création rapide: le repository ajoute ensuite le créateur comme membre
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardBg) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Créer un groupe", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Stone800)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Nom du groupe") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RedPrimary,
                    unfocusedBorderColor = Stone300
                )
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RedPrimary,
                    unfocusedBorderColor = Stone300
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isPrivate,
                    onClick = { isPrivate = false },
                    label = { Text("Public") },
                    leadingIcon = { if (!isPrivate) Icon(Icons.Default.Check, null) }
                )
                FilterChip(
                    selected = isPrivate,
                    onClick = { isPrivate = true },
                    label = { Text("Privé") },
                    leadingIcon = { if (isPrivate) Icon(Icons.Default.Check, null) }
                )
            }

            Button(
                onClick = { onCreate(name.trim(), description.trim(), isPrivate) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
            ) {
                Text("Créer le groupe")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
