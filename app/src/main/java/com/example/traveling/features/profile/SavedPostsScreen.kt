package com.example.traveling.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.traveling.features.travelshare.model.PhotoPostUi
import com.example.traveling.ui.theme.*
import kotlinx.coroutines.launch

private data class SavedPostUi(
    val post: PhotoPostUi,
    val category: String,
    val savedAt: String,
    val linkedToTravelPath: Boolean = false,
    val groupName: String? = null
)

@Composable
fun SavedPostsScreen(
    onBack: () -> Unit = {},
    onOpenPhotoDetail: (String) -> Unit = {}
) {
    val viewModel: SavedPostsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var posts by remember { mutableStateOf(emptyList<SavedPostUi>()) }
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tous") }
    var viewMode by remember { mutableStateOf("Liste") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadSavedPosts()
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is SavedPostsUiState.Success) {
            posts = state.posts.map { it.toSavedPostUi() }
        }
    }

    val filtered = remember(posts, query, selectedCategory) {
        posts.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.post.title.contains(query, true) ||
                item.post.location.contains(query, true) ||
                item.post.description.contains(query, true) ||
                item.post.author.contains(query, true) ||
                item.post.tags.any { it.contains(query, true) }
            val matchesCategory = when (selectedCategory) {
                "À visiter", "Inspiration", "TravelPath", "Groupes" -> item.category == selectedCategory
                else -> true
            }
            matchesQuery && matchesCategory
        }
    }

    Scaffold(
        containerColor = ProfilePageBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = ProfileCardBg) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Stone800) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Posts enregistrés", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StoneText)
                        Text("${posts.size} sauvegardés", fontSize = 12.sp, color = StoneMuted)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(onClick = { viewMode = "Liste" }, label = { Text("Liste") })
                        AssistChip(onClick = { viewMode = "Grille" }, label = { Text("Grille") })
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SearchInputSaved(value = query, onValueChange = { query = it }, placeholder = "Rechercher des posts enregistrés...")

            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Tous", "À visiter", "Inspiration", "TravelPath", "Groupes").forEach { category ->
                    FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) })
                }
            }

            if (viewMode == "Liste") {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(filtered, key = { it.post.id }) { item ->
                        SavedPostListCard(
                            item = item,
                            onView = { onOpenPhotoDetail(item.post.id) },
                            onRemove = {
                                viewModel.unsave(item.post.id)
                                scope.launch { snackbarHostState.showSnackbar("Retiré des enregistrements") }
                            },
                            onAddToTravelPath = {
                                scope.launch { snackbarHostState.showSnackbar("Ajouté comme inspiration TravelPath") }
                            }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filtered, key = { it.post.id }) { item ->
                        SavedPostGridCard(
                            item = item,
                            onView = { onOpenPhotoDetail(item.post.id) },
                            onRemove = {
                                viewModel.unsave(item.post.id)
                                scope.launch { snackbarHostState.showSnackbar("Retiré des enregistrements") }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun PhotoPostUi.toSavedPostUi(): SavedPostUi {
    val category = when {
        tags.any { it.equals("TravelPath", true) } -> "TravelPath"
        tags.any { it.equals("Inspiration", true) } -> "Inspiration"
        tags.any { it.equals("Groupes", true) } -> "Groupes"
        else -> "À visiter"
    }
    return SavedPostUi(
        post = this,
        category = category,
        savedAt = "Enregistré récemment",
        linkedToTravelPath = tags.any { it.equals("TravelPath", true) }
    )
}

@Composable
private fun SavedPostListCard(
    item: SavedPostUi,
    onView: () -> Unit,
    onRemove: () -> Unit,
    onAddToTravelPath: () -> Unit
) {
    val displayTitle = item.post.title.ifBlank { item.post.location }
    val displayLocation = listOf(item.post.location, item.post.country)
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" · ")
    Column(
        modifier = Modifier.fillMaxWidth().background(CardBg, RoundedCornerShape(12.dp)).border(1.dp, StoneBorder, RoundedCornerShape(12.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(model = item.post.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp)).clickable { onView() })
        Text(displayTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Stone800)
        Text(displayLocation, color = StoneMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.post.author, color = StoneMuted, fontSize = 11.sp)
        Text(item.savedAt, color = RedPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item.post.tags.take(3).forEach { AssistChip(onClick = {}, label = { Text("#$it") }) }
            if (item.linkedToTravelPath) {
                AssistChip(onClick = {}, label = { Text("TravelPath") }, leadingIcon = { Icon(Icons.Default.Route, null, modifier = Modifier.size(14.dp)) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onView, modifier = Modifier.weight(1f)) { Text("Voir détail") }
            OutlinedButton(onClick = onRemove, modifier = Modifier.weight(1f)) { Text("Retirer") }
            Button(onClick = onAddToTravelPath, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))) {
                Text("TravelPath")
            }
        }
    }
}

@Composable
private fun SavedPostGridCard(
    item: SavedPostUi,
    onView: () -> Unit,
    onRemove: () -> Unit
) {
    val displayTitle = item.post.title.ifBlank { item.post.location }
    Column(
        modifier = Modifier.fillMaxWidth().background(CardBg, RoundedCornerShape(10.dp)).border(1.dp, StoneBorder, RoundedCornerShape(10.dp)).padding(8.dp)
    ) {
        AsyncImage(model = item.post.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).clickable { onView() })
        Spacer(Modifier.height(6.dp))
        Text(displayTitle, fontSize = 13.sp, color = Stone800, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.post.location, fontSize = 10.sp, color = StoneMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.savedAt, fontSize = 10.sp, color = StoneMuted, maxLines = 1)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.BookmarkRemove, null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SearchInputSaved(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp).background(CardBg, RoundedCornerShape(10.dp)).border(1.dp, StoneBorder, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = StoneMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(color = StoneText)) { inner ->
            if (value.isBlank()) Text(placeholder, color = StoneMuted, fontSize = 13.sp)
            inner()
        }
    }
}
