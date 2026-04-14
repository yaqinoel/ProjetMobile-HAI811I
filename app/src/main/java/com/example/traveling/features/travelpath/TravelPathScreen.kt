package com.example.traveling.features.travelpath

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.traveling.data.model.TravelPathData
import com.example.traveling.data.model.TravelRoute
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.saveable.rememberSaveable

// ─── Chinese-style Colors ───
private val RedPrimary = Color(0xFFB91C1C)
private val RedDark = Color(0xFF991B1B)
private val CardBg = Color(0xFFFFFBF5)
private val PageBg = Color(0xFFFDF8F4)
private val AmberAccent = Color(0xFFFBBF24)
private val AmberLight = Color(0xFFFEF3C7)
private val StoneText = Color(0xFF44403C)
private val StoneMuted = Color(0xFF78716C)
private val StoneLighter = Color(0xFFA8A29E)
private val StoneBorder = Color(0x14795548)

@Composable
fun TravelPathScreen(
    isAnonymous: Boolean = false,
    initialDestination: String? = null,
    travelViewModel: TravelViewModel = viewModel()
) {
    val step by travelViewModel.currentStep.collectAsState()
    val selectedRouteId by travelViewModel.currentRouteId.collectAsState()

    when {
        step == "detail" && selectedRouteId != null -> {
            RouteDetailScreen(
                routeId = selectedRouteId!!,
                onBack = {
                    travelViewModel.setCurrentRouteId(null)
                    travelViewModel.setStep("results")
                }
            )
        }
        step == "loading" -> LoadingScreen()
        step == "results" -> ResultsScreen(
            travelViewModel = travelViewModel,
            onBack = { travelViewModel.setStep("preferences") },
            onViewDetail = { id ->
                travelViewModel.selectRoute(id)
                travelViewModel.setCurrentRouteId(id)
                travelViewModel.setStep("detail")
            }
        )
        else -> PreferencesForm(
            initialDestination = initialDestination,
            travelViewModel = travelViewModel,
            onGenerate = {
                travelViewModel.setStep("loading")
            },
            onLoadingComplete = { travelViewModel.setStep("results") }
        )
    }

    // Auto-transition from loading to results
    if (step == "loading") {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1800)
            travelViewModel.setStep("results")
        }
    }
}

// ═════════════════════════════════════════════
//  PREFERENCES FORM
// ═════════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferencesForm(
    initialDestination: String?,
    travelViewModel: TravelViewModel,
    onGenerate: () -> Unit,
    onLoadingComplete: () -> Unit
) {
    val quickCities by travelViewModel.quickCities.collectAsState()
    val citiesList = quickCities.ifEmpty { TravelPathData.defaultQuickCities }

    // ── Form state backed by ViewModel (persists across navigation) ──
    var destination by remember { mutableStateOf(travelViewModel.formDestination.value.ifEmpty { initialDestination ?: "" }) }
    var selectedActivities by remember { mutableStateOf(travelViewModel.formActivities.value) }
    var budget by remember { mutableFloatStateOf(travelViewModel.formBudget.value) }
    var duration by remember { mutableFloatStateOf(travelViewModel.formDuration.value) }
    var effort by remember { mutableFloatStateOf(travelViewModel.formEffort.value) }
    var favoritePlaces by remember { mutableStateOf(travelViewModel.formFavoritePlaces.value) }
    var newPlace by remember { mutableStateOf("") }
    var coldTolerance by remember { mutableStateOf(true) }
    var heatTolerance by remember { mutableStateOf(true) }
    var humidityTolerance by remember { mutableStateOf(false) }

    // Sync form state back to ViewModel whenever values change
    LaunchedEffect(destination) { travelViewModel.formDestination.value = destination }
    LaunchedEffect(selectedActivities) { travelViewModel.formActivities.value = selectedActivities }
    LaunchedEffect(budget) { travelViewModel.formBudget.value = budget }
    LaunchedEffect(duration) { travelViewModel.formDuration.value = duration }
    LaunchedEffect(effort) { travelViewModel.formEffort.value = effort }
    LaunchedEffect(favoritePlaces) { travelViewModel.formFavoritePlaces.value = favoritePlaces }

    val destinationNotFound by travelViewModel.destinationNotFound.collectAsState()

    LaunchedEffect(destination) {
        travelViewModel.updateSuggestedAttractions(destination)
        travelViewModel.checkDestination(destination)
    }
    val suggestedAttractions by travelViewModel.suggestedAttractions.collectAsState()

    val effortLabels = mapOf(
        1 to "Très facile", 2 to "Facile", 3 to "Modéré", 4 to "Élevé", 5 to "Intense"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        // ── Header ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardBg.copy(alpha = 0.9f),
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    "Planification d'Itinéraire",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = StoneText, letterSpacing = 1.sp
                )
                Text(
                    "Personnalisez intelligemment votre itinéraire idéal",
                    fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    color = StoneLighter
                )
            }
        }
        HorizontalDivider(color = Color(0xFFE7E5E4), thickness = 0.5.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Destination ──
            SectionCard {
                Text("Destination", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    placeholder = { Text("Où voulez-vous aller ?", color = StoneLighter) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = RedPrimary) },
                    isError = destinationNotFound && destination.isNotBlank(),
                    supportingText = if (destinationNotFound && destination.isNotBlank()) {
                        { Text("⚠️ Pas de données disponibles pour cette ville", color = Color(0xFFDC2626), fontSize = 11.sp) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = Color.Transparent,
                        unfocusedContainerColor = Color(0xFFFAF5ED),
                        focusedContainerColor = Color.White,
                        errorBorderColor = Color(0xFFDC2626),
                        errorContainerColor = Color(0xFFFEF2F2)
                    ),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(citiesList) { city ->
                        val selected = destination == city
                        Surface(
                            onClick = { destination = city },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) RedPrimary else Color(0xFFF5F5F4),
                            contentColor = if (selected) Color.White else StoneMuted
                        ) {
                            Text(
                                city,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── Activity Preferences ──
            SectionCard {
                Text("Préférences d'Intérêts", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                Spacer(Modifier.height(12.dp))
                // Using a fixed grid since LazyVerticalGrid can't be inside a scrollable column
                val activities = TravelPathData.activities
                for (row in activities.chunked(4)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { activity ->
                            val selected = activity.id in selectedActivities
                            Surface(
                                onClick = {
                                    selectedActivities = if (selected) selectedActivities - activity.id
                                    else selectedActivities + activity.id
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    2.dp,
                                    if (selected) Color(0xFFF87171) else Color.Transparent
                                ),
                                color = if (selected) Color(0xFFFEF2F2) else Color(0xFFF5F5F4)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(activity.icon, fontSize = 20.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        activity.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selected) RedPrimary else StoneMuted
                                    )
                                }
                            }
                        }
                        // Fill remaining space if row is incomplete
                        repeat(4 - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Favorite Places ──
            SectionCard {
                Text("Lieux à Visiter (Optionnel)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPlace,
                        onValueChange = { newPlace = it },
                        placeholder = { Text("Ajouter un lieu...", color = StoneLighter, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = Color(0xFFF5F5F4)
                        ),
                        singleLine = true
                    )
                    Surface(
                        onClick = {
                            if (newPlace.isNotBlank()) {
                                favoritePlaces = favoritePlaces + newPlace.trim()
                                newPlace = ""
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", color = RedPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                // Suggested Attractions Row
                if (suggestedAttractions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Suggestions pour cette ville :", fontSize = 11.sp, color = StoneLighter)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suggestedAttractions) { attr ->
                            val isAdded = favoritePlaces.contains(attr.name)
                            Surface(
                                onClick = {
                                    if (!isAdded) favoritePlaces = favoritePlaces + attr.name
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAdded) Color(0xFFFEF2F2) else Color(0xFFF5F5F4),
                                border = BorderStroke(1.dp, if (isAdded) Color(0xFFFECACA) else Color.Transparent)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(attr.name, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (isAdded) RedPrimary else StoneText)
                                    if (!isAdded) {
                                        Text("+", fontSize = 12.sp, color = StoneMuted)
                                    }
                                }
                            }
                        }
                    }
                }

                if (favoritePlaces.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        favoritePlaces.forEachIndexed { index, place ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFECACA))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(12.dp), tint = RedPrimary)
                                    Text(place, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = RedPrimary)
                                    IconButton(
                                        onClick = { favoritePlaces = favoritePlaces.filterIndexed { i, _ -> i != index } },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Text("×", color = Color(0xFFF87171), fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Budget Slider ──
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Limite de Budget", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                    Text("${budget.toInt()} €", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RedPrimary)
                }
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = budget,
                    onValueChange = { budget = it },
                    valueRange = 0f..2500f,
                    colors = SliderDefaults.colors(
                        thumbColor = RedPrimary,
                        activeTrackColor = RedPrimary,
                        inactiveTrackColor = Color(0xFFE7E5E4)
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0 €", fontSize = 10.sp, color = StoneLighter)
                    Text("2500 €", fontSize = 10.sp, color = StoneLighter)
                }
            }

            // ── Duration Slider ──
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Durée du Tour", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                    Text("${duration.toInt()} heures", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RedPrimary)
                }
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = duration,
                    onValueChange = { duration = it },
                    valueRange = 1f..12f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = RedPrimary,
                        activeTrackColor = RedPrimary,
                        inactiveTrackColor = Color(0xFFE7E5E4)
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("1 heure", fontSize = 10.sp, color = StoneLighter)
                    Text("12 heures", fontSize = 10.sp, color = StoneLighter)
                }
            }

            // ── Effort Slider ──
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Effort Physique", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Text(
                            effortLabels[effort.toInt()] ?: "Modéré",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RedPrimary
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = effort,
                    onValueChange = { effort = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = RedPrimary,
                        activeTrackColor = RedPrimary,
                        inactiveTrackColor = Color(0xFFE7E5E4)
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (1..5).forEach { level ->
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (effort.toInt() >= level) Color(0xFFF87171) else Color(0xFFF5F5F4))
                        )
                    }
                }
            }

            // ── Weather Tolerance ──
            SectionCard {
                Text("Tolérance Météo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeatherToggle(
                        label = "Froid",
                        icon = Icons.Default.AcUnit,
                        selected = coldTolerance,
                        selectedColor = Color(0xFF3B82F6),
                        selectedBg = Color(0xFFEFF6FF),
                        onClick = { coldTolerance = !coldTolerance },
                        modifier = Modifier.weight(1f)
                    )
                    WeatherToggle(
                        label = "Chaleur",
                        icon = Icons.Default.WbSunny,
                        selected = heatTolerance,
                        selectedColor = Color(0xFFF97316),
                        selectedBg = Color(0xFFFFF7ED),
                        onClick = { heatTolerance = !heatTolerance },
                        modifier = Modifier.weight(1f)
                    )
                    WeatherToggle(
                        label = "Humidité",
                        icon = Icons.Default.WaterDrop,
                        selected = humidityTolerance,
                        selectedColor = Color(0xFF06B6D4),
                        selectedBg = Color(0xFFECFEFF),
                        onClick = { humidityTolerance = !humidityTolerance },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Generate Button ──
            Button(
                onClick = {
                    travelViewModel.selectDestination(
                        destinationName = destination,
                        budget = budget.toInt(),
                        activities = selectedActivities,
                        durationHours = duration.toInt(),
                        effort = effort.toInt(),
                        favoritePlaces = favoritePlaces
                    )
                    onGenerate()
                },
                enabled = selectedActivities.isNotEmpty() && destination.isNotBlank() && !destinationNotFound,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color(0xFFD6D3D1)
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (selectedActivities.isNotEmpty() && destination.isNotBlank() && !destinationNotFound)
                                Brush.horizontalGradient(listOf(RedPrimary, RedDark))
                            else Brush.horizontalGradient(listOf(Color(0xFFD6D3D1), Color(0xFFD6D3D1)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text(
                            "Générer Itinéraire Intelligent",
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            color = Color.White, letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp)) // Bottom padding for navigation bar
        }
    }
}

// ═════════════════════════════════════════════
//  LOADING SCREEN
// ═════════════════════════════════════════════
@Composable
private fun LoadingScreen() {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (progress < 1f) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed / 1800f).coerceAtMost(1f)
            kotlinx.coroutines.delay(16)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rotating icon
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFFF59E0B)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Planification Intelligente en Cours...",
            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StoneText
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "L'IA génère le meilleur itinéraire\nselon vos préférences",
            fontSize = 14.sp, color = StoneLighter,
            lineHeight = 20.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .width(192.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = RedPrimary,
            trackColor = Color(0xFFF5F5F4)
        )
    }
}

// ═════════════════════════════════════════════
//  RESULTS SCREEN
// ═════════════════════════════════════════════
@Composable
private fun ResultsScreen(
    travelViewModel: TravelViewModel,
    onBack: () -> Unit,
    onViewDetail: (String) -> Unit
) {
    val routes by travelViewModel.routes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        // ── Header ──
        Surface(color = CardBg.copy(alpha = 0.9f), shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Icon(
                        Icons.Default.ArrowBack, null,
                        modifier = Modifier.padding(8.dp).size(18.dp),
                        tint = StoneMuted
                    )
                }
                Column {
                    Text(
                        "Itinéraires Recommandés",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = StoneText, letterSpacing = 1.sp
                    )
                    Text(
                        "3 routes trouvées pour vous",
                        fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        color = StoneLighter
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recommendation tip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFFECACA))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Star, null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                    Text(
                        "La Route Équilibrée correspond le mieux à vos préférences",
                        fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF991B1B)
                    )
                }
            }

            // Route cards
            routes.forEachIndexed { index, route ->
                RouteCard(
                    route = route,
                    isRecommended = index == 1,
                    onViewDetail = { onViewDetail(route.id) }
                )
            }

            // Offline mode banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AmberLight,
                border = BorderStroke(1.dp, Color(0x80FDE68A))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.WifiOff, null, tint = StoneLighter, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Mode Hors Ligne", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneMuted)
                        Text(
                            "Téléchargez l'itinéraire pour consulter sans Internet",
                            fontSize = 11.sp, color = StoneLighter
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ═════════════════════════════════════════════
//  ROUTE CARD COMPONENT
// ═════════════════════════════════════════════
@Composable
private fun RouteCard(
    route: TravelRoute,
    isRecommended: Boolean,
    onViewDetail: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, StoneBorder)
    ) {
        Column {
            // Image header
            Box(modifier = Modifier.height(144.dp).fillMaxWidth()) {
                AsyncImage(
                    model = route.imageUrl,
                    contentDescription = route.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )
                // Recommended badge
                if (isRecommended) {
                    Surface(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopStart),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF59E0B)
                    ) {
                        Text(
                            "Recommandé",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                    }
                }
                // Rating badge
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = AmberAccent, modifier = Modifier.size(12.dp))
                        Text("${route.rating}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
                // Title
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(route.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(route.subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatChip(
                        icon = Icons.Default.AttachMoney,
                        value = "${route.budget} €",
                        label = "Budget",
                        bgColor = Color(0xFFFEF2F2),
                        iconColor = RedPrimary,
                        borderColor = Color(0xFFFECACA),
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        icon = Icons.Default.Schedule,
                        value = route.duration,
                        label = "Durée",
                        bgColor = AmberLight,
                        iconColor = Color(0xFFB45309),
                        borderColor = Color(0xFFFDE68A),
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        icon = Icons.Outlined.DirectionsWalk,
                        value = "${route.stops}",
                        label = "Arrêts",
                        bgColor = Color(0xFFF5F5F4),
                        iconColor = StoneMuted,
                        borderColor = Color(0xFFE7E5E4),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Highlights timeline
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    route.highlights.take(4).forEachIndexed { i, highlight ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color(0xFFFEE2E2), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${i + 1}",
                                        fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RedPrimary
                                    )
                                }
                                if (i < 3) {
                                    Box(
                                        modifier = Modifier
                                            .width(1.5.dp)
                                            .height(8.dp)
                                            .background(Color(0xFFFEE2E2))
                                    )
                                }
                            }
                            Text(highlight, fontSize = 12.sp, color = StoneMuted)
                        }
                    }
                    if (route.highlights.size > 4) {
                        Text(
                            "+ ${route.highlights.size - 4} arrêts",
                            fontSize = 11.sp, color = StoneLighter,
                            modifier = Modifier.padding(start = 28.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onViewDetail,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(RedPrimary, RedDark))),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Voir Détails", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Surface(
                        onClick = { },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5F5F4)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Download, null, tint = StoneMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════
//  HELPER COMPOSABLES
// ═════════════════════════════════════════════
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, StoneBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    bgColor: Color,
    iconColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StoneText)
            Text(label, fontSize = 10.sp, color = StoneLighter)
        }
    }
}

@Composable
private fun WeatherToggle(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    selectedColor: Color,
    selectedBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, if (selected) selectedColor else Color.Transparent),
        color = if (selected) selectedBg else Color(0xFFF5F5F4)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) selectedColor else StoneLighter
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                color = if (selected) selectedColor else StoneMuted
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TravelPathScreenPreview() {
    TravelPathScreen(
        isAnonymous = false,
        initialDestination = null
    )
}