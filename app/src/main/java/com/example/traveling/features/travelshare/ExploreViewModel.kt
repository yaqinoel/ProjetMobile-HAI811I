package com.example.traveling.features.travelshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.model.Destination
import com.example.traveling.data.repository.TravelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pour l'écran Explore.
 * Fournit les destinations depuis Firestore.
 */
class ExploreViewModel : ViewModel() {

    private val repository = TravelRepository()

    private val _destinations = MutableStateFlow<List<Destination>>(emptyList())
    val destinations: StateFlow<List<Destination>> = _destinations.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getDestinations().collect { list ->
                _destinations.value = list
                _isLoading.value = false
            }
        }
    }
}
