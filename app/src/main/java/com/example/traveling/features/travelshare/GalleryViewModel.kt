package com.example.traveling.features.travelshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.model.GroupDocument
import com.example.traveling.data.model.User
import com.example.traveling.data.repository.GroupRepository
import com.example.traveling.data.repository.NotificationRepository
import com.example.traveling.data.repository.PhotoPostRepository
import com.example.traveling.data.repository.UserRepository
import com.example.traveling.features.travelshare.PhotoPostUi
import com.example.traveling.features.travelshare.toPhotoPostUi
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
    val color: androidx.compose.ui.graphics.Color,
    val avatarUrl: String? = null
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
    private var postsLoaded = false
    private var reactionsReady = false
    private var postsVersion = 0
    private val optimisticLikeStates = mutableMapOf<String, Boolean>()
    private val optimisticLikeCounts = mutableMapOf<String, Int>()
    private val optimisticSaveStates = mutableMapOf<String, Boolean>()

    fun observeVisiblePosts() {
        listener?.remove()
        settingsListener?.remove()
        groupsListener?.remove()
        latestPosts = emptyList()
        followedUsers = emptyList()
        joinedGroups = emptyList()
        // les états optimistes ne doivent pas survivre à un nouvel observer
        optimisticLikeStates.clear()
        optimisticLikeCounts.clear()
        optimisticSaveStates.clear()
        postsLoaded = false
        reactionsReady = false
        postsVersion += 1
        _uiState.value = GalleryUiState.Loading

        // on écoute les posts visibles selon le mode connecté ou anonyme
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
                    // ces auteurs deviennent les raccourcis du haut dans la galerie
                    followedUsers = runCatching { userRepository.getUsers(settings.followedUserIds) }.getOrDefault(emptyList())
                    if (postsLoaded) emitSuccess()
                }
            },
            onError = {}
        )
        groupsListener = groupRepository.observeMyGroups(
            userId = uid,
            onChanged = {
                // les groupes rejoints sont affichés avec les auteurs suivis
                joinedGroups = it
                if (postsLoaded) emitSuccess()
            },
            onError = {}
        )
    }

    fun toggleLike(postId: String, currentlyLiked: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        postsVersion += 1
        val nextLiked = !currentlyLiked
        var nextLikeCount = 0
        // mise à jour immédiate pour éviter un délai visuel après le clic
        updatePostLocally(postId) { post ->
            nextLikeCount = (post.likes + if (nextLiked) 1 else -1).coerceAtLeast(0)
            post.copy(
                isLiked = nextLiked,
                likes = nextLikeCount
            )
        }
        optimisticLikeStates[postId] = nextLiked
        optimisticLikeCounts[postId] = nextLikeCount
        viewModelScope.launch {
            val result = runCatching {
                if (currentlyLiked) repository.unlikePost(uid, postId) else repository.likePost(uid, postId)
            }
            optimisticLikeStates.remove(postId)
            optimisticLikeCounts.remove(postId)
            if (result.isFailure) {
                // rollback visuel si Firestore refuse l'opération
                updatePostLocally(postId) { post ->
                    post.copy(
                        isLiked = currentlyLiked,
                        likes = (post.likes + if (currentlyLiked) 1 else -1).coerceAtLeast(0)
                    )
                }
            }
        }
    }

    fun toggleSave(postId: String, currentlySaved: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        postsVersion += 1
        val nextSaved = !currentlySaved
        updatePostLocally(postId) { post ->
            post.copy(isSaved = nextSaved)
        }
        optimisticSaveStates[postId] = nextSaved
        viewModelScope.launch {
            val result = runCatching {
                if (currentlySaved) repository.unsavePost(uid, postId) else repository.savePost(uid, postId, null)
            }
            optimisticSaveStates.remove(postId)
            if (result.isFailure) {
                // rollback uniquement du save, sans toucher au like
                updatePostLocally(postId) { post ->
                    post.copy(isSaved = currentlySaved)
                }
            }
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
        val version = ++postsVersion
        postsLoaded = true
        val hadVisiblePosts = latestPosts.isNotEmpty()
        reactionsReady = uid == null || documents.isEmpty() || hadVisiblePosts
        val previousPostsById = latestPosts.associateBy { it.id }
        // on garde l'ancien état local en attendant les likes/saves depuis Firestore
        latestPosts = documents.map { document ->
            val previous = previousPostsById[document.postId]
            val liked = optimisticLikeStates[document.postId] ?: previous?.isLiked ?: false
            val saved = optimisticSaveStates[document.postId] ?: previous?.isSaved ?: false
            val optimisticLikeCount = optimisticLikeCounts[document.postId]
            document.toPhotoPostUi(isLiked = liked, isSaved = saved).let { post ->
                if (optimisticLikeCount != null) post.copy(likes = optimisticLikeCount) else post
            }
        }

        if (uid == null || documents.isEmpty()) {
            emitSuccess()
            return
        }
        if (hadVisiblePosts) {
            // on affiche vite les posts, puis on corrige les états like/save
            emitSuccess()
        }

        viewModelScope.launch {
            // lecture groupée des ids pour éviter une requête par carte
            val likedIds = runCatching { repository.getLikedPostIds(uid) }.getOrDefault(emptySet())
            val savedIds = runCatching { repository.getSavedPostIds(uid) }.getOrDefault(emptySet())
            val posts = documents.map { doc ->
                val liked = likedIds.contains(doc.postId)
                val saved = savedIds.contains(doc.postId)
                val resolvedLiked = optimisticLikeStates[doc.postId] ?: liked
                val resolvedSaved = optimisticSaveStates[doc.postId] ?: saved
                val optimisticLikeCount = optimisticLikeCounts[doc.postId]
                doc.toPhotoPostUi(isLiked = resolvedLiked, isSaved = resolvedSaved).let { post ->
                    if (optimisticLikeCount != null) post.copy(likes = optimisticLikeCount) else post
                }
            }
            if (version != postsVersion) return@launch
            latestPosts = posts
            reactionsReady = true
            emitSuccess()
        }
    }

    private fun updatePostLocally(postId: String, transform: (PhotoPostUi) -> PhotoPostUi) {
        latestPosts = latestPosts.map { post ->
            if (post.id == postId) transform(post) else post
        }
        emitSuccess()
    }

    private fun emitSuccess() {
        // attendre reactionsReady évite les icônes qui disparaissent au premier chargement
        if (!postsLoaded || !reactionsReady) return
        _uiState.value = GalleryUiState.Success(
            posts = latestPosts,
            shortcuts = buildShortcuts()
        )
    }

    private fun buildShortcuts(): List<TravelShareShortcutUi> {
        // raccourcis mixtes: voyageurs suivis puis groupes rejoints
        val userShortcuts = followedUsers.map { user ->
            val name = user.displayName.ifBlank { user.email.substringBefore("@").ifBlank { "Voyageur" } }
            TravelShareShortcutUi(
                id = user.userId,
                label = name,
                initial = name.firstOrNull()?.uppercase() ?: "V",
                type = "user",
                color = shortcutColor(user.userId.ifBlank { name }),
                avatarUrl = user.avatarUrl
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
