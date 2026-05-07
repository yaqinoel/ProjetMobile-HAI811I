package com.example.traveling.features.travelshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.model.PhotoPostDocument
import com.example.traveling.data.repository.PhotoPostRepository
import com.example.traveling.features.travelshare.model.PhotoPostUi
import com.example.traveling.features.travelshare.model.toPhotoPostUi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GalleryUiState {
    data object Loading : GalleryUiState
    data class Success(val posts: List<PhotoPostUi>) : GalleryUiState
    data class Error(val message: String) : GalleryUiState
}

class GalleryViewModel(
    private val repository: PhotoPostRepository = PhotoPostRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private var listener: ListenerRegistration? = null
    private var latestDocs: List<PhotoPostDocument> = emptyList()

    fun observePublicPosts() {
        listener?.remove()
        _uiState.value = GalleryUiState.Loading

        listener = repository.observePublicPublishedPosts(
            onChanged = { documents ->
                latestDocs = documents
                emitUiFromDocuments(documents)
            },
            onError = { error ->
                _uiState.value = GalleryUiState.Error(
                    error.localizedMessage ?: "Erreur de chargement de la galerie"
                )
            }
        )
    }

    fun toggleLike(postId: String, currentlyLiked: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            if (currentlyLiked) repository.unlikePost(uid, postId) else repository.likePost(uid, postId)
        }
    }

    fun toggleSave(postId: String, currentlySaved: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            if (currentlySaved) repository.unsavePost(uid, postId) else repository.savePost(uid, postId, null)
        }
    }

    private fun emitUiFromDocuments(documents: List<PhotoPostDocument>) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = GalleryUiState.Success(
                documents.map { it.toPhotoPostUi(isLiked = false, isSaved = false) }
            )
            return
        }

        viewModelScope.launch {
            val posts = documents.map { doc ->
                val liked = runCatching { repository.isPostLikedByUser(uid, doc.postId) }.getOrDefault(false)
                val saved = runCatching { repository.isPostSavedByUser(uid, doc.postId) }.getOrDefault(false)
                doc.toPhotoPostUi(isLiked = liked, isSaved = saved)
            }
            _uiState.value = GalleryUiState.Success(posts)
        }
    }

    override fun onCleared() {
        listener?.remove()
        listener = null
        super.onCleared()
    }
}
