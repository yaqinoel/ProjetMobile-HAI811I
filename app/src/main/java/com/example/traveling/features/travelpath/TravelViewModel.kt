package com.example.traveling.features.travelpath

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.model.Attraction
import com.example.traveling.data.model.Destination
import com.example.traveling.data.model.RouteStop
import com.example.traveling.data.model.TimeSlot
import com.example.traveling.data.model.TravelRoute
import com.example.traveling.data.repository.TravelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel partagé entre TravelPathScreen et RouteDetailScreen.
 * Charge les destinations et attractions depuis Firestore.
 */
class TravelViewModel : ViewModel() {

    private val repository = TravelRepository()

    // ── Destinations ──
    private val _destinations = MutableStateFlow<List<Destination>>(emptyList())
    val destinations: StateFlow<List<Destination>> = _destinations.asStateFlow()

    /** Noms des villes pour le sélecteur rapide */
    private val _quickCities = MutableStateFlow<List<String>>(emptyList())
    val quickCities: StateFlow<List<String>> = _quickCities.asStateFlow()

    // ── Attractions de la destination sélectionnée ──
    private val _attractions = MutableStateFlow<List<Attraction>>(emptyList())
    val attractions: StateFlow<List<Attraction>> = _attractions.asStateFlow()

    // ── Routes générées ──
    private val _routes = MutableStateFlow<List<TravelRoute>>(emptyList())
    val routes: StateFlow<List<TravelRoute>> = _routes.asStateFlow()

    // ── Stops pour RouteDetail ──
    private val _routeStops = MutableStateFlow<List<RouteStop>>(emptyList())
    val routeStops: StateFlow<List<RouteStop>> = _routeStops.asStateFlow()

    // ── Loading state ──
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadDestinations()
    }

    private fun loadDestinations() {
        viewModelScope.launch {
            repository.getDestinations().collect { list ->
                _destinations.value = list
                _quickCities.value = list.map { it.name }
            }
        }
    }

    /**
     * Charge les attractions d'une destination et génère des routes.
     */
    fun selectDestination(destinationName: String) {
        val dest = _destinations.value.find {
            it.name.equals(destinationName, ignoreCase = true)
        } ?: return

        viewModelScope.launch {
            _isLoading.value = true
            repository.getAttractionsByDestination(dest.id).collect { list ->
                _attractions.value = list
                _routes.value = generateRoutes(list, dest)
                _routeStops.value = generateStops(list)
                _isLoading.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════════
    //  ROUTE GENERATION LOGIC
    // ═══════════════════════════════════════════════════

    /**
     * Génère 3 routes (Économique, Équilibrée, Premium) à partir des attractions.
     */
    private fun generateRoutes(attractions: List<Attraction>, dest: Destination): List<TravelRoute> {
        if (attractions.isEmpty()) return emptyList()

        val sorted = attractions.sortedBy { it.cost }
        val cheap = sorted.take((sorted.size * 0.5).toInt().coerceAtLeast(3))
        val mid = sorted.take((sorted.size * 0.75).toInt().coerceAtLeast(4))
        val all = sorted

        return listOf(
            TravelRoute(
                id = "1", name = "Route Économique",
                subtitle = "Pour voyageurs avec budget limité",
                budget = cheap.sumOf { it.cost },
                duration = "${cheap.sumOf { it.duration } / 60}-${cheap.sumOf { it.duration } / 60 + 1} heures",
                effort = "Modéré", effortLevel = 3,
                stops = cheap.size, rating = (cheap.map { it.rating }.average() * 10).toInt() / 10f,
                reviews = 128,
                imageUrl = dest.imageUrl,
                highlights = cheap.map { it.name },
                gradientColors = Pair(0xFF10B981, 0xFF0D9488)
            ),
            TravelRoute(
                id = "2", name = "Route Équilibrée",
                subtitle = "Meilleur rapport qualité-prix",
                budget = mid.sumOf { it.cost },
                duration = "${mid.sumOf { it.duration } / 60}-${mid.sumOf { it.duration } / 60 + 1} heures",
                effort = "Facile", effortLevel = 2,
                stops = mid.size, rating = (mid.map { it.rating }.average() * 10).toInt() / 10f,
                reviews = 256,
                imageUrl = if (mid.isNotEmpty()) mid[0].imageUrl else dest.imageUrl,
                highlights = mid.map { it.name },
                gradientColors = Pair(0xFFB91C1C, 0xFF991B1B)
            ),
            TravelRoute(
                id = "3", name = "Route Premium",
                subtitle = "Expérience haut de gamme",
                budget = all.sumOf { it.cost },
                duration = "${all.sumOf { it.duration } / 60}-${all.sumOf { it.duration } / 60 + 1} heures",
                effort = "Très facile", effortLevel = 1,
                stops = all.size, rating = (all.map { it.rating }.average() * 10).toInt() / 10f,
                reviews = 89,
                imageUrl = if (all.size > 1) all[1].imageUrl else dest.imageUrl,
                highlights = all.map { it.name },
                gradientColors = Pair(0xFFF59E0B, 0xFFB45309)
            )
        )
    }

    /**
     * Convertit les attractions en RouteStops avec des time slots assignés automatiquement.
     */
    private fun generateStops(attractions: List<Attraction>): List<RouteStop> {
        if (attractions.isEmpty()) return emptyList()

        // Trier par bestTimeSlots pour un planning logique
        val ordered = attractions.sortedBy { attr ->
            when {
                attr.bestTimeSlots.contains("matin") -> 0
                attr.bestTimeSlots.contains("apres-midi") -> 1
                attr.bestTimeSlots.contains("soir") -> 2
                else -> 1
            }
        }

        var currentHour = 9
        var currentMinute = 0

        return ordered.mapIndexed { index, attr ->
            val timeSlot = when {
                attr.bestTimeSlots.contains("matin") && currentHour < 12 -> TimeSlot.MATIN
                attr.bestTimeSlots.contains("soir") || currentHour >= 18 -> TimeSlot.SOIR
                else -> TimeSlot.APRES_MIDI
            }

            val arrivalTime = "%02d:%02d".format(currentHour, currentMinute)
            val durationStr = if (attr.duration >= 60) "${attr.duration / 60}h${if (attr.duration % 60 > 0) "%02d".format(attr.duration % 60) else ""}"
                              else "${attr.duration} min"

            val stop = RouteStop(
                id = "s${index + 1}",
                name = attr.name,
                type = attr.type,
                timeSlot = timeSlot,
                arrivalTime = arrivalTime,
                duration = durationStr,
                distance = if (index == 0) "Départ" else "~2km",
                walkTime = if (index == 0) "" else "~10 min",
                cost = attr.cost,
                description = attr.description,
                imageUrl = attr.imageUrl,
                rating = attr.rating.toFloat(),
                openHours = attr.openHours
            )

            // Avancer l'heure pour le prochain stop
            currentMinute += attr.duration + 30 // 30 min de transit
            currentHour += currentMinute / 60
            currentMinute %= 60

            stop
        }
    }
}
