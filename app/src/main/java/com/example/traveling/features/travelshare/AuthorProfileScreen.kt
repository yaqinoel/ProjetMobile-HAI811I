package com.example.traveling.features.travelshare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.traveling.core.utils.openNavigationToPlace
import com.example.traveling.data.model.NotificationSettingsDocument
import com.example.traveling.data.model.User
import com.example.traveling.data.repository.NotificationRepository
import com.example.traveling.data.repository.PhotoPostRepository
import com.example.traveling.data.repository.UserRepository
import com.example.traveling.features.travelshare.model.PhotoPostUi
import com.example.traveling.features.travelshare.model.toPhotoPostUi
import com.example.traveling.ui.components.UserAvatar
import com.example.traveling.ui.theme.PageBg
import com.example.traveling.ui.theme.RedDark
import com.example.traveling.ui.theme.RedPrimary
import com.example.traveling.ui.theme.Stone400
import com.example.traveling.ui.theme.Stone500
import com.example.traveling.ui.theme.Stone800
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthorProfileUiState {
    data object Loading : AuthorProfileUiState
    data class Success(
        val user: User?,
        val posts: List<PhotoPostUi>,
        val isFollowing: Boolean,
        val isCurrentUser: Boolean
    ) : AuthorProfileUiState
    data class Error(val message: String) : AuthorProfileUiState
}

class AuthorProfileViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = UserRepository(),
    private val postRepository: PhotoPostRepository = PhotoPostRepository(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthorProfileUiState>(AuthorProfileUiState.Loading)
    val uiState: StateFlow<AuthorProfileUiState> = _uiState.asStateFlow()

    private var userListener: ListenerRegistration? = null
    private var postsListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    private var user: User? = null
    private var posts: List<PhotoPostUi> = emptyList()
    private var settings: NotificationSettingsDocument = NotificationSettingsDocument()
    private var authorId: String = ""
    private var postsVersion = 0
    private val optimisticLikeStates = mutableMapOf<String, Boolean>()
    private val optimisticLikeCounts = mutableMapOf<String, Int>()
    private val optimisticSaveStates = mutableMapOf<String, Boolean>()

    fun observe(authorId: String) {
        this.authorId = authorId
        postsVersion += 1
        optimisticLikeStates.clear()
        optimisticLikeCounts.clear()
        optimisticSaveStates.clear()
        _uiState.value = AuthorProfileUiState.Loading
        userListener?.remove()
        postsListener?.remove()
        settingsListener?.remove()

        userListener = userRepository.observeUser(
            userId = authorId,
            onChanged = {
                user = it
                emit()
            },
            onError = { _uiState.value = AuthorProfileUiState.Error(it.localizedMessage ?: "Erreur de chargement du profil") }
        )
        postsListener = postRepository.observeVisiblePublishedPosts(
            userId = auth.currentUser?.uid,
            onChanged = {
                emitPostsFromDocuments(it.filter { doc -> doc.authorId == authorId })
            },
            onError = { _uiState.value = AuthorProfileUiState.Error(it.localizedMessage ?: "Erreur de chargement des posts") }
        )
        auth.currentUser?.uid?.let { uid ->
            settingsListener = notificationRepository.observeNotificationSettings(
                userId = uid,
                onChanged = {
                    settings = it
                    emit()
                },
                onError = { _uiState.value = AuthorProfileUiState.Error(it.localizedMessage ?: "Erreur de chargement des suivis") }
            )
        }
    }

    fun toggleLike(postId: String, currentlyLiked: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        postsVersion += 1
        val nextLiked = !currentlyLiked
        var nextLikeCount = 0
        updatePostLocally(postId) { post ->
            nextLikeCount = (post.likes + if (nextLiked) 1 else -1).coerceAtLeast(0)
            post.copy(isLiked = nextLiked, likes = nextLikeCount)
        }
        optimisticLikeStates[postId] = nextLiked
        optimisticLikeCounts[postId] = nextLikeCount
        viewModelScope.launch {
            val result = runCatching {
                if (currentlyLiked) postRepository.unlikePost(uid, postId) else postRepository.likePost(uid, postId)
            }
            optimisticLikeStates.remove(postId)
            optimisticLikeCounts.remove(postId)
            if (result.isFailure) {
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
                if (currentlySaved) postRepository.unsavePost(uid, postId) else postRepository.savePost(uid, postId, null)
            }
            optimisticSaveStates.remove(postId)
            if (result.isFailure) {
                updatePostLocally(postId) { post ->
                    post.copy(isSaved = currentlySaved)
                }
            }
        }
    }

    fun reportPost(postId: String) {
        viewModelScope.launch {
            postRepository.reportPost(postId = postId, reporterId = auth.currentUser?.uid, reason = "Signalement depuis le profil auteur")
        }
    }

    fun toggleFollow() {
        val uid = auth.currentUser?.uid ?: return
        if (authorId.isBlank() || authorId == uid) return
        val next = if (settings.followedUserIds.contains(authorId)) {
            settings.copy(followedUserIds = settings.followedUserIds - authorId)
        } else {
            settings.copy(followedUserIds = settings.followedUserIds + authorId)
        }
        viewModelScope.launch {
            notificationRepository.updateNotificationSettings(uid, next)
        }
    }

    private fun emitPostsFromDocuments(documents: List<com.example.traveling.data.model.PhotoPostDocument>) {
        val uid = auth.currentUser?.uid
        val version = ++postsVersion
        val previousPostsById = posts.associateBy { it.id }
        if (uid == null) {
            posts = documents.map { doc ->
                val previous = previousPostsById[doc.postId]
                val liked = optimisticLikeStates[doc.postId] ?: previous?.isLiked ?: false
                val saved = optimisticSaveStates[doc.postId] ?: previous?.isSaved ?: false
                val optimisticLikeCount = optimisticLikeCounts[doc.postId]
                doc.toPhotoPostUi(isLiked = liked, isSaved = saved).let { post ->
                    if (optimisticLikeCount != null) post.copy(likes = optimisticLikeCount) else post
                }
            }
            emit()
            return
        }

        viewModelScope.launch {
            posts = documents.map { doc ->
                val liked = runCatching { postRepository.isPostLikedByUser(uid, doc.postId) }.getOrDefault(false)
                val saved = runCatching { postRepository.isPostSavedByUser(uid, doc.postId) }.getOrDefault(false)
                val resolvedLiked = optimisticLikeStates[doc.postId] ?: liked
                val resolvedSaved = optimisticSaveStates[doc.postId] ?: saved
                val optimisticLikeCount = optimisticLikeCounts[doc.postId]
                doc.toPhotoPostUi(isLiked = resolvedLiked, isSaved = resolvedSaved).let { post ->
                    if (optimisticLikeCount != null) post.copy(likes = optimisticLikeCount) else post
                }
            }
            if (version != postsVersion) return@launch
            emit()
        }
    }

    private fun updatePostLocally(postId: String, transform: (PhotoPostUi) -> PhotoPostUi) {
        posts = posts.map { post ->
            if (post.id == postId) transform(post) else post
        }
        emit()
    }

    private fun emit() {
        val uid = auth.currentUser?.uid
        _uiState.value = AuthorProfileUiState.Success(
            user = user,
            posts = posts,
            isFollowing = settings.followedUserIds.contains(authorId),
            isCurrentUser = uid == authorId
        )
    }

    override fun onCleared() {
        userListener?.remove()
        postsListener?.remove()
        settingsListener?.remove()
        super.onCleared()
    }
}

@Composable
fun AuthorProfileScreen(
    authorId: String,
    onBack: () -> Unit,
    onOpenPhotoDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: AuthorProfileViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(authorId) {
        viewModel.observe(authorId)
    }

    when (val current = state) {
        AuthorProfileUiState.Loading -> Box(Modifier.fillMaxSize().background(PageBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RedPrimary)
        }
        is AuthorProfileUiState.Error -> Box(Modifier.fillMaxSize().background(PageBg).padding(24.dp), contentAlignment = Alignment.Center) {
            Text(current.message, color = Stone500)
        }
        is AuthorProfileUiState.Success -> {
            val user = current.user
            val displayName = user?.displayName?.ifBlank { "Voyageur" } ?: "Voyageur"
            Box(Modifier.fillMaxSize().background(PageBg)) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Stone800)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Profil voyageur", color = Stone800, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                avatarUrl = user?.avatarUrl,
                                fallbackText = displayName.firstOrNull()?.uppercase() ?: "V",
                                backgroundColor = RedPrimary,
                                modifier = Modifier.size(72.dp),
                                textSize = 28.sp
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(displayName, color = Stone800, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    user?.bio?.takeIf { it.isNotBlank() } ?: user?.homeCity?.takeIf { it.isNotBlank() } ?: "Voyageur",
                                    color = Stone500,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${current.posts.size} posts visibles", color = RedPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    user?.homeCity?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, color = Stone500, fontSize = 11.sp)
                                    }
                                }
                            }
                            if (!current.isCurrentUser) {
                                if (current.isFollowing) {
                                    OutlinedButton(onClick = { viewModel.toggleFollow() }) {
                                        Text("Ne plus suivre", fontSize = 12.sp)
                                    }
                                } else {
                                    Button(onClick = { viewModel.toggleFollow() }, colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)) {
                                        Text("Suivre", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (current.posts.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Aucun post visible", color = Stone400)
                        }
                    } else {
                        PhotoListView(
                            photos = current.posts,
                            onLike = { postId ->
                                current.posts.find { it.id == postId }?.let { post ->
                                    viewModel.toggleLike(postId, post.isLiked)
                                }
                            },
                            onSave = { postId ->
                                current.posts.find { it.id == postId }?.let { post ->
                                    viewModel.toggleSave(postId, post.isSaved)
                                }
                            },
                            onNavigate = { postId ->
                                current.posts.find { it.id == postId }?.let { post ->
                                    val opened = openNavigationToPlace(
                                        context = context,
                                        placeName = post.location,
                                        latitude = post.latitude,
                                        longitude = post.longitude
                                    )
                                    if (!opened) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Aucune application de carte disponible.")
                                        }
                                    }
                                }
                            },
                            showStories = false,
                            isAnonymous = false,
                            onPhotoClick = onOpenPhotoDetail,
                            onAuthorClick = {},
                            onGroupClick = {}
                        )
                    }
                }
                SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
            }
        }
    }
}
