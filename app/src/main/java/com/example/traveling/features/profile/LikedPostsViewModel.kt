package com.example.traveling.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.repository.PhotoPostRepository
import com.example.traveling.features.travelshare.PhotoPostUi
import com.example.traveling.features.travelshare.toPhotoPostUi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LikedPostsUiState {
    data object Loading : LikedPostsUiState
    data class Success(val posts: List<PhotoPostUi>) : LikedPostsUiState
    data class Error(val message: String) : LikedPostsUiState
}

class LikedPostsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val repository: PhotoPostRepository = PhotoPostRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LikedPostsUiState>(LikedPostsUiState.Loading)
    val uiState: StateFlow<LikedPostsUiState> = _uiState.asStateFlow()

    fun loadLikedPosts() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = LikedPostsUiState.Error("Utilisateur non connecté")
            return
        }

        _uiState.value = LikedPostsUiState.Loading
        viewModelScope.launch {
            repository.getLikedPosts(uid)
                .onSuccess { docs ->
                    _uiState.value = LikedPostsUiState.Success(
                        docs.map { it.toPhotoPostUi(isLiked = true, isSaved = false) }
                    )
                }
                .onFailure {
                    _uiState.value = LikedPostsUiState.Error(
                        it.localizedMessage ?: "Erreur de chargement des favoris"
                    )
                }
        }
    }

    fun unlike(postId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.unlikePost(uid, postId)
            loadLikedPosts()
        }
    }

    fun toggleSave(postId: String, currentlySaved: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            if (currentlySaved) repository.unsavePost(uid, postId) else repository.savePost(uid, postId, null)
            loadLikedPosts()
        }
    }
}
