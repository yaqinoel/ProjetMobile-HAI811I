package com.example.traveling.features.travelshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.model.NotificationDocument
import com.example.traveling.data.model.NotificationSettingsDocument
import com.example.traveling.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Success(
        val notifications: List<NotificationDocument>,
        val settings: NotificationSettingsDocument
    ) : NotificationsUiState

    data class Error(val message: String) : NotificationsUiState
}

class NotificationsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val repository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var notificationsListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null

    private var cachedNotifications: List<NotificationDocument> = emptyList()
    private var cachedSettings: NotificationSettingsDocument = NotificationSettingsDocument()

    fun observeNotifications() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = NotificationsUiState.Error("Utilisateur non connecté")
            return
        }

        _uiState.value = NotificationsUiState.Loading
        notificationsListener?.remove()
        settingsListener?.remove()

        // les notifications et les réglages arrivent séparément mais alimentent le même écran
        notificationsListener = repository.observeMyNotifications(
            userId = uid,
            onChanged = { list ->
                cachedNotifications = list
                _uiState.value = NotificationsUiState.Success(cachedNotifications, cachedSettings)
            },
            onError = { error ->
                _uiState.value = NotificationsUiState.Error(error.localizedMessage ?: "Erreur de chargement des notifications")
            }
        )

        settingsListener = repository.observeNotificationSettings(
            userId = uid,
            onChanged = { settings ->
                cachedSettings = settings
                _uiState.value = NotificationsUiState.Success(cachedNotifications, cachedSettings)
            },
            onError = { error ->
                _uiState.value = NotificationsUiState.Error(error.localizedMessage ?: "Erreur de chargement des paramètres")
            }
        )
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.markAllAsRead(uid)
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId)
        }
    }

    fun updateSettings(settings: NotificationSettingsDocument) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.updateNotificationSettings(uid, settings)
        }
    }

    fun toggleFollowedTag(tag: String) {
        val current = (uiState.value as? NotificationsUiState.Success)?.settings ?: return
        // toggle simple pour les tags suivis
        val next = if (current.followedTags.any { it.equals(tag, ignoreCase = true) }) {
            current.copy(followedTags = current.followedTags.filterNot { it.equals(tag, ignoreCase = true) })
        } else {
            current.copy(followedTags = current.followedTags + tag)
        }
        updateSettings(next)
    }

    fun toggleFollowedPlaceType(placeType: String) {
        val current = (uiState.value as? NotificationsUiState.Success)?.settings ?: return
        val next = if (current.followedPlaceTypes.any { it.equals(placeType, ignoreCase = true) }) {
            current.copy(followedPlaceTypes = current.followedPlaceTypes.filterNot { it.equals(placeType, ignoreCase = true) })
        } else {
            current.copy(followedPlaceTypes = current.followedPlaceTypes + placeType)
        }
        updateSettings(next)
    }

    fun toggleGroupNotification(groupId: String) {
        val current = (uiState.value as? NotificationsUiState.Success)?.settings ?: return
        val nextList = if (current.followedGroupIds.contains(groupId)) {
            current.followedGroupIds - groupId
        } else {
            current.followedGroupIds + groupId
        }
        updateSettings(current.copy(followedGroupIds = nextList))
    }

    fun toggleUserNotification(userId: String) {
        val current = (uiState.value as? NotificationsUiState.Success)?.settings ?: return
        val nextList = if (current.followedUserIds.contains(userId)) {
            current.followedUserIds - userId
        } else {
            current.followedUserIds + userId
        }
        updateSettings(current.copy(followedUserIds = nextList))
    }

    override fun onCleared() {
        notificationsListener?.remove()
        settingsListener?.remove()
        notificationsListener = null
        settingsListener = null
        super.onCleared()
    }
}
