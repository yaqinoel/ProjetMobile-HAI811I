package com.example.traveling.features.travelpath

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.model.Attraction
import com.example.traveling.data.model.Destination
import com.example.traveling.data.model.PhotoPostDocument
import com.example.traveling.data.model.RouteStop
import com.example.traveling.data.model.TimeSlot
import com.example.traveling.data.model.TravelRoute
import com.example.traveling.data.model.TravelShareAttractionDocument
import com.example.traveling.data.repository.LocalStorageRepository
import com.example.traveling.data.repository.OpenMeteoService
import com.example.traveling.data.repository.PdfExportService
import com.example.traveling.data.repository.PhotoPostRepository
import com.example.traveling.data.repository.SavedRoutesRepository
import com.example.traveling.data.repository.TravelBridgeRepository
import com.example.traveling.data.repository.TravelRepository
import com.example.traveling.data.repository.WeatherInfo
import com.example.traveling.features.main.TravelPathSeed
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.io.File

class TravelViewModel : ViewModel() {

    private val repository = TravelRepository()
    private val weatherService = OpenMeteoService()
    private val pdfService = PdfExportService()
    private var localStorage: LocalStorageRepository? = null
    private val savedRoutesRepo = SavedRoutesRepository()
    private val photoPostRepository = PhotoPostRepository()
    private val travelBridgeRepository = TravelBridgeRepository()

    /** Must be called once from a Composable with LocalContext */
    fun initLocalStorage(context: Context) {
        if (localStorage == null) {
            localStorage = LocalStorageRepository(context.applicationContext)
        }
    }

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
    private val _stopTravelSharePhotos = MutableStateFlow<Map<String, List<PhotoPostDocument>>>(emptyMap())
    val stopTravelSharePhotos: StateFlow<Map<String, List<PhotoPostDocument>>> = _stopTravelSharePhotos.asStateFlow()

    // ── Attractions suggérées pour le formulaire ──
    private val _suggestedAttractions = MutableStateFlow<List<Attraction>>(emptyList())
    val suggestedAttractions: StateFlow<List<Attraction>> = _suggestedAttractions.asStateFlow()
    private val _travelSharePhotoSuggestions = MutableStateFlow<List<PhotoPostDocument>>(emptyList())
    val travelSharePhotoSuggestions: StateFlow<List<PhotoPostDocument>> =
        _travelSharePhotoSuggestions.asStateFlow()
    private val _isLoadingTravelShareSuggestions = MutableStateFlow(false)
    val isLoadingTravelShareSuggestions: StateFlow<Boolean> = _isLoadingTravelShareSuggestions.asStateFlow()

    // ── Destination & Route sélectionnées ──
    private val _selectedDestination = MutableStateFlow<Destination?>(null)
    val selectedDestination: StateFlow<Destination?> = _selectedDestination.asStateFlow()

    private val _selectedRoute = MutableStateFlow<TravelRoute?>(null)
    val selectedRoute: StateFlow<TravelRoute?> = _selectedRoute.asStateFlow()

    // ── Loading state ──
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Weather ──
    private val _weather = MutableStateFlow<WeatherInfo?>(null)
    val weather: StateFlow<WeatherInfo?> = _weather.asStateFlow()

    // ── PDF export result ──
    private val _pdfExportPath = MutableStateFlow<File?>(null)
    val pdfExportPath: StateFlow<File?> = _pdfExportPath.asStateFlow()

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
    val formAvoidRain = MutableStateFlow(false)
    val formAvoidHeat = MutableStateFlow(false)
    val formAvoidCold = MutableStateFlow(false)
    val formTravelShareSourcePostId = MutableStateFlow<String?>(null)
    val formTravelSharePlaceName = MutableStateFlow("")
    val formTravelShareTags = MutableStateFlow<List<String>>(emptyList())
    val travelShareSeedPostId = formTravelShareSourcePostId
    val travelShareSeedPlaceName = formTravelSharePlaceName

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
    private var savedAvoidRain: Boolean = false
    private var savedAvoidHeat: Boolean = false
    private var savedAvoidCold: Boolean = false

    // ── Attractions par route ──
    private var routeAttractionsMap: Map<String, List<Attraction>> = emptyMap()
    private var suggestedAttractionsJob: Job? = null
    private var travelShareSuggestionsJob: Job? = null
    private var stopPhotosJob: Job? = null
    private val selectedTravelSharePosts = MutableStateFlow<List<PhotoPostDocument>>(emptyList())

    enum class RegenerationAdjustment {
        INDOOR,
        CHEAPER,
        LESS_WALKING,
        SURPRISE
    }

    init {
        loadDestinations()
    }

    fun applyTravelSharePostSeed(postId: String) {
        if (postId.isBlank()) return

        viewModelScope.launch {
            val post = photoPostRepository.getPostOnce(postId).getOrNull() ?: return@launch
            addTravelSharePhotoCandidate(post)
            applyTravelShareSeed(
                TravelPathSeed(
                    sourcePostId = post.postId,
                    placeName = post.locationName,
                    destinationName = post.travelPathDestinationName ?: post.city,
                    latitude = post.displayLatitude ?: post.latitude ?: post.rawLatitude,
                    longitude = post.displayLongitude ?: post.longitude ?: post.rawLongitude,
                    placeType = post.placeType,
                    tags = post.tags
                )
            )
        }
    }

    fun addTravelSharePhotoCandidate(post: PhotoPostDocument) {
        if (post.postId.isBlank()) return
        if (post.visibility != "public" || post.status != "published") return
        if (!post.hasUsableLocation()) return

        val current = selectedTravelSharePosts.value
        if (current.none { it.postId == post.postId }) {
            selectedTravelSharePosts.value = current + post
        }

        val placeName = post.locationName.trim()
        if (placeName.isNotBlank() &&
            formFavoritePlaces.value.none { it.equals(placeName, ignoreCase = true) }
        ) {
            formFavoritePlaces.value = formFavoritePlaces.value + placeName
        }
    }

    fun applyTravelShareSeed(seed: TravelPathSeed) {
        val placeName = seed.placeName.trim()
        val cleanedTags = seed.tags.map { it.trim() }.filter { it.isNotBlank() }

        formTravelShareSourcePostId.value = seed.sourcePostId
        formTravelSharePlaceName.value = placeName
        formTravelShareTags.value = cleanedTags

        val candidateDestination = seed.destinationName?.trim().orEmpty()
        val exists = _destinations.value.any { it.name.equals(candidateDestination, ignoreCase = true) }
        if (candidateDestination.isNotBlank() && exists) {
            formDestination.value = candidateDestination
        }

        if (placeName.isNotBlank() &&
            formFavoritePlaces.value.none { it.equals(placeName, ignoreCase = true) }
        ) {
            formFavoritePlaces.value = formFavoritePlaces.value + placeName
        }

        val inferred = inferActivitiesFromTravelShareSeed(seed)
        if (inferred.isNotEmpty()) {
            formActivities.value = formActivities.value + inferred
        }
    }

    fun clearTravelShareSeed() {
        val placeName = formTravelSharePlaceName.value
        if (placeName.isNotBlank()) {
            formFavoritePlaces.value = formFavoritePlaces.value.filterNot {
                it.equals(placeName, ignoreCase = true)
            }
        }
        formTravelShareSourcePostId.value = null
        formTravelSharePlaceName.value = ""
        formTravelShareTags.value = emptyList()
    }

    private fun inferActivitiesFromTravelShareSeed(seed: TravelPathSeed): Set<String> {
        val values = buildList {
            add(seed.placeType.lowercase())
            addAll(seed.tags.map { it.lowercase() })
        }

        val inferred = mutableSetOf<String>()
        values.forEach { value ->
            when {
                value.contains("museum") || value.contains("musée") || value.contains("culture") || value.contains("cultural") -> {
                    inferred += "culture"
                }
                value.contains("nature") || value.contains("park") || value.contains("parc") ||
                    value.contains("mountain") || value.contains("beach") || value.contains("lake") || value.contains("forest") -> {
                    inferred += "nature"
                }
                value.contains("restaurant") || value.contains("food") || value.contains("gastronomie") ||
                    value.contains("cafe") || value.contains("café") -> {
                    inferred += "food"
                }
                value.contains("shop") || value.contains("shopping") || value.contains("magasin") || value.contains("market") -> {
                    inferred += "shopping"
                }
                value.contains("street") || value.contains("rue") || value.contains("monument") ||
                    value.contains("architecture") || value.contains("photo") -> {
                    inferred += "photo"
                    inferred += "culture"
                }
            }
        }
        return inferred
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
        favoritePlaces: List<String> = emptyList(),
        avoidRain: Boolean = false,
        avoidHeat: Boolean = false,
        avoidCold: Boolean = false
    ) {
        val dest = _destinations.value.find {
            it.name.equals(destinationName, ignoreCase = true)
        } ?: return

        _selectedDestination.value = dest
        savedFavoritePlaces = favoritePlaces
        savedActivities = activities
        savedAvoidRain = avoidRain
        savedAvoidHeat = avoidHeat
        savedAvoidCold = avoidCold

        viewModelScope.launch {
            _isLoading.value = true
            repository.getAttractionsByDestination(dest.id).collect { allAttractions ->
                val linkedTravelSharePosts = runCatching {
                    travelBridgeRepository.getLinkedTravelSharePhotosForDestination(dest.id)
                }.getOrDefault(emptyList())
                val linkedTravelShareAttractions = runCatching {
                    travelBridgeRepository.getTravelShareAttractionsForDestination(dest.id)
                }.getOrDefault(emptyList())
                val candidatePosts = (selectedTravelSharePosts.value + linkedTravelSharePosts)
                    .distinctBy { it.postId }
                val travelShareCandidates = buildTravelSharePhotoAttractionCandidates(
                    posts = candidatePosts,
                    officialAttractions = allAttractions,
                    destination = dest
                )
                val travelShareAttractionCandidates = buildTravelShareStoredAttractionCandidates(
                    attractions = linkedTravelShareAttractions,
                    officialAttractions = allAttractions + travelShareCandidates,
                    destination = dest
                )
                val mergedAttractions = allAttractions + travelShareCandidates + travelShareAttractionCandidates

                _attractions.value = mergedAttractions
                val currentWeather = weatherService.getWeather(dest.lat, dest.lng)
                _weather.value = currentWeather

                // ── Filtrage strict → relaxé ──
                val filtered = filterAttractions(mergedAttractions, budget, activities, effort, currentWeather)
                val weatherAdjustedAll = applyWeatherPreferences(mergedAttractions, currentWeather)

                // Générer les 3 routes avec des sous-ensembles différents
                _routes.value = generateRoutes(filtered, weatherAdjustedAll, dest, budget, durationHours)

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
        updateOfficialAttractionSuggestions(cityName)
        updateTravelSharePhotoSuggestions(cityName)
    }

    private fun updateOfficialAttractionSuggestions(cityName: String) {
        val dest = _destinations.value.find { it.name.equals(cityName, ignoreCase = true) }
        if (dest == null) {
            _suggestedAttractions.value = emptyList()
            suggestedAttractionsJob?.cancel()
            suggestedAttractionsJob = null
            return
        }

        suggestedAttractionsJob?.cancel()
        suggestedAttractionsJob = viewModelScope.launch {
            repository.getAttractionsByDestination(dest.id).collect { list ->
                _suggestedAttractions.value = list.sortedByDescending { it.rating }.take(5)
            }
        }
    }

    private fun updateTravelSharePhotoSuggestions(cityName: String) {
        val dest = _destinations.value.find { it.name.equals(cityName, ignoreCase = true) }
        if (cityName.isBlank() || dest == null) {
            _travelSharePhotoSuggestions.value = emptyList()
            travelShareSuggestionsJob?.cancel()
            travelShareSuggestionsJob = null
            _isLoadingTravelShareSuggestions.value = false
            return
        }

        travelShareSuggestionsJob?.cancel()
        travelShareSuggestionsJob = viewModelScope.launch {
            _isLoadingTravelShareSuggestions.value = true
            travelBridgeRepository.observeLinkedTravelSharePhotosForDestination(
                destinationId = dest.id,
                fallbackCity = dest.name
            ).collect { posts ->
                _travelSharePhotoSuggestions.value = posts.sortedByDescending { rankTravelShareSuggestion(it) }
                _isLoadingTravelShareSuggestions.value = false
            }
        }
    }

    fun loadTravelSharePhotosForStops(stops: List<RouteStop>) {
        if (stops.isEmpty()) {
            _stopTravelSharePhotos.value = emptyMap()
            stopPhotosJob?.cancel()
            stopPhotosJob = null
            return
        }

        val destinationName = _selectedDestination.value?.name
        stopPhotosJob?.cancel()
        stopPhotosJob = viewModelScope.launch {
            val result = mutableMapOf<String, List<PhotoPostDocument>>()
            val selectedPosts = selectedTravelSharePosts.value
            stops.take(8).forEach { stop ->
                val linkedPoolPhotos = runCatching {
                    travelBridgeRepository.findPhotosForRouteStop(
                        stopName = stop.name,
                        lat = stop.lat.takeIf { it != 0.0 },
                        lng = stop.lng.takeIf { it != 0.0 },
                        city = destinationName,
                        radiusKm = 0.5
                    )
                }.getOrDefault(emptyList())

                val selectedMatches = selectedPosts.filter { post ->
                    val postLat = post.displayLatitude ?: post.latitude ?: post.rawLatitude
                    val postLng = post.displayLongitude ?: post.longitude ?: post.rawLongitude
                    val byName = areNamesSimilar(post.locationName, stop.name)
                    val byDistance = if (
                        postLat != null && postLng != null &&
                        stop.lat != 0.0 && stop.lng != 0.0
                    ) {
                        haversine(postLat, postLng, stop.lat, stop.lng) <= 0.5
                    } else {
                        false
                    }
                    byName || byDistance
                }

                val photos = (selectedMatches + linkedPoolPhotos)
                    .distinctBy { it.postId }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }

                if (photos.isNotEmpty()) {
                    result[stop.id] = photos.take(6)
                }
            }
            _stopTravelSharePhotos.value = result
        }
    }

    private fun rankTravelShareSuggestion(post: PhotoPostDocument): Int {
        var score = 0
        val selectedActivities = formActivities.value
        val seedTags = formTravelShareTags.value.map { it.lowercase() }
        val postTags = post.tags.map { it.lowercase() }

        if (postTags.any { seedTags.contains(it) }) score += 4
        if (placeTypeMatchesSelectedActivities(post.placeType, selectedActivities)) score += 3
        score += minOf(post.likeCount, 10)
        score += ((post.createdAt?.seconds ?: 0L) / 86_400).toInt()
        return score
    }

    private fun placeTypeMatchesSelectedActivities(placeType: String, activities: Set<String>): Boolean {
        val value = placeType.lowercase()
        if (activities.isEmpty()) return false
        return when {
            value.contains("nature") || value.contains("park") -> "nature" in activities
            value.contains("museum") || value.contains("culture") || value.contains("monument") -> "culture" in activities
            value.contains("food") || value.contains("restaurant") || value.contains("cafe") -> "food" in activities
            value.contains("shop") || value.contains("market") -> "shopping" in activities
            value.contains("street") || value.contains("photo") -> "photo" in activities || "culture" in activities
            else -> false
        }
    }

    private fun buildTravelSharePhotoAttractionCandidates(
        posts: List<PhotoPostDocument>,
        officialAttractions: List<Attraction>,
        destination: Destination
    ): List<Attraction> {
        return posts
            .filter { it.visibility == "public" && it.status == "published" }
            .filter {
                it.travelPathDestinationId.isNullOrBlank() ||
                    it.travelPathDestinationId == destination.id ||
                    it.city.equals(destination.name, ignoreCase = true)
            }
            .filter { it.hasUsableLocation() }
            .filterNot { post ->
                officialAttractions.any { attr -> isDuplicateTravelSharePlace(post, attr) }
            }
            .map { it.toAttractionCandidate(destination.id) }
    }

    private fun PhotoPostDocument.toAttractionCandidate(destinationId: String): Attraction {
        val resolvedLat = displayLatitude ?: latitude ?: rawLatitude ?: 0.0
        val resolvedLng = displayLongitude ?: longitude ?: rawLongitude ?: 0.0
        val attractionType = mapPlaceTypeToAttractionType(placeType, tags)
        return Attraction(
            id = "photo_$postId",
            destinationId = destinationId,
            name = locationName.ifBlank { title.ifBlank { "Lieu TravelShare" } },
            type = attractionType,
            cost = estimateTravelShareCost(attractionType, placeType, tags),
            duration = 45,
            rating = 4.2 + minOf(likeCount, 50) / 100.0,
            description = description.ifBlank { title },
            imageUrl = imageUrls.firstOrNull().orEmpty(),
            lat = resolvedLat,
            lng = resolvedLng,
            tags = (tags + "TravelShare").distinct(),
            imageUrls = imageUrls,
            source = "travelshare",
            sourcePostId = postId
        )
    }

    private fun buildTravelShareStoredAttractionCandidates(
        attractions: List<TravelShareAttractionDocument>,
        officialAttractions: List<Attraction>,
        destination: Destination
    ): List<Attraction> {
        return attractions
            .filter { it.status == "active" && it.destinationId == destination.id }
            .filter { it.name.isNotBlank() && it.lat != 0.0 && it.lng != 0.0 }
            .filterNot { travelShareAttraction ->
                officialAttractions.any { attr -> isDuplicateTravelShareAttraction(travelShareAttraction, attr) }
            }
            .map { it.toAttractionCandidate() }
    }

    private fun TravelShareAttractionDocument.toAttractionCandidate(): Attraction {
        return Attraction(
            id = id,
            destinationId = destinationId,
            name = name,
            type = type.ifBlank { "Photo" },
            cost = cost.takeIf { it > 0 } ?: estimateTravelShareCost(type, type, tags),
            duration = duration,
            rating = rating,
            description = description,
            imageUrl = imageUrl,
            lat = lat,
            lng = lng,
            openHours = openHours,
            closedDay = closedDay,
            effortLevel = effortLevel,
            weatherType = weatherType,
            bestTimeSlots = bestTimeSlots,
            tags = (tags + "TravelShare").distinct(),
            imageUrls = imageUrls.ifEmpty { listOf(imageUrl).filter { it.isNotBlank() } },
            source = source,
            sourcePostId = sourcePostId
        )
    }

    private fun mapPlaceTypeToAttractionType(placeType: String, tags: List<String>): String {
        val terms = (listOf(placeType) + tags).joinToString(" ").lowercase()
        return when {
            terms.contains("museum") || terms.contains("musée") || terms.contains("culture") || terms.contains("cultural") -> "Culture"
            terms.contains("monument") || terms.contains("architecture") || terms.contains("street") || terms.contains("rue") -> "Monument"
            terms.contains("nature") || terms.contains("park") || terms.contains("parc") || terms.contains("beach") ||
                terms.contains("mountain") || terms.contains("lake") || terms.contains("forest") -> "Nature"
            terms.contains("restaurant") || terms.contains("food") || terms.contains("gastronomie") ||
                terms.contains("cafe") || terms.contains("café") -> "Gastronomie"
            terms.contains("shop") || terms.contains("shopping") || terms.contains("market") || terms.contains("magasin") -> "Shopping"
            terms.contains("sport") -> "Loisirs"
            terms.contains("nightlife") || terms.contains("soir") || terms.contains("bar") -> "Loisirs"
            terms.contains("leisure") || terms.contains("loisirs") -> "Loisirs"
            else -> "Photo"
        }
    }

    private fun estimateTravelShareCost(type: String, placeType: String, tags: List<String>): Int {
        val terms = (listOf(type, placeType) + tags).joinToString(" ").lowercase()
        return when {
            terms.contains("restaurant") || terms.contains("food") || terms.contains("gastronomie") ||
                terms.contains("cafe") || terms.contains("café") -> 25
            terms.contains("shop") || terms.contains("shopping") || terms.contains("market") || terms.contains("magasin") -> 20
            terms.contains("museum") || terms.contains("musée") || terms.contains("culture") -> 12
            terms.contains("monument") || terms.contains("architecture") -> 10
            terms.contains("sport") -> 15
            terms.contains("nightlife") || terms.contains("bar") -> 30
            terms.contains("leisure") || terms.contains("loisirs") -> 18
            terms.contains("nature") || terms.contains("park") || terms.contains("parc") ||
                terms.contains("beach") || terms.contains("mountain") || terms.contains("lake") || terms.contains("forest") -> 0
            else -> 5
        }
    }

    private fun isDuplicateTravelSharePlace(post: PhotoPostDocument, attraction: Attraction): Boolean {
        val postLat = post.displayLatitude ?: post.latitude ?: post.rawLatitude
        val postLng = post.displayLongitude ?: post.longitude ?: post.rawLongitude
        if (postLat != null && postLng != null && attraction.lat != 0.0 && attraction.lng != 0.0) {
            val distanceKm = haversine(postLat, postLng, attraction.lat, attraction.lng)
            if (distanceKm <= 0.2) return true
        }
        return areNamesSimilar(post.locationName, attraction.name)
    }

    private fun isDuplicateTravelShareAttraction(travelShareAttraction: TravelShareAttractionDocument, attraction: Attraction): Boolean {
        if (travelShareAttraction.lat != 0.0 && travelShareAttraction.lng != 0.0 && attraction.lat != 0.0 && attraction.lng != 0.0) {
            val distanceKm = haversine(travelShareAttraction.lat, travelShareAttraction.lng, attraction.lat, attraction.lng)
            if (distanceKm <= 0.2) return true
        }
        return areNamesSimilar(travelShareAttraction.name, attraction.name)
    }

    private fun areNamesSimilar(a: String, b: String): Boolean {
        val left = normalizePlaceName(a)
        val right = normalizePlaceName(b)
        if (left.isBlank() || right.isBlank()) return false
        if (left == right) return true
        return left.contains(right) || right.contains(left)
    }

    private fun normalizePlaceName(input: String): String {
        val ascii = Normalizer.normalize(input.lowercase(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return ascii
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .split("\\s+".toRegex())
            .filter { token ->
                token.isNotBlank() && token !in setOf("the", "le", "la", "les", "de", "des", "du", "d")
            }
            .joinToString(" ")
            .trim()
    }

    private fun PhotoPostDocument.hasUsableLocation(): Boolean {
        val lat = displayLatitude ?: latitude ?: rawLatitude
        val lng = displayLongitude ?: longitude ?: rawLongitude
        return lat != null && lng != null && locationName.isNotBlank()
    }
    //  FILTERING LOGIC
    private fun filterAttractions(
        all: List<Attraction>,
        budget: Int,
        activities: Set<String>,
        effort: Int,
        weather: WeatherInfo?
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

        return applyWeatherPreferences(result, weather).sortedByDescending { weatherScore(it, weather) }
    }

    private fun applyWeatherPreferences(
        attractions: List<Attraction>,
        weather: WeatherInfo?
    ): List<Attraction> {
        if (weather == null || !shouldPreferShelter(weather)) return attractions

        val sheltered = attractions.filter { it.weatherType == "indoor" || it.weatherType == "both" }
        return if (sheltered.size >= 3) {
            sheltered.sortedByDescending { weatherScore(it, weather) }
        } else {
            attractions.sortedByDescending { weatherScore(it, weather) }
        }
    }

    private fun shouldPreferShelter(weather: WeatherInfo): Boolean {
        return (savedAvoidRain && isRainy(weather.weatherCode)) ||
            (savedAvoidHeat && weather.temperature >= 28.0) ||
            (savedAvoidCold && weather.temperature <= 5.0)
    }

    private fun weatherScore(attr: Attraction, weather: WeatherInfo?): Double {
        if (weather == null || !shouldPreferShelter(weather)) return attr.rating

        val shelterBonus = when (attr.weatherType) {
            "indoor" -> 2.0
            "both" -> 1.0
            else -> -2.0
        }
        val durationPenalty = if (attr.weatherType == "outdoor" && attr.duration > 120) 0.8 else 0.0
        return attr.rating + shelterBonus - durationPenalty
    }

    private fun isRainy(code: Int): Boolean {
        return code in 51..67 || code in 80..82 || code in 95..99
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

        val usedIds = mutableSetOf<String>() // Pour éviter les doublons entre routes
        val favoriteAttractionIds = (filtered + allAttractions)
            .filter { matchesFavoritePlace(it) }
            .map { it.id }
            .toSet()

        // ── Route 1 : Économique ──
        val cheapPool = filtered.sortedBy { it.cost }
        val cheapAttractions = selectWithinConstraints(cheapPool, budget, maxMinutes, wantedTypes, usedIds)
        usedIds.addAll(cheapAttractions.map { it.id }.filterNot { it in favoriteAttractionIds })

        // ── Route 2 : Équilibrée ──
        val balancedPool = filtered.sortedByDescending { it.rating / (it.cost.coerceAtLeast(1).toDouble()) }
        val balancedAttractions = selectWithinConstraints(balancedPool, budget, maxMinutes, wantedTypes, usedIds)
        usedIds.addAll(balancedAttractions.map { it.id }.filterNot { it in favoriteAttractionIds })

        // ── Route 3 : Premium (pool élargi, budget +50%) ──
        val premiumPool = allAttractions.sortedByDescending { it.rating }
        val premiumAttractions = selectWithinConstraints(premiumPool, (budget * 1.5).toInt(), (maxMinutes * 1.3).toInt(), wantedTypes, usedIds)

        // Stocker les associations route → attractions
        routeAttractionsMap = mapOf(
            "${dest.id}_eco" to cheapAttractions,
            "${dest.id}_bal" to balancedAttractions,
            "${dest.id}_pre" to premiumAttractions
        )

        val routes = mutableListOf<TravelRoute>()

        if (cheapAttractions.isNotEmpty()) {
            routes.add(buildTravelRoute("${dest.id}_eco", "Route Économique", "Budget maîtrisé", cheapAttractions, dest,
                "Modéré", 3, Pair(0xFF10B981, 0xFF0D9488)))
        }
        if (balancedAttractions.isNotEmpty()) {
            routes.add(buildTravelRoute("${dest.id}_bal", "Route Équilibrée", "Meilleur rapport qualité-prix", balancedAttractions, dest,
                "Facile", 2, Pair(0xFFB91C1C, 0xFF991B1B)))
        }
        if (premiumAttractions.isNotEmpty()) {
            routes.add(buildTravelRoute("${dest.id}_pre", "Route Premium", "Expérience haut de gamme", premiumAttractions, dest,
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

        // D'abord, essayer d'inclure les lieux demandés par l'utilisateur ou TravelShare.
        for (attr in available.filter { matchesFavoritePlace(it) }) {
            val transitTime = if (selected.isEmpty()) 0 else 15
            val newTotalMinutes = totalMinutes + attr.duration + transitTime
            val newTotalCost = totalCost + attr.cost

            if (newTotalCost <= maxBudget && newTotalMinutes <= maxMinutes) {
                selected.add(attr)
                totalCost = newTotalCost
                totalMinutes = newTotalMinutes
                if (attr.type in wantedTypes) hasPrefMatch = true
            }
        }

        // Ensuite, garantir au moins une attraction de la préférence.
        if (wantedTypes.isNotEmpty()) {
            val prefFirst = available.firstOrNull { it.type in wantedTypes && it !in selected }
            if (prefFirst != null) {
                val transitTime = if (selected.isEmpty()) 0 else 15
                val newTotalMinutes = totalMinutes + prefFirst.duration + transitTime
                val newTotalCost = totalCost + prefFirst.cost

                if (newTotalCost <= maxBudget && newTotalMinutes <= maxMinutes) {
                    selected.add(prefFirst)
                    totalCost = newTotalCost
                    totalMinutes = newTotalMinutes
                    hasPrefMatch = true
                }
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

    private fun matchesFavoritePlace(attr: Attraction): Boolean {
        return savedFavoritePlaces.any { favorite ->
            val place = favorite.trim()
            place.isNotBlank() &&
                (attr.name.contains(place, ignoreCase = true) || place.contains(attr.name, ignoreCase = true))
        }
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
            destName = dest.name,
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
                imageUrls = attr.imageUrls.ifEmpty { listOf(attr.imageUrl).filter { it.isNotBlank() } },
                rating = attr.rating.toFloat(),
                openHours = attr.openHours,
                lat = attr.lat,
                lng = attr.lng,
                source = attr.source,
                sourcePostId = attr.sourcePostId
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

    // ═══════════════════════════════════════════════════
    //  §3. INTERACTION — Like / Save / Share / Regenerate
    // ═══════════════════════════════════════════════════

    fun isRouteLiked(routeId: String): Boolean =
        localStorage?.isRouteLiked(routeId) ?: false

    fun toggleRouteLike(routeId: String): Boolean {
        val route = _selectedRoute.value
        val destName = route?.destName ?: _selectedDestination.value?.name ?: ""

        // Local persistence
        route?.let {
            localStorage?.storeRouteInfo(it, destName)
            localStorage?.cacheRoute(it.id, it, _routeStops.value)
        }
        val nowLiked = localStorage?.toggleRouteLike(routeId) ?: false

        // Sync to Firestore
        viewModelScope.launch {
            try {
                if (nowLiked && route != null) {
                    savedRoutesRepo.saveRoute(route, _routeStops.value, destName, "liked")
                } else {
                    savedRoutesRepo.removeRoute(routeId, "liked")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return nowLiked
    }

    fun isRouteSaved(routeId: String): Boolean =
        localStorage?.isRouteSaved(routeId) ?: false

    fun toggleRouteSave(routeId: String): Boolean {
        val route = _selectedRoute.value
        val destName = route?.destName ?: _selectedDestination.value?.name ?: ""

        route?.let {
            localStorage?.storeRouteInfo(it, destName)
            localStorage?.cacheRoute(it.id, it, _routeStops.value)
        }
        val nowSaved = localStorage?.toggleRouteSave(routeId) ?: false

        // Sync to Firestore
        viewModelScope.launch {
            try {
                if (nowSaved && route != null) {
                    savedRoutesRepo.saveRoute(route, _routeStops.value, destName, "saved")
                } else {
                    savedRoutesRepo.removeRoute(routeId, "saved")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return nowSaved
    }

    fun getLikedRoutes() = localStorage?.getLikedRoutes() ?: emptyList()
    fun getSavedRoutes() = localStorage?.getSavedRoutes() ?: emptyList()

    /** Load a cached route into ViewModel state so RouteDetailScreen can display it.
     *  Tries local cache first, then falls back to Firestore. */
    fun loadCachedRoute(routeId: String): Boolean {
        // Try local cache first
        val cached = localStorage?.getCachedRoute(routeId)
        if (cached != null) {
            val (route, stops) = cached
            _selectedRoute.value = route
            _routeStops.value = stops
            return true
        }
        // Will try Firestore asynchronously
        return false
    }

    /** Async version: load route from Firestore when local cache is empty */
    suspend fun loadCachedRouteAsync(routeId: String): Boolean {
        // Try local first
        val cached = localStorage?.getCachedRoute(routeId)
        if (cached != null) {
            val (route, stops) = cached
            _selectedRoute.value = route
            _routeStops.value = stops
            return true
        }
        // Try Firestore (check both types)
        for (type in listOf("liked", "saved")) {
            val doc = savedRoutesRepo.getRoute(routeId, type)
            if (doc != null) {
                val (route, stops) = savedRoutesRepo.toRouteAndStops(doc)
                _selectedRoute.value = route
                _routeStops.value = stops
                // Also cache locally for next time
                localStorage?.cacheRoute(route.id, route, stops)
                return true
            }
        }
        return false
    }

    /** Re-generate the current route with a concrete adjustment. */
    fun regenerateCurrentRoute(adjustment: RegenerationAdjustment = RegenerationAdjustment.SURPRISE) {
        val route = _selectedRoute.value ?: return
        val routeId = route.id
        val currentAttractions = routeAttractionsMap[routeId] ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val targetCount = currentAttractions.size.coerceAtLeast(1)
            val candidatePool = _attractions.value.ifEmpty { currentAttractions }
            val adjusted = when (adjustment) {
                RegenerationAdjustment.INDOOR -> candidatePool.sortedByDescending {
                    when (it.weatherType) {
                        "indoor" -> 3
                        "both" -> 2
                        else -> 1
                    }
                }
                RegenerationAdjustment.CHEAPER -> candidatePool.sortedWith(
                    compareBy<Attraction> { it.cost }.thenByDescending { it.rating }
                )
                RegenerationAdjustment.LESS_WALKING -> candidatePool.sortedBy { it.effortLevel }
                RegenerationAdjustment.SURPRISE -> candidatePool.shuffled()
            }.distinctBy { it.id }.take(targetCount).ifEmpty { currentAttractions.shuffled() }
            val adjustedStops = generateStops(adjusted)
            routeAttractionsMap = routeAttractionsMap + (routeId to adjusted)
            _routeStops.value = adjustedStops
            _selectedRoute.value = route.copy(
                budget = adjusted.sumOf { it.cost },
                stops = adjusted.size,
                imageUrl = adjusted.firstOrNull()?.imageUrl ?: route.imageUrl,
                highlights = adjusted.take(3).map { it.name }
            )
            _isLoading.value = false
        }
    }

    /** Build a share text and return an ACTION_SEND Intent */
    fun buildShareIntent(): Intent {
        val route = _selectedRoute.value
        val dest = _selectedDestination.value
        val stops = _routeStops.value

        val text = buildString {
            appendLine("Mon itinéraire ${route?.name ?: ""} - ${dest?.name ?: route?.destName ?: ""}")
            appendLine("Budget : ${route?.budget ?: 0} € | Durée : ${route?.duration ?: ""}")
            appendLine()
            stops.forEachIndexed { i, s ->
                appendLine("${i + 1}. ${s.arrivalTime} - ${s.name} (${s.type})")
            }
            appendLine()
            appendLine("Généré par TravelPath")
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Itinéraire ${dest?.name ?: route?.destName ?: ""}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    // ═══════════════════════════════════════════════════
    //  §4. OFFLINE MODE — Cache routes locally
    // ═══════════════════════════════════════════════════

    fun cacheCurrentRoute() {
        val route = _selectedRoute.value ?: return
        val stops = _routeStops.value
        if (stops.isEmpty()) return
        val key = "${_selectedDestination.value?.name ?: route.destName.ifBlank { "dest" }}_${route.id}"
        localStorage?.cacheRoute(key, route, stops)
    }

    suspend fun cacheCurrentRouteLite(): Boolean {
        val route = _selectedRoute.value ?: return false
        val stops = _routeStops.value
        if (stops.isEmpty()) return false
        val key = "${_selectedDestination.value?.name ?: route.destName.ifBlank { "dest" }}_${route.id}"
        localStorage?.cacheRouteLite(key, route, stops) ?: return false
        return true
    }

    // ═══════════════════════════════════════════════════
    //  §5. WEATHER — Fetch from Open-Meteo
    // ═══════════════════════════════════════════════════

    fun fetchWeather() {
        val dest = _selectedDestination.value ?: return
        if (dest.lat == 0.0 && dest.lng == 0.0) {
            // Use first stop coords as fallback
            val firstStop = _routeStops.value.firstOrNull { it.lat != 0.0 } ?: return
            fetchWeatherAt(firstStop.lat, firstStop.lng)
        } else {
            fetchWeatherAt(dest.lat, dest.lng)
        }
    }

    private fun fetchWeatherAt(lat: Double, lng: Double) {
        viewModelScope.launch {
            _weather.value = weatherService.getWeather(lat, lng)
        }
    }

    // ═══════════════════════════════════════════════════
    //  §6. PDF EXPORT
    // ═══════════════════════════════════════════════════

    fun exportPdf(context: Context) {
        val route = _selectedRoute.value ?: return
        val stops = _routeStops.value
        val destName = _selectedDestination.value?.name ?: "Destination"
        viewModelScope.launch {
            val file = pdfService.exportItinerary(context, route, stops, destName)
            _pdfExportPath.value = file
        }
    }

    fun clearPdfExport() {
        _pdfExportPath.value = null
    }
}
