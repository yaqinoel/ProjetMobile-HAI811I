package com.example.traveling.features.travelshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.model.GroupDocument
import com.example.traveling.data.repository.CreateGroupInput
import com.example.traveling.data.repository.GroupRepository
import com.example.traveling.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupsUiState(
    val isLoading: Boolean = true,
    val myGroups: List<GroupDocument> = emptyList(),
    val discoverGroups: List<GroupDocument> = emptyList(),
    val errorMessage: String? = null
)

class GroupsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = UserRepository(),
    private val repository: GroupRepository = GroupRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var myGroupsListener: ListenerRegistration? = null
    private var discoverGroupsListener: ListenerRegistration? = null

    fun observeGroups() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = GroupsUiState(isLoading = false, errorMessage = "Utilisateur non connecté")
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        myGroupsListener?.remove()
        discoverGroupsListener?.remove()

        // deux listes différentes: groupes rejoints et groupes proposés
        myGroupsListener = repository.observeMyGroups(
            userId = uid,
            onChanged = { groups ->
                _uiState.update { it.copy(myGroups = groups, isLoading = false) }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Erreur de chargement des groupes")
                }
            }
        )

        discoverGroupsListener = repository.observeDiscoverGroups(
            userId = uid,
            onChanged = { groups ->
                _uiState.update { it.copy(discoverGroups = groups, isLoading = false) }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Erreur de chargement des groupes")
                }
            }
        )
    }

    fun createGroup(name: String, description: String, isPrivate: Boolean) {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            val user = userRepository.getUser(currentUser.uid)
            // le créateur devient aussi membre du groupe côté repository
            val ownerName = user?.displayName
                ?.takeIf { it.isNotBlank() }
                ?: currentUser.displayName
                ?.takeIf { it.isNotBlank() }
                ?: currentUser.email?.substringBefore('@')
                ?: "Voyageur"

            repository.createGroup(
                CreateGroupInput(
                    name = name,
                    description = description,
                    ownerId = currentUser.uid,
                    ownerName = ownerName,
                    ownerAvatarUrl = user?.avatarUrl,
                    visibility = if (isPrivate) "private" else "public"
                )
            ).onSuccess {
                _events.emit("Groupe créé")
            }.onFailure {
                _events.emit(it.localizedMessage ?: "Échec de création du groupe")
            }
        }
    }

    fun joinGroup(groupId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.joinGroup(uid, groupId)
                .onSuccess { _events.emit("Groupe rejoint") }
                .onFailure { _events.emit(it.localizedMessage ?: "Impossible de rejoindre le groupe") }
        }
    }

    fun leaveGroup(groupId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.leaveGroup(uid, groupId)
                .onSuccess { _events.emit("Groupe quitté") }
                .onFailure { _events.emit(it.localizedMessage ?: "Impossible de quitter le groupe") }
        }
    }

    override fun onCleared() {
        myGroupsListener?.remove()
        discoverGroupsListener?.remove()
        myGroupsListener = null
        discoverGroupsListener = null
        super.onCleared()
    }
}
