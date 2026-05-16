package com.example.traveling.features.profile

import androidx.lifecycle.ViewModel
import com.example.traveling.data.model.PhotoPostDocument
import com.example.traveling.data.repository.PhotoPostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed interface MyPublishedUiState {
    data object Loading : MyPublishedUiState
    data class Success(val posts: List<PhotoPostDocument>) : MyPublishedUiState
    data class Error(val message: String) : MyPublishedUiState
}

class MyPublishedPostsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val repository: PhotoPostRepository = PhotoPostRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyPublishedUiState>(MyPublishedUiState.Loading)
    val uiState: StateFlow<MyPublishedUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var listener: ListenerRegistration? = null

    fun observeMyPosts() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = MyPublishedUiState.Error("Utilisateur non connecté")
            return
        }

        listener?.remove()
        _uiState.value = MyPublishedUiState.Loading

        // listener temps réel pour gérer rapidement suppression et modification
        listener = repository.observeMyPublishedPosts(
            userId = uid,
            onChanged = { documents ->
                _uiState.value = MyPublishedUiState.Success(documents)
            },
            onError = { error ->
                _uiState.value = MyPublishedUiState.Error(
                    error.localizedMessage ?: "Erreur de chargement de mes publications"
                )
            }
        )
    }

    fun deletePost(postId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            // suppression douce: le post reste en base mais n'est plus publié
            repository.softDeletePost(postId, uid)
                .onSuccess { _events.emit("Publication supprimée") }
                .onFailure { _events.emit(it.localizedMessage ?: "Échec de suppression") }
        }
    }

    fun updatePost(
        postId: String,
        title: String,
        description: String,
        locationName: String,
        city: String?,
        country: String?,
        locationPrecision: String,
        placeType: String,
        visibility: String,
        tags: List<String>,
        isLinkedToTravelPath: Boolean
    ) {
        viewModelScope.launch {
            repository.updatePost(
                postId = postId,
                title = title,
                description = description,
                locationName = locationName,
                city = city,
                country = country,
                locationPrecision = locationPrecision.lowercase(),
                placeType = placeType.lowercase(),
                visibility = visibility.lowercase(),
                tags = tags,
                isLinkedToTravelPath = isLinkedToTravelPath
            ).onSuccess {
                _events.emit("Publication modifiée")
            }.onFailure {
                _events.emit(it.localizedMessage ?: "Échec de mise à jour")
            }
        }
    }

    override fun onCleared() {
        listener?.remove()
        listener = null
        super.onCleared()
    }
}
