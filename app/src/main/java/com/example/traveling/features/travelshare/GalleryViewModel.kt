package com.example.traveling.features.travelshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.model.GroupDocument
import com.example.traveling.data.model.User
import com.example.traveling.data.repository.GroupRepository
import com.example.traveling.data.repository.NotificationRepository
import com.example.traveling.data.repository.PhotoPostRepository
import com.example.traveling.data.repository.UserRepository
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
    data class Success(
        val posts: List<PhotoPostUi>,
        val shortcuts: List<TravelShareShortcutUi> = emptyList()
    ) : GalleryUiState
    data class Error(val message: String) : GalleryUiState
}

data class TravelShareShortcutUi(
    val id: String,
    val label: String,
    val initial: String,
    val type: String,
    val color: androidx.compose.ui.graphics.Color
)

class GalleryViewModel(
    private val repository: PhotoPostRepository = PhotoPostRepository(),
    private val notificationRepository: NotificationRepository = NotificationRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val groupRepository: GroupRepository = GroupRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private var listener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    private var groupsListener: ListenerRegistration? = null
    private var latestPosts: List<PhotoPostUi> = emptyList()
    private var followedUsers: List<User> = emptyList()
    private var joinedGroups: List<GroupDocument> = emptyList()

    fun observeVisiblePosts() {
        listener?.remove()
        settingsListener?.remove()
        groupsListener?.remove()
        _uiState.value = GalleryUiState.Loading

        listener = repository.observeVisiblePublishedPosts(
            userId = auth.currentUser?.uid,
            onChanged = { documents ->
                emitUiFromDocuments(documents)
            },
            onError = { error ->
                _uiState.value = GalleryUiState.Error(
                    error.localizedMessage ?: "Erreur de chargement de la galerie"
                )
            }
        )

        val uid = auth.currentUser?.uid ?: return
        settingsListener = notificationRepository.observeNotificationSettings(
            userId = uid,
            onChanged = { settings ->
                viewModelScope.launch {
                    followedUsers = runCatching { userRepository.getUsers(settings.followedUserIds) }.getOrDefault(emptyList())
                    emitSuccess()
                }
            },
            onError = {}
        )
        groupsListener = groupRepository.observeMyGroups(
            userId = uid,
            onChanged = {
                joinedGroups = it
                emitSuccess()
            },
            onError = {}
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

    fun reportPost(postId: String, reason: String = "Signalement depuis la galerie") {
        val reporterId = auth.currentUser?.uid
        viewModelScope.launch {
            repository.reportPost(
                postId = postId,
                reporterId = reporterId,
                reason = reason
            )
        }
    }

    private fun emitUiFromDocuments(documents: List<com.example.traveling.data.model.PhotoPostDocument>) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            latestPosts = documents.map { it.toPhotoPostUi(isLiked = false, isSaved = false) }
            emitSuccess()
            return
        }

        viewModelScope.launch {
            val posts = documents.map { doc ->
                val liked = runCatching { repository.isPostLikedByUser(uid, doc.postId) }.getOrDefault(false)
                val saved = runCatching { repository.isPostSavedByUser(uid, doc.postId) }.getOrDefault(false)
                doc.toPhotoPostUi(isLiked = liked, isSaved = saved)
            }
            latestPosts = posts
            emitSuccess()
        }
    }

    private fun emitSuccess() {
        _uiState.value = GalleryUiState.Success(
            posts = latestPosts,
            shortcuts = buildShortcuts()
        )
    }

    private fun buildShortcuts(): List<TravelShareShortcutUi> {
        val userShortcuts = followedUsers.map { user ->
            val name = user.displayName.ifBlank { user.email.substringBefore("@").ifBlank { "Voyageur" } }
            TravelShareShortcutUi(
                id = user.userId,
                label = name,
                initial = name.firstOrNull()?.uppercase() ?: "V",
                type = "user",
                color = shortcutColor(user.userId.ifBlank { name })
            )
        }
        val groupShortcuts = joinedGroups.map { group ->
            TravelShareShortcutUi(
                id = group.groupId,
                label = group.name.ifBlank { "Groupe" },
                initial = group.name.firstOrNull()?.uppercase() ?: "G",
                type = "group",
                color = androidx.compose.ui.graphics.Color(0xFFD97706)
            )
        }
        return userShortcuts + groupShortcuts
    }

    private fun shortcutColor(seed: String): androidx.compose.ui.graphics.Color {
        val palette = listOf(
            androidx.compose.ui.graphics.Color(0xFFB91C1C),
            androidx.compose.ui.graphics.Color(0xFF7C3AED),
            androidx.compose.ui.graphics.Color(0xFF0D9488),
            androidx.compose.ui.graphics.Color(0xFF2563EB),
            androidx.compose.ui.graphics.Color(0xFFDC2626)
        )
        val index = seed.fold(0) { acc, char -> acc + char.code }.mod(palette.size)
        return palette[index]
    }

    override fun onCleared() {
        listener?.remove()
        settingsListener?.remove()
        groupsListener?.remove()
        listener = null
        settingsListener = null
        groupsListener = null
        super.onCleared()
    }
}
