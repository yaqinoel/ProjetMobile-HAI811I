package com.example.traveling.features.travelshare

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traveling.data.model.PhotoPostDocument
import com.example.traveling.data.model.UserJoinedGroupDocument
import com.example.traveling.data.repository.PlaceSearchRepository
import com.example.traveling.features.travelshare.MapLocationPickerOverlay
import com.example.traveling.features.travelshare.SelectedLocationUi
import coil.compose.AsyncImage
import com.example.traveling.ui.theme.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

private const val MAX_SELECTED_PHOTOS = 14

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishPhotosScreen(
    editPostId: String? = null,
    onBack: () -> Unit = {},
    onPublishSuccess: (String) -> Unit = {},
    onOpenMapPicker: () -> Unit = {}
) {
    val context = LocalContext.current
    val isEditMode = !editPostId.isNullOrBlank()

    var title by remember { mutableStateOf("") }
    var selectedPhotos by remember { mutableStateOf(emptyList<String>()) }
    var selectedLocation by remember { mutableStateOf<SelectedLocationUi?>(null) }
    var visibility by remember { mutableStateOf("public") }
    var selectedGroup by remember { mutableStateOf<UserJoinedGroupDocument?>(null) }
    var pendingGroupId by remember { mutableStateOf<String?>(null) }

    var description by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(emptyList<String>()) }
    var voiceNoteUri by remember { mutableStateOf<String?>(null) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var isPlayingVoice by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isLinkedToPath by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var publishPreview by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val placeSearchRepository = remember(context) { PlaceSearchRepository(context) }
    val publishViewModel: PublishPhotosViewModel = viewModel()
    val publishState by publishViewModel.uiState.collectAsState()
    val aiAnnotationState by publishViewModel.aiAnnotationState.collectAsState()
    val joinedGroups by publishViewModel.joinedGroups.collectAsState()

    fun stopVoicePlayback() {
        mediaPlayer?.runCatchingStopAndRelease()
        mediaPlayer = null
        isPlayingVoice = false
    }

    fun stopVoiceRecording() {
        val recorder = mediaRecorder ?: return
        val stopped = runCatching { recorder.stop() }.isSuccess
        recorder.release()
        mediaRecorder = null
        isRecordingVoice = false
        if (!stopped) {
            voiceNoteUri = null
            coroutineScope.launch { snackbarHostState.showSnackbar("Enregistrement trop court.") }
        }
    }

    fun startVoiceRecording() {
        stopVoicePlayback()
        val outputFile = File(context.cacheDir, "travelshare_voice_${System.currentTimeMillis()}.m4a")
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        runCatching {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(44100)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setOutputFile(outputFile.absolutePath)
            recorder.prepare()
            recorder.start()
        }.onSuccess {
            mediaRecorder = recorder
            voiceNoteUri = Uri.fromFile(outputFile).toString()
            isRecordingVoice = true
        }.onFailure {
            recorder.release()
            coroutineScope.launch { snackbarHostState.showSnackbar("Impossible de démarrer l'enregistrement.") }
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceRecording()
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar("Permission micro refusée.") }
        }
    }

    fun setCurrentLocationAsDefault() {
        val location = context.lastKnownLocationOrNull()
        if (location == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Position actuelle indisponible. Sélectionnez un lieu sur la carte.") }
            return
        }

        coroutineScope.launch {
            val latLng = LatLng(location.latitude, location.longitude)
            val candidate = placeSearchRepository.reverseLookup(latLng)
                .getOrNull()
                ?.firstOrNull()
                ?: placeSearchRepository.defaultCandidate(latLng)
            selectedLocation = buildSelectedLocation(
                name = candidate.name,
                rawLatitude = candidate.latitude,
                rawLongitude = candidate.longitude,
                precision = "exact",
                address = candidate.address,
                city = candidate.city,
                country = candidate.country,
                googlePlaceId = candidate.googlePlaceId,
                source = "current_location"
            )
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            setCurrentLocationAsDefault()
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar("Permission localisation refusée.") }
        }
    }

    fun requestCurrentLocationIfNeeded() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            setCurrentLocationAsDefault()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun requestOrStartVoiceRecording() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            startVoiceRecording()
        } else {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun validateBeforePreview(): Boolean {
        val message = when {
            title.isBlank() -> "Ajoutez un titre avant de publier."
            selectedPhotos.isEmpty() -> "Ajoutez au moins une photo."
            selectedLocation == null -> "Sélectionnez un lieu."
            visibility == "group" && selectedGroup == null -> "Sélectionnez un groupe."
            isRecordingVoice -> "Arrêtez l'enregistrement vocal avant de publier."
            else -> null
        }
        if (message != null) {
            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
            return false
        }
        return true
    }

    fun toggleVoicePlayback() {
        val uri = voiceNoteUri ?: return
        if (isPlayingVoice) {
            stopVoicePlayback()
            return
        }
        stopVoiceRecording()
        val player = MediaPlayer()
        runCatching {
            player.setDataSource(context, Uri.parse(uri))
            player.setOnCompletionListener {
                it.release()
                if (mediaPlayer === it) {
                    mediaPlayer = null
                    isPlayingVoice = false
                }
            }
            player.prepare()
            player.start()
        }.onSuccess {
            mediaPlayer = player
            isPlayingVoice = true
        }.onFailure {
            player.release()
            coroutineScope.launch { snackbarHostState.showSnackbar("Lecture audio impossible.") }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.runCatchingStopAndRelease()
            mediaPlayer?.runCatchingStopAndRelease()
        }
    }

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

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var expense by remember { mutableStateOf("") }
    var duration by remember { mutableFloatStateOf(0f) }
    var effort by remember { mutableFloatStateOf(0f) }
    var openHours by remember { mutableStateOf("") }
    var closedDay by remember { mutableStateOf("") }
    var weatherType by remember { mutableStateOf("both") }
    var bestTimeSlots by remember { mutableStateOf(setOf("apres-midi")) }
    var editContentLoaded by remember(editPostId) { mutableStateOf(!isEditMode) }

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
    val mergeTags: (List<String>) -> Int = { tags ->
        val normalizedTags = tags
            .map { it.trim().trimStart('#') }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .filter { candidate ->
                selectedTags.none { it.equals(candidate, ignoreCase = true) }
            }
        selectedTags = selectedTags + normalizedTags
        normalizedTags.size
    }

    LaunchedEffect(publishState) {
        when (val state = publishState) {
            is PublishUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                publishViewModel.resetState()
            }
            is PublishUiState.Success -> {
                snackbarHostState.showSnackbar(if (isEditMode) "Publication modifiée" else "Publication réussie")
                publishViewModel.resetState()
                onPublishSuccess(state.postId)
            }
            else -> Unit
        }
    }

    LaunchedEffect(aiAnnotationState) {
        when (val state = aiAnnotationState) {
            is AiAnnotationUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                publishViewModel.resetAiAnnotationState()
            }
            AiAnnotationUiState.Loading -> Unit
            is AiAnnotationUiState.Success -> {
                val addedCount = mergeTags(state.tags)
                snackbarHostState.showSnackbar(
                    if (addedCount > 0) {
                        "$addedCount tag${if (addedCount > 1) "s" else ""} IA ajouté${if (addedCount > 1) "s" else ""}."
                    } else {
                        "Les tags IA sont déjà présents."
                    }
                )
                publishViewModel.resetAiAnnotationState()
            }
            AiAnnotationUiState.Idle -> Unit
        }
    }

    LaunchedEffect(Unit) {
        publishViewModel.loadJoinedGroups()
        if (!isEditMode) {
            requestCurrentLocationIfNeeded()
        }
    }

    LaunchedEffect(editPostId) {
        val postId = editPostId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        publishViewModel.loadPostForEdit(postId)
            .onSuccess { post ->
                val normalizedVisibility = post.visibility.lowercase().ifBlank { "public" }
                val travelShareAttraction = publishViewModel.loadTravelShareAttractionForEdit(postId).getOrNull()
                val hasTravelPathLink = post.isLinkedToTravelPath ||
                    !post.travelPathDestinationId.isNullOrBlank() ||
                    !post.travelPathPlaceId.isNullOrBlank() ||
                    travelShareAttraction?.status == "active"

                title = post.title
                selectedPhotos = post.imageUrls
                selectedLocation = post.toSelectedLocationUi()
                visibility = normalizedVisibility
                pendingGroupId = post.groupId
                description = post.description
                selectedTags = post.tags
                voiceNoteUri = post.voiceNoteUrl
                isLinkedToPath = normalizedVisibility == "public" && hasTravelPathLink
                selectedCategory = post.placeType.ifBlank { null }
                travelShareAttraction?.let { attraction ->
                    expense = attraction.cost.takeIf { it > 0 }?.toString().orEmpty()
                    duration = (attraction.duration / 60f).takeIf { it > 0f } ?: 0f
                    effort = attraction.effortLevel.takeIf { it in 1..5 }?.toFloat() ?: 0f
                    openHours = attraction.openHours.takeUnless { it == "Horaires non renseignés" }.orEmpty()
                    closedDay = attraction.closedDay
                    weatherType = attraction.weatherType.ifBlank { "both" }
                    bestTimeSlots = attraction.bestTimeSlots.toSet().ifEmpty { setOf("apres-midi") }
                }
                editContentLoaded = true
            }
            .onFailure { error ->
                editContentLoaded = true
                snackbarHostState.showSnackbar(error.localizedMessage ?: "Impossible de charger la publication")
            }
    }

    LaunchedEffect(joinedGroups, pendingGroupId) {
        val groupId = pendingGroupId ?: return@LaunchedEffect
        selectedGroup = joinedGroups.firstOrNull { it.groupId == groupId }
    }

    LaunchedEffect(visibility) {
        if (visibility != "public") {
            isLinkedToPath = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(PageBg)) {
        if (!editContentLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RedPrimary)
            }
            return@Box
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Annuler", tint = Stone800) }
            Text(
                if (isEditMode) "Modifier la publication" else "Publier une photo",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Stone800
            )
            TextButton(
                onClick = {
                    if (validateBeforePreview()) {
                        publishPreview = true
                    }
                },
                enabled = publishState !is PublishUiState.Uploading
            ) {
                val uploading = publishState is PublishUiState.Uploading
                Text(
                    if (uploading) {
                        if (isEditMode) "Enregistrement..." else "Publication..."
                    } else {
                        if (isEditMode) "Enregistrer" else "Publier"
                    },
                    color = if (uploading) Stone400 else RedPrimary,
                    fontWeight = FontWeight.Bold
                )
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

            Column(
                modifier = Modifier
                    .background(CardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, StoneBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {

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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    VoiceNoteRecorder(
                        hasVoiceNote = voiceNoteUri != null,
                        isRecording = isRecordingVoice,
                        isPlaying = isPlayingVoice,
                        onRecord = { requestOrStartVoiceRecording() },
                        onStopRecording = { stopVoiceRecording() },
                        onPlayPause = { toggleVoicePlayback() },
                        onDelete = {
                            stopVoicePlayback()
                            stopVoiceRecording()
                            voiceNoteUri?.let { uri ->
                                Uri.parse(uri).path?.let { path ->
                                    runCatching { File(path).delete() }
                                }
                            }
                            voiceNoteUri = null
                        }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(enabled = aiAnnotationState !is AiAnnotationUiState.Loading) {
                            publishViewModel.annotateSelectedImages(context, selectedPhotos)
                        }
                    ) {
                        if (aiAnnotationState is AiAnnotationUiState.Loading) {
                            CircularProgressIndicator(
                                color = Color(0xFFD97706),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, "IA", tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (aiAnnotationState is AiAnnotationUiState.Loading) "Analyse IA..." else "Générer via IA",
                            color = Color(0xFFD97706),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

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
                        subtitle = selectedGroup?.name ?: "Choisir un groupe",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            visibility = "group"
                            isLinkedToPath = false
                        }
                    )
                }
                if (visibility == "group") {
                    if (joinedGroups.isEmpty()) {
                        Text(
                            "Vous n'avez rejoint aucun groupe. Créez ou rejoignez un groupe d'abord.",
                            color = Stone400,
                            fontSize = 12.sp
                        )
                    } else {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            joinedGroups.forEach { group ->
                                AssistChip(
                                    onClick = { selectedGroup = group },
                                    label = { Text(group.name) },
                                    leadingIcon = {
                                        if (selectedGroup?.groupId == group.groupId) {
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                Text("Localisation", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone800)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, if(selectedLocation == null) Color(0xFFFCA5A5) else StoneBorder, RoundedCornerShape(12.dp))
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
                            Text(
                                selectedLocation?.name ?: "Sélectionner un lieu *",
                                fontSize = 14.sp,
                                color = if (selectedLocation != null) Stone800 else RedPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            selectedLocation?.let {
                                val mode = if (it.precision == "approx") "Zone approximative" else "Position exacte"
                                it.address?.let { address ->
                                    Text(
                                        address,
                                        fontSize = 11.sp,
                                        color = Stone400,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    "$mode · ${"%.5f".format(Locale.US, it.displayLatitude)}, ${"%.5f".format(Locale.US, it.displayLongitude)}",
                                    fontSize = 11.sp,
                                    color = Stone400
                                )
                            }
                        }
                    }
                    Icon(Icons.Default.Map, "Ouvrir la carte", tint = Stone400)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isLinkedToPath) Color(0xFFFEF2F2) else CardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, if (isLinkedToPath) Color(0xFFFCA5A5) else StoneBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(32.dp).background(if (isLinkedToPath) RedPrimary else Stone100, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Route, null, tint = if (isLinkedToPath) Color.White else Stone500, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ajouter au TravelPath", fontSize = 14.sp, color = Stone800, fontWeight = FontWeight.SemiBold)
                            Text(
                                when {
                                    visibility != "public" -> "Disponible uniquement pour les publications publiques"
                                    isLinkedToPath -> "Créer aussi une étape exploitable par TravelPath"
                                    else -> "Optionnel : enrichir la future génération de parcours"
                                },
                                fontSize = 11.sp,
                                color = if (isLinkedToPath) RedPrimary else Stone400,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Switch(
                        checked = isLinkedToPath,
                        onCheckedChange = { checked ->
                            isLinkedToPath = checked && visibility == "public"
                        },
                        enabled = visibility == "public",
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RedPrimary, uncheckedTrackColor = Stone300, uncheckedThumbColor = Color.White)
                    )
                }
            }

            if (isLinkedToPath) Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                Text("Paramètres d'itinéraire (Optionnel)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Stone800)

                SectionCard {
                    Text("Type d'activité", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                    Spacer(Modifier.height(12.dp))
                    val types = listOf(
                        Triple("culture", Icons.Default.AccountBalance, "Culture"),
                        Triple("food", Icons.Default.Restaurant, "Gastronomie"),
                        Triple("nature", Icons.Default.Park, "Nature"),
                        Triple("leisure", Icons.Default.SportsEsports, "Loisirs"),
                        Triple("shopping", Icons.Default.ShoppingBag, "Shopping"),
                        Triple("nightlife", Icons.Default.Nightlife, "Vie nocturne"),
                        Triple("sport", Icons.Default.DirectionsRun, "Sport"),
                        Triple("photo", Icons.Default.PhotoCamera, "Photo")
                    )
                    types.chunked(4).forEach { rowTypes ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowTypes.forEach { (id, icon, label) ->
                                val selected = selectedCategory == id
                                Surface(
                                    onClick = { selectedCategory = if (selected) null else id },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(2.dp, if (selected) Color(0xFFF87171) else Color.Transparent),
                                    color = if (selected) Color(0xFFFEF2F2) else Color(0xFFF5F5F4)
                                ) {
                                    Column(modifier = Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(icon, null, tint = if (selected) RedPrimary else StoneMuted, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.height(4.dp))
                                        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = if (selected) RedPrimary else StoneMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            repeat(4 - rowTypes.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

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

                SectionCard {
                    Text("Horaires et disponibilité", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = openHours,
                        onValueChange = { openHours = it },
                        placeholder = { Text("Ex: 09:00-18:00", color = StoneLighter, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Schedule, null, tint = RedPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = Color(0xFFF5F5F4),
                            focusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = closedDay,
                        onValueChange = { closedDay = it },
                        placeholder = { Text("Jour fermé (optionnel)", color = StoneLighter, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.EventBusy, null, tint = RedPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = Color(0xFFF5F5F4),
                            focusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )
                }

                SectionCard {
                    Text("Conditions et meilleur moment", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StoneLighter)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            "indoor" to "Intérieur",
                            "outdoor" to "Extérieur",
                            "both" to "Les deux"
                        ).forEach { (id, label) ->
                            FilterChip(
                                selected = weatherType == id,
                                onClick = { weatherType = id },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            "matin" to "Matin",
                            "apres-midi" to "Après-midi",
                            "soir" to "Soir"
                        ).forEach { (id, label) ->
                            val selected = id in bestTimeSlots
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    bestTimeSlots = if (selected) {
                                        (bestTimeSlots - id).ifEmpty { setOf("apres-midi") }
                                    } else {
                                        bestTimeSlots + id
                                    }
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

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
        if (publishState is PublishUiState.Uploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardBg,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = RedPrimary)
                        Text(
                            (publishState as PublishUiState.Uploading).progressText,
                            color = Stone800,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showMapPicker) {
        MapLocationPickerOverlay(
            initialLocation = selectedLocation,
            onDismiss = { showMapPicker = false },
            onConfirm = {
                selectedLocation = it
                showMapPicker = false
            }
        )
    }

    if (publishPreview) {
        val previewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { publishPreview = false },
            sheetState = previewSheetState,
            containerColor = CardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    if (isEditMode) "Résumé des modifications" else "Résumé de publication",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Stone800
                )
                Spacer(Modifier.height(8.dp))
                Text("Vérifiez les informations avant publication.", fontSize = 13.sp, color = Stone500)
                Spacer(Modifier.height(16.dp))
                InfoLine("Titre", title)
                InfoLine("Visibilité", if (visibility == "public") "Public" else "Groupe : ${selectedGroup?.name ?: "-"}")
                InfoLine(
                    "Lieu",
                    selectedLocation?.let {
                        "${it.name} (${if (it.precision == "exact") "exact" else "approximatif"})"
                    } ?: "Non défini"
                )
                InfoLine("Photos", "${selectedPhotos.size}")
                InfoLine("Note vocale", if (voiceNoteUri != null) "Ajoutée" else "Aucune")
                InfoLine("Tags", selectedTags.ifEmpty { listOf("Voyage") }.joinToString(", ") { "#$it" })
                InfoLine("TravelPath", if (isLinkedToPath) "Étape TravelPath" else "Non lié")
                if (isLinkedToPath) {
                    InfoLine("Coût TravelPath", expense.toIntOrNull()?.let { "$it EUR" } ?: "Non spécifié")
                    InfoLine("Durée TravelPath", if (duration > 0f) "${duration.toInt()} h" else "Non spécifiée")
                    InfoLine("Effort", if (effort > 0f) effortLabels[effort.toInt()] ?: "Modéré" else "Non spécifié")
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (isRecordingVoice) stopVoiceRecording()
                        publishPreview = false
                        val location = selectedLocation ?: return@Button
                        val commonTags = selectedTags.ifEmpty { listOf("Voyage") }
                        val commonPlaceType = selectedCategory ?: "photo"
                        val commonCost = if (isLinkedToPath) expense.toIntOrNull() else null
                        val commonDuration = if (isLinkedToPath && duration > 0f) duration.toInt() * 60 else null
                        val commonEffort = if (isLinkedToPath && effort > 0f) effort.toInt() else null
                        val commonOpenHours = if (isLinkedToPath) openHours.trim().takeIf { it.isNotBlank() } else null
                        val commonClosedDay = if (isLinkedToPath) closedDay.trim().takeIf { it.isNotBlank() } else null
                        val commonWeatherType = if (isLinkedToPath) weatherType else null
                        val commonBestTimeSlots = if (isLinkedToPath) bestTimeSlots.toList() else emptyList()
                        if (isEditMode && !editPostId.isNullOrBlank()) {
                            publishViewModel.updateExistingPost(
                                postId = editPostId,
                                selectedPhotoUris = selectedPhotos,
                                voiceNoteUri = voiceNoteUri,
                                title = title,
                                description = description.ifBlank { title },
                                selectedLocation = location,
                                visibility = visibility,
                                selectedGroup = selectedGroup,
                                tags = commonTags,
                                placeType = commonPlaceType,
                                isLinkedToTravelPath = isLinkedToPath,
                                travelPathCost = commonCost,
                                travelPathDurationMinutes = commonDuration,
                                travelPathEffortLevel = commonEffort,
                                travelPathOpenHours = commonOpenHours,
                                travelPathClosedDay = commonClosedDay,
                                travelPathWeatherType = commonWeatherType,
                                travelPathBestTimeSlots = commonBestTimeSlots
                            )
                        } else {
                            publishViewModel.publish(
                                selectedPhotoUris = selectedPhotos,
                                voiceNoteUri = voiceNoteUri,
                                title = title,
                                description = description.ifBlank { title },
                                selectedLocation = location,
                                visibility = visibility,
                                selectedGroup = selectedGroup,
                                tags = commonTags,
                                placeType = commonPlaceType,
                                isLinkedToTravelPath = isLinkedToPath,
                                travelPathCost = commonCost,
                                travelPathDurationMinutes = commonDuration,
                                travelPathEffortLevel = commonEffort,
                                travelPathOpenHours = commonOpenHours,
                                travelPathClosedDay = commonClosedDay,
                                travelPathWeatherType = commonWeatherType,
                                travelPathBestTimeSlots = commonBestTimeSlots
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    enabled = publishState !is PublishUiState.Uploading
                ) {
                    Text(
                        if (publishState is PublishUiState.Uploading) {
                            (publishState as PublishUiState.Uploading).progressText
                        } else {
                            if (isEditMode) "Enregistrer" else "Publier"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceNoteRecorder(
    hasVoiceNote: Boolean,
    isRecording: Boolean,
    isPlaying: Boolean,
    onRecord: () -> Unit,
    onStopRecording: () -> Unit,
    onPlayPause: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (isRecording) RedPrimary else Color(0xFFFEF2F2),
        border = BorderStroke(1.dp, if (isRecording) RedPrimary else Color(0xFFFECACA))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(if (isRecording) Color.White.copy(alpha = 0.18f) else Color.White, CircleShape)
                    .clickable {
                        when {
                            isRecording -> onStopRecording()
                            hasVoiceNote -> onPlayPause()
                            else -> onRecord()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    isRecording -> Icons.Default.Stop
                    hasVoiceNote && isPlaying -> Icons.Default.Pause
                    hasVoiceNote -> Icons.Default.PlayArrow
                    else -> Icons.Outlined.Mic
                }
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isRecording) Color.White else RedPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                when {
                    isRecording -> "Enregistrement..."
                    hasVoiceNote -> if (isPlaying) "Lecture vocale" else "Note vocale"
                    else -> "Note vocale"
                },
                color = if (isRecording) Color.White else RedPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (hasVoiceNote && !isRecording) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Supprimer la note vocale",
                    tint = RedPrimary,
                    modifier = Modifier.size(16.dp).clickable(onClick = onDelete)
                )
            }
        }
    }
}

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
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, StoneBorder)
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
        color = if (selected) selectedBg else Color(0xFFF5F5F4)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = if (selected) selectedColor else StoneLighter)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (selected) selectedColor else StoneMuted)
        }
    }
}

private fun MediaRecorder.runCatchingStopAndRelease() {
    runCatching { stop() }
    release()
}

private fun MediaPlayer.runCatchingStopAndRelease() {
    runCatching {
        if (isPlaying) stop()
    }
    release()
}

private fun Context.lastKnownLocationOrNull(): Location? {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    return providers
        .filter { provider -> runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false) }
        .mapNotNull { provider ->
            runCatching {
                locationManager.getLastKnownLocation(provider)
            }.getOrNull()
        }
        .maxByOrNull { it.time }
}

private fun PhotoPostDocument.toSelectedLocationUi(): SelectedLocationUi? {
    val rawLat = rawLatitude ?: latitude ?: displayLatitude ?: return null
    val rawLng = rawLongitude ?: longitude ?: displayLongitude ?: return null
    return buildSelectedLocation(
        name = locationName.ifBlank { title.ifBlank { "Lieu sélectionné" } },
        rawLatitude = rawLat,
        rawLongitude = rawLng,
        precision = locationPrecision.ifBlank { "exact" },
        address = locationAddress,
        city = city,
        country = country,
        googlePlaceId = googlePlaceId,
        source = locationSource ?: "edit"
    )
}

@Preview(showBackground = true)
@Composable
fun PublishPhotosScreenPreview() {
    PublishPhotosScreen()
}
