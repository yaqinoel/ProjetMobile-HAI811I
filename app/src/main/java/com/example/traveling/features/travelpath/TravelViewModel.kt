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

    // ── Attractions suggérées pour le formulaire ──
    private val _suggestedAttractions = MutableStateFlow<List<Attraction>>(emptyList())
    val suggestedAttractions: StateFlow<List<Attraction>> = _suggestedAttractions.asStateFlow()

    // ── Destination & Route sélectionnées ──
    private val _selectedDestination = MutableStateFlow<Destination?>(null)
    val selectedDestination: StateFlow<Destination?> = _selectedDestination.asStateFlow()

    private val _selectedRoute = MutableStateFlow<TravelRoute?>(null)
    val selectedRoute: StateFlow<TravelRoute?> = _selectedRoute.asStateFlow()

    // ── Loading state ──
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── État de navigation de l'écran (persiste entre les changements de tab) ──
    private val _currentStep = MutableStateFlow("preferences") // preferences, loading, results, detail
    val currentStep: StateFlow<String> = _currentStep.asStateFlow()

    private val _currentRouteId = MutableStateFlow<String?>(null)
    val currentRouteId: StateFlow<String?> = _currentRouteId.asStateFlow()

    fun setStep(step: String) { _currentStep.value = step }
    fun setCurrentRouteId(id: String?) { _currentRouteId.value = id }

    // ── État du formulaire (persiste entre retour résultats → formulaire) ──
    val formDestination = MutableStateFlow("")
    val formActivities = MutableStateFlow(setOf<String>())
    val formBudget = MutableStateFlow(600f)
    val formDuration = MutableStateFlow(5f)
    val formEffort = MutableStateFlow(3f)
    val formFavoritePlaces = MutableStateFlow(listOf<String>())

    // ── Validation destination ──
    private val _destinationNotFound = MutableStateFlow(false)
    val destinationNotFound: StateFlow<Boolean> = _destinationNotFound.asStateFlow()

    /** Vérifie si la destination saisie a des données */
    fun checkDestination(name: String) {
        if (name.isBlank()) {
            _destinationNotFound.value = false
            return
        }
        val found = _destinations.value.any { it.name.equals(name, ignoreCase = true) }
        _destinationNotFound.value = !found
    }

    // ── Mapping activités UI → types Firestore ──
    private val activityTypeMapping = mapOf(
        "culture" to listOf("Culture", "Monument"),
        "food" to listOf("Gastronomie"),
        "nature" to listOf("Nature"),
        "leisure" to listOf("Loisirs"),
        "shopping" to listOf("Shopping"),
        "nightlife" to listOf("Loisirs"),
        "sport" to listOf("Nature", "Loisirs"),
        "photo" to listOf("Photo", "Nature", "Monument")
    )

    // ── Données de filtrage sauvegardées ──
    private var savedFavoritePlaces: List<String> = emptyList()
    private var savedActivities: Set<String> = emptySet()

    // ── Attractions par route ──
    private var routeAttractionsMap: Map<String, List<Attraction>> = emptyMap()

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

    fun selectDestination(
        destinationName: String,
        budget: Int = 5000,
        activities: Set<String> = emptySet(),
        durationHours: Int = 8,
        effort: Int = 3,
        favoritePlaces: List<String> = emptyList()
    ) {
        val dest = _destinations.value.find {
            it.name.equals(destinationName, ignoreCase = true)
        } ?: return

        _selectedDestination.value = dest
        savedFavoritePlaces = favoritePlaces
        savedActivities = activities

        viewModelScope.launch {
            _isLoading.value = true
            repository.getAttractionsByDestination(dest.id).collect { allAttractions ->
                _attractions.value = allAttractions

                // ── Filtrage strict → relaxé ──
                val filtered = filterAttractions(allAttractions, budget, activities, effort)

                // Générer les 3 routes avec des sous-ensembles différents
                _routes.value = generateRoutes(filtered, allAttractions, dest, budget, durationHours)

                // Précharger les stops de la première route
                if (_routes.value.isNotEmpty()) {
                    val firstRouteAttractions = routeAttractionsMap[_routes.value[0].id] ?: emptyList()
                    _routeStops.value = generateStops(firstRouteAttractions)
                }

                _isLoading.value = false
            }
        }
    }

    fun updateSuggestedAttractions(cityName: String) {
        val dest = _destinations.value.find {
            it.name.equals(cityName, ignoreCase = true)
        }
        if (dest == null) {
            _suggestedAttractions.value = emptyList()
            return
        }

        viewModelScope.launch {
            repository.getAttractionsByDestination(dest.id).collect { list ->
                _suggestedAttractions.value = list.sortedByDescending { it.rating }.take(5)
            }
        }
    }
    //  FILTERING LOGIC
    private fun filterAttractions(
        all: List<Attraction>,
        budget: Int,
        activities: Set<String>,
        effort: Int
    ): List<Attraction> {
        // Déterminer les types d'attractions recherchés
        val wantedTypes = activities.flatMap { activityTypeMapping[it] ?: emptyList() }.toSet()

        // Étape 1 : filtre strict (type + coût individuel ≤ budget + effort)
        var result = all.filter { attr ->
            (wantedTypes.isEmpty() || attr.type in wantedTypes) &&
            attr.cost <= budget &&
            attr.effortLevel <= effort + 1
        }

        // Étape 2 : relâcher le filtre de type si trop peu de résultats
        if (result.size < 3) {
            result = all.filter { attr ->
                attr.cost <= budget &&
                attr.effortLevel <= effort + 2
            }
        }

        // Étape 3 : en dernier recours, prendre les 5 meilleurs par note
        if (result.size < 3) {
            result = all.sortedByDescending { it.rating }.take(5)
        }

        // Prioriser les lieux favoris de l'utilisateur
        if (savedFavoritePlaces.isNotEmpty()) {
            val favorites = result.filter { attr ->
                savedFavoritePlaces.any { fav ->
                    attr.name.contains(fav, ignoreCase = true) || fav.contains(attr.name, ignoreCase = true)
                }
            }
            val others = result.filter { it !in favorites }
            result = favorites + others
        }

        return result.sortedByDescending { it.rating }
    }

    private fun generateRoutes(
        filtered: List<Attraction>,
        allAttractions: List<Attraction>,
        dest: Destination,
        budget: Int,
        durationHours: Int
    ): List<TravelRoute> {
        if (filtered.isEmpty()) return emptyList()

        val maxMinutes = durationHours * 60
        val wantedTypes = savedActivities.flatMap { activityTypeMapping[it] ?: emptyList() }.toSet()

        // Séparer les attractions correspondant aux préférences
        val prefMatched = filtered.filter { it.type in wantedTypes }
        val prefOthers = filtered.filter { it.type !in wantedTypes }

        val usedIds = mutableSetOf<String>() // Pour éviter les doublons entre routes

        // ── Route 1 : Économique ──
        val cheapPool = filtered.sortedBy { it.cost }
        val cheapAttractions = selectWithinConstraints(cheapPool, budget, maxMinutes, wantedTypes, usedIds)
        usedIds.addAll(cheapAttractions.map { it.id })

        // ── Route 2 : Équilibrée ──
        val balancedPool = filtered.sortedByDescending { it.rating / (it.cost.coerceAtLeast(1).toDouble()) }
        val balancedAttractions = selectWithinConstraints(balancedPool, budget, maxMinutes, wantedTypes, usedIds)
        usedIds.addAll(balancedAttractions.map { it.id })

        // ── Route 3 : Premium (pool élargi, budget +50%) ──
        val premiumPool = allAttractions.sortedByDescending { it.rating }
        val premiumAttractions = selectWithinConstraints(premiumPool, (budget * 1.5).toInt(), (maxMinutes * 1.3).toInt(), wantedTypes, usedIds)

        // Stocker les associations route → attractions
        routeAttractionsMap = mapOf(
            "1" to cheapAttractions,
            "2" to balancedAttractions,
            "3" to premiumAttractions
        )

        val routes = mutableListOf<TravelRoute>()

        if (cheapAttractions.isNotEmpty()) {
            routes.add(buildTravelRoute("1", "Route Économique", "Budget maîtrisé", cheapAttractions, dest,
                "Modéré", 3, Pair(0xFF10B981, 0xFF0D9488)))
        }
        if (balancedAttractions.isNotEmpty()) {
            routes.add(buildTravelRoute("2", "Route Équilibrée", "Meilleur rapport qualité-prix", balancedAttractions, dest,
                "Facile", 2, Pair(0xFFB91C1C, 0xFF991B1B)))
        }
        if (premiumAttractions.isNotEmpty()) {
            routes.add(buildTravelRoute("3", "Route Premium", "Expérience haut de gamme", premiumAttractions, dest,
                "Très facile", 1, Pair(0xFFF59E0B, 0xFFB45309)))
        }

        return routes
    }

    private fun selectWithinConstraints(
        sorted: List<Attraction>,
        maxBudget: Int,
        maxMinutes: Int,
        wantedTypes: Set<String>,
        excludeIds: Set<String>
    ): List<Attraction> {
        // Filtrer les attractions déjà prises par d'autres routes
        val available = sorted.filter { it.id !in excludeIds }

        val selected = mutableListOf<Attraction>()
        var totalCost = 0
        var totalMinutes = 0
        var hasPrefMatch = false

        // D'abord, garantir au moins une attraction de la préférence
        if (wantedTypes.isNotEmpty()) {
            val prefFirst = available.firstOrNull { it.type in wantedTypes }
            if (prefFirst != null) {
                selected.add(prefFirst)
                totalCost += prefFirst.cost
                totalMinutes += prefFirst.duration
                hasPrefMatch = true
            }
        }

        // Ensuite, compléter avec le reste
        for (attr in available) {
            if (attr in selected) continue // Déjà ajouté
            val transitTime = if (selected.isEmpty()) 0 else 15
            val newTotalMinutes = totalMinutes + attr.duration + transitTime
            val newTotalCost = totalCost + attr.cost

            if (newTotalCost <= maxBudget && newTotalMinutes <= maxMinutes) {
                selected.add(attr)
                totalCost = newTotalCost
                totalMinutes = newTotalMinutes
                if (attr.type in wantedTypes) hasPrefMatch = true
            }

            if (selected.size >= 8) break
        }

        return selected
    }

    private fun buildTravelRoute(
        id: String, name: String, subtitle: String,
        attractions: List<Attraction>, dest: Destination,
        effort: String, effortLevel: Int, colors: Pair<Long, Long>
    ): TravelRoute {
        val totalMinutes = attractions.sumOf { it.duration } + (attractions.size - 1).coerceAtLeast(0) * 15
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        val durationStr = if (mins > 0) "${hours}h${"%02d".format(mins)}" else "${hours}h"

        return TravelRoute(
            id = id, name = name, subtitle = subtitle,
            budget = attractions.sumOf { it.cost },
            duration = durationStr,
            effort = effort, effortLevel = effortLevel,
            stops = attractions.size,
            rating = (attractions.map { it.rating }.average() * 10).toInt() / 10f,
            reviews = (50..300).random(),
            imageUrl = attractions.firstOrNull()?.imageUrl ?: dest.imageUrl,
            highlights = attractions.map { it.name },
            gradientColors = colors
        )
    }

    /** Sélectionne une route et génère les stops correspondants. */
    fun selectRoute(routeId: String) {
        _selectedRoute.value = _routes.value.find { it.id == routeId }
        val routeAttractions = routeAttractionsMap[routeId] ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _routeStops.value = generateStops(routeAttractions)
            _isLoading.value = false
        }
    }

    //  STOP GENERATION (SCHEDULING)

    private suspend fun generateStops(attractions: List<Attraction>): List<RouteStop> {
        if (attractions.isEmpty()) return emptyList()

        // Trier par créneaux horaires pour un planning logique
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

        val basicStops = ordered.mapIndexed { index, attr ->
            // ── Distance & temps de marche ──
            var distString = "Départ"
            var walkStr = ""
            var walkMinutes = 0

            if (index > 0) {
                val prev = ordered[index - 1]
                if (prev.lat != 0.0 && attr.lat != 0.0) {
                    val distKm = haversine(prev.lat, prev.lng, attr.lat, attr.lng)
                    distString = if (distKm < 1.0) "${(distKm * 1000).toInt()} m" else "%.1f km".format(distKm)
                    walkMinutes = (distKm * 12).toInt().coerceAtLeast(5)
                    walkStr = "~$walkMinutes min"
                } else {
                    distString = "~2 km"
                    walkMinutes = 15
                    walkStr = "~15 min"
                }

                // Ajouter le temps de marche pour l'arrivée
                currentMinute += walkMinutes
                currentHour += currentMinute / 60
                currentMinute %= 60
            }

            val timeSlot = when {
                currentHour < 12 -> TimeSlot.MATIN
                currentHour < 18 -> TimeSlot.APRES_MIDI
                else -> TimeSlot.SOIR
            }

            val arrivalTime = "%02d:%02d".format(currentHour, currentMinute)
            val durationStr = if (attr.duration >= 60)
                "${attr.duration / 60}h${if (attr.duration % 60 > 0) "%02d".format(attr.duration % 60) else ""}"
            else "${attr.duration} min"

            val stop = RouteStop(
                id = "s${index + 1}",
                name = attr.name,
                type = attr.type,
                timeSlot = timeSlot,
                arrivalTime = arrivalTime,
                duration = durationStr,
                distance = distString,
                walkTime = walkStr,
                cost = attr.cost,
                description = attr.description,
                imageUrl = attr.imageUrl,
                rating = attr.rating.toFloat(),
                openHours = attr.openHours,
                lat = attr.lat,
                lng = attr.lng
            )

            // Avancer l'heure : UNIQUEMENT la durée de la visite
            currentMinute += attr.duration
            currentHour += currentMinute / 60
            currentMinute %= 60

            stop
        }
        
        // ── Real Directions via Google Maps API ──
        try {
            val apiKey = com.example.traveling.BuildConfig.MAPS_API_KEY
            val directionsService = com.example.traveling.data.repository.GoogleDirectionsService()
            val directions = directionsService.getDirections(basicStops, apiKey)
            
            if (directions.size == basicStops.size - 1) {
                return basicStops.mapIndexed { index, stop ->
                    if (index > 0) {
                        val dir = directions[index - 1]
                        stop.copy(
                            distance = dir.distanceText,
                            walkTime = dir.durationText,
                            polylineToNext = dir.polyline
                        )
                    } else {
                        stop
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return basicStops
    }

    /**
     * Calcule la distance géolocalisée (Haversine) entre deux points en kilomètres.
     */
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
