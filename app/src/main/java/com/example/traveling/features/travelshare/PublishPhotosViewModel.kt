package com.example.traveling.features.travelshare

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.repository.PhotoPostRepository
import com.example.traveling.data.repository.PublishPhotoPostInput
import com.example.traveling.data.repository.UserRepository
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

class PublishPhotosViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = UserRepository(),
    private val photoPostRepository: PhotoPostRepository = PhotoPostRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<PublishUiState>(PublishUiState.Idle)
    val uiState: StateFlow<PublishUiState> = _uiState.asStateFlow()

    fun publish(
        selectedPhotoUris: List<String>,
        title: String,
        description: String,
        locationName: String,
        locationPrecision: String,
        visibility: String,
        selectedGroup: String?,
        tags: List<String>,
        placeType: String,
        isLinkedToTravelPath: Boolean
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
                title = title,
                description = description,
                locationName = locationName,
                locationPrecision = locationPrecision,
                placeType = placeType,
                tags = tags,
                visibility = visibility,
                groupId = null,
                groupName = if (visibility == "group") selectedGroup else null,
                isLinkedToTravelPath = isLinkedToTravelPath
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

    fun resetState() {
        _uiState.value = PublishUiState.Idle
    }
}
