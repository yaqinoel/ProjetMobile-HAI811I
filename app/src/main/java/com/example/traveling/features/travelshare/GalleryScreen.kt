package com.example.traveling.features.travelshare

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.traveling.core.utils.openNavigationToPlace
import com.example.traveling.features.travelshare.model.GalleryFilter
import com.example.traveling.features.travelshare.model.PhotoPostUi
import com.example.traveling.features.travelshare.util.filterGalleryPosts
import com.example.traveling.ui.theme.*
import kotlinx.coroutines.launch

private data class PhotoFilterOption(val id: String, val label: String)

private val PHOTO_PLACE_TYPES = listOf(
    PhotoFilterOption("all", "Tous"),
    PhotoFilterOption("nature", "Nature"),
    PhotoFilterOption("museum", "Musée"),
    PhotoFilterOption("street", "Rue"),
    PhotoFilterOption("shop", "Magasin"),
    PhotoFilterOption("monument", "Monument"),
    PhotoFilterOption("architecture", "Architecture")
)

private val PHOTO_PERIODS = listOf(
    PhotoFilterOption("all", "Toutes"),
    PhotoFilterOption("week", "Cette semaine"),
    PhotoFilterOption("month", "Ce mois"),
    PhotoFilterOption("3months", "3 derniers mois"),
    PhotoFilterOption("year", "Cette année")
)

private val PHOTO_RADIUS = listOf(
    PhotoFilterOption("all", "Tout lieu"),
    PhotoFilterOption("nearby", "Autour d'un lieu"),
    PhotoFilterOption("similar", "Photos similaires")
)

// ─── 核心 UI ───
@Composable
fun GalleryScreen(
    isAnonymous: Boolean = false,
    onOpenNotifications: () -> Unit = {},
    onPhotoClick: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onGroupClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val galleryViewModel: GalleryViewModel = viewModel()
    val galleryState by galleryViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        galleryViewModel.observeVisiblePosts()
    }

    val remotePosts = when (val state = galleryState) {
        is GalleryUiState.Success -> state.posts
        else -> emptyList()
    }
    val shortcuts = when (val state = galleryState) {
        is GalleryUiState.Success -> state.shortcuts
        else -> emptyList()
    }

    var viewMode by rememberSaveable { mutableStateOf("list") } // "list", "grid", "map"
    var shuffledPhotos by remember { mutableStateOf<List<PhotoPostUi>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("all") }
    var selectedPeriod by remember { mutableStateOf("all") }
    var selectedDiscovery by remember { mutableStateOf("all") }
    var isVoiceSearchActive by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(remotePosts) {
        shuffledPhotos = null
    }

    val photos = shuffledPhotos ?: remotePosts.sortedByDescending { it.createdAtMillis ?: 0L }
    val nearbyCenter = remember(remotePosts, searchQuery, selectedDiscovery) {
        if (selectedDiscovery == "nearby") findNearbyCenter(remotePosts, searchQuery) else null
    }
    val effectiveQuery = if (selectedDiscovery == "nearby" && nearbyCenter != null) "" else searchQuery
    val galleryFilter = remember(effectiveQuery, selectedType, selectedPeriod, selectedDiscovery, nearbyCenter) {
        GalleryFilter(
            query = effectiveQuery,
            placeType = selectedType,
            period = selectedPeriod,
            discoveryMode = selectedDiscovery,
            centerLatitude = nearbyCenter?.first,
            centerLongitude = nearbyCenter?.second
        )
    }
    val filteredPhotos = remember(photos, galleryFilter) {
        filterGalleryPosts(photos, galleryFilter)
    }

    Box(modifier = Modifier.fillMaxSize().background(PageBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderBar(
                viewMode = viewMode,
                searchQuery = searchQuery,
                showFilters = showFilters,
                selectedType = selectedType,
                selectedPeriod = selectedPeriod,
                selectedDiscovery = selectedDiscovery,
                resultCount = filteredPhotos.size,
                isVoiceSearchActive = isVoiceSearchActive,
                onOpenNotifications = onOpenNotifications,
                onViewModeChanged = { viewMode = it },
                onSearchQueryChanged = { searchQuery = it },
                onToggleFilters = { showFilters = !showFilters },
                onTypeSelected = { selectedType = it },
                onPeriodSelected = { selectedPeriod = it },
                onDiscoverySelected = { selectedDiscovery = it },
                onToggleVoiceSearch = {
                    isVoiceSearchActive = !isVoiceSearchActive
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Recherche vocale à connecter plus tard.")
                    }
                },
                isAnonymous = isAnonymous,
                onShuffle = { shuffledPhotos = photos.shuffled() }
            )

            Crossfade(targetState = viewMode, label = "ViewMode") { mode ->
                if (galleryState is GalleryUiState.Loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RedPrimary)
                    }
                } else if (galleryState is GalleryUiState.Error) {
                    val message = (galleryState as GalleryUiState.Error).message
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(message, color = StoneMuted, fontSize = 13.sp)
                    }
                } else if (filteredPhotos.isEmpty()) {
                    EmptyPhotoResults()
                } else {
                    when (mode) {
                        "list" -> PhotoListView(
                            photos = filteredPhotos,
                            onLike = { photoId ->
                                val target = filteredPhotos.find { it.id == photoId } ?: return@PhotoListView
                                galleryViewModel.toggleLike(photoId, target.isLiked)
                            },
                            onSave = { photoId ->
                                val target = filteredPhotos.find { it.id == photoId } ?: return@PhotoListView
                                galleryViewModel.toggleSave(photoId, target.isSaved)
                            },
                            onReport = { photoId ->
                                galleryViewModel.reportPost(photoId)
                                coroutineScope.launch { snackbarHostState.showSnackbar("Signalement enregistré.") }
                            },
                            onNavigate = { photoId ->
                                val target = filteredPhotos.find { it.id == photoId }
                                if (target != null) {
                                    val opened = openNavigationToPlace(
                                        context = context,
                                        placeName = target.location,
                                        latitude = target.latitude,
                                        longitude = target.longitude
                                    )
                                    if (!opened) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Aucune application de carte disponible.")
                                        }
                                    }
                                }
                            },
                            showStories = searchQuery.isBlank() && selectedType == "all" && selectedPeriod == "all" && selectedDiscovery == "all",
                            shortcuts = shortcuts,
                            isAnonymous = isAnonymous,
                            onPhotoClick = onPhotoClick,
                            onAuthorClick = onAuthorClick,
                            onGroupClick = onGroupClick
                        )
                        "grid" -> PhotoGridView(photos = filteredPhotos, onPhotoClick = onPhotoClick)
                        "map" -> MapView(photos = filteredPhotos, onSelectPhoto = onPhotoClick)
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

// ─── 顶部导航栏 ───
@Composable
private fun HeaderBar(
    viewMode: String,
    searchQuery: String,
    showFilters: Boolean,
    selectedType: String,
    selectedPeriod: String,
    selectedDiscovery: String,
    resultCount: Int,
    isVoiceSearchActive: Boolean,
    onOpenNotifications: () -> Unit,
    onViewModeChanged: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleFilters: () -> Unit,
    onTypeSelected: (String) -> Unit,
    onPeriodSelected: (String) -> Unit,
    onDiscoverySelected: (String) -> Unit,
    onToggleVoiceSearch: () -> Unit,
    isAnonymous: Boolean,
    onShuffle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBg,
        shadowElevation = if (showFilters) 4.dp else 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Galerie", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StoneText)
                    Text(if (viewMode == "map") "Vue carte" else "Photos de voyage", fontSize = 11.sp, color = StoneMuted)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onShuffle, modifier = Modifier.size(36.dp).background(RedLight, RoundedCornerShape(8.dp)).border(1.dp, RedPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Aléatoire", tint = RedPrimary, modifier = Modifier.size(18.dp))
                    }
                    ViewToggle(viewMode = viewMode, onSetViewMode = onViewModeChanged)

                    if (!isAnonymous) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(RedLight, RoundedCornerShape(8.dp))
                                .border(1.dp, RedPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .clickable { onOpenNotifications()},
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = RedPrimary, modifier = Modifier.size(18.dp))
                            Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape).align(Alignment.TopEnd).offset((-6).dp, 6.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Stone100, RoundedCornerShape(8.dp))
                    .border(1.dp, if (searchQuery.isNotBlank()) RedPrimary.copy(alpha = 0.25f) else Color.Transparent, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = StoneMuted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    textStyle = TextStyle(color = StoneText, fontSize = 14.sp),
                    cursorBrush = SolidColor(RedPrimary),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (searchQuery.isBlank()) {
                            Text("Description, lieu, thème, auteur...", color = StoneMuted, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )
                IconButton(onClick = onToggleVoiceSearch, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Outlined.Mic,
                        contentDescription = "Recherche vocale",
                        tint = if (isVoiceSearchActive) RedPrimary else StoneMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChanged("") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Effacer", tint = StoneMuted, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(
                    onClick = onToggleFilters,
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (showFilters) RedPrimary else RedLight, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Filtres", tint = if (showFilters) Color.White else RedPrimary, modifier = Modifier.size(18.dp))
                }
            }

            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FilterRow(
                        title = "Type de lieu",
                        options = PHOTO_PLACE_TYPES,
                        selected = selectedType,
                        onSelected = onTypeSelected
                    )
                    FilterRow(
                        title = "Période",
                        options = PHOTO_PERIODS,
                        selected = selectedPeriod,
                        onSelected = onPeriodSelected
                    )
                    FilterRow(
                        title = "Mode de découverte",
                        options = PHOTO_RADIUS,
                        selected = selectedDiscovery,
                        onSelected = onDiscoverySelected
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$resultCount photo${if (resultCount > 1) "s" else ""}", fontSize = 11.sp, color = StoneMuted, fontWeight = FontWeight.Medium)
                if (selectedType != "all" || selectedPeriod != "all" || selectedDiscovery != "all") {
                    Text("Filtres actifs", fontSize = 11.sp, color = RedPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    title: String,
    options: List<PhotoFilterOption>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = StoneMuted, modifier = Modifier.padding(bottom = 6.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                val isSelected = selected == option.id
                Text(
                    text = option.label,
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White else StoneMuted,
                    modifier = Modifier
                        .background(if (isSelected) RedPrimary else Stone100, RoundedCornerShape(6.dp))
                        .clickable { onSelected(option.id) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ─── 视图切换器 ───
@Composable
private fun ViewToggle(viewMode: String, onSetViewMode: (String) -> Unit) {
    Row(modifier = Modifier.background(Color(0xFFF5F5F4), RoundedCornerShape(8.dp)).padding(2.dp)) {
        listOf("list" to Icons.Default.ViewList, "grid" to Icons.Default.GridView, "map" to Icons.Default.Map).forEach { (mode, icon) ->
            val isSelected = viewMode == mode
            Box(
                modifier = Modifier.size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable { onSetViewMode(mode) },
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = mode, tint = if (isSelected) RedPrimary else StoneMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─── 顶部 Stories 横向列表 ───
@Composable
private fun FollowedShortcutsRow(
    shortcuts: List<TravelShareShortcutUi>,
    onAuthorClick: (String) -> Unit,
    onGroupClick: (String) -> Unit
) {
    if (shortcuts.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth().background(CardBg, RoundedCornerShape(12.dp)).padding(vertical = 10.dp)
    ) {
        Text(
            "Voyageurs suivis & groupes",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = StoneMuted,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(shortcuts, key = { "${it.type}-${it.id}" }) { shortcut ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(62.dp)
                        .clickable {
                            if (shortcut.type == "user") onAuthorClick(shortcut.id) else onGroupClick(shortcut.id)
                        }
                ) {
                    val borderBrush = Brush.linearGradient(
                        if (shortcut.type == "user") {
                            listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFFDC2626))
                        } else {
                            listOf(Color(0xFFD97706), Color(0xFFFBBF24))
                        }
                    )
                    Box(modifier = Modifier.size(48.dp).background(borderBrush, CircleShape).padding(2.dp)) {
                        Box(modifier = Modifier.fillMaxSize().background(CardBg, CircleShape).padding(2.dp)) {
                            Box(modifier = Modifier.fillMaxSize().background(shortcut.color, CircleShape), contentAlignment = Alignment.Center) {
                                Text(shortcut.initial, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(shortcut.label, fontSize = 9.sp, color = StoneText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ─── List 列表视图 (详细卡片) ───
@Composable
fun PhotoListView(
    photos: List<PhotoPostUi>,
    onLike: (String) -> Unit,
    onSave: (String) -> Unit,
    onReport: (String) -> Unit,
    onNavigate: (String) -> Unit,
    showStories: Boolean,
    shortcuts: List<TravelShareShortcutUi> = emptyList(),
    isAnonymous: Boolean,
    onPhotoClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onGroupClick: (String) -> Unit = {}
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (showStories && shortcuts.isNotEmpty()) {
            item {
                FollowedShortcutsRow(
                    shortcuts = shortcuts,
                    onAuthorClick = onAuthorClick,
                    onGroupClick = onGroupClick
                )
            }
        }
        items(
            items = photos,
            key = { it.id }
        ) { photo ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth().clickable { onPhotoClick(photo.id)}
            ) {
                Column {
                    // 卡片头部 (作者、位置)
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(photo.authorColor, CircleShape)
                                .clickable(enabled = photo.authorId.isNotBlank()) { onAuthorClick(photo.authorId) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(photo.authorAvatar, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(photo.author, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StoneText)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = StoneMuted, modifier = Modifier.size(12.dp))
                                Text("${photo.location}, ${photo.country}", fontSize = 11.sp, color = StoneMuted, maxLines = 1)
                            }
                        }
                        if (photo.visibility == "group" && !photo.groupName.isNullOrBlank()) {
                            Text(
                                text = photo.groupName.orEmpty(),
                                fontSize = 10.sp,
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .widthIn(max = 92.dp)
                                    .background(Color(0xFFFFF7ED), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        IconButton(onClick = { onNavigate(photo.id) }, modifier = Modifier.size(32.dp).background(RedLight, RoundedCornerShape(8.dp))) {
                            Icon(Icons.Outlined.Navigation, contentDescription = "Naviguer", tint = RedPrimary, modifier = Modifier.size(14.dp))
                        }
                    }

                    // 主图片
                    AsyncImage(
                        model = photo.imageUrl, contentDescription = photo.description, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                    )

                    // 互动按钮、文字与标签
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (photo.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (photo.isLiked) Color.Red else StoneText,
                                        modifier = Modifier.size(22.dp).clickable { onLike(photo.id) }
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("${photo.likes}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment", tint = StoneText, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${photo.comments}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Icon(Icons.Outlined.Share, contentDescription = "Share", tint = StoneText, modifier = Modifier.size(20.dp))
                                Icon(Icons.Outlined.Flag, contentDescription = "Signaler", tint = StoneText, modifier = Modifier.size(20.dp).clickable { onReport(photo.id) })
                            }
                            Icon(
                                if (photo.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save",
                                tint = if (photo.isSaved) Color(0xFFD97706) else StoneText,
                                modifier = Modifier.size(22.dp).clickable { onSave(photo.id) }
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(text = photo.description, fontSize = 13.sp, color = StoneText, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            photo.tags.forEach { tag ->
                                Text("#$tag", fontSize = 11.sp, color = RedPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.background(RedLight, RoundedCornerShape(12.dp)).border(1.dp, RedPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(photo.date, fontSize = 10.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

// ─── Grid 网格视图 ───
@Composable
private fun PhotoGridView(photos: List<PhotoPostUi>, onPhotoClick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = photos,
            key = { it.id }
        ) { photo ->
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(12.dp)).clickable { onPhotoClick(photo.id) }) {
                AsyncImage(model = photo.imageUrl, contentDescription = photo.location, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                // 底部渐变黑色背景
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.8f)))))

                // 右上角点赞按钮
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(if (photo.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Like", tint = if (photo.isLiked) Color.Red else Color.White, modifier = Modifier.size(14.dp))
                }
                if (photo.visibility == "group" && !photo.groupName.isNullOrBlank()) {
                    Text(
                        text = photo.groupName.orEmpty(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .widthIn(max = 96.dp)
                            .background(Color(0xFFD97706).copy(alpha = 0.86f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // 左下角文字信息
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(photo.location, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(photo.country, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyPhotoResults() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(64.dp).background(Stone100, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.SearchOff, contentDescription = null, tint = StoneMuted, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Aucune photo trouvée", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StoneText)
        Spacer(Modifier.height(4.dp))
        Text("Essayez un autre lieu, auteur, tag ou filtre.", fontSize = 13.sp, color = StoneMuted)
    }
}

private fun findNearbyCenter(posts: List<PhotoPostUi>, query: String): Pair<Double, Double>? {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return null

    return posts.firstOrNull { post ->
        post.latitude != null &&
            post.longitude != null &&
            (
                post.location.contains(normalizedQuery, ignoreCase = true) ||
                    post.city.contains(normalizedQuery, ignoreCase = true) ||
                    post.country.contains(normalizedQuery, ignoreCase = true)
                )
    }?.let { post ->
        val lat = post.latitude
        val lng = post.longitude
        if (lat != null && lng != null) lat to lng else null
    }
}

@Preview(showBackground = true, name = "Mode Connecté (已登录状态)")
@Composable
fun GalleryScreenPreview() {
    // 预览已登录用户的界面
    GalleryScreen(isAnonymous = false)
}

@Preview(showBackground = true, name = "Mode Anonyme (匿名状态)")
@Composable
fun GalleryScreenAnonymousPreview() {
    // 预览匿名用户的界面（你会发现顶部的加号和通知小铃铛不见了）
    GalleryScreen(isAnonymous = true)
}
