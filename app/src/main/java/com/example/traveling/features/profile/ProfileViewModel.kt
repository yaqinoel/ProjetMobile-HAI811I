package com.example.traveling.features.profile

import androidx.lifecycle.ViewModel
import com.example.traveling.data.model.NotificationSettingsDocument
import com.example.traveling.data.model.User
import com.example.traveling.data.repository.NotificationRepository
import com.example.traveling.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: User, val followedUserCount: Int = 0) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

class ProfileViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val repository: UserRepository = UserRepository(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var userListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    private var currentUser: User? = null
    private var currentSettings: NotificationSettingsDocument = NotificationSettingsDocument()

    fun observeCurrentUser() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = ProfileUiState.Error("Utilisateur non connecté")
            return
        }

        _uiState.value = ProfileUiState.Loading
        userListener?.remove()
        settingsListener?.remove()
        userListener = repository.observeUser(
            userId = uid,
            onChanged = { user ->
                currentUser = user
                emitSuccessOrError()
            },
            onError = { error ->
                _uiState.value = ProfileUiState.Error(error.localizedMessage ?: "Erreur de lecture du profil")
            }
        )
        settingsListener = notificationRepository.observeNotificationSettings(
            userId = uid,
            onChanged = { settings ->
                currentSettings = settings
                if (currentUser != null) emitSuccessOrError()
            },
            onError = { error ->
                _uiState.value = ProfileUiState.Error(error.localizedMessage ?: "Erreur de lecture des suivis")
            }
        )
    }

    private fun emitSuccessOrError() {
        val user = currentUser
        _uiState.value = if (user != null) {
            ProfileUiState.Success(
                user = user,
                followedUserCount = currentSettings.followedUserIds.size
            )
        } else {
            ProfileUiState.Error("Profil utilisateur introuvable")
        }
    }

    override fun onCleared() {
        userListener?.remove()
        settingsListener?.remove()
        userListener = null
        settingsListener = null
        super.onCleared()
    }
}
