package com.example.traveling.features.profile

import androidx.lifecycle.ViewModel
import com.example.traveling.data.model.PhotoPostDocument
import com.example.traveling.data.repository.PhotoPostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface MyPublishedUiState {
    data object Loading : MyPublishedUiState
    data class Success(val posts: List<PhotoPostDocument>) : MyPublishedUiState
    data class Error(val message: String) : MyPublishedUiState
}

class MyPublishedPostsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val repository: PhotoPostRepository = PhotoPostRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyPublishedUiState>(MyPublishedUiState.Loading)
    val uiState: StateFlow<MyPublishedUiState> = _uiState.asStateFlow()

    private var listener: ListenerRegistration? = null

    fun observeMyPosts() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = MyPublishedUiState.Error("Utilisateur non connecté")
            return
        }

        listener?.remove()
        _uiState.value = MyPublishedUiState.Loading

        listener = repository.observeMyPublishedPosts(
            userId = uid,
            onChanged = { documents ->
                _uiState.value = MyPublishedUiState.Success(documents)
            },
            onError = { error ->
                _uiState.value = MyPublishedUiState.Error(
                    error.localizedMessage ?: "Erreur de chargement de mes publications"
                )
            }
        )
    }

    override fun onCleared() {
        listener?.remove()
        listener = null
        super.onCleared()
    }
}
