package com.example.traveling.features.travelshare

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.model.PhotoPostDocument
import com.example.traveling.data.model.TravelShareAttractionDocument
import com.example.traveling.data.model.UserJoinedGroupDocument
import com.example.traveling.data.repository.GroupRepository
import com.example.traveling.data.repository.PhotoPostRepository
import com.example.traveling.data.repository.PublishPhotoPostInput
import com.example.traveling.data.repository.UserRepository
import com.example.traveling.features.travelshare.ImageAnnotationRepository
import com.example.traveling.features.travelshare.SelectedLocationUi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PublishUiState {
    data object Idle : PublishUiState
    data class Uploading(val progressText: String) : PublishUiState
    data class Success(val postId: String) : PublishUiState
    data class Error(val message: String) : PublishUiState
}

sealed interface AiAnnotationUiState {
    data object Idle : AiAnnotationUiState
    data object Loading : AiAnnotationUiState
    data class Success(val tags: List<String>) : AiAnnotationUiState
    data class Error(val message: String) : AiAnnotationUiState
}

class PublishPhotosViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = UserRepository(),
    private val photoPostRepository: PhotoPostRepository = PhotoPostRepository(),
    private val groupRepository: GroupRepository = GroupRepository(),
    private val imageAnnotationRepository: ImageAnnotationRepository = ImageAnnotationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<PublishUiState>(PublishUiState.Idle)
    val uiState: StateFlow<PublishUiState> = _uiState.asStateFlow()
    private val _aiAnnotationState = MutableStateFlow<AiAnnotationUiState>(AiAnnotationUiState.Idle)
    val aiAnnotationState: StateFlow<AiAnnotationUiState> = _aiAnnotationState.asStateFlow()
    private val _joinedGroups = MutableStateFlow<List<UserJoinedGroupDocument>>(emptyList())
    val joinedGroups: StateFlow<List<UserJoinedGroupDocument>> = _joinedGroups.asStateFlow()

    fun loadJoinedGroups() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            groupRepository.getJoinedGroups(uid)
                .onSuccess { _joinedGroups.value = it }
                .onFailure { _uiState.value = PublishUiState.Error(it.localizedMessage ?: "Impossible de charger les groupes") }
        }
    }

    suspend fun loadPostForEdit(postId: String): Result<PhotoPostDocument> {
        return photoPostRepository.getPostOnce(postId)
    }

    suspend fun loadTravelShareAttractionForEdit(postId: String): Result<TravelShareAttractionDocument?> {
        return photoPostRepository.getTravelShareAttractionOnce(postId)
    }

    fun annotateSelectedImages(context: Context, selectedPhotoUris: List<String>) {
        if (selectedPhotoUris.isEmpty()) {
            _aiAnnotationState.value = AiAnnotationUiState.Error("Ajoutez au moins une photo avant l'annotation IA")
            return
        }

        viewModelScope.launch {
            _aiAnnotationState.value = AiAnnotationUiState.Loading
            runCatching {
                imageAnnotationRepository.annotateImages(
                    context = context.applicationContext,
                    imageUris = selectedPhotoUris.map(Uri::parse)
                )
            }.onSuccess { result ->
                _aiAnnotationState.value = if (result.tags.isEmpty()) {
                    AiAnnotationUiState.Error("Aucun tag IA pertinent trouvé")
                } else {
                    AiAnnotationUiState.Success(result.tags)
                }
            }.onFailure { error ->
                _aiAnnotationState.value = AiAnnotationUiState.Error(
                    error.localizedMessage ?: "Échec de l'annotation IA"
                )
            }
        }
    }

    fun publish(
        selectedPhotoUris: List<String>,
        voiceNoteUri: String?,
        title: String,
        description: String,
        selectedLocation: SelectedLocationUi,
        visibility: String,
        selectedGroup: UserJoinedGroupDocument?,
        tags: List<String>,
        placeType: String,
        isLinkedToTravelPath: Boolean,
        travelPathCost: Int? = null,
        travelPathDurationMinutes: Int? = null,
        travelPathEffortLevel: Int? = null,
        travelPathOpenHours: String? = null,
        travelPathClosedDay: String? = null,
        travelPathWeatherType: String? = null,
        travelPathBestTimeSlots: List<String> = emptyList()
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = PublishUiState.Error("Vous devez être connecté pour publier")
            return
        }

        if (selectedPhotoUris.isEmpty()) {
            _uiState.value = PublishUiState.Error("Ajoutez au moins une photo")
            return
        }
        val normalizedVisibility = visibility.lowercase()
        if (normalizedVisibility == "group" && selectedGroup == null) {
            _uiState.value = PublishUiState.Error("Sélectionnez un groupe")
            return
        }

        viewModelScope.launch {
            _uiState.value = PublishUiState.Uploading("Upload des photos...")

            val userDoc = runCatching { userRepository.getUser(currentUser.uid) }.getOrNull()
            val authorName = userDoc?.displayName
                ?.takeIf { it.isNotBlank() }
                ?: currentUser.displayName
                ?.takeIf { it.isNotBlank() }
                ?: currentUser.email?.substringBefore("@")
                ?: "Voyageur"

            val input = PublishPhotoPostInput(
                authorId = currentUser.uid,
                authorName = authorName,
                authorAvatarUrl = userDoc?.avatarUrl,
                localImageUris = selectedPhotoUris.map(Uri::parse),
                localVoiceNoteUri = voiceNoteUri?.let(Uri::parse),
                title = title,
                description = description,
                locationName = selectedLocation.name,
                locationAddress = selectedLocation.address,
                googlePlaceId = selectedLocation.googlePlaceId,
                locationSource = selectedLocation.source,
                city = selectedLocation.city,
                country = selectedLocation.country,
                rawLatitude = selectedLocation.rawLatitude,
                rawLongitude = selectedLocation.rawLongitude,
                displayLatitude = selectedLocation.displayLatitude,
                displayLongitude = selectedLocation.displayLongitude,
                locationPrecision = selectedLocation.precision,
                placeType = placeType,
                tags = tags,
                visibility = normalizedVisibility,
                groupId = if (normalizedVisibility == "group") selectedGroup?.groupId else null,
                groupName = if (normalizedVisibility == "group") selectedGroup?.name else null,
                isLinkedToTravelPath = isLinkedToTravelPath,
                travelPathCost = travelPathCost,
                travelPathDurationMinutes = travelPathDurationMinutes,
                travelPathEffortLevel = travelPathEffortLevel,
                travelPathOpenHours = travelPathOpenHours,
                travelPathClosedDay = travelPathClosedDay,
                travelPathWeatherType = travelPathWeatherType,
                travelPathBestTimeSlots = travelPathBestTimeSlots
            )

            photoPostRepository.publishPhotoPost(input)
                .onSuccess { postId ->
                    _uiState.value = PublishUiState.Success(postId)
                }
                .onFailure { error ->
                    _uiState.value = PublishUiState.Error(
                        error.localizedMessage ?: "Échec de publication"
                    )
                }
        }
    }

    fun updateExistingPost(
        postId: String,
        selectedPhotoUris: List<String>,
        voiceNoteUri: String?,
        title: String,
        description: String,
        selectedLocation: SelectedLocationUi,
        visibility: String,
        selectedGroup: UserJoinedGroupDocument?,
        tags: List<String>,
        placeType: String,
        isLinkedToTravelPath: Boolean,
        travelPathCost: Int? = null,
        travelPathDurationMinutes: Int? = null,
        travelPathEffortLevel: Int? = null,
        travelPathOpenHours: String? = null,
        travelPathClosedDay: String? = null,
        travelPathWeatherType: String? = null,
        travelPathBestTimeSlots: List<String> = emptyList()
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = PublishUiState.Error("Vous devez être connecté pour modifier")
            return
        }
        if (selectedPhotoUris.isEmpty()) {
            _uiState.value = PublishUiState.Error("Ajoutez au moins une photo")
            return
        }
        if (visibility == "group" && selectedGroup == null) {
            _uiState.value = PublishUiState.Error("Sélectionnez un groupe")
            return
        }

        viewModelScope.launch {
            _uiState.value = PublishUiState.Uploading("Enregistrement...")
            photoPostRepository.updatePostFromPublishForm(
                postId = postId,
                authorId = currentUser.uid,
                imageRefs = selectedPhotoUris,
                voiceNoteRef = voiceNoteUri,
                title = title,
                description = description,
                locationName = selectedLocation.name,
                locationAddress = selectedLocation.address,
                googlePlaceId = selectedLocation.googlePlaceId,
                locationSource = selectedLocation.source,
                city = selectedLocation.city,
                country = selectedLocation.country,
                rawLatitude = selectedLocation.rawLatitude,
                rawLongitude = selectedLocation.rawLongitude,
                displayLatitude = selectedLocation.displayLatitude,
                displayLongitude = selectedLocation.displayLongitude,
                locationPrecision = selectedLocation.precision,
                visibility = visibility,
                groupId = if (visibility == "group") selectedGroup?.groupId else null,
                groupName = if (visibility == "group") selectedGroup?.name else null,
                tags = tags,
                placeType = placeType,
                isLinkedToTravelPath = isLinkedToTravelPath,
                travelPathCost = travelPathCost,
                travelPathDurationMinutes = travelPathDurationMinutes,
                travelPathEffortLevel = travelPathEffortLevel,
                travelPathOpenHours = travelPathOpenHours,
                travelPathClosedDay = travelPathClosedDay,
                travelPathWeatherType = travelPathWeatherType,
                travelPathBestTimeSlots = travelPathBestTimeSlots
            ).onSuccess {
                _uiState.value = PublishUiState.Success(postId)
            }.onFailure { error ->
                _uiState.value = PublishUiState.Error(error.localizedMessage ?: "Échec de modification")
            }
        }
    }

    fun resetState() {
        _uiState.value = PublishUiState.Idle
    }

    fun resetAiAnnotationState() {
        _aiAnnotationState.value = AiAnnotationUiState.Idle
    }
}
