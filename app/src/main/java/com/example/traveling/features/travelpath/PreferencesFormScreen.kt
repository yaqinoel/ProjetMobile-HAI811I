package com.example.traveling.features.travelpath

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traveling.data.model.TravelPathData
import com.example.traveling.ui.theme.*

//  PREFERENCES FORM
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PreferencesForm(
    initialDestination: String?,
    travelViewModel: TravelViewModel,
    onGenerate: () -> Unit,
    onLoadingComplete: () -> Unit
) {
    val quickCities by travelViewModel.quickCities.collectAsState()
    val citiesList = quickCities.ifEmpty { TravelPathData.defaultQuickCities }

    // -- Form state backed by ViewModel (persists across navigation) --
    var destination by remember { mutableStateOf(travelViewModel.formDestination.value.ifEmpty { initialDestination ?: "" }) }
    var selectedActivities by remember { mutableStateOf(travelViewModel.formActivities.value) }
    var budget by remember { mutableFloatStateOf(travelViewModel.formBudget.value) }
    var duration by remember { mutableFloatStateOf(travelViewModel.formDuration.value) }
    var effort by remember { mutableFloatStateOf(travelViewModel.formEffort.value) }
    var favoritePlaces by remember { mutableStateOf(travelViewModel.formFavoritePlaces.value) }
    var newPlace by remember { mutableStateOf("") }
    var avoidRain by remember { mutableStateOf(travelViewModel.formAvoidRain.value) }
    var avoidHeat by remember { mutableStateOf(travelViewModel.formAvoidHeat.value) }
    var avoidCold by remember { mutableStateOf(travelViewModel.formAvoidCold.value) }

    // Sync form state back to ViewModel whenever values change
    LaunchedEffect(destination) { travelViewModel.formDestination.value = destination }
    LaunchedEffect(selectedActivities) { travelViewModel.formActivities.value = selectedActivities }
    LaunchedEffect(budget) { travelViewModel.formBudget.value = budget }
    LaunchedEffect(duration) { travelViewModel.formDuration.value = duration }
    LaunchedEffect(effort) { travelViewModel.formEffort.value = effort }
    LaunchedEffect(favoritePlaces) { travelViewModel.formFavoritePlaces.value = favoritePlaces }
    LaunchedEffect(avoidRain) { travelViewModel.formAvoidRain.value = avoidRain }
    LaunchedEffect(avoidHeat) { travelViewModel.formAvoidHeat.value = avoidHeat }
    LaunchedEffect(avoidCold) { travelViewModel.formAvoidCold.value = avoidCold }

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
        // -- Header --
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardBg.copy(alpha = 0.9f),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    "Planification d'itinéraire",
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
            // -- Destination --
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
                        { Text("Pas de données disponibles pour cette ville!", color = Color(0xFFDC2626), fontSize = 11.sp) }
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

            // -- Activity Preferences --
            SectionCard {
                Text("Préférences d'intérêts", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                Spacer(Modifier.height(12.dp))
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
                        repeat(4 - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // -- Favorite Places --
            SectionCard {
                Text("Lieux à visiter (optionnel)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
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

            // -- Budget Slider --
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Limite de Budget", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                    Text("${budget.toInt()} EUR", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RedPrimary)
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
                    Text("0 EUR", fontSize = 10.sp, color = StoneLighter)
                    Text("2500 EUR", fontSize = 10.sp, color = StoneLighter)
                }
            }

            // -- Duration Slider --
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Durée du tour", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
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

            // -- Effort Slider --
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

            // -- Weather Tolerance --
            SectionCard {
                Text("Meteo & Confort", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeatherToggle(
                        label = "Eviter pluie",
                        icon = Icons.Default.WaterDrop,
                        selected = avoidRain,
                        selectedColor = Color(0xFF06B6D4),
                        selectedBg = Color(0xFFECFEFF),
                        onClick = { avoidRain = !avoidRain },
                        modifier = Modifier.weight(1f)
                    )
                    WeatherToggle(
                        label = "Eviter chaud",
                        icon = Icons.Default.WbSunny,
                        selected = avoidHeat,
                        selectedColor = Color(0xFFF97316),
                        selectedBg = Color(0xFFFFF7ED),
                        onClick = { avoidHeat = !avoidHeat },
                        modifier = Modifier.weight(1f)
                    )
                    WeatherToggle(
                        label = "Eviter froid",
                        icon = Icons.Default.AcUnit,
                        selected = avoidCold,
                        selectedColor = Color(0xFF3B82F6),
                        selectedBg = Color(0xFFEFF6FF),
                        onClick = { avoidCold = !avoidCold },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // -- Generate Button --
            Button(
                onClick = {
                    travelViewModel.selectDestination(
                        destinationName = destination,
                        budget = budget.toInt(),
                        activities = selectedActivities,
                        durationHours = duration.toInt(),
                        effort = effort.toInt(),
                        favoritePlaces = favoritePlaces,
                        avoidRain = avoidRain,
                        avoidHeat = avoidHeat,
                        avoidCold = avoidCold
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
                            "Générer un itinéraire intelligent",
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
