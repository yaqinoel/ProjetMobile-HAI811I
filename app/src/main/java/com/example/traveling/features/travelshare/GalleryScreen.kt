package com.example.traveling.features.travelshare

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.example.traveling.ui.components.UserAvatar
import com.example.traveling.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
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
    PhotoFilterOption("year", "Cette année"),
    PhotoFilterOption("custom", "Plage")
)

private val PHOTO_RADIUS = listOf(
    PhotoFilterOption("all", "Tout lieu"),
    PhotoFilterOption("nearby", "Autour d'un lieu"),
    PhotoFilterOption("similar", "Photos similaires")
)

@Composable
fun GalleryScreen(
    isAnonymous: Boolean = false,
    initialSimilarPostId: String? = null,
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

    var viewMode by rememberSaveable { mutableStateOf("list") }
    var shuffledPhotos by remember { mutableStateOf<List<PhotoPostUi>?>(null) }
    var similarToPostId by rememberSaveable { mutableStateOf(initialSimilarPostId.orEmpty()) }
    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("all") }
    var selectedPeriod by remember { mutableStateOf("all") }
    var selectedDiscovery by remember { mutableStateOf(if (initialSimilarPostId.isNullOrBlank()) "all" else "similar") }
    var dateRangeStartMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var dateRangeEndMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var nearbyLocationName by rememberSaveable { mutableStateOf("") }
    var nearbyLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var nearbyLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var nearbyRadiusKm by rememberSaveable { mutableStateOf(10f) }
    var showNearbyPicker by remember { mutableStateOf(false) }
    var isVoiceSearchActive by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isVoiceSearchActive = false
        if (result.resultCode == Activity.RESULT_OK) {
            val recognizedText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()

            if (recognizedText.isNotBlank()) {
                searchQuery = recognizedText
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Aucun texte reconnu.")
                }
            }
        }
    }
    val startVoiceSearch: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Décrivez une photo, un lieu ou un thème")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        isVoiceSearchActive = true
        runCatching { voiceSearchLauncher.launch(intent) }
            .onFailure {
                isVoiceSearchActive = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Recherche vocale indisponible sur cet appareil.")
                }
            }
        Unit
    }

    LaunchedEffect(remotePosts) {
        shuffledPhotos = null
    }

    LaunchedEffect(initialSimilarPostId) {
        if (!initialSimilarPostId.isNullOrBlank() && initialSimilarPostId != similarToPostId) {
            similarToPostId = initialSimilarPostId
            selectedDiscovery = "similar"
            selectedType = "all"
            selectedPeriod = "all"
            dateRangeStartMillis = null
            dateRangeEndMillis = null
            searchQuery = ""
            showFilters = false
            shuffledPhotos = null
        }
    }

    val photos = shuffledPhotos ?: remotePosts.sortedByDescending { it.createdAtMillis ?: 0L }
    val activeSimilarPostId = similarToPostId.takeIf { it.isNotBlank() }
    val effectiveDiscovery = if (activeSimilarPostId != null) "similar" else selectedDiscovery
    val similarSourcePhoto = remember(photos, activeSimilarPostId) {
        activeSimilarPostId?.let { sourceId -> photos.find { it.id == sourceId } }
    }
    val nearbyLat = nearbyLatitude
    val nearbyLng = nearbyLongitude
    val nearbyCenter = if (effectiveDiscovery == "nearby" && nearbyLat != null && nearbyLng != null) {
        nearbyLat to nearbyLng
    } else {
        null
    }
    val effectiveQuery = searchQuery
    val dateRangeLabel = remember(dateRangeStartMillis, dateRangeEndMillis) {
        formatDateRangeLabel(dateRangeStartMillis, dateRangeEndMillis)
    }
    val hasActiveFilters = selectedType != "all" ||
        selectedPeriod != "all" ||
        dateRangeStartMillis != null ||
        dateRangeEndMillis != null ||
        effectiveDiscovery != "all"
    val clearFilters = {
        selectedType = "all"
        selectedPeriod = "all"
        dateRangeStartMillis = null
        dateRangeEndMillis = null
        selectedDiscovery = "all"
        similarToPostId = ""
        nearbyLocationName = ""
        nearbyLatitude = null
        nearbyLongitude = null
        nearbyRadiusKm = 10f
    }
    val galleryFilter = remember(
        effectiveQuery,
        selectedType,
        selectedPeriod,
        dateRangeStartMillis,
        dateRangeEndMillis,
        effectiveDiscovery,
        nearbyCenter,
        nearbyRadiusKm,
        activeSimilarPostId
    ) {
        GalleryFilter(
            query = effectiveQuery,
            placeType = selectedType,
            period = selectedPeriod,
            dateRangeStartMillis = dateRangeStartMillis,
            dateRangeEndMillis = dateRangeEndMillis,
            discoveryMode = effectiveDiscovery,
            centerLatitude = nearbyCenter?.first,
            centerLongitude = nearbyCenter?.second,
            radiusKm = nearbyRadiusKm.toDouble(),
            similarToPostId = activeSimilarPostId
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
                selectedDiscovery = effectiveDiscovery,
                dateRangeLabel = dateRangeLabel,
                nearbyLocationName = nearbyLocationName,
                nearbyRadiusKm = nearbyRadiusKm,
                hasNearbyCenter = nearbyCenter != null,
                hasActiveFilters = hasActiveFilters,
                resultCount = filteredPhotos.size,
                isVoiceSearchActive = isVoiceSearchActive,
                onOpenNotifications = onOpenNotifications,
                onViewModeChanged = { viewMode = it },
                onSearchQueryChanged = { searchQuery = it },
                onToggleFilters = { showFilters = !showFilters },
                onTypeSelected = { selectedType = it },
                onPeriodSelected = {
                    selectedPeriod = it
                    if (it == "custom") {
                        showDateRangePicker = true
                    } else {
                        dateRangeStartMillis = null
                        dateRangeEndMillis = null
                    }
                },
                onDiscoverySelected = {
                    selectedDiscovery = it
                    if (it != "similar") similarToPostId = ""
                },
                onOpenDateRangePicker = { showDateRangePicker = true },
                onClearDateRange = {
                    selectedPeriod = "all"
                    dateRangeStartMillis = null
                    dateRangeEndMillis = null
                },
                onOpenNearbyPicker = { showNearbyPicker = true },
                onRadiusChanged = { nearbyRadiusKm = it },
                onClearNearbyLocation = {
                    nearbyLocationName = ""
                    nearbyLatitude = null
                    nearbyLongitude = null
                },
                onClearFilters = clearFilters,
                onToggleVoiceSearch = startVoiceSearch,
                isAnonymous = isAnonymous,
                onShuffle = { shuffledPhotos = photos.shuffled() }
            )

            if (activeSimilarPostId != null) {
                SimilarSearchBanner(
                    sourcePhoto = similarSourcePhoto,
                    onClear = {
                        similarToPostId = ""
                        selectedDiscovery = "all"
                    }
                )
            }

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
                            showStories = searchQuery.isBlank() && selectedType == "all" && selectedPeriod == "all" && effectiveDiscovery == "all",
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

        if (showDateRangePicker) {
            GalleryDateRangeDialog(
                initialStartMillis = dateRangeStartMillis,
                initialEndMillis = dateRangeEndMillis,
                onDismiss = { showDateRangePicker = false },
                onConfirm = { startMillis, endMillis ->
                    dateRangeStartMillis = startMillis
                    dateRangeEndMillis = endMillis
                    selectedPeriod = "custom"
                    showDateRangePicker = false
                }
            )
        }

        if (showNearbyPicker) {
            NearbyMapPickerOverlay(
                initialLatitude = nearbyLatitude,
                initialLongitude = nearbyLongitude,
                onDismiss = { showNearbyPicker = false },
                onConfirm = { latLng ->
                    nearbyLocationName = formatCoordinateLabel(latLng.latitude, latLng.longitude)
                    nearbyLatitude = latLng.latitude
                    nearbyLongitude = latLng.longitude
                    selectedDiscovery = "nearby"
                    similarToPostId = ""
                    showNearbyPicker = false
                }
            )
        }
    }
}

@Composable
private fun HeaderBar(
    viewMode: String,
    searchQuery: String,
    showFilters: Boolean,
    selectedType: String,
    selectedPeriod: String,
    selectedDiscovery: String,
    dateRangeLabel: String?,
    nearbyLocationName: String,
    nearbyRadiusKm: Float,
    hasNearbyCenter: Boolean,
    hasActiveFilters: Boolean,
    resultCount: Int,
    isVoiceSearchActive: Boolean,
    onOpenNotifications: () -> Unit,
    onViewModeChanged: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleFilters: () -> Unit,
    onTypeSelected: (String) -> Unit,
    onPeriodSelected: (String) -> Unit,
    onDiscoverySelected: (String) -> Unit,
    onOpenDateRangePicker: () -> Unit,
    onClearDateRange: () -> Unit,
    onOpenNearbyPicker: () -> Unit,
    onRadiusChanged: (Float) -> Unit,
    onClearNearbyLocation: () -> Unit,
    onClearFilters: () -> Unit,
    onToggleVoiceSearch: () -> Unit,
    isAnonymous: Boolean,
    onShuffle: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val filtersMaxHeight = (configuration.screenHeightDp * 0.42f).dp

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
                            Text("Lieu, thème, auteur...", color = StoneMuted, fontSize = 14.sp)
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
                Spacer(Modifier.width(8.dp))
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
                Column(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .heightIn(max = filtersMaxHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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
                    if (selectedPeriod == "custom" || dateRangeLabel != null) {
                        DateRangeFilterCard(
                            label = dateRangeLabel,
                            onOpenPicker = onOpenDateRangePicker,
                            onClear = onClearDateRange
                        )
                    }
                    FilterRow(
                        title = "Mode de découverte",
                        options = PHOTO_RADIUS,
                        selected = selectedDiscovery,
                        onSelected = onDiscoverySelected
                    )
                    if (selectedDiscovery == "nearby") {
                        NearbyFilterCard(
                            locationName = nearbyLocationName,
                            radiusKm = nearbyRadiusKm,
                            hasCenter = hasNearbyCenter,
                            onChooseLocation = onOpenNearbyPicker,
                            onRadiusChanged = onRadiusChanged,
                            onClearLocation = onClearNearbyLocation
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$resultCount photo${if (resultCount > 1) "s" else ""}", fontSize = 11.sp, color = StoneMuted, fontWeight = FontWeight.Medium)
                if (hasActiveFilters) {
                    Text(
                        "Réinitialiser",
                        fontSize = 11.sp,
                        color = RedPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onClearFilters() }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateRangeFilterCard(
    label: String?,
    onOpenPicker: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, StoneBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Plage de dates", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StoneText)
                Text(label ?: "Choisissez une date de début et de fin", fontSize = 11.sp, color = StoneMuted)
            }
            TextButton(onClick = onOpenPicker, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Text(if (label == null) "Choisir" else "Modifier", fontSize = 12.sp)
            }
            if (label != null) {
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Effacer", tint = StoneMuted, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun NearbyFilterCard(
    locationName: String,
    radiusKm: Float,
    hasCenter: Boolean,
    onChooseLocation: () -> Unit,
    onRadiusChanged: (Float) -> Unit,
    onClearLocation: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Stone100,
        border = androidx.compose.foundation.BorderStroke(1.dp, StoneBorder.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.TravelExplore, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Centre de recherche", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StoneText)
                    Text(
                        if (hasCenter) locationName.ifBlank { "Point sélectionné" } else "Déplacez la carte pour choisir un point",
                        fontSize = 11.sp,
                        color = if (hasCenter) StoneMuted else Color(0xFFB45309),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(onClick = onChooseLocation, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(if (hasCenter) "Modifier" else "Choisir", fontSize = 12.sp)
                }
                if (hasCenter) {
                    IconButton(onClick = onClearLocation, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Effacer", tint = StoneMuted, modifier = Modifier.size(15.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("1", fontSize = 10.sp, color = StoneMuted)
                Slider(
                    value = radiusKm,
                    onValueChange = { onRadiusChanged(it.roundToInt().coerceIn(1, 50).toFloat()) },
                    valueRange = 1f..50f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = RedPrimary,
                        activeTrackColor = RedPrimary,
                        inactiveTrackColor = Color.White
                    )
                )
                Text("50 km", fontSize = 10.sp, color = StoneMuted)
                Text(
                    "${radiusKm.roundToInt()} km",
                    fontSize = 12.sp,
                    color = RedPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(48.dp)
                )
            }
        }
    }
}

@Composable
private fun GalleryDateRangeDialog(
    initialStartMillis: Long?,
    initialEndMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
) {
    val today = remember { DateParts.today() }
    val defaultStart = remember { today.minusDays(30) }
    val initialStart = remember(initialStartMillis) { initialStartMillis?.toDateParts() ?: defaultStart }
    val initialEnd = remember(initialEndMillis) { initialEndMillis?.toDateParts() ?: today }

    var startYear by remember { mutableStateOf(initialStart.year) }
    var startMonth by remember { mutableStateOf(initialStart.month) }
    var startDay by remember { mutableStateOf(initialStart.day) }
    var endYear by remember { mutableStateOf(initialEnd.year) }
    var endMonth by remember { mutableStateOf(initialEnd.month) }
    var endDay by remember { mutableStateOf(initialEnd.day) }

    LaunchedEffect(startYear, startMonth) {
        startDay = startDay.coerceAtMost(daysInMonth(startYear, startMonth))
    }
    LaunchedEffect(endYear, endMonth) {
        endDay = endDay.coerceAtMost(daysInMonth(endYear, endMonth))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("Plage de dates", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StoneText)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DateWheelSection(
                    title = "Début",
                    year = startYear,
                    month = startMonth,
                    day = startDay,
                    onYearChange = { startYear = it },
                    onMonthChange = { startMonth = it },
                    onDayChange = { startDay = it }
                )
                DateWheelSection(
                    title = "Fin",
                    year = endYear,
                    month = endMonth,
                    day = endDay,
                    onYearChange = { endYear = it },
                    onMonthChange = { endMonth = it },
                    onDayChange = { endDay = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val start = DateParts(startYear, startMonth, startDay).toStartMillis()
                    val end = DateParts(endYear, endMonth, endDay).toStartMillis()
                    if (start <= end) {
                        onConfirm(start, end)
                    } else {
                        onConfirm(end, start)
                    }
                }
            ) {
                Text("Appliquer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun DateWheelSection(
    title: String,
    year: Int,
    month: Int,
    day: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit
) {
    val yearRange = remember { (2020..2030).toList() }
    val monthRange = remember { (1..12).toList() }
    val dayRange = remember(year, month) { (1..daysInMonth(year, month)).toList() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StoneMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            WheelNumberPicker(
                label = "Année",
                values = yearRange,
                selected = year,
                onSelected = onYearChange,
                modifier = Modifier.weight(1.25f)
            )
            WheelNumberPicker(
                label = "Mois",
                values = monthRange,
                selected = month,
                onSelected = onMonthChange,
                formatter = { "%02d".format(it) },
                modifier = Modifier.weight(1f)
            )
            WheelNumberPicker(
                label = "Jour",
                values = dayRange,
                selected = day.coerceAtMost(dayRange.last()),
                onSelected = onDayChange,
                formatter = { "%02d".format(it) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WheelNumberPicker(
    label: String,
    values: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    formatter: (Int) -> String = { it.toString() }
) {
    val selectedIndex = values.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = StoneMuted)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .background(Stone100, RoundedCornerShape(10.dp))
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(values) { value ->
                val isSelected = value == selected
                Text(
                    text = formatter(value),
                    fontSize = if (isSelected) 16.sp else 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) RedPrimary else StoneMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(value) }
                        .background(if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                        .padding(vertical = 7.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun NearbyMapPickerOverlay(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onDismiss: () -> Unit,
    onConfirm: (LatLng) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val initialTarget = remember(initialLatitude, initialLongitude) {
        LatLng(initialLatitude ?: 43.6108, initialLongitude ?: 3.8767)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialTarget, 13f)
    }
    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = true,
            scrollGesturesEnabled = true,
            zoomGesturesEnabled = true,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false,
            mapToolbarEnabled = false
        )
    }
    val mapProperties = remember { MapProperties(isBuildingEnabled = true) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f))) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = mapUiSettings,
            properties = mapProperties,
            onMapClick = { latLng ->
                coroutineScope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLng(latLng))
                }
            }
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-18).dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(42.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardBg.copy(alpha = 0.96f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(60.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = StoneText)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Choisir le centre", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StoneText)
                    Text("Déplacez la carte ou touchez un point", fontSize = 11.sp, color = StoneMuted)
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = CardBg,
            shadowElevation = 8.dp
        ) {
            val target = cameraPositionState.position.target
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Point sélectionné", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StoneText)
                Text(formatCoordinateLabel(target.latitude, target.longitude), fontSize = 12.sp, color = StoneMuted)
                Button(
                    onClick = { onConfirm(cameraPositionState.position.target) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Utiliser ce point")
                }
            }
        }
    }
}

@Composable
private fun SimilarSearchBanner(
    sourcePhoto: PhotoPostUi?,
    onClear: () -> Unit
) {
    val sourceLabel = sourcePhoto
        ?.title
        ?.takeIf { it.isNotBlank() }
        ?: sourcePhoto?.location
        ?: "cette photo"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFF7ED),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFFFEDD5), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Collections, contentDescription = null, tint = Color(0xFFC2410C), modifier = Modifier.size(17.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Recherche par similarité", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Stone800)
                Text(
                    "Photos proches de $sourceLabel",
                    fontSize = 11.sp,
                    color = StoneMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Annuler", tint = StoneMuted, modifier = Modifier.size(16.dp))
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
                            UserAvatar(
                                avatarUrl = shortcut.avatarUrl,
                                fallbackText = shortcut.initial,
                                backgroundColor = shortcut.color,
                                modifier = Modifier.fillMaxSize(),
                                textSize = 13.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(shortcut.label, fontSize = 9.sp, color = StoneText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun PhotoListView(
    photos: List<PhotoPostUi>,
    onLike: (String) -> Unit,
    onSave: (String) -> Unit,
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
            val displayTitle = photo.title.ifBlank { photo.location }
            val bodyText = photo.description.takeIf { it.isNotBlank() && it != displayTitle }
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth().clickable { onPhotoClick(photo.id)}
            ) {
                Column {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(
                            avatarUrl = photo.authorAvatarUrl,
                            fallbackText = photo.authorAvatar,
                            backgroundColor = photo.authorColor,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable(enabled = photo.authorId.isNotBlank()) { onAuthorClick(photo.authorId) },
                            textSize = 12.sp
                        )
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

                    AsyncImage(
                        model = photo.imageUrl, contentDescription = photo.description, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                    )

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
                            }
                            Icon(
                                if (photo.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save",
                                tint = if (photo.isSaved) Color(0xFFD97706) else StoneText,
                                modifier = Modifier.size(22.dp).clickable { onSave(photo.id) }
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(text = displayTitle, fontSize = 14.sp, color = StoneText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (bodyText != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(text = bodyText, fontSize = 13.sp, color = StoneText, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }

                        Spacer(Modifier.height(8.dp))
                        PostTags(tags = photo.tags)

                        Spacer(Modifier.height(8.dp))
                        Text(photo.date, fontSize = 10.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PostTags(tags: List<String>) {
    if (tags.isEmpty()) return

    val visibleTags = tags.take(6)
    val hiddenCount = tags.size - visibleTags.size

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        visibleTags.forEach { tag ->
            TagChip(text = "#$tag")
        }
        if (hiddenCount > 0) {
            TagChip(text = "+$hiddenCount")
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = RedPrimary,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .widthIn(max = 112.dp)
            .background(RedLight, RoundedCornerShape(12.dp))
            .border(1.dp, RedPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

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
            val displayTitle = photo.title.ifBlank { photo.location }
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(12.dp)).clickable { onPhotoClick(photo.id) }) {
                AsyncImage(model = photo.imageUrl, contentDescription = displayTitle, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())

                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.8f)))))

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

                Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(displayTitle, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(listOf(photo.location, photo.country).filter { it.isNotBlank() }.distinct().joinToString(" · "), color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

private fun formatDateRangeLabel(startMillis: Long?, endMillis: Long?): String? {
    if (startMillis == null && endMillis == null) return null
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.FRANCE)
    val start = startMillis?.let { formatter.format(Date(it)) }
    val end = endMillis?.let { formatter.format(Date(it)) }
    return when {
        start != null && end != null -> "$start - $end"
        start != null -> "Depuis $start"
        end != null -> "Jusqu'au $end"
        else -> null
    }
}

private fun formatCoordinateLabel(latitude: Double, longitude: Double): String {
    return "%.5f, %.5f".format(Locale.US, latitude, longitude)
}

private data class DateParts(
    val year: Int,
    val month: Int,
    val day: Int
) {
    fun toStartMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day.coerceAtMost(daysInMonth(year, month)))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun minusDays(days: Int): DateParts {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            add(Calendar.DAY_OF_MONTH, -days)
        }.toDateParts()
    }

    companion object {
        fun today(): DateParts = Calendar.getInstance().toDateParts()
    }
}

private fun Long.toDateParts(): DateParts {
    return Calendar.getInstance().apply { timeInMillis = this@toDateParts }.toDateParts()
}

private fun Calendar.toDateParts(): DateParts {
    return DateParts(
        year = get(Calendar.YEAR),
        month = get(Calendar.MONTH) + 1,
        day = get(Calendar.DAY_OF_MONTH)
    )
}

private fun daysInMonth(year: Int, month: Int): Int {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)
}

@Preview(showBackground = true, name = "Mode Connecté")
@Composable
fun GalleryScreenPreview() {
    GalleryScreen(isAnonymous = false)
}

@Preview(showBackground = true, name = "Mode Anonyme")
@Composable
fun GalleryScreenAnonymousPreview() {
    GalleryScreen(isAnonymous = true)
}
