package com.example.traveling.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traveling.data.model.NotificationSettingsDocument
import com.example.traveling.data.model.User
import com.example.traveling.data.repository.NotificationRepository
import com.example.traveling.data.repository.UserRepository
import com.example.traveling.ui.components.UserAvatar
import com.example.traveling.ui.theme.ProfileCardBg
import com.example.traveling.ui.theme.ProfilePageBg
import com.example.traveling.ui.theme.RedPrimary
import com.example.traveling.ui.theme.Stone500
import com.example.traveling.ui.theme.Stone800
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FollowingUiState {
    data object Loading : FollowingUiState
    data class Success(val settings: NotificationSettingsDocument, val users: List<User>) : FollowingUiState
    data class Error(val message: String) : FollowingUiState
}

class FollowingManagementViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val notificationRepository: NotificationRepository = NotificationRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<FollowingUiState>(FollowingUiState.Loading)
    val uiState: StateFlow<FollowingUiState> = _uiState.asStateFlow()
    private var listener: ListenerRegistration? = null

    fun observe() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = FollowingUiState.Error("Utilisateur non connecté")
            return
        }
        listener?.remove()
        listener = notificationRepository.observeNotificationSettings(
            userId = uid,
            onChanged = { settings ->
                viewModelScope.launch {
                    // les suivis stockent des ids, donc on recharge les profils pour l'affichage
                    val users = runCatching { userRepository.getUsers(settings.followedUserIds) }.getOrDefault(emptyList())
                    _uiState.value = FollowingUiState.Success(settings, users)
                }
            },
            onError = { _uiState.value = FollowingUiState.Error(it.localizedMessage ?: "Erreur de chargement des suivis") }
        )
    }

    fun unfollowUser(userId: String) {
        val uid = auth.currentUser?.uid ?: return
        val current = (uiState.value as? FollowingUiState.Success)?.settings ?: return
        viewModelScope.launch {
            notificationRepository.updateNotificationSettings(uid, current.copy(followedUserIds = current.followedUserIds - userId))
        }
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}

@Composable
fun FollowingManagementScreen(
    onBack: () -> Unit,
    onOpenAuthorProfile: (String) -> Unit
) {
    val viewModel: FollowingManagementViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.observe() }

    Column(Modifier.fillMaxSize().background(ProfilePageBg)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Stone800)
            }
            Spacer(Modifier.width(8.dp))
            Text("Suivis", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Stone800)
        }

        when (val current = state) {
            FollowingUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RedPrimary)
            }
            is FollowingUiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(current.message, color = Stone500)
            }
            is FollowingUiState.Success -> {
                val idsWithoutProfiles = current.settings.followedUserIds.filterNot { id -> current.users.any { it.userId == id } }

                if (current.settings.followedUserIds.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucun voyageur suivi", color = Stone500)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { SectionTitle("Voyageurs suivis") }
                        items(current.users, key = { it.userId }) { user ->
                            FollowedUserRow(
                                displayName = user.displayName.ifBlank { "Voyageur" },
                                subtitle = user.homeCity?.takeIf { it.isNotBlank() } ?: user.email,
                                avatarUrl = user.avatarUrl,
                                onOpen = { onOpenAuthorProfile(user.userId) },
                                onRemove = { viewModel.unfollowUser(user.userId) }
                            )
                        }
                        items(idsWithoutProfiles, key = { it }) { userId ->
                            FollowedUserRow(
                                displayName = userId,
                                subtitle = "Profil non chargé",
                                avatarUrl = null,
                                onOpen = { onOpenAuthorProfile(userId) },
                                onRemove = { viewModel.unfollowUser(userId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        color = Stone500,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun FollowedUserRow(
    displayName: String,
    subtitle: String,
    avatarUrl: String?,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProfileCardBg, RoundedCornerShape(12.dp))
            .clickable { onOpen() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            avatarUrl = avatarUrl,
            fallbackText = displayName.firstOrNull()?.uppercase() ?: "V",
            backgroundColor = RedPrimary,
            modifier = Modifier.size(42.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(displayName, color = Stone800, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Stone500, fontSize = 11.sp)
        }
        Icon(Icons.Default.Person, null, tint = RedPrimary, modifier = Modifier.size(18.dp))
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, null, tint = Stone500, modifier = Modifier.size(18.dp))
        }
    }
}
