package com.example.traveling.features.travelshare

import androidx.lifecycle.ViewModel
import com.example.traveling.data.repository.PhotoPostRepository
import com.example.traveling.features.travelshare.model.PhotoPostUi
import com.example.traveling.features.travelshare.model.toPhotoPostUi
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface GalleryUiState {
    data object Loading : GalleryUiState
    data class Success(val posts: List<PhotoPostUi>) : GalleryUiState
    data class Error(val message: String) : GalleryUiState
}

class GalleryViewModel(
    private val repository: PhotoPostRepository = PhotoPostRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private var listener: ListenerRegistration? = null

    fun observePublicPosts() {
        listener?.remove()
        _uiState.value = GalleryUiState.Loading

        listener = repository.observePublicPublishedPosts(
            onChanged = { documents ->
                _uiState.value = GalleryUiState.Success(
                    documents.map { it.toPhotoPostUi(isLiked = false, isSaved = false) }
                )
            },
            onError = { error ->
                _uiState.value = GalleryUiState.Error(
                    error.localizedMessage ?: "Erreur de chargement de la galerie"
                )
            }
        )
    }

    override fun onCleared() {
        listener?.remove()
        listener = null
        super.onCleared()
    }
}
