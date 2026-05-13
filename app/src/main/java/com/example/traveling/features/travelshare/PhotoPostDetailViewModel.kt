package com.example.traveling.features.travelshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.model.PhotoCommentDocument
import com.example.traveling.data.model.PhotoPostDocument
import com.example.traveling.data.repository.PhotoPostRepository
import com.example.traveling.data.repository.UserRepository
import com.example.traveling.features.travelshare.PhotoPostDetailUi
import com.example.traveling.features.travelshare.toPhotoPostDetailUi
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
    private val userRepository: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<PhotoPostDetailUiState>(PhotoPostDetailUiState.Loading)
    val uiState: StateFlow<PhotoPostDetailUiState> = _uiState.asStateFlow()

    private var detailListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null

    private var currentPost: PhotoPostDocument? = null
    private var currentComments: List<PhotoCommentDocument> = emptyList()
    private var currentPostId: String? = null
    private var currentLiked = false
    private var currentSaved = false
    private var stateVersion = 0

    fun loadPhoto(photoId: String) {
        currentPostId = photoId
        currentPost = null
        currentComments = emptyList()
        currentLiked = false
        currentSaved = false
        stateVersion += 1
        _uiState.value = PhotoPostDetailUiState.Loading

        detailListener?.remove()
        commentsListener?.remove()

        detailListener = repository.observePostDetail(
            postId = photoId,
            onChanged = { post ->
                if (post == null) {
                    _uiState.value = PhotoPostDetailUiState.Error("Photo introuvable.")
                } else {
                    currentPost = post
                    publishUiState()
                    checkLikeSaveStateAndRefresh()
                }
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
        if (user.isAnonymous) return
        if (content.isBlank()) return

        viewModelScope.launch {
            val userDoc = runCatching { userRepository.getUser(user.uid) }.getOrNull()
            val comment = PhotoCommentDocument(
                commentId = "",
                postId = postId,
                authorId = user.uid,
                authorName = userDoc?.displayName?.takeIf { it.isNotBlank() }
                    ?: user.displayName
                    ?: user.email?.substringBefore("@")
                    ?: "Voyageur",
                authorAvatarUrl = userDoc?.avatarUrl,
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

        stateVersion += 1
        val previousLiked = currentLiked
        val nextLiked = !previousLiked
        currentLiked = nextLiked
        currentPost = currentPost?.copy(
            likeCount = ((currentPost?.likeCount ?: 0) + if (nextLiked) 1 else -1).coerceAtLeast(0)
        )
        publishUiState()

        viewModelScope.launch {
            val result = if (previousLiked) {
                repository.unlikePost(user.uid, postId)
            } else {
                repository.likePost(user.uid, postId)
            }
            result.onFailure {
                currentLiked = previousLiked
                currentPost = currentPost?.copy(
                    likeCount = ((currentPost?.likeCount ?: 0) + if (previousLiked) 1 else -1).coerceAtLeast(0)
                )
                publishUiState()
            }
        }
    }

    fun toggleSave() {
        val postId = currentPostId ?: return
        val user = auth.currentUser ?: return

        stateVersion += 1
        val previousSaved = currentSaved
        currentSaved = !previousSaved
        publishUiState()

        viewModelScope.launch {
            val result = if (previousSaved) {
                repository.unsavePost(user.uid, postId)
            } else {
                repository.savePost(user.uid, postId, null)
            }
            result.onFailure {
                currentSaved = previousSaved
                publishUiState()
            }
        }
    }

    fun reportPost(reason: String, description: String? = null) {
        val postId = currentPostId ?: return
        val reporterId = auth.currentUser?.uid
        viewModelScope.launch {
            repository.reportPost(
                postId = postId,
                reporterId = reporterId,
                reason = reason,
                description = description
            )
        }
    }

    private fun publishUiState() {
        val post = currentPost
        if (post == null) {
            return
        }

        _uiState.value = PhotoPostDetailUiState.Success(
            photo = post.toPhotoPostDetailUi(
                comments = currentComments,
                isLiked = currentLiked,
                isSaved = currentSaved
            )
        )
    }

    private fun checkLikeSaveStateAndRefresh() {
        val postId = currentPostId ?: return
        val user = auth.currentUser ?: return
        val version = stateVersion

        viewModelScope.launch {
            val liked = runCatching { repository.isPostLikedByUser(user.uid, postId) }.getOrDefault(false)
            val saved = runCatching { repository.isPostSavedByUser(user.uid, postId) }.getOrDefault(false)
            if (version != stateVersion) return@launch
            currentLiked = liked
            currentSaved = saved
            publishUiState()
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
