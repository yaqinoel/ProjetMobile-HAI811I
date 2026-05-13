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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.traveling.core.utils.openNavigationToPlace
import com.example.traveling.features.travelshare.PhotoPostUi
import com.example.traveling.ui.theme.*
import kotlinx.coroutines.launch

private val MOCK_LIKED_POSTS = listOf(
    PhotoPostUi("201", "https://images.unsplash.com/photo-1773318901379-aac92fdf5611?w=800", "Guilin", "Guangxi, Chine", "28 fév 2026", "Zhang Zhiyuan", "Z", Color(0xFF7C3AED), 891, true, false, "Les pics karstiques de Guilin", 35, listOf("Nature", "Montagne"), "nature", "3months"),
    PhotoPostUi("202", "https://images.unsplash.com/photo-1603120527222-33f28c2ce89e?w=800", "Cité Interdite", "Pékin, Chine", "10 mars 2026", "Wang Wanqing", "W", Color(0xFFD97706), 2567, true, true, "Architecture impériale", 89, listOf("Musée", "Architecture"), "museum", "3months"),
    PhotoPostUi("203", "https://images.unsplash.com/photo-1770035242840-4e25de3298ee?w=800", "Zhangjiajie", "Hunan, Chine", "15 fév 2026", "Chen Minghui", "C", Color(0xFF0D9488), 756, true, false, "Falaises de grès", 28, listOf("Nature", "Rue"), "street", "3months"),
    PhotoPostUi("204", "https://images.unsplash.com/photo-1586862118451-efc84a66e704?w=800", "Xi'an", "Shaanxi, Chine", "01 mars 2026", "Lin Yuxuan", "L", Color(0xFF2563EB), 645, true, true, "Marché street food du soir", 18, listOf("Food", "Rue"), "street", "month")
)

@Composable
fun LikedPostsScreen(
    onBack: () -> Unit = {},
    onOpenPhotoDetail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: LikedPostsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var posts by remember { mutableStateOf(emptyList<PhotoPostUi>()) }
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Tous") }
    var viewMode by remember { mutableStateOf("Liste") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadLikedPosts()
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is LikedPostsUiState.Success) {
            posts = state.posts
        }
    }

    val filtered = remember(posts, query, selectedFilter) {
        posts.filter { post ->
            val matchesQuery = query.isBlank() ||
                post.title.contains(query, true) ||
                post.location.contains(query, true) ||
                post.description.contains(query, true) ||
                post.author.contains(query, true) ||
                post.tags.any { it.contains(query, true) }
            val matchesFilter = when (selectedFilter) {
                "Nature" -> post.placeType == "nature"
                "Musée" -> post.placeType == "museum"
                "Rue" -> post.placeType == "street"
                "Food" -> post.tags.any { it.equals("Food", true) }
                "Proche de moi" -> post.country.contains("Pékin")
                else -> true
            }
            matchesQuery && matchesFilter
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
                        Text("Posts aimés", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StoneText)
                        Text("${posts.size} photos", fontSize = 12.sp, color = StoneMuted)
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
            SearchInputLiked(value = query, onValueChange = { query = it }, placeholder = "Rechercher des posts aimés...")

            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Tous", "Nature", "Musée", "Rue", "Food", "Proche de moi").forEach { filter ->
                    FilterChip(selected = selectedFilter == filter, onClick = { selectedFilter = filter }, label = { Text(filter) })
                }
            }

            if (viewMode == "Liste") {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(filtered, key = { it.id }) { post ->
                        LikedPostListCard(
                            post = post,
                            onOpen = { onOpenPhotoDetail(post.id) },
                            onUnlike = {
                                viewModel.unlike(post.id)
                                scope.launch { snackbarHostState.showSnackbar("Retiré des favoris") }
                            },
                            onToggleSave = {
                                viewModel.toggleSave(post.id, post.isSaved)
                            },
                            onRoute = {
                                val opened = openNavigationToPlace(
                                    context = context,
                                    placeName = post.location,
                                    latitude = post.latitude,
                                    longitude = post.longitude
                                )
                                if (!opened) {
                                    scope.launch { snackbarHostState.showSnackbar("Aucune application de carte disponible.") }
                                }
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
                    items(filtered, key = { it.id }) { post ->
                        LikedPostGridCard(
                            post = post,
                            onOpen = { onOpenPhotoDetail(post.id) },
                            onUnlike = {
                                viewModel.unlike(post.id)
                                scope.launch { snackbarHostState.showSnackbar("Retiré des favoris") }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LikedPostListCard(
    post: PhotoPostUi,
    onOpen: () -> Unit,
    onUnlike: () -> Unit,
    onToggleSave: () -> Unit,
    onRoute: () -> Unit
) {
    val displayTitle = post.title.ifBlank { post.location }
    val bodyText = post.description.takeIf { it.isNotBlank() && it != displayTitle }
    val displayLocation = listOf(post.location, post.country)
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" · ")
    Column(
        modifier = Modifier.fillMaxWidth().background(CardBg, RoundedCornerShape(12.dp)).border(1.dp, StoneBorder, RoundedCornerShape(12.dp)).clickable { onOpen() }.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(model = post.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp)))
        Text(displayTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Stone800)
        Text(displayLocation, color = StoneMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (bodyText != null) {
            Text(bodyText, maxLines = 2, overflow = TextOverflow.Ellipsis, color = StoneMuted, fontSize = 12.sp)
        }
        Text(post.author, color = StoneMuted, fontSize = 11.sp)
        Text(post.date, color = StoneMuted, fontSize = 11.sp)
        Text("${post.likes} likes", color = StoneMuted, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            post.tags.take(3).forEach { AssistChip(onClick = {}, label = { Text("#$it") }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onUnlike, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Favorite, null, tint = Color(0xFFDC2626)) }
            IconButton(onClick = onToggleSave, modifier = Modifier.weight(1f)) { Icon(if (post.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null, tint = Stone800) }
            IconButton(onClick = onRoute, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Route, null, tint = Color(0xFFD97706)) }
        }
    }
}

@Composable
private fun LikedPostGridCard(
    post: PhotoPostUi,
    onOpen: () -> Unit,
    onUnlike: () -> Unit
) {
    val displayTitle = post.title.ifBlank { post.location }
    Column(
        modifier = Modifier.fillMaxWidth().background(CardBg, RoundedCornerShape(10.dp)).border(1.dp, StoneBorder, RoundedCornerShape(10.dp)).padding(8.dp)
    ) {
        AsyncImage(
            model = post.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).clickable { onOpen() }
        )
        Spacer(Modifier.height(6.dp))
        Text(displayTitle, fontSize = 13.sp, color = Stone800, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(post.location, fontSize = 10.sp, color = StoneMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${post.likes}", fontSize = 11.sp, color = StoneMuted)
            IconButton(onClick = onUnlike, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Favorite, null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SearchInputLiked(value: String, onValueChange: (String) -> Unit, placeholder: String) {
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
