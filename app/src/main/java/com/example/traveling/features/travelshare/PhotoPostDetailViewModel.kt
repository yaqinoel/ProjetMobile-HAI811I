package com.example.traveling.features.travelshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.model.PhotoCommentDocument
import com.example.traveling.data.model.PhotoPostDocument
import com.example.traveling.data.repository.PhotoPostRepository
import com.example.traveling.features.travelshare.model.PhotoPostDetailUi
import com.example.traveling.features.travelshare.model.toPhotoPostDetailUi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PhotoPostDetailUiState {
    data object Loading : PhotoPostDetailUiState()
    data class Success(val photo: PhotoPostDetailUi) : PhotoPostDetailUiState()
    data class Error(val message: String) : PhotoPostDetailUiState()
}

class PhotoPostDetailViewModel(
    private val repository: PhotoPostRepository = PhotoPostRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<PhotoPostDetailUiState>(PhotoPostDetailUiState.Loading)
    val uiState: StateFlow<PhotoPostDetailUiState> = _uiState.asStateFlow()

    private var detailListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null

    private var currentPost: PhotoPostDocument? = null
    private var currentComments: List<PhotoCommentDocument> = emptyList()
    private var currentPostId: String? = null

    fun loadPhoto(photoId: String) {
        currentPostId = photoId
        currentPost = null
        currentComments = emptyList()
        _uiState.value = PhotoPostDetailUiState.Loading

        detailListener?.remove()
        commentsListener?.remove()

        detailListener = repository.observePostDetail(
            postId = photoId,
            onChanged = { post ->
                currentPost = post
                publishUiState()
                checkLikeSaveStateAndRefresh()
            },
            onError = { error ->
                _uiState.value = PhotoPostDetailUiState.Error(
                    error.localizedMessage ?: "Erreur de lecture du post"
                )
            }
        )

        commentsListener = repository.observePostComments(
            postId = photoId,
            onChanged = { comments ->
                currentComments = comments
                publishUiState()
            },
            onError = { error ->
                _uiState.value = PhotoPostDetailUiState.Error(
                    error.localizedMessage ?: "Erreur de lecture des commentaires"
                )
            }
        )
    }

    fun addComment(content: String) {
        val postId = currentPostId ?: return
        val user = auth.currentUser ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            val comment = PhotoCommentDocument(
                commentId = "",
                postId = postId,
                authorId = user.uid,
                authorName = user.displayName ?: user.email?.substringBefore("@") ?: "Voyageur",
                authorAvatarUrl = null,
                content = content.trim(),
                status = "visible"
            )

            repository.addComment(postId, comment)
                .onFailure { }
        }
    }

    fun toggleLike() {
        val postId = currentPostId ?: return
        val user = auth.currentUser ?: return

        viewModelScope.launch {
            val isLiked = runCatching { repository.isPostLikedByUser(user.uid, postId) }.getOrDefault(false)
            val result = if (isLiked) {
                repository.unlikePost(user.uid, postId)
            } else {
                repository.likePost(user.uid, postId)
            }
            result.onFailure {
                // Keep the current detail visible; listener updates state when write succeeds.
            }
        }
    }

    fun toggleSave() {
        val postId = currentPostId ?: return
        val user = auth.currentUser ?: return

        viewModelScope.launch {
            val isSaved = runCatching { repository.isPostSavedByUser(user.uid, postId) }.getOrDefault(false)
            val result = if (isSaved) {
                repository.unsavePost(user.uid, postId)
            } else {
                repository.savePost(user.uid, postId, null)
            }
            result.onFailure {
                // Keep the current detail visible; listener updates state when write succeeds.
            }
        }
    }

    private fun publishUiState(isLiked: Boolean = false, isSaved: Boolean = false) {
        val post = currentPost
        if (post == null) {
            _uiState.value = PhotoPostDetailUiState.Error("Photo introuvable.")
            return
        }

        _uiState.value = PhotoPostDetailUiState.Success(
            photo = post.toPhotoPostDetailUi(
                comments = currentComments,
                isLiked = isLiked,
                isSaved = isSaved
            )
        )
    }

    private fun checkLikeSaveStateAndRefresh() {
        val postId = currentPostId ?: return
        val user = auth.currentUser ?: return

        viewModelScope.launch {
            val liked = runCatching { repository.isPostLikedByUser(user.uid, postId) }.getOrDefault(false)
            val saved = runCatching { repository.isPostSavedByUser(user.uid, postId) }.getOrDefault(false)
            publishUiState(isLiked = liked, isSaved = saved)
        }
    }

    override fun onCleared() {
        detailListener?.remove()
        commentsListener?.remove()
        detailListener = null
        commentsListener = null
        super.onCleared()
    }
}
