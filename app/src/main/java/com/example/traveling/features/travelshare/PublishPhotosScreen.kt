package com.example.traveling.features.travelshare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.traveling.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishPhotosScreen(
    onBack: () -> Unit = {},
    onPublish: () -> Unit = {},
    onOpenMapPicker: () -> Unit = {} // Inject Map API caller
) {
    // États Obligatoires (Required)
    var title by remember { mutableStateOf("") }
    val mockPhotos = listOf("https://images.unsplash.com/photo-1603120527222-33f28c2ce89e?w=400&fit=crop")
    // Defaulting to current GPS location.
    var locationName by remember { mutableStateOf("Montpellier, France (Position Actuelle)") }

    // États Optionnels (Optional)
    var description by remember { mutableStateOf("") }
    var isLinkedToPath by remember { mutableStateOf(true) }

    // TravelPath Parameters
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var expense by remember { mutableStateOf("") }
    var duration by remember { mutableFloatStateOf(0f) } // 0 = Not specified
    var effort by remember { mutableFloatStateOf(0f) }   // 0 = Not specified

    // Weather Tolerance
    var coldTolerance by remember { mutableStateOf(false) }
    var heatTolerance by remember { mutableStateOf(false) }
    var humidityTolerance by remember { mutableStateOf(false) }

    val effortLabels = mapOf(1 to "Très facile", 2 to "Facile", 3 to "Modéré", 4 to "Élevé", 5 to "Intense")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .systemBarsPadding()
    ) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Annuler", tint = Stone800) }
            Text("Créer une étape", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Stone800)
            TextButton(
                onClick = onPublish,
                enabled = title.isNotBlank() && mockPhotos.isNotEmpty() && locationName.isNotBlank()
            ) {
                Text("Publier", color = if (title.isNotBlank()) RedPrimary else Stone400, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // PHOTOS (OBLIGATOIRE)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                        .clickable { /* Ouvrir galerie */ },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, "Ajouter", tint = RedPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("Photos", fontSize = 10.sp, color = RedPrimary, fontWeight = FontWeight.Medium)
                    }
                }
                mockPhotos.forEach { url ->
                    Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))) {
                        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
            }

            // INFORMATIONS DE BASE
            Column(
                modifier = Modifier
                    .background(CardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, StoneBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                // Titre (OBLIGATOIRE)
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(color = Stone800, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Titre de l'étape", color = Stone400, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(" *", color = RedPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        innerTextField()
                    }
                )

                HorizontalDivider(color = Stone300.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 12.dp))

                // Description (Optionnel)
                BasicTextField(
                    value = description,
                    onValueChange = { description = it },
                    textStyle = TextStyle(color = Stone600, fontSize = 14.sp, lineHeight = 22.sp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    decorationBox = { innerTextField ->
                        if (description.isEmpty()) Text("Racontez ce moment... L'IA peut vous aider à résumer !", color = Stone300, fontSize = 14.sp)
                        innerTextField()
                    }
                )

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                        Box(modifier = Modifier.size(28.dp).background(Color(0xFFFEF2F2), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Mic, "Audio", tint = RedPrimary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Note vocale", color = RedPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                        Icon(Icons.Default.AutoAwesome, "IA", tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Générer via IA", color = Color(0xFFD97706), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // GÉOLOCALISATION (OBLIGATOIRE) & TRAVELPATH
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                Text("Localisation & Itinéraire", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone800)

                // Lieu GPS avec Google Maps Picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, if(locationName.isBlank()) Color(0xFFFCA5A5) else StoneBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                        .clickable { onOpenMapPicker() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.LocationOn, null, tint = RedPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(if(locationName.isNotBlank()) locationName else "Sélectionner un lieu *", fontSize = 14.sp, color = if(locationName.isNotBlank()) Stone800 else RedPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                    Icon(Icons.Default.Map, "Ouvrir la carte", tint = Stone400)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isLinkedToPath) Color(0xFFFEF2F2) else CardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, if (isLinkedToPath) Color(0xFFFCA5A5) else StoneBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).background(if (isLinkedToPath) RedPrimary else Stone100, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Route, null, tint = if (isLinkedToPath) Color.White else Stone500, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Ajouter au TravelPath", fontSize = 14.sp, color = Stone800, fontWeight = FontWeight.SemiBold)
                            if (isLinkedToPath) Text("Jour 2 · Voyage en Chine 2026", fontSize = 11.sp, color = RedPrimary)
                        }
                    }
                    Switch(
                        checked = isLinkedToPath,
                        onCheckedChange = { isLinkedToPath = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RedPrimary, uncheckedTrackColor = Stone300, uncheckedThumbColor = Color.White)
                    )
                }
            }

            // INFOS PRATIQUES
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                Text("Paramètres d'itinéraire (Optionnel)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone800)

                // Type d'activité
                SectionCard {
                    Text("Type d'activité", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val types = listOf(
                            Triple("Culture", Icons.Default.AccountBalance, "Culture"),
                            Triple("Nature", Icons.Default.Park, "Nature"),
                            Triple("Food", Icons.Default.Restaurant, "Food"),
                            Triple("Aventure", Icons.Default.Explore, "Aventure")
                        )
                        types.forEach { (id, icon, label) ->
                            val selected = selectedCategory == id
                            Surface(
                                onClick = { selectedCategory = if (selected) null else id },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(2.dp, if (selected) Color(0xFFF87171) else Color.Transparent),
                                color = if (selected) Color(0xFFFEF2F2) else Color(0xFFF5F5F4)
                            ) {
                                Column(modifier = Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(icon, null, tint = if (selected) RedPrimary else StoneMuted, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (selected) RedPrimary else StoneMuted)
                                }
                            }
                        }
                    }
                }

                // Budget (Style TravelPath TextField)
                SectionCard {
                    Text("Budget dépensé", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = expense,
                        onValueChange = { expense = it },
                        placeholder = { Text("Ex: 45", color = StoneLighter, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Euro, null, tint = RedPrimary) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = Color(0xFFF5F5F4),
                            focusedContainerColor = Color.White
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Durée Slider
                SectionCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Durée estimée", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                        if (duration > 0f) Text("${duration.toInt()} heures", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RedPrimary)
                        else Text("Non spécifié", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                    }
                    Spacer(Modifier.height(16.dp))
                    Slider(
                        value = duration,
                        onValueChange = { duration = it },
                        valueRange = 0f..12f,
                        steps = 11,
                        colors = SliderDefaults.colors(thumbColor = RedPrimary, activeTrackColor = RedPrimary, inactiveTrackColor = Color(0xFFE7E5E4))
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0 heure", fontSize = 10.sp, color = StoneLighter)
                        Text("12 heures", fontSize = 10.sp, color = StoneLighter)
                    }
                }

                // Effort Slider
                SectionCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Effort Physique", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                        if (effort > 0f) {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFEF2F2), border = BorderStroke(1.dp, Color(0xFFFECACA))) {
                                Text(effortLabels[effort.toInt()] ?: "Modéré", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RedPrimary)
                            }
                        } else {
                            Text("Non spécifié", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Slider(
                        value = effort,
                        onValueChange = { effort = it },
                        valueRange = 0f..5f,
                        steps = 4,
                        colors = SliderDefaults.colors(thumbColor = RedPrimary, activeTrackColor = RedPrimary, inactiveTrackColor = Color(0xFFE7E5E4))
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

                // Weather Tolerance
                SectionCard {
                    Text("Tolérance Météo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeatherToggle("Froid", Icons.Default.AcUnit, coldTolerance, Color(0xFF3B82F6), Color(0xFFEFF6FF), { coldTolerance = !coldTolerance }, Modifier.weight(1f))
                        WeatherToggle("Chaleur", Icons.Default.WbSunny, heatTolerance, Color(0xFFF97316), Color(0xFFFFF7ED), { heatTolerance = !heatTolerance }, Modifier.weight(1f))
                        WeatherToggle("Humidité", Icons.Default.WaterDrop, humidityTolerance, Color(0xFF06B6D4), Color(0xFFECFEFF), { humidityTolerance = !humidityTolerance }, Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// --- Helper Composables (from TravelPathScreen) ---
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        shadowElevation = 1.dp, // Match TravelPath
        border = BorderStroke(1.dp, StoneBorder) // Match TravelPath
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun WeatherToggle(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean,
    selectedColor: Color, selectedBg: Color, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, if (selected) selectedColor else Color.Transparent),
        color = if (selected) selectedBg else Color(0xFFF5F5F4) // Match TravelPath
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = if (selected) selectedColor else StoneLighter)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (selected) selectedColor else StoneMuted)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PublishPhotosScreenPreview() {
    PublishPhotosScreen()
}