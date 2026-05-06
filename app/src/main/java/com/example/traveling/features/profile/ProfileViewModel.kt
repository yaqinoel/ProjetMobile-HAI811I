package com.example.traveling.features.profile

import androidx.lifecycle.ViewModel
import com.example.traveling.data.model.User
import com.example.traveling.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: User) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

class ProfileViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var userListener: ListenerRegistration? = null

    fun observeCurrentUser() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = ProfileUiState.Error("Utilisateur non connecté")
            return
        }

        _uiState.value = ProfileUiState.Loading
        userListener?.remove()
        userListener = repository.observeUser(
            userId = uid,
            onChanged = { user ->
                _uiState.value = if (user != null) {
                    ProfileUiState.Success(user)
                } else {
                    ProfileUiState.Error("Profil utilisateur introuvable")
                }
            },
            onError = { error ->
                _uiState.value = ProfileUiState.Error(error.localizedMessage ?: "Erreur de lecture du profil")
            }
        )
    }

    override fun onCleared() {
        userListener?.remove()
        userListener = null
        super.onCleared()
    }
}
