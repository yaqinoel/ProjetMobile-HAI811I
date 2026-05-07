package com.example.traveling.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.repository.PhotoPostRepository
import com.example.traveling.features.travelshare.model.PhotoPostUi
import com.example.traveling.features.travelshare.model.toPhotoPostUi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SavedPostsUiState {
    data object Loading : SavedPostsUiState
    data class Success(val posts: List<PhotoPostUi>) : SavedPostsUiState
    data class Error(val message: String) : SavedPostsUiState
}

class SavedPostsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val repository: PhotoPostRepository = PhotoPostRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SavedPostsUiState>(SavedPostsUiState.Loading)
    val uiState: StateFlow<SavedPostsUiState> = _uiState.asStateFlow()

    fun loadSavedPosts() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = SavedPostsUiState.Error("Utilisateur non connecté")
            return
        }

        _uiState.value = SavedPostsUiState.Loading
        viewModelScope.launch {
            repository.getSavedPosts(uid)
                .onSuccess { docs ->
                    _uiState.value = SavedPostsUiState.Success(
                        docs.map { it.toPhotoPostUi(isLiked = false, isSaved = true) }
                    )
                }
                .onFailure {
                    _uiState.value = SavedPostsUiState.Error(
                        it.localizedMessage ?: "Erreur de chargement des enregistrements"
                    )
                }
        }
    }

    fun unsave(postId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.unsavePost(uid, postId)
            loadSavedPosts()
        }
    }
}
