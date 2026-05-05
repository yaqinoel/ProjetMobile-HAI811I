package com.example.traveling.features.travelshare

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.traveling.features.travelshare.model.PhotoPostUi
import com.example.traveling.ui.theme.*
import kotlinx.coroutines.launch

private const val MAX_SELECTED_PHOTOS = 14

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishPhotosScreen(
    onBack: () -> Unit = {},
    onPublish: (PhotoPostUi) -> Unit = {},
    onOpenMapPicker: () -> Unit = {} // Inject Map API caller
) {
    // États Obligatoires (Required)
    var title by remember { mutableStateOf("") }
    var selectedPhotos by remember { mutableStateOf(emptyList<String>()) }
    // Defaulting to current GPS location.
    var locationName by remember { mutableStateOf("Montpellier, France (Position Actuelle)") }
    var locationPrecision by remember { mutableStateOf("exact") }
    var visibility by remember { mutableStateOf("public") }
    var selectedGroup by remember { mutableStateOf("Voyageurs de Chine") }

    // États Optionnels (Optional)
    var description by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(emptyList<String>()) }
    var voiceNoteAdded by remember { mutableStateOf(false) }
    var aiAnnotationEnabled by remember { mutableStateOf(false) }
    var isLinkedToPath by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var publishPreview by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_SELECTED_PHOTOS)
    ) { uris ->
        val remainingSlots = MAX_SELECTED_PHOTOS - selectedPhotos.size
        val pickedPhotos = uris.take(remainingSlots).map { it.toString() }
        selectedPhotos = (selectedPhotos + pickedPhotos).take(MAX_SELECTED_PHOTOS)
        val message = when {
            uris.isEmpty() -> "Aucune photo sélectionnée."
            uris.size > remainingSlots -> "Limite de $MAX_SELECTED_PHOTOS photos atteinte."
            else -> "${pickedPhotos.size} photo${if (pickedPhotos.size > 1) "s" else ""} ajoutée${if (pickedPhotos.size > 1) "s" else ""}."
        }
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

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
    val addTag = {
        val newTags = tagInput
            .split(",", "，")
            .map { it.trim().trimStart('#') }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .filter { candidate ->
                selectedTags.none { it.equals(candidate, ignoreCase = true) }
            }
        if (newTags.isNotEmpty()) {
            selectedTags = selectedTags + newTags
        }
        tagInput = ""
    }

    Box(modifier = Modifier.fillMaxSize().background(PageBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Annuler", tint = Stone800) }
            Text("Publier une photo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Stone800)
            TextButton(
                onClick = {
                    publishPreview = true
                },
                enabled = title.isNotBlank() && selectedPhotos.isNotEmpty() && locationName.isNotBlank()
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Photos *", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone800)
                    Text("${selectedPhotos.size}/$MAX_SELECTED_PHOTOS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selectedPhotos.size == MAX_SELECTED_PHOTOS) RedPrimary else Stone400)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedPhotos.size < MAX_SELECTED_PHOTOS) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddPhotoAlternate, "Ajouter", tint = RedPrimary)
                                Spacer(Modifier.height(4.dp))
                                Text("Ajouter", fontSize = 10.sp, color = RedPrimary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    selectedPhotos.forEachIndexed { index, url ->
                        Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                    .clickable {
                                        selectedPhotos = selectedPhotos.filterIndexed { photoIndex, _ -> photoIndex != index }
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Photo retirée.") }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Retirer", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("${index + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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
                                Text("Titre de la photo", color = Stone400, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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

                TagsEditor(
                    input = tagInput,
                    tags = selectedTags,
                    onInputChange = { tagInput = it },
                    onAddTag = addTag,
                    onRemoveTag = { tag -> selectedTags = selectedTags - tag },
                    modifier = Modifier.padding(top = 12.dp)
                )

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { voiceNoteAdded = !voiceNoteAdded }) {
                        Box(modifier = Modifier.size(28.dp).background(Color(0xFFFEF2F2), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Mic, "Audio", tint = RedPrimary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (voiceNoteAdded) "Audio ajouté" else "Note vocale", color = RedPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { aiAnnotationEnabled = !aiAnnotationEnabled }) {
                        Icon(Icons.Default.AutoAwesome, "IA", tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (aiAnnotationEnabled) "IA activée" else "Générer via IA", color = Color(0xFFD97706), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // PARTAGE PUBLIC / GROUPE
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                Text("Partage", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone800)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ShareTargetButton(
                        selected = visibility == "public",
                        icon = Icons.Default.Public,
                        title = "Public",
                        subtitle = "Visible par tous",
                        modifier = Modifier.weight(1f),
                        onClick = { visibility = "public" }
                    )
                    ShareTargetButton(
                        selected = visibility == "group",
                        icon = Icons.Default.Group,
                        title = "Groupe",
                        subtitle = selectedGroup,
                        modifier = Modifier.weight(1f),
                        onClick = { visibility = "group" }
                    )
                }
                if (visibility == "group") {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Voyageurs de Chine", "Route de la Soie 2026", "Photo Paysages").forEach { group ->
                            AssistChip(
                                onClick = { selectedGroup = group },
                                label = { Text(group) },
                                leadingIcon = {
                                    if (selectedGroup == group) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }

            // GÉOLOCALISATION (OBLIGATOIRE) & TRAVELPATH
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                Text("Localisation", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone800)

                // Lieu GPS avec Google Maps Picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, if(locationName.isBlank()) Color(0xFFFCA5A5) else StoneBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                        .clickable {
                            showMapPicker = true
                            onOpenMapPicker()
                        },
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PrecisionChip("Exacte", "exact", locationPrecision, { locationPrecision = it }, Modifier.weight(1f))
                    PrecisionChip("Approximative", "approx", locationPrecision, { locationPrecision = it }, Modifier.weight(1f))
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
                            Text(if (isLinkedToPath) "Créer aussi une étape exploitable par TravelPath" else "Optionnel : enrichir la future génération de parcours", fontSize = 11.sp, color = if (isLinkedToPath) RedPrimary else Stone400)
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
            if (isLinkedToPath) Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
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
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (showMapPicker) {
        ModalBottomSheet(
            onDismissRequest = { showMapPicker = false },
            containerColor = CardBg
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
                Text("Choisir un lieu", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Stone800)
                Spacer(Modifier.height(12.dp))
                listOf(
                    "Montpellier, France (Position Actuelle)",
                    "Grande Muraille, Pékin",
                    "Cité Interdite, Pékin",
                    "Paysages de Guilin"
                ).forEach { place ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            locationName = place
                            showMapPicker = false
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, null, tint = RedPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(place, fontSize = 14.sp, color = Stone800)
                    }
                }
            }
        }
    }

    if (publishPreview) {
        ModalBottomSheet(
            onDismissRequest = { publishPreview = false },
            containerColor = CardBg
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
                Text("Résumé de publication", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Stone800)
                Spacer(Modifier.height(8.dp))
                Text("Vérifiez les informations avant publication.", fontSize = 13.sp, color = Stone500)
                Spacer(Modifier.height(16.dp))
                InfoLine("Titre", title)
                InfoLine("Visibilité", if (visibility == "public") "Public" else "Groupe : $selectedGroup")
                InfoLine("Lieu", "$locationName (${if (locationPrecision == "exact") "exact" else "approximatif"})")
                InfoLine("Photos", "${selectedPhotos.size}")
                InfoLine("Tags", selectedTags.ifEmpty { listOf("Voyage") }.joinToString(", ") { "#$it" })
                InfoLine("TravelPath", if (isLinkedToPath) "Étape TravelPath" else "Non lié")
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        publishPreview = false
                        onPublish(
                            PhotoPostUi(
                                id = "local-${System.currentTimeMillis()}",
                                imageUrl = selectedPhotos.first(),
                                location = locationName.substringBefore(","),
                                country = locationName.substringAfter(",", "France").trim(),
                                date = "À l'instant",
                                author = "Vous",
                                authorAvatar = "V",
                                authorColor = RedPrimary,
                                likes = 0,
                                isLiked = false,
                                isSaved = false,
                                description = description.ifBlank { title },
                                comments = 0,
                                tags = selectedTags.ifEmpty { listOf("Voyage") },
                                placeType = selectedCategory?.lowercase() ?: "street",
                                period = "week"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Publier")
                }
            }
        }
    }
}

// --- Helper Composables (from TravelPathScreen) ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsEditor(
    input: String,
    tags: List<String>,
    onInputChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text("Ajouter un tag : nature, musée, rue...", color = StoneLighter, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Tag, null, tint = RedPrimary) },
            trailingIcon = {
                IconButton(
                    onClick = onAddTag,
                    enabled = input.trim().trimStart('#').isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Ajouter le tag",
                        tint = if (input.trim().trimStart('#').isNotBlank()) RedPrimary else Stone300
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RedPrimary,
                unfocusedBorderColor = StoneBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF5F5F4)
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAddTag() })
        )

        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                tags.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#$tag", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RedPrimary)
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Supprimer le tag",
                                tint = RedPrimary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable { onRemoveTag(tag) }
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareTargetButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, if (selected) RedPrimary.copy(alpha = 0.7f) else StoneBorder),
        color = if (selected) Color(0xFFFEF2F2) else CardBg
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (selected) RedPrimary else Stone500, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontSize = 13.sp, color = Stone800, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 10.sp, color = Stone400, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PrecisionChip(
    label: String,
    value: String,
    selectedValue: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = selectedValue == value
    Surface(
        onClick = { onSelected(value) },
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color(0xFFFEF2F2) else Stone100,
        border = BorderStroke(1.dp, if (selected) RedPrimary.copy(alpha = 0.4f) else Color.Transparent)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked, null, tint = if (selected) RedPrimary else Stone400, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (selected) RedPrimary else Stone500)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Stone400)
        Text(value, fontSize = 12.sp, color = Stone800, fontWeight = FontWeight.SemiBold)
    }
}

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
